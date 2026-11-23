package avilaos.gui;

import avilaos.archivos.FsJson;
import avilaos.archivos.PerfilFS;
import avilaos.sistema.PerfilProcesos;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Carga inicial. Para garantizar la consistencia, el sistema solo se crea (y el reloj solo puede arrancar) cuando se
 * cargaron AMBOS JSON: el del sistema de archivos (inicializa el disco) y el de procesos y configuración.
 */
public class PanelCarga extends JPanel {
    private final AplicacionAvila app;
    private PerfilFS perfilFs;
    private PerfilProcesos perfilProc;
    private final JLabel lblFs = new JLabel("Sin cargar"), lblProc = new JLabel("Sin cargar");
    private final JButton btnIniciar = new JButton("Crear el sistema con estos archivos");
    private final JButton btnGuardar = new JButton("Guardar el estado del sistema de archivos (JSON)…");
    private final JCheckBox chkConservar = new JCheckBox("Conservar el estado actual del disco (de la ejecución en curso) en lugar del JSON del sistema de archivos");

    public PanelCarga(AplicacionAvila app) {
        this.app = app;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JTextArea ayuda = new JTextArea(
            "ÁvilaOS necesita DOS archivos JSON antes de poder iniciar el reloj:\n"
          + "  1) el estado del sistema de archivos (inicializa el disco: bloques, archivos, directorios y posición del cabezal);\n"
          + "  2) los procesos y la configuración (política de CPU y de disco, RAM, área de intercambio, duración del ciclo).\n"
          + "Si arrancara sin el sistema de archivos, los procesos de E/S fallarían al buscar rutas que no existen; sin los procesos, el disco y la RAM quedarían vacíos.\n"
          + "Separar la carga permite reiniciar con nuevos procesos conservando el disco modificado de una ejecución anterior (guarde su estado en JSON).");
        ayuda.setEditable(false);
        ayuda.setOpaque(false);
        ayuda.setFont(new Font("SansSerif", Font.PLAIN, 13));
        add(ayuda, BorderLayout.NORTH);

        JPanel centro = new JPanel(new java.awt.GridLayout(0, 1, 8, 8));
        centro.add(fila("1) Sistema de archivos:", "Cargar JSON del sistema de archivos…", lblFs, e -> cargarFs()));
        centro.add(fila("2) Procesos y configuración:", "Cargar JSON de procesos…", lblProc, e -> cargarProc()));
        JPanel ej = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        JButton btnEj = new JButton("Cargar el ejemplo del enunciado (ambos archivos)");
        btnEj.addActionListener(e -> ejemplo());
        ej.add(btnEj);
        centro.add(ej);
        chkConservar.setEnabled(false);
        centro.add(chkConservar);
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        btnIniciar.setEnabled(false);
        btnIniciar.addActionListener(e -> iniciar());
        btnGuardar.setEnabled(false);
        btnGuardar.addActionListener(e -> guardar());
        acciones.add(btnIniciar);
        acciones.add(btnGuardar);
        centro.add(acciones);
        JPanel envoltura = new JPanel(new BorderLayout());
        envoltura.add(centro, BorderLayout.NORTH);
        add(envoltura, BorderLayout.CENTER);
    }

    private JPanel fila(String titulo, String boton, JLabel estado, java.awt.event.ActionListener al) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        JLabel t = new JLabel(titulo);
        t.setPreferredSize(new Dimension(190, 20));
        t.setFont(t.getFont().deriveFont(Font.BOLD));
        JButton b = new JButton(boton);
        b.addActionListener(al);
        p.add(t);
        p.add(b);
        p.add(estado);
        return p;
    }

    private String elegirYLeer() throws Exception {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Archivos JSON", "json"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return null;
        File f = fc.getSelectedFile();
        return Files.readString(f.toPath(), StandardCharsets.UTF_8);
    }

    private void error(String titulo, Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), titulo, JOptionPane.ERROR_MESSAGE);
    }

    private void cargarFs() {
        try {
            String t = elegirYLeer();
            if (t == null) return;
            ponerFs(FsJson.leer(t));
        } catch (Exception ex) { error("JSON del sistema de archivos inválido", ex); }
    }

    private void cargarProc() {
        try {
            String t = elegirYLeer();
            if (t == null) return;
            ponerProc(PerfilProcesos.leer(t));
        } catch (Exception ex) { error("JSON de procesos inválido", ex); }
    }

    private void ponerFs(PerfilFS p) {
        perfilFs = p;
        lblFs.setText("✔ " + p.totalBloques + " bloques, cabezal en " + p.cabezalInicial + ", " + p.entradas.tamano() + " elementos");
        habilitar();
    }

    private void ponerProc(PerfilProcesos p) {
        perfilProc = p;
        lblProc.setText("✔ " + p.procesos.tamano() + " procesos, RAM " + p.ram + ", swap " + p.bloquesSwap + ", CPU " + p.politicaCPU + ", disco " + p.politicaDisco);
        habilitar();
    }

    private void habilitar() { btnIniciar.setEnabled(perfilProc != null && (perfilFs != null || (chkConservar.isSelected() && app.getCluster() != null))); }

    private String recurso(String n) throws Exception {
        try (InputStream in = PanelCarga.class.getResourceAsStream("/ejemplos/" + n)) {
            if (in == null) throw new IllegalStateException("No se encontró el ejemplo " + n);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void ejemplo() {
        try {
            ponerFs(FsJson.leer(recurso("sistema_archivos.json")));
            ponerProc(PerfilProcesos.leer(recurso("procesos.json")));
        } catch (Exception ex) { error("No se pudo cargar el ejemplo", ex); }
    }

    private void iniciar() {
        try {
            PerfilFS pf = perfilFs;
            if (chkConservar.isSelected() && app.getCluster() != null) pf = FsJson.leer(app.estadoFsJson());
            if (pf == null || perfilProc == null) throw new IllegalArgumentException("Faltan archivos por cargar");
            app.iniciar(pf, perfilProc);
            chkConservar.setEnabled(true);
            chkConservar.addActionListener(e -> habilitar());
            btnGuardar.setEnabled(true);
        } catch (IllegalArgumentException ex) { error("No se pudo crear el sistema", ex); }
    }

    private void guardar() { app.guardarEstadoFs(this); }
}
