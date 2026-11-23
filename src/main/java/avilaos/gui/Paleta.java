package avilaos.gui;

import java.awt.Color;

/** Colores de archivos y de procesos suspendidos en el mapa del disco. */
public final class Paleta {
    private Paleta() { }
    public static final Color LIBRE = new Color(0xEDF2F7);

    public static Color archivo(int idx) {
        if (idx < 0) return LIBRE;
        return Color.getHSBColor((idx * 0.618034f) % 1f, 0.50f, 0.96f);
    }
    public static Color proceso(int pid) { return Color.getHSBColor((pid * 0.37f) % 1f, 0.35f, 0.80f); }
}
