package avilaos.so;

import avilaos.hardware.CPU;
import avilaos.archivos.AreaSwap;
import avilaos.archivos.SistemaArchivos;
import avilaos.hardware.ControladorES;
import avilaos.hardware.Disco;
import avilaos.modelo.SolicitudDisco;
import avilaos.hardware.Memoria;
import avilaos.hardware.Reloj;
import avilaos.modelo.Clockable;
import avilaos.modelo.Proceso;
import avilaos.sistema.ConfigSimulacion;
import avilaos.sistema.Registro;
import avilaos.sync.BufferAcotado;
import avilaos.sync.SemaforoSO;
import java.util.concurrent.Semaphore;

/**
 * Un computador del clúster: la unidad reutilizable donde se ensamblan hardware (CPU, RAM,
 * controlador de E/S) y sistema operativo. TODOS los computadores son instancias de esta clase.
 * Cada uno corre en su propio hilo de Java, sincronizado con el reloj global.
 */
public class Computador {
    private final int id;
    private final String nombre;
    private final Memoria ram;
    private final CPU cpu;
    private final ControladorES controladorES;
    private final NucleoSO so;
    private final Disco disco;
    private final SistemaArchivos fs;
    private final AreaSwap swap;
    private final ServicioArchivos servicioArchivos;
    private final Clockable[] componentes;
    private final ConfigSimulacion config;
    private final Registro registro;
    private final Reloj reloj;

    private final Semaphore inicioCiclo = new Semaphore(0);
    private volatile boolean activo;
    private volatile long ciclo;
    private Thread hilo;

    public Computador(int id, int ramTotal, ConfigSimulacion config, Registro registro, Reloj reloj) {
        this.id = id;
        this.nombre = "Computador " + (id + 1);
        this.config = config;
        this.registro = registro;
        this.reloj = reloj;
        this.ram = new Memoria(ramTotal);
        this.so = new NucleoSO(this, ram);
        this.cpu = new CPU(this, so);
        this.controladorES = new ControladorES(so);
        this.so.conectar(cpu, controladorES);
        // Disco local único, sistema de archivos sobre él y área de intercambio
        this.disco = new Disco(config.getTotalBloques(), config.getCabezalInicial(), config.getVelocidadCabezal(), config.getPoliticaDisco());
        this.fs = new SistemaArchivos(config.getTotalBloques());
        this.swap = new AreaSwap(config.getBloquesSwap());
        this.servicioArchivos = new ServicioArchivos(this);
        this.disco.setEscucha(new Disco.EscuchaDisco() {
            @Override public void solicitudIniciada(SolicitudDisco s, long ciclo) {
                registrar("El disco atiende la solicitud de " + (s.getProceso() != null ? s.getProceso().etiqueta() : "SO") + " ("
                        + s.getOperacion().getTexto() + ") en el bloque " + s.getBloque() + " (" + s.getPoliticaUsada() + ")");
            }
            @Override public void solicitudAtendida(SolicitudDisco s, long ciclo) { so.interrupcionDisco(s); }
        });
        // Orden de reacción al reloj: controlador de E/S, disco (interrupciones) y por último la CPU
        this.componentes = new Clockable[] { controladorES, disco, cpu };
    }

    // ---------------------------------------------------------------- hilo del computador

    public void arrancar() {
        if (hilo != null) return;
        activo = true;
        hilo = new Thread(this::bucle, "Hilo-" + nombre);
        hilo.setDaemon(true);
        hilo.start();
    }

    public void detener() {
        activo = false;
        inicioCiclo.release();
    }

    /** El reloj global ordena avanzar un ciclo. */
    public void iniciarCiclo(long c) {
        this.ciclo = c;
        inicioCiclo.release();
    }

    private void bucle() {
        while (activo) {
            inicioCiclo.acquireUninterruptibly();
            if (!activo) break;
            try {
                ejecutarCiclo(ciclo);
            } catch (Throwable t) {
                t.printStackTrace();
                registrar("ERROR interno en " + nombre + ": " + t);
            } finally {
                reloj.cicloTerminado();
            }
        }
    }

    private void ejecutarCiclo(long c) {
        so.entrar();
        try {
            so.antesDelCiclo(c);
            for (Clockable comp : componentes) comp.tick(c);
            so.despuesDelCiclo(c);
        } finally {
            so.salir();
        }
    }

    // ---------------------------------------------------------------- servicios para los programas

    public void registrar(String mensaje) { registro.registrar(ciclo, mensaje); }

    /**
     * semSignal sobre un semáforo: si despierta a un proceso, se le avisa a SU computador
     * (puede ser otro) por la bandeja de entrada; nunca se toma el lock de otro SO.
     */
    public void senalar(SemaforoSO sem, BufferAcotado buffer) {
        Proceso w = sem.semSignal();
        if (w != null) {
            registrar("semSignal('" + sem.getNombre() + "'" + (buffer != null ? " de " + buffer.getNombre() : "")
                    + ") despierta a " + w.etiqueta());
            w.getComputador().getNucleo().recibirDesbloqueado(w);
        }
    }

    /** Reserva RAM para un buffer residente en este computador. */
    public boolean reservarMemoria(int cantidad) { return ram.reservar(cantidad); }

    public Disco getDisco() { return disco; }
    public SistemaArchivos getFs() { return fs; }
    public AreaSwap getSwap() { return swap; }
    public ServicioArchivos getServicioArchivos() { return servicioArchivos; }
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public Memoria getRam() { return ram; }
    public CPU getCpu() { return cpu; }
    public NucleoSO getNucleo() { return so; }
    public ConfigSimulacion getConfig() { return config; }
    public long getCiclo() { return ciclo; }
}
