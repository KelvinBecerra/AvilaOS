package avilaos.modelo;

import avilaos.estructuras.ListaEnlazada;

/**
 * Política de planificación intercambiable. Agregar una política nueva consiste en
 * implementar esta interfaz y registrarla en FabricaPoliticas: el planificador no cambia.
 */
public interface PoliticaPlanificacion {
    TipoPolitica tipo();

    String nombre();

    /** Reordena la cola de listos según el criterio de la política (se invoca tras cada inserción/selección). */
    void ordenar(ListaEnlazada<Proceso> listos);

    /** ¿Debe sacarse de la CPU al proceso actual? (la cola de listos ya está ordenada). */
    boolean debeExpropiar(Proceso actual, ListaEnlazada<Proceso> listos, int ciclosEnCPU);

    /** Selecciona (y retira) el siguiente proceso de la cola de listos. */
    default Proceso seleccionar(ListaEnlazada<Proceso> listos) {
        ordenar(listos);
        return listos.quitarPrimero();
    }
}
