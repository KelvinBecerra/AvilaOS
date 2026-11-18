package avilaos.hardware;

import avilaos.estructuras.ListaEnlazada;
import avilaos.estructuras.SerieDatos;
import avilaos.modelo.Clockable;
import avilaos.modelo.DireccionCabezal;
import avilaos.modelo.PoliticaDisco;
import avilaos.modelo.SeleccionDisco;
import avilaos.modelo.SolicitudDisco;
import avilaos.modelo.TipoPoliticaDisco;
import avilaos.politicas.FabricaPoliticasDisco;
import java.util.concurrent.Semaphore;

/**
 * Disco simulado (hardware: obedece al reloj, no decide por sí mismo el orden: lo decide la PoliticaDisco).
 * Atiende UNA solicitud a la vez. Cada ciclo el cabezal recorre hasta 'velocidad' posiciones; cada bloque leído o
 * escrito cuesta un ciclo. Una única cola de E/S protegida por un semáforo de Java.
 */
public class Disco implements Clockable {

    /** Se invoca (con el semáforo del disco ya liberado) al terminar de atender una solicitud. */
    public interface EscuchaDisco {
        void solicitudAtendida(SolicitudDisco solicitud, long ciclo);

        /** El disco empieza a atender la solicitud (decisión de la política). No debe volver a llamar al disco. */
        default void solicitudIniciada(SolicitudDisco solicitud, long ciclo) { }
    }

    private enum Fase { OCIOSO, MOVIENDO, ACCEDIENDO }

    private final int totalBloques;
    private volatile int velocidad;
    private final Semaphore mutex = new Semaphore(1);
    private final ListaEnlazada<SolicitudDisco> cola = new ListaEnlazada<>();
    private volatile PoliticaDisco politica;
    private EscuchaDisco escucha;

    private volatile int cabezal;
    private volatile DireccionCabezal direccion = DireccionCabezal.ASCENDENTE;

    private Fase fase = Fase.OCIOSO;
    private volatile SolicitudDisco actual;
    private int[] ruta;
    private int saltoEn, idxRuta, ciclosAcceso;

    // métricas
    private long desplazamientoTotal, sumaEspera, nAtendidas;
    private final long[] atendidasPorPolitica = new long[TipoPoliticaDisco.values().length];
    private final SerieDatos serieDesplazamiento = new SerieDatos();
    private final SerieDatos seriePolitica = new SerieDatos();
    private volatile boolean retenido;

    public Disco(int totalBloques, int cabezalInicial, int velocidad, TipoPoliticaDisco inicial) {
        if (totalBloques < 2) throw new IllegalArgumentException("El disco necesita al menos 2 bloques");
        if (cabezalInicial < 0 || cabezalInicial >= totalBloques) throw new IllegalArgumentException("Cabezal inicial fuera del disco");
        this.totalBloques = totalBloques;
        this.cabezal = cabezalInicial;
        this.velocidad = Math.max(1, velocidad);
        this.politica = FabricaPoliticasDisco.crear(inicial);
    }

    public void setEscucha(EscuchaDisco e) { this.escucha = e; }

    /** Pone una solicitud en la única cola de E/S del disco. */
    public void encolar(SolicitudDisco s, long ciclo) {
        if (s.getBloque() < 0 || s.getBloque() >= totalBloques)
            throw new IllegalArgumentException("Bloque " + s.getBloque() + " fuera del disco (0 a " + (totalBloques - 1) + ")");
        mutex.acquireUninterruptibly();
        try {
            s.setCicloLlegada(ciclo);
            cola.agregarAlFinal(s);
        } finally { mutex.release(); }
    }

    /** Política cambiable en caliente. */
    public void setPolitica(TipoPoliticaDisco tipo) {
        mutex.acquireUninterruptibly();
        try { this.politica = FabricaPoliticasDisco.crear(tipo); } finally { mutex.release(); }
    }

    public void setVelocidad(int v) { this.velocidad = Math.max(1, v); }
    public int getVelocidad() { return velocidad; }

    /** Mientras esté retenido el disco acumula solicitudes y no empieza a atender (útil para demostrar las políticas). */
    public void setRetenido(boolean r) { this.retenido = r; }
    public boolean isRetenido() { return retenido; }

    /** Saca de la cola las solicitudes de un proceso (por ejemplo, si murió por deadline). */
    public int cancelarDe(avilaos.modelo.Proceso p) {
        mutex.acquireUninterruptibly();
        try {
            int n = 0;
            ListaEnlazada<SolicitudDisco> copia = cola.copiar();
            for (int i = 0; i < copia.tamano(); i++) {
                if (copia.obtener(i).getProceso() == p) { cola.quitar(copia.obtener(i)); n++; }
            }
            if (actual != null && actual.getProceso() == p) actual.setCancelada(true);
            return n;
        } finally { mutex.release(); }
    }

    @Override public void tick(long ciclo) {
        SolicitudDisco terminada = null;
        mutex.acquireUninterruptibly();
        try {
            if (fase == Fase.OCIOSO && !retenido) iniciarSiguiente(ciclo);
            if (fase == Fase.MOVIENDO) {
                int movido = mover();
                if (idxRuta >= ruta.length) fase = Fase.ACCEDIENDO;
                if (fase == Fase.ACCEDIENDO && movido > 0) { /* el acceso empieza en el próximo ciclo */ }
                else if (fase == Fase.ACCEDIENDO) terminada = acceder(ciclo);
            } else if (fase == Fase.ACCEDIENDO) {
                terminada = acceder(ciclo);
            }
            serieDesplazamiento.agregar(desplazamientoTotal);
            seriePolitica.agregar(politica.tipo().ordinal());
        } finally { mutex.release(); }
        if (terminada != null && escucha != null) escucha.solicitudAtendida(terminada, ciclo);   // fuera del lock
    }

    private void iniciarSiguiente(long ciclo) {
        SeleccionDisco sel = politica.seleccionar(cola, cabezal, direccion, totalBloques);
        if (sel == null) return;
        cola.quitar(sel.solicitud);
        actual = sel.solicitud;
        actual.setCicloInicio(ciclo);
        actual.setPoliticaUsada(politica.tipo());
        sumaEspera += ciclo - actual.getCicloLlegada();
        ruta = sel.ruta;
        saltoEn = sel.saltoEn;
        idxRuta = 0;
        ciclosAcceso = actual.getBloquesAcceso();
        fase = Fase.MOVIENDO;
        if (escucha != null) escucha.solicitudIniciada(actual, ciclo);
    }

    /** Mueve el cabezal hasta 'velocidad' posiciones a lo largo de la ruta. @return posiciones recorridas */
    private int mover() {
        int restante = velocidad, movido = 0;
        while (idxRuta < ruta.length && restante > 0) {
            int objetivo = ruta[idxRuta];
            int dist = Math.abs(objetivo - cabezal);
            int paso = Math.min(dist, restante);
            if (paso > 0) {
                direccion = objetivo > cabezal ? DireccionCabezal.ASCENDENTE : DireccionCabezal.DESCENDENTE;
                cabezal += objetivo > cabezal ? paso : -paso;
                desplazamientoTotal += paso;
                restante -= paso;
                movido += paso;
            }
            if (cabezal == objetivo) {
                if (idxRuta == saltoEn) { cabezal = ruta[idxRuta + 1]; idxRuta += 2; }   // salto circular sin costo
                else idxRuta++;
            }
        }
        return movido;
    }

    private SolicitudDisco acceder(long ciclo) {
        ciclosAcceso--;
        if (ciclosAcceso > 0) return null;
        SolicitudDisco s = actual;
        int[] cad = s.getCadena();
        if (cad != null && cad.length > 1) {           // recorre el resto de la cadena de bloques del archivo
            for (int k = 1; k < cad.length; k++) {
                int d = Math.abs(cad[k] - cad[k - 1]);
                desplazamientoTotal += d;
                if (d > 0) direccion = cad[k] > cad[k - 1] ? DireccionCabezal.ASCENDENTE : DireccionCabezal.DESCENDENTE;
            }
            cabezal = cad[cad.length - 1];
        }
        s.setCicloFin(ciclo);
        atendidasPorPolitica[s.getPoliticaUsada().ordinal()]++;
        nAtendidas++;
        actual = null;
        fase = Fase.OCIOSO;
        return s;
    }

    // ------------------------------------------------------------ consultas (GUI / métricas)
    public int getTotalBloques() { return totalBloques; }
    public int getCabezal() { return cabezal; }
    public DireccionCabezal getDireccion() { return direccion; }
    public SolicitudDisco getActual() { return actual; }
    public PoliticaDisco getPolitica() { return politica; }
    public SerieDatos getSerieDesplazamiento() { return serieDesplazamiento; }
    public SerieDatos getSeriePolitica() { return seriePolitica; }

    public ListaEnlazada<SolicitudDisco> copiarCola() {
        mutex.acquireUninterruptibly();
        try { return cola.copiar(); } finally { mutex.release(); }
    }

    public long getDesplazamientoTotal() {
        mutex.acquireUninterruptibly();
        try { return desplazamientoTotal; } finally { mutex.release(); }
    }
    public double getEsperaPromedio() {
        mutex.acquireUninterruptibly();
        try { return nAtendidas == 0 ? 0 : (double) sumaEspera / nAtendidas; } finally { mutex.release(); }
    }
    public long getAtendidas(TipoPoliticaDisco p) {
        mutex.acquireUninterruptibly();
        try { return atendidasPorPolitica[p.ordinal()]; } finally { mutex.release(); }
    }
    public long getTotalAtendidas() {
        mutex.acquireUninterruptibly();
        try { return nAtendidas; } finally { mutex.release(); }
    }
}
