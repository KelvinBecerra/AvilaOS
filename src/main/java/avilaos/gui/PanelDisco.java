package avilaos.gui;

import avilaos.archivos.InstantaneaFS;
import avilaos.estructuras.ListaEnlazada;
import avilaos.hardware.Disco;
import avilaos.modelo.SolicitudDisco;
import avilaos.modelo.TipoPoliticaDisco;
import avilaos.sistema.Cluster;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;

/** Vista del disco: mapa de bloques con el cabezal, cola de E/S con la operación de cada solicitud y política con selector. */
public class PanelDisco extends JPanel {
    private final Cluster cluster;
    private final MapaDisco mapa = new MapaDisco();
    private final JComboBox<TipoPoliticaDisco> cmbPolitica = new JComboBox<>(TipoPoliticaDisco.values());
    private final JCheckBox chkRetener = new JCheckBox("Retener el disco (acumula solicitudes)");
    private final JLabel lblEstado = new JLabel(), lblSwap = new JLabel();
    private final ModeloTabla modeloCola = new ModeloTabla("Orden", "Proceso", "Operación", "Archivo / área", "Bloque", "Llegó (ciclo)");
    private boolean actualizando;

    public PanelDisco(Cluster cluster) {
        this.cluster = cluster;
        Disco d = cluster.getUnico().getDisco();
        setLayout(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new java.awt.Color(0xC05621), 2), " Disco local ", 0, 0, new Font("SansSerif", Font.BOLD, 14)));

        JPanel norte = new JPanel(new java.awt.GridLayout(0, 1, 2, 2));
        JPanel fila1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        fila1.add(new JLabel("Política de disco:"));
        cmbPolitica.setSelectedItem(d.getPolitica().tipo());
        cmbPolitica.addActionListener(e -> { if (!actualizando) d.setPolitica((TipoPoliticaDisco) cmbPolitica.getSelectedItem()); });
        fila1.add(cmbPolitica);
        fila1.add(new JLabel("Cabezal (posiciones por ciclo):"));
        JSpinner spVel = new JSpinner(new SpinnerNumberModel(d.getVelocidad(), 1, 500, 1));
        spVel.addChangeListener(e -> d.setVelocidad((Integer) spVel.getValue()));
        fila1.add(spVel);
        chkRetener.addActionListener(e -> d.setRetenido(chkRetener.isSelected()));
        fila1.add(chkRetener);
        norte.add(fila1);
        norte.add(lblEstado);
        norte.add(lblSwap);
        add(norte, BorderLayout.NORTH);

        JScrollPane spMapa = new JScrollPane(mapa);
        spMapa.setBorder(BorderFactory.createTitledBorder("Mapa de bloques (pase el mouse sobre un bloque; punto naranja = solicitud pendiente)"));
        add(spMapa, BorderLayout.CENTER);

        JTable tabla = new JTable(modeloCola);
        tabla.setFillsViewportHeight(true);
        JScrollPane spCola = new JScrollPane(tabla);
        spCola.setBorder(BorderFactory.createTitledBorder("Cola de E/S del disco"));
        spCola.setPreferredSize(new Dimension(300, 170));
        add(spCola, BorderLayout.SOUTH);
    }

    public void refrescar(InstantaneaFS fs) {
        Disco d = cluster.getUnico().getDisco();
        ListaEnlazada<SolicitudDisco> cola = d.copiarCola();
        SolicitudDisco act = d.getActual();
        int extra = act != null ? 1 : 0;
        String[][] filas = new String[cola.tamano() + extra][];
        int[] pend = new int[filas.length];
        if (act != null) { filas[0] = fila("atendiendo", act); pend[0] = act.getBloque(); }
        for (int i = 0; i < cola.tamano(); i++) { filas[i + extra] = fila(String.valueOf(i + 1), cola.obtener(i)); pend[i + extra] = cola.obtener(i).getBloque(); }
        modeloCola.setDatos(filas);

        actualizando = true;
        if (cmbPolitica.getSelectedItem() != d.getPolitica().tipo()) cmbPolitica.setSelectedItem(d.getPolitica().tipo());
        if (chkRetener.isSelected() != d.isRetenido()) chkRetener.setSelected(d.isRetenido());
        actualizando = false;
        lblEstado.setText("  Cabezal en el bloque " + d.getCabezal() + "   |   dirección: " + d.getDireccion()
                + "   |   atendiendo: " + (act == null ? "nada" : act.etiqueta()));
        int[] duenos = cluster.getUnico().getSwap().copiarDuenos();
        int usados = 0;
        for (int x : duenos) if (x != 0) usados++;
        lblSwap.setText("  Swap: " + usados + " / " + duenos.length + " bloques ocupados   |   desplazamiento total del cabezal: " + d.getDesplazamientoTotal() + " bloques");
        mapa.actualizar(fs, d.getCabezal(), d.getDireccion(), duenos, pend);
    }

    private static String[] fila(String orden, SolicitudDisco s) {
        return new String[] { orden, s.getProceso() == null ? "SO" : s.getProceso().etiqueta() + " " + s.getProceso().getNombre(),
                s.getOperacion().getTexto(), s.getRuta(), String.valueOf(s.getBloque()), String.valueOf(s.getCicloLlegada()) };
    }
}
