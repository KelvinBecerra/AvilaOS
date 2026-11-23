package avilaos.sistema;

import avilaos.estructuras.ListaEnlazada;
import avilaos.json.Json;
import avilaos.modelo.Permiso;
import avilaos.modelo.TipoOperacionES;
import avilaos.modelo.TipoPolitica;
import avilaos.modelo.TipoPoliticaDisco;

/** Contenido del JSON de procesos y configuración (globalConfig, computer, processes). */
public class PerfilProcesos {
    public int cicloMs, bloquesSwap, ram, quantum;
    public TipoPoliticaDisco politicaDisco;
    public TipoPolitica politicaCPU;
    public final ListaEnlazada<EspecProcesoES> procesos = new ListaEnlazada<>();

    public static PerfilProcesos leer(String texto) {
        Json.Obj raiz = Json.leerObjeto(texto);
        PerfilProcesos p = new PerfilProcesos();
        Json.Obj g = raiz.obj("globalConfig");
        p.cicloMs = rango(g.entero("cycleDurationMs"), 1, 60000, "globalConfig.cycleDurationMs");
        p.bloquesSwap = rango(g.entero("swapSizeBlocks"), 0, 100000, "globalConfig.swapSizeBlocks");
        p.politicaDisco = politicaDisco(g.texto("initialDiskPolicy"));
        Json.Obj c = raiz.obj("computer");
        p.ram = rango(c.entero("ramCapacity"), 1, 1000000, "computer.ramCapacity");
        p.politicaCPU = politicaCPU(c.texto("policy"));
        p.quantum = rango(c.entero("quantum", 0), 0, 1000, "computer.quantum");
        Json.Arr ps = raiz.arr("processes");
        for (int i = 0; i < ps.tamano(); i++) {
            String donde = "processes[" + i + "]";
            try {
                Json.Obj o = ps.obj(i);
                EspecProcesoES e = new EspecProcesoES();
                e.nombre = o.texto("name");
                e.memoria = rango(o.entero("memory"), 1, p.ram, "memory");
                e.deadline = rango(o.entero("deadline"), 1, 1000000, "deadline");
                e.instrucciones = rango(o.entero("instructions"), 1, 1000000, "instructions");
                e.ruta = o.texto("targetPath");
                e.operacion = operacion(o.texto("operation"));
                e.tamanoArchivo = rango(o.entero("fileSizeBlocks", 0), 0, 100000, "fileSizeBlocks");
                e.nuevoNombre = o.texto("newName", null);
                e.prioridad = rango(o.entero("priority", 5), 1, 10, "priority");
                e.usuario = o.texto("user", "admin");
                if (o.tiene("permissions") && o.get("permissions") != null) e.permisoNuevo = Permiso.desdeTexto(o.texto("permissions"));
                p.procesos.agregarAlFinal(e);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(donde + ": " + ex.getMessage());
            }
        }
        return p;
    }

    private static int rango(long v, long min, long max, String campo) {
        if (v < min || v > max) throw new IllegalArgumentException("'" + campo + "' debe estar entre " + min + " y " + max + " (valor: " + v + ")");
        return (int) v;
    }

    static TipoOperacionES operacion(String t) {
        String s = t.trim().toUpperCase();
        switch (s) {
            case "READ": case "LEER": return TipoOperacionES.LEER;
            case "CREATE": case "CREAR": return TipoOperacionES.CREAR;
            case "UPDATE": case "ACTUALIZAR": return TipoOperacionES.ACTUALIZAR;
            case "DELETE": case "ELIMINAR": return TipoOperacionES.ELIMINAR;
            default: throw new IllegalArgumentException("operation inválida '" + t + "' (READ, CREATE, UPDATE o DELETE)");
        }
    }

    static TipoPolitica politicaCPU(String t) {
        String s = t.trim().toUpperCase().replace('-', '_').replace(' ', '_');
        switch (s) {
            case "FCFS": return TipoPolitica.FCFS;
            case "EDF": return TipoPolitica.EDF;
            case "RR": case "ROUND_ROBIN": return TipoPolitica.ROUND_ROBIN;
            case "PRIORITY": case "PRIORIDAD": case "PRIORIDADES": return TipoPolitica.PRIORIDADES;
            default: throw new IllegalArgumentException("computer.policy inválida '" + t + "' (FCFS, EDF, RR o PRIORITY)");
        }
    }

    static TipoPoliticaDisco politicaDisco(String t) {
        String s = t.trim().toUpperCase().replace('-', '_');
        switch (s) {
            case "FIFO": return TipoPoliticaDisco.FIFO;
            case "SSTF": return TipoPoliticaDisco.SSTF;
            case "SCAN": return TipoPoliticaDisco.SCAN;
            case "C_SCAN": case "CSCAN": return TipoPoliticaDisco.C_SCAN;
            case "LOOK": return TipoPoliticaDisco.LOOK;
            case "C_LOOK": case "CLOOK": return TipoPoliticaDisco.C_LOOK;
            default: throw new IllegalArgumentException("globalConfig.initialDiskPolicy inválida '" + t + "' (FIFO, SSTF, SCAN, C-SCAN, LOOK o C-LOOK)");
        }
    }
}
