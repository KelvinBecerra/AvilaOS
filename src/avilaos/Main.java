package avilaos;

import avilaos.gui.AplicacionAvila;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Punto de entrada de ÁvilaOS (simulador de sistema operativo distribuido). */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel"); }
            catch (Exception ignorada) { /* se usa el aspecto por defecto */ }
            JFrame f = new JFrame("ÁvilaOS — Simulador de Sistema Operativo Distribuido");
            AplicacionAvila app = new AplicacionAvila();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { app.detener(); }
            });
            f.setContentPane(app);
            f.setSize(1400, 880);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
