package avilaos.sistema;

import avilaos.estructuras.ListaEnlazada;

/** Configuración completa de una simulación (lo que se guarda y se carga desde CSV). */
public class PerfilSimulacion {
    public ConfigSimulacion config;
    public final ListaEnlazada<EspecBuffer> buffers = new ListaEnlazada<>();
    public final ListaEnlazada<EspecProceso> procesos = new ListaEnlazada<>();
}
