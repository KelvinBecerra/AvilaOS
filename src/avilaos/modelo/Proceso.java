package avilaos.modelo;

import avilaos.so.Computador;
import avilaos.sync.BufferAcotado;
import avilaos.sync.SemaforoSO;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * PCB de ÁvilaOS. Cada proceso tiene un único hilo de ejecución (modelo monohilo), por eso
 * PCB y TCB se asocian 1:1 y aquí se integran en una sola clase.
 */
public class Proceso {
    private static final AtomicInteger contadorId = new AtomicInteger(0);
    public static void reiniciarContador() { contadorId.set(0); }

    // --- Identidad (inmutable) ---
    private final int id;                 // único en todo el sistema distribuido
    private final String nombre;
    private final TipoProceso tipo;
    private final int prioridad;          // 1 = más prioritario
    private final int memoria;
    private final int instruccionesTotales;
    private final int deadlineInicial;
    private final Computador computador;
    private final Programa programa;
    private final BufferAcotado buffer;   // solo productor/consumidor

    // --- Estado dinámico ---
    private volatile EstadoProceso estado = EstadoProceso.NUEVO;
    private volatile MotivoBloqueo motivo = MotivoBloqueo.NINGUNO;
    private volatile String detalleBloqueo = "";
    private volatile int contadorBloqueo;
    private volatile int deadline;
    private volatile int tiempoRestante;
    private volatile int pc, mar;
    private volatile long secuenciaListo;
    private volatile long cicloCreacion, cicloPrimeraCPU = -1, cicloFin = -1;
    private volatile int ciclosCPU, ciclosBloqSem;
    private volatile CausaFin causaFin;
    private volatile boolean redPagada;
    private volatile SemaforoSO semBloq;

    // Semáforos cuyo permiso retiene actualmente (para liberarlos si el proceso muere por deadline)
    private final SemaforoSO[] tenidos = new SemaforoSO[4];
    private int nTenidos;
    private final Semaphore mutexTenidos = new Semaphore(1);

    public Proceso(String nombre, TipoProceso tipo, int prioridad, int memoria, int instrucciones,
                   int deadline, Computador computador, Programa programa, BufferAcotado buffer, long cicloCreacion) {
        this.id = contadorId.incrementAndGet();
        this.nombre = nombre;
        this.tipo = tipo;
        this.prioridad = prioridad;
        this.memoria = memoria;
        this.instruccionesTotales = instrucciones;
        this.deadlineInicial = deadline;
        this.deadline = deadline;
        this.tiempoRestante = instrucciones;
        this.computador = computador;
        this.programa = programa;
        this.buffer = buffer;
        this.cicloCreacion = cicloCreacion;
    }

    // ---------- Operaciones sobre el PCB ----------
    public void bloquear(MotivoBloqueo m, int ciclos, String detalle) {
        this.motivo = m;
        this.contadorBloqueo = ciclos;
        this.detalleBloqueo = detalle;
    }

    public void limpiarBloqueo() {
        motivo = MotivoBloqueo.NINGUNO;
        detalleBloqueo = "";
        contadorBloqueo = 0;
    }

    /** Un ciclo de CPU consumido: el PC y el MAR avanzan una unidad. */
    public void registrarCicloCPU() {
        pc++;
        mar++;
        ciclosCPU++;
        if (tiempoRestante > 0) tiempoRestante--;
    }

    /** @return true si el contador llegó a 0 */
    public boolean decrementarContadorBloqueo() {
        if (contadorBloqueo > 0) contadorBloqueo--;
        return contadorBloqueo <= 0;
    }

    public void decrementarDeadline() { if (deadline > 0) deadline--; }
    public void sumarBloqSem() { ciclosBloqSem++; }

    // ---------- Control de semáforos retenidos ----------
    public void alBloquearseEn(SemaforoSO s) { semBloq = s; }

    public void alDespertar(SemaforoSO s) {
        semBloq = null;
        agregarTenido(s);
    }

    public void agregarTenido(SemaforoSO s) {
        mutexTenidos.acquireUninterruptibly();
        try { if (nTenidos < tenidos.length) tenidos[nTenidos++] = s; } finally { mutexTenidos.release(); }
    }

    public void quitarTenido(SemaforoSO s) {
        mutexTenidos.acquireUninterruptibly();
        try {
            for (int i = 0; i < nTenidos; i++) {
                if (tenidos[i] == s) {
                    for (int j = i; j < nTenidos - 1; j++) tenidos[j] = tenidos[j + 1];
                    tenidos[--nTenidos] = null;
                    return;
                }
            }
        } finally { mutexTenidos.release(); }
    }

    /** Devuelve y vacía los semáforos retenidos (el más reciente primero). */
    public SemaforoSO[] tomarTenidos() {
        mutexTenidos.acquireUninterruptibly();
        try {
            SemaforoSO[] r = new SemaforoSO[nTenidos];
            for (int i = 0; i < nTenidos; i++) r[i] = tenidos[nTenidos - 1 - i];
            for (int i = 0; i < nTenidos; i++) tenidos[i] = null;
            nTenidos = 0;
            return r;
        } finally { mutexTenidos.release(); }
    }

    /** Saca al proceso de la cola de semáforo en la que esté (si estaba) y devuelve su semáforo. */
    public SemaforoSO sacarDeSemaforoBloqueante() {
        SemaforoSO s = semBloq;
        if (s != null) { s.quitarDeCola(this); semBloq = null; }
        return s;
    }

    // ---------- Consultas ----------
    public String etiqueta() { return "P" + id; }
    public String descripcionInstruccion() { return programa.descripcionProximaInstruccion(this); }

    public String resumenPCB() {
        return etiqueta() + " | " + nombre + " | " + computador.getNombre() + " | " + estado + " | " + tipo
                + " | prio " + prioridad + " | mem " + memoria + " | deadline " + deadline
                + " | restante " + tiempoRestante;
    }

    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public TipoProceso getTipo() { return tipo; }
    public int getPrioridad() { return prioridad; }
    public int getMemoria() { return memoria; }
    public int getInstruccionesTotales() { return instruccionesTotales; }
    public int getDeadlineInicial() { return deadlineInicial; }
    public Computador getComputador() { return computador; }
    public Programa getPrograma() { return programa; }
    public BufferAcotado getBuffer() { return buffer; }
    public EstadoProceso getEstado() { return estado; }
    public void setEstado(EstadoProceso e) { this.estado = e; }
    public MotivoBloqueo getMotivo() { return motivo; }
    public String getDetalleBloqueo() { return detalleBloqueo; }
    public int getDeadline() { return deadline; }
    public int getTiempoRestante() { return tiempoRestante; }
    public int getPc() { return pc; }
    public int getMar() { return mar; }
    public long getSecuenciaListo() { return secuenciaListo; }
    public void setSecuenciaListo(long s) { this.secuenciaListo = s; }
    public long getCicloCreacion() { return cicloCreacion; }
    public long getCicloPrimeraCPU() { return cicloPrimeraCPU; }
    public void setCicloPrimeraCPU(long c) { this.cicloPrimeraCPU = c; }
    public long getCicloFin() { return cicloFin; }
    public void setCicloFin(long c) { this.cicloFin = c; }
    public int getCiclosCPU() { return ciclosCPU; }
    public int getCiclosBloqSem() { return ciclosBloqSem; }
    public CausaFin getCausaFin() { return causaFin; }
    public void setCausaFin(CausaFin c) { this.causaFin = c; }
    public boolean isRedPagada() { return redPagada; }
    public void setRedPagada(boolean b) { this.redPagada = b; }
}
