package avilaos.hardware;

import java.util.concurrent.Semaphore;

/** RAM de un computador. Solo se lleva la cuenta de memoria usada/libre (sin particiones). */
public class Memoria {
    private final int total;
    private int usada;
    private final Semaphore mutex = new Semaphore(1);

    public Memoria(int total) { this.total = total; }

    public boolean reservar(int cantidad) {
        mutex.acquireUninterruptibly();
        try {
            if (usada + cantidad > total) return false;
            usada += cantidad;
            return true;
        } finally { mutex.release(); }
    }

    public void liberar(int cantidad) {
        mutex.acquireUninterruptibly();
        try { usada = Math.max(0, usada - cantidad); } finally { mutex.release(); }
    }

    public int getTotal() { return total; }
    public int getUsada() {
        mutex.acquireUninterruptibly();
        try { return usada; } finally { mutex.release(); }
    }
    public int getLibre() { return total - getUsada(); }
}
