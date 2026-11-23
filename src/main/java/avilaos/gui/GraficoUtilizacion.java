package avilaos.gui;

import avilaos.so.Computador;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** Gráfico de líneas propio: utilización de la CPU de cada computador vs. tiempo (ventana de 5 ciclos). */
public class GraficoUtilizacion extends JPanel {
    private static final Color[] COLORES = { new Color(0x1F77B4), new Color(0xFF7F0E), new Color(0x2CA02C),
            new Color(0xD62728), new Color(0x9467BD), new Color(0x8C564B) };
    private static final int MAX_PUNTOS = 300, MUESTRA = 5;
    private final Computador[] computadores;

    public GraficoUtilizacion(Computador[] computadores) {
        this.computadores = computadores;
        setBackground(Color.WHITE);
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        int mi = 55, md = 20, ms = 40, mb = 40;
        int pw = w - mi - md, ph = h - ms - mb;
        if (pw <= 10 || ph <= 10) return;

        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.drawString("Utilización del procesador por computador (%) vs. tiempo (ciclos de reloj)", mi, 20);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fm = g.getFontMetrics();
        for (int v = 0; v <= 100; v += 25) {
            int y = ms + ph - (int) (ph * v / 100.0);
            g.setColor(new Color(0xDDDDDD));
            g.drawLine(mi, y, mi + pw, y);
            g.setColor(Color.DARK_GRAY);
            String s = v + "%";
            g.drawString(s, mi - 8 - fm.stringWidth(s), y + 4);
        }
        g.setColor(Color.BLACK);
        g.drawLine(mi, ms, mi, ms + ph);
        g.drawLine(mi, ms + ph, mi + pw, ms + ph);

        int maxN = 0;
        double[][] series = new double[computadores.length][];
        for (int i = 0; i < computadores.length; i++) {
            series[i] = computadores[i].getNucleo().getSerieUtilizacion().ultimos(MAX_PUNTOS);
            maxN = Math.max(maxN, series[i].length);
        }
        int total = computadores.length > 0 ? computadores[0].getNucleo().getSerieUtilizacion().tamano() : 0;
        if (maxN > 1) {
            for (int k = 0; k <= 4; k++) {
                int idx = k * (maxN - 1) / 4;
                int x = mi + (int) (pw * idx / (double) (maxN - 1));
                long ciclo = (long) (total - maxN + idx + 1) * MUESTRA;
                g.setColor(Color.DARK_GRAY);
                String s = String.valueOf(ciclo);
                g.drawString(s, x - fm.stringWidth(s) / 2, ms + ph + 16);
            }
        }
        g.setStroke(new BasicStroke(2f));
        for (int i = 0; i < series.length; i++) {
            g.setColor(COLORES[i % COLORES.length]);
            double[] s = series[i];
            for (int j = 1; j < s.length; j++) {
                int x1 = mi + (int) (pw * (j - 1) / (double) Math.max(1, maxN - 1));
                int x2 = mi + (int) (pw * j / (double) Math.max(1, maxN - 1));
                int y1 = ms + ph - (int) (ph * s[j - 1] / 100.0);
                int y2 = ms + ph - (int) (ph * s[j] / 100.0);
                g.drawLine(x1, y1, x2, y2);
            }
        }
        g.setStroke(new BasicStroke(1f));
        int lx = mi + 10;
        for (int i = 0; i < computadores.length; i++) {
            g.setColor(COLORES[i % COLORES.length]);
            g.fillRect(lx, ms + 6, 14, 8);
            g.setColor(Color.BLACK);
            g.drawString(computadores[i].getNombre(), lx + 18, ms + 14);
            lx += 30 + fm.stringWidth(computadores[i].getNombre());
        }
    }
}
