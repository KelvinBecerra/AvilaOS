package avilaos.so;

import avilaos.modelo.TipoPolitica;

/** Instantánea de un computador para la GUI (la GUI observa al sistema, no es el sistema). */
public class EstadoComputador {
    public String nombre, politica;
    public TipoPolitica tipoPolitica;
    public String proceso = "—", instruccion = "—", modo = "—", prioridad = "—", deadline = "—", pc = "—";
    public boolean enEjecucionSO;
    public int ramTotal, ramUsada;
    public String[] nuevos, listos, bloqueados, terminados;
    public String[][] pcb;
}
