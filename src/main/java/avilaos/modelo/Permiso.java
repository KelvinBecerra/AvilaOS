package avilaos.modelo;

public enum Permiso {
    PRIVADO("private"), PUBLICO("public");

    private final String json;
    Permiso(String json) { this.json = json; }
    public String getJson() { return json; }
    public static Permiso desdeTexto(String t) {
        if (t != null) {
            String s = t.trim().toLowerCase();
            if (s.equals("public") || s.equals("publico") || s.equals("público")) return PUBLICO;
            if (s.equals("private") || s.equals("privado")) return PRIVADO;
        }
        throw new IllegalArgumentException("Permiso inválido '" + t + "' (use public o private)");
    }
    @Override public String toString() { return this == PUBLICO ? "Público" : "Privado"; }
}
