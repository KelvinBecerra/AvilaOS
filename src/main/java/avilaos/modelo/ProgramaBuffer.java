package avilaos.modelo;

import avilaos.so.Computador;
import avilaos.sync.BufferAcotado;
import avilaos.sync.SemaforoSO;

/**
 * Programa de un productor o consumidor. Cada paso es una instrucción de un ciclo.
 * producir()/consumir() dura 'periodo' ciclos en modo usuario; semWait, semSignal, anyadir
 * y extraer son instrucciones en modo sistema operativo. Si el buffer está en otro
 * computador, cada operación sobre él bloquea al proceso 'latencia' ciclos (red).
 */
public class ProgramaBuffer implements Programa {
    private static final PasoBuffer[] PRODUCTOR = {
        PasoBuffer.PRODUCIR, PasoBuffer.SEM_WAIT_VACIOS, PasoBuffer.SEM_WAIT_MUTEX,
        PasoBuffer.ANADIR, PasoBuffer.SEM_SIGNAL_MUTEX, PasoBuffer.SEM_SIGNAL_LLENOS };
    private static final PasoBuffer[] CONSUMIDOR = {
        PasoBuffer.SEM_WAIT_LLENOS, PasoBuffer.SEM_WAIT_MUTEX, PasoBuffer.EXTRAER,
        PasoBuffer.SEM_SIGNAL_MUTEX, PasoBuffer.SEM_SIGNAL_VACIOS, PasoBuffer.CONSUMIR };

    private final boolean productor;
    private final BufferAcotado buffer;
    private final int periodo, elementos;
    private final PasoBuffer[] pasos;
    private int idx, ciclosEnPaso, hechos;

    public ProgramaBuffer(boolean productor, BufferAcotado buffer, int periodo, int elementos) {
        this.productor = productor;
        this.buffer = buffer;
        this.periodo = periodo;
        this.elementos = elementos;
        this.pasos = productor ? PRODUCTOR : CONSUMIDOR;
    }

    /** Instrucciones totales estimadas (para el PCB / EDF). */
    public static int instruccionesEstimadas(int periodo, int elementos) { return elementos * (periodo + 5); }

    @Override public ModoCPU modoProximaInstruccion(Proceso p) {
        return pasos[idx].esOperacionBuffer() ? ModoCPU.SISTEMA_OPERATIVO : ModoCPU.USUARIO;
    }

    @Override public String descripcionProximaInstruccion(Proceso p) {
        PasoBuffer paso = pasos[idx];
        String s = paso.getTexto();
        if (paso.esOperacionBuffer()) s += " en " + buffer.getNombre();
        return s + "  [" + (hechos + 1) + "/" + elementos + "]";
    }

    @Override public ResultadoInstruccion ejecutar(Proceso p, Computador c) {
        PasoBuffer paso = pasos[idx];

        // Acceso remoto: primero se paga la latencia de red (el proceso se bloquea y libera la CPU).
        if (paso.esOperacionBuffer() && buffer.getHost() != c) {
            int lat = c.getConfig().getLatenciaRed();
            if (!p.isRedPagada() && lat > 0) {
                p.setRedPagada(true);
                p.bloquear(MotivoBloqueo.RED, lat, "red hacia " + buffer.getNombre() + " (" + lat + " ciclos)");
                c.registrar("Proceso " + p.etiqueta() + " accede al buffer " + buffer.getNombre()
                        + " de forma remota (" + paso.getTexto() + ", latencia " + lat + " ciclos)");
                return ResultadoInstruccion.BLOQUEADO;
            }
            p.setRedPagada(false);
        }

        switch (paso) {
            case PRODUCIR:
            case CONSUMIR:
                ciclosEnPaso++;
                if (ciclosEnPaso >= periodo) { ciclosEnPaso = 0; return avanzar(); }
                return ResultadoInstruccion.CONTINUA;
            case SEM_WAIT_VACIOS: return esperar(p, c, buffer.getVacios());
            case SEM_WAIT_LLENOS: return esperar(p, c, buffer.getLlenos());
            case SEM_WAIT_MUTEX:  return esperar(p, c, buffer.getMutex());
            case ANADIR:
                buffer.anadir();
                p.quitarTenido(buffer.getVacios());   // el espacio queda ocupado: ya no se "retiene"
                c.registrar("Proceso " + p.etiqueta() + " añade un elemento al buffer " + buffer.getNombre()
                        + " (" + buffer.getCantidad() + "/" + buffer.getCapacidad() + ")");
                return avanzar();
            case EXTRAER:
                buffer.extraer();
                p.quitarTenido(buffer.getLlenos());
                c.registrar("Proceso " + p.etiqueta() + " extrae un elemento del buffer " + buffer.getNombre()
                        + " (" + buffer.getCantidad() + "/" + buffer.getCapacidad() + ")");
                return avanzar();
            case SEM_SIGNAL_MUTEX:
                p.quitarTenido(buffer.getMutex());
                c.senalar(buffer.getMutex(), buffer);
                return avanzar();
            case SEM_SIGNAL_LLENOS:
                c.senalar(buffer.getLlenos(), buffer);
                return avanzar();
            case SEM_SIGNAL_VACIOS:
                c.senalar(buffer.getVacios(), buffer);
                return avanzar();
            default:
                return ResultadoInstruccion.CONTINUA;
        }
    }

    private ResultadoInstruccion esperar(Proceso p, Computador c, SemaforoSO sem) {
        boolean pasa = sem.semWait(p);
        ResultadoInstruccion r = avanzar();     // la instrucción cuenta como ejecutada aunque bloquee
        if (!pasa) {
            p.bloquear(MotivoBloqueo.SEMAFORO, 0, "semáforo '" + sem.getNombre() + "' de " + buffer.getNombre());
            c.registrar("Proceso " + p.etiqueta() + " se bloquea en el semáforo '" + sem.getNombre()
                    + "' del buffer " + buffer.getNombre());
            return ResultadoInstruccion.BLOQUEADO;
        }
        return r;
    }

    private ResultadoInstruccion avanzar() {
        idx++;
        if (idx == pasos.length) {
            idx = 0;
            hechos++;
            if (hechos >= elementos) return ResultadoInstruccion.TERMINADO;
        }
        return ResultadoInstruccion.CONTINUA;
    }

    /**
     * Si el proceso muere a mitad del protocolo, completa lo que dejó pendiente para no
     * corromper los contadores del buffer (los permisos retenidos se liberan en Proceso/NucleoSO).
     */
    @Override public void alTerminarAbruptamente(Proceso p, Computador c) {
        if (productor && (idx == 4 || idx == 5)) c.senalar(buffer.getLlenos(), buffer);   // elemento ya añadido
        if (!productor && (idx == 3 || idx == 4)) c.senalar(buffer.getVacios(), buffer);  // espacio ya liberado
    }
}
