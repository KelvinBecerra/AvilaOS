package avilaos.archivos;

import avilaos.modelo.ModoUsuario;
import avilaos.modelo.Permiso;
import avilaos.modelo.TipoNodo;
import avilaos.modelo.TipoOperacionES;

/**
 * Reglas de los dos modos de uso. Devuelve null si la operación está permitida o el MOTIVO del rechazo
 * (la GUI lo muestra sin interrumpir la simulación).
 *  - ADMINISTRADOR: todo.
 *  - USUARIO: lee archivos propios o públicos; crea/modifica/elimina archivos solo si son suyos (o dentro de sus
 *    directorios); no crea ni elimina directorios, no renombra y no toca archivos del sistema ni de otros usuarios.
 */
public final class ControlAcceso {
    private ControlAcceso() { }

    public static String verificar(ModoUsuario modo, String usuario, TipoOperacionES op, String ruta, int tamanoNuevo,
                                   String nuevoNombre, SistemaArchivos fs) {
        if (modo == ModoUsuario.ADMINISTRADOR) return null;
        String r;
        try { r = SistemaArchivos.normalizar(ruta); } catch (IllegalArgumentException e) { return e.getMessage(); }
        NodoVista n = fs.consultar(r);
        switch (op) {
            case CREAR: {
                if (tamanoNuevo <= 0) return "Los usuarios no pueden crear directorios: solo el administrador.";
                int k = r.lastIndexOf('/');
                String padre = k <= 0 ? "/" : r.substring(0, k);
                NodoVista p = fs.consultar(padre);
                if (p != null && !p.owner.equals(usuario))
                    return "Solo puede crear archivos dentro de sus propios directorios; '" + padre + "' pertenece a '" + p.owner + "'.";
                return null;
            }
            case LEER:
                if (n == null) return null;
                if (n.tipo == TipoNodo.DIRECTORIO) return "Solo se pueden leer archivos, no directorios.";
                if (!n.owner.equals(usuario) && n.permiso == Permiso.PRIVADO)
                    return "El archivo '" + r + "' es privado y pertenece a '" + n.owner + "'.";
                return null;
            case ACTUALIZAR:
                if (nuevoNombre != null && !nuevoNombre.trim().isEmpty()) return "Solo el administrador puede cambiar el nombre de archivos y directorios.";
                if (n == null) return null;
                if (n.tipo == TipoNodo.DIRECTORIO) return "Los usuarios no pueden modificar directorios.";
                if (!n.owner.equals(usuario)) return "No puede modificar '" + r + "': pertenece a '" + n.owner + "' (archivos del sistema o de otros usuarios están protegidos).";
                return null;
            case ELIMINAR:
                if (n == null) return null;
                if (n.tipo == TipoNodo.DIRECTORIO) return "Solo el administrador puede eliminar directorios.";
                if (!n.owner.equals(usuario)) return "No puede eliminar '" + r + "': pertenece a '" + n.owner + "'.";
                return null;
            default:
                return "Operación interna del sistema operativo: no puede solicitarla un usuario.";
        }
    }
}
