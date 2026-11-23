package avilaos.modelo;

import avilaos.so.Computador;

/**
 * Proceso I/O bound: cada 'periodoES' instrucciones emite una solicitud de E/S (instrucción
 * en modo SO) y se bloquea 'ciclosES' ciclos hasta que el controlador de E/S la satisface.
 */
public class ProgramaES implements Programa {
    private final int total, periodoES, ciclosES;
    private int ejecutadas;

    public ProgramaES(int total, int periodoES, int ciclosES) {
        this.total = total;
        this.periodoES = periodoES;
        this.ciclosES = ciclosES;
    }

    private boolean proximaEsES() {
        int i = ejecutadas + 1;
        return i % periodoES == 0 && i < total;
    }

    @Override public ModoCPU modoProximaInstruccion(Proceso p) {
        return proximaEsES() ? ModoCPU.SISTEMA_OPERATIVO : ModoCPU.USUARIO;
    }

    @Override public ResultadoInstruccion ejecutar(Proceso p, Computador c) {
        boolean pideES = proximaEsES();
        ejecutadas++;
        if (ejecutadas >= total) return ResultadoInstruccion.TERMINADO;
        if (pideES) {
            p.bloquear(MotivoBloqueo.ENTRADA_SALIDA, ciclosES, "E/S (" + ciclosES + " ciclos)");
            return ResultadoInstruccion.BLOQUEADO;
        }
        return ResultadoInstruccion.CONTINUA;
    }

    @Override public String descripcionProximaInstruccion(Proceso p) {
        return proximaEsES() ? "solicitud de E/S" : "instrucción " + (ejecutadas + 1) + "/" + total;
    }

    @Override public void alTerminarAbruptamente(Proceso p, Computador c) { }
}
