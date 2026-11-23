# ÁvilaOS — Proyecto 2: sistema de archivos, planificación de disco y suspensión de procesos

Proyecto Maven en Java (sin librerías externas; JSON propio). En NetBeans: **File → Open Project** y elegir esta carpeta; ejecutar `avilaos.Main`.
Ejemplos del enunciado en `src/main/resources/ejemplos/` (también se cargan con el botón "Cargar el ejemplo del enunciado").
Pruebas por consola: `avilaos.PruebaSistema` (extremo a extremo) y `avilaos.PruebaDisco` (casos P1–P4 y anexo del enunciado).

## Qué agrega el Proyecto 2 sobre el Proyecto 1
| Paquete | Novedad |
|---|---|
| `hardware` | `Disco` (implementa `Clockable`): cabezal, cola única de E/S, una solicitud a la vez. |
| `modelo` | `PoliticaDisco` (interfaz), `SolicitudDisco`, `SeleccionDisco`, enums `TipoOperacionES`, `TipoPoliticaDisco`, `DireccionCabezal`, `ModoUsuario`, `Permiso`, `TipoNodo`; `EstadoProceso` pasa a siete estados; `ProgramaArchivo`. |
| `politicas` | `PoliticaDiscoFIFO/SSTF/SCAN/CSCAN/LOOK/CLOOK` + `FabricaPoliticasDisco`. |
| `archivos` | `SistemaArchivos` (árbol propio + asignación encadenada), `AreaSwap`, `ControlAcceso`, lectura/escritura JSON. |
| `json` | `Json`: lector/escritor JSON propio. |
| `so` | `ServicioArchivos`; `NucleoSO` con planificador a mediano plazo; `Computador` ensambla disco, FS y swap. |
| `gui` | Carga inicial (2 JSON), Vista 2 (computador + disco), Vista 1 (JTree + tabla + procesos de E/S), métricas. |

## Decisiones de diseño (justificar en la defensa)
- **Cada operación de archivo la ejecuta un proceso** (`ProgramaArchivo`): N−1 instrucciones de usuario, la última (modo SO) genera la solicitud y bloquea al proceso (E/S); el disco lo despierta y una última instrucción SO completa la operación. La GUI solo crea el proceso.
- **Asignación encadenada**: cada bloque guarda el siguiente; los archivos nuevos toman los primeros bloques libres. Los archivos del JSON se cargan en bloques consecutivos desde `startBlock` (o según `chain`, que se escribe al guardar el estado).
- **Crear** reserva los bloques al generar la solicitud (el archivo aparece "creando…" hasta que el disco termina). **Eliminar** libra los bloques al terminar la solicitud. **Actualizar** reescribe los bloques y, si hay nombre nuevo, renombra. Directorios: crear, renombrar y eliminar vacíos no usan el disco; eliminar un directorio recorre todos los bloques de su contenido.
- **Archivos de varios bloques**: la política decide por el primer bloque; cada bloque cuesta un ciclo y los saltos entre bloques de la cadena cuentan en el desplazamiento.
- **Área de intercambio**: partición propia (los archivos del ejemplo llegan al bloque 180 con 200 bloques). Sus solicitudes se planifican en la posición del borde exterior (último bloque).
- **C-SCAN / C-LOOK**: el salto de regreso no se cuenta (como en el anexo del enunciado). **SCAN** llega hasta el extremo del disco; **LOOK** no.
- **Mediano plazo**: si un proceso nuevo no cabe, suspende procesos BLOQUEADOS: primero el de menor prioridad y, a igual prioridad, el que lleva más tiempo bloqueado; si todos los candidatos juntos no liberan lo suficiente, no suspende a nadie. Los nuevos se admiten antes que reactivar procesos Listo/Suspendido (para no quitarles la memoria liberada). Reactivar elige por prioridad.
- **Deadline**: también vencen los procesos suspendidos; sus solicitudes salen del disco, sus bloques de swap se liberan y un CREAR a medias se deshace.
- **Modo usuario**: lee archivos propios o públicos; crea archivos solo en directorios propios; actualiza o elimina solo archivos propios; no crea ni elimina directorios ni renombra. Los rechazos se explican sin detener la simulación.
- **Retener el disco** (casilla en la Vista 2): permite acumular solicitudes para reproducir los casos P2–P4 del enunciado, que suponen toda la cola presente.
