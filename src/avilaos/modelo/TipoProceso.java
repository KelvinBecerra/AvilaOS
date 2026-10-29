package avilaos.modelo;

public enum TipoProceso {
    CPU_BOUND("CPU bound"),
    IO_BOUND("I/O bound"),
    PRODUCTOR("Productor"),
    CONSUMIDOR("Consumidor");

    private final String texto;
    TipoProceso(String texto) { this.texto = texto; }
    public String getTexto() { return texto; }
    public boolean esDeBuffer() { return this == PRODUCTOR || this == CONSUMIDOR; }
    @Override public String toString() { return texto; }
}
