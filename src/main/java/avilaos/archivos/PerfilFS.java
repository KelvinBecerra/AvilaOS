package avilaos.archivos;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.Permiso;
import avilaos.modelo.TipoNodo;

/** Contenido del JSON del sistema de archivos (configuración del disco + archivos y directorios). */
public class PerfilFS {
    public static class Entrada {
        public String ruta, owner;
        public TipoNodo tipo;
        public Permiso permiso;
        public int inicio, bloques;
        public int[] cadena;     // opcional: bloques exactos (se escribe al guardar el estado)
    }

    public int totalBloques, cabezalInicial;
    public final ListaEnlazada<Entrada> entradas = new ListaEnlazada<>();
}
