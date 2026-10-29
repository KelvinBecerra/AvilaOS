package avilaos.gui;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.Proceso;
import avilaos.sistema.Cluster;
import avilaos.sistema.Registro;
import avilaos.so.Computador;
import avilaos.sync.BufferAcotado;
import avilaos.sync.SemaforoSO;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

/** Vista 2: estado de cada computador + estado global (ciclo, buffers con sus semáforos, log de eventos). */
public class PanelSimulacion extends JPanel {
    private final Cluster cluster;
    private final PanelComputador[] paneles;
    private final JLabel lblCiclo = new JLabel("Ciclo de reloj: 0");
    private final JButton btnPlay = new JButton("▶ Iniciar");
    private final JButton btnPaso = new JButton("⏭ Un ciclo");
    private final ModeloTabla modeloBuffers = new ModeloTabla("Buffer", "Anfitrión", "Elementos / capacidad",
            "mutex", "lleno", "vacío", "Esperan en mutex", "Esperan en lleno", "Esperan en vacío");
    private final JTextArea log = new JTextArea();
    private long logIdx;

    public PanelSimulacion(Cluster cluster) {
        this.cluster = cluster;
        setLayout(new BorderLayout(6, 6));

        // ---- barra de control
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        lblCiclo.setFont(lblCiclo.getFont().deriveFont(Font.BOLD, 18f));
        barra.add(lblCiclo);
        barra.add(btnPlay);
        barra.add(btnPaso);
        btnPlay.addActionListener(e -> alternar());
        btnPaso.addActionListener(e -> {
            if (cluster.isPausado()) cluster.paso();
        });
        JSpinner spMs = new JSpinner(new SpinnerNumberModel(cluster.getConfig().getMsPorCiclo(), 1, 10000, 10));
        spMs.addChangeListener(e -> cluster.getConfig().setMsPorCiclo((Integer) spMs.getValue()));
        JSpinner spQ = new JSpinner(new SpinnerNumberModel(cluster.getConfig().getQuantum(), 1, 1000, 1));
        spQ.addChangeListener(e -> cluster.getConfig().setQuantum((Integer) spQ.getValue()));
        JSpinner spLat = new JSpinner(new SpinnerNumberModel(cluster.getConfig().getLatenciaRed(), 0, 1000, 1));
        spLat.addChangeListener(e -> cluster.getConfig().setLatenciaRed((Integer) spLat.getValue()));
        barra.add(new JLabel("Duración del ciclo (ms):"));
        barra.add(spMs);
        barra.add(new JLabel("Quantum Round Robin:"));
        barra.add(spQ);
        barra.add(new JLabel("Latencia de red (ciclos):"));
        barra.add(spLat);
        add(barra, BorderLayout.NORTH);

        // ---- computadores
        Computador[] comps = cluster.getComputadores();
        paneles = new PanelComputador[comps.length];
        JPanel grid = new JPanel(new GridLayout(1, comps.length, 6, 6));
        for (int i = 0; i < comps.length; i++) {
            paneles[i] = new PanelComputador(cluster, comps[i]);
            grid.add(paneles[i]);
        }
        grid.setPreferredSize(new Dimension(570 * comps.length, 620));
        add(new JScrollPane(grid), BorderLayout.CENTER);

        // ---- global: buffers + log
        JTable tablaBuf = new JTable(modeloBuffers);
        tablaBuf.setFillsViewportHeight(true);
        JScrollPane spBuf = new JScrollPane(tablaBuf);
        spBuf.setBorder(BorderFactory.createTitledBorder("Buffers compartidos y sus semáforos"));
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane spLog = new JScrollPane(log);
        spLog.setBorder(BorderFactory.createTitledBorder("Log de eventos"));
        JSplitPane sur = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, spBuf, spLog);
        sur.setResizeWeight(0.45);
        sur.setPreferredSize(new Dimension(800, 190));
        add(sur, BorderLayout.SOUTH);
    }

    private void alternar() {
        if (cluster.isPausado()) { cluster.reanudar(); btnPlay.setText("⏸ Pausar"); }
        else { cluster.pausar(); btnPlay.setText("▶ Reanudar"); }
    }

    private static String nombres(ListaEnlazada<Proceso> l) {
        if (l.estaVacia()) return "-";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.tamano(); i++) { if (i > 0) sb.append(", "); sb.append(l.obtener(i).etiqueta()); }
        return sb.toString();
    }

    public void refrescar() {
        lblCiclo.setText("Ciclo de reloj: " + cluster.getCiclo());
        for (int i = 0; i < paneles.length; i++) paneles[i].refrescar(cluster.getComputador(i).getNucleo().instantanea());

        ListaEnlazada<BufferAcotado> bs = cluster.copiarBuffers();
        String[][] filas = new String[bs.tamano()][];
        for (int i = 0; i < filas.length; i++) {
            BufferAcotado b = bs.obtener(i);
            SemaforoSO m = b.getMutex(), n = b.getLlenos(), e = b.getVacios();
            filas[i] = new String[] { b.getNombre(), b.getHost().getNombre(), b.getCantidad() + " / " + b.getCapacidad(),
                    String.valueOf(m.getValor()), String.valueOf(n.getValor()), String.valueOf(e.getValor()),
                    nombres(m.copiarCola()), nombres(n.copiarCola()), nombres(e.copiarCola()) };
        }
        modeloBuffers.setDatos(filas);

        Registro.Lectura l = cluster.getRegistro().leerDesde(logIdx);
        if (!l.texto.isEmpty()) {
            log.append(l.texto);
            log.setCaretPosition(log.getDocument().getLength());
        }
        logIdx = l.siguiente;
    }
}
