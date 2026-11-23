package avilaos.modelo;

import avilaos.so.Computador;
import avilaos.so.ServicioArchivos;

/**
 * Programa de un proceso de E/S sobre archivos (crear, leer, actualizar, eliminar). Pasa por las colas del Proyecto 1
 * como cualquier proceso: ejecuta N-1 instrucciones de usuario y la última, en modo SISTEMA OPERATIVO, genera la
 * solicitud al disco y se bloquea (motivo E/S). Cuando el disco termina lo despierta y una última instrucción
 * (modo SO) completa la operación. Nunca es la GUI quien opera sobre el disco.
 */
public class ProgramaArchivo implements Programa {
    private final int total;
    private int ejecutadas;
    private boolean solicitada;

    public ProgramaArchivo(int total) { this.total = Math.max(1, total); }

    private boolean proximaEsSolicitud() { return !solicitada && ejecutadas + 1 >= total; }

    @Override public ModoCPU modoProximaInstruccion(Proceso p) {
        return (solicitada || proximaEsSolicitud()) ? ModoCPU.SISTEMA_OPERATIVO : ModoCPU.USUARIO;
    }

    @Override public String descripcionProximaInstruccion(Proceso p) {
        if (solicitada) return "completar la operación (el disco ya atendió)";
        if (proximaEsSolicitud()) return "solicitud al disco: " + p.getOperacion().getTexto() + " " + p.getRutaArchivo();
        return "preparar operación " + (ejecutadas + 1) + "/" + total;
    }

    @Override public ResultadoInstruccion ejecutar(Proceso p, Computador c) {
        if (solicitada) return ResultadoInstruccion.TERMINADO;
        ejecutadas++;
        if (ejecutadas < total) return ResultadoInstruccion.CONTINUA;
        ServicioArchivos.Resultado r = c.getServicioArchivos().iniciar(p);
        if (r == ServicioArchivos.Resultado.ESPERA_DISCO) {
            solicitada = true;
            p.bloquear(MotivoBloqueo.ENTRADA_SALIDA, 0, "disco: " + p.getOperacion().getTexto() + " " + p.getRutaArchivo());
            return ResultadoInstruccion.BLOQUEADO;
        }
        return ResultadoInstruccion.TERMINADO;      // sin acceso al disco (directorios) o solicitud rechazada
    }

    @Override public void alTerminarAbruptamente(Proceso p, Computador c) { }
}
