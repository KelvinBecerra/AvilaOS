package avilaos.so;

import avilaos.archivos.ControlAcceso;
import avilaos.archivos.NodoSA;
import avilaos.archivos.NodoVista;
import avilaos.archivos.SistemaArchivos;
import avilaos.hardware.Disco;
import avilaos.modelo.ModoUsuario;
import avilaos.modelo.Permiso;
import avilaos.modelo.Proceso;
import avilaos.modelo.SolicitudDisco;
import avilaos.modelo.TipoNodo;
import avilaos.modelo.TipoOperacionES;

/**
 * Parte del sistema operativo que traduce una operación de archivo de un proceso en cambios del sistema de
 * archivos y solicitudes al disco. Se invoca SIEMPRE desde un proceso (instrucción en modo SO) o desde el disco al
 * terminar: la interfaz gráfica jamás llama a esta clase.
 */
public class ServicioArchivos {
    public enum Resultado { SIN_DISCO, ESPERA_DISCO, RECHAZADO }

    private final Computador computador;

    public ServicioArchivos(Computador computador) { this.computador = computador; }

    /** Posición del disco donde se atienden las solicitudes del área de intercambio (borde exterior). */
    public int posicionSwap() { return computador.getFs().getTotalBloques() - 1; }

    public Resultado iniciar(Proceso p) {
        SistemaArchivos fs = computador.getFs();
        Disco disco = computador.getDisco();
        TipoOperacionES op = p.getOperacion();
        try {
            String ruta = SistemaArchivos.normalizar(p.getRutaArchivo());
            String motivo = ControlAcceso.verificar(p.getModoCreacion(), p.getUsuario(), op, ruta, p.getTamanoArchivo(), p.getNuevoNombre(), fs);
            if (motivo != null) return rechazar(p, motivo);

            switch (op) {
                case CREAR: {
                    Permiso perm = p.getPermisoNuevo() != null ? p.getPermisoNuevo()
                            : (p.getModoCreacion() == ModoUsuario.ADMINISTRADOR ? Permiso.PUBLICO : Permiso.PRIVADO);
                    if (p.getTamanoArchivo() <= 0) {
                        fs.crearDirectorio(ruta, p.getUsuario(), perm, p.etiqueta());
                        computador.registrar("Proceso " + p.etiqueta() + " crea el directorio " + ruta + " (sin acceso al disco)");
                        return fin(p, Resultado.SIN_DISCO);
                    }
                    NodoSA n = fs.reservarArchivo(ruta, p.getTamanoArchivo(), p.getUsuario(), perm, p.etiqueta());
                    p.setObjetivoFS(n);
                    return encolar(p, disco, fs.cadena(n), ruta);
                }
                case LEER: {
                    NodoSA n = existente(fs, ruta);
                    if (n.getTipo() != TipoNodo.ARCHIVO) return rechazar(p, "'" + ruta + "' es un directorio: solo se pueden leer archivos");
                    if (n.isPendiente() || n.isEliminando()) return rechazar(p, "'" + ruta + "' está siendo creado o eliminado por otro proceso");
                    p.setObjetivoFS(n);
                    return encolar(p, disco, fs.cadena(n), ruta);
                }
                case ACTUALIZAR: {
                    NodoSA n = existente(fs, ruta);
                    String nuevo = p.getNuevoNombre() == null ? "" : p.getNuevoNombre().trim();
                    if (!nuevo.isEmpty()) {
                        SistemaArchivos.validarNombre(nuevo);
                        String padre = ruta.lastIndexOf('/') <= 0 ? "" : ruta.substring(0, ruta.lastIndexOf('/'));
                        NodoVista otro = fs.consultar(padre + "/" + nuevo);
                        if (otro != null && !otro.ruta.equals(ruta)) return rechazar(p, "ya existe '" + padre + "/" + nuevo + "'");
                    }
                    if (n.getTipo() == TipoNodo.DIRECTORIO) {
                        if (nuevo.isEmpty()) return rechazar(p, "para actualizar un directorio indique el nombre nuevo");
                        fs.renombrar(n, nuevo);
                        computador.registrar("Proceso " + p.etiqueta() + " renombra el directorio " + ruta + " a '" + nuevo + "' (sin acceso al disco)");
                        return fin(p, Resultado.SIN_DISCO);
                    }
                    if (n.isPendiente() || n.isEliminando()) return rechazar(p, "'" + ruta + "' está siendo creado o eliminado por otro proceso");
                    p.setObjetivoFS(n);
                    return encolar(p, disco, fs.cadena(n), ruta);
                }
                case ELIMINAR: {
                    NodoSA n = existente(fs, ruta);
                    if (n.isEliminando()) return rechazar(p, "'" + ruta + "' ya está siendo eliminado");
                    int[] bloques = fs.bloquesDe(n);
                    if (bloques.length == 0) {
                        fs.eliminar(n);
                        computador.registrar("Proceso " + p.etiqueta() + " elimina " + ruta + " (no ocupaba bloques)");
                        return fin(p, Resultado.SIN_DISCO);
                    }
                    fs.marcarEliminando(n, true);
                    p.setObjetivoFS(n);
                    return encolar(p, disco, bloques, ruta);
                }
                default:
                    return rechazar(p, "operación no válida para un proceso de usuario");
            }
        } catch (IllegalArgumentException e) {
            return rechazar(p, e.getMessage());
        }
    }

    private NodoSA existente(SistemaArchivos fs, String ruta) {
        NodoSA n = fs.buscar(ruta);
        if (n == null) throw new IllegalArgumentException("'" + ruta + "' no existe");
        return n;
    }

    private Resultado encolar(Proceso p, Disco disco, int[] cadena, String ruta) {
        SolicitudDisco s = new SolicitudDisco(cadena[0], p.getOperacion(), p, ruta, cadena.length);
        s.setCadena(cadena);
        disco.encolar(s, computador.getCiclo());
        computador.registrar("Proceso " + p.etiqueta() + " genera una solicitud " + p.getOperacion().getTexto() + " sobre " + ruta
                + " (bloque " + cadena[0] + ", " + cadena.length + " bloque(s)) y se bloquea por E/S");
        return Resultado.ESPERA_DISCO;
    }

    private Resultado fin(Proceso p, Resultado r) { p.setResultado("OK"); return r; }

    private Resultado rechazar(Proceso p, String motivo) {
        p.setResultado("RECHAZADO: " + motivo);
        computador.registrar("Proceso " + p.etiqueta() + " (" + p.getOperacion().getTexto() + " " + p.getRutaArchivo() + ") fue RECHAZADO: " + motivo);
        return Resultado.RECHAZADO;
    }

    /** El disco terminó una solicitud de archivo: se aplica el cambio en el sistema de archivos. */
    public void completar(SolicitudDisco s) {
        Proceso p = s.getProceso();
        SistemaArchivos fs = computador.getFs();
        NodoSA n = (NodoSA) p.getObjetivoFS();
        try {
            switch (s.getOperacion()) {
                case CREAR:
                    fs.confirmar(n);
                    break;
                case ACTUALIZAR:
                    if (p.getNuevoNombre() != null && !p.getNuevoNombre().trim().isEmpty()) fs.renombrar(n, p.getNuevoNombre());
                    break;
                case ELIMINAR:
                    fs.eliminar(n);
                    break;
                default:
                    break;
            }
            p.setResultado("OK");
        } catch (IllegalArgumentException e) {
            p.setResultado("ERROR: " + e.getMessage());
            computador.registrar("Proceso " + p.etiqueta() + ": no se pudo completar la operación: " + e.getMessage());
        }
    }

    /** El proceso murió (deadline) a mitad de una operación: se deja el sistema de archivos consistente. */
    public void abortar(Proceso p) {
        Object o = p.getObjetivoFS();
        if (!(o instanceof NodoSA) || p.getOperacion() == null) return;
        NodoSA n = (NodoSA) o;
        SistemaArchivos fs = computador.getFs();
        try {
            if (p.getOperacion() == TipoOperacionES.CREAR && n.isPendiente()) fs.eliminar(n);       // libera sus bloques
            else if (p.getOperacion() == TipoOperacionES.ELIMINAR) fs.marcarEliminando(n, false);
        } catch (IllegalArgumentException ignorada) { /* ya no existe */ }
        p.setObjetivoFS(null);
    }
}
