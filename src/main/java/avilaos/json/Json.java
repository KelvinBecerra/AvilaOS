package avilaos.json;

/**
 * Lector/escritor JSON mínimo y propio (sin librerías ni colecciones de java.util).
 * Objetos -> Json.Obj, arreglos -> Json.Arr, números enteros -> Long, decimales -> Double, texto -> String,
 * true/false -> Boolean, null -> null.
 */
public final class Json {
    private Json() { }

    public static final class Obj {
        private String[] claves = new String[8];
        private Object[] valores = new Object[8];
        private int n;

        void poner(String k, Object v) {
            if (n == claves.length) {
                String[] c2 = new String[n * 2]; Object[] v2 = new Object[n * 2];
                System.arraycopy(claves, 0, c2, 0, n); System.arraycopy(valores, 0, v2, 0, n);
                claves = c2; valores = v2;
            }
            claves[n] = k; valores[n] = v; n++;
        }
        public boolean tiene(String k) { for (int i = 0; i < n; i++) if (claves[i].equals(k)) return true; return false; }
        public Object get(String k) { for (int i = 0; i < n; i++) if (claves[i].equals(k)) return valores[i]; return null; }

        public String texto(String k) {
            Object o = get(k);
            if (!(o instanceof String)) throw new IllegalArgumentException("El campo '" + k + "' debe ser un texto");
            return (String) o;
        }
        public String texto(String k, String def) { return tiene(k) && get(k) != null ? texto(k) : def; }
        public long entero(String k) {
            Object o = get(k);
            if (!(o instanceof Long)) throw new IllegalArgumentException("El campo '" + k + "' debe ser un número entero");
            return (Long) o;
        }
        public long entero(String k, long def) { return tiene(k) && get(k) != null ? entero(k) : def; }
        public Obj obj(String k) {
            Object o = get(k);
            if (!(o instanceof Obj)) throw new IllegalArgumentException("Falta el objeto '" + k + "'");
            return (Obj) o;
        }
        public Arr arr(String k) {
            Object o = get(k);
            if (!(o instanceof Arr)) throw new IllegalArgumentException("Falta el arreglo '" + k + "'");
            return (Arr) o;
        }
    }

    public static final class Arr {
        private Object[] items = new Object[8];
        private int n;
        void agregar(Object o) {
            if (n == items.length) { Object[] i2 = new Object[n * 2]; System.arraycopy(items, 0, i2, 0, n); items = i2; }
            items[n++] = o;
        }
        public int tamano() { return n; }
        public Object get(int i) { return items[i]; }
        public Obj obj(int i) {
            if (!(items[i] instanceof Obj)) throw new IllegalArgumentException("El elemento " + (i + 1) + " debe ser un objeto");
            return (Obj) items[i];
        }
        public long entero(int i) {
            if (!(items[i] instanceof Long)) throw new IllegalArgumentException("El elemento " + (i + 1) + " debe ser un entero");
            return (Long) items[i];
        }
    }

    // ------------------------------------------------------------------ lectura
    public static Object leer(String s) {
        Lector l = new Lector(s);
        l.blancos();
        Object r = l.valor();
        l.blancos();
        if (l.i < s.length()) throw l.error("texto sobrante después del JSON");
        return r;
    }

    public static Obj leerObjeto(String s) {
        Object o = leer(s);
        if (!(o instanceof Obj)) throw new IllegalArgumentException("El JSON debe ser un objeto { ... }");
        return (Obj) o;
    }

    private static final class Lector {
        final String s; int i;
        Lector(String s) { this.s = s == null ? "" : s; }

        IllegalArgumentException error(String m) {
            int linea = 1, col = 1;
            for (int k = 0; k < Math.min(i, s.length()); k++) { if (s.charAt(k) == '\n') { linea++; col = 1; } else col++; }
            return new IllegalArgumentException("JSON inválido (línea " + linea + ", columna " + col + "): " + m);
        }
        void blancos() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

        Object valor() {
            if (i >= s.length()) throw error("se esperaba un valor");
            char c = s.charAt(i);
            if (c == '{') return objeto();
            if (c == '[') return arreglo();
            if (c == '"') return cadena();
            if (c == '-' || (c >= '0' && c <= '9')) return numero();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            throw error("carácter inesperado '" + c + "'");
        }
        Obj objeto() {
            Obj o = new Obj();
            i++; blancos();
            if (i < s.length() && s.charAt(i) == '}') { i++; return o; }
            while (true) {
                blancos();
                if (i >= s.length() || s.charAt(i) != '"') throw error("se esperaba el nombre de un campo entre comillas");
                String k = cadena();
                blancos();
                if (i >= s.length() || s.charAt(i) != ':') throw error("se esperaba ':'");
                i++; blancos();
                o.poner(k, valor());
                blancos();
                if (i >= s.length()) throw error("objeto sin cerrar");
                char c = s.charAt(i++);
                if (c == '}') return o;
                if (c != ',') { i--; throw error("se esperaba ',' o '}'"); }
            }
        }
        Arr arreglo() {
            Arr a = new Arr();
            i++; blancos();
            if (i < s.length() && s.charAt(i) == ']') { i++; return a; }
            while (true) {
                blancos();
                a.agregar(valor());
                blancos();
                if (i >= s.length()) throw error("arreglo sin cerrar");
                char c = s.charAt(i++);
                if (c == ']') return a;
                if (c != ',') { i--; throw error("se esperaba ',' o ']'"); }
            }
        }
        String cadena() {
            StringBuilder sb = new StringBuilder();
            i++;
            while (true) {
                if (i >= s.length()) throw error("texto sin cerrar");
                char c = s.charAt(i++);
                if (c == '"') return sb.toString();
                if (c == '\\') {
                    if (i >= s.length()) throw error("escape incompleto");
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n': sb.append('\n'); break;
                        case 't': sb.append('\t'); break;
                        case 'r': sb.append('\r'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'u':
                            if (i + 4 > s.length()) throw error("escape \\u incompleto");
                            try { sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); }
                            catch (NumberFormatException ex) { throw error("escape \\u inválido"); }
                            i += 4;
                            break;
                        default: sb.append(e);
                    }
                } else sb.append(c);
            }
        }
        Object numero() {
            int ini = i;
            if (s.charAt(i) == '-') i++;
            while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            boolean dec = false;
            if (i < s.length() && s.charAt(i) == '.') { dec = true; i++; while (i < s.length() && Character.isDigit(s.charAt(i))) i++; }
            if (i < s.length() && (s.charAt(i) == 'e' || s.charAt(i) == 'E')) {
                dec = true; i++;
                if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
                while (i < s.length() && Character.isDigit(s.charAt(i))) i++;
            }
            String t = s.substring(ini, i);
            try { return dec ? (Object) Double.valueOf(t) : (Object) Long.valueOf(t); }
            catch (NumberFormatException e) { i = ini; throw error("número inválido '" + t + "'"); }
        }
    }

    // ------------------------------------------------------------------ escritura
    public static String escapar(String t) {
        StringBuilder sb = new StringBuilder("\"");
        for (int k = 0; k < t.length(); k++) {
            char c = t.charAt(k);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: if (c < 0x20) sb.append(String.format("\\u%04x", (int) c)); else sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
