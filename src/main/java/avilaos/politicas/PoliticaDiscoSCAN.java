package avilaos.politicas;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.DireccionCabezal;
import avilaos.modelo.PoliticaDisco;
import avilaos.modelo.SeleccionDisco;
import avilaos.modelo.SolicitudDisco;
import avilaos.modelo.TipoPoliticaDisco;

/** SCAN (ascensor): sigue en su dirección; si no hay más solicitudes llega hasta el extremo del disco y regresa. */
public class PoliticaDiscoSCAN implements PoliticaDisco {
    @Override public TipoPoliticaDisco tipo() { return TipoPoliticaDisco.SCAN; }
    @Override public String nombre() { return "SCAN"; }

    @Override public SeleccionDisco seleccionar(ListaEnlazada<SolicitudDisco> cola, int cabezal, DireccionCabezal dir, int total) {
        if (cola.estaVacia()) return null;
        int fin = total - 1;
        if (dir == DireccionCabezal.ASCENDENTE) {
            SolicitudDisco s = UtilDisco.menorDesde(cola, cabezal);
            if (s != null) return SeleccionDisco.directa(s);
            s = UtilDisco.mayorHasta(cola, fin);                       // todas quedaron detrás: extremo y regreso
            return new SeleccionDisco(s, new int[] { fin, s.getBloque() }, -1);
        }
        SolicitudDisco s = UtilDisco.mayorHasta(cola, cabezal);
        if (s != null) return SeleccionDisco.directa(s);
        s = UtilDisco.menorDesde(cola, 0);
        return new SeleccionDisco(s, new int[] { 0, s.getBloque() }, -1);
    }
}
