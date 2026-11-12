import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;

public class Datos {
    static final double TOLERANCIA = 0.005, EPSILON = 1e-9;
    static final String ARCHIVO_CUENTAS = "data/cuentas.txt", ARCHIVO_ASIENTOS = "data/asientos.txt", ARCHIVO_BALANCE = "data/balance.txt";
    static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter FORMATO_FECHA_ESTRICTA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter FORMATO_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    static final String[] TIPOS_CUENTA = {"Activo", "Pasivo", "Patrimonio", "Ingreso", "Costo", "Gasto"};

    static boolean datosInvalidos = false;
    static List<String> cuentaCodigos = new ArrayList<>(), cuentaNombres = new ArrayList<>(), cuentaTipos = new ArrayList<>();
    static List<Double> cuentaSaldos = new ArrayList<>();
    static List<String> asientoFechas = new ArrayList<>(), asientoDescripciones = new ArrayList<>();
    static List<Double> asientoTotales = new ArrayList<>();
    static List<List<String>> partidaCodigos = new ArrayList<>(), partidaTipos = new ArrayList<>();
    static List<List<Double>> partidaMontos = new ArrayList<>();

    // Catalogo estandar de 75 cuentas: codigo|nombre|tipo, separadas por ;
    static final String[] CATALOGO_ESTANDAR = {
        "1000|Caja|Activo;1100|Bancos|Activo;1110|Inversiones temporales|Activo;1200|Cuentas por cobrar|Activo;1210|Clientes|Activo;1220|Documentos por cobrar|Activo;1230|Deudores diversos|Activo;1300|Inventarios|Activo;1310|Materias primas|Activo;1320|Productos terminados|Activo;1400|Activos fijos|Activo;1410|Mobiliario y equipo|Activo;1420|Equipo de transporte|Activo;1430|Equipo de computo|Activo;1500|Activos diferidos|Activo;1510|Gastos pagados por anticipado|Activo;1520|Seguros pagados por anticipado|Activo;1600|Cuentas por cobrar a largo plazo|Activo;1700|Propiedad, planta y equipo|Activo;1790|Depreciacion acumulada|Activo;1800|Activos intangibles|Activo;1890|Amortizacion acumulada|Activo",
        "2000|Cuentas por pagar|Pasivo;2010|Proveedores|Pasivo;2020|Acreedores diversos|Pasivo;2100|Prestamos bancarios|Pasivo;2110|Prestamos a corto plazo|Pasivo;2120|Prestamos a largo plazo|Pasivo;2200|Impuestos por pagar|Pasivo;2210|IVA por pagar|Pasivo;2220|ISR por pagar|Pasivo;2300|Pasivos diferidos|Pasivo;2310|Anticipos de clientes|Pasivo;2320|Intereses cobrados por anticipado|Pasivo;2400|Pasivos por impuestos diferidos|Pasivo;2500|Obligaciones financieras a largo plazo|Pasivo;2600|Pasivos por beneficios a empleados|Pasivo",
        "3000|Capital social|Patrimonio;3010|Capital aportado|Patrimonio;3020|Capital ganado|Patrimonio;3100|Utilidades retenidas|Patrimonio;3110|Utilidad del ejercicio|Patrimonio;3120|Perdida del ejercicio|Patrimonio;3200|Resultados de ejercicios anteriores|Patrimonio;3300|Reservas legales|Patrimonio;3400|Ajustes por revaluacion|Patrimonio;3500|Dividendos decretados|Patrimonio",
        "4000|Ingresos|Ingreso;4010|Ventas|Ingreso;4020|Ingresos por servicios|Ingreso;4030|Ingresos por intereses|Ingreso;4040|Otros ingresos|Ingreso;4050|Ingresos por alquileres|Ingreso;4060|Ingresos por comisiones|Ingreso;4070|Ingresos por dividendos|Ingreso",
        "5000|Costos|Costo;5010|Costo de ventas|Costo;5020|Costo de produccion|Costo;5030|Costo de lo vendido|Costo",
        "6000|Gastos|Gasto;6010|Gastos de venta|Gasto;6020|Gastos de administracion|Gasto;6030|Gastos financieros|Gasto;6040|Gastos de operacion|Gasto;6050|Sueldos y salarios|Gasto;6060|Renta|Gasto;6070|Servicios publicos|Gasto;6080|Depreciacion|Gasto;6090|Amortizacion|Gasto;6100|Gastos de publicidad y mercadeo|Gasto;6110|Gastos de mantenimiento|Gasto;6120|Gastos de seguros|Gasto;6130|Gastos de impuestos|Gasto;6140|Gastos de capacitacion|Gasto;6150|Gastos de viajes y representacion|Gasto"
    };

    // FECHAS Y FORMATO
    static String fechaActual() { return LocalDate.now().format(FORMATO_FECHA); }

    static LocalDate parsearFecha(String texto) {
        if (texto == null) return null;
        try { return LocalDate.parse(texto.trim(), FORMATO_FECHA_ESTRICTA); }
        catch (DateTimeParseException e) { return null; }
    }

    static String fechaOrdenable(String fecha) {
        String[] p = fecha.split("/");
        return p.length == 3 ? p[2] + p[1] + p[0] : "99999999";
    }

    static String formatearMonto(double valor) {
        if (Math.abs(valor) < TOLERANCIA) valor = 0;
        return String.format(Locale.ROOT, "%.2f", valor);
    }

    static String limpiarCampo(String valor) { return valor == null ? "" : valor.replace("\r", " ").replace("\n", " "); }
    static String recortar(String texto, int ancho) { return texto == null ? "" : (texto.length() <= ancho ? texto : texto.substring(0, ancho)); }
    static String etiquetaLado(String tipo) { return tipo.equalsIgnoreCase("Debito") ? "Debe" : "Haber"; }

    // CUENTAS
    static void inicializarCuentas() {
        cuentaCodigos.clear(); cuentaNombres.clear(); cuentaTipos.clear(); cuentaSaldos.clear();
        for (String grupo : CATALOGO_ESTANDAR) for (String linea : grupo.split(";")) {
            String[] p = linea.split("\\|");
            agregarCuenta(p[0], p[1], p[2]);
        }
    }

    static void agregarCuenta(String codigo, String nombre, String tipo) {
        cuentaCodigos.add(codigo); cuentaNombres.add(nombre); cuentaTipos.add(tipo); cuentaSaldos.add(0.0);
    }

    static boolean tipoValido(String tipo) { return Arrays.asList(TIPOS_CUENTA).contains(tipo); }

    static int indiceCuenta(String codigo) {
        for (int i = 0; i < cuentaCodigos.size(); i++) if (cuentaCodigos.get(i).equals(codigo)) return i;
        return -1;
    }

    static String nombreCuenta(String codigo) {
        int i = indiceCuenta(codigo);
        return i < 0 ? "(no existe)" : cuentaNombres.get(i);
    }

    static String pedirCuenta(String contexto) {
        while (true) {
            String codigo = Contabilidad.leerEntrada(contexto + " (codigo, 0 = cancelar): ");
            if (codigo == null || codigo.equals("0")) return null;
            int i = indiceCuenta(codigo);
            if (i >= 0) {
                System.out.println("  " + codigo + " - " + cuentaNombres.get(i) + " (" + cuentaTipos.get(i) + ")");
                return codigo;
            }
            System.out.println("La cuenta no existe. Consulte el catalogo en la opcion 1.1.");
        }
    }

    static void mostrarCatalogo() {
        System.out.println("\n=== CATALOGO DE CUENTAS ===");
        System.out.printf("%-8s %-35s %-12s %12s%n", "Codigo", "Nombre", "Tipo", "Saldo");
        System.out.println("-".repeat(70));
        for (int i = 0; i < cuentaCodigos.size(); i++)
            System.out.printf("%-8s %-35s %-12s %12.2f%n", cuentaCodigos.get(i), cuentaNombres.get(i), cuentaTipos.get(i), cuentaSaldos.get(i));
    }

    static void crearCuenta() {
        if (datosInvalidos) { System.out.println("No se pueden crear cuentas: hay datos invalidos en los archivos."); return; }
        System.out.println("\n--- NUEVA CUENTA ---");
        String nombre = Contabilidad.leerTextoPersistible("Nombre de la cuenta: ", "nombre");
        if (Contabilidad.entradaCerrada || nombre == null) return;
        String tipo = pedirTipoCuenta();
        if (Contabilidad.entradaCerrada) return;
        String codigo;
        while (true) {
            codigo = Contabilidad.leerEntrada("Codigo de la cuenta (ej: 1010): ");
            if (codigo == null) return;
            if (codigo.equals("0") || codigo.equals("?") || codigo.contains("|")) {
                System.out.println("Codigo invalido. No use 0, ? ni el caracter |."); continue;
            }
            if (indiceCuenta(codigo) >= 0) { System.out.println("El codigo ya existe. Ingrese otro."); continue; }
            break;
        }
        agregarCuenta(codigo, nombre, tipo);
        if (!guardarCuentas()) {
            int u = cuentaCodigos.size() - 1;
            cuentaCodigos.remove(u); cuentaNombres.remove(u); cuentaTipos.remove(u); cuentaSaldos.remove(u);
            System.out.println("No se pudo guardar la cuenta."); return;
        }
        System.out.println("Cuenta creada: " + codigo + " - " + nombre + " (" + tipo + ").");
    }

    static String pedirTipoCuenta() {
        System.out.println("Tipo de cuenta:");
        for (int i = 0; i < TIPOS_CUENTA.length; i++) System.out.println("  " + (i + 1) + ". " + TIPOS_CUENTA[i]);
        return TIPOS_CUENTA[Contabilidad.leerInt("Seleccione (1-6): ", 1, 6) - 1];
    }

    // PERSISTENCIA
    static void cargarDatos() {
        new File("data").mkdirs();
        if (!cargarCuentasTexto()) {
            System.out.println("Inicializando catalogo de cuentas...");
            inicializarCuentas();
            if (!datosInvalidos) guardarCuentas();
        } else if (cuentaCodigos.isEmpty()) {
            System.out.println("ADVERTENCIA: no hay cuentas validas en " + ARCHIVO_CUENTAS + ".");
            System.out.println("Se usara el catalogo estandar en memoria; no se guardara hasta corregir el archivo.");
            inicializarCuentas();
            datosInvalidos = true;
        }
        cargarAsientosTexto();
        Asientos.recalcularSaldos();
    }

    static boolean cargarCuentasTexto() {
        cuentaCodigos.clear(); cuentaNombres.clear(); cuentaTipos.clear(); cuentaSaldos.clear();
        File archivo = new File(ARCHIVO_CUENTAS);
        if (!archivo.exists()) return false;
        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea; int numero = 0;
            while ((linea = reader.readLine()) != null) {
                numero++;
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1);
                if (linea.trim().isEmpty()) continue;
                String[] p = linea.split("\\|", -1);
                if (p.length != 4) { advertirCuenta(numero, "se esperaban 4 campos"); continue; }
                String codigo = p[0].trim(), nombre = p[1].trim(), tipo = p[2].trim();
                if (codigo.isEmpty() || nombre.isEmpty()) { advertirCuenta(numero, "codigo o nombre vacio"); continue; }
                if (!tipoValido(tipo)) { advertirCuenta(numero, "tipo de cuenta invalido"); continue; }
                double saldo;
                try {
                    saldo = Double.parseDouble(p[3].trim());
                    if (!Double.isFinite(saldo)) throw new NumberFormatException();
                } catch (NumberFormatException e) { advertirCuenta(numero, "saldo invalido"); continue; }
                if (indiceCuenta(codigo) >= 0) { advertirCuenta(numero, "codigo duplicado " + codigo); continue; }
                agregarCuenta(codigo, nombre, tipo);
                cuentaSaldos.set(cuentaSaldos.size() - 1, saldo);
            }
        } catch (IOException e) {
            System.out.println("Error leyendo " + ARCHIVO_CUENTAS + ": " + e.getMessage());
            datosInvalidos = true;
        }
        return true;
    }

    static void advertirCuenta(int numero, String mensaje) {
        System.out.println("ADVERTENCIA " + ARCHIVO_CUENTAS + " linea " + numero + ": " + mensaje + ". Linea omitida.");
        datosInvalidos = true;
    }

    static void cargarAsientosTexto() {
        asientoFechas.clear(); asientoDescripciones.clear(); asientoTotales.clear();
        partidaCodigos.clear(); partidaTipos.clear(); partidaMontos.clear();
        File archivo = new File(ARCHIVO_ASIENTOS);
        if (!archivo.exists()) return;
        List<String> bloque = new ArrayList<>();
        int lineaInicio = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea; int numero = 0;
            while ((linea = reader.readLine()) != null) {
                numero++;
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1);
                if (linea.trim().isEmpty()) continue;
                String etiqueta = linea.split("\\|", -1)[0].trim();
                if (etiqueta.equals("ASIENTO")) {
                    if (!bloque.isEmpty()) cargarBloque(bloque, lineaInicio);
                    bloque.clear(); bloque.add(linea); lineaInicio = numero;
                } else if (!bloque.isEmpty()) {
                    bloque.add(linea);
                } else {
                    System.out.println("ADVERTENCIA " + ARCHIVO_ASIENTOS + " linea " + numero + ": linea desconocida. Omitida.");
                    datosInvalidos = true;
                }
            }
            if (!bloque.isEmpty()) cargarBloque(bloque, lineaInicio);
        } catch (IOException e) {
            System.out.println("Error leyendo " + ARCHIVO_ASIENTOS + ": " + e.getMessage());
            datosInvalidos = true;
        }
    }

    static void cargarBloque(List<String> bloque, int numero) {
        String[] encabezado = bloque.get(0).split("\\|", -1);
        String error = encabezado.length != 4 ? "se esperaban 4 campos en ASIENTO" : null;
        String fecha = encabezado.length > 1 ? encabezado[1].trim() : "";
        String descripcion = encabezado.length > 2 ? encabezado[2].trim() : "";
        if (error == null && (fecha.isEmpty() || descripcion.isEmpty())) error = "fecha o descripcion vacia";
        LocalDate fechaValida = null;
        if (error == null) {
            fechaValida = parsearFecha(fecha);
            if (fechaValida == null) error = "fecha invalida";
        }
        List<String> codigos = new ArrayList<>(), tipos = new ArrayList<>();
        List<Double> montos = new ArrayList<>();
        for (int i = 1; i < bloque.size() && error == null; i++) {
            String[] p = bloque.get(i).split("\\|", -1);
            if (!p[0].trim().equals("PARTIDA") || p.length != 4) { error = "linea invalida dentro del asiento"; break; }
            String codigo = p[1].trim(), tipo = p[2].trim();
            if (tipo.equalsIgnoreCase("Debito")) tipo = "Debito";
            else if (tipo.equalsIgnoreCase("Credito")) tipo = "Credito";
            else { error = "tipo de partida invalido"; break; }
            if (indiceCuenta(codigo) < 0) { error = "la cuenta " + codigo + " no existe"; break; }
            double monto;
            try {
                monto = Double.parseDouble(p[3].trim());
                if (!Double.isFinite(monto) || monto <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) { error = "monto invalido"; break; }
            codigos.add(codigo); tipos.add(tipo); montos.add(monto);
        }
        if (error == null && codigos.size() < 2) error = "minimo 2 partidas";
        if (error == null) {
            double debe = Asientos.totalLado(tipos, montos, "Debito"), haber = Asientos.totalLado(tipos, montos, "Credito");
            if (debe <= 0 || haber <= 0) error = "debe haber partidas al Debe y al Haber";
            else if (!Asientos.cuadra(debe, haber)) error = "no cuadra: Debe " + formatearMonto(debe) + ", Haber " + formatearMonto(haber);
        }
        if (error != null) {
            System.out.println("ADVERTENCIA " + ARCHIVO_ASIENTOS + " linea " + numero + ": " + error + ". Asiento omitido.");
            datosInvalidos = true; return;
        }
        Asientos.agregarAsiento(fechaValida.format(FORMATO_FECHA), descripcion, codigos, tipos, montos, Asientos.totalLado(tipos, montos, "Debito"));
    }

    static void guardarDatos() {
        if (datosInvalidos) { System.out.println("No se guardaron cambios: hay datos invalidos en los archivos."); return; }
        if (!guardarAsientos() || !guardarCuentas()) System.out.println("Error guardando.");
    }

    static boolean guardarCuentas() {
        StringBuilder contenido = new StringBuilder();
        for (int i = 0; i < cuentaCodigos.size(); i++)
            contenido.append(limpiarCampo(cuentaCodigos.get(i))).append('|').append(limpiarCampo(cuentaNombres.get(i))).append('|')
                    .append(limpiarCampo(cuentaTipos.get(i))).append('|').append(formatearMonto(cuentaSaldos.get(i))).append(System.lineSeparator());
        return escribirArchivoAtomico(ARCHIVO_CUENTAS, contenido.toString());
    }

    static boolean guardarAsientos() {
        StringBuilder contenido = new StringBuilder();
        for (int i = 0; i < asientoFechas.size(); i++) {
            contenido.append("ASIENTO|").append(limpiarCampo(asientoFechas.get(i))).append('|')
                    .append(limpiarCampo(asientoDescripciones.get(i))).append('|').append(formatearMonto(asientoTotales.get(i))).append(System.lineSeparator());
            List<String> codigos = partidaCodigos.get(i), tipos = partidaTipos.get(i);
            List<Double> montos = partidaMontos.get(i);
            for (int j = 0; j < codigos.size(); j++)
                contenido.append("PARTIDA|").append(limpiarCampo(codigos.get(j))).append('|').append(limpiarCampo(tipos.get(j))).append('|')
                        .append(formatearMonto(montos.get(j))).append(System.lineSeparator());
        }
        return escribirArchivoAtomico(ARCHIVO_ASIENTOS, contenido.toString());
    }

    static boolean escribirArchivoAtomico(String ruta, String contenido) {
        File temporal = new File(ruta + ".tmp");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(temporal))) {
            writer.write(contenido);
        } catch (IOException e) {
            System.out.println("Error escribiendo " + ruta + ": " + e.getMessage()); temporal.delete(); return false;
        }
        try {
            Files.move(temporal.toPath(), new File(ruta).toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (AtomicMoveNotSupportedException e) {
            try { Files.move(temporal.toPath(), new File(ruta).toPath(), StandardCopyOption.REPLACE_EXISTING); return true; }
            catch (IOException ex) { System.out.println("Error escribiendo " + ruta + ": " + ex.getMessage()); temporal.delete(); return false; }
        } catch (IOException e) {
            System.out.println("Error escribiendo " + ruta + ": " + e.getMessage()); temporal.delete(); return false;
        }
    }

    static boolean guardarBalance(String fecha, double activo, double pasivo, double patrimonio) {
        return escribirArchivoAtomico(ARCHIVO_BALANCE, fecha + "|" + formatearMonto(activo) + "|" + formatearMonto(pasivo) + "|" + formatearMonto(patrimonio) + System.lineSeparator());
    }

    static void respaldarInformacion() {
        System.out.println("\n--- RESPALDO ---");
        File carpeta = new File("backup/manual_" + LocalDateTime.now().format(FORMATO_TIMESTAMP));
        if (!carpeta.mkdirs()) { System.out.println("No se pudo crear la carpeta de respaldo."); return; }
        String[] archivos = {ARCHIVO_CUENTAS, ARCHIVO_ASIENTOS, ARCHIVO_BALANCE};
        int copiados = 0;
        try {
            for (String ruta : archivos) {
                File origen = new File(ruta);
                if (origen.exists()) {
                    Files.copy(origen.toPath(), new File(carpeta, origen.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    copiados++;
                }
            }
        } catch (IOException e) {
            System.out.println("Error creando respaldo: " + e.getMessage());
            File[] creados = carpeta.listFiles();
            if (creados != null) for (File f : creados) f.delete();
            carpeta.delete(); return;
        }
        if (copiados == 0) { carpeta.delete(); System.out.println("No hay datos para respaldar."); return; }
        System.out.println("Respaldo creado en: " + carpeta.getPath() + " (" + copiados + " archivos).");
    }
}
