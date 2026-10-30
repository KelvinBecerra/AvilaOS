package avilaos.politicas;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.PoliticaPlanificacion;
import avilaos.modelo.Proceso;
import avilaos.modelo.TipoPolitica;

/**
 * Implementación de la política de planificación <b>Earliest Deadline First (EDF)</b>.
 * <p>
 * EDF es un algoritmo dinámico y apropiativo (preemptivo) diseñado para sistemas de tiempo real.
 * Asigna la CPU en todo momento al proceso que posea el límite de tiempo (<i>deadline</i>) 
 * más cercano a expirar.
 * </p>
 * 
 * @author ÁvilaOS Team
 * @see PoliticaPlanificacion
 * @see Proceso
 * @see TipoPolitica
 */
public class PoliticaEDF implements PoliticaPlanificacion {

    /**
     * Retorna el identificador de enumeración correspondiente a la política EDF.
     * 
     * @return {@link TipoPolitica#EDF}
     */
    @Override 
    public TipoPolitica tipo() { 
        return TipoPolitica.EDF; 
    }

    /**
     * Retorna la representación en formato de texto legible del nombre de la política.
     * 
     * @return Nombre textual de la política obtenido desde {@link TipoPolitica}.
     */
    @Override 
    public String nombre() { 
        return TipoPolitica.EDF.getTexto(); 
    }

    /**
     * Ordena la lista de procesos en estado LISTO aplicando el criterio EDF.
     * <p>
     * Criterio de ordenamiento:
     * <ol>
     *   <li><b>Primario:</b> Compara el atributo {@code deadline} de menor a mayor. Un valor menor 
     *       indica mayor urgencia de ejecución.</li>
     *   <li><b>Secundario (Desempate):</b> En caso de igualdad en el deadline, desempata mediante 
     *       {@code secuenciaListo} garantizando un orden FIFO (First In, First Out).</li>
     * </ol>
     * </p>
     * 
     * @param listos Colección {@link ListaEnlazada} con los procesos candidatos a ser ejecutados.
     */
    @Override 
    public void ordenar(ListaEnlazada<Proceso> listos) {
        listos.ordenar((a, b) -> {
            int c = Integer.compare(a.getDeadline(), b.getDeadline());
            return c != 0 ? c : Long.compare(a.getSecuenciaListo(), b.getSecuenciaListo());
        });
    }

    /**
     * Determina si el proceso actual en CPU debe ser desalojado (expropiado).
     * <p>
     * Evalúa si el proceso candidato al frente de la cola de listos tiene un deadline estrictamente 
     * menor que el proceso en ejecución.
     * </p>
     * 
     * @param actual Proceso que se encuentra usando la CPU en el ciclo corriente.
     * @param listos Cola de procesos listos previa u ordenadamente procesada.
     * @param ciclosEnCPU Cantidad de ciclos continuos de reloj que lleva el proceso en ejecución.
     * @return {@code true} si existe un proceso listo más urgente que deba tomar la CPU; {@code false} en caso contrario.
     */
    @Override 
    public boolean debeExpropiar(Proceso actual, ListaEnlazada<Proceso> listos, int ciclosEnCPU) {
        Proceso cand = listos.verPrimero();
        return cand != null && cand.getDeadline() < actual.getDeadline();
    }
}