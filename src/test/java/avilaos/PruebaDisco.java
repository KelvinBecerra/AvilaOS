package avilaos;

import avilaos.hardware.Disco;
import avilaos.modelo.*;

public class PruebaDisco {
    static int fallos = 0;
    // Ejecuta todas las solicitudes (llegadas en el ciclo 0) y devuelve el orden de atención
    static String correr(TipoPoliticaDisco pol, int cabezal0, int total, int[] bloques, long[] desplazamiento, int velocidad) {
        Disco d = new Disco(total, cabezal0, velocidad, pol);
        StringBuilder sb = new StringBuilder();
        d.setEscucha((s, c) -> sb.append(sb.length() == 0 ? "" : " ").append(s.getBloque()));
        for (int b : bloques) d.encolar(new SolicitudDisco(b, TipoOperacionES.LEER, null, "", 1), 0);
        for (long c = 1; c < 100000 && d.getTotalAtendidas() < bloques.length; c++) d.tick(c);
        desplazamiento[0] = d.getDesplazamientoTotal();
        return sb.toString();
    }
    static void verificar(String nombre, String obtenido, String esperado, long desp, long despEsperado) {
        boolean ok = obtenido.equals(esperado) && (despEsperado < 0 || desp == despEsperado);
        if (!ok) fallos++;
        System.out.printf("%-34s %s%n   orden: %s%s%n", nombre, ok ? "OK" : "FALLO", obtenido, despEsperado >= 0 ? "   desplazamiento: " + desp : "");
        if (!ok) System.out.println("   esperado: " + esperado + (despEsperado >= 0 ? "   desplazamiento: " + despEsperado : ""));
    }
    public static void main(String[] a) {
        long[] d = new long[1];
        int[] q = {95, 180, 34, 119, 11, 123, 62, 64};
        System.out.println("== Casos de prueba P1-P4 del enunciado (cabezal 50, 200 bloques, hacia mayores) ==");
        for (int v : new int[]{1000, 1}) {
            verificar("P1 FIFO (vel " + v + ")", correr(TipoPoliticaDisco.FIFO, 50, 200, q, d, v), "95 180 34 119 11 123 62 64", d[0], -1);
            verificar("P2 SSTF (vel " + v + ")", correr(TipoPoliticaDisco.SSTF, 50, 200, q, d, v), "62 64 34 11 95 119 123 180", d[0], -1);
            verificar("P3 SCAN (vel " + v + ")", correr(TipoPoliticaDisco.SCAN, 50, 200, q, d, v), "62 64 95 119 123 180 34 11", d[0], -1);
            verificar("P4 C-SCAN (vel " + v + ")", correr(TipoPoliticaDisco.C_SCAN, 50, 200, q, d, v), "62 64 95 119 123 180 11 34", d[0], -1);
        }
        System.out.println("== Anexo: ejemplo con desplazamientos (82,17,43,140,24,16,190) ==");
        int[] e = {82, 17, 43, 140, 24, 16, 190};
        verificar("Anexo FIFO", correr(TipoPoliticaDisco.FIFO, 50, 200, e, d, 7), "82 17 43 140 24 16 190", d[0], 518);
        verificar("Anexo SSTF", correr(TipoPoliticaDisco.SSTF, 50, 200, e, d, 7), "43 24 17 16 82 140 190", d[0], 208);
        verificar("Anexo SCAN", correr(TipoPoliticaDisco.SCAN, 50, 200, e, d, 7), "82 140 190 43 24 17 16", d[0], 332);
        verificar("Anexo C-SCAN (sin contar el salto)", correr(TipoPoliticaDisco.C_SCAN, 50, 200, e, d, 7), "82 140 190 16 17 24 43", d[0], 192);
        System.out.println("== Extras (no están en el enunciado) ==");
        verificar("LOOK", correr(TipoPoliticaDisco.LOOK, 50, 200, e, d, 7), "82 140 190 43 24 17 16", d[0], 140 + 0 + 0 + 0 + 0 + 0 + 0 > 0 ? (190 - 50) + (190 - 16) : 0);
        verificar("C-LOOK", correr(TipoPoliticaDisco.C_LOOK, 50, 200, e, d, 7), "82 140 190 16 17 24 43", d[0], (190 - 50) + (43 - 16));
        System.out.println(fallos == 0 ? "\nTODAS LAS PRUEBAS PASARON" : "\nFALLOS: " + fallos);
    }
}
