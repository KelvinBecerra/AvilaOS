package avilaos.modelo;

import avilaos.estructuras.ListaEnlazada;

/**
 * Política de planificación de disco intercambiable. Agregar una política nueva no modifica al disco:
 * se implementa esta interfaz y se registra en FabricaPoliticasDisco.
 */
public interface PoliticaDisco {
    TipoPoliticaDisco tipo();

    String nombre();

    /**
     * Elige la próxima solicitud de la cola (no la retira) y el recorrido del cabezal.
     * @param direccion sentido en que se movió por última vez el cabezal
     * @param total     número de bloques del disco (posiciones 0 .. total-1)
     * @return null si la cola está vacía
     */
    SeleccionDisco seleccionar(ListaEnlazada<SolicitudDisco> cola, int cabezal, DireccionCabezal direccion, int total);
}
