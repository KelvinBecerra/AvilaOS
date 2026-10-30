package avilaos.sistema;

import java.util.concurrent.Semaphore;

/** Log de eventos global (anillo de tamaño fijo, thread-safe). */
public class Registro {
    private static final int CAPACIDAD = 4000;
    private final String[] anillo = new String[CAPACIDAD];
    private long total;
    private final Semaphore mutex = new Semaphore(1);

    public static class Lectura {
        public final String texto;
        public final long siguiente;
        Lectura(String texto, long siguiente) { this.texto = texto; this.siguiente = siguiente; }
    }

    public void registrar(long ciclo, String mensaje) {
        mutex.acquireUninterruptibly();
        try { anillo[(int) (total++ % CAPACIDAD)] = "[Ciclo " + ciclo + "] " + mensaje; }
        finally { mutex.release(); }
    }

    /** Líneas escritas desde el índice 'desde' (las más antiguas que ya se pisaron se omiten). */
    public Lectura leerDesde(long desde) {
        mutex.acquireUninterruptibly();
        try {
            long ini = Math.max(desde, total - CAPACIDAD);
            StringBuilder sb = new StringBuilder();
            for (long i = ini; i < total; i++) sb.append(anillo[(int) (i % CAPACIDAD)]).append('\n');
            return new Lectura(sb.toString(), total);
        } finally { mutex.release(); }
    }
}
