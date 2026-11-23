package avilaos.politicas;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.DireccionCabezal;
import avilaos.modelo.PoliticaDisco;
import avilaos.modelo.SeleccionDisco;
import avilaos.modelo.SolicitudDisco;
import avilaos.modelo.TipoPoliticaDisco;

/** Shortest Seek Time First: la solicitud más cercana al cabezal. */
public class PoliticaDiscoSSTF implements PoliticaDisco {
    @Override public TipoPoliticaDisco tipo() { return TipoPoliticaDisco.SSTF; }
    @Override public String nombre() { return "SSTF"; }

    @Override public SeleccionDisco seleccionar(ListaEnlazada<SolicitudDisco> cola, int cabezal, DireccionCabezal dir, int total) {
        SolicitudDisco s = UtilDisco.masCercana(cola, cabezal);
        return s == null ? null : SeleccionDisco.directa(s);
    }
}
