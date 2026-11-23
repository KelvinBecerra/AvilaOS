package avilaos.gui;

import avilaos.archivos.NodoVista;
import avilaos.modelo.TipoNodo;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/** TreeModel propio sobre una instantánea del sistema de archivos (sin DefaultMutableTreeNode, que usa Vector). */
public class ModeloArbol implements TreeModel {
    private final NodoVista raiz;

    public ModeloArbol(NodoVista raiz) { this.raiz = raiz; }

    @Override public Object getRoot() { return raiz; }
    @Override public Object getChild(Object padre, int i) { return ((NodoVista) padre).hijos[i]; }
    @Override public int getChildCount(Object padre) { return ((NodoVista) padre).hijos.length; }
    @Override public boolean isLeaf(Object n) { return ((NodoVista) n).tipo == TipoNodo.ARCHIVO; }
    @Override public int getIndexOfChild(Object padre, Object hijo) {
        NodoVista[] hs = ((NodoVista) padre).hijos;
        for (int i = 0; i < hs.length; i++) if (hs[i] == hijo) return i;
        return -1;
    }
    @Override public void valueForPathChanged(TreePath path, Object nuevo) { }
    @Override public void addTreeModelListener(TreeModelListener l) { }
    @Override public void removeTreeModelListener(TreeModelListener l) { }

    /** Camino desde la raíz hasta la ruta dada (o null si no existe en esta instantánea). */
    public TreePath caminoA(String ruta) {
        if (ruta == null) return null;
        NodoVista act = raiz;
        Object[] camino = new Object[64];
        int n = 0;
        camino[n++] = act;
        if (!ruta.equals("/")) {
            for (String p : ruta.substring(1).split("/")) {
                NodoVista sig = null;
                for (NodoVista h : act.hijos) if (h.nombre.equals(p)) { sig = h; break; }
                if (sig == null || n == camino.length) return null;
                act = sig;
                camino[n++] = act;
            }
        }
        Object[] r = new Object[n];
        System.arraycopy(camino, 0, r, 0, n);
        return new TreePath(r);
    }
}
