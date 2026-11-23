package avilaos.sistema;

import avilaos.modelo.ModoUsuario;
import avilaos.modelo.Permiso;
import avilaos.modelo.TipoOperacionES;

/** Datos de un proceso de E/S sobre archivos (lo que define el usuario o el JSON de procesos). */
public class EspecProcesoES {
    public String nombre;
    public int memoria, deadline, instrucciones, prioridad = 5;
    public String ruta;
    public TipoOperacionES operacion;
    public int tamanoArchivo;           // CREAR: bloques del archivo nuevo (0 = directorio)
    public String nuevoNombre;          // ACTUALIZAR: nombre nuevo (opcional)
    public ModoUsuario modo = ModoUsuario.ADMINISTRADOR;
    public String usuario = "admin";
    public Permiso permisoNuevo;        // CREAR: permiso del archivo/directorio nuevo (opcional)
}
