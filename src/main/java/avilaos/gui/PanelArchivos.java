package avilaos.gui;

import avilaos.archivos.InstantaneaFS;
import avilaos.archivos.NodoVista;
import avilaos.modelo.ModoUsuario;
import avilaos.modelo.Proceso;
import avilaos.modelo.TipoNodo;
import avilaos.modelo.TipoOperacionES;
import avilaos.sistema.Cluster;
import avilaos.sistema.EspecProcesoES;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
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
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;

/**
 * Vista 1: sistema de archivos (JTree), información del elemento seleccionado, tabla de asignación de archivos
 * (JTable) y formulario para crear procesos de E/S. La interfaz NUNCA opera sobre el disco: solo crea el proceso.
 */
public class PanelArchivos extends JPanel {
    private static final String[] OPS = { "Crear archivo", "Crear directorio", "Leer archivo", "Actualizar (reescribir) archivo",
            "Cambiar nombre (actualizar)", "Eliminar archivo o directorio" };

    private final Cluster cluster;
    private final ContextoUsuario ctx;
    private final JTree arbol = new JTree(new ModeloArbol(NodoVista.vacio()));
    private ModeloArbol modeloArbol;
    private long versionVista = -1;
    private int[] colores = new int[0];
    private final ModeloTabla modeloTabla = new ModeloTabla("Archivo", "Bloques", "1.er bloque", "Dueño", "Proceso que lo creó", "Color");
    private final JLabel lblInfo = new JLabel("Seleccione un elemento del árbol");

    private final JComboBox<String> cmbOp = new JComboBox<>(OPS);
    private final JTextField txtRuta = new JTextField("/", 24), txtNombreNuevo = new JTextField(14), txtTamano = new JTextField("3", 6);
    private final JTextField txtNombre = new JTextField("ES_1", 10), txtMem = new JTextField("16", 6), txtPrio = new JTextField("5", 4),
            txtDeadline = new JTextField("500", 6), txtInstr = new JTextField("2", 4);
    private int contador = 1;

    public PanelArchivos(Cluster cluster, ContextoUsuario ctx) {
        this.cluster = cluster;
        this.ctx = ctx;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        arbol.setRootVisible(true);
        arbol.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override public Component getTreeCellRendererComponent(JTree t, Object v, boolean sel, boolean exp, boolean leaf, int row, boolean focus) {
                super.getTreeCellRendererComponent(t, v, sel, exp, leaf, row, focus);
                if (!(v instanceof NodoVista)) return this;
                NodoVista n = (NodoVista) v;
                if (n.tipo == TipoNodo.ARCHIVO)
                    setText(n.nombre + (n.pendiente ? " (creando…)" : "") + "   [" + n.bloques + " bl · " + n.owner + " · " + n.permiso + "]");
                return this;
            }
        });
        arbol.addTreeSelectionListener(e -> seleccion());
        JScrollPane spArbol = new JScrollPane(arbol);
        spArbol.setBorder(BorderFactory.createTitledBorder("Sistema de archivos (JTree)"));

        lblInfo.setVerticalAlignment(JLabel.TOP);
        JScrollPane spInfo = new JScrollPane(lblInfo);
        spInfo.setBorder(BorderFactory.createTitledBorder("Información del elemento seleccionado"));
        spInfo.setPreferredSize(new java.awt.Dimension(300, 140));
        JPanel izquierda = new JPanel(new BorderLayout(6, 6));
        izquierda.add(spArbol, BorderLayout.CENTER);
        izquierda.add(spInfo, BorderLayout.SOUTH);

        JTable tabla = new JTable(modeloTabla);
        tabla.setFillsViewportHeight(true);
        tabla.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int fila, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, fila, col);
                if (fila < colores.length) setBackground(Paleta.archivo(colores[fila]));
                setHorizontalAlignment(CENTER);
                return this;
            }
        });
        JScrollPane spTabla = new JScrollPane(tabla);
        spTabla.setBorder(BorderFactory.createTitledBorder("Tabla de asignación de archivos"));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Crear un proceso de E/S (el proceso es quien opera sobre el disco)"));
        int r = 0;
        fila(form, r++, "Operación:", cmbOp);
        fila(form, r++, "Ruta del archivo o directorio (puede elegirla en el árbol):", txtRuta);
        fila(form, r++, "Tamaño del archivo nuevo, en bloques (crear archivo):", txtTamano);
        fila(form, r++, "Nombre nuevo (cambiar nombre):", txtNombreNuevo);
        fila(form, r++, "Nombre del proceso:", txtNombre);
        fila(form, r++, "Memoria (unidades de RAM):", txtMem);
        fila(form, r++, "Prioridad (1 = máxima ... 10):", txtPrio);
        fila(form, r++, "Deadline (ciclos):", txtDeadline);
        fila(form, r++, "Instrucciones:", txtInstr);
        JButton btn = new JButton("Crear proceso de E/S");
        btn.addActionListener(e -> crear());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = r; gc.gridwidth = 2; gc.insets = new Insets(8, 4, 4, 4);
        form.add(btn, gc);
        cmbOp.addActionListener(e -> ajustar());
        ajustar();

        JPanel derecha = new JPanel(new BorderLayout(6, 6));
        derecha.add(spTabla, BorderLayout.CENTER);
        derecha.add(form, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, izquierda, derecha);
        split.setResizeWeight(0.38);
        add(split, BorderLayout.CENTER);
    }

    private static void fila(JPanel p, int fila, String etiqueta, JComponent c) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = fila; g.insets = new Insets(2, 4, 2, 4); g.anchor = GridBagConstraints.WEST;
        g.gridx = 0; p.add(new JLabel(etiqueta), g);
        g.gridx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1; p.add(c, g);
    }

    private void ajustar() {
        int k = cmbOp.getSelectedIndex();
        txtTamano.setEnabled(k == 0);
        txtNombreNuevo.setEnabled(k == 4);
    }

    private void seleccion() {
        TreePath tp = arbol.getSelectionPath();
        if (tp == null) return;
        NodoVista n = (NodoVista) tp.getLastPathComponent();
        lblInfo.setText("<html><b>Nombre:</b> " + n.nombre + "<br><b>Ruta:</b> " + n.ruta + "<br><b>Tipo:</b> "
                + (n.tipo == TipoNodo.DIRECTORIO ? "directorio" : "archivo") + "<br><b>Tamaño:</b> "
                + (n.tipo == TipoNodo.DIRECTORIO ? "—" : n.bloques + " bloque(s)") + "<br><b>Dueño:</b> " + n.owner
                + "<br><b>Permisos:</b> " + n.permiso + "<br><b>Primer bloque:</b> " + (n.tipo == TipoNodo.DIRECTORIO ? "—" : String.valueOf(n.primerBloque))
                + "<br><b>Creado por:</b> " + n.creador + "</html>");
        int k = cmbOp.getSelectedIndex();
        if (k <= 1) txtRuta.setText(n.tipo == TipoNodo.DIRECTORIO ? (n.ruta.equals("/") ? "/" : n.ruta + "/") : n.ruta);
        else txtRuta.setText(n.ruta);
    }

    private static int entero(JTextField f, String campo, int min, int max) {
        String t = f.getText().trim();
        if (t.isEmpty()) throw new IllegalArgumentException("El campo '" + campo + "' está vacío.");
        int v;
        try { v = Integer.parseInt(t); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("'" + campo + "' debe ser un número entero (escribió '" + t + "')."); }
        if (v < min || v > max) throw new IllegalArgumentException("'" + campo + "' debe estar entre " + min + " y " + max + " (escribió " + v + ").");
        return v;
    }

    private void crear() {
        try {
            int k = cmbOp.getSelectedIndex();
            EspecProcesoES e = new EspecProcesoES();
            e.nombre = txtNombre.getText().trim();
            e.memoria = entero(txtMem, "Memoria", 1, 1000000);
            e.prioridad = entero(txtPrio, "Prioridad", 1, 10);
            e.deadline = entero(txtDeadline, "Deadline", 1, 1000000);
            e.instrucciones = entero(txtInstr, "Instrucciones", 1, 1000000);
            e.ruta = txtRuta.getText().trim();
            e.modo = ctx.getModo();
            e.usuario = ctx.identidad();
            switch (k) {
                case 0: e.operacion = TipoOperacionES.CREAR; e.tamanoArchivo = entero(txtTamano, "Tamaño del archivo (bloques)", 1, 100000); break;
                case 1: e.operacion = TipoOperacionES.CREAR; e.tamanoArchivo = 0; break;
                case 2: e.operacion = TipoOperacionES.LEER; break;
                case 3: e.operacion = TipoOperacionES.ACTUALIZAR; break;
                case 4:
                    e.operacion = TipoOperacionES.ACTUALIZAR;
                    e.nuevoNombre = txtNombreNuevo.getText().trim();
                    if (e.nuevoNombre.isEmpty()) throw new IllegalArgumentException("Indique el nombre nuevo.");
                    break;
                default: e.operacion = TipoOperacionES.ELIMINAR; break;
            }
            Proceso p = cluster.crearProcesoArchivo(e);
            txtNombre.setText("ES_" + (++contador));
            JOptionPane.showMessageDialog(this, "Proceso " + p.etiqueta() + " creado: " + e.operacion.getTexto() + " " + p.getRutaArchivo()
                    + "\nSe ejecutará con las colas y la planificación del computador.", "Proceso de E/S creado", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage() + (ctx.getModo() == ModoUsuario.USUARIO ? "\n\n(Modo usuario activo: " + ctx.identidad() + ")" : "")
                    + "\n\nLa simulación continúa sin cambios.", "Operación rechazada", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void refrescar(InstantaneaFS fs) {
        if (fs.version == versionVista) return;
        versionVista = fs.version;
        TreePath sel = arbol.getSelectionPath();
        String rutaSel = sel == null ? null : ((NodoVista) sel.getLastPathComponent()).ruta;
        modeloArbol = new ModeloArbol(fs.raiz);
        arbol.setModel(modeloArbol);
        for (int i = 0; i < arbol.getRowCount(); i++) arbol.expandRow(i);
        TreePath nuevo = modeloArbol.caminoA(rutaSel);
        if (nuevo != null) arbol.setSelectionPath(nuevo);

        String[][] filas = new String[fs.archivos.length][];
        colores = new int[fs.archivos.length];
        for (int i = 0; i < filas.length; i++) {
            NodoVista a = fs.archivos[i];
            colores[i] = a.color;
            filas[i] = new String[] { a.ruta, String.valueOf(a.bloques), String.valueOf(a.primerBloque), a.owner, a.creador, "■ " + a.color };
        }
        modeloTabla.setDatos(filas);
    }
}
