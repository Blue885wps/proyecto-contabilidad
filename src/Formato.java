import java.text.Normalizer;
import java.util.Locale;

public class Formato {
    static double redondearMonto(double valor) { return Math.round(valor * 100.0) / 100.0; }

    static String formatearMonto(double valor) {
        if (Math.abs(valor) < Estado.TOLERANCIA) valor = 0;
        return String.format(Locale.ROOT, "%.2f", valor);
    }

    static String limpiarCampo(String valor) { return valor == null ? "" : valor.replace("\r", " ").replace("\n", " "); }

    static String recortar(String t, int a) { return t == null ? "" : (t.length() <= a ? t : t.substring(0, a)); }

    static String normalizarTexto(String t) {
        if (t == null) return "";
        String s = Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    static String etiquetaLado(String t) { return t.equalsIgnoreCase("Debito") ? "Debe" : "Haber"; }
}
