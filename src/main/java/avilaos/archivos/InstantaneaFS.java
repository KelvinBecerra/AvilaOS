package avilaos.archivos;

/** Estado completo del sistema de archivos y del mapa de bloques, listo para dibujar. */
public class InstantaneaFS {
    public NodoVista raiz;
    public int totalBloques, libres;
    public int[] colorBloque;        // -1 = libre
    public String[] nombreBloque;    // archivo que ocupa cada bloque ("" si libre)
    public int[] siguienteBloque;    // puntero de la lista enlazada (-1 = fin / libre)
    public NodoVista[] archivos;     // todos los archivos (para la tabla de asignación)
    public long version;
}
