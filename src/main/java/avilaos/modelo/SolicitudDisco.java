package avilaos.modelo;

import java.util.concurrent.atomic.AtomicLong;

/** Solicitud de E/S al disco: la genera un proceso (o el planificador a mediano plazo) y espera en la cola del disco. */
public class SolicitudDisco {
    private static final AtomicLong contador = new AtomicLong(0);
    public static void reiniciarContador() { contador.set(0); }

    private final long id = contador.incrementAndGet();
    private final int bloque;                 // posición del disco a la que se dirige (primer bloque)
    private final TipoOperacionES operacion;
    private final Proceso proceso;            // puede ser null en pruebas aisladas del disco
    private final String ruta;                // archivo o descripción, para mostrar
    private final int bloquesAcceso;          // bloques a leer/escribir: cada uno cuesta un ciclo

    private volatile long cicloLlegada = -1, cicloInicio = -1, cicloFin = -1;
    private volatile TipoPoliticaDisco politicaUsada;
    private volatile int[] cadena;                // todos los bloques a recorrer (asignación encadenada)
    private volatile boolean cancelada;

    public SolicitudDisco(int bloque, TipoOperacionES operacion, Proceso proceso, String ruta, int bloquesAcceso) {
        this.bloque = bloque;
        this.operacion = operacion;
        this.proceso = proceso;
        this.ruta = ruta;
        this.bloquesAcceso = Math.max(1, bloquesAcceso);
    }

    public long getId() { return id; }
    public int getBloque() { return bloque; }
    public TipoOperacionES getOperacion() { return operacion; }
    public Proceso getProceso() { return proceso; }
    public String getRuta() { return ruta; }
    public int getBloquesAcceso() { return bloquesAcceso; }
    public long getCicloLlegada() { return cicloLlegada; }
    public void setCicloLlegada(long c) { cicloLlegada = c; }
    public long getCicloInicio() { return cicloInicio; }
    public void setCicloInicio(long c) { cicloInicio = c; }
    public long getCicloFin() { return cicloFin; }
    public void setCicloFin(long c) { cicloFin = c; }
    public TipoPoliticaDisco getPoliticaUsada() { return politicaUsada; }
    public void setPoliticaUsada(TipoPoliticaDisco p) { politicaUsada = p; }

    public int[] getCadena() { return cadena; }
    public void setCadena(int[] c) { cadena = c; }
    public boolean isCancelada() { return cancelada; }
    public void setCancelada(boolean b) { cancelada = b; }

    public String etiqueta() {
        return (proceso != null ? proceso.etiqueta() : "SO") + " " + operacion.getTexto() + " @" + bloque;
    }
}
