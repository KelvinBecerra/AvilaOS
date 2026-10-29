package avilaos.gui;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.Proceso;
import avilaos.modelo.TipoProceso;
import avilaos.sistema.Cluster;
import avilaos.sistema.EspecProceso;
import avilaos.sync.BufferAcotado;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/** Vista 1: creación de procesos y buffers por el usuario, con validación de tipo y rango en cada campo. */
public class PanelCreacion extends JPanel {
    private final Cluster cluster;

    private final JTextField txtNombre = new JTextField("Proceso", 14);
    private final JComboBox<TipoProceso> cmbTipo = new JComboBox<>(TipoProceso.values());
    private final JTextField txtInstr = new JTextField("20", 8);
    private final JTextField txtMem = new JTextField("8", 8);
    private final JTextField txtPrio = new JTextField("3", 8);
    private final JTextField txtDeadline = new JTextField("200", 8);
    private final JComboBox<String> cmbComp = new JComboBox<>();
    private final JTextField txtPeriodoES = new JTextField("5", 8);
    private final JTextField txtCiclosES = new JTextField("4", 8);
    private final JComboBox<String> cmbBuffer = new JComboBox<>();
    private final JTextField txtPeriodo = new JTextField("2", 8);
    private final JTextField txtElementos = new JTextField("6", 8);

    private final JTextField txtBufNombre = new JTextField("B1", 10);
    private final JTextField txtBufCap = new JTextField("3", 8);
    private final JComboBox<String> cmbHost = new JComboBox<>();

    private final JTextArea txtCreados = new JTextArea();

    public PanelCreacion(Cluster cluster) {
        this.cluster = cluster;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        cmbComp.addItem("Automático (ÁvilaOS balancea la carga)");
        for (int i = 0; i < cluster.getComputadores().length; i++) {
            cmbComp.addItem(cluster.getComputador(i).getNombre());
            cmbHost.addItem(cluster.getComputador(i).getNombre());
        }
        cmbTipo.addActionListener(e -> ajustarCampos());

        // ----- formulario de proceso
        JPanel fp = new JPanel(new GridBagLayout());
        fp.setBorder(BorderFactory.createTitledBorder("Nuevo proceso"));
        int r = 0;
        fila(fp, r++, "Nombre:", txtNombre);
        fila(fp, r++, "Tipo:", cmbTipo);
        fila(fp, r++, "Cantidad de instrucciones (CPU/IO bound):", txtInstr);
        fila(fp, r++, "Memoria requerida:", txtMem);
        fila(fp, r++, "Prioridad (1 = máxima ... 10):", txtPrio);
        fila(fp, r++, "Deadline (ciclos desde su creación):", txtDeadline);
        fila(fp, r++, "Computador:", cmbComp);
        fila(fp, r++, "I/O bound: pide E/S cada N instrucciones:", txtPeriodoES);
        fila(fp, r++, "I/O bound: ciclos que dura cada E/S:", txtCiclosES);
        fila(fp, r++, "Prod./Cons.: buffer:", cmbBuffer);
        fila(fp, r++, "Prod./Cons.: cada cuántos ciclos produce/consume:", txtPeriodo);
        fila(fp, r++, "Prod./Cons.: elementos para terminar:", txtElementos);
        JButton btnProc = new JButton("Crear proceso");
        btnProc.addActionListener(e -> crearProceso());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = r; gc.gridwidth = 2; gc.insets = new Insets(8, 4, 4, 4);
        fp.add(btnProc, gc);

        // ----- formulario de buffer
        JPanel fb = new JPanel(new GridBagLayout());
        fb.setBorder(BorderFactory.createTitledBorder("Nuevo buffer (ocupa 1 unidad de RAM por posición en su anfitrión)"));
        fila(fb, 0, "Nombre:", txtBufNombre);
        fila(fb, 1, "Capacidad:", txtBufCap);
        fila(fb, 2, "Computador anfitrión:", cmbHost);
        JButton btnBuf = new JButton("Crear buffer");
        btnBuf.addActionListener(e -> crearBuffer());
        JButton btnEj = new JButton("Cargar ejemplo de demostración");
        btnEj.addActionListener(e -> cargarEjemplo());
        GridBagConstraints g2 = new GridBagConstraints();
        g2.gridx = 0; g2.gridy = 3; g2.gridwidth = 2; g2.insets = new Insets(8, 4, 4, 4);
        fb.add(btnBuf, g2);
        g2.gridy = 4;
        fb.add(btnEj, g2);

        txtCreados.setEditable(false);
        JScrollPane spCreados = new JScrollPane(txtCreados);
        spCreados.setBorder(BorderFactory.createTitledBorder("Creados en esta sesión"));

        JPanel der = new JPanel(new BorderLayout(6, 6));
        der.add(fb, BorderLayout.NORTH);
        der.add(spCreados, BorderLayout.CENTER);
        add(fp, BorderLayout.WEST);
        add(der, BorderLayout.CENTER);
        ajustarCampos();
    }

    private static void fila(JPanel p, int fila, String etiqueta, JComponent c) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = fila; g.insets = new Insets(3, 4, 3, 4); g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; p.add(new JLabel(etiqueta), g);
        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; p.add(c, g);
    }

    private void ajustarCampos() {
        TipoProceso t = (TipoProceso) cmbTipo.getSelectedItem();
        boolean io = t == TipoProceso.IO_BOUND, buf = t.esDeBuffer();
        txtInstr.setEnabled(!buf);
        txtPeriodoES.setEnabled(io);
        txtCiclosES.setEnabled(io);
        cmbBuffer.setEnabled(buf);
        txtPeriodo.setEnabled(buf);
        txtElementos.setEnabled(buf);
    }

    /** Se llama cuando se crea un buffer para que los combos de proceso lo ofrezcan. */
    private void refrescarBuffers() {
        cmbBuffer.removeAllItems();
        ListaEnlazada<BufferAcotado> bs = cluster.copiarBuffers();
        for (int i = 0; i < bs.tamano(); i++) cmbBuffer.addItem(bs.obtener(i).getNombre());
    }

    public void sincronizar() { refrescarBuffers(); }

    // ---------------------------------------------------------------- validación

    private static int entero(JTextField f, String campo, int min, int max) {
        String t = f.getText().trim();
        if (t.isEmpty()) throw new IllegalArgumentException("El campo '" + campo + "' está vacío.");
        int v;
        try { v = Integer.parseInt(t); }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + campo + "' debe ser un número entero (escribió '" + t + "').");
        }
        if (v < min || v > max)
            throw new IllegalArgumentException("'" + campo + "' debe estar entre " + min + " y " + max + " (escribió " + v + ").");
        return v;
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Dato inválido", JOptionPane.ERROR_MESSAGE);
    }

    private void crearProceso() {
        try {
            EspecProceso e = new EspecProceso();
            e.nombre = txtNombre.getText().trim();
            e.tipo = (TipoProceso) cmbTipo.getSelectedItem();
            e.memoria = entero(txtMem, "Memoria", 1, 100000);
            e.prioridad = entero(txtPrio, "Prioridad", 1, 10);
            e.deadline = entero(txtDeadline, "Deadline", 1, 1000000);
            e.computador = cmbComp.getSelectedIndex() - 1;
            if (e.tipo == TipoProceso.CPU_BOUND || e.tipo == TipoProceso.IO_BOUND)
                e.instrucciones = entero(txtInstr, "Instrucciones", 1, 1000000);
            if (e.tipo == TipoProceso.IO_BOUND) {
                e.periodoES = entero(txtPeriodoES, "Cada cuántas instrucciones pide E/S", 1, 1000000);
                e.ciclosES = entero(txtCiclosES, "Ciclos de E/S", 1, 1000000);
            }
            if (e.tipo.esDeBuffer()) {
                if (cmbBuffer.getSelectedItem() == null) throw new IllegalArgumentException("Primero cree un buffer.");
                e.buffer = (String) cmbBuffer.getSelectedItem();
                e.periodo = entero(txtPeriodo, "Ciclos por elemento", 1, 100000);
                e.elementos = entero(txtElementos, "Elementos", 1, 1000000);
            }
            Proceso p = cluster.crearProceso(e);
            txtCreados.append("Proceso " + p.etiqueta() + " '" + p.getNombre() + "' (" + p.getTipo() + ") → "
                    + p.getComputador().getNombre() + "\n");
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private void crearBuffer() {
        try {
            int cap = entero(txtBufCap, "Capacidad", 1, 1000);
            BufferAcotado b = cluster.crearBuffer(txtBufNombre.getText().trim(), cap, cmbHost.getSelectedIndex());
            txtCreados.append("Buffer " + b.getNombre() + " (capacidad " + cap + ") en " + b.getHost().getNombre() + "\n");
            refrescarBuffers();
            cmbBuffer.setSelectedItem(b.getNombre());
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private EspecProceso espec(String n, TipoProceso t, int ins, int mem, int prio, int dl, int comp,
                               String buf, int per, int elem, int periodoES, int ciclosES) {
        EspecProceso e = new EspecProceso();
        e.nombre = n; e.tipo = t; e.instrucciones = ins; e.memoria = mem; e.prioridad = prio; e.deadline = dl;
        e.computador = comp; e.buffer = buf; e.periodo = per; e.elementos = elem; e.periodoES = periodoES; e.ciclosES = ciclosES;
        return e;
    }

    private void cargarEjemplo() {
        try {
            int ultimo = cluster.getComputadores().length - 1;
            cluster.crearBuffer("B1", 3, 0);
            refrescarBuffers();
            EspecProceso[] ej = {
                espec("Productor-A", TipoProceso.PRODUCTOR, 0, 4, 2, 600, 0, "B1", 2, 8, 0, 0),
                espec("Productor-B", TipoProceso.PRODUCTOR, 0, 4, 2, 600, ultimo, "B1", 3, 6, 0, 0),
                espec("Consumidor-A", TipoProceso.CONSUMIDOR, 0, 4, 3, 600, 0, "B1", 2, 7, 0, 0),
                espec("Consumidor-B", TipoProceso.CONSUMIDOR, 0, 4, 3, 600, ultimo, "B1", 3, 7, 0, 0),
                espec("Calculo", TipoProceso.CPU_BOUND, 25, 8, 4, 300, 0, "", 1, 1, 0, 0),
                espec("Navegador", TipoProceso.IO_BOUND, 18, 8, 1, 200, ultimo, "", 1, 1, 4, 5),
                espec("Compilador", TipoProceso.CPU_BOUND, 30, 10, 5, 120, -1, "", 1, 1, 0, 0) };
            for (EspecProceso e : ej) {
                Proceso p = cluster.crearProceso(e);
                txtCreados.append("Proceso " + p.etiqueta() + " '" + p.getNombre() + "' (" + p.getTipo() + ") → "
                        + p.getComputador().getNombre() + "\n");
            }
        } catch (IllegalArgumentException ex) {
            error("No se pudo cargar el ejemplo: " + ex.getMessage());
        }
    }
}
