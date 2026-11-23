package avilaos.gui;

import avilaos.hardware.Disco;
import avilaos.modelo.TipoPoliticaDisco;
import avilaos.sistema.Cluster;
import avilaos.so.EstadoComputador;
import avilaos.so.MetricasComputador;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

/** Métricas del Proyecto 1 (CPU) y del Proyecto 2 (disco por política, suspensiones) con los gráficos de CPU y de cabezal. */
public class PanelMetricas extends JPanel {
    private final Cluster cluster;
    private final ModeloTabla modeloCpu = new ModeloTabla("", "Throughput (proc/100 ciclos)", "Utilización CPU (%)",
            "Tiempo de respuesta prom. (ciclos)", "Cumplimiento de deadlines (%)", "Equidad (Jain)", "Completados", "Vencidos (deadline)");
    private final ModeloTabla modeloDisco = new ModeloTabla("Política de disco", "Solicitudes atendidas", "Activa ahora");
    private final ModeloTabla modeloResumen = new ModeloTabla("Métrica", "Valor");
    private final GraficoUtilizacion graficoCpu;
    private final GraficoCabezal graficoCabezal;

    public PanelMetricas(Cluster cluster) {
        this.cluster = cluster;
        this.graficoCpu = new GraficoUtilizacion(cluster.getComputadores());
        this.graficoCabezal = new GraficoCabezal(cluster.getUnico().getDisco());
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTable t1 = new JTable(modeloCpu);
        t1.setFillsViewportHeight(true);
        JScrollPane s1 = new JScrollPane(t1);
        s1.setBorder(BorderFactory.createTitledBorder("Rendimiento de la CPU (Proyecto 1)"));
        s1.setPreferredSize(new Dimension(800, 62));

        JTable t2 = new JTable(modeloResumen);
        t2.setFillsViewportHeight(true);
        JScrollPane s2 = new JScrollPane(t2);
        s2.setBorder(BorderFactory.createTitledBorder("Disco y suspensión"));
        JTable t3 = new JTable(modeloDisco);
        t3.setFillsViewportHeight(true);
        JScrollPane s3 = new JScrollPane(t3);
        s3.setBorder(BorderFactory.createTitledBorder("Solicitudes atendidas por política"));
        JPanel tablas = new JPanel(new GridLayout(1, 2, 8, 8));
        tablas.add(s2);
        tablas.add(s3);
        tablas.setPreferredSize(new Dimension(800, 175));

        JPanel norte = new JPanel(new BorderLayout(4, 4));
        norte.add(s1, BorderLayout.NORTH);
        norte.add(tablas, BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);

        JPanel graficos = new JPanel(new GridLayout(1, 2, 8, 8));
        graficos.add(graficoCabezal);
        graficos.add(graficoCpu);
        add(graficos, BorderLayout.CENTER);
    }

    private static String f(double v, int dec) { return Double.isNaN(v) ? "-" : String.format("%." + dec + "f", v); }

    public void refrescar() {
        MetricasComputador m = cluster.metricasPorComputador()[0];
        modeloCpu.setDatos(new String[][] { { cluster.getUnico().getNombre(), f(m.throughput(), 2), f(m.utilizacion(), 1), f(m.respuestaPromedio(), 1),
                f(m.cumplimiento(), 1), f(m.equidad(), 3), String.valueOf(m.completados), String.valueOf(m.vencidos) } });
        Disco d = cluster.getUnico().getDisco();
        EstadoComputador e = cluster.getUnico().getNucleo().instantanea();
        modeloResumen.setDatos(new String[][] {
            { "Desplazamiento total del cabezal (bloques)", String.valueOf(d.getDesplazamientoTotal()) },
            { "Tiempo promedio de espera en la cola de E/S (ciclos)", f(d.getEsperaPromedio(), 2) },
            { "Solicitudes atendidas (total)", String.valueOf(d.getTotalAtendidas()) },
            { "Solicitudes en cola ahora", String.valueOf(d.copiarCola().tamano()) },
            { "Suspensiones de procesos", String.valueOf(e.suspensiones) },
            { "Reactivaciones de procesos", String.valueOf(e.reactivaciones) } });
        TipoPoliticaDisco[] ps = TipoPoliticaDisco.values();
        String[][] filas = new String[ps.length][];
        for (int i = 0; i < ps.length; i++)
            filas[i] = new String[] { ps[i].getTexto(), String.valueOf(d.getAtendidas(ps[i])), d.getPolitica().tipo() == ps[i] ? "●" : "" };
        modeloDisco.setDatos(filas);
        graficoCabezal.repaint();
        graficoCpu.repaint();
    }
}
