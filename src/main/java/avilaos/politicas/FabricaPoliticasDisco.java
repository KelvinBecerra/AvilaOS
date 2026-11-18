package avilaos.politicas;

import avilaos.modelo.PoliticaDisco;
import avilaos.modelo.TipoPoliticaDisco;

/** Único lugar que conoce las políticas de disco concretas. */
public final class FabricaPoliticasDisco {
    private FabricaPoliticasDisco() { }

    public static PoliticaDisco crear(TipoPoliticaDisco tipo) {
        switch (tipo) {
            case SSTF: return new PoliticaDiscoSSTF();
            case SCAN: return new PoliticaDiscoSCAN();
            case C_SCAN: return new PoliticaDiscoCSCAN();
            case LOOK: return new PoliticaDiscoLOOK();
            case C_LOOK: return new PoliticaDiscoCLOOK();
            case FIFO:
            default: return new PoliticaDiscoFIFO();
        }
    }
}
