package avilaos.hardware;

import avilaos.sistema.ConfigSimulacion;
import avilaos.so.Computador;
import java.util.concurrent.Semaphore;

/**
 * Reloj global. En cada ciclo despierta a los hilos de todos los computadores y espera
 * en una barrera (semáforo) a que cada uno termine EXACTAMENTE un ciclo antes de dar el siguiente.
 */
public class Reloj implements Runnable {
    private final ConfigSimulacion config;
    private Computador[] computadores = new Computador[0];
    private final Semaphore barrera = new Semaphore(0);
    private final Semaphore pasos = new Semaphore(0);
    private volatile boolean corriendo;
    private volatile boolean pausado = true;
    private volatile long ciclo;
    private Thread hilo;

    public Reloj(ConfigSimulacion config) { this.config = config; }

    public void setComputadores(Computador[] c) { this.computadores = c; }

    public synchronized void iniciar() {
        if (hilo != null) return;
        corriendo = true;
        for (Computador c : computadores) c.arrancar();
        hilo = new Thread(this, "Reloj-global");
        hilo.setDaemon(true);
        hilo.start();
    }

    public void detener() {
        corriendo = false;
        for (Computador c : computadores) c.detener();
    }

    /** Llamado por cada computador al terminar su ciclo. */
    public void cicloTerminado() { barrera.release(); }

    public void setPausado(boolean p) { this.pausado = p; }
    public boolean isPausado() { return pausado; }
    /** Ejecuta un único ciclo (modo paso a paso, con el reloj en pausa). */
    public void paso() { pasos.release(); }
    public long getCiclo() { return ciclo; }

    @Override public void run() {
        while (corriendo) {
            boolean enPaso = false;
            if (pausado) {
                try {
                    if (!pasos.tryAcquire(50, java.util.concurrent.TimeUnit.MILLISECONDS)) continue;
                } catch (InterruptedException e) { return; }
                enPaso = true;
            }
            ciclo++;
            for (Computador c : computadores) c.iniciarCiclo(ciclo);
            barrera.acquireUninterruptibly(computadores.length);
            if (!enPaso) {
                try { Thread.sleep(config.getMsPorCiclo()); } catch (InterruptedException e) { return; }
            }
        }
    }
}
