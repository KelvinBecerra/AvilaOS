package avilaos.estructuras;

/** Criterio de comparación propio (evita depender de java.util.Comparator). */
@FunctionalInterface
public interface Criterio<T> {
    /** negativo si a va antes que b, positivo si va después, 0 si son equivalentes. */
    int comparar(T a, T b);
}
