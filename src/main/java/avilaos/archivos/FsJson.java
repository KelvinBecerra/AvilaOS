package avilaos.archivos;

import avilaos.estructuras.ListaEnlazada;
import avilaos.json.Json;
import avilaos.modelo.Permiso;
import avilaos.modelo.TipoNodo;

/** Lectura y aplicación del JSON del sistema de archivos. Todas las validaciones lanzan IllegalArgumentException con un mensaje claro. */
public final class FsJson {
    private FsJson() { }

    public static PerfilFS leer(String texto) {
        Json.Obj raiz = Json.leerObjeto(texto);
        PerfilFS p = new PerfilFS();
        Json.Obj dc = raiz.obj("diskConfig");
        long total = dc.entero("totalBlocks");
        if (total < 16 || total > 2000) throw new IllegalArgumentException("diskConfig.totalBlocks debe estar entre 16 y 2000 (valor: " + total + ")");
        long cab = dc.entero("initialHead", 0);
        if (cab < 0 || cab >= total) throw new IllegalArgumentException("diskConfig.initialHead debe estar entre 0 y " + (total - 1) + " (valor: " + cab + ")");
        p.totalBloques = (int) total;
        p.cabezalInicial = (int) cab;
        Json.Arr fs = raiz.arr("fileSystem");
        for (int i = 0; i < fs.tamano(); i++) {
            Json.Obj o = fs.obj(i);
            PerfilFS.Entrada e = new PerfilFS.Entrada();
            String donde = "fileSystem[" + i + "]";
            try {
                e.ruta = SistemaArchivos.normalizar(o.texto("path"));
                String t = o.texto("type").trim().toUpperCase();
                if (t.equals("DIRECTORY")) e.tipo = TipoNodo.DIRECTORIO;
                else if (t.equals("FILE")) e.tipo = TipoNodo.ARCHIVO;
                else throw new IllegalArgumentException("type debe ser DIRECTORY o FILE (valor: " + t + ")");
                e.owner = o.texto("owner");
                if (e.owner.trim().isEmpty()) throw new IllegalArgumentException("owner está vacío");
                e.permiso = Permiso.desdeTexto(o.texto("permissions"));
                if (e.tipo == TipoNodo.ARCHIVO) {
                    e.inicio = (int) o.entero("startBlock");
                    e.bloques = (int) o.entero("blocks");
                    if (e.bloques < 1) throw new IllegalArgumentException("blocks debe ser al menos 1");
                    if (e.inicio < 0 || e.inicio >= total) throw new IllegalArgumentException("startBlock fuera del disco (0 a " + (total - 1) + ")");
                    if (o.tiene("chain") && o.get("chain") != null) {
                        Json.Arr c = o.arr("chain");
                        e.cadena = new int[c.tamano()];
                        for (int k = 0; k < e.cadena.length; k++) e.cadena[k] = (int) c.entero(k);
                    }
                }
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(donde + ": " + ex.getMessage());
            }
            p.entradas.agregarAlFinal(e);
        }
        return p;
    }

    /** Crea el árbol y asigna los bloques. Admite que los directorios aparezcan en cualquier orden. */
    public static void aplicar(PerfilFS p, SistemaArchivos fs) {
        ListaEnlazada<PerfilFS.Entrada> pend = p.entradas.copiar();
        while (!pend.estaVacia()) {
            boolean progreso = false;
            String primerError = null;
            ListaEnlazada<PerfilFS.Entrada> copia = pend.copiar();
            for (int i = 0; i < copia.tamano(); i++) {
                PerfilFS.Entrada e = copia.obtener(i);
                try {
                    fs.cargarNodo(e.ruta, e.tipo, e.owner, e.permiso, e.inicio, e.bloques, e.cadena);
                    pend.quitar(e);
                    progreso = true;
                } catch (IllegalArgumentException ex) {
                    if (primerError == null) primerError = ex.getMessage();
                }
            }
            if (!progreso) throw new IllegalArgumentException(primerError);
        }
    }
}
