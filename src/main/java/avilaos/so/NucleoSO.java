package avilaos.so;

import avilaos.estructuras.ListaEnlazada;
import avilaos.estructuras.SerieDatos;
import avilaos.hardware.CPU;
import avilaos.hardware.ControladorES;
import avilaos.hardware.Memoria;
import avilaos.modelo.CausaFin;
import avilaos.modelo.EstadoProceso;
import avilaos.modelo.MotivoBloqueo;
import avilaos.modelo.PoliticaPlanificacion;
import avilaos.modelo.Proceso;
import avilaos.modelo.SolicitudDisco;
import avilaos.modelo.TipoOperacionES;
import avilaos.sync.BufferAcotado;
import avilaos.sync.SemaforoSO;
import java.util.concurrent.Semaphore;

/**
 * Núcleo del sistema operativo de UN computador: admisión (largo plazo), planificador
 * (corto plazo), colas y control de bloqueos. Todo su estado lo protege 'mutex'; el hilo del
 * computador lo toma durante todo su ciclo y la GUI lo toma brevemente al sacar instantáneas.
 * Orden de adquisición de locks: mutex del SO -> mutex de semáforo/buffer -> locks hoja.
 */
public class NucleoSO {
    private static final int VENTANA_MUESTRA = 5;   // ciclos por punto del gráfico de utilización

    private final Computador computador;
    private final Memoria ram;
    private CPU cpu;
    private ControladorES controlador;

    private final Semaphore mutex = new Semaphore(1);
    private final ListaEnlazada<Proceso> nuevos = new ListaEnlazada<>();
    private final ListaEnlazada<Proceso> listos = new ListaEnlazada<>();
    private final ListaEnlazada<Proceso> bloqueados = new ListaEnlazada<>();
    private final ListaEnlazada<Proceso> terminados = new ListaEnlazada<>();
    // Modelo de siete estados: procesos en el área de intercambio (fuera de la RAM)
    private final ListaEnlazada<Proceso> listosSusp = new ListaEnlazada<>();
    private final ListaEnlazada<Proceso> bloqSusp = new ListaEnlazada<>();
    private int suspensiones, reactivaciones;
    private final ListaEnlazada<Proceso> todos = new ListaEnlazada<>();

    // Procesos de OTROS computadores (o de este) desbloqueados por un semSignal: se procesan al inicio del ciclo.
    private final ListaEnlazada<Proceso> bandeja = new ListaEnlazada<>();
    private final Semaphore mutexBandeja = new Semaphore(1);

    private volatile PoliticaPlanificacion politica;
    private long secuencia;
    private int completados, vencidos;
    private long cicloActual;
    private final SerieDatos serieUtilizacion = new SerieDatos();

    public NucleoSO(Computador computador, Memoria ram) {
        this.computador = computador;
        this.ram = ram;
    }

    public void conectar(CPU cpu, ControladorES controlador) {
        this.cpu = cpu;
        this.controlador = controlador;
    }

    public void entrar() { mutex.acquireUninterruptibly(); }
    public void salir() { mutex.release(); }

    // ------------------------------------------------------------------ API externa (otros hilos)

    /** Un proceso es creado en este computador (cola de nuevos). Llamado desde la GUI. */
    public void recibirNuevo(Proceso p) {
        entrar();
        try {
            todos.agregarAlFinal(p);
            nuevos.agregarAlFinal(p);
            computador.registrar("Proceso " + p.etiqueta() + " (" + p.getNombre() + ", " + p.getTipo()
                    + ") creado en " + computador.getNombre() + ": entra a la cola de nuevos");
        } finally { salir(); }
    }

    /** Un semSignal despertó a p: se deja en la bandeja y se atiende al inicio del próximo ciclo. */
    public void recibirDesbloqueado(Proceso p) {
        mutexBandeja.acquireUninterruptibly();
        try { bandeja.agregarAlFinal(p); } finally { mutexBandeja.release(); }
    }

    public void setPolitica(PoliticaPlanificacion nueva) {
        entrar();
        try {
            this.politica = nueva;
            nueva.ordenar(listos);
            computador.registrar(computador.getNombre() + " cambia su política de planificación a " + nueva.nombre());
        } finally { salir(); }
    }

    public PoliticaPlanificacion getPolitica() { return politica; }

    // ------------------------------------------------------------------ ciclo (con el mutex ya tomado)

    public void antesDelCiclo(long ciclo) {
        cicloActual = ciclo;
        while (true) {
            Proceso p;
            mutexBandeja.acquireUninterruptibly();
            try { p = bandeja.quitarPrimero(); } finally { mutexBandeja.release(); }
            if (p == null) break;
            if (p.getEstado() == EstadoProceso.BLOQUEADO && bloqueados.quitar(p)) {
                p.limpiarBloqueo();
                computador.registrar("Proceso " + p.etiqueta() + " se desbloquea y pasa a la cola de listos");
                pasarAListo(p);
            }
        }
        admitirNuevos();
    }

    public void despuesDelCiclo(long ciclo) {
        // Tiempo bloqueado en semáforos (métrica del productor-consumidor)
        for (int i = 0; i < bloqueados.tamano(); i++) {
            Proceso p = bloqueados.obtener(i);
            if (p.getMotivo() == MotivoBloqueo.SEMAFORO) {
                p.sumarBloqSem();
                BufferAcotado b = p.getBuffer();
                if (b != null) b.sumarBloqueo(1);
            }
        }
        // Deadlines: todo proceso vivo (nuevo, listo, bloqueado o en CPU) consume su deadline
        ListaEnlazada<Proceso> vencen = new ListaEnlazada<>();
        descontarDeadline(nuevos, vencen);
        descontarDeadline(listos, vencen);
        descontarDeadline(bloqueados, vencen);
        descontarDeadline(listosSusp, vencen);
        descontarDeadline(bloqSusp, vencen);
        Proceso enCPU = cpu.getActual();
        if (enCPU != null) {
            enCPU.decrementarDeadline();
            if (enCPU.getDeadline() <= 0) vencen.agregarAlFinal(enCPU);
        }
        for (int i = 0; i < vencen.tamano(); i++) terminarPorDeadline(vencen.obtener(i));

        if (ciclo % VENTANA_MUESTRA == 0) {
            serieUtilizacion.agregar(100.0 * cpu.tomarOcupadaVentana() / VENTANA_MUESTRA);
        }
    }

    private void descontarDeadline(ListaEnlazada<Proceso> cola, ListaEnlazada<Proceso> vencen) {
        for (int i = 0; i < cola.tamano(); i++) {
            Proceso p = cola.obtener(i);
            p.decrementarDeadline();
            if (p.getDeadline() <= 0) vencen.agregarAlFinal(p);
        }
    }

    // ------------------------------------------------------------------ planificación

    private void pasarAListo(Proceso p) {
        p.setEstado(EstadoProceso.LISTO);
        p.setSecuenciaListo(++secuencia);
        listos.agregarAlFinal(p);
        politica.ordenar(listos);       // el cambio de orden es visible de inmediato
    }

    /**
     * Planificación a largo plazo: admite procesos nuevos solo si hay RAM suficiente. Si el primer proceso que no cabe
     * está esperando, interviene el planificador a mediano plazo para hacerle espacio. Al final intenta reactivar
     * procesos Listo/Suspendido (los nuevos tienen prioridad para no quitarles la memoria que se liberó para ellos).
     */
    private void admitirNuevos() {
        boolean medianoIntentado = false;
        ListaEnlazada<Proceso> copia = nuevos.copiar();
        for (int i = 0; i < copia.tamano(); i++) {
            Proceso p = copia.obtener(i);
            if (ram.reservar(p.getMemoria())) {
                nuevos.quitar(p);
                computador.registrar(computador.getNombre() + " admite a " + p.etiqueta() + " (reserva "
                        + p.getMemoria() + " de RAM): pasa de nuevo a listo");
                pasarAListo(p);
            } else if (!medianoIntentado) {
                medianoIntentado = true;
                planificarMedianoPlazo(p);
            }
        }
        reactivarSuspendidos();
    }

    // ------------------------------------------------------------------ planificador a mediano plazo

    /**
     * Para admitir a 'espera' elige procesos BLOQUEADOS y los suspende (los escribe en el área de intercambio, lo que
     * es una solicitud de E/S al disco). Criterio de selección: el de MENOR prioridad (número mayor); a igual prioridad,
     * el que lleva MÁS tiempo bloqueado. Si ni suspendiendo a todos los candidatos alcanza la memoria, no suspende a nadie.
     */
    private void planificarMedianoPlazo(Proceso espera) {
        int enCurso = 0;
        for (int i = 0; i < bloqueados.tamano(); i++) if (bloqueados.obtener(i).isSuspendiendo()) enCurso += bloqueados.obtener(i).getMemoria();
        int disponible = ram.getLibre() + enCurso;
        if (disponible >= espera.getMemoria()) return;             // ya hay suspensiones en curso que bastan

        ListaEnlazada<Proceso> victimas = new ListaEnlazada<>();
        ListaEnlazada<Proceso> candidatos = bloqueados.copiar();
        int swapLibre = computador.getSwap().getLibres();
        while (disponible < espera.getMemoria()) {
            Proceso v = elegirVictima(candidatos, swapLibre);
            if (v == null) return;
            victimas.agregarAlFinal(v);
            candidatos.quitar(v);
            disponible += v.getMemoria();
            swapLibre -= v.getMemoria();
        }
        for (int i = 0; i < victimas.tamano(); i++) iniciarSuspension(victimas.obtener(i), espera);
    }

    private Proceso elegirVictima(ListaEnlazada<Proceso> candidatos, int swapLibre) {
        Proceso mejor = null;
        for (int i = 0; i < candidatos.tamano(); i++) {
            Proceso c = candidatos.obtener(i);
            if (c.isSuspendiendo() || c.getEstado() != EstadoProceso.BLOQUEADO || c.getMemoria() > swapLibre) continue;
            if (mejor == null || c.getPrioridad() > mejor.getPrioridad()
                    || (c.getPrioridad() == mejor.getPrioridad() && c.getCicloBloqueado() < mejor.getCicloBloqueado())) mejor = c;
        }
        return mejor;
    }

    private void iniciarSuspension(Proceso v, Proceso espera) {
        if (!computador.getSwap().asignar(v.getId(), v.getMemoria())) return;
        v.setSuspendiendo(true);
        v.setEventoDuranteSuspension(false);
        SolicitudDisco s = new SolicitudDisco(computador.getServicioArchivos().posicionSwap(), TipoOperacionES.SUSPENDER,
                v, "swap de " + v.etiqueta(), v.getMemoria());
        computador.getDisco().encolar(s, cicloActual);
        computador.registrar("Se suspende al proceso " + v.etiqueta() + " para admitir a " + espera.etiqueta()
                + " (escribe " + v.getMemoria() + " bloques en el área de intercambio)");
    }

    private void finalizarSuspension(Proceso p) {
        p.setSuspendiendo(false);
        bloqueados.quitar(p);
        ram.liberar(p.getMemoria());
        suspensiones++;
        if (p.isEventoDuranteSuspension()) {
            p.limpiarBloqueo();
            p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
            listosSusp.agregarAlFinal(p);
        } else {
            p.setEstado(EstadoProceso.BLOQUEADO_SUSPENDIDO);
            bloqSusp.agregarAlFinal(p);
        }
        computador.registrar("Proceso " + p.etiqueta() + " queda " + p.getEstado().getTexto() + " en el área de intercambio; libera "
                + p.getMemoria() + " de RAM");
        admitirNuevos();
    }

    /** Si hay RAM, trae de vuelta (por prioridad y luego por antigüedad) a los procesos Listo/Suspendido. */
    private void reactivarSuspendidos() {
        ListaEnlazada<Proceso> cand = listosSusp.copiar();
        while (!cand.estaVacia()) {
            Proceso mejor = null;
            for (int i = 0; i < cand.tamano(); i++) {
                Proceso c = cand.obtener(i);
                if (mejor == null || c.getPrioridad() < mejor.getPrioridad()) mejor = c;
            }
            cand.quitar(mejor);
            if (mejor.isReactivando()) continue;
            if (!ram.reservar(mejor.getMemoria())) continue;
            mejor.setReactivando(true);
            SolicitudDisco s = new SolicitudDisco(computador.getServicioArchivos().posicionSwap(), TipoOperacionES.REACTIVAR,
                    mejor, "swap de " + mejor.etiqueta(), mejor.getMemoria());
            computador.getDisco().encolar(s, cicloActual);
            computador.registrar("Se reactiva al proceso " + mejor.etiqueta() + " (lee " + mejor.getMemoria() + " bloques del área de intercambio)");
        }
    }

    private void finalizarReactivacion(Proceso p) {
        computador.getSwap().liberar(p.getId());
        listosSusp.quitar(p);
        p.setReactivando(false);
        reactivaciones++;
        computador.registrar("Proceso " + p.etiqueta() + " vuelve a la RAM (" + p.getMemoria() + " unidades) y pasa a Listo");
        pasarAListo(p);
    }

    /** Interrupción del disco: terminó de atender una solicitud (de archivo, de suspensión o de reactivación). */
    public void interrupcionDisco(SolicitudDisco s) {
        Proceso p = s.getProceso();
        if (p == null || p.getEstado() == EstadoProceso.TERMINADO) return;        // el proceso murió mientras esperaba
        switch (s.getOperacion()) {
            case SUSPENDER: finalizarSuspension(p); break;
            case REACTIVAR: finalizarReactivacion(p); break;
            default:
                computador.getServicioArchivos().completar(s);
                eventoOcurrido(p, "termina su E/S de disco (" + s.getOperacion().getTexto() + " " + s.getRuta() + ")");
        }
    }

    /** Ocurrió el evento que esperaba el proceso: Bloqueado -> Listo, o Bloqueado/Suspendido -> Listo/Suspendido. */
    private void eventoOcurrido(Proceso p, String texto) {
        if (p.isSuspendiendo()) {                    // lo están escribiendo en el swap: pasará a Listo/Suspendido al terminar
            p.setEventoDuranteSuspension(true);
            computador.registrar("Proceso " + p.etiqueta() + " " + texto + " (mientras se suspendía)");
        } else if (bloqueados.quitar(p)) {
            computador.registrar("Proceso " + p.etiqueta() + " " + texto + " y pasa a Listo");
            p.limpiarBloqueo();
            pasarAListo(p);
        } else if (bloqSusp.quitar(p)) {
            p.limpiarBloqueo();
            p.setEstado(EstadoProceso.LISTO_SUSPENDIDO);
            listosSusp.agregarAlFinal(p);
            computador.registrar("Proceso " + p.etiqueta() + " " + texto + " y pasa de Bloqueado/Suspendido a Listo/Suspendido");
            reactivarSuspendidos();
        }
    }

    public Proceso despachar() { return politica.seleccionar(listos); }

    public boolean debeExpropiar(Proceso actual, int ciclosEnCPU) {
        return politica.debeExpropiar(actual, listos, ciclosEnCPU);
    }

    public void expropiar(Proceso p) {
        computador.registrar(computador.getNombre() + " expropia a " + p.etiqueta() + " (" + politica.tipo() + ")");
        pasarAListo(p);
    }

    public void bloquear(Proceso p) {
        p.setEstado(EstadoProceso.BLOQUEADO);
        bloqueados.agregarAlFinal(p);
        p.setCicloBloqueado(cicloActual);
        if (p.getMotivo() == MotivoBloqueo.ENTRADA_SALIDA) {
            if (p.getContadorBloqueo() > 0) {            // E/S con duración fija (Proyecto 1): la despierta el controlador
                computador.registrar("Proceso " + p.etiqueta() + " se bloquea por E/S");
                controlador.registrar(p);
            }                                            // E/S de disco: la despierta el disco al atender su solicitud
        } else if (p.getMotivo() == MotivoBloqueo.RED) {
            controlador.registrar(p);
        }
    }

    /** Interrupción del controlador de E/S: la solicitud (E/S o red) terminó. */
    public void interrupcionES(Proceso p) {
        eventoOcurrido(p, "termina su " + p.getMotivo().getTexto());
    }

    public void terminarNormal(Proceso p) {
        p.setEstado(EstadoProceso.TERMINADO);
        p.setCausaFin(CausaFin.NORMAL);
        p.setCicloFin(cicloActual);
        ram.liberar(p.getMemoria());
        terminados.agregarAlFinal(p);
        completados++;
        computador.registrar("Proceso " + p.etiqueta() + " (" + p.getNombre() + ") termina; libera "
                + p.getMemoria() + " de RAM en " + computador.getNombre());
        admitirNuevos();     // la memoria liberada puede permitir admitir procesos de la cola de nuevos
    }

    private void terminarPorDeadline(Proceso p) {
        EstadoProceso est = p.getEstado();
        boolean enRam = est.enMemoria() || (est == EstadoProceso.LISTO_SUSPENDIDO && p.isReactivando());
        boolean usaSwap = est.suspendido() || p.isSuspendiendo();
        nuevos.quitar(p);
        listos.quitar(p);
        bloqueados.quitar(p);
        listosSusp.quitar(p);
        bloqSusp.quitar(p);
        controlador.cancelar(p);
        if (cpu.getActual() == p) cpu.liberar();

        computador.getDisco().cancelarDe(p);                // sus solicitudes pendientes salen de la cola del disco
        computador.getServicioArchivos().abortar(p);        // deja consistente el sistema de archivos
        if (usaSwap) computador.getSwap().liberar(p.getId());
        p.setSuspendiendo(false);
        p.setReactivando(false);

        // Devuelve los semáforos que tenía o en los que esperaba (Proyecto 1), para no dejar un buffer interbloqueado
        p.sacarDeSemaforoBloqueante();
        SemaforoSO[] tenidos = p.tomarTenidos();
        for (SemaforoSO sem : tenidos) computador.senalar(sem, p.getBuffer());
        p.getPrograma().alTerminarAbruptamente(p, computador);

        p.setEstado(EstadoProceso.TERMINADO);
        p.setCausaFin(CausaFin.DEADLINE);
        p.setCicloFin(cicloActual);
        if (enRam) ram.liberar(p.getMemoria());
        terminados.agregarAlFinal(p);
        vencidos++;
        computador.registrar("Proceso " + p.etiqueta() + " (" + p.getNombre()
                + ") alcanzó su deadline y es terminado por " + computador.getNombre());
        admitirNuevos();
    }

    // ------------------------------------------------------------------ consultas para la GUI / métricas

    public int carga() {
        entrar();
        try { return nuevos.tamano() + listos.tamano() + bloqueados.tamano() + listosSusp.tamano() + bloqSusp.tamano() + (cpu.getActual() != null ? 1 : 0); }
        finally { salir(); }
    }

    public SerieDatos getSerieUtilizacion() { return serieUtilizacion; }

    public int getSuspensiones() { entrar(); try { return suspensiones; } finally { salir(); } }
    public int getReactivaciones() { entrar(); try { return reactivaciones; } finally { salir(); } }

    public MetricasComputador metricas() {
        entrar();
        try {
            MetricasComputador m = new MetricasComputador();
            m.ciclos = cpu.getCiclosTotales();
            m.ciclosReloj = m.ciclos;
            m.ocupados = cpu.getCiclosOcupada();
            m.completados = completados;
            m.vencidos = vencidos;
            for (int i = 0; i < todos.tamano(); i++) {
                Proceso p = todos.obtener(i);
                if (p.getCicloPrimeraCPU() >= 0) {
                    m.respuestaSuma += p.getCicloPrimeraCPU() - p.getCicloCreacion();
                    m.respuestaN++;
                }
                long vida = (p.getCicloFin() >= 0 ? p.getCicloFin() : cicloActual) - p.getCicloCreacion();
                if (vida > 0) {
                    double x = Math.min(1.0, (double) p.getCiclosCPU() / vida);
                    m.ratioSuma += x; m.ratioSuma2 += x * x; m.ratioN++;
                }
            }
            return m;
        } finally { salir(); }
    }

    public EstadoComputador instantanea() {
        entrar();
        try {
            EstadoComputador e = new EstadoComputador();
            e.nombre = computador.getNombre();
            e.politica = politica.nombre();
            e.tipoPolitica = politica.tipo();
            Proceso p = cpu.getActual();
            e.enEjecucionSO = cpu.getModo() == avilaos.modelo.ModoCPU.SISTEMA_OPERATIVO;
            e.modo = p == null ? "Sistema operativo (CPU ociosa)"
                    : (e.enEjecucionSO ? "SISTEMA OPERATIVO" : "PROGRAMA DE USUARIO");
            if (p != null) {
                e.proceso = p.etiqueta() + " (" + p.getNombre() + ")";
                e.pc = String.valueOf(p.getPc());
                e.instruccion = p.descripcionInstruccion();
                e.prioridad = String.valueOf(p.getPrioridad());
                e.deadline = String.valueOf(p.getDeadline());
            }
            e.ramTotal = ram.getTotal();
            e.ramUsada = ram.getUsada();
            e.nuevos = textoCola(nuevos, false);
            e.listos = textoCola(listos, false);
            e.bloqueados = textoCola(bloqueados, true);
            e.terminados = textoCola(terminados, false);
            e.listosSuspendidos = textoCola(listosSusp, false);
            e.bloqueadosSuspendidos = textoCola(bloqSusp, true);
            e.suspensiones = suspensiones;
            e.reactivaciones = reactivaciones;
            e.swapTotal = computador.getSwap().getTotal();
            e.swapUsado = e.swapTotal - computador.getSwap().getLibres();

            int n = todos.tamano();
            e.pcb = new String[n][];
            for (int i = 0; i < n; i++) {
                Proceso q = todos.obtener(i);
                String cola = (q.getEstado() == EstadoProceso.BLOQUEADO || q.getEstado() == EstadoProceso.BLOQUEADO_SUSPENDIDO)
                        ? q.getEstado().getTexto() + " (" + q.getMotivo().getTexto() + ")"
                        : q.getEstado().toString() + (q.getEstado() == EstadoProceso.TERMINADO && q.getCausaFin() == CausaFin.DEADLINE ? " (deadline)" : "");
                e.pcb[i] = new String[] { q.etiqueta(), q.getNombre(), cola, q.getTipo().getTexto(),
                        String.valueOf(q.getPrioridad()), String.valueOf(q.getMemoria()),
                        String.valueOf(q.getDeadline()), String.valueOf(q.getTiempoRestante()),
                        String.valueOf(q.getPc()),
                        q.getOperacion() == null ? "-" : q.getOperacion().getTexto(),
                        q.getRutaArchivo() == null || q.getRutaArchivo().isEmpty() ? "-" : q.getRutaArchivo() };
            }
            return e;
        } finally { salir(); }
    }

    private String[] textoCola(ListaEnlazada<Proceso> cola, boolean conMotivo) {
        String[] r = new String[cola.tamano()];
        for (int i = 0; i < r.length; i++) {
            Proceso p = cola.obtener(i);
            r[i] = p.etiqueta() + " " + p.getNombre()
                    + (conMotivo ? "  ⟶ " + p.getMotivo().getTexto() + ": " + p.getDetalleBloqueo() : "");
        }
        return r;
    }
}
