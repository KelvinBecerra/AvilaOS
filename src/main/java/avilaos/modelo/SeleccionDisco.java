package avilaos.modelo;

/**
 * Resultado de una decisión del planificador de disco: la solicitud elegida y el RECORRIDO del cabezal.
 * 'ruta' son puntos de paso. Si saltoEn >= 0, al llegar al punto ruta[saltoEn] el cabezal salta (sin costo)
 * a ruta[saltoEn + 1] y continúa desde ahí (así se modela el regreso de C-SCAN / C-LOOK, que no se cuenta).
 */
public class SeleccionDisco {
    public final SolicitudDisco solicitud;
    public final int[] ruta;
    public final int saltoEn;

    public SeleccionDisco(SolicitudDisco solicitud, int[] ruta, int saltoEn) {
        this.solicitud = solicitud;
        this.ruta = ruta;
        this.saltoEn = saltoEn;
    }

    public static SeleccionDisco directa(SolicitudDisco s) { return new SeleccionDisco(s, new int[] { s.getBloque() }, -1); }
}
