package avilaos.sistema;

import avilaos.modelo.TipoPolitica;

/** Parámetros de la simulación. Los marcados volatile pueden cambiarse en tiempo de ejecución. */
public class ConfigSimulacion {
    public static final int MIN_COMPUTADORES = 2, MAX_COMPUTADORES = 6;

    private int numComputadores;
    private int[] ram;
    private TipoPolitica[] politicas;
    private volatile int quantum = 4;
    private volatile int latenciaRed = 3;
    private volatile int msPorCiclo = 300;

    public ConfigSimulacion(int numComputadores) {
        if (numComputadores < MIN_COMPUTADORES || numComputadores > MAX_COMPUTADORES)
            throw new IllegalArgumentException("El número de computadores debe estar entre "
                    + MIN_COMPUTADORES + " y " + MAX_COMPUTADORES);
        this.numComputadores = numComputadores;
        this.ram = new int[numComputadores];
        this.politicas = new TipoPolitica[numComputadores];
        for (int i = 0; i < numComputadores; i++) { ram[i] = 64; politicas[i] = TipoPolitica.ROUND_ROBIN; }
    }

    public int getNumComputadores() { return numComputadores; }
    public int getRam(int i) { return ram[i]; }
    public void setRam(int i, int v) { ram[i] = v; }
    public TipoPolitica getPolitica(int i) { return politicas[i]; }
    public void setPolitica(int i, TipoPolitica t) { politicas[i] = t; }
    public int getQuantum() { return quantum; }
    public void setQuantum(int q) { this.quantum = Math.max(1, q); }
    public int getLatenciaRed() { return latenciaRed; }
    public void setLatenciaRed(int l) { this.latenciaRed = Math.max(0, l); }
    public int getMsPorCiclo() { return msPorCiclo; }
    public void setMsPorCiclo(int ms) { this.msPorCiclo = Math.max(1, ms); }
}
