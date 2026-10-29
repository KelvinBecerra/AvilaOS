package avilaos.politicas;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.PoliticaPlanificacion;
import avilaos.modelo.Proceso;
import avilaos.modelo.TipoPolitica;

/** First Come First Served: no apropiativa, ordena por orden de llegada a la cola de listos. */
public class PoliticaFCFS implements PoliticaPlanificacion {
    @Override public TipoPolitica tipo() { return TipoPolitica.FCFS; }
    @Override public String nombre() { return TipoPolitica.FCFS.getTexto(); }

    @Override public void ordenar(ListaEnlazada<Proceso> listos) {
        listos.ordenar((a, b) -> Long.compare(a.getSecuenciaListo(), b.getSecuenciaListo()));
    }

    @Override public boolean debeExpropiar(Proceso actual, ListaEnlazada<Proceso> listos, int ciclosEnCPU) {
        return false;
    }
}
