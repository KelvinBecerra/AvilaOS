package avilaos.politicas;

import avilaos.modelo.PoliticaPlanificacion;
import avilaos.modelo.TipoPolitica;
import avilaos.sistema.ConfigSimulacion;

/** Único lugar que conoce las implementaciones: agregar una política = una clase nueva + un case. */
public final class FabricaPoliticas {
    private FabricaPoliticas() { }

    public static PoliticaPlanificacion crear(TipoPolitica tipo, ConfigSimulacion config) {
        switch (tipo) {
            case EDF: return new PoliticaEDF();
            case ROUND_ROBIN: return new PoliticaRoundRobin(config);
            case PRIORIDADES: return new PoliticaPrioridades();
            case FCFS:
            default: return new PoliticaFCFS();
        }
    }
}
