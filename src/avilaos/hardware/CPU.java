package avilaos.hardware;

import avilaos.modelo.Clockable;
import avilaos.modelo.EstadoProceso;
import avilaos.modelo.ModoCPU;
import avilaos.modelo.Proceso;
import avilaos.modelo.ResultadoInstruccion;
import avilaos.so.Computador;
import avilaos.so.NucleoSO;

/** Procesador: ejecuta una instrucción por ciclo del proceso que el SO le asigne. */
public class CPU implements Clockable {
    private final Computador computador;
    private final NucleoSO so;

    private volatile Proceso actual;
    private volatile ModoCPU modo = ModoCPU.SISTEMA_OPERATIVO;
    private volatile String instruccion = "-";
    private int ciclosEnCPU;
    private long ciclosOcupada, ciclosTotales, ocupadaVentana;

    public CPU(Computador computador, NucleoSO so) {
        this.computador = computador;
        this.so = so;
    }

    @Override public void tick(long ciclo) {
        ciclosTotales++;

        if (actual != null && so.debeExpropiar(actual, ciclosEnCPU)) {
            Proceso viejo = actual;
            actual = null;
            so.expropiar(viejo);
        }
        if (actual == null) {
            Proceso nuevo = so.despachar();
            if (nuevo != null) {
                actual = nuevo;
                ciclosEnCPU = 0;
                nuevo.setEstado(EstadoProceso.EJECUCION);
                if (nuevo.getCicloPrimeraCPU() < 0) nuevo.setCicloPrimeraCPU(ciclo);
                computador.registrar(computador.getNombre() + " selecciona el proceso " + nuevo.etiqueta()
                        + " (" + nuevo.getNombre() + ") con " + so.getPolitica().nombre());
            }
        }
        Proceso p = actual;
        if (p == null) {                           // CPU ociosa
            modo = ModoCPU.SISTEMA_OPERATIVO;
            instruccion = "(ociosa)";
            return;
        }

        modo = p.getPrograma().modoProximaInstruccion(p);
        instruccion = "PC=" + p.getPc() + " → " + p.descripcionInstruccion();
        ResultadoInstruccion r = p.getPrograma().ejecutar(p, computador);
        p.registrarCicloCPU();
        ciclosEnCPU++;
        ciclosOcupada++;
        ocupadaVentana++;

        switch (r) {
            case TERMINADO:
                actual = null;
                so.terminarNormal(p);
                break;
            case BLOQUEADO:
                actual = null;
                so.bloquear(p);
                break;
            default:
                break;
        }
    }

    /** Lo usa el SO cuando mata al proceso que está en la CPU (deadline). */
    public void liberar() { actual = null; }

    public long tomarOcupadaVentana() { long v = ocupadaVentana; ocupadaVentana = 0; return v; }

    public Proceso getActual() { return actual; }
    public ModoCPU getModo() { return modo; }
    public String getInstruccion() { return instruccion; }
    public long getCiclosOcupada() { return ciclosOcupada; }
    public long getCiclosTotales() { return ciclosTotales; }
}
