package avilaos.modelo;

/** Pasos del ciclo productor / consumidor del anexo (Stallings, fig. 5.13). */
public enum PasoBuffer {
    PRODUCIR(false, "producir()"),
    CONSUMIR(false, "consumir()"),
    SEM_WAIT_VACIOS(true, "semWait(vacio)"),
    SEM_WAIT_LLENOS(true, "semWait(lleno)"),
    SEM_WAIT_MUTEX(true, "semWait(mutex)"),
    ANADIR(true, "anyadir()"),
    EXTRAER(true, "extraer()"),
    SEM_SIGNAL_MUTEX(true, "semSignal(mutex)"),
    SEM_SIGNAL_LLENOS(true, "semSignal(lleno)"),
    SEM_SIGNAL_VACIOS(true, "semSignal(vacio)");

    private final boolean operacionBuffer;
    private final String texto;
    PasoBuffer(boolean op, String texto) { this.operacionBuffer = op; this.texto = texto; }
    /** Las operaciones sobre el buffer se ejecutan en modo SO y pagan latencia si el buffer es remoto. */
    public boolean esOperacionBuffer() { return operacionBuffer; }
    public String getTexto() { return texto; }
}
