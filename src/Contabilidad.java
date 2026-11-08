import java.io.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.*;
import java.time.format.*;
import java.util.*;
public class Contabilidad {
    static final String ARCHIVO_CUENTAS = "data/cuentas.txt";
    static final String ARCHIVO_ASIENTOS = "data/asientos.txt";
    static final String ARCHIVO_BALANCE = "data/balance.txt";
    static final Scanner scanner = new Scanner(System.in);
    static final String[] TIPOS_CUENTA = {"Activo", "Pasivo", "Patrimonio", "Ingreso", "Costo", "Gasto"};
    static final List<String> TIPOS = Arrays.asList(TIPOS_CUENTA);
    static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter FORMATO_FECHA_ESTRICTA = DateTimeFormatter.ofPattern("dd/MM/uuuu")
            .withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter FORMATO_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    static final double TOLERANCIA = 0.005;
    static final double TOLERANCIA_SALDO = 0.005;
    static final double EPSILON = 1e-9;
    // Catalogo estandar de 75 cuentas: codigo|nombre|tipo
    static final String[] CATALOGO_ESTANDAR = {
        "1000|Caja|Activo", "1100|Bancos|Activo", "1110|Inversiones temporales|Activo",
        "1200|Cuentas por cobrar|Activo", "1210|Clientes|Activo", "1220|Documentos por cobrar|Activo",
        "1230|Deudores diversos|Activo", "1300|Inventarios|Activo", "1310|Materias primas|Activo",
        "1320|Productos terminados|Activo", "1400|Activos fijos|Activo", "1410|Mobiliario y equipo|Activo",
        "1420|Equipo de transporte|Activo", "1430|Equipo de computo|Activo", "1500|Activos diferidos|Activo",
        "1510|Gastos pagados por anticipado|Activo", "1520|Seguros pagados por anticipado|Activo",
        "1600|Cuentas por cobrar a largo plazo|Activo", "1700|Propiedad, planta y equipo|Activo",
        "1790|Depreciacion acumulada|Activo", "1800|Activos intangibles|Activo", "1890|Amortizacion acumulada|Activo",
        "2000|Cuentas por pagar|Pasivo", "2010|Proveedores|Pasivo", "2020|Acreedores diversos|Pasivo",
        "2100|Prestamos bancarios|Pasivo", "2110|Prestamos a corto plazo|Pasivo", "2120|Prestamos a largo plazo|Pasivo",
        "2200|Impuestos por pagar|Pasivo", "2210|IVA por pagar|Pasivo", "2220|ISR por pagar|Pasivo",
        "2300|Pasivos diferidos|Pasivo", "2310|Anticipos de clientes|Pasivo", "2320|Intereses cobrados por anticipado|Pasivo",
        "2400|Pasivos por impuestos diferidos|Pasivo", "2500|Obligaciones financieras a largo plazo|Pasivo",
        "2600|Pasivos por beneficios a empleados|Pasivo",
        "3000|Capital social|Patrimonio", "3010|Capital aportado|Patrimonio", "3020|Capital ganado|Patrimonio",
        "3100|Utilidades retenidas|Patrimonio", "3110|Utilidad del ejercicio|Patrimonio", "3120|Perdida del ejercicio|Patrimonio",
        "3200|Resultados del ejercicio|Patrimonio", "3300|Reservas legales|Patrimonio",
        "3400|Ajustes por revaluacion|Patrimonio", "3500|Dividendos decretados|Patrimonio",
        "4000|Ingresos|Ingreso", "4010|Ventas|Ingreso", "4020|Ingresos por servicios|Ingreso",
        "4030|Ingresos por intereses|Ingreso", "4040|Otros ingresos|Ingreso", "4050|Ingresos por alquileres|Ingreso",
        "4060|Ingresos por comisiones|Ingreso", "4070|Ingresos por dividendos|Ingreso",
        "5000|Costos|Costo", "5010|Costo de ventas|Costo", "5020|Costo de produccion|Costo", "5030|Costo de lo vendido|Costo",
        "6000|Gastos|Gasto", "6010|Gastos de venta|Gasto", "6020|Gastos de administracion|Gasto",
        "6030|Gastos financieros|Gasto", "6040|Gastos de operacion|Gasto", "6050|Sueldos y salarios|Gasto",
        "6060|Renta|Gasto", "6070|Servicios publicos|Gasto", "6080|Depreciacion|Gasto", "6090|Amortizacion|Gasto",
        "6100|Gastos de publicidad y mercadeo|Gasto", "6110|Gastos de mantenimiento|Gasto",
        "6120|Gastos de seguros|Gasto", "6130|Gastos de impuestos|Gasto",
        "6140|Gastos de capacitacion|Gasto", "6150|Gastos de viajes y representacion|Gasto"
    };
    static List<String> cuentaCodigos = new ArrayList<>();
    static List<String> cuentaNombres = new ArrayList<>();
    static List<String> cuentaTipos = new ArrayList<>();
    static List<Double> cuentaSaldos = new ArrayList<>();
    static List<String> asientoFechas = new ArrayList<>();
    static List<String> asientoDescripciones = new ArrayList<>();
    static List<Double> asientoTotales = new ArrayList<>();
    static List<List<String>> partidaCodigos = new ArrayList<>();
    static List<List<String>> partidaTipos = new ArrayList<>();
    static List<List<Double>> partidaMontos = new ArrayList<>();
    static boolean bloquearGuardadoAsientos = false;
    static boolean bloquearGuardadoCuentas = false;
    static boolean saldoContrarioEnReporte = false;
    public static void main(String[] args) {
        System.out.println("=== SISTEMA CONTABLE EMPRESARIAL ===");
        cargarDatos();
        while (true) {
            mostrarMenu();
            switch (leerInt("Seleccione una opcion (0-11): ", 0, 11)) {
                case 1: menuAdministrarCuentas(); break;
                case 2: registrarAsiento(); break;
                case 3: menuConsultarCuentasT(); break;
                case 4: verLibroDiario(); break;
                case 5: verLibroMayor(); break;
                case 6: verBalanceComprobacion(); break;
                case 7: verEstadoResultados(); break;
                case 8: generarBalance(); break;
                case 9: menuConsultarMovimientos(); break;
                case 10: respaldarInformacion(); break;
                case 11: case 0: salir(); return;
            }
        }
    }
    static void linea(String... textos) { for (String texto : textos) System.out.println(texto); }
    static boolean cuadra(double debe, double haber) { return Math.abs(debe - haber) <= TOLERANCIA + EPSILON; }
    static boolean tipoDeudor(String tipoCuenta) { return tipoCuenta.equals("Activo") || tipoCuenta.equals("Costo") || tipoCuenta.equals("Gasto"); }
    static boolean esCuentaContraria(String codigo) {
        return codigo.equals("1790") || codigo.equals("1890") || codigo.equals("3120") || codigo.equals("3500");
    }
    static double totalLado(List<String> tips, List<Double> mons, String lado) {
        double total = 0;
        for (int i = 0; i < tips.size() && i < mons.size(); i++) {
            if (tips.get(i).equalsIgnoreCase(lado)) total += mons.get(i);
        }
        return total;
    }
    static void agregarAsiento(String fecha, String descripcion, List<String> cods,
            List<String> tips, List<Double> mons, double total) {
        asientoFechas.add(fecha); asientoDescripciones.add(descripcion); asientoTotales.add(total);
        partidaCodigos.add(cods); partidaTipos.add(tips); partidaMontos.add(mons);
    }
    static void quitarUltimoAsiento() {
        int ultimo = asientoFechas.size() - 1;
        if (ultimo < 0) return;
        asientoFechas.remove(ultimo); asientoDescripciones.remove(ultimo); asientoTotales.remove(ultimo);
        partidaCodigos.remove(ultimo); partidaTipos.remove(ultimo); partidaMontos.remove(ultimo);
    }
    static void mostrarMenu() {
        linea("\n===========================================",
                "       SISTEMA CONTABLE EMPRESARIAL",
                "===========================================",
                "1. Administrar cuentas", "2. Registrar operacion contable",
                "3. Consultar cuentas T", "4. Generar libro diario",
                "5. Generar libro mayor", "6. Generar balanza de comprobacion",
                "7. Generar estado de resultados", "8. Generar balance general",
                "9. Consultar movimientos", "10. Respaldar informacion", "11. Salir (o 0)");
    }
    static int menu(String titulo, String... opciones) {
        System.out.println("\n--- " + titulo + " ---");
        for (int i = 0; i < opciones.length; i++) System.out.println((i + 1) + ". " + opciones[i]);
        System.out.println("0. Volver");
        return leerInt("Seleccione una opcion (0-" + opciones.length + "): ", 0, opciones.length);
    }
    static void menuAdministrarCuentas() {
        while (true) {
            switch (menu("ADMINISTRAR CUENTAS", "Ver catalogo", "Crear cuenta", "Buscar cuenta")) {
                case 1: mostrarCuentas(); break; case 2: crearCuenta(); break;
                case 3: buscarCuenta(); break; case 0: return;
            }
        }
    }
    static void menuConsultarCuentasT() {
        while (true) {
            switch (menu("CONSULTAR CUENTAS T", "Consultar una cuenta",
                    "Cuentas con movimientos", "Cuentas T de todo el catalogo")) {
                case 1: verCuentaTIndividual(); break; case 2: verCuentasTConMovimientos(); break;
                case 3: verTodasCuentasT(); break; case 0: return;
            }
        }
    }
    static void menuConsultarMovimientos() {
        while (true) {
            switch (menu("CONSULTAR MOVIMIENTOS", "Ver asientos registrados", "Buscar asientos")) {
                case 1: mostrarAsientos(); break; case 2: buscarAsientos(); break; case 0: return;
            }
        }
    }
    static void cargarDatos() {
        File dir = new File("data");
        if (!dir.exists()) dir.mkdirs();
        if (!cargarCuentasTexto(ARCHIVO_CUENTAS)) {
            bloquearGuardadoCuentas = true;
        } else if (cuentaCodigos.isEmpty()) {
            if (new File(ARCHIVO_CUENTAS).exists()) {
                linea("ADVERTENCIA: " + ARCHIVO_CUENTAS + " existe pero no tiene cuentas validas.",
                        "No se inicializara el catalogo para no sobrescribir el archivo.");
                bloquearGuardadoCuentas = true;
            } else {
                System.out.println("Inicializando catalogo de cuentas...");
                inicializarCuentas();
            }
        }
        if (!cargarAsientosTexto(ARCHIVO_ASIENTOS)) bloquearGuardadoAsientos = true;
        recalcularSaldos();
        advertirSaldosContrarios();
    }
    static void inicializarCuentas() {
        for (String linea : CATALOGO_ESTANDAR) {
            String[] partes = linea.split("\\|");
            agregarCuenta(partes[0], partes[1], partes[2]);
        }
        if (!guardarCuentas()) System.out.println("ADVERTENCIA: no se pudo guardar el catalogo inicial.");
    }
    static void agregarCuenta(String codigo, String nombre, String tipo) {
        cuentaCodigos.add(codigo); cuentaNombres.add(nombre); cuentaTipos.add(tipo); cuentaSaldos.add(0.0);
    }
    static boolean guardarCuentas() {
        if (bloquearGuardadoCuentas) {
            System.out.println("No se guardaron las cuentas: corrija " + ARCHIVO_CUENTAS + " y reinicie el programa.");
            return false;
        }
        StringBuilder contenido = new StringBuilder();
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            contenido.append(limpiarCampo(cuentaCodigos.get(i))).append('|')
                    .append(limpiarCampo(cuentaNombres.get(i))).append('|')
                    .append(limpiarCampo(cuentaTipos.get(i))).append('|')
                    .append(cuentaSaldos.get(i)).append(System.lineSeparator());
        }
        return escribirArchivoAtomico(ARCHIVO_CUENTAS, contenido.toString());
    }
    static boolean escribirArchivoAtomico(String ruta, String contenido) {
        File temporal = new File(ruta + ".tmp");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(temporal))) {
            writer.write(contenido);
        } catch (IOException e) {
            System.out.println("Error escribiendo " + ruta + ": " + e.getMessage());
            temporal.delete();
            return false;
        }
        try {
            try {
                Files.move(temporal.toPath(), new File(ruta).toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal.toPath(), new File(ruta).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            System.out.println("Error escribiendo " + ruta + ": " + e.getMessage());
            temporal.delete();
            return false;
        }
    }
    static String limpiarCampo(String valor) { return valor == null ? "" : valor.replace("\r", " ").replace("\n", " "); }
    static void advertir(String archivo, int numeroLinea, String mensaje) {
        System.out.println("ADVERTENCIA " + archivo + " linea " + numeroLinea + ": " + mensaje);
    }
    static boolean cargarCuentasTexto(String archivo) {
        cuentaCodigos.clear(); cuentaNombres.clear(); cuentaTipos.clear(); cuentaSaldos.clear();
        File file = new File(archivo);
        if (!file.exists()) return true;
        boolean huboDescarte = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String linea;
            int numeroLinea = 0;
            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1);
                if (linea.trim().isEmpty()) continue;
                String[] partes = linea.split("\\|", -1);
                if (partes.length != 4) { advertir(archivo, numeroLinea, "se esperaban 4 campos. Linea omitida."); huboDescarte = true; continue; }
                String codigo = partes[0].trim(), nombre = partes[1].trim(), tipo = partes[2].trim();
                String error = codigo.isEmpty() ? "codigo de cuenta vacio"
                        : nombre.isEmpty() ? "nombre de cuenta vacio"
                        : !tipoValido(tipo) ? "tipo de cuenta invalido ('" + tipo + "')" : null;
                if (error != null) { advertir(archivo, numeroLinea, error + ". Linea omitida."); huboDescarte = true; continue; }
                double saldo = 0;
                try { saldo = Double.parseDouble(partes[3].trim()); if (!Double.isFinite(saldo)) throw new NumberFormatException(); }
                catch (NumberFormatException e) { advertir(archivo, numeroLinea, "saldo invalido ('" + partes[3].trim() + "'). Se usara 0.0."); }
                if (existeCuenta(codigo) || existeCodigoNormalizado(codigo)) {
                    advertir(archivo, numeroLinea, "codigo duplicado " + codigo + ". Se conserva la primera aparicion."); huboDescarte = true; continue;
                }
                cuentaCodigos.add(codigo); cuentaNombres.add(nombre); cuentaTipos.add(tipo); cuentaSaldos.add(saldo);
            }
        } catch (IOException e) {
            System.out.println("Error leyendo " + archivo + ": " + e.getMessage());
            return false;
        }
        if (huboDescarte) {
            System.out.println("ADVERTENCIA: " + archivo + " contiene lineas invalidas. Se cargaron solo las cuentas validas.");
            if (!respaldarArchivoInvalido(archivo, "cuentas_invalidas")) {
                System.out.println("No se sobrescribira " + archivo + " hasta corregirlo.");
                return false;
            }
        }
        return true;
    }
    static boolean respaldarArchivoInvalido(String archivo, String prefijo) {
        File origen = new File(archivo);
        if (!origen.exists()) return true;
        File dir = new File("backup");
        if (!dir.exists() && !dir.mkdirs()) { System.out.println("ADVERTENCIA: no se pudo crear la carpeta backup."); return false; }
        File destino = new File(dir, prefijo + "_" + LocalDateTime.now().format(FORMATO_TIMESTAMP) + ".txt");
        try {
            Files.copy(origen.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Se guardo una copia del archivo original en: " + destino.getPath()); return true;
        } catch (IOException e) {
            System.out.println("ADVERTENCIA: no se pudo guardar la copia de seguridad de " + archivo + ": " + e.getMessage()); return false;
        }
    }
    static boolean guardarAsientos() {
        if (bloquearGuardadoAsientos) { System.out.println("No se guardaron los asientos: corrija " + ARCHIVO_ASIENTOS + " y reinicie el programa."); return false; }
        StringBuilder contenido = new StringBuilder();
        for (int i = 0; i < asientoFechas.size(); i++) {
            contenido.append("ASIENTO|").append(limpiarCampo(asientoFechas.get(i))).append('|')
                    .append(limpiarCampo(asientoDescripciones.get(i))).append('|')
                    .append(asientoTotales.get(i)).append(System.lineSeparator());
            List<String> cods = partidaCodigos.get(i), tips = partidaTipos.get(i);
            List<Double> mons = partidaMontos.get(i);
            for (int j = 0; j < cods.size(); j++)
                contenido.append("PARTIDA|").append(limpiarCampo(cods.get(j))).append('|')
                        .append(limpiarCampo(tips.get(j))).append('|').append(mons.get(j)).append(System.lineSeparator());
        }
        return escribirArchivoAtomico(ARCHIVO_ASIENTOS, contenido.toString());
    }
    static boolean cargarAsientosTexto(String archivo) {
        asientoFechas.clear(); asientoDescripciones.clear(); asientoTotales.clear();
        partidaCodigos.clear(); partidaTipos.clear(); partidaMontos.clear();
        File file = new File(archivo);
        if (!file.exists()) return true;
        int[] stats = {0, 0}; boolean[] descarte = {false}; boolean enAsiento = false; int lineaAsiento = 0;
        String fecha = null, descripcion = null, totalTexto = null;
        List<String> cods = null, tips = null, errores = new ArrayList<>(), avisos = new ArrayList<>();
        List<Double> mons = null;
        boolean bloqueInvalido = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String linea;
            int numeroLinea = 0;
            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1);
                if (linea.trim().isEmpty()) continue;
                String[] partes = linea.split("\\|", -1);
                String etiqueta = partes[0].trim();
                if (etiqueta.equals("ASIENTO")) {
                    if (enAsiento) cerrarAsientoCargado(archivo, lineaAsiento, fecha, descripcion, totalTexto,
                            cods, tips, mons, bloqueInvalido, errores, avisos, stats, descarte);
                    enAsiento = true; lineaAsiento = numeroLinea;
                    fecha = null; descripcion = null; totalTexto = null;
                    cods = new ArrayList<>(); tips = new ArrayList<>(); mons = new ArrayList<>();
                    bloqueInvalido = false; errores = new ArrayList<>(); avisos = new ArrayList<>();
                    if (partes.length != 4) { bloqueInvalido = true; errores.add("se esperaban 4 campos en ASIENTO y hay " + partes.length); }
                    else { fecha = partes[1].trim(); descripcion = partes[2].trim(); totalTexto = partes[3].trim(); }
                } else if (etiqueta.equals("PARTIDA")) {
                    if (!enAsiento) { advertir(archivo, numeroLinea, "PARTIDA sin ASIENTO previo. Linea omitida."); descarte[0] = true; }
                    else if (!bloqueInvalido) {
                        String error = agregarPartidaCargada(partes, numeroLinea, cods, tips, mons);
                        if (error != null) { bloqueInvalido = true; errores.add(error); }
                    }
                } else if (enAsiento) {
                    bloqueInvalido = true; errores.add("linea desconocida en linea " + numeroLinea + " ('" + linea.trim() + "')");
                } else { advertir(archivo, numeroLinea, "linea desconocida. Linea omitida."); descarte[0] = true; }
            }
            if (enAsiento) cerrarAsientoCargado(archivo, lineaAsiento, fecha, descripcion, totalTexto,
                    cods, tips, mons, bloqueInvalido, errores, avisos, stats, descarte);
        } catch (IOException e) { System.out.println("Error leyendo " + archivo + ": " + e.getMessage()); return false; }
        if (descarte[0]) {
            System.out.println("Carga de asientos: " + stats[0] + " validos, " + stats[1] + " omitidos.");
            System.out.println("ADVERTENCIA: " + archivo + " contiene asientos o lineas invalidas.");
            System.out.println("Se cargaron solo los asientos validos.");
            if (!respaldarArchivoInvalido(archivo, "asientos_invalidos")) {
                bloquearGuardadoAsientos = true;
                System.out.println("El archivo " + archivo + " no se sobrescribira y no se podran registrar asientos hasta corregirlo.");
            }
        }
        return true;
    }
    static void cerrarAsientoCargado(String archivo, int linea, String fecha, String descripcion,
            String totalTexto, List<String> cods, List<String> tips, List<Double> mons,
            boolean bloqueInvalido, List<String> errores, List<String> avisos,
            int[] stats, boolean[] descarte) {
        if (agregarAsientoCargado(archivo, linea, fecha, descripcion, totalTexto, cods, tips, mons,
                bloqueInvalido, errores, avisos)) stats[0]++;
        else { stats[1]++; descarte[0] = true; }
    }
    static String agregarPartidaCargada(String[] partes, int numeroLinea, List<String> cods,
            List<String> tips, List<Double> mons) {
        if (partes.length != 4) return "PARTIDA en linea " + numeroLinea + " tiene " + partes.length + " campos (se esperaban 4)";
        String codigo = partes[1].trim(), tipoTexto = normalizarTexto(partes[2]);
        String tipo = tipoTexto.equals("debito") ? "Debito" : tipoTexto.equals("credito") ? "Credito" : null;
        if (tipo == null) return "tipo de partida invalido ('" + partes[2].trim() + "') en linea " + numeroLinea;
        if (codigo.isEmpty()) return "codigo de cuenta vacio en linea " + numeroLinea;
        if (!existeCuenta(codigo)) return "la cuenta " + codigo + " no existe en el catalogo (linea " + numeroLinea + ")";
        try {
            double monto = parsearMonto(partes[3]);
            if (!Double.isFinite(monto) || monto <= 0) throw new NumberFormatException();
            cods.add(codigo); tips.add(tipo); mons.add(monto);
            return null;
        } catch (NumberFormatException e) { return "monto invalido ('" + partes[3].trim() + "') en linea " + numeroLinea; }
    }
    static boolean agregarAsientoCargado(String archivo, int linea, String fecha, String descripcion,
            String totalTexto, List<String> cods, List<String> tips, List<Double> mons,
            boolean bloqueInvalido, List<String> errores, List<String> avisos) {
        if (bloqueInvalido) { for (String error : errores) advertir(archivo, linea, error + ". Asiento omitido."); return false; }
        String fechaNormalizada = null;
        if (fecha == null || fecha.isEmpty()) errores.add("fecha vacia");
        else {
            LocalDate f = parsearFecha(fecha);
            if (f == null) errores.add("fecha invalida ('" + fecha + "')");
            else if (f.getYear() < 1900 || f.getYear() > 2100) errores.add("ano fuera de rango ('" + fecha + "')");
            else fechaNormalizada = f.format(FORMATO_FECHA);
        }
        if (descripcion == null || descripcion.isEmpty()) errores.add("descripcion vacia");
        int cantidad = cods == null ? 0 : cods.size();
        if (cantidad < 2) errores.add("tiene " + cantidad + (cantidad == 1 ? " partida" : " partidas") + " (minimo 2)");
        else {
            boolean hayDebe = false, hayHaber = false;
            for (int i = 0; i < cantidad; i++) { if (tips.get(i).equalsIgnoreCase("Debito")) hayDebe = true; else hayHaber = true; }
            if (!hayDebe || !hayHaber) errores.add("debe tener al menos una partida al Debe y una al Haber");
            else {
                double totalDebe = totalLado(tips, mons, "Debito"), totalHaber = totalLado(tips, mons, "Credito");
                if (!Double.isFinite(totalDebe) || !Double.isFinite(totalHaber)) errores.add("la suma de las partidas no es un numero finito");
                else if (!cuadra(totalDebe, totalHaber)) errores.add(String.format("no cuadra: Debe %.2f, Haber %.2f", totalDebe, totalHaber));
                double totalEncabezado = 0; boolean encabezadoValido = false;
                if (totalTexto != null) {
                    try { totalEncabezado = Double.parseDouble(totalTexto); encabezadoValido = Double.isFinite(totalEncabezado) && totalEncabezado > 0; }
                    catch (NumberFormatException e) { /* Se avisa abajo. */ }
                }
                if (!encabezadoValido) avisos.add("total del encabezado invalido ('" + totalTexto + "'); se usara el calculado");
                else if (!cuadra(totalEncabezado, totalDebe))
                    avisos.add(String.format("total del encabezado %.2f no coincide con las partidas %.2f; se usara el calculado",
                            totalEncabezado, totalDebe));
            }
        }
        for (String error : errores) advertir(archivo, linea, error + ". Asiento omitido.");
        for (String aviso : avisos) advertir(archivo, linea, aviso);
        if (!errores.isEmpty()) return false;
        agregarAsiento(fechaNormalizada, descripcion, cods, tips, mons, totalLado(tips, mons, "Debito"));
        return true;
    }
    static void mostrarCuentas() {
        System.out.println("\n=== CATALOGO DE CUENTAS ===");
        System.out.printf("%-8s %-35s %-15s %12s%n", "Codigo", "Nombre", "Tipo", "Saldo");
        System.out.println("-".repeat(75));
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            System.out.printf("%-8s %-35s %-15s %12.2f%n",
                    cuentaCodigos.get(i), cuentaNombres.get(i), cuentaTipos.get(i), cuentaSaldos.get(i));
        }
    }
    static void crearCuenta() {
        if (bloquearGuardadoCuentas) {
            System.out.println("No se pueden crear cuentas hasta corregir " + ARCHIVO_CUENTAS + " y reiniciar el programa.");
            return;
        }
        linea("\n--- NUEVA CUENTA ---", "Se pediran tres datos: nombre, tipo y codigo.");
        String nombre = leerTextoPersistible("Nombre de la cuenta (ej: Caja chica): ", "nombre");
        String tipo = pedirTipoCuenta();
        String sugerido = sugerirCodigoCuenta(tipo);
        String codigo;
        while (true) {
            if (sugerido.isEmpty()) {
                codigo = leerString("Codigo de la cuenta (ej: 1010): ");
            } else {
                codigo = leerTextoOpcional("Codigo de la cuenta (ej: " + sugerido + "; Enter para usarlo): ");
                if (codigo.isEmpty()) codigo = sugerido;
            }
            String error = codigo.equals("0") ? "El codigo 0 esta reservado para cancelar. Ingrese otro."
                    : codigo.equals("?") || codigo.contains("|") ? "El codigo no puede ser ? ni contener el caracter |."
                    : existeCuenta(codigo) || existeCodigoNormalizado(codigo) ? "El codigo ya existe. Ingrese otro." : null;
            if (error != null) { System.out.println(error); continue; }
            break;
        }
        agregarCuenta(codigo, nombre, tipo);
        if (!guardarCuentas()) {
            int ultimo = cuentaCodigos.size() - 1;
            cuentaCodigos.remove(ultimo); cuentaNombres.remove(ultimo);
            cuentaTipos.remove(ultimo); cuentaSaldos.remove(ultimo);
            System.out.println("No se pudo guardar la cuenta. No se agrego al catalogo.");
            return;
        }
        System.out.println("Cuenta creada exitosamente: " + codigo + " - " + nombre + " (" + tipo + ").");
    }
    static String pedirTipoCuenta() {
        System.out.println("Tipo de cuenta:");
        for (int i = 0; i < TIPOS_CUENTA.length; i++) System.out.println("  " + (i + 1) + ". " + TIPOS_CUENTA[i]);
        return TIPOS_CUENTA[leerInt("Seleccione el tipo (1-" + TIPOS_CUENTA.length + "): ", 1, TIPOS_CUENTA.length) - 1];
    }
    static String sugerirCodigoCuenta(String tipo) {
        int indiceTipo = TIPOS.indexOf(tipo);
        if (indiceTipo < 0) return "";
        int base = (indiceTipo + 1) * 1000, mayor = base - 10;
        for (String codigo : cuentaCodigos) {
            try {
                int numero = Integer.parseInt(codigo);
                if (numero >= base && numero <= base + 999 && numero > mayor) mayor = numero;
            } catch (NumberFormatException e) { /* Los codigos no numericos no afectan la sugerencia. */ }
        }
        int siguiente = mayor + 10;
        return siguiente > base + 999 ? "" : String.format("%04d", siguiente);
    }
    static String leerTextoOpcional(String mensaje) {
        System.out.print(mensaje);
        return scanner.nextLine().trim();
    }
    static String leerTextoPersistible(String mensaje, String campo) {
        while (true) {
            String texto = leerString(mensaje);
            if (!texto.contains("|")) return texto;
            System.out.println("El campo " + campo + " no puede contener el caracter |.");
        }
    }
    static boolean tipoValido(String tipo) { return TIPOS.contains(tipo); }
    static void registrarAsiento() {
        if (bloquearGuardadoAsientos || bloquearGuardadoCuentas) {
            System.out.println("No se pueden registrar asientos hasta corregir los archivos de datos y reiniciar el programa."); return;
        }
        linea("\n--- REGISTRAR ASIENTO ---", "Se pediran fecha, descripcion y al menos dos partidas.");
        String fecha = leerFecha("Fecha del asiento");
        String descripcion = leerTextoPersistible("Descripcion del asiento (ej: Compra de papeleria): ", "descripcion");
        List<String> cods = new ArrayList<>(), tips = new ArrayList<>();
        List<Double> mons = new ArrayList<>(), saldosSimulados = new ArrayList<>(cuentaSaldos);
        double[] totales = {0, 0};
        while (true) {
            if (!pedirPartida(cods, tips, mons, saldosSimulados, totales)) { System.out.println("Asiento cancelado."); return; }
            if (cods.size() < 2) { System.out.println("Un asiento necesita al menos 2 partidas. Ingrese la segunda."); continue; }
            boolean cuadrado = cuadra(totales[0], totales[1]);
            if (!cuadrado) {
                System.out.println("\nEl asiento no cuadra:");
                System.out.printf("  Total del Debe:  %.2f%n  Total del Haber: %.2f%n  Diferencia:      %.2f%n",
                        totales[0], totales[1], Math.abs(totales[0] - totales[1]));
                System.out.println("Un asiento debe tener el total del Debe igual al total del Haber.");
            }
            String pregunta = cuadrado ? "Agregar otra partida? (S/N): "
                    : "Desea agregar otra partida para cuadrar? (S/N): ";
            if (confirmar(pregunta)) continue;
            if (!cuadrado) { System.out.println("Asiento cancelado. No se guardo nada."); return; }
            break;
        }
        if (totales[0] <= 0 || totales[1] <= 0) {
            linea("El asiento debe tener al menos una partida en el Debe y una en el Haber.", "Asiento cancelado. No se guardo nada."); return;
        }
        linea("\n--- RESUMEN DEL ASIENTO ---", "Fecha: " + fecha, "Descripcion: " + descripcion);
        mostrarPartidas(cods, tips, mons);
        System.out.println("-".repeat(70));
        System.out.printf("%-54s %12.2f%n", "TOTAL DEBE:", totales[0]);
        System.out.printf("%-54s %12.2f%n", "TOTAL HABER:", totales[1]);
        if (!confirmar("Guardar el asiento? (S/N): ")) { System.out.println("Asiento cancelado. No se guardo nada."); return; }
        agregarAsiento(fecha, descripcion, cods, tips, mons, totales[0]);
        actualizarSaldos(cods, tips, mons);
        if (!guardarAsientos()) {
            quitarUltimoAsiento(); recalcularSaldos(); System.out.println("No se pudo guardar el asiento. No se registro."); return;
        }
        if (!guardarCuentas()) {
            linea("Asiento guardado, pero no se pudo actualizar " + ARCHIVO_CUENTAS + ".", "Los saldos se recalcularan al iniciar el programa."); return;
        }
        System.out.println("Asiento registrado exitosamente.");
    }
    static boolean pedirPartida(List<String> cods, List<String> tips, List<Double> mons,
            List<Double> saldosSimulados, double[] totales) {
        while (true) {
            String codigo = pedirCuenta("Cuenta de la partida " + (cods.size() + 1), "cancelar el asiento");
            if (codigo == null) return false;
            String tipo = pedirTipoPartida();
            double monto = leerDouble("  Monto (ej: 1500,00): ");
            int idxCuenta = indiceCuenta(codigo);
            String tipoCuenta = cuentaTipos.get(idxCuenta);
            double saldoActual = saldosSimulados.get(idxCuenta), nuevoSaldo = saldoActual + efectoSaldo(tipoCuenta, tipo, monto);
            if (!esCuentaContraria(codigo) && nuevoSaldo < -TOLERANCIA_SALDO) {
                System.out.println("\nADVERTENCIA: la cuenta quedaria con saldo en el lado contrario al normal.");
                System.out.printf("  Cuenta: %s - %s (%s)%n  Saldo actual: %.2f%n  Partida: %s %.2f%n  Saldo resultante: %.2f%n",
                        codigo, cuentaNombres.get(idxCuenta), tipoCuenta, saldoActual,
                        etiquetaLado(tipo), monto, nuevoSaldo);
                System.out.println("  Una cuenta de tipo " + tipoCuenta + " normalmente tiene saldo "
                        + (tipoDeudor(tipoCuenta) ? "deudor" : "acreedor") + ".");
                if (!confirmar("  Desea continuar? (S/N): ")) {
                    System.out.println("Partida descartada. Ingrese nuevamente la partida " + (cods.size() + 1) + "."); continue;
                }
            }
            saldosSimulados.set(idxCuenta, nuevoSaldo);
            cods.add(codigo); tips.add(tipo); mons.add(monto);
            if (tipo.equalsIgnoreCase("Debito")) totales[0] += monto; else totales[1] += monto;
            System.out.printf("  Acumulado: Debe %.2f | Haber %.2f%n", totales[0], totales[1]);
            return true;
        }
    }
    static String pedirTipoPartida() {
        linea("  Lado de la partida:", "    1. Debe (debito)", "    2. Haber (credito)");
        return leerInt("  Seleccione 1 o 2: ", 1, 2) == 1 ? "Debito" : "Credito";
    }
    static boolean confirmar(String mensaje) {
        while (true) {
            String respuesta = leerString(mensaje);
            if (respuesta.equalsIgnoreCase("S") || respuesta.equalsIgnoreCase("SI")) return true;
            if (respuesta.equalsIgnoreCase("N") || respuesta.equalsIgnoreCase("NO")) return false;
            System.out.println("Responda S (si) o N (no).");
        }
    }
    static void actualizarSaldos(List<String> cods, List<String> tips, List<Double> mons) { actualizarSaldos(cods, tips, mons, new ArrayList<>()); }
    static void actualizarSaldos(List<String> cods, List<String> tips, List<Double> mons, List<String> avisados) {
        int cantidad = Math.min(cods.size(), Math.min(tips.size(), mons.size()));
        for (int i = 0; i < cantidad; i++) {
            String codigo = cods.get(i);
            int j = indiceCuenta(codigo);
            if (j < 0) {
                if (!avisados.contains(codigo)) {
                    avisados.add(codigo);
                    System.out.println("ADVERTENCIA: La cuenta " + codigo + " no existe. Partida omitida.");
                }
                continue;
            }
            cuentaSaldos.set(j, cuentaSaldos.get(j) + efectoSaldo(cuentaTipos.get(j), tips.get(i), mons.get(i)));
        }
    }
    static double efectoSaldo(String tipoCuenta, String tipoPartida, double monto) {
        boolean debito = tipoPartida.equalsIgnoreCase("Debito");
        return tipoDeudor(tipoCuenta) ? (debito ? monto : -monto) : (debito ? -monto : monto);
    }
    static void recalcularSaldos() {
        for (int i = 0; i < cuentaSaldos.size(); i++) cuentaSaldos.set(i, 0.0);
        List<String> avisados = new ArrayList<>();
        for (int i = 0; i < asientoFechas.size(); i++)
            actualizarSaldos(partidaCodigos.get(i), partidaTipos.get(i), partidaMontos.get(i), avisados);
    }
    static void advertirSaldosContrarios() {
        boolean hay = false;
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            double saldo = cuentaSaldos.get(i);
            if (!esCuentaContraria(cuentaCodigos.get(i)) && saldo < -TOLERANCIA_SALDO) {
                if (!hay) { System.out.println("\nAVISO: cuentas con saldo en el lado contrario a su tipo:"); hay = true; }
                System.out.printf("  %s - %s (%s): %.2f%n",
                        cuentaCodigos.get(i), cuentaNombres.get(i), cuentaTipos.get(i), saldo);
            }
        }
        if (hay) System.out.println("Verifique si son cuentas de naturaleza contraria o errores de registro.");
    }
    static String fechaActual() { return LocalDate.now().format(FORMATO_FECHA); }
    static String leerFecha(String mensaje) {
        while (true) {
            System.out.print(mensaje + " (dd/MM/yyyy, Enter = hoy): ");
            String entrada = scanner.nextLine().trim();
            if (entrada.isEmpty()) return fechaActual();
            LocalDate fecha = parsearFecha(entrada);
            if (fecha != null && fecha.getYear() >= 1900 && fecha.getYear() <= 2100) return fecha.format(FORMATO_FECHA);
            System.out.println("Fecha invalida. Use dd/MM/yyyy y una fecha real entre 1900 y 2100.");
        }
    }
    static LocalDate parsearFecha(String entrada) {
        if (entrada == null) return null;
        try { return LocalDate.parse(entrada.trim(), FORMATO_FECHA_ESTRICTA); } catch (DateTimeParseException e) { return null; }
    }
    static String fechaOrdenable(String fecha) { String[] p = fecha.split("/"); return p.length == 3 ? p[2] + p[1] + p[0] : "99999999"; }
    static void ordenarMovimientos(List<Integer> asientos, List<Integer> partidas) {
        for (int i = 0; i < asientos.size() - 1; i++) {
            for (int j = 0; j < asientos.size() - 1 - i; j++) {
                String fechaA = fechaOrdenable(asientoFechas.get(asientos.get(j)));
                String fechaB = fechaOrdenable(asientoFechas.get(asientos.get(j + 1)));
                if (fechaB.compareTo(fechaA) < 0) {
                    int tempA = asientos.get(j);
                    asientos.set(j, asientos.get(j + 1));
                    asientos.set(j + 1, tempA);
                    if (partidas != null) {
                        int tempP = partidas.get(j); partidas.set(j, partidas.get(j + 1)); partidas.set(j + 1, tempP);
                    }
                }
            }
        }
    }
    static void cargarMovimientosCuenta(String codigo, List<Integer> asientos, List<Integer> partidas) {
        asientos.clear();
        partidas.clear();
        for (int i = 0; i < asientoFechas.size(); i++) {
            List<String> cods = partidaCodigos.get(i);
            for (int j = 0; j < cods.size(); j++) {
                if (cods.get(j).equals(codigo)) {
                    asientos.add(i);
                    partidas.add(j);
                }
            }
        }
        ordenarMovimientos(asientos, partidas);
    }
    static void mostrarAsientos() {
        System.out.println("\n=== ASIENTOS REGISTRADOS ===");
        if (asientoFechas.isEmpty()) {
            System.out.println("No hay asientos registrados.");
            return;
        }
        for (int i = 0; i < asientoFechas.size(); i++) {
            System.out.println("\nAsiento #" + (i + 1));
            mostrarAsiento(i);
        }
    }
    static void mostrarAsiento(int i) {
        System.out.println("Fecha: " + asientoFechas.get(i));
        System.out.println("Descripcion: " + asientoDescripciones.get(i));
        mostrarPartidas(partidaCodigos.get(i), partidaTipos.get(i), partidaMontos.get(i));
        System.out.printf("  Total: %.2f%n", asientoTotales.get(i));
    }
    static void mostrarPartidas(List<String> cods, List<String> tips, List<Double> mons) {
        System.out.printf("  %-6s %-30s %-9s %12s%n", "Codigo", "Nombre", "Lado", "Monto");
        for (int j = 0; j < cods.size(); j++) {
            System.out.printf("  %-6s %-30s %-9s %12.2f%n",
                    cods.get(j), nombreCuenta(cods.get(j)), etiquetaLado(tips.get(j)), mons.get(j));
        }
    }
    static void verBalanceComprobacion() {
        linea("\n=== BALANZA DE COMPROBACION ===", "Historico completo",
                "(Verificacion: total de debitos = total de creditos)", "");
        System.out.printf("%-8s %-35s %12s %12s %12s %12s%n",
                "Codigo", "Nombre", "Debitos", "Creditos", "Deudor", "Acreedor");
        System.out.println("-".repeat(96));
        double[] debitos = new double[cuentaCodigos.size()];
        double[] creditos = new double[cuentaCodigos.size()];
        for (int a = 0; a < asientoFechas.size(); a++) {
            List<String> cods = partidaCodigos.get(a);
            List<String> tips = partidaTipos.get(a);
            List<Double> mons = partidaMontos.get(a);
            for (int p = 0; p < cods.size(); p++) {
                int i = indiceCuenta(cods.get(p));
                if (i < 0) continue;
                if (tips.get(p).equalsIgnoreCase("Debito")) debitos[i] += mons.get(p);
                else creditos[i] += mons.get(p);
            }
        }
        double totalDebitos = 0, totalCreditos = 0, totalDeudor = 0, totalAcreedor = 0;
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            if (debitos[i] <= 0 && creditos[i] <= 0) continue;
            double saldo = debitos[i] - creditos[i];
            double deudor = saldo > TOLERANCIA_SALDO ? saldo : 0;
            double acreedor = saldo < -TOLERANCIA_SALDO ? -saldo : 0;
            System.out.printf("%-8s %-35s %12.2f %12.2f %12.2f %12.2f%n",
                    cuentaCodigos.get(i), cuentaNombres.get(i), debitos[i], creditos[i], deudor, acreedor);
            totalDebitos += debitos[i];
            totalCreditos += creditos[i];
            totalDeudor += deudor;
            totalAcreedor += acreedor;
        }
        System.out.println("-".repeat(96));
        System.out.printf("%-44s %12.2f %12.2f %12.2f %12.2f%n",
                "TOTALES:", totalDebitos, totalCreditos, totalDeudor, totalAcreedor);
        System.out.println();
        if (cuadra(totalDebitos, totalCreditos)) {
            System.out.println("LA BALANZA DE COMPROBACION ESTA CUADRADA.");
        } else {
            System.out.println("ADVERTENCIA: La balanza de comprobacion NO cuadra.");
            System.out.printf("Diferencia: %.2f%n", totalDebitos - totalCreditos);
        }
    }
    static void verLibroDiario() {
        linea("\n=== LIBRO DIARIO ===", "Historico completo",
                "Formato clasico: Cta, Detalle, Parcial, Debe, Haber");
        if (asientoFechas.isEmpty()) {
            System.out.println("No hay asientos registrados.");
            return;
        }
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < asientoFechas.size(); i++) indices.add(i);
        ordenarMovimientos(indices, null);
        System.out.println();
        System.out.printf("%-10s %-42s %12s %12s %12s%n",
                "Cta", "Detalle", "Parcial", "Debe", "Haber");
        System.out.println("=".repeat(92));
        double totalGeneralDebe = 0, totalGeneralHaber = 0;
        for (int k = 0; k < indices.size(); k++) {
            int i = indices.get(k);
            List<String> cods = partidaCodigos.get(i);
            List<String> tips = partidaTipos.get(i);
            List<Double> mons = partidaMontos.get(i);
            totalGeneralDebe += totalLado(tips, mons, "Debito");
            totalGeneralHaber += totalLado(tips, mons, "Credito");
            System.out.println("Asiento #" + (i + 1));
            for (int j = 0; j < cods.size(); j++) {
                if (tips.get(j).equalsIgnoreCase("Debito")) {
                    mostrarLineaLibroDiario(cods.get(j), mons.get(j), false);
                }
            }
            for (int j = 0; j < cods.size(); j++) {
                if (tips.get(j).equalsIgnoreCase("Credito")) {
                    mostrarLineaLibroDiario(cods.get(j), mons.get(j), true);
                }
            }
            System.out.printf("%-10s %s%n", asientoFechas.get(i), asientoDescripciones.get(i));
            System.out.println("-".repeat(92));
        }
        System.out.println("=".repeat(92));
        System.out.printf("%-66s %12.2f %12.2f%n",
                "TOTAL GENERAL", totalGeneralDebe, totalGeneralHaber);
        System.out.println("Total de asientos: " + indices.size());
    }
    static void mostrarLineaLibroDiario(String codigo, double monto, boolean credito) {
        String detalle = recortar((credito ? "A " : "") + nombreCuenta(codigo), 42);
        String debe = credito ? "" : String.format("%12.2f", monto);
        String haber = credito ? String.format("%12.2f", monto) : "";
        System.out.printf("%-10s %-42s %12s %12s %12s%n", codigo, detalle, "", debe, haber);
    }
    static void verLibroMayor() {
        linea("\n=== LIBRO MAYOR ===", "Historico completo",
                "(Movimientos por cuenta individual)", "(As. = numero de asiento)", "");
        String codigo = pedirCuenta("Cuenta para el libro mayor", "cancelar");
        if (codigo == null) {
            System.out.println("Operacion cancelada.");
            return;
        }
        int idxCuenta = indiceCuenta(codigo);
        System.out.println();
        System.out.println("CUENTA: " + cuentaCodigos.get(idxCuenta) + " - " + cuentaNombres.get(idxCuenta));
        System.out.println("Tipo: " + cuentaTipos.get(idxCuenta));
        System.out.println();
        List<Integer> asientosMov = new ArrayList<>(), partidasMov = new ArrayList<>();
        cargarMovimientosCuenta(codigo, asientosMov, partidasMov);
        if (asientosMov.isEmpty()) {
            System.out.println("La cuenta no tiene movimientos.");
            return;
        }
        System.out.printf("%-5s %-20s %-10s %12s %12s%n", "As.", "Fecha", "Lado", "Monto", "Saldo");
        System.out.println("-".repeat(65));
        double saldo = 0;
        String tipoCuenta = cuentaTipos.get(idxCuenta);
        for (int k = 0; k < asientosMov.size(); k++) {
            int i = asientosMov.get(k);
            int j = partidasMov.get(k);
            String tipo = partidaTipos.get(i).get(j);
            double monto = partidaMontos.get(i).get(j);
            saldo += efectoSaldo(tipoCuenta, tipo, monto);
            System.out.printf("%-5d %-20s %-10s %12.2f %12.2f%n",
                    i + 1, asientoFechas.get(i), etiquetaLado(tipo), monto, saldo);
        }
        System.out.println("-".repeat(65));
        System.out.printf("Saldo final: %.2f%n", saldo);
    }
    static void verCuentaTIndividual() {
        linea("\n=== CUENTA T ===", "Seleccione la cuenta que desea consultar.");
        String codigo = pedirCuenta("Cuenta que desea consultar", "cancelar");
        if (codigo == null) {
            System.out.println("Operacion cancelada.");
            return;
        }
        mostrarCuentaT(indiceCuenta(codigo));
    }
    static void verCuentasTConMovimientos() {
        System.out.println("\n=== CUENTAS T CON MOVIMIENTOS ===");
        int mostradas = 0;
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            if (cuentaTieneMovimientos(i)) {
                mostrarCuentaT(i);
                mostradas++;
            }
        }
        if (mostradas == 0) System.out.println("No hay cuentas con movimientos registrados.");
        else System.out.println("\nTotal de cuentas con movimientos: " + mostradas);
    }
    static void verTodasCuentasT() {
        System.out.println("\n=== CUENTAS T - CATALOGO COMPLETO ===");
        for (int i = 0; i < cuentaCodigos.size(); i++) mostrarCuentaT(i);
        System.out.println("\nTotal de cuentas: " + cuentaCodigos.size());
    }
    static boolean cuentaTieneMovimientos(int idxCuenta) {
        List<Integer> asientos = new ArrayList<>(), partidas = new ArrayList<>();
        cargarMovimientosCuenta(cuentaCodigos.get(idxCuenta), asientos, partidas);
        return !asientos.isEmpty();
    }
    static void mostrarCuentaT(int idxCuenta) {
        if (idxCuenta < 0 || idxCuenta >= cuentaCodigos.size()) return;
        String codigo = cuentaCodigos.get(idxCuenta);
        List<Integer> asientosMov = new ArrayList<>(), partidasMov = new ArrayList<>();
        cargarMovimientosCuenta(codigo, asientosMov, partidasMov);
        List<String> filasDebe = new ArrayList<>(), filasHaber = new ArrayList<>();
        double totalDebe = 0, totalHaber = 0;
        for (int k = 0; k < asientosMov.size(); k++) {
            int i = asientosMov.get(k), j = partidasMov.get(k);
            double monto = partidaMontos.get(i).get(j);
            String fila = String.format("%-10s %-22s %12.2f",
                    asientoFechas.get(i), recortar(asientoDescripciones.get(i), 22), monto);
            if (partidaTipos.get(i).get(j).equalsIgnoreCase("Debito")) {
                filasDebe.add(fila);
                totalDebe += monto;
            } else {
                filasHaber.add(fila);
                totalHaber += monto;
            }
        }
        System.out.println("\n" + "=".repeat(95));
        System.out.printf(" CUENTA T: %s - %s (%s)%n",
                codigo, cuentaNombres.get(idxCuenta), cuentaTipos.get(idxCuenta));
        System.out.println("=".repeat(95));
        System.out.printf("%-10s %-22s %12s | %-10s %-22s %12s%n",
                "Fecha", "Concepto", "Monto", "Fecha", "Concepto", "Monto");
        System.out.println("-".repeat(95));
        int filas = Math.max(filasDebe.size(), filasHaber.size());
        if (filas == 0) {
            System.out.println("Sin movimientos.");
        } else {
            for (int f = 0; f < filas; f++) {
                String izquierda = f < filasDebe.size() ? filasDebe.get(f) : "";
                String derecha = f < filasHaber.size() ? filasHaber.get(f) : "";
                System.out.printf("%-46s | %-46s%n", izquierda, derecha);
            }
        }
        System.out.println("-".repeat(95));
        System.out.printf("%-10s %-22s %12.2f | %-10s %-22s %12.2f%n",
                "TOTAL DEBE", "", totalDebe, "TOTAL HABER", "", totalHaber);
        double saldo = totalDebe - totalHaber;
        String etiqueta = saldo > TOLERANCIA_SALDO ? "Deudor"
                : saldo < -TOLERANCIA_SALDO ? "Acreedor" : "Cero";
        if (etiqueta.equals("Cero")) saldo = 0;
        System.out.printf("%63s SALDO: %.2f (%s)%n", "", Math.abs(saldo), etiqueta);
        String tipoCuenta = cuentaTipos.get(idxCuenta);
        boolean ladoContrario = !esCuentaContraria(codigo)
                && (tipoDeudor(tipoCuenta) ? saldo < -TOLERANCIA_SALDO : saldo > TOLERANCIA_SALDO);
        if (ladoContrario) {
            System.out.println("  AVISO: saldo en el lado contrario al normal para una cuenta de tipo " + tipoCuenta + ".");
        }
        System.out.println("=".repeat(95));
    }
    static String recortar(String texto, int ancho) {
        if (texto == null) return "";
        return texto.length() <= ancho ? texto : texto.substring(0, ancho);
    }
    static void verEstadoResultados() {
        linea("\n=== ESTADO DE RESULTADOS ===", "Historico completo");
        System.out.printf("%-8s %-35s %12s%n", "Codigo", "Nombre", "Saldo");
        System.out.println("-".repeat(60));
        saldoContrarioEnReporte = false;
        double totalIngresos = mostrarSeccion("Ingreso", "INGRESOS", cuentaSaldos);
        double totalCostos = mostrarSeccion("Costo", "COSTOS", cuentaSaldos);
        double totalGastos = mostrarSeccion("Gasto", "GASTOS", cuentaSaldos);
        double utilidadBruta = totalIngresos - totalCostos;
        double utilidadNeta = utilidadBruta - totalGastos;
        System.out.println("\n" + "-".repeat(60));
        System.out.printf("Utilidad bruta:  %.2f%n", utilidadBruta);
        System.out.printf("Utilidad neta:   %.2f%n", utilidadNeta);
        mostrarNotaSaldosContrarios();
    }
    static void generarBalance() {
        linea("\n=== BALANCE GENERAL ===", "Historico completo", "Generado: " + fechaActual());
        System.out.printf("%-8s %-35s %12s%n", "Codigo", "Nombre", "Saldo");
        System.out.println("-".repeat(60));
        saldoContrarioEnReporte = false;
        double totalActivo = mostrarSeccion("Activo", "ACTIVO", cuentaSaldos);
        double totalPasivo = mostrarSeccion("Pasivo", "PASIVO", cuentaSaldos);
        double totalPatrimonio = mostrarSeccion("Patrimonio", "PATRIMONIO", cuentaSaldos);
        double resultado = resultadoEjercicio(cuentaSaldos);
        double patrimonioConResultado = totalPatrimonio + resultado;
        System.out.println("\n-- RESULTADO DEL EJERCICIO --");
        System.out.printf("%-44s %12.2f%n", "Resultado del ejercicio:", resultado);
        System.out.println("\n" + "-".repeat(60));
        System.out.printf("Total Pasivo + Patrimonio + Resultado: %.2f%n", totalPasivo + patrimonioConResultado);
        System.out.printf("Diferencia: %.2f%n", totalActivo - (totalPasivo + patrimonioConResultado));
        if (cuadra(totalActivo, totalPasivo + patrimonioConResultado)) {
            System.out.println("EL BALANCE CUADRA CORRECTAMENTE.");
        } else {
            System.out.println("ADVERTENCIA: El balance NO cuadra.");
        }
        mostrarNotaSaldosContrarios();
        if (!guardarBalance(fechaActual(), totalActivo, totalPasivo, patrimonioConResultado)) {
            System.out.println("ADVERTENCIA: no se pudo guardar " + ARCHIVO_BALANCE + ".");
        }
    }
    static double mostrarSeccion(String tipo, String titulo, List<Double> saldos) {
        System.out.println("\n-- " + titulo + " --");
        double total = 0;
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            if (!cuentaTipos.get(i).equals(tipo)) continue;
            double saldo = saldos.get(i);
            String marca = "";
            if (!esCuentaContraria(cuentaCodigos.get(i)) && saldo < -TOLERANCIA_SALDO) {
                marca = " *";
                saldoContrarioEnReporte = true;
            }
            System.out.printf("%-8s %-35s %12.2f%s%n",
                    cuentaCodigos.get(i), cuentaNombres.get(i), saldo, marca);
            total += saldo;
        }
        System.out.printf("%-44s %12.2f%n", "TOTAL " + titulo + ":", total);
        return total;
    }
    static void mostrarNotaSaldosContrarios() {
        if (!saldoContrarioEnReporte) return;
        linea("\n* Saldo en el lado contrario al normal. Verifique si es una cuenta",
                "  de naturaleza contraria o un error de registro.");
    }
    static double resultadoEjercicio(List<Double> saldos) {
        double total = 0;
        for (int i = 0; i < cuentaTipos.size() && i < saldos.size(); i++) {
            String tipo = cuentaTipos.get(i);
            if (tipo.equals("Ingreso")) total += saldos.get(i);
            else if (tipo.equals("Costo") || tipo.equals("Gasto")) total -= saldos.get(i);
        }
        return total;
    }
    static boolean guardarBalance(String fecha, double activo, double pasivo, double patrimonio) {
        String contenido = fecha + "|" + activo + "|" + pasivo + "|" + patrimonio + System.lineSeparator();
        return escribirArchivoAtomico(ARCHIVO_BALANCE, contenido);
    }
    static void buscarCuenta() {
        System.out.println("\n--- BUSCAR CUENTA ---");
        String termino = leerString("Escriba el codigo o parte del nombre (ej: 1000, caja, depreciacion): ");
        System.out.println("\n=== RESULTADOS ===");
        List<String> encontradas = buscarCuentas(termino);
        if (encontradas.isEmpty()) {
            System.out.println("No se encontraron resultados.");
            return;
        }
        System.out.printf("%-8s %-35s %-15s %12s%n", "Codigo", "Nombre", "Tipo", "Saldo");
        System.out.println("-".repeat(75));
        for (String codigo : encontradas) {
            int i = indiceCuenta(codigo);
            System.out.printf("%-8s %-35s %-15s %12.2f%n",
                    cuentaCodigos.get(i), cuentaNombres.get(i), cuentaTipos.get(i), cuentaSaldos.get(i));
        }
    }
    static boolean existeCuenta(String codigo) {
        return indiceCuenta(codigo) >= 0;
    }
    static boolean existeCodigoNormalizado(String codigo) {
        String buscado = normalizarTexto(codigo);
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            if (normalizarTexto(cuentaCodigos.get(i)).equals(buscado)) return true;
        }
        return false;
    }
    static int indiceCuenta(String codigo) {
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            if (cuentaCodigos.get(i).equals(codigo)) return i;
        }
        return -1;
    }
    static String nombreCuenta(String codigo) {
        int indice = indiceCuenta(codigo);
        return indice < 0 ? "(cuenta no existe)" : cuentaNombres.get(indice);
    }
    static String etiquetaLado(String tipo) {
        return tipo.equalsIgnoreCase("Debito") ? "Debe" : "Haber";
    }
    static List<String> buscarCuentas(String termino) {
        List<String> encontradas = new ArrayList<>();
        String busqueda = normalizarTexto(termino);
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            if (normalizarTexto(cuentaCodigos.get(i)).contains(busqueda)
                    || normalizarTexto(cuentaNombres.get(i)).contains(busqueda)) {
                encontradas.add(cuentaCodigos.get(i));
            }
        }
        return encontradas;
    }
    static String normalizarTexto(String texto) {
        if (texto == null) return "";
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
    static void mostrarCuentaEncontrada(int indice) {
        System.out.printf("    Cuenta seleccionada: %s - %s (%s)%n",
                cuentaCodigos.get(indice), cuentaNombres.get(indice), cuentaTipos.get(indice));
    }
    static String pedirCuenta(String contexto, String accionCancelar) {
        while (true) {
            linea("  " + contexto,
                    "    Escriba el codigo (ej: 1000) o parte del nombre (ej: caja).",
                    "    ? = ver el catalogo | 0 = " + accionCancelar);
            String termino = leerString("    Cuenta: ");
            if (termino.equals("0")) return null;
            if (termino.equals("?")) {
                mostrarCuentas();
                continue;
            }
            int indiceExacto = indiceCuenta(termino);
            if (indiceExacto >= 0) {
                mostrarCuentaEncontrada(indiceExacto);
                return cuentaCodigos.get(indiceExacto);
            }
            List<String> coincidencias = buscarCuentas(termino);
            if (coincidencias.isEmpty()) {
                System.out.println("  No se encontro ninguna cuenta con ese codigo o nombre.");
                continue;
            }
            if (coincidencias.size() == 1) {
                int indice = indiceCuenta(coincidencias.get(0));
                mostrarCuentaEncontrada(indice);
                return coincidencias.get(0);
            }
            System.out.println("  Coincidencias encontradas:");
            for (int i = 0; i < coincidencias.size(); i++) {
                int indice = indiceCuenta(coincidencias.get(i));
                System.out.printf("  %2d) %-6s %-35s (%s)%n",
                        i + 1, cuentaCodigos.get(indice), cuentaNombres.get(indice), cuentaTipos.get(indice));
            }
            linea("   0) Cancelar", "   V) Volver a buscar");
            while (true) {
                String seleccion = leerString("  Numero de cuenta (0 = cancelar, V = volver a buscar): ");
                if (seleccion.equalsIgnoreCase("V")) break;
                if (seleccion.equals("0")) return null;
                try {
                    int numero = Integer.parseInt(seleccion);
                    if (numero >= 1 && numero <= coincidencias.size()) {
                        int indice = indiceCuenta(coincidencias.get(numero - 1));
                        mostrarCuentaEncontrada(indice);
                        return coincidencias.get(numero - 1);
                    }
                } catch (NumberFormatException e) {
                    // Se informa abajo.
                }
                System.out.println("  Opcion invalida. Intente de nuevo.");
            }
        }
    }
    static void buscarAsientos() {
        int opcion = menu("BUSCAR ASIENTOS", "Por fecha exacta", "Por texto en la descripcion");
        if (opcion == 0) return;
        String fecha = opcion == 1 ? leerFecha("Fecha a buscar") : "";
        String termino = opcion == 2
                ? normalizarTexto(leerString("Texto a buscar en la descripcion: ")) : "";
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < asientoFechas.size(); i++) {
            boolean coincide = opcion == 1
                    ? asientoFechas.get(i).equals(fecha)
                    : normalizarTexto(asientoDescripciones.get(i)).contains(termino);
            if (coincide) indices.add(i);
        }
        System.out.println("\n=== RESULTADOS DE BUSQUEDA ===");
        if (indices.isEmpty()) {
            System.out.println("No se encontraron asientos.");
            return;
        }
        ordenarMovimientos(indices, null);
        for (int k = 0; k < indices.size(); k++) {
            int i = indices.get(k);
            System.out.println("\nResultado #" + (k + 1) + " (Asiento #" + (i + 1) + ")");
            mostrarAsiento(i);
        }
    }
    static void respaldarInformacion() {
        System.out.println("\n--- RESPALDAR INFORMACION ---");
        File carpeta = new File("backup/manual_" + LocalDateTime.now().format(FORMATO_TIMESTAMP));
        if (!carpeta.mkdirs()) {
            System.out.println("No se pudo crear la carpeta de respaldo: " + carpeta.getPath());
            return;
        }
        String[] archivos = {ARCHIVO_CUENTAS, ARCHIVO_ASIENTOS, ARCHIVO_BALANCE};
        int copiados = 0;
        try {
            for (String archivo : archivos) {
                File origen = new File(archivo);
                if (origen.exists()) {
                    Files.copy(origen.toPath(), new File(carpeta, origen.getName()).toPath(),
                            StandardCopyOption.REPLACE_EXISTING);
                    copiados++;
                }
            }
        } catch (IOException e) {
            System.out.println("No se pudo crear el respaldo: " + e.getMessage());
            File[] hijos = carpeta.listFiles();
            if (hijos != null) for (File hijo : hijos) hijo.delete();
            carpeta.delete();
            return;
        }
        if (copiados == 0) {
            System.out.println("No hay archivos de datos para respaldar.");
            carpeta.delete();
            return;
        }
        System.out.println("Respaldo creado en: " + carpeta.getPath());
        System.out.println("Archivos copiados: " + copiados + ".");
    }
    static String leerString(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String entrada = scanner.nextLine().trim();
            if (!entrada.isEmpty()) return entrada;
            System.out.println("La entrada no puede estar vacia.");
        }
    }
    static int leerInt(String mensaje, int minimo, int maximo) {
        while (true) {
            try {
                System.out.print(mensaje);
                String entrada = scanner.nextLine().trim();
                if (entrada.isEmpty()) {
                    System.out.println("La entrada no puede estar vacia.");
                    continue;
                }
                int valor = Integer.parseInt(entrada);
                if (valor >= minimo && valor <= maximo) return valor;
                System.out.println("Opcion invalida. Debe estar entre " + minimo + " y " + maximo + ".");
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un numero entero valido.");
            }
        }
    }
    static double leerDouble(String mensaje) {
        while (true) {
            try {
                System.out.print(mensaje);
                String entrada = scanner.nextLine().trim();
                if (entrada.isEmpty()) {
                    System.out.println("La entrada no puede estar vacia.");
                    continue;
                }
                double valor = parsearMonto(entrada);
                if (!Double.isFinite(valor) || valor <= 0) {
                    System.out.println("El monto debe ser un numero mayor que cero.");
                    continue;
                }
                return valor;
            } catch (NumberFormatException e) {
                if ("monto ambiguo".equals(e.getMessage())) {
                    linea("Monto invalido: hay mas de dos decimales o el separador es ambiguo.",
                            "Escriba 1800 o use el formato 1.800,00.");
                } else {
                    System.out.println("Monto invalido. Use solo numeros y coma o punto decimal (ej: 1800,50).");
                }
            }
        }
    }
    static double parsearMonto(String entrada) {
        String valor = entrada == null ? "" : entrada.trim().replace(" ", "");
        boolean negativo = valor.startsWith("-");
        if (negativo) valor = valor.substring(1);
        int coma = valor.lastIndexOf(','), punto = valor.lastIndexOf('.');
        boolean hayComa = coma >= 0, hayPunto = punto >= 0;
        String entera = valor, decimales = "";
        if (hayComa && hayPunto) {
            boolean comaDecimal = coma > punto;
            char separador = comaDecimal ? ',' : '.';
            char grupo = comaDecimal ? '.' : ',';
            int pos = valor.lastIndexOf(separador);
            entera = valor.substring(0, pos);
            decimales = valor.substring(pos + 1);
            String[] grupos = entera.split(grupo == '.' ? "\\." : ",");
            if (grupos.length > 1) {
                if (grupos[0].isEmpty() || grupos[0].length() > 3) throw new NumberFormatException();
                for (int i = 1; i < grupos.length; i++) {
                    if (grupos[i].length() != 3) throw new NumberFormatException();
                }
                entera = entera.replace(String.valueOf(grupo), "");
            }
        } else if (hayComa || hayPunto) {
            int pos = hayComa ? coma : punto;
            entera = valor.substring(0, pos);
            decimales = valor.substring(pos + 1);
        }
        if (!entera.matches("\\d+")) throw new NumberFormatException();
        if (decimales.length() > 2) throw new NumberFormatException("monto ambiguo");
        if ((hayComa || hayPunto) && !decimales.matches("\\d{1,2}")) throw new NumberFormatException();
        double monto = Double.parseDouble(entera + (decimales.isEmpty() ? "" : "." + decimales));
        return negativo ? -monto : monto;
    }
    static void salir() {
        System.out.println("\nGuardando datos y saliendo...");
        if (bloquearGuardadoAsientos || bloquearGuardadoCuentas) {
            System.out.println("No se guardaron cambios: corrija los archivos de datos y reinicie el programa.");
        } else {
            boolean asientosOk = guardarAsientos();
            boolean cuentasOk = guardarCuentas();
            if (!asientosOk || !cuentasOk) System.out.println("No se pudieron guardar todos los datos.");
        }
        System.out.println("Hasta luego.");
    }
}