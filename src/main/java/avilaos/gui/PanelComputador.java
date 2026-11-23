package avilaos.gui;

import avilaos.modelo.TipoPolitica;
import avilaos.sistema.Cluster;
import avilaos.so.Computador;
import avilaos.so.EstadoComputador;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

/** Estado del computador: CPU, modo, RAM y swap, las seis colas del modelo de siete estados y el PCB de cada proceso. */
public class PanelComputador extends JPanel {
    private final Cluster cluster;
    private final int indice;
    private final JComboBox<TipoPolitica> cmbPolitica = new JComboBox<>(TipoPolitica.values());
    private boolean actualizando;

    private final JLabel lblProceso = new JLabel(), lblPC = new JLabel(), lblModo = new JLabel(), lblPrioDl = new JLabel(), lblSusp = new JLabel();
    private final JProgressBar barRam = new JProgressBar(), barSwap = new JProgressBar();
    private final JTextArea txtNuevos = area(), txtListos = area(), txtBloq = area(), txtTerm = area(), txtLS = area(), txtBS = area();
    private final ModeloTabla modeloPcb = new ModeloTabla("ID", "Nombre", "Estado / cola", "Tipo", "Prio", "Mem", "Deadline", "Restante", "PC", "Operación", "Ruta del archivo");
    private final String[] ultimo = new String[6];

    public PanelComputador(Cluster cluster, Computador comp) {
        this.cluster = cluster;
        this.indice = comp.getId();
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x3F72AF), 2),
                " " + comp.getNombre() + " ", 0, 0, new Font("SansSerif", Font.BOLD, 14)));

        JPanel norte = new JPanel(new GridLayout(0, 1, 2, 2));
        JPanel filaPol = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filaPol.add(new JLabel("Política de planificación de CPU:"));
        cmbPolitica.setSelectedItem(cluster.getConfig().getPolitica(indice));
        filaPol.add(cmbPolitica);
        cmbPolitica.addActionListener(e -> { if (!actualizando) cluster.cambiarPolitica(indice, (TipoPolitica) cmbPolitica.getSelectedItem()); });
        lblModo.setOpaque(true);
        lblModo.setFont(lblModo.getFont().deriveFont(Font.BOLD));
        barRam.setStringPainted(true);
        barSwap.setStringPainted(true);
        norte.add(filaPol);
        norte.add(lblProceso);
        norte.add(lblPC);
        norte.add(lblPrioDl);
        norte.add(lblModo);
        norte.add(barRam);
        norte.add(barSwap);
        norte.add(lblSusp);
        add(norte, BorderLayout.NORTH);

        JPanel colas = new JPanel(new GridLayout(3, 2, 4, 4));
        colas.add(conTitulo(txtNuevos, "Nuevos (cola a largo plazo)"));
        colas.add(conTitulo(txtListos, "Listos"));
        colas.add(conTitulo(txtBloq, "Bloqueados (motivo)"));
        colas.add(conTitulo(txtTerm, "Terminados"));
        colas.add(conTitulo(txtLS, "Listo / Suspendido (en swap)"));
        colas.add(conTitulo(txtBS, "Bloqueado / Suspendido (en swap)"));
        add(colas, BorderLayout.CENTER);

        JTable tabla = new JTable(modeloPcb);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(18);
        JScrollPane sp = new JScrollPane(tabla);
        sp.setBorder(BorderFactory.createTitledBorder("PCB de los procesos"));
        sp.setPreferredSize(new Dimension(400, 150));
        add(sp, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(520, 640));
    }

    private static JTextArea area() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        return a;
    }

    private static JScrollPane conTitulo(JTextArea a, String titulo) {
        JScrollPane sp = new JScrollPane(a);
        sp.setBorder(BorderFactory.createTitledBorder(titulo));
        return sp;
    }

    private static String texto(String[] lineas) {
        if (lineas.length == 0) return "(vacía)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lineas.length; i++) sb.append(i + 1).append(". ").append(lineas[i]).append('\n');
        return sb.toString();
    }

    private void poner(JTextArea a, int k, String t) {
        if (!t.equals(ultimo[k])) { a.setText(t); ultimo[k] = t; }
    }

    public void refrescar(EstadoComputador e) {
        actualizando = true;
        if (cmbPolitica.getSelectedItem() != e.tipoPolitica) cmbPolitica.setSelectedItem(e.tipoPolitica);
        actualizando = false;
        lblProceso.setText("  En CPU: " + e.proceso);
        lblPC.setText("  Program counter: " + e.pc + "   |   instrucción: " + e.instruccion);
        lblPrioDl.setText("  Prioridad: " + e.prioridad + "   |   Deadline restante: " + e.deadline);
        lblModo.setText("  Ejecutando: " + e.modo);
        lblModo.setBackground(e.enEjecucionSO ? new Color(0xFFD6A5) : new Color(0xB5EAD7));
        barRam.setMaximum(Math.max(1, e.ramTotal));
        barRam.setValue(e.ramUsada);
        barRam.setString("RAM: " + e.ramUsada + " usada / " + (e.ramTotal - e.ramUsada) + " libre (total " + e.ramTotal + ")");
        barSwap.setMaximum(Math.max(1, e.swapTotal));
        barSwap.setValue(e.swapUsado);
        barSwap.setString("Swap: " + e.swapUsado + " usado / " + (e.swapTotal - e.swapUsado) + " libre (total " + e.swapTotal + ")");
        lblSusp.setText("  Suspensiones: " + e.suspensiones + "   |   Reactivaciones: " + e.reactivaciones);
        poner(txtNuevos, 0, texto(e.nuevos));
        poner(txtListos, 1, texto(e.listos));
        poner(txtBloq, 2, texto(e.bloqueados));
        poner(txtTerm, 3, texto(e.terminados));
        poner(txtLS, 4, texto(e.listosSuspendidos));
        poner(txtBS, 5, texto(e.bloqueadosSuspendidos));
        modeloPcb.setDatos(e.pcb);
    }
}
