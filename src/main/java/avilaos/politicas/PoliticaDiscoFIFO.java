package avilaos.politicas;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.DireccionCabezal;
import avilaos.modelo.PoliticaDisco;
import avilaos.modelo.SeleccionDisco;
import avilaos.modelo.SolicitudDisco;
import avilaos.modelo.TipoPoliticaDisco;

/** Atiende en orden de llegada. */
public class PoliticaDiscoFIFO implements PoliticaDisco {
    @Override public TipoPoliticaDisco tipo() { return TipoPoliticaDisco.FIFO; }
    @Override public String nombre() { return "FIFO"; }

    @Override public SeleccionDisco seleccionar(ListaEnlazada<SolicitudDisco> cola, int cabezal, DireccionCabezal dir, int total) {
        SolicitudDisco s = cola.verPrimero();
        return s == null ? null : SeleccionDisco.directa(s);
    }
}
