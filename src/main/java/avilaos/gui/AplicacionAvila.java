package avilaos.gui;

import avilaos.archivos.FsJson;
import avilaos.archivos.NodoVista;
import avilaos.archivos.PerfilFS;
import avilaos.modelo.ModoUsuario;
import avilaos.sistema.Cluster;
import avilaos.sistema.PerfilProcesos;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Contenedor de la aplicación. La GUI solo OBSERVA al Cluster (lee instantáneas con un Timer) y le envía órdenes (crear
 * procesos de E/S, pausar, cambiar políticas); no contiene lógica del sistema operativo ni opera directamente sobre el disco.
 */
public class AplicacionAvila extends JPanel {
    private Cluster cluster;
    private final ContextoUsuario ctx = new ContextoUsuario();
    private final JTabbedPane pestanas = new JTabbedPane();
    private final PanelCarga pCarga = new PanelCarga(this);
    private PanelSimulacion pSim;
    private PanelArchivos pArch;
    private PanelMetricas pMet;
    private final JComboBox<ModoUsuario> cmbModo = new JComboBox<>(ModoUsuario.values());
    private final JComboBox<String> cmbUsuario = new JComboBox<>();
    private final Timer refresco;

    public AplicacionAvila() {
        setLayout(new BorderLayout());
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        barra.add(new JLabel("Modo de uso:"));
        barra.add(cmbModo);
        barra.add(new JLabel("Usuario activo:"));
        barra.add(cmbUsuario);
        barra.add(new JLabel("   (Administrador: todas las operaciones · Usuario: lee archivos propios o públicos y opera solo sobre los propios)"));
        cmbModo.addActionListener(e -> {
            ctx.setModo((ModoUsuario) cmbModo.getSelectedItem());
            cmbUsuario.setEnabled(ctx.getModo() == ModoUsuario.USUARIO);
        });
        cmbUsuario.addActionListener(e -> { if (cmbUsuario.getSelectedItem() != null) ctx.setUsuario((String) cmbUsuario.getSelectedItem()); });
        cmbUsuario.setEnabled(false);
        cmbUsuario.addItem("user1");
        add(barra, BorderLayout.NORTH);
        add(pestanas, BorderLayout.CENTER);
        pestanas.addTab("1. Carga inicial (JSON)", pCarga);
        refresco = new Timer(200, e -> refrescar());
        refresco.start();
    }

    public Cluster getCluster() { return cluster; }
    public ContextoUsuario getContexto() { return ctx; }

    /** Crea el sistema a partir de los dos JSON (reloj en pausa) y muestra las demás pestañas. */
    public void iniciar(PerfilFS pf, PerfilProcesos pp) {
        Cluster nuevo = Cluster.desdePerfiles(pf, pp);       // puede lanzar IllegalArgumentException: el sistema anterior sigue intacto
        if (cluster != null) cluster.detener();
        cluster = nuevo;
        pSim = new PanelSimulacion(cluster);
        pArch = new PanelArchivos(cluster, ctx);
        pMet = new PanelMetricas(cluster);
        while (pestanas.getTabCount() > 1) pestanas.removeTabAt(1);
        pestanas.addTab("2. Simulación: computador y disco (Vista 2)", pSim);
        pestanas.addTab("3. Archivos y procesos de E/S (Vista 1)", pArch);
        pestanas.addTab("4. Métricas", pMet);
        cargarUsuarios();
        pSim.refrescar();
        pestanas.setSelectedIndex(1);
    }

    /** Usuarios conocidos = dueños que aparecen en el sistema de archivos (sin el administrador). */
    private void cargarUsuarios() {
        String[] duenos = new String[16];
        int[] n = { 0 };
        reunir(cluster.getUnico().getFs().instantanea().raiz, duenos, n);
        cmbUsuario.removeAllItems();
        for (int i = 0; i < n[0]; i++) if (!duenos[i].equals("admin")) cmbUsuario.addItem(duenos[i]);
        if (cmbUsuario.getItemCount() == 0) { cmbUsuario.addItem("user1"); cmbUsuario.addItem("user2"); }
        ctx.setUsuario((String) cmbUsuario.getSelectedItem());
    }

    private static void reunir(NodoVista nodo, String[] duenos, int[] n) {
        boolean esta = false;
        for (int i = 0; i < n[0]; i++) if (duenos[i].equals(nodo.owner)) esta = true;
        if (!esta && n[0] < duenos.length) duenos[n[0]++] = nodo.owner;
        for (NodoVista h : nodo.hijos) reunir(h, duenos, n);
    }

    public String estadoFsJson() {
        return cluster.getUnico().getFs().aJson(cluster.getUnico().getDisco().getCabezal());
    }

    public void guardarEstadoFs(Component padre) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Archivos JSON", "json"));
        fc.setSelectedFile(new File("sistema_archivos_guardado.json"));
        if (fc.showSaveDialog(padre) != JFileChooser.APPROVE_OPTION) return;
        try {
            Files.writeString(fc.getSelectedFile().toPath(), estadoFsJson(), StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(padre, "Estado del sistema de archivos guardado en " + fc.getSelectedFile().getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(padre, "No se pudo guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refrescar() {
        if (cluster == null) return;
        Object sel = pestanas.getSelectedComponent();
        if (sel == pSim) pSim.refrescar();
        else if (sel == pArch) pArch.refrescar(cluster.getUnico().getFs().instantanea());
        else if (sel == pMet) pMet.refrescar();
    }

    public void seleccionarPestana(int i) { pestanas.setSelectedIndex(i); }

    public void detener() {
        refresco.stop();
        if (cluster != null) cluster.detener();
    }
}
