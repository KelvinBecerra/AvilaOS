package avilaos.sistema;

import avilaos.estructuras.ListaEnlazada;
import avilaos.hardware.Reloj;
import avilaos.modelo.PoliticaPlanificacion;
import avilaos.modelo.Programa;
import avilaos.modelo.ProgramaBuffer;
import avilaos.modelo.ProgramaCPU;
import avilaos.modelo.ProgramaES;
import avilaos.modelo.Proceso;
import avilaos.modelo.TipoProceso;
import avilaos.politicas.FabricaPoliticas;
import avilaos.so.Computador;
import avilaos.so.MetricasComputador;
import avilaos.sync.BufferAcotado;
import java.util.concurrent.Semaphore;

/**
 * El sistema distribuido completo: reloj global + N computadores (todas instancias de
 * Computador) + buffers compartidos. Agregar un computador = una instancia más.
 */
public class Cluster {
    private final ConfigSimulacion config;
    private final Registro registro = new Registro();
    private final Reloj reloj;
    private final Computador[] computadores;
    private final ListaEnlazada<BufferAcotado> buffers = new ListaEnlazada<>();
    private final PerfilSimulacion perfil = new PerfilSimulacion();
    private final Semaphore mutexCreacion = new Semaphore(1);

    public Cluster(ConfigSimulacion config) {
        Proceso.reiniciarContador();
        this.config = config;
        this.perfil.config = config;
        this.reloj = new Reloj(config);
        int n = config.getNumComputadores();
        this.computadores = new Computador[n];
        for (int i = 0; i < n; i++) {
            computadores[i] = new Computador(i, config.getRam(i), config, registro, reloj);
            computadores[i].getNucleo().setPolitica(FabricaPoliticas.crear(config.getPolitica(i), config));
        }
        reloj.setComputadores(computadores);
        reloj.iniciar();           // los hilos arrancan, pero el reloj empieza en pausa
    }

    // ------------------------------------------------------------------ creación de buffers y procesos

    public BufferAcotado crearBuffer(String nombre, int capacidad, int hostIdx) {
        mutexCreacion.acquireUninterruptibly();
        try {
            if (nombre == null || nombre.trim().isEmpty() || nombre.contains(","))
                throw new IllegalArgumentException("El nombre del buffer no puede estar vacío ni contener comas");
            if (buscarBuffer(nombre) != null)
                throw new IllegalArgumentException("Ya existe un buffer llamado '" + nombre + "'");
            if (capacidad < 1 || capacidad > 1000)
                throw new IllegalArgumentException("La capacidad debe estar entre 1 y 1000");
            if (hostIdx < 0 || hostIdx >= computadores.length)
                throw new IllegalArgumentException("Computador anfitrión inválido");
            Computador host = computadores[hostIdx];
            if (!host.reservarMemoria(capacidad))
                throw new IllegalArgumentException("No hay RAM suficiente en " + host.getNombre()
                        + " para un buffer de capacidad " + capacidad + " (cada posición ocupa 1 unidad de RAM)");
            BufferAcotado b = new BufferAcotado(nombre.trim(), capacidad, host);
            buffers.agregarAlFinal(b);
            perfil.buffers.agregarAlFinal(new EspecBuffer(nombre.trim(), capacidad, hostIdx));
            host.registrar("Buffer " + b.getNombre() + " creado en " + host.getNombre() + " (capacidad " + capacidad + ")");
            return b;
        } finally { mutexCreacion.release(); }
    }

    public Proceso crearProceso(EspecProceso e) {
        mutexCreacion.acquireUninterruptibly();
        try {
            validar(e);
            Computador destino = e.computador >= 0 ? computadores[e.computador] : elegirAutomatico(e.memoria);
            if (e.memoria > destino.getRam().getTotal())
                throw new IllegalArgumentException("El proceso necesita " + e.memoria + " de RAM pero "
                        + destino.getNombre() + " solo tiene " + destino.getRam().getTotal() + " en total");

            BufferAcotado buf = null;
            Programa programa;
            int instrucciones = e.instrucciones;
            switch (e.tipo) {
                case CPU_BOUND:
                    programa = new ProgramaCPU(e.instrucciones);
                    break;
                case IO_BOUND:
                    programa = new ProgramaES(e.instrucciones, e.periodoES, e.ciclosES);
                    break;
                default:
                    buf = buscarBuffer(e.buffer);
                    buf.registrarUsuario();
                    instrucciones = ProgramaBuffer.instruccionesEstimadas(e.periodo, e.elementos);
                    programa = new ProgramaBuffer(e.tipo == TipoProceso.PRODUCTOR, buf, e.periodo, e.elementos);
            }
            Proceso p = new Proceso(e.nombre.trim(), e.tipo, e.prioridad, e.memoria, instrucciones,
                    e.deadline, destino, programa, buf, reloj.getCiclo());
            destino.getNucleo().recibirNuevo(p);

            EspecProceso guardada = new EspecProceso();
            guardada.nombre = e.nombre.trim(); guardada.tipo = e.tipo; guardada.instrucciones = e.instrucciones;
            guardada.memoria = e.memoria; guardada.prioridad = e.prioridad; guardada.deadline = e.deadline;
            guardada.computador = e.computador; guardada.buffer = e.buffer; guardada.periodo = e.periodo;
            guardada.elementos = e.elementos; guardada.ciclosES = e.ciclosES; guardada.periodoES = e.periodoES;
            perfil.procesos.agregarAlFinal(guardada);
            return p;
        } finally { mutexCreacion.release(); }
    }

    private void validar(EspecProceso e) {
        if (e.nombre == null || e.nombre.trim().isEmpty() || e.nombre.contains(","))
            throw new IllegalArgumentException("El nombre del proceso no puede estar vacío ni contener comas");
        if (e.tipo == null) throw new IllegalArgumentException("Falta el tipo de proceso");
        if (e.memoria < 1) throw new IllegalArgumentException("La memoria debe ser al menos 1");
        if (e.prioridad < 1 || e.prioridad > 10) throw new IllegalArgumentException("La prioridad debe estar entre 1 (máxima) y 10");
        if (e.deadline < 1) throw new IllegalArgumentException("El deadline debe ser al menos 1 ciclo");
        if (e.computador >= computadores.length) throw new IllegalArgumentException("Computador inválido");
        if (e.tipo == TipoProceso.CPU_BOUND || e.tipo == TipoProceso.IO_BOUND) {
            if (e.instrucciones < 1) throw new IllegalArgumentException("Las instrucciones deben ser al menos 1");
        }
        if (e.tipo == TipoProceso.IO_BOUND) {
            if (e.periodoES < 1) throw new IllegalArgumentException("'Cada cuántas instrucciones pide E/S' debe ser al menos 1");
            if (e.ciclosES < 1) throw new IllegalArgumentException("Los ciclos de E/S deben ser al menos 1");
        }
        if (e.tipo.esDeBuffer()) {
            if (buscarBuffer(e.buffer) == null) throw new IllegalArgumentException("El buffer '" + e.buffer + "' no existe: créelo primero");
            if (e.periodo < 1) throw new IllegalArgumentException("El periodo (ciclos por elemento) debe ser al menos 1");
            if (e.elementos < 1) throw new IllegalArgumentException("Los elementos a procesar deben ser al menos 1");
        }
    }

    /**
     * Balanceo de carga automático (criterio propio): se elige el computador cuya RAM total
     * alcanza para el proceso y que tenga MENOS procesos activos (nuevos + listos + bloqueados + en CPU);
     * en empate, el que tenga más RAM libre.
     */
    private Computador elegirAutomatico(int memoria) {
        Computador mejor = null;
        int mejorCarga = Integer.MAX_VALUE;
        for (Computador c : computadores) {
            if (c.getRam().getTotal() < memoria) continue;
            int carga = c.getNucleo().carga();
            if (mejor == null || carga < mejorCarga || (carga == mejorCarga && c.getRam().getLibre() > mejor.getRam().getLibre())) {
                mejor = c;
                mejorCarga = carga;
            }
        }
        return mejor != null ? mejor : computadores[0];
    }

    // ------------------------------------------------------------------ control

    public void reanudar() { reloj.setPausado(false); }
    public void pausar() { reloj.setPausado(true); }
    public void paso() { reloj.paso(); }
    public boolean isPausado() { return reloj.isPausado(); }
    public void detener() { reloj.detener(); }

    public void cambiarPolitica(int computadorIdx, avilaos.modelo.TipoPolitica tipo) {
        PoliticaPlanificacion p = FabricaPoliticas.crear(tipo, config);
        computadores[computadorIdx].getNucleo().setPolitica(p);
    }

    // ------------------------------------------------------------------ consultas

    public BufferAcotado buscarBuffer(String nombre) {
        if (nombre == null) return null;
        for (int i = 0; i < buffers.tamano(); i++)
            if (buffers.obtener(i).getNombre().equals(nombre.trim())) return buffers.obtener(i);
        return null;
    }

    public ListaEnlazada<BufferAcotado> copiarBuffers() {
        mutexCreacion.acquireUninterruptibly();
        try { return buffers.copiar(); } finally { mutexCreacion.release(); }
    }

    public MetricasComputador[] metricasPorComputador() {
        MetricasComputador[] m = new MetricasComputador[computadores.length];
        for (int i = 0; i < m.length; i++) m[i] = computadores[i].getNucleo().metricas();
        return m;
    }

    public PerfilSimulacion getPerfil() { return perfil; }
    public ConfigSimulacion getConfig() { return config; }
    public Registro getRegistro() { return registro; }
    public Computador[] getComputadores() { return computadores; }
    public Computador getComputador(int i) { return computadores[i]; }
    public long getCiclo() { return reloj.getCiclo(); }
}
