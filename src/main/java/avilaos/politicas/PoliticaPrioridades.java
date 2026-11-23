package avilaos.politicas;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.PoliticaPlanificacion;
import avilaos.modelo.Proceso;
import avilaos.modelo.TipoPolitica;

/** Prioridades apropiativas: número menor = mayor prioridad; empata por orden de llegada. */
public class PoliticaPrioridades implements PoliticaPlanificacion {
    @Override public TipoPolitica tipo() { return TipoPolitica.PRIORIDADES; }
    @Override public String nombre() { return TipoPolitica.PRIORIDADES.getTexto(); }

    @Override public void ordenar(ListaEnlazada<Proceso> listos) {
        listos.ordenar((a, b) -> {
            int c = Integer.compare(a.getPrioridad(), b.getPrioridad());
            return c != 0 ? c : Long.compare(a.getSecuenciaListo(), b.getSecuenciaListo());
        });
    }

    @Override public boolean debeExpropiar(Proceso actual, ListaEnlazada<Proceso> listos, int ciclosEnCPU) {
        Proceso cand = listos.verPrimero();
        return cand != null && cand.getPrioridad() < actual.getPrioridad();
    }
}
