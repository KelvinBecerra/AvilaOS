package avilaos.hardware;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.Clockable;
import avilaos.modelo.Proceso;
import avilaos.so.NucleoSO;

/**
 * Controlador de E/S / red del computador (hardware: no decide nada). Cada ciclo decrementa
 * el contador de las solicitudes pendientes y, al llegar a 0, avisa al SO ("interrupción").
 */
public class ControladorES implements Clockable {
    private final NucleoSO so;
    private final ListaEnlazada<Proceso> pendientes = new ListaEnlazada<>();

    public ControladorES(NucleoSO so) { this.so = so; }

    public void registrar(Proceso p) { pendientes.agregarAlFinal(p); }
    public void cancelar(Proceso p) { pendientes.quitar(p); }

    @Override public void tick(long ciclo) {
        ListaEnlazada<Proceso> copia = pendientes.copiar();
        for (int i = 0; i < copia.tamano(); i++) {
            Proceso p = copia.obtener(i);
            if (p.decrementarContadorBloqueo()) {
                pendientes.quitar(p);
                so.interrupcionES(p);
            }
        }
    }
}
