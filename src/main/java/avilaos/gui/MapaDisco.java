package avilaos.gui;

import avilaos.archivos.InstantaneaFS;
import avilaos.modelo.DireccionCabezal;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.ToolTipManager;

/**
 * Mapa visual del disco: un cuadro por bloque (numerado), coloreado según el archivo que lo ocupa, con la posición del
 * cabezal y su dirección, los bloques que tienen solicitudes pendientes y el área de intercambio con el proceso dueño.
 */
public class MapaDisco extends JComponent {
    private static final int CELDA = 28, MARGEN = 10, ARRIBA = 30;

    private InstantaneaFS fs;
    private int cabezal;
    private DireccionCabezal direccion = DireccionCabezal.ASCENDENTE;
    private int[] swap = new int[0];
    private int[] pendientes = new int[0];
    private int cols = 20;

    public MapaDisco() {
        ToolTipManager.sharedInstance().registerComponent(this);
        setBackground(Color.WHITE);
        setOpaque(true);
    }

    public void actualizar(InstantaneaFS fs, int cabezal, DireccionCabezal dir, int[] swapDuenos, int[] bloquesPendientes) {
        this.fs = fs;
        this.cabezal = cabezal;
        this.direccion = dir;
        this.swap = swapDuenos;
        this.pendientes = bloquesPendientes;
        this.cols = fs != null && fs.totalBloques > 300 ? 25 : 20;
        revalidate();
        repaint();
    }

    @Override public Dimension getPreferredSize() {
        int total = fs == null ? 0 : fs.totalBloques;
        int filas = (total + cols - 1) / cols;
        int filasSwap = (swap.length + cols - 1) / cols;
        return new Dimension(2 * MARGEN + cols * CELDA, ARRIBA + filas * CELDA + (swap.length > 0 ? 36 + filasSwap * CELDA : 0) + 14);
    }

    @Override public String getToolTipText(MouseEvent e) {
        if (fs == null) return null;
        int c = (e.getX() - MARGEN) / CELDA, f = (e.getY() - ARRIBA) / CELDA;
        if (e.getX() < MARGEN || e.getY() < ARRIBA || c >= cols) return null;
        int i = f * cols + c;
        if (i >= 0 && i < fs.totalBloques) {
            return "Bloque " + i + (fs.nombreBloque[i].isEmpty() ? ": libre" : ": " + fs.nombreBloque[i]
                    + " → siguiente: " + (fs.siguienteBloque[i] < 0 ? "ninguno (último)" : String.valueOf(fs.siguienteBloque[i])));
        }
        int filas = (fs.totalBloques + cols - 1) / cols;
        int fs2 = f - filas - 1;
        int j = fs2 * cols + c;
        if (fs2 >= 0 && j >= 0 && j < swap.length) return "Swap " + j + (swap[j] == 0 ? ": libre" : ": proceso suspendido P" + swap[j]);
        return null;
    }

    @Override protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (fs == null) return;
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(Color.BLACK);
        g.drawString("Disco: " + fs.totalBloques + " bloques  (" + (fs.totalBloques - fs.libres) + " ocupados, " + fs.libres + " libres)"
                + "     Cabezal: bloque " + cabezal + "  " + direccion, MARGEN, 18);
        g.setFont(new Font("SansSerif", Font.PLAIN, 9));
        FontMetrics fm = g.getFontMetrics();
        boolean[] pend = new boolean[fs.totalBloques];
        for (int b : pendientes) if (b >= 0 && b < pend.length) pend[b] = true;

        for (int i = 0; i < fs.totalBloques; i++) {
            int x = MARGEN + (i % cols) * CELDA, y = ARRIBA + (i / cols) * CELDA;
            g.setColor(Paleta.archivo(fs.colorBloque[i]));
            g.fillRect(x, y, CELDA - 2, CELDA - 2);
            g.setColor(new Color(0x718096));
            g.drawRect(x, y, CELDA - 2, CELDA - 2);
            g.setColor(Color.DARK_GRAY);
            String t = String.valueOf(i);
            g.drawString(t, x + (CELDA - 2 - fm.stringWidth(t)) / 2, y + (CELDA - 2 + fm.getAscent()) / 2 - 1);
            if (pend[i]) { g.setColor(new Color(0xDD6B20)); g.fillOval(x + CELDA - 11, y + 1, 8, 8); }
        }
        // cabezal
        if (cabezal >= 0 && cabezal < fs.totalBloques) {
            int x = MARGEN + (cabezal % cols) * CELDA, y = ARRIBA + (cabezal / cols) * CELDA;
            g.setColor(Color.RED);
            g.setStroke(new BasicStroke(3f));
            g.drawRect(x - 1, y - 1, CELDA, CELDA);
            g.setStroke(new BasicStroke(1f));
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.drawString(direccion == DireccionCabezal.ASCENDENTE ? "▲" : "▼", x + 8, y - 3 < ARRIBA - 18 ? y + CELDA : y - 3);
        }
        // área de intercambio
        if (swap.length > 0) {
            int filas = (fs.totalBloques + cols - 1) / cols;
            int y0 = ARRIBA + filas * CELDA + 22;
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            g.setColor(Color.BLACK);
            g.drawString("Área de intercambio (swap): " + swap.length + " bloques", MARGEN, y0 - 6);
            g.setFont(new Font("SansSerif", Font.PLAIN, 9));
            for (int j = 0; j < swap.length; j++) {
                int x = MARGEN + (j % cols) * CELDA, y = y0 + (j / cols) * CELDA;
                g.setColor(swap[j] == 0 ? Paleta.LIBRE : Paleta.proceso(swap[j]));
                g.fillRect(x, y, CELDA - 2, CELDA - 2);
                g.setColor(new Color(0x718096));
                g.drawRect(x, y, CELDA - 2, CELDA - 2);
                if (swap[j] != 0) {
                    g.setColor(Color.DARK_GRAY);
                    String t = "P" + swap[j];
                    g.drawString(t, x + (CELDA - 2 - fm.stringWidth(t)) / 2, y + (CELDA - 2 + fm.getAscent()) / 2 - 1);
                }
            }
        }
    }
}
