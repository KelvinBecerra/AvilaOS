package avilaos.modelo;

import avilaos.so.Computador;

/** Proceso CPU bound: todas sus instrucciones son de usuario y consumen procesador. */
public class ProgramaCPU implements Programa {
    private final int total;
    private int ejecutadas;

    public ProgramaCPU(int total) { this.total = total; }

    @Override public ModoCPU modoProximaInstruccion(Proceso p) { return ModoCPU.USUARIO; }

    @Override public ResultadoInstruccion ejecutar(Proceso p, Computador c) {
        ejecutadas++;
        return ejecutadas >= total ? ResultadoInstruccion.TERMINADO : ResultadoInstruccion.CONTINUA;
    }

    @Override public String descripcionProximaInstruccion(Proceso p) {
        return "cálculo " + (ejecutadas + 1) + "/" + total;
    }

    @Override public void alTerminarAbruptamente(Proceso p, Computador c) { }
}
