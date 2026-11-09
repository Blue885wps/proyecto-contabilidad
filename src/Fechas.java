import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public class Fechas {
    static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter FORMATO_FECHA_ESTRICTA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    static final String FECHA_MIN = "01/01/1900";
    static final String FECHA_MAX = "31/12/2100";

    static String fechaActual() { return LocalDate.now().format(FORMATO_FECHA); }

    static LocalDate parsearFecha(String s) {
        if (s == null) return null;
        try { return LocalDate.parse(s.trim(), FORMATO_FECHA_ESTRICTA); } catch (DateTimeParseException e) { return null; }
    }

    static String fechaOrdenable(String f) { String[] p = f.split("/"); return p.length == 3 ? p[2] + p[1] + p[0] : "99999999"; }

    static boolean fechaEnRango(String f, String d, String h) {
        String fo = fechaOrdenable(f);
        return fo.compareTo(fechaOrdenable(d)) >= 0 && fo.compareTo(fechaOrdenable(h)) <= 0;
    }
}
