package avilaos.gui;

import avilaos.hardware.Disco;
import avilaos.modelo.TipoPoliticaDisco;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * Desplazamiento ACUMULADO del cabezal vs. tiempo. Cada tramo se dibuja con el color de la política de disco que estaba
 * activa, de modo que cambiando de política durante la ejecución se pueden comparar sus pendientes (menos pendiente = menos movimiento).
 */
public class GraficoCabezal extends JPanel {
    private static final Color[] COLORES = { new Color(0x718096), new Color(0x2B6CB0), new Color(0x2F855A), new Color(0xC53030), new Color(0x6B46C1), new Color(0xDD6B20) };
    private static final int MAX_PUNTOS = 3000;
    private final Disco disco;

    public GraficoCabezal(Disco disco) {
        this.disco = disco;
        setBackground(Color.WHITE);
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int mi = 60, md = 20, ms = 40, mb = 40;
        int pw = w - mi - md, ph = h - ms - mb;
        if (pw <= 10 || ph <= 10) return;
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("Desplazamiento acumulado del cabezal (bloques) vs. tiempo (ciclos)", mi, 20);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();

        double[] y = disco.getSerieDesplazamiento().ultimos(MAX_PUNTOS);
        double[] pol = disco.getSeriePolitica().ultimos(MAX_PUNTOS);
        int total = disco.getSerieDesplazamiento().tamano();
        double max = 1;
        for (double v : y) max = Math.max(max, v);
        max = Math.ceil(max / 10.0) * 10;

        for (int k = 0; k <= 4; k++) {
            double v = max * k / 4;
            int py = ms + ph - (int) (ph * k / 4.0);
            g.setColor(new Color(0xDDDDDD));
            g.drawLine(mi, py, mi + pw, py);
            g.setColor(Color.DARK_GRAY);
            String s = String.valueOf((long) v);
            g.drawString(s, mi - 8 - fm.stringWidth(s), py + 4);
        }
        g.setColor(Color.BLACK);
        g.drawLine(mi, ms, mi, ms + ph);
        g.drawLine(mi, ms + ph, mi + pw, ms + ph);
        if (y.length > 1) {
            for (int k = 0; k <= 4; k++) {
                int idx = k * (y.length - 1) / 4;
                int x = mi + (int) (pw * idx / (double) (y.length - 1));
                g.setColor(Color.DARK_GRAY);
                String s = String.valueOf(total - y.length + idx + 1);
                g.drawString(s, x - fm.stringWidth(s) / 2, ms + ph + 16);
            }
            g.setStroke(new BasicStroke(2.2f));
            for (int j = 1; j < y.length; j++) {
                int x1 = mi + (int) (pw * (j - 1) / (double) (y.length - 1)), x2 = mi + (int) (pw * j / (double) (y.length - 1));
                int y1 = ms + ph - (int) (ph * y[j - 1] / max), y2 = ms + ph - (int) (ph * y[j] / max);
                g.setColor(COLORES[((int) pol[j]) % COLORES.length]);
                g.drawLine(x1, y1, x2, y2);
            }
            g.setStroke(new BasicStroke(1f));
        }
        int lx = mi + 10;
        for (TipoPoliticaDisco p : TipoPoliticaDisco.values()) {
            g.setColor(COLORES[p.ordinal() % COLORES.length]);
            g.fillRect(lx, ms + 6, 14, 8);
            g.setColor(Color.BLACK);
            g.drawString(p.getTexto(), lx + 18, ms + 14);
            lx += 30 + fm.stringWidth(p.getTexto());
        }
    }
}
