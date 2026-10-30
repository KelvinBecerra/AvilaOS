package avilaos.so;

/** Foto de métricas de un computador (o del sistema completo, si se combinan varias). */
public class MetricasComputador {
    public long ciclos;          // ciclos acumulados de CPU (suma sobre los computadores combinados)
    public long ciclosReloj;     // ciclos transcurridos del reloj global
    public long ocupados;
    public int completados, vencidos;
    public double respuestaSuma; public int respuestaN;
    public double ratioSuma, ratioSuma2; public int ratioN;

    public double utilizacion() { return ciclos == 0 ? 0 : 100.0 * ocupados / ciclos; }
    /** Procesos completados con éxito por cada 100 ciclos de reloj. */
    public double throughput() { return ciclosReloj == 0 ? 0 : 100.0 * completados / ciclosReloj; }
    public double respuestaPromedio() { return respuestaN == 0 ? Double.NaN : respuestaSuma / respuestaN; }
    /** % de procesos terminados que cumplieron su deadline. */
    public double cumplimiento() {
        int t = completados + vencidos;
        return t == 0 ? Double.NaN : 100.0 * completados / t;
    }
    /** Índice de equidad de Jain sobre la fracción de vida que cada proceso estuvo en CPU (1 = perfecta). */
    public double equidad() {
        return (ratioN == 0 || ratioSuma2 == 0) ? Double.NaN : (ratioSuma * ratioSuma) / (ratioN * ratioSuma2);
    }

    public static MetricasComputador combinar(MetricasComputador[] ms) {
        MetricasComputador g = new MetricasComputador();
        for (MetricasComputador m : ms) {
            g.ciclos += m.ciclos;
            g.ciclosReloj = Math.max(g.ciclosReloj, m.ciclosReloj);
            g.ocupados += m.ocupados;
            g.completados += m.completados;
            g.vencidos += m.vencidos;
            g.respuestaSuma += m.respuestaSuma; g.respuestaN += m.respuestaN;
            g.ratioSuma += m.ratioSuma; g.ratioSuma2 += m.ratioSuma2; g.ratioN += m.ratioN;
        }
        return g;
    }
}
