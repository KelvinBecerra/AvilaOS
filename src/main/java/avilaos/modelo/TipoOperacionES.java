package avilaos.modelo;

public enum TipoOperacionES {
    CREAR("Crear"), LEER("Leer"), ACTUALIZAR("Actualizar"), ELIMINAR("Eliminar"), SUSPENDER("Suspender"), REACTIVAR("Reactivar");

    private final String texto;
    TipoOperacionES(String texto) { this.texto = texto; }
    public String getTexto() { return texto; }
    @Override public String toString() { return texto; }
}
