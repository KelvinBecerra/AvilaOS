package avilaos.gui;

import avilaos.sistema.Cluster;
import avilaos.sistema.ConfigSimulacion;
import avilaos.sistema.PerfilSimulacion;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.Timer;
import java.awt.BorderLayout;

/**
 * Contenedor de la aplicación. La GUI solo OBSERVA al Cluster (lee instantáneas con un Timer)
 * y le envía órdenes (crear, pausar, cambiar política); no contiene lógica del sistema operativo.
 */
public class AplicacionAvila extends JPanel {
    private Cluster cluster;
    private final JTabbedPane pestanas = new JTabbedPane();
    private final PanelConfiguracion pConfig;
    private PanelCreacion pCrear;
    private PanelSimulacion pSim;
    private PanelMetricas pMetricas;
    private final Timer refresco;

    public AplicacionAvila() {
        setLayout(new BorderLayout());
        pConfig = new PanelConfiguracion(this);
        add(pestanas, BorderLayout.CENTER);
        PerfilSimulacion inicial = new PerfilSimulacion();
        inicial.config = new ConfigSimulacion(2);
        reconstruir(inicial);
        refresco = new Timer(200, e -> refrescar());
        refresco.start();
    }

    public Cluster getCluster() { return cluster; }

    /** Detiene el sistema actual y levanta uno nuevo con el perfil dado (config + buffers + procesos iniciales). */
    public final void reconstruir(PerfilSimulacion perfil) {
        if (cluster != null) cluster.detener();
        cluster = new Cluster(perfil.config);

        StringBuilder errores = new StringBuilder();
        for (int i = 0; i < perfil.buffers.tamano(); i++) {
            try {
                var b = perfil.buffers.obtener(i);
                cluster.crearBuffer(b.nombre, b.capacidad, b.host);
            } catch (IllegalArgumentException ex) { errores.append("Buffer: ").append(ex.getMessage()).append('\n'); }
        }
        for (int i = 0; i < perfil.procesos.tamano(); i++) {
            try { cluster.crearProceso(perfil.procesos.obtener(i)); }
            catch (IllegalArgumentException ex) { errores.append("Proceso: ").append(ex.getMessage()).append('\n'); }
        }

        pCrear = new PanelCreacion(cluster);
        pCrear.sincronizar();
        pSim = new PanelSimulacion(cluster);
        pMetricas = new PanelMetricas(cluster);
        pestanas.removeAll();
        pestanas.addTab("1. Configuración", pConfig);
        pestanas.addTab("2. Crear procesos y buffers (Vista 1)", pCrear);
        pestanas.addTab("3. Simulación (Vista 2)", pSim);
        pestanas.addTab("4. Métricas", pMetricas);
        pestanas.setSelectedIndex(perfil.procesos.estaVacia() ? 1 : 2);
        pSim.refrescar();
        if (errores.length() > 0)
            JOptionPane.showMessageDialog(this, "Algunos elementos no se pudieron crear:\n" + errores,
                    "Advertencia", JOptionPane.WARNING_MESSAGE);
    }

    public void seleccionarPestana(int i) { pestanas.setSelectedIndex(i); }

    public void refrescar() {
        Object sel = pestanas.getSelectedComponent();
        if (sel == pSim) pSim.refrescar();
        else if (sel == pMetricas) pMetricas.refrescar();
    }

    public void detener() {
        refresco.stop();
        if (cluster != null) cluster.detener();
    }
}
