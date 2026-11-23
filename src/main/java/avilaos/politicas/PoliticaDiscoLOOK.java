package avilaos.politicas;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.DireccionCabezal;
import avilaos.modelo.PoliticaDisco;
import avilaos.modelo.SeleccionDisco;
import avilaos.modelo.SolicitudDisco;
import avilaos.modelo.TipoPoliticaDisco;

/** LOOK: como SCAN, pero invierte el sentido en la última solicitud sin llegar al extremo del disco. */
public class PoliticaDiscoLOOK implements PoliticaDisco {
    @Override public TipoPoliticaDisco tipo() { return TipoPoliticaDisco.LOOK; }
    @Override public String nombre() { return "LOOK"; }

    @Override public SeleccionDisco seleccionar(ListaEnlazada<SolicitudDisco> cola, int cabezal, DireccionCabezal dir, int total) {
        if (cola.estaVacia()) return null;
        SolicitudDisco s;
        if (dir == DireccionCabezal.ASCENDENTE) {
            s = UtilDisco.menorDesde(cola, cabezal);
            if (s == null) s = UtilDisco.mayorHasta(cola, total - 1);
        } else {
            s = UtilDisco.mayorHasta(cola, cabezal);
            if (s == null) s = UtilDisco.menorDesde(cola, 0);
        }
        return SeleccionDisco.directa(s);
    }
}
