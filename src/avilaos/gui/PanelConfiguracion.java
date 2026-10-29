package avilaos.gui;

import avilaos.modelo.TipoPolitica;
import avilaos.sistema.ConfigCSV;
import avilaos.sistema.ConfigSimulacion;
import avilaos.sistema.PerfilSimulacion;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/** Vista de configuración: número de computadores, RAM, política inicial, quantum, latencia y duración del ciclo. */
public class PanelConfiguracion extends JPanel {
    private final AplicacionAvila app;
    private final JSpinner spNum = new JSpinner(new SpinnerNumberModel(2, ConfigSimulacion.MIN_COMPUTADORES, ConfigSimulacion.MAX_COMPUTADORES, 1));
    private final JSpinner spMs = new JSpinner(new SpinnerNumberModel(300, 1, 10000, 10));
    private final JSpinner spQuantum = new JSpinner(new SpinnerNumberModel(4, 1, 1000, 1));
    private final JSpinner spLatencia = new JSpinner(new SpinnerNumberModel(3, 0, 1000, 1));
    private final JSpinner[] spRam = new JSpinner[ConfigSimulacion.MAX_COMPUTADORES];
    @SuppressWarnings("unchecked")
    private final JComboBox<TipoPolitica>[] cmbPol = new JComboBox[ConfigSimulacion.MAX_COMPUTADORES];
    private final JPanel filas = new JPanel(new GridLayout(0, 3, 8, 6));

    public PanelConfiguracion(AplicacionAvila app) {
        this.app = app;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        for (int i = 0; i < spRam.length; i++) {
            spRam[i] = new JSpinner(new SpinnerNumberModel(64, 8, 100000, 8));
            cmbPol[i] = new JComboBox<>(TipoPolitica.values());
            cmbPol[i].setSelectedItem(TipoPolitica.ROUND_ROBIN);
        }

        JPanel globales = new JPanel(new GridLayout(0, 2, 8, 6));
        globales.setBorder(BorderFactory.createTitledBorder("Parámetros globales"));
        globales.add(new JLabel("Número de computadores (2 a " + ConfigSimulacion.MAX_COMPUTADORES + "):"));
        globales.add(spNum);
        globales.add(new JLabel("Duración de un ciclo de reloj (ms):"));
        globales.add(spMs);
        globales.add(new JLabel("Quantum de Round Robin (ciclos):"));
        globales.add(spQuantum);
        globales.add(new JLabel("Latencia de red para buffers remotos (ciclos):"));
        globales.add(spLatencia);

        JPanel porComp = new JPanel(new BorderLayout());
        porComp.setBorder(BorderFactory.createTitledBorder("Cada computador"));
        porComp.add(filas, BorderLayout.NORTH);
        spNum.addChangeListener(e -> reconstruirFilas());
        reconstruirFilas();

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        JButton btnCrear = new JButton("Aplicar y crear sistema (reinicia la simulación)");
        JButton btnGuardar = new JButton("Guardar sistema actual en CSV…");
        JButton btnCargar = new JButton("Cargar configuración desde CSV…");
        btnCrear.addActionListener(e -> aplicar());
        btnGuardar.addActionListener(e -> guardar());
        btnCargar.addActionListener(e -> cargar());
        botones.add(btnCrear);
        botones.add(btnGuardar);
        botones.add(btnCargar);

        JPanel centro = new JPanel(new BorderLayout(10, 10));
        centro.add(globales, BorderLayout.NORTH);
        centro.add(porComp, BorderLayout.CENTER);
        add(centro, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
    }

    private void reconstruirFilas() {
        filas.removeAll();
        filas.add(new JLabel("Computador"));
        filas.add(new JLabel("Tamaño de la RAM"));
        filas.add(new JLabel("Política de planificación inicial"));
        int n = (Integer) spNum.getValue();
        for (int i = 0; i < n; i++) {
            filas.add(new JLabel("Computador " + (i + 1)));
            filas.add(spRam[i]);
            filas.add(cmbPol[i]);
        }
        filas.revalidate();
        filas.repaint();
    }

    public ConfigSimulacion leerConfig() {
        int n = (Integer) spNum.getValue();
        ConfigSimulacion c = new ConfigSimulacion(n);
        c.setMsPorCiclo((Integer) spMs.getValue());
        c.setQuantum((Integer) spQuantum.getValue());
        c.setLatenciaRed((Integer) spLatencia.getValue());
        for (int i = 0; i < n; i++) {
            c.setRam(i, (Integer) spRam[i].getValue());
            c.setPolitica(i, (TipoPolitica) cmbPol[i].getSelectedItem());
        }
        return c;
    }

    public void mostrarConfig(ConfigSimulacion c) {
        spNum.setValue(c.getNumComputadores());
        spMs.setValue(c.getMsPorCiclo());
        spQuantum.setValue(c.getQuantum());
        spLatencia.setValue(c.getLatenciaRed());
        for (int i = 0; i < c.getNumComputadores(); i++) {
            spRam[i].setValue(c.getRam(i));
            cmbPol[i].setSelectedItem(c.getPolitica(i));
        }
        reconstruirFilas();
    }

    private void aplicar() {
        int r = JOptionPane.showConfirmDialog(this, "Se creará un sistema nuevo y se perderán los procesos y buffers actuales. ¿Continuar?",
                "Reiniciar", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;
        PerfilSimulacion p = new PerfilSimulacion();
        p.config = leerConfig();
        app.reconstruir(p);
    }

    private void guardar() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("perfil_avilaos.csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            ConfigCSV.guardar(fc.getSelectedFile(), app.getCluster().getPerfil());
            JOptionPane.showMessageDialog(this, "Configuración guardada en " + fc.getSelectedFile().getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargar() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            PerfilSimulacion p = ConfigCSV.cargar(fc.getSelectedFile());
            mostrarConfig(p.config);
            app.reconstruir(p);
        } catch (IOException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Archivo inválido: " + ex.getMessage(), "Error al cargar", JOptionPane.ERROR_MESSAGE);
        }
    }
}
