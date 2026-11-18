package avilaos.politicas;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.DireccionCabezal;
import avilaos.modelo.PoliticaDisco;
import avilaos.modelo.SeleccionDisco;
import avilaos.modelo.SolicitudDisco;
import avilaos.modelo.TipoPoliticaDisco;

/** C-LOOK: sirve hacia bloques mayores; sin más solicitudes adelante salta (sin costo) a la menor pendiente. */
public class PoliticaDiscoCLOOK implements PoliticaDisco {
    @Override public TipoPoliticaDisco tipo() { return TipoPoliticaDisco.C_LOOK; }
    @Override public String nombre() { return "C-LOOK"; }

    @Override public SeleccionDisco seleccionar(ListaEnlazada<SolicitudDisco> cola, int cabezal, DireccionCabezal dir, int total) {
        if (cola.estaVacia()) return null;
        SolicitudDisco s = UtilDisco.menorDesde(cola, cabezal);
        if (s != null) return SeleccionDisco.directa(s);
        s = UtilDisco.menorDesde(cola, 0);
        return new SeleccionDisco(s, new int[] { cabezal, s.getBloque() }, 0);
    }
}
