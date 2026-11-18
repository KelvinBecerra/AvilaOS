package avilaos.politicas;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.SolicitudDisco;

/** Búsquedas sobre la cola del disco (a igualdad gana la solicitud que llegó primero). */
final class UtilDisco {
    private UtilDisco() { }

    static SolicitudDisco menorDesde(ListaEnlazada<SolicitudDisco> cola, int min) {
        SolicitudDisco mejor = null;
        for (int i = 0; i < cola.tamano(); i++) {
            SolicitudDisco s = cola.obtener(i);
            if (s.getBloque() >= min && (mejor == null || s.getBloque() < mejor.getBloque())) mejor = s;
        }
        return mejor;
    }

    static SolicitudDisco mayorHasta(ListaEnlazada<SolicitudDisco> cola, int max) {
        SolicitudDisco mejor = null;
        for (int i = 0; i < cola.tamano(); i++) {
            SolicitudDisco s = cola.obtener(i);
            if (s.getBloque() <= max && (mejor == null || s.getBloque() > mejor.getBloque())) mejor = s;
        }
        return mejor;
    }

    static SolicitudDisco masCercana(ListaEnlazada<SolicitudDisco> cola, int cabezal) {
        SolicitudDisco mejor = null;
        int d = Integer.MAX_VALUE;
        for (int i = 0; i < cola.tamano(); i++) {
            SolicitudDisco s = cola.obtener(i);
            int dist = Math.abs(s.getBloque() - cabezal);
            if (dist < d) { d = dist; mejor = s; }
        }
        return mejor;
    }
}
