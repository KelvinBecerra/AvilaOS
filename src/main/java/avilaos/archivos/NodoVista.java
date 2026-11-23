package avilaos.archivos;

import avilaos.modelo.Permiso;
import avilaos.modelo.TipoNodo;

/** Copia inmutable de un nodo para la GUI (la interfaz nunca toca el árbol vivo). */
public class NodoVista {
    public final String nombre, ruta, owner, creador;
    public final TipoNodo tipo;
    public final Permiso permiso;
    public final int primerBloque, bloques, color;
    public final boolean pendiente;
    public final NodoVista[] hijos;

    NodoVista(NodoSA n, NodoVista[] hijos) {
        this.nombre = n.padre == null ? "/" : n.nombre;
        this.ruta = n.ruta();
        this.owner = n.owner;
        this.creador = n.creador;
        this.tipo = n.tipo;
        this.permiso = n.permiso;
        this.primerBloque = n.primerBloque;
        this.bloques = n.bloques;
        this.color = n.color;
        this.pendiente = n.pendiente;
        this.hijos = hijos;
    }

    private NodoVista() {
        this.nombre = "(sin datos)"; this.ruta = "/"; this.owner = ""; this.creador = "";
        this.tipo = TipoNodo.DIRECTORIO; this.permiso = Permiso.PUBLICO;
        this.primerBloque = -1; this.bloques = 0; this.color = -1; this.pendiente = false; this.hijos = new NodoVista[0];
    }

    /** Raíz vacía para inicializar el JTree antes de que haya datos. */
    public static NodoVista vacio() { return new NodoVista(); }

    @Override public String toString() { return nombre + (pendiente ? " (creando…)" : ""); }
}
