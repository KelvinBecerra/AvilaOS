package avilaos.estructuras;

import java.util.concurrent.Semaphore;

/** Arreglo dinámico de doubles (thread-safe) para series temporales de métricas. */
public class SerieDatos {
    private double[] datos = new double[64];
    private int tam;
    private final Semaphore mutex = new Semaphore(1);

    public void agregar(double v) {
        mutex.acquireUninterruptibly();
        try {
            if (tam == datos.length) {
                double[] nuevo = new double[datos.length * 2];
                System.arraycopy(datos, 0, nuevo, 0, tam);
                datos = nuevo;
            }
            datos[tam++] = v;
        } finally { mutex.release(); }
    }

    public int tamano() {
        mutex.acquireUninterruptibly();
        try { return tam; } finally { mutex.release(); }
    }

    /** Copia de los últimos 'max' valores (o todos si hay menos). */
    public double[] ultimos(int max) {
        mutex.acquireUninterruptibly();
        try {
            int n = Math.min(max, tam);
            double[] r = new double[n];
            System.arraycopy(datos, tam - n, r, 0, n);
            return r;
        } finally { mutex.release(); }
    }
}
