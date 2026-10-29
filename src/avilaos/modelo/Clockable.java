package avilaos.modelo;

/** Interfaz común de todo componente que reacciona al reloj global (CPU, controlador de E/S, ...). */
public interface Clockable {
    /** Avanza exactamente un ciclo de reloj. */
    void tick(long ciclo);
}
