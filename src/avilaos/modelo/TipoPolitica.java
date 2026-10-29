package avilaos.modelo;

/** Conjunto fijo de políticas disponibles (el comportamiento vive en PoliticaPlanificacion). */
public enum TipoPolitica {
    FCFS("FCFS"),
    EDF("EDF (Earliest Deadline First)"),
    ROUND_ROBIN("Round Robin"),
    PRIORIDADES("Prioridades apropiativas");

    private final String texto;
    TipoPolitica(String texto) { this.texto = texto; }
    public String getTexto() { return texto; }
    @Override public String toString() { return texto; }
}
