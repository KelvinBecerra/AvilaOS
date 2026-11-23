package avilaos.modelo;

import avilaos.so.Computador;

/**
 * "Código" de un proceso: define qué hace cada instrucción. Es un comportamiento
 * intercambiable (CPU bound, I/O bound, productor, consumidor), por eso es interfaz.
 */
public interface Programa {
    /** Modo en que se ejecutará la próxima instrucción. */
    ModoCPU modoProximaInstruccion(Proceso p);

    /** Ejecuta UNA instrucción (un ciclo). */
    ResultadoInstruccion ejecutar(Proceso p, Computador c);

    /** Texto de la instrucción a la que apunta el program counter. */
    String descripcionProximaInstruccion(Proceso p);

    /** Deshace efectos pendientes en semáforos si el proceso muere (deadline) a mitad de protocolo. */
    void alTerminarAbruptamente(Proceso p, Computador c);
}
