package avilaos.estructuras;

/**
 * Lista enlazada simple propia. Se usa como cola (FIFO), como lista ordenable
 * y como base de todas las colas del sistema (nuevos, listos, bloqueados, terminados,
 * colas de semáforos). NO es thread-safe: quien la comparte debe protegerla con un semáforo.
 */
public class ListaEnlazada<T> {

    private static class Nodo<T> {
        T dato;
        Nodo<T> sig;
        Nodo(T dato) { this.dato = dato; }
    }

    private Nodo<T> cabeza;
    private Nodo<T> cola;
    private int tam;

    public void agregarAlFinal(T dato) {
        Nodo<T> n = new Nodo<>(dato);
        if (cabeza == null) { cabeza = n; cola = n; }
        else { cola.sig = n; cola = n; }
        tam++;
    }

    public void agregarAlInicio(T dato) {
        Nodo<T> n = new Nodo<>(dato);
        n.sig = cabeza;
        cabeza = n;
        if (cola == null) cola = n;
        tam++;
    }

    public T verPrimero() { return cabeza == null ? null : cabeza.dato; }

    public T quitarPrimero() {
        if (cabeza == null) return null;
        T d = cabeza.dato;
        cabeza = cabeza.sig;
        if (cabeza == null) cola = null;
        tam--;
        return d;
    }

    /** Quita la primera aparición (por referencia). Devuelve true si estaba. */
    public boolean quitar(T dato) {
        Nodo<T> prev = null, act = cabeza;
        while (act != null) {
            if (act.dato == dato) {
                if (prev == null) cabeza = act.sig; else prev.sig = act.sig;
                if (act == cola) cola = prev;
                tam--;
                return true;
            }
            prev = act;
            act = act.sig;
        }
        return false;
    }

    public boolean contiene(T dato) {
        for (Nodo<T> n = cabeza; n != null; n = n.sig) if (n.dato == dato) return true;
        return false;
    }

    public T obtener(int indice) {
        if (indice < 0 || indice >= tam) return null;
        Nodo<T> n = cabeza;
        for (int i = 0; i < indice; i++) n = n.sig;
        return n.dato;
    }

    public int tamano() { return tam; }
    public boolean estaVacia() { return tam == 0; }

    public void vaciar() { cabeza = null; cola = null; tam = 0; }

    /** Copia superficial (los elementos son los mismos objetos). */
    public ListaEnlazada<T> copiar() {
        ListaEnlazada<T> c = new ListaEnlazada<>();
        for (Nodo<T> n = cabeza; n != null; n = n.sig) c.agregarAlFinal(n.dato);
        return c;
    }

    /**
     * Ordenamiento por inserción ESTABLE programado a mano: los elementos equivalentes
     * conservan su orden relativo de llegada.
     */
    public void ordenar(Criterio<T> criterio) {
        Nodo<T> ordenada = null;
        Nodo<T> actual = cabeza;
        while (actual != null) {
            Nodo<T> siguiente = actual.sig;
            if (ordenada == null || criterio.comparar(actual.dato, ordenada.dato) < 0) {
                actual.sig = ordenada;
                ordenada = actual;
            } else {
                Nodo<T> p = ordenada;
                while (p.sig != null && criterio.comparar(p.sig.dato, actual.dato) <= 0) p = p.sig;
                actual.sig = p.sig;
                p.sig = actual;
            }
            actual = siguiente;
        }
        cabeza = ordenada;
        cola = cabeza;
        while (cola != null && cola.sig != null) cola = cola.sig;
    }
}
