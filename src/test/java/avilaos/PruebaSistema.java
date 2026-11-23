package avilaos;

import avilaos.archivos.*;
import avilaos.modelo.*;
import avilaos.sistema.*;
import avilaos.so.*;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Prueba de punta a punta (por consola) de las funciones del Proyecto 2. Se ejecuta con: java avilaos.PruebaSistema */
public class PruebaSistema {
    static int fallos = 0, pruebas = 0;

    static String recurso(String n) throws Exception {
        try (InputStream in = PruebaSistema.class.getResourceAsStream("/ejemplos/" + n)) { return new String(in.readAllBytes(), "UTF-8"); }
    }
    static void ok(String nombre, boolean cond, String detalle) {
        pruebas++;
        if (!cond) fallos++;
        System.out.println((cond ? "  OK     " : "  FALLO  ") + nombre + (detalle.isEmpty() ? "" : "  -> " + detalle));
    }
    static boolean esperar(java.util.function.BooleanSupplier c, long ms) throws Exception {
        long t0 = System.currentTimeMillis();
        while (System.currentTimeMillis() - t0 < ms) { if (c.getAsBoolean()) return true; Thread.sleep(2); }
        return c.getAsBoolean();
    }
    static boolean todosTerminados(Cluster c) {
        EstadoComputador e = c.getUnico().getNucleo().instantanea();
        return e.nuevos.length + e.listos.length + e.bloqueados.length + e.listosSuspendidos.length + e.bloqueadosSuspendidos.length == 0
                && e.proceso.equals("—");
    }
    static String ordenDisco(Cluster c) {
        StringBuilder sb = new StringBuilder();
        Matcher m = Pattern.compile("en el bloque (\\d+) \\(").matcher(c.getRegistro().leerDesde(0).texto);
        while (m.find()) sb.append(sb.length() == 0 ? "" : " ").append(m.group(1));
        return sb.toString();
    }
    static EspecProcesoES esp(String n, TipoOperacionES op, String ruta, int mem, int tam) {
        EspecProcesoES e = new EspecProcesoES();
        e.nombre = n; e.operacion = op; e.ruta = ruta; e.memoria = mem; e.tamanoArchivo = tam; e.deadline = 5000; e.instrucciones = 2;
        return e;
    }

    public static void main(String[] a) throws Exception {
        String fsJson = recurso("sistema_archivos.json"), prJson = recurso("procesos.json");

        System.out.println("== T1. Casos P1-P4 del enunciado, de punta a punta (procesos -> CPU -> disco), disco reteniendo solicitudes ==");
        String[][] casos = { {"FIFO", "95 180 34 119 11 123 62 64"}, {"SSTF", "62 64 34 11 95 119 123 180"},
                             {"SCAN", "62 64 95 119 123 180 34 11"}, {"C-SCAN", "62 64 95 119 123 180 11 34"} };
        for (String[] cs : casos) {
            PerfilProcesos pp = PerfilProcesos.leer(prJson.replace("\"FIFO\"", "\"" + cs[0] + "\""));
            Cluster c = Cluster.desdePerfiles(FsJson.leer(fsJson), pp);
            c.getConfig().setMsPorCiclo(1);
            c.getUnico().getDisco().setRetenido(true);
            c.reanudar();
            boolean llenas = esperar(() -> c.getUnico().getDisco().copiarCola().tamano() == 8, 10000);
            c.getUnico().getDisco().setRetenido(false);
            boolean fin = esperar(() -> todosTerminados(c), 20000);
            MetricasComputador m = c.metricasPorComputador()[0];
            ok(cs[0] + ": orden de atención", llenas && fin && ordenDisco(c).equals(cs[1]), ordenDisco(c));
            ok(cs[0] + ": 8 procesos completados, 0 vencidos", m.completados == 8 && m.vencidos == 0, m.completados + "/" + m.vencidos);
            if (cs[0].equals("FIFO")) {
                InstantaneaFS fs = c.getUnico().getFs().instantanea();
                ok("DELETE de img1.png liberó su bloque (119)", c.getUnico().getFs().consultar("/usuarios/img1.png") == null && fs.colorBloque[119] == -1, "");
            }
            c.detener();
        }

        System.out.println("== T2. CRUD con asignación encadenada ==");
        {
            Cluster c = Cluster.desdePerfiles(FsJson.leer(fsJson), PerfilProcesos.leer(prJson.replace("\"processes\": [", "\"processes\": []; ").replaceAll("(?s)\"processes\":.*\\}", "\"processes\": []}")));
            c.getConfig().setMsPorCiclo(1); c.getUnico().getDisco().setVelocidad(100);
            SistemaArchivos fs = c.getUnico().getFs();
            int libresAntes = fs.getLibres();
            c.reanudar();
            c.crearProcesoArchivo(esp("crear", TipoOperacionES.CREAR, "/sistema/nuevo.dat", 8, 5));
            ok("CREAR archivo de 5 bloques", esperar(() -> { NodoVista n = fs.consultar("/sistema/nuevo.dat"); return n != null && !n.pendiente; }, 10000), "");
            NodoVista n = fs.consultar("/sistema/nuevo.dat");
            int[] cad = fs.cadena(fs.buscar("/sistema/nuevo.dat"));
            ok("el archivo ocupa 5 bloques encadenados", n.bloques == 5 && cad.length == 5 && fs.getLibres() == libresAntes - 5, java.util.Arrays.toString(cad));
            InstantaneaFS ins = fs.instantanea();
            boolean enlazados = true;
            for (int i = 0; i + 1 < cad.length; i++) enlazados &= ins.siguienteBloque[cad[i]] == cad[i + 1];
            ok("cada bloque apunta al siguiente (último = -1)", enlazados && ins.siguienteBloque[cad[cad.length - 1]] == -1, "");
            EspecProcesoES u = esp("rename", TipoOperacionES.ACTUALIZAR, "/sistema/nuevo.dat", 8, 0); u.nuevoNombre = "nuevo2.dat";
            c.crearProcesoArchivo(u);
            ok("ACTUALIZAR cambia el nombre", esperar(() -> fs.consultar("/sistema/nuevo2.dat") != null && fs.consultar("/sistema/nuevo.dat") == null, 10000), "");
            c.crearProcesoArchivo(esp("leer", TipoOperacionES.LEER, "/sistema/nuevo2.dat", 8, 0));
            ok("LEER termina", esperar(() -> todosTerminados(c), 10000), "");
            c.crearProcesoArchivo(esp("borrar", TipoOperacionES.ELIMINAR, "/sistema/nuevo2.dat", 8, 0));
            ok("ELIMINAR libera los 5 bloques", esperar(() -> fs.consultar("/sistema/nuevo2.dat") == null, 10000) && fs.getLibres() == libresAntes, "libres " + fs.getLibres());
            c.crearProcesoArchivo(esp("borrardir", TipoOperacionES.ELIMINAR, "/usuarios", 8, 0));
            ok("ELIMINAR directorio borra todo su contenido",
               esperar(() -> fs.consultar("/usuarios") == null, 10000) && fs.consultar("/usuarios/video.mp4") == null && fs.getLibres() == libresAntes + 4, "libres " + fs.getLibres());
            EspecProcesoES d = esp("mkdir", TipoOperacionES.CREAR, "/datos", 8, 0);
            c.crearProcesoArchivo(d);
            ok("CREAR directorio (sin acceso a disco)", esperar(() -> fs.consultar("/datos") != null, 10000), "");
            c.crearProcesoArchivo(esp("faltante", TipoOperacionES.LEER, "/sistema/noexiste.txt", 8, 0));
            ok("LEER archivo inexistente: proceso rechazado, simulación sigue",
               esperar(() -> c.getRegistro().leerDesde(0).texto.contains("RECHAZADO") && todosTerminados(c), 10000), "");
            c.detener();
        }

        System.out.println("== T3. Modo usuario: permisos y rechazos explicados ==");
        {
            Cluster c = Cluster.desdePerfiles(FsJson.leer(fsJson), PerfilProcesos.leer("{\"globalConfig\":{\"cycleDurationMs\":10,\"swapSizeBlocks\":10,\"initialDiskPolicy\":\"FIFO\"},\"computer\":{\"ramCapacity\":100,\"policy\":\"FCFS\",\"quantum\":0},\"processes\":[]}"));
            String[][] intentos = {
                {"user2", "LEER", "/usuarios/notas.txt", "0", "privado"},
                {"user2", "LEER", "/usuarios/data.csv", "0", null},
                {"user1", "LEER", "/usuarios/video.mp4", "0", null},
                {"user1", "ACTUALIZAR", "/sistema/boot.bin", "0", "protegidos"},
                {"user1", "ELIMINAR", "/usuarios/data.csv", "0", "pertenece"},
                {"user1", "CREAR", "/usuarios/otro.txt", "3", "propios directorios"},
                {"user1", "CREAR", "/usuarios/dir", "0", "directorios"},
                {"user1", "ELIMINAR", "/usuarios/notas.txt", "0", null} };
            for (String[] t : intentos) {
                EspecProcesoES e = esp("p", TipoOperacionES.valueOf(t[1]), t[2], 8, Integer.parseInt(t[3]));
                e.modo = ModoUsuario.USUARIO; e.usuario = t[0];
                String res = null;
                try { c.crearProcesoArchivo(e); } catch (IllegalArgumentException ex) { res = ex.getMessage(); }
                boolean bien = t[4] == null ? res == null : (res != null && res.toLowerCase().contains(t[4]));
                ok(t[0] + " " + t[1] + " " + t[2], bien, res == null ? "permitido" : res);
            }
            c.detener();
        }

        System.out.println("== T4. Suspensión y reactivación (RAM 40, procesos de 16, disco lento) ==");
        {
            String pr = "{\"globalConfig\":{\"cycleDurationMs\":1,\"swapSizeBlocks\":100,\"initialDiskPolicy\":\"FIFO\"},"
                    + "\"computer\":{\"ramCapacity\":40,\"policy\":\"FCFS\",\"quantum\":0},\"processes\":[";
            String[] rutas = {"/sistema/config.sys", "/usuarios/video.mp4", "/usuarios/data.csv", "/sistema/boot.bin", "/sistema/script.py"};
            for (int i = 0; i < rutas.length; i++)
                pr += (i > 0 ? "," : "") + "{\"name\":\"P" + i + "\",\"memory\":16,\"deadline\":5000,\"instructions\":2,\"targetPath\":\"" + rutas[i] + "\",\"operation\":\"READ\",\"fileSizeBlocks\":0}";
            pr += "]}";
            Cluster c = Cluster.desdePerfiles(FsJson.leer(fsJson), PerfilProcesos.leer(pr));
            c.getConfig().setMsPorCiclo(1); c.getUnico().getDisco().setVelocidad(1);
            c.reanudar();
            boolean fin = esperar(() -> todosTerminados(c), 30000);
            MetricasComputador m = c.metricasPorComputador()[0];
            Computador comp = c.getUnico();
            ok("los 5 procesos terminan", fin && m.completados == 5 && m.vencidos == 0, m.completados + " completados");
            ok("hubo suspensiones y reactivaciones", comp.getNucleo().getSuspensiones() >= 1 && comp.getNucleo().getReactivaciones() >= 1,
               comp.getNucleo().getSuspensiones() + " susp / " + comp.getNucleo().getReactivaciones() + " react");
            ok("RAM y swap quedan vacíos", comp.getRam().getUsada() == 0 && comp.getSwap().getLibres() == comp.getSwap().getTotal(), "RAM usada " + comp.getRam().getUsada());
            String log = c.getRegistro().leerDesde(0).texto;
            ok("el log registra la suspensión y la reactivación", log.contains("Se suspende al proceso") && log.contains("Se reactiva al proceso"), "");
            c.detener();
        }

        System.out.println("== T5. Deadline durante una E/S de disco y durante la creación de un archivo ==");
        {
            Cluster c = Cluster.desdePerfiles(FsJson.leer(fsJson), PerfilProcesos.leer("{\"globalConfig\":{\"cycleDurationMs\":1,\"swapSizeBlocks\":50,\"initialDiskPolicy\":\"FIFO\"},\"computer\":{\"ramCapacity\":100,\"policy\":\"FCFS\",\"quantum\":0},\"processes\":[]}"));
            c.getConfig().setMsPorCiclo(1); c.getUnico().getDisco().setVelocidad(1);
            SistemaArchivos fs = c.getUnico().getFs();
            int libres = fs.getLibres();
            EspecProcesoES e = esp("muere", TipoOperacionES.CREAR, "/sistema/efimero.bin", 8, 6); e.deadline = 15;
            c.crearProcesoArchivo(e);
            c.reanudar();
            boolean fin = esperar(() -> todosTerminados(c), 10000);
            MetricasComputador m = c.metricasPorComputador()[0];
            ok("el proceso vence por deadline", fin && m.vencidos == 1, "vencidos " + m.vencidos);
            ok("no queda el archivo a medias: bloques liberados", fs.consultar("/sistema/efimero.bin") == null && fs.getLibres() == libres, "libres " + fs.getLibres() + "/" + libres);
            ok("su solicitud salió de la cola del disco", c.getUnico().getDisco().copiarCola().tamano() == 0, "");
            c.detener();
        }

        System.out.println("== T6. Persistencia JSON (guardar el estado y volver a cargarlo) ==");
        {
            Cluster c = Cluster.desdePerfiles(FsJson.leer(fsJson), PerfilProcesos.leer(prJson));
            c.getConfig().setMsPorCiclo(1);
            c.reanudar();
            esperar(() -> todosTerminados(c), 20000);
            c.pausar();
            String guardado = c.getUnico().getFs().aJson(c.getUnico().getDisco().getCabezal());
            PerfilFS pf = FsJson.leer(guardado);
            SistemaArchivos nuevo = new SistemaArchivos(pf.totalBloques);
            FsJson.aplicar(pf, nuevo);
            InstantaneaFS a1 = c.getUnico().getFs().instantanea(), a2 = nuevo.instantanea();
            boolean igual = a1.libres == a2.libres && a1.archivos.length == a2.archivos.length;
            for (int i = 0; i < a1.totalBloques; i++) igual &= a1.colorBloque[i] == -1 ? a2.colorBloque[i] == -1 : a2.colorBloque[i] != -1;
            ok("el estado guardado se recarga idéntico (archivos y bloques)", igual, a1.archivos.length + " archivos, " + a1.libres + " bloques libres");
            ok("el cabezal guardado es la posición final", pf.cabezalInicial == c.getUnico().getDisco().getCabezal(), "cabezal " + pf.cabezalInicial);
            c.detener();
        }

        System.out.println("== T7. Validación de los JSON (entradas inválidas) ==");
        String[][] malos = {
            {"fs", "{ \"diskConfig\": { \"totalBlocks\": 200 ", "JSON inválido"},
            {"fs", "{ \"diskConfig\": { \"totalBlocks\": \"abc\" }, \"fileSystem\": [] }", "entero"},
            {"fs", "{ \"diskConfig\": { \"totalBlocks\": 200, \"initialHead\": 500 }, \"fileSystem\": [] }", "initialHead"},
            {"fs", "{ \"diskConfig\": { \"totalBlocks\": 200 }, \"fileSystem\": [ {\"path\":\"/a\",\"type\":\"FILE\",\"startBlock\":300,\"blocks\":1,\"owner\":\"x\",\"permissions\":\"public\"} ] }", "fuera del disco"},
            {"pr", prJson.replace("\"READ\"", "\"BORRAR_TODO\""), "operation"},
            {"pr", prJson.replace("\"FCFS\"", "\"MAGIA\""), "policy"},
            {"pr", prJson.replace("\"memory\": 16", "\"memory\": -3"), "memory"} };
        for (String[] t : malos) {
            String msg = null;
            try { if (t[0].equals("fs")) FsJson.leer(t[1]); else PerfilProcesos.leer(t[1]); } catch (IllegalArgumentException ex) { msg = ex.getMessage(); }
            ok("rechaza: " + t[2], msg != null && msg.contains(t[2]), msg == null ? "(aceptó)" : msg);
        }
        System.out.println("== T8. Continuidad con el Proyecto 1: las cuatro políticas de CPU siguen funcionando con procesos de E/S ==");
        for (String pol : new String[] { "FCFS", "EDF", "RR", "PRIORITY" }) {
            String pr = prJson.replace("\"FCFS\"", "\"" + pol + "\"").replace("\"quantum\": 0", "\"quantum\": 2");
            Cluster c = Cluster.desdePerfiles(FsJson.leer(fsJson), PerfilProcesos.leer(pr));
            c.getConfig().setMsPorCiclo(1);
            c.reanudar();
            boolean fin = esperar(() -> todosTerminados(c), 20000);
            MetricasComputador m = c.metricasPorComputador()[0];
            ok("CPU " + pol + ": 8 procesos completados, RAM liberada", fin && m.completados == 8 && c.getUnico().getRam().getUsada() == 0,
               m.completados + " completados, utilización " + String.format("%.0f", m.utilizacion()) + "%");
            c.detener();
        }
        System.out.println("\n" + (fallos == 0 ? "TODAS LAS PRUEBAS PASARON (" + pruebas + ")" : "FALLOS: " + fallos + " de " + pruebas));
        System.exit(fallos == 0 ? 0 : 1);
    }
}
