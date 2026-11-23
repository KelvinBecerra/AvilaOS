package avilaos.modelo;

/** Modelo de siete estados (Stallings cap. 3). */
public enum EstadoProceso {
    NUEVO("Nuevo"),
    LISTO("Listo"),
    EJECUCION("Ejecución"),
    BLOQUEADO("Bloqueado"),
    LISTO_SUSPENDIDO("Listo/Suspendido"),
    BLOQUEADO_SUSPENDIDO("Bloqueado/Suspendido"),
    TERMINADO("Terminado");

    private final String texto;
    EstadoProceso(String texto) { this.texto = texto; }
    public String getTexto() { return texto; }
    /** ¿El proceso ocupa memoria RAM en este estado? (los suspendidos están en el área de intercambio) */
    public boolean enMemoria() { return this == LISTO || this == EJECUCION || this == BLOQUEADO; }
    public boolean suspendido() { return this == LISTO_SUSPENDIDO || this == BLOQUEADO_SUSPENDIDO; }
    @Override public String toString() { return texto; }
}
