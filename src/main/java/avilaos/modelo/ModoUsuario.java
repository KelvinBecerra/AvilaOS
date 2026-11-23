package avilaos.modelo;

public enum ModoUsuario {
    ADMINISTRADOR("Administrador"), USUARIO("Usuario");

    private final String texto;
    ModoUsuario(String texto) { this.texto = texto; }
    @Override public String toString() { return texto; }
}
