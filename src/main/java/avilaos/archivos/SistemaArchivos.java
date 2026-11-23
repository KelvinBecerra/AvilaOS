package avilaos.archivos;

import avilaos.estructuras.ListaEnlazada;
import avilaos.modelo.Permiso;
import avilaos.modelo.TipoNodo;
import java.util.concurrent.Semaphore;

/**
 * Sistema de archivos jerárquico sobre el mapa de bloques del disco, con asignación ENCADENADA:
 * cada archivo es una lista enlazada de bloques y cada bloque guarda cuál es el siguiente.
 * Todo acceso está protegido por un semáforo de Java (lo usan el hilo del computador y la GUI).
 */
public class SistemaArchivos {
    private static final int PALETA = 24;

    private final int total;
    private final int[] siguiente;       // puntero al próximo bloque de la cadena (-1 = último)
    private final NodoSA[] dueno;        // archivo dueño de cada bloque (null = libre)
    private final NodoSA raiz = new NodoSA("/", TipoNodo.DIRECTORIO, "admin", Permiso.PUBLICO);
    private int libres;
    private int colorSiguiente;
    private long version;
    private final Semaphore mutex = new Semaphore(1);

    public SistemaArchivos(int totalBloques) {
        if (totalBloques < 2) throw new IllegalArgumentException("El disco necesita al menos 2 bloques");
        this.total = totalBloques;
        this.siguiente = new int[totalBloques];
        this.dueno = new NodoSA[totalBloques];
        for (int i = 0; i < totalBloques; i++) siguiente[i] = -1;
        this.libres = totalBloques;
    }

    // ------------------------------------------------------------------ rutas
    /** Normaliza "a//b/" -> "/a/b". Lanza IllegalArgumentException si la ruta es inválida. */
    public static String normalizar(String ruta) {
        if (ruta == null || ruta.trim().isEmpty()) throw new IllegalArgumentException("La ruta está vacía");
        String r = ruta.trim();
        if (!r.startsWith("/")) throw new IllegalArgumentException("La ruta debe comenzar con '/' (ej. /sistema/boot.bin)");
        String[] partes = r.split("/");
        StringBuilder sb = new StringBuilder();
        for (String p : partes) {
            if (p.isEmpty()) continue;
            validarNombre(p);
            sb.append('/').append(p);
        }
        return sb.length() == 0 ? "/" : sb.toString();
    }

    public static void validarNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) throw new IllegalArgumentException("El nombre no puede estar vacío");
        if (nombre.length() > 60) throw new IllegalArgumentException("El nombre es demasiado largo (máximo 60 caracteres)");
        for (int i = 0; i < nombre.length(); i++) {
            char c = nombre.charAt(i);
            if (c == '/' || c == '\\' || c < 32 || c == ',' || c == '"')
                throw new IllegalArgumentException("El nombre '" + nombre + "' contiene caracteres no permitidos");
        }
        if (nombre.equals(".") || nombre.equals("..")) throw new IllegalArgumentException("Nombre reservado: " + nombre);
    }

    private NodoSA buscarInterno(String rutaNormalizada) {
        if (rutaNormalizada.equals("/")) return raiz;
        NodoSA act = raiz;
        for (String p : rutaNormalizada.substring(1).split("/")) {
            NodoSA sig = null;
            for (int i = 0; i < act.hijos.tamano(); i++) if (act.hijos.obtener(i).nombre.equals(p)) { sig = act.hijos.obtener(i); break; }
            if (sig == null) return null;
            act = sig;
        }
        return act;
    }

    private NodoSA hijo(NodoSA dir, String nombre) {
        for (int i = 0; i < dir.hijos.tamano(); i++) if (dir.hijos.obtener(i).nombre.equals(nombre)) return dir.hijos.obtener(i);
        return null;
    }

    private static String nombreDe(String ruta) { return ruta.substring(ruta.lastIndexOf('/') + 1); }
    private static String padreDe(String ruta) { int k = ruta.lastIndexOf('/'); return k <= 0 ? "/" : ruta.substring(0, k); }

    // ------------------------------------------------------------------ consultas
    public NodoSA buscar(String ruta) {
        String r = normalizar(ruta);
        mutex.acquireUninterruptibly();
        try { return buscarInterno(r); } finally { mutex.release(); }
    }

    public NodoVista consultar(String ruta) {
        String r = normalizar(ruta);
        mutex.acquireUninterruptibly();
        try {
            NodoSA n = buscarInterno(r);
            return n == null ? null : vista(n, false);
        } finally { mutex.release(); }
    }

    public int getTotalBloques() { return total; }
    public int getLibres() { mutex.acquireUninterruptibly(); try { return libres; } finally { mutex.release(); } }
    public long getVersion() { mutex.acquireUninterruptibly(); try { return version; } finally { mutex.release(); } }

    /** Bloques del archivo siguiendo los punteros de la lista enlazada. */
    public int[] cadena(NodoSA archivo) {
        mutex.acquireUninterruptibly();
        try { return cadenaInterna(archivo); } finally { mutex.release(); }
    }

    private int[] cadenaInterna(NodoSA a) {
        if (a.tipo != TipoNodo.ARCHIVO || a.primerBloque < 0) return new int[0];
        int[] r = new int[a.bloques];
        int k = 0, b = a.primerBloque;
        while (b >= 0 && k < r.length) { r[k++] = b; b = siguiente[b]; }
        if (k < r.length) { int[] c = new int[k]; System.arraycopy(r, 0, c, 0, k); return c; }
        return r;
    }

    /** Todos los bloques de un archivo o de todo un directorio (en orden de recorrido). */
    public int[] bloquesDe(NodoSA n) {
        mutex.acquireUninterruptibly();
        try {
            int cuantos = contarBloques(n);
            int[] r = new int[cuantos];
            llenar(n, r, 0);
            return r;
        } finally { mutex.release(); }
    }

    private int contarBloques(NodoSA n) {
        if (n.tipo == TipoNodo.ARCHIVO) return cadenaInterna(n).length;
        int s = 0;
        for (int i = 0; i < n.hijos.tamano(); i++) s += contarBloques(n.hijos.obtener(i));
        return s;
    }

    private int llenar(NodoSA n, int[] dest, int pos) {
        if (n.tipo == TipoNodo.ARCHIVO) {
            int[] c = cadenaInterna(n);
            System.arraycopy(c, 0, dest, pos, c.length);
            return pos + c.length;
        }
        for (int i = 0; i < n.hijos.tamano(); i++) pos = llenar(n.hijos.obtener(i), dest, pos);
        return pos;
    }

    // ------------------------------------------------------------------ operaciones
    public void crearDirectorio(String ruta, String owner, Permiso permiso, String creador) {
        String r = normalizar(ruta);
        mutex.acquireUninterruptibly();
        try {
            NodoSA padre = padreParaCrear(r);
            NodoSA n = new NodoSA(nombreDe(r), TipoNodo.DIRECTORIO, owner, permiso);
            n.creador = creador;
            n.padre = padre;
            padre.hijos.agregarAlFinal(n);
            version++;
        } finally { mutex.release(); }
    }

    private NodoSA padreParaCrear(String r) {
        if (r.equals("/")) throw new IllegalArgumentException("La raíz ya existe");
        NodoSA padre = buscarInterno(padreDe(r));
        if (padre == null) throw new IllegalArgumentException("El directorio '" + padreDe(r) + "' no existe");
        if (padre.tipo != TipoNodo.DIRECTORIO) throw new IllegalArgumentException("'" + padreDe(r) + "' es un archivo, no un directorio");
        if (hijo(padre, nombreDe(r)) != null) throw new IllegalArgumentException("Ya existe '" + r + "'");
        return padre;
    }

    /** Reserva (de inmediato) los bloques de un archivo nuevo: queda "pendiente" hasta que el disco termine su escritura. */
    public NodoSA reservarArchivo(String ruta, int bloques, String owner, Permiso permiso, String creador) {
        String r = normalizar(ruta);
        if (bloques < 1) throw new IllegalArgumentException("El archivo debe tener al menos 1 bloque");
        mutex.acquireUninterruptibly();
        try {
            NodoSA padre = padreParaCrear(r);
            if (libres < bloques)
                throw new IllegalArgumentException("No hay bloques libres suficientes: se necesitan " + bloques + " y hay " + libres);
            NodoSA n = new NodoSA(nombreDe(r), TipoNodo.ARCHIVO, owner, permiso);
            n.creador = creador;
            n.padre = padre;
            n.pendiente = true;
            asignarEncadenado(n, bloques);
            padre.hijos.agregarAlFinal(n);
            version++;
            return n;
        } finally { mutex.release(); }
    }

    /** Asigna los 'cantidad' primeros bloques libres y los enlaza uno a uno. */
    private void asignarEncadenado(NodoSA n, int cantidad) {
        int previo = -1, restantes = cantidad;
        for (int b = 0; b < total && restantes > 0; b++) {
            if (dueno[b] != null) continue;
            dueno[b] = n;
            siguiente[b] = -1;
            if (previo >= 0) siguiente[previo] = b; else n.primerBloque = b;
            previo = b;
            restantes--;
        }
        n.bloques = cantidad;
        libres -= cantidad;
        n.color = colorSiguiente++ % PALETA;
    }

    public void confirmar(NodoSA n) {
        mutex.acquireUninterruptibly();
        try { n.pendiente = false; version++; } finally { mutex.release(); }
    }

    public void marcarEliminando(NodoSA n, boolean v) {
        mutex.acquireUninterruptibly();
        try { n.eliminando = v; version++; } finally { mutex.release(); }
    }

    public void renombrar(NodoSA n, String nuevo) {
        validarNombre(nuevo);
        mutex.acquireUninterruptibly();
        try {
            if (n.padre == null) throw new IllegalArgumentException("No se puede renombrar la raíz");
            NodoSA otro = hijo(n.padre, nuevo.trim());
            if (otro != null && otro != n) throw new IllegalArgumentException("Ya existe '" + nuevo + "' en ese directorio");
            n.nombre = nuevo.trim();
            version++;
        } finally { mutex.release(); }
    }

    /** Elimina un archivo (libera sus bloques) o un directorio con todo su contenido. */
    public void eliminar(NodoSA n) {
        mutex.acquireUninterruptibly();
        try {
            if (n.padre == null) throw new IllegalArgumentException("No se puede eliminar la raíz");
            liberarSubarbol(n);
            n.padre.hijos.quitar(n);
            n.padre = null;
            version++;
        } finally { mutex.release(); }
    }

    private void liberarSubarbol(NodoSA n) {
        if (n.tipo == TipoNodo.ARCHIVO) {
            int b = n.primerBloque;
            int guarda = 0;
            while (b >= 0 && guarda++ <= total) {
                int sig = siguiente[b];
                if (dueno[b] == n) { dueno[b] = null; siguiente[b] = -1; libres++; }
                b = sig;
            }
            n.primerBloque = -1;
            return;
        }
        ListaEnlazada<NodoSA> copia = n.hijos.copiar();
        for (int i = 0; i < copia.tamano(); i++) liberarSubarbol(copia.obtener(i));
    }

    /** Carga un nodo existente (JSON). 'cadena' puede ser null: entonces se usan 'bloques' bloques consecutivos desde 'inicio'. */
    public void cargarNodo(String ruta, TipoNodo tipo, String owner, Permiso permiso, int inicio, int bloques, int[] cadena) {
        String r = normalizar(ruta);
        mutex.acquireUninterruptibly();
        try {
            NodoSA padre = padreParaCrear(r);
            NodoSA n = new NodoSA(nombreDe(r), tipo, owner, permiso);
            n.padre = padre;
            if (tipo == TipoNodo.ARCHIVO) {
                int[] bl = cadena;
                if (bl == null) {
                    if (bloques < 1) throw new IllegalArgumentException("'" + r + "': el archivo debe tener al menos 1 bloque");
                    bl = new int[bloques];
                    for (int i = 0; i < bloques; i++) bl[i] = inicio + i;
                }
                for (int b : bl) {
                    if (b < 0 || b >= total) throw new IllegalArgumentException("'" + r + "': el bloque " + b + " está fuera del disco (0 a " + (total - 1) + ")");
                    if (dueno[b] != null) throw new IllegalArgumentException("'" + r + "': el bloque " + b + " ya pertenece a '" + dueno[b].ruta() + "'");
                }
                for (int i = 0; i < bl.length; i++) {
                    dueno[bl[i]] = n;
                    siguiente[bl[i]] = i + 1 < bl.length ? bl[i + 1] : -1;
                }
                n.primerBloque = bl[0];
                n.bloques = bl.length;
                libres -= bl.length;
                n.color = colorSiguiente++ % PALETA;
            }
            padre.hijos.agregarAlFinal(n);
            version++;
        } finally { mutex.release(); }
    }

    // ------------------------------------------------------------------ instantáneas para la GUI
    private NodoVista vista(NodoSA n, boolean conHijos) {
        NodoVista[] hs = new NodoVista[conHijos ? n.hijos.tamano() : 0];
        for (int i = 0; i < hs.length; i++) hs[i] = vista(n.hijos.obtener(i), true);
        return new NodoVista(n, hs);
    }

    public InstantaneaFS instantanea() {
        mutex.acquireUninterruptibly();
        try {
            InstantaneaFS s = new InstantaneaFS();
            s.totalBloques = total;
            s.libres = libres;
            s.version = version;
            s.raiz = vista(raiz, true);
            s.colorBloque = new int[total];
            s.nombreBloque = new String[total];
            s.siguienteBloque = new int[total];
            for (int i = 0; i < total; i++) {
                s.colorBloque[i] = dueno[i] == null ? -1 : dueno[i].color;
                s.nombreBloque[i] = dueno[i] == null ? "" : dueno[i].ruta();
                s.siguienteBloque[i] = dueno[i] == null ? -1 : siguiente[i];
            }
            ListaEnlazada<NodoVista> arch = new ListaEnlazada<>();
            reunirArchivos(s.raiz, arch);
            s.archivos = new NodoVista[arch.tamano()];
            for (int i = 0; i < s.archivos.length; i++) s.archivos[i] = arch.obtener(i);
            return s;
        } finally { mutex.release(); }
    }

    private void reunirArchivos(NodoVista n, ListaEnlazada<NodoVista> dest) {
        if (n.tipo == TipoNodo.ARCHIVO) dest.agregarAlFinal(n);
        for (NodoVista h : n.hijos) reunirArchivos(h, dest);
    }

    /** Recorre en preorden (directorios antes que su contenido) para guardar el estado en JSON. */
    public String aJson(int cabezalActual) {
        mutex.acquireUninterruptibly();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"diskConfig\": {\n    \"totalBlocks\": ").append(total)
              .append(",\n    \"initialHead\": ").append(cabezalActual).append("\n  },\n  \"fileSystem\": [");
            boolean[] primero = { true };
            escribirNodos(raiz, sb, primero);
            sb.append("\n  ]\n}\n");
            return sb.toString();
        } finally { mutex.release(); }
    }

    private void escribirNodos(NodoSA n, StringBuilder sb, boolean[] primero) {
        if (n.padre != null && !n.pendiente) {
            if (!primero[0]) sb.append(',');
            primero[0] = false;
            sb.append("\n    {\n      \"path\": ").append(avilaos.json.Json.escapar(n.ruta()))
              .append(",\n      \"type\": \"").append(n.tipo == TipoNodo.DIRECTORIO ? "DIRECTORY" : "FILE").append('"');
            if (n.tipo == TipoNodo.ARCHIVO) {
                sb.append(",\n      \"startBlock\": ").append(n.primerBloque).append(",\n      \"blocks\": ").append(n.bloques);
                int[] c = cadenaInterna(n);
                sb.append(",\n      \"chain\": [");
                for (int i = 0; i < c.length; i++) sb.append(i > 0 ? ", " : "").append(c[i]);
                sb.append(']');
            }
            sb.append(",\n      \"owner\": ").append(avilaos.json.Json.escapar(n.owner))
              .append(",\n      \"permissions\": \"").append(n.permiso.getJson()).append("\"\n    }");
        }
        for (int i = 0; i < n.hijos.tamano(); i++) escribirNodos(n.hijos.obtener(i), sb, primero);
    }
}
