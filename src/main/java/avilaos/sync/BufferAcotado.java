package avilaos.sync;

import avilaos.modelo.TipoSemaforo;
import avilaos.so.Computador;
import java.util.concurrent.Semaphore;

/**
 * Buffer de capacidad limitada residente en la RAM de su computador anfitrión.
 * Tres semáforos (algoritmo de Stallings, fig. 5.13):
 *  - mutex  (binario, 1)         : exclusión mutua sobre el buffer
 *  - lleno  (conteo, 0)          : elementos disponibles  (la 'n' del libro)
 *  - vacio  (conteo, capacidad)  : espacios libres        (la 'e' del libro)
 */
public class BufferAcotado {
    private final String nombre;
    private final int capacidad;
    private final Computador host;
    private final SemaforoSO mutex;
    private final SemaforoSO llenos;
    private final SemaforoSO vacios;

    private final int[] datos;
    private int inicio, fin, cantidad, siguienteValor = 1;
    private long producidos, consumidos, ciclosBloqueoTotal;
    private int usuarios;
    private final Semaphore mutexDatos = new Semaphore(1);

    public BufferAcotado(String nombre, int capacidad, Computador host) {
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.host = host;
        this.datos = new int[capacidad];
        this.mutex = new SemaforoSO("mutex", TipoSemaforo.BINARIO, 1);
        this.llenos = new SemaforoSO("lleno", TipoSemaforo.CONTEO, 0);
        this.vacios = new SemaforoSO("vacio", TipoSemaforo.CONTEO, capacidad);
    }

    /** Inserta un elemento (se invoca solo dentro de la región crítica protegida por 'mutex'). */
    public void anadir() {
        mutexDatos.acquireUninterruptibly();
        try {
            datos[fin] = siguienteValor++;
            fin = (fin + 1) % capacidad;
            cantidad++;
            producidos++;
        } finally { mutexDatos.release(); }
    }

    public void extraer() {
        mutexDatos.acquireUninterruptibly();
        try {
            inicio = (inicio + 1) % capacidad;
            cantidad--;
            consumidos++;
        } finally { mutexDatos.release(); }
    }

    public void registrarUsuario() {
        mutexDatos.acquireUninterruptibly();
        try { usuarios++; } finally { mutexDatos.release(); }
    }

    public void sumarBloqueo(int ciclos) {
        mutexDatos.acquireUninterruptibly();
        try { ciclosBloqueoTotal += ciclos; } finally { mutexDatos.release(); }
    }

    public String getNombre() { return nombre; }
    public int getCapacidad() { return capacidad; }
    public Computador getHost() { return host; }
    public SemaforoSO getMutex() { return mutex; }
    public SemaforoSO getLlenos() { return llenos; }
    public SemaforoSO getVacios() { return vacios; }

    public int getCantidad() {
        mutexDatos.acquireUninterruptibly();
        try { return cantidad; } finally { mutexDatos.release(); }
    }
    public long getProducidos() {
        mutexDatos.acquireUninterruptibly();
        try { return producidos; } finally { mutexDatos.release(); }
    }
    public long getConsumidos() {
        mutexDatos.acquireUninterruptibly();
        try { return consumidos; } finally { mutexDatos.release(); }
    }
    /** Tiempo promedio (ciclos) que cada proceso asociado pasó bloqueado en semáforos de este buffer. */
    public double getBloqueoPromedio() {
        mutexDatos.acquireUninterruptibly();
        try { return usuarios == 0 ? 0 : (double) ciclosBloqueoTotal / usuarios; } finally { mutexDatos.release(); }
    }
}
