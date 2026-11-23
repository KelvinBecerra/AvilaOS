package avilaos.archivos;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.Permiso;
import avilaos.modelo.TipoNodo;

/** Nodo del árbol del sistema de archivos (directorio o archivo). Se modifica solo bajo el semáforo de SistemaArchivos. */
public class NodoSA {
    String nombre;
    final TipoNodo tipo;
    String owner;
    Permiso permiso;
    NodoSA padre;
    final ListaEnlazada<NodoSA> hijos = new ListaEnlazada<>();
    int primerBloque = -1;
    int bloques;
    String creador = "carga inicial";
    int color = -1;
    boolean pendiente;     // el proceso de E/S que lo crea todavía no terminó
    boolean eliminando;    // un proceso de E/S lo está eliminando

    NodoSA(String nombre, TipoNodo tipo, String owner, Permiso permiso) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.owner = owner;
        this.permiso = permiso;
    }

    public String getNombre() { return nombre; }
    public TipoNodo getTipo() { return tipo; }
    public String getOwner() { return owner; }
    public Permiso getPermiso() { return permiso; }
    public int getPrimerBloque() { return primerBloque; }
    public int getBloques() { return bloques; }
    public boolean isPendiente() { return pendiente; }
    public boolean isEliminando() { return eliminando; }
    public NodoSA getPadre() { return padre; }

    public String ruta() {
        if (padre == null) return "/";
        String r = padre.ruta();
        return (r.equals("/") ? "" : r) + "/" + nombre;
    }
}
