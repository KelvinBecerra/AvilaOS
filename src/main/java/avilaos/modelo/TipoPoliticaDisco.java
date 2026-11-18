package avilaos.modelo;

public enum TipoPoliticaDisco {
    FIFO("FIFO"), SSTF("SSTF"), SCAN("SCAN"), C_SCAN("C-SCAN"), LOOK("LOOK"), C_LOOK("C-LOOK");

    private final String texto;
    TipoPoliticaDisco(String texto) { this.texto = texto; }
    public String getTexto() { return texto; }
    @Override public String toString() { return texto; }
}
