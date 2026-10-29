package avilaos.politicas;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.PoliticaPlanificacion;
import avilaos.modelo.Proceso;
import avilaos.modelo.TipoPolitica;

/** Earliest Deadline First (apropiativa): primero el proceso con menor deadline restante. */
public class PoliticaEDF implements PoliticaPlanificacion {
    @Override public TipoPolitica tipo() { return TipoPolitica.EDF; }
    @Override public String nombre() { return TipoPolitica.EDF.getTexto(); }

    @Override public void ordenar(ListaEnlazada<Proceso> listos) {
        listos.ordenar((a, b) -> {
            int c = Integer.compare(a.getDeadline(), b.getDeadline());
            return c != 0 ? c : Long.compare(a.getSecuenciaListo(), b.getSecuenciaListo());
        });
    }

    @Override public boolean debeExpropiar(Proceso actual, ListaEnlazada<Proceso> listos, int ciclosEnCPU) {
        Proceso cand = listos.verPrimero();
        return cand != null && cand.getDeadline() < actual.getDeadline();
    }
}
