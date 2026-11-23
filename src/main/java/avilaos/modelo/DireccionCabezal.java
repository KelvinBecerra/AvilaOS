package avilaos.modelo;

public enum DireccionCabezal {
    ASCENDENTE("↑ hacia bloques mayores"), DESCENDENTE("↓ hacia bloques menores");

    private final String texto;
    DireccionCabezal(String texto) { this.texto = texto; }
    @Override public String toString() { return texto; }
}
