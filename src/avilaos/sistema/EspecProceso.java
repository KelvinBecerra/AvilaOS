package avilaos.sistema;

import avilaos.modelo.TipoProceso;

/** Datos con los que el usuario define un proceso (Vista 1). También es la unidad que se guarda en CSV. */
public class EspecProceso {
    public String nombre;
    public TipoProceso tipo;
    public int instrucciones, memoria, prioridad, deadline;
    public int computador = -1;          // -1 = asignación automática por ÁvilaOS
    public String buffer = "";           // productor / consumidor
    public int periodo = 1, elementos = 1;
    public int ciclosES = 0, periodoES = 0;   // I/O bound
}
