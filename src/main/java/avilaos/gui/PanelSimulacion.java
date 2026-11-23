package avilaos.gui;

import avilaos.archivos.InstantaneaFS;
import avilaos.sistema.Cluster;
import avilaos.sistema.Registro;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

/** Vista 2: estado del computador (izquierda) y del disco (derecha), controles de la simulación y log de eventos. */
public class PanelSimulacion extends JPanel {
    private final Cluster cluster;
    private final PanelComputador panelComputador;
    private final PanelDisco panelDisco;
    private final JLabel lblCiclo = new JLabel("Ciclo de reloj: 0");
    private final JButton btnPlay = new JButton("▶ Iniciar");
    private final JTextArea log = new JTextArea();
    private long logIdx;

    public PanelSimulacion(Cluster cluster) {
        this.cluster = cluster;
        setLayout(new BorderLayout(6, 6));

        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        lblCiclo.setFont(lblCiclo.getFont().deriveFont(Font.BOLD, 18f));
        barra.add(lblCiclo);
        barra.add(btnPlay);
        JButton btnPaso = new JButton("⏭ Un ciclo");
        barra.add(btnPaso);
        btnPlay.addActionListener(e -> alternar());
        btnPaso.addActionListener(e -> { if (cluster.isPausado()) cluster.paso(); });
        JSpinner spMs = new JSpinner(new SpinnerNumberModel(cluster.getConfig().getMsPorCiclo(), 1, 10000, 10));
        spMs.addChangeListener(e -> cluster.getConfig().setMsPorCiclo((Integer) spMs.getValue()));
        JSpinner spQ = new JSpinner(new SpinnerNumberModel(cluster.getConfig().getQuantum(), 1, 1000, 1));
        spQ.addChangeListener(e -> cluster.getConfig().setQuantum((Integer) spQ.getValue()));
        barra.add(new JLabel("Duración del ciclo (ms):"));
        barra.add(spMs);
        barra.add(new JLabel("Quantum Round Robin:"));
        barra.add(spQ);
        add(barra, BorderLayout.NORTH);

        panelComputador = new PanelComputador(cluster, cluster.getUnico());
        panelDisco = new PanelDisco(cluster);
        JSplitPane centro = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelComputador, panelDisco);
        centro.setResizeWeight(0.48);
        add(centro, BorderLayout.CENTER);

        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane spLog = new JScrollPane(log);
        spLog.setBorder(BorderFactory.createTitledBorder("Log de eventos (decisiones del planificador de CPU, del disco y del planificador a mediano plazo)"));
        spLog.setPreferredSize(new Dimension(800, 150));
        add(spLog, BorderLayout.SOUTH);
    }

    private void alternar() {
        if (cluster.isPausado()) { cluster.reanudar(); btnPlay.setText("⏸ Pausar"); }
        else { cluster.pausar(); btnPlay.setText("▶ Reanudar"); }
    }

    public void refrescar() {
        lblCiclo.setText("Ciclo de reloj: " + cluster.getCiclo());
        panelComputador.refrescar(cluster.getUnico().getNucleo().instantanea());
        InstantaneaFS fs = cluster.getUnico().getFs().instantanea();
        panelDisco.refrescar(fs);
        Registro.Lectura l = cluster.getRegistro().leerDesde(logIdx);
        if (!l.texto.isEmpty()) {
            log.append(l.texto);
            log.setCaretPosition(log.getDocument().getLength());
        }
        logIdx = l.siguiente;
    }
}
