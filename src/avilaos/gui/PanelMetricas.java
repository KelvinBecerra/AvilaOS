package avilaos.gui;

import avilaos.estructuras.ListaEnlazada;
import avilaos.sistema.Cluster;
import avilaos.so.MetricasComputador;
import avilaos.sync.BufferAcotado;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/** Métricas de rendimiento por computador y del sistema, métricas del productor-consumidor y gráfico de CPU. */
public class PanelMetricas extends JPanel {
    private final Cluster cluster;
    private final ModeloTabla modeloComp = new ModeloTabla("", "Throughput (proc/100 ciclos)", "Utilización CPU (%)",
            "Tiempo de respuesta prom. (ciclos)", "Cumplimiento de deadlines (%)", "Equidad (Jain)", "Completados", "Vencidos (deadline)");
    private final ModeloTabla modeloBuf = new ModeloTabla("Buffer", "Anfitrión", "Elementos producidos",
            "Elementos consumidos", "Tiempo prom. bloqueado en semáforos (ciclos)");
    private final GraficoUtilizacion grafico;

    public PanelMetricas(Cluster cluster) {
        this.cluster = cluster;
        this.grafico = new GraficoUtilizacion(cluster.getComputadores());
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTable t1 = new JTable(modeloComp);
        t1.setFillsViewportHeight(true);
        JScrollPane s1 = new JScrollPane(t1);
        s1.setBorder(BorderFactory.createTitledBorder("Rendimiento por computador y del sistema"));
        s1.setPreferredSize(new Dimension(800, 40 + 22 * (cluster.getComputadores().length + 2)));
        add(s1, BorderLayout.NORTH);

        add(grafico, BorderLayout.CENTER);

        JTable t2 = new JTable(modeloBuf);
        t2.setFillsViewportHeight(true);
        JScrollPane s2 = new JScrollPane(t2);
        s2.setBorder(BorderFactory.createTitledBorder("Productor-consumidor por buffer"));
        s2.setPreferredSize(new Dimension(800, 130));
        add(s2, BorderLayout.SOUTH);
    }

    private static String f(double v, int dec) { return Double.isNaN(v) ? "-" : String.format("%." + dec + "f", v); }

    public void refrescar() {
        MetricasComputador[] ms = cluster.metricasPorComputador();
        String[][] filas = new String[ms.length + 1][];
        for (int i = 0; i < ms.length; i++) filas[i] = fila(cluster.getComputador(i).getNombre(), ms[i]);
        filas[ms.length] = fila("SISTEMA COMPLETO", MetricasComputador.combinar(ms));
        modeloComp.setDatos(filas);

        ListaEnlazada<BufferAcotado> bs = cluster.copiarBuffers();
        String[][] fb = new String[bs.tamano()][];
        for (int i = 0; i < fb.length; i++) {
            BufferAcotado b = bs.obtener(i);
            fb[i] = new String[] { b.getNombre(), b.getHost().getNombre(), String.valueOf(b.getProducidos()),
                    String.valueOf(b.getConsumidos()), f(b.getBloqueoPromedio(), 1) };
        }
        modeloBuf.setDatos(fb);
        grafico.repaint();
    }

    private static String[] fila(String nombre, MetricasComputador m) {
        return new String[] { nombre, f(m.throughput(), 2), f(m.utilizacion(), 1), f(m.respuestaPromedio(), 1),
                f(m.cumplimiento(), 1), f(m.equidad(), 3), String.valueOf(m.completados), String.valueOf(m.vencidos) };
    }
}
