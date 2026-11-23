package avilaos.archivos;

import java.util.concurrent.Semaphore;

/** Área de intercambio del disco: un proceso suspendido ocupa tantos bloques como unidades de memoria tenía. */
public class AreaSwap {
    private final int total;
    private final int[] dueno;           // id del proceso dueño de cada bloque (0 = libre)
    private final Semaphore mutex = new Semaphore(1);

    public AreaSwap(int total) {
        this.total = Math.max(0, total);
        this.dueno = new int[this.total];
    }

    /** @return false si no hay bloques libres suficientes (no asigna nada) */
    public boolean asignar(int idProceso, int cantidad) {
        mutex.acquireUninterruptibly();
        try {
            if (libresInterno() < cantidad) return false;
            int n = cantidad;
            for (int i = 0; i < total && n > 0; i++) if (dueno[i] == 0) { dueno[i] = idProceso; n--; }
            return true;
        } finally { mutex.release(); }
    }

    public void liberar(int idProceso) {
        mutex.acquireUninterruptibly();
        try { for (int i = 0; i < total; i++) if (dueno[i] == idProceso) dueno[i] = 0; }
        finally { mutex.release(); }
    }

    private int libresInterno() { int n = 0; for (int i = 0; i < total; i++) if (dueno[i] == 0) n++; return n; }

    public int getTotal() { return total; }
    public int getLibres() {
        mutex.acquireUninterruptibly();
        try { return libresInterno(); } finally { mutex.release(); }
    }
    public int[] copiarDuenos() {
        mutex.acquireUninterruptibly();
        try { int[] c = new int[total]; System.arraycopy(dueno, 0, c, 0, total); return c; } finally { mutex.release(); }
    }
}
