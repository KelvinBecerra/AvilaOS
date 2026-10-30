package avilaos.sistema;

import avilaos.modelo.TipoPolitica;
import avilaos.modelo.TipoProceso;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Lectura/escritura del perfil de simulación en CSV (una fila por elemento, primera columna = tipo de fila):
 *   CONFIG,msPorCiclo,latenciaRed,quantum,numComputadores
 *   COMPUTADOR,indice,ram,politica
 *   BUFFER,nombre,capacidad,hostIndice
 *   PROCESO,nombre,tipo,instrucciones,memoria,prioridad,deadline,computador,buffer,periodo,elementos,ciclosES,periodoES
 */
public final class ConfigCSV {
    private ConfigCSV() { }

    public static void guardar(File archivo, PerfilSimulacion perfil) throws IOException {
        ConfigSimulacion c = perfil.config;
        try (BufferedWriter w = new BufferedWriter(new FileWriter(archivo))) {
            w.write("CONFIG," + c.getMsPorCiclo() + "," + c.getLatenciaRed() + "," + c.getQuantum() + "," + c.getNumComputadores());
            w.newLine();
            for (int i = 0; i < c.getNumComputadores(); i++) {
                w.write("COMPUTADOR," + i + "," + c.getRam(i) + "," + c.getPolitica(i).name());
                w.newLine();
            }
            for (int i = 0; i < perfil.buffers.tamano(); i++) {
                EspecBuffer b = perfil.buffers.obtener(i);
                w.write("BUFFER," + b.nombre + "," + b.capacidad + "," + b.host);
                w.newLine();
            }
            for (int i = 0; i < perfil.procesos.tamano(); i++) {
                EspecProceso p = perfil.procesos.obtener(i);
                w.write("PROCESO," + p.nombre + "," + p.tipo.name() + "," + p.instrucciones + "," + p.memoria + ","
                        + p.prioridad + "," + p.deadline + "," + p.computador + "," + p.buffer + "," + p.periodo + ","
                        + p.elementos + "," + p.ciclosES + "," + p.periodoES);
                w.newLine();
            }
        }
    }

    /** @throws IllegalArgumentException con el número de línea si el archivo es inválido */
    public static PerfilSimulacion cargar(File archivo) throws IOException {
        PerfilSimulacion perfil = new PerfilSimulacion();
        try (BufferedReader r = new BufferedReader(new FileReader(archivo))) {
            String linea;
            int n = 0;
            while ((linea = r.readLine()) != null) {
                n++;
                linea = linea.trim();
                if (linea.isEmpty() || linea.startsWith("#")) continue;
                String[] f = linea.split(",", -1);
                try {
                    interpretar(perfil, f);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Línea " + n + ": valor numérico inválido (" + e.getMessage() + ")");
                } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
                    throw new IllegalArgumentException("Línea " + n + ": " + e.getMessage());
                }
            }
        }
        if (perfil.config == null) throw new IllegalArgumentException("El archivo no contiene la fila CONFIG");
        return perfil;
    }

    private static void interpretar(PerfilSimulacion perfil, String[] f) {
        switch (f[0]) {
            case "CONFIG": {
                ConfigSimulacion c = new ConfigSimulacion(Integer.parseInt(f[4].trim()));
                c.setMsPorCiclo(Integer.parseInt(f[1].trim()));
                c.setLatenciaRed(Integer.parseInt(f[2].trim()));
                c.setQuantum(Integer.parseInt(f[3].trim()));
                perfil.config = c;
                break;
            }
            case "COMPUTADOR": {
                if (perfil.config == null) throw new IllegalArgumentException("COMPUTADOR antes de CONFIG");
                int i = Integer.parseInt(f[1].trim());
                if (i < 0 || i >= perfil.config.getNumComputadores()) throw new IllegalArgumentException("índice de computador fuera de rango");
                int ram = Integer.parseInt(f[2].trim());
                if (ram < 1) throw new IllegalArgumentException("la RAM debe ser positiva");
                perfil.config.setRam(i, ram);
                perfil.config.setPolitica(i, TipoPolitica.valueOf(f[3].trim()));
                break;
            }
            case "BUFFER":
                perfil.buffers.agregarAlFinal(new EspecBuffer(f[1].trim(), Integer.parseInt(f[2].trim()), Integer.parseInt(f[3].trim())));
                break;
            case "PROCESO": {
                EspecProceso p = new EspecProceso();
                p.nombre = f[1].trim();
                p.tipo = TipoProceso.valueOf(f[2].trim());
                p.instrucciones = Integer.parseInt(f[3].trim());
                p.memoria = Integer.parseInt(f[4].trim());
                p.prioridad = Integer.parseInt(f[5].trim());
                p.deadline = Integer.parseInt(f[6].trim());
                p.computador = Integer.parseInt(f[7].trim());
                p.buffer = f[8].trim();
                p.periodo = Integer.parseInt(f[9].trim());
                p.elementos = Integer.parseInt(f[10].trim());
                p.ciclosES = Integer.parseInt(f[11].trim());
                p.periodoES = Integer.parseInt(f[12].trim());
                perfil.procesos.agregarAlFinal(p);
                break;
            }
            default:
                throw new IllegalArgumentException("tipo de fila desconocido '" + f[0] + "'");
        }
    }
}
