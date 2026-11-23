package avilaos.politicas;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.DireccionCabezal;
import avilaos.modelo.PoliticaDisco;
import avilaos.modelo.SeleccionDisco;
import avilaos.modelo.SolicitudDisco;
import avilaos.modelo.TipoPoliticaDisco;

/** C-SCAN: solo sirve hacia bloques mayores; al llegar al extremo salta al bloque 0 (el salto no se cuenta). */
public class PoliticaDiscoCSCAN implements PoliticaDisco {
    @Override public TipoPoliticaDisco tipo() { return TipoPoliticaDisco.C_SCAN; }
    @Override public String nombre() { return "C-SCAN"; }

    @Override public SeleccionDisco seleccionar(ListaEnlazada<SolicitudDisco> cola, int cabezal, DireccionCabezal dir, int total) {
        if (cola.estaVacia()) return null;
        SolicitudDisco s = UtilDisco.menorDesde(cola, cabezal);
        if (s != null) return SeleccionDisco.directa(s);
        s = UtilDisco.menorDesde(cola, 0);
        return new SeleccionDisco(s, new int[] { total - 1, 0, s.getBloque() }, 0);   // extremo -> salto a 0 -> bloque
    }
}
