package avilaos.gui;

import javax.swing.table.AbstractTableModel;

/** Modelo de tabla de solo lectura respaldado por un arreglo propio (sin colecciones de java.util). */
public class ModeloTabla extends AbstractTableModel {
    private final String[] columnas;
    private String[][] datos = new String[0][];

    public ModeloTabla(String... columnas) { this.columnas = columnas; }

    public void setDatos(String[][] nuevos) {
        this.datos = nuevos;
        fireTableDataChanged();
    }

    @Override public int getRowCount() { return datos.length; }
    @Override public int getColumnCount() { return columnas.length; }
    @Override public String getColumnName(int c) { return columnas[c]; }
    @Override public Object getValueAt(int f, int c) { return datos[f][c]; }
}
