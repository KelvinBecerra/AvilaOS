package avilaos.sync;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.Proceso;
import avilaos.modelo.TipoSemaforo;
import java.util.concurrent.Semaphore;

/**
 * Semáforo SIMULADO de ÁvilaOS (el que ven los procesos simulados). Tiene valor y una cola
 * de procesos bloqueados. Es distinto del java.util.concurrent.Semaphore, que se usa
 * internamente aquí (campo 'mutex') solo para que las operaciones sean atómicas entre
 * los hilos de Java de los distintos computadores.
 *
 * Semántica (Stallings): semWait decrementa y bloquea si el valor queda negativo;
 * semSignal incrementa y, si había bloqueados, despierta al primero (FIFO).
 */
public class SemaforoSO {
    private final String nombre;
    private final TipoSemaforo tipo;
    private int valor;
    private final ListaEnlazada<Proceso> cola = new ListaEnlazada<>();
    private final Semaphore mutex = new Semaphore(1);

    public SemaforoSO(String nombre, TipoSemaforo tipo, int valorInicial) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.valor = valorInicial;
    }

    /** @return true si el proceso puede continuar; false si quedó bloqueado en la cola del semáforo. */
    public boolean semWait(Proceso p) {
        mutex.acquireUninterruptibly();
        try {
            valor--;
            if (valor < 0) {
                cola.agregarAlFinal(p);
                p.alBloquearseEn(this);
                return false;
            }
            p.agregarTenido(this);
            return true;
        } finally { mutex.release(); }
    }

    /** @return el proceso despertado (el llamador debe avisarle a su computador) o null. */
    public Proceso semSignal() {
        mutex.acquireUninterruptibly();
        try {
            valor++;
            if (tipo == TipoSemaforo.BINARIO && valor > 1) valor = 1;
            if (valor <= 0) {
                Proceso w = cola.quitarPrimero();
                if (w != null) w.alDespertar(this);
                return w;
            }
            return null;
        } finally { mutex.release(); }
    }

    /** Usado cuando un proceso bloqueado muere: lo saca de la cola y deshace su decremento. */
    public boolean quitarDeCola(Proceso p) {
        mutex.acquireUninterruptibly();
        try {
            if (cola.quitar(p)) { valor++; return true; }
            return false;
        } finally { mutex.release(); }
    }

    public String getNombre() { return nombre; }
    public TipoSemaforo getTipo() { return tipo; }

    public int getValor() {
        mutex.acquireUninterruptibly();
        try { return valor; } finally { mutex.release(); }
    }

    public ListaEnlazada<Proceso> copiarCola() {
        mutex.acquireUninterruptibly();
        try { return cola.copiar(); } finally { mutex.release(); }
    }
}
