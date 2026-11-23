package avilaos.modelo;

/**
 * Subestados del estado BLOQUEADO. Se justifican porque cada motivo tiene un
 * mecanismo de salida distinto:
 *  - ENTRADA_SALIDA: lo despierta el controlador de E/S cuando vence el contador.
 *  - SEMAFORO: lo despierta otro proceso con semSignal (puede ser de otro computador).
 *  - RED: lo despierta el controlador de E/S al cumplirse la latencia de red.
 */
public enum MotivoBloqueo {
    NINGUNO("-"),
    ENTRADA_SALIDA("E/S"),
    SEMAFORO("Semáforo"),
    RED("Red");

    private final String texto;
    MotivoBloqueo(String texto) { this.texto = texto; }
    public String getTexto() { return texto; }
}
