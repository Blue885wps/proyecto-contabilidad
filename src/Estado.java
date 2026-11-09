import java.util.ArrayList;
import java.util.List;

public class Estado {
    // Tolerancias contables
    static final double TOLERANCIA = 0.005;
    static final double TOLERANCIA_SALDO = 0.005;
    static final double EPSILON = 1e-9;

    // ============================================================
    // ESTADO GLOBAL (listas paralelas)
    // ============================================================
    static List<String> cuentaCodigos = new ArrayList<>();
    static List<String> cuentaNombres = new ArrayList<>();
    static List<String> cuentaTipos = new ArrayList<>();
    static List<Double> cuentaSaldos = new ArrayList<>();
    static List<String> asientoFechas = new ArrayList<>();
    static List<String> asientoDescripciones = new ArrayList<>();
    static List<Double> asientoTotales = new ArrayList<>();
    static List<String> asientoTipos = new ArrayList<>(); // NORMAL | CIERRE (legacy)
    static List<List<String>> partidaCodigos = new ArrayList<>();
    static List<List<String>> partidaTipos = new ArrayList<>();
    static List<List<Double>> partidaMontos = new ArrayList<>();

    // Rango de reportes (opcion 12). Vacio = historico completo.
    static String rangoDesde = "";
    static String rangoHasta = "";

    static String vistaInicio() { return rangoDesde.isEmpty() ? Fechas.FECHA_MIN : rangoDesde; }
    static String vistaFin() { return rangoHasta.isEmpty() ? Fechas.FECHA_MAX : rangoHasta; }
    static String descripcionRango() { return rangoDesde.isEmpty() ? "Historico completo" : rangoDesde + " al " + rangoHasta; }
}
