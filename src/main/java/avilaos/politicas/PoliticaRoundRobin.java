package avilaos.politicas;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.PoliticaPlanificacion;
import avilaos.modelo.Proceso;
import avilaos.modelo.TipoPolitica;
import avilaos.sistema.ConfigSimulacion;

/** Round Robin con quantum configurable en tiempo de ejecución (se lee de ConfigSimulacion). */
public class PoliticaRoundRobin implements PoliticaPlanificacion {
    private final ConfigSimulacion config;

    public PoliticaRoundRobin(ConfigSimulacion config) { this.config = config; }

    @Override public TipoPolitica tipo() { return TipoPolitica.ROUND_ROBIN; }
    @Override public String nombre() { return TipoPolitica.ROUND_ROBIN.getTexto() + " (q=" + config.getQuantum() + ")"; }

    @Override public void ordenar(ListaEnlazada<Proceso> listos) {
        listos.ordenar((a, b) -> Long.compare(a.getSecuenciaListo(), b.getSecuenciaListo()));
    }

    @Override public boolean debeExpropiar(Proceso actual, ListaEnlazada<Proceso> listos, int ciclosEnCPU) {
        return ciclosEnCPU >= config.getQuantum() && !listos.estaVacia();
    }
}
