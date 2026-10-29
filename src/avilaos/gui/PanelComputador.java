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

/** Vista 2 de UN computador: CPU, modo, colas, RAM, política y PCB de sus procesos. Solo observa. */
public class PanelComputador extends JPanel {
    private final Cluster cluster;
    private final int indice;
    private final JComboBox<TipoPolitica> cmbPolitica = new JComboBox<>(TipoPolitica.values());
    private boolean actualizando;

    private final JLabel lblProceso = new JLabel(), lblPC = new JLabel(), lblModo = new JLabel(),
            lblPrioDl = new JLabel();
    private final JProgressBar barRam = new JProgressBar();
    private final JTextArea txtNuevos = areaCola(), txtListos = areaCola(), txtBloq = areaCola(), txtTerm = areaCola();
    private final ModeloTabla modeloPcb = new ModeloTabla("ID", "Nombre", "Estado / cola", "Tipo", "Prio", "Mem", "Deadline", "Restante", "PC");
    private String ultNuevos = "", ultListos = "", ultBloq = "", ultTerm = "";

    public PanelComputador(Cluster cluster, Computador comp) {
        this.cluster = cluster;
        this.indice = comp.getId();
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(0x3F72AF), 2),
                " " + comp.getNombre() + " ", 0, 0, new Font("SansSerif", Font.BOLD, 14)));

        JPanel norte = new JPanel(new GridLayout(0, 1, 2, 2));
        JPanel filaPol = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        filaPol.add(new JLabel("Política de planificación:"));
        filaPol.add(cmbPolitica);
        cmbPolitica.addActionListener(e -> {
            if (!actualizando) cluster.cambiarPolitica(indice, (TipoPolitica) cmbPolitica.getSelectedItem());
        });
        lblModo.setOpaque(true);
        lblModo.setFont(lblModo.getFont().deriveFont(Font.BOLD));
        barRam.setStringPainted(true);
        norte.add(filaPol);
        norte.add(lblProceso);
        norte.add(lblPC);
        norte.add(lblPrioDl);
        norte.add(lblModo);
        norte.add(barRam);
        add(norte, BorderLayout.NORTH);

        JPanel colas = new JPanel(new GridLayout(2, 2, 4, 4));
        colas.add(conTitulo(txtNuevos, "Nuevos (esperan RAM)"));
        colas.add(conTitulo(txtListos, "Listos (orden de selección)"));
        colas.add(conTitulo(txtBloq, "Bloqueados (motivo)"));
        colas.add(conTitulo(txtTerm, "Terminados"));
        add(colas, BorderLayout.CENTER);

        JTable tabla = new JTable(modeloPcb);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(18);
        JScrollPane sp = new JScrollPane(tabla);
        sp.setBorder(BorderFactory.createTitledBorder("PCB de los procesos de este computador"));
        sp.setPreferredSize(new Dimension(400, 170));
        add(sp, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(560, 620));
    }

    private static JTextArea areaCola() {
        JTextArea a = new JTextArea();
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
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

        String n = texto(e.nuevos), l = texto(e.listos), b = texto(e.bloqueados), t = texto(e.terminados);
        if (!n.equals(ultNuevos)) { txtNuevos.setText(ultNuevos = n); }
        if (!l.equals(ultListos)) { txtListos.setText(ultListos = l); }
        if (!b.equals(ultBloq)) { txtBloq.setText(ultBloq = b); }
        if (!t.equals(ultTerm)) { txtTerm.setText(ultTerm = t); }
        modeloPcb.setDatos(e.pcb);
    }
}
