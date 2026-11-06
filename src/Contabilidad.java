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

    static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter FORMATO_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

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

    public static void main(String[] args) {
        System.out.println("=== SISTEMA CONTABLE EMPRESARIAL ===");
        cargarDatos();

        while (true) {
            mostrarMenu();
            int opcion = leerInt("Seleccione una opcion (0-11): ", 0, 11);

            switch (opcion) {
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
                case 0:
                case 11: salir(); return;
                default: System.out.println("Opcion invalida.");
            }
        }
    }

    static void mostrarMenu() {
        System.out.println("\n===========================================");
        System.out.println("       SISTEMA CONTABLE EMPRESARIAL");
        System.out.println("===========================================");
        System.out.println("1. Administrar cuentas");
        System.out.println("2. Registrar operacion contable");
        System.out.println("3. Consultar cuentas T");
        System.out.println("4. Generar libro diario");
        System.out.println("5. Generar libro mayor");
        System.out.println("6. Generar balanza de comprobacion");
        System.out.println("7. Generar estado de resultados");
        System.out.println("8. Generar balance general");
        System.out.println("9. Consultar movimientos");
        System.out.println("10. Respaldar informacion");
        System.out.println("11. Salir (o 0)");
    }

    static void menuAdministrarCuentas() {
        while (true) {
            System.out.println("\n--- ADMINISTRAR CUENTAS ---");
            System.out.println("1. Ver catalogo");
            System.out.println("2. Crear cuenta");
            System.out.println("3. Buscar cuenta");
            System.out.println("0. Volver");
            int opcion = leerInt("Seleccione una opcion (0-3): ", 0, 3);

            switch (opcion) {
                case 1: mostrarCuentas(); break;
                case 2: crearCuenta(); break;
                case 3: buscarCuenta(); break;
                case 0: return;
                default: System.out.println("Opcion invalida.");
            }
        }
    }

    static void menuConsultarCuentasT() {
        while (true) {
            System.out.println("\n--- CONSULTAR CUENTAS T ---");
            System.out.println("1. Consultar una cuenta");
            System.out.println("2. Cuentas con movimientos");
            System.out.println("3. Cuentas T de todo el catalogo");
            System.out.println("0. Volver");
            int opcion = leerInt("Seleccione una opcion (0-3): ", 0, 3);

            switch (opcion) {
                case 1: verCuentaTIndividual(); break;
                case 2: verCuentasTConMovimientos(); break;
                case 3: verTodasCuentasT(); break;
                case 0: return;
                default: System.out.println("Opcion invalida.");
            }
        }
    }

    static void menuConsultarMovimientos() {
        while (true) {
            System.out.println("\n--- CONSULTAR MOVIMIENTOS ---");
            System.out.println("1. Ver asientos registrados");
            System.out.println("2. Buscar asientos");
            System.out.println("0. Volver");
            int opcion = leerInt("Seleccione una opcion (0-2): ", 0, 2);

            switch (opcion) {
                case 1: mostrarAsientos(); break;
                case 2: buscarAsientos(); break;
                case 0: return;
                default: System.out.println("Opcion invalida.");
            }
        }
    }

    static void cargarDatos() {
        File dir = new File("data");
        if (!dir.exists()) dir.mkdirs();

        cargarCuentasTexto(ARCHIVO_CUENTAS);
        cargarAsientosTexto(ARCHIVO_ASIENTOS);

        if (cuentaCodigos.isEmpty()) {
            System.out.println("Inicializando catalogo de cuentas...");
            inicializarCuentas();
        }

        recalcularSaldos();
    }

    static void inicializarCuentas() {
        for (String linea : CATALOGO_ESTANDAR) {
            String[] partes = linea.split("\\|");
            agregarCuenta(partes[0], partes[1], partes[2]);
        }
        guardarCuentas();
    }

    static void agregarCuenta(String codigo, String nombre, String tipo) {
        cuentaCodigos.add(codigo);
        cuentaNombres.add(nombre);
        cuentaTipos.add(tipo);
        cuentaSaldos.add(0.0);
    }

    static void guardarCuentas() {
        StringBuilder contenido = new StringBuilder();
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            contenido.append(limpiarCampo(cuentaCodigos.get(i))).append('|')
                    .append(limpiarCampo(cuentaNombres.get(i))).append('|')
                    .append(limpiarCampo(cuentaTipos.get(i))).append('|')
                    .append(cuentaSaldos.get(i)).append(System.lineSeparator());
        }
        escribirArchivoAtomico(ARCHIVO_CUENTAS, contenido.toString());
    }

    static void escribirArchivoAtomico(String ruta, String contenido) {
        File temporal = new File(ruta + ".tmp");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(temporal))) {
            writer.write(contenido);
        } catch (IOException e) {
            System.out.println("Error escribiendo " + ruta + ": " + e.getMessage());
            temporal.delete();
            return;
        }

        try {
            try {
                Files.move(temporal.toPath(), new File(ruta).toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal.toPath(), new File(ruta).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.out.println("Error escribiendo " + ruta + ": " + e.getMessage());
            temporal.delete();
        }
    }

    static String limpiarCampo(String valor) {
        if (valor == null) return "";
        return valor.replace("\r", " ").replace("\n", " ");
    }

    static void advertir(String archivo, int numeroLinea, String mensaje) {
        System.out.println("ADVERTENCIA " + archivo + " linea " + numeroLinea + ": " + mensaje);
    }

    static void cargarCuentasTexto(String archivo) {
        cuentaCodigos.clear();
        cuentaNombres.clear();
        cuentaTipos.clear();
        cuentaSaldos.clear();

        File file = new File(archivo);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String linea;
            int numeroLinea = 0;
            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1);
                if (linea.trim().isEmpty()) continue;
                String[] partes = linea.split("\\|");
                if (partes.length != 4) {
                    advertir(archivo, numeroLinea, "se esperaban 4 campos. Linea omitida.");
                    continue;
                }

                if (!tipoValido(partes[2])) {
                    advertir(archivo, numeroLinea, "tipo de cuenta invalido ('" + partes[2] + "'). Linea omitida.");
                    continue;
                }

                double saldo = 0;
                try {
                    saldo = Double.parseDouble(partes[3]);
                    if (!Double.isFinite(saldo)) throw new NumberFormatException();
                } catch (NumberFormatException e) {
                    advertir(archivo, numeroLinea, "saldo invalido ('" + partes[3] + "'). Se usara 0.0.");
                    saldo = 0;
                }

                if (existeCuenta(partes[0]) || existeCodigoNormalizado(partes[0])) {
                    advertir(archivo, numeroLinea, "codigo duplicado " + partes[0] + ". Se conserva la primera aparicion.");
                    continue;
                }

                cuentaCodigos.add(partes[0]);
                cuentaNombres.add(partes[1]);
                cuentaTipos.add(partes[2]);
                cuentaSaldos.add(saldo);
            }
        } catch (IOException e) {
            System.out.println("Error leyendo " + archivo + ": " + e.getMessage());
        }
    }

    static void guardarAsientos() {
        StringBuilder contenido = new StringBuilder();
        for (int i = 0; i < asientoFechas.size(); i++) {
            contenido.append("ASIENTO|").append(limpiarCampo(asientoFechas.get(i))).append('|')
                    .append(limpiarCampo(asientoDescripciones.get(i))).append('|')
                    .append(asientoTotales.get(i)).append(System.lineSeparator());
            List<String> cods = partidaCodigos.get(i);
            List<String> tips = partidaTipos.get(i);
            List<Double> mons = partidaMontos.get(i);
            for (int j = 0; j < cods.size(); j++) {
                contenido.append("PARTIDA|").append(limpiarCampo(cods.get(j))).append('|')
                        .append(limpiarCampo(tips.get(j))).append('|')
                        .append(mons.get(j)).append(System.lineSeparator());
            }
        }
        escribirArchivoAtomico(ARCHIVO_ASIENTOS, contenido.toString());
    }

    static void cargarAsientosTexto(String archivo) {
        asientoFechas.clear();
        asientoDescripciones.clear();
        asientoTotales.clear();
        partidaCodigos.clear();
        partidaTipos.clear();
        partidaMontos.clear();

        File file = new File(archivo);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String linea;
            int numeroLinea = 0;
            List<String> cods = null;
            List<String> tips = null;
            List<Double> mons = null;

            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1);
                if (linea.trim().isEmpty()) continue;
                String[] partes = linea.split("\\|");

                if (partes[0].equals("ASIENTO")) {
                    cods = null;
                    tips = null;
                    mons = null;
                    try {
                        double total = Double.parseDouble(partes[3]);
                        if (!Double.isFinite(total)) throw new NumberFormatException();
                        asientoFechas.add(partes[1]);
                        asientoDescripciones.add(partes[2]);
                        asientoTotales.add(total);
                        cods = new ArrayList<>();
                        tips = new ArrayList<>();
                        mons = new ArrayList<>();
                        partidaCodigos.add(cods);
                        partidaTipos.add(tips);
                        partidaMontos.add(mons);
                    } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                        advertir(archivo, numeroLinea, "ASIENTO invalido. Asiento omitido.");
                    }
                } else if (partes[0].equals("PARTIDA")) {
                    if (cods == null) {
                        advertir(archivo, numeroLinea, "PARTIDA sin ASIENTO previo. Linea omitida.");
                        continue;
                    }
                    try {
                        String tipo = partes[2];
                        double monto = Double.parseDouble(partes[3]);
                        if (!tipo.equalsIgnoreCase("Debito") && !tipo.equalsIgnoreCase("Credito")) {
                            throw new NumberFormatException();
                        }
                        if (!Double.isFinite(monto) || monto <= 0) throw new NumberFormatException();
                        cods.add(partes[1]);
                        tips.add(partes[2]);
                        mons.add(monto);
                    } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                        advertir(archivo, numeroLinea, "PARTIDA invalida. Partida omitida.");
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error leyendo " + archivo + ": " + e.getMessage());
        }
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
        System.out.println("\n--- NUEVA CUENTA ---");
        System.out.println("Se pediran tres datos: nombre, tipo y codigo.");
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

            if (codigo.equals("0")) {
                System.out.println("El codigo 0 esta reservado para cancelar. Ingrese otro.");
                continue;
            }
            if (codigo.equals("?") || codigo.contains("|")) {
                System.out.println("El codigo no puede ser ? ni contener el caracter |.");
                continue;
            }
            if (existeCuenta(codigo) || existeCodigoNormalizado(codigo)) {
                System.out.println("El codigo ya existe. Ingrese otro.");
                continue;
            }
            break;
        }

        agregarCuenta(codigo, nombre, tipo);
        guardarCuentas();
        System.out.println("Cuenta creada exitosamente: " + codigo + " - " + nombre + " (" + tipo + ").");
    }

    static String pedirTipoCuenta() {
        System.out.println("Tipo de cuenta:");
        for (int i = 0; i < TIPOS_CUENTA.length; i++) {
            System.out.println("  " + (i + 1) + ". " + TIPOS_CUENTA[i]);
        }
        int opcion = leerInt("Seleccione el tipo (1-" + TIPOS_CUENTA.length + "): ", 1, TIPOS_CUENTA.length);
        return TIPOS_CUENTA[opcion - 1];
    }

    static String sugerirCodigoCuenta(String tipo) {
        int indiceTipo = -1;
        for (int i = 0; i < TIPOS_CUENTA.length; i++) {
            if (TIPOS_CUENTA[i].equals(tipo)) {
                indiceTipo = i;
                break;
            }
        }
        if (indiceTipo < 0) return "";

        int base = (indiceTipo + 1) * 1000;
        int mayor = base - 10;
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            try {
                int numero = Integer.parseInt(cuentaCodigos.get(i));
                if (numero >= base && numero <= base + 999 && numero > mayor) {
                    mayor = numero;
                }
            } catch (NumberFormatException e) {
                // Los codigos no numericos no afectan la sugerencia.
            }
        }

        int siguiente = mayor + 10;
        if (siguiente > base + 999) return "";
        return String.format("%04d", siguiente);
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

    static boolean tipoValido(String tipo) {
        for (int i = 0; i < TIPOS_CUENTA.length; i++) {
            if (TIPOS_CUENTA[i].equals(tipo)) return true;
        }
        return false;
    }

    static void registrarAsiento() {
        System.out.println("\n--- REGISTRAR ASIENTO ---");
        System.out.println("Se pediran fecha, descripcion y al menos dos partidas.");
        System.out.println("Cada partida tiene: cuenta, lado (Debe/Haber) y monto.");
        String fecha = leerFecha("Fecha del asiento");
        String descripcion = leerTextoPersistible("Descripcion del asiento (ej: Compra de papeleria): ", "descripcion");

        List<String> cods = new ArrayList<>();
        List<String> tips = new ArrayList<>();
        List<Double> mons = new ArrayList<>();
        double totalDebito = 0;
        double totalCredito = 0;

        while (true) {
            String codigo = pedirCuenta("Cuenta de la partida " + (cods.size() + 1), "cancelar el asiento");
            if (codigo == null) {
                System.out.println("Asiento cancelado.");
                return;
            }

            String tipo = pedirTipoPartida();
            double monto = leerDouble("  Monto (ej: 1500,00): ");
            cods.add(codigo);
            tips.add(tipo);
            mons.add(monto);
            if (tipo.equalsIgnoreCase("Debito")) totalDebito += monto;
            else totalCredito += monto;
            System.out.printf("  Acumulado: Debe %.2f | Haber %.2f%n", totalDebito, totalCredito);

            if (cods.size() < 2) {
                System.out.println("Un asiento necesita al menos 2 partidas. Ingrese la segunda.");
                continue;
            }

            boolean cuadrado = Math.abs(totalDebito - totalCredito) <= 0.01;
            if (!cuadrado) {
                System.out.println("\nEl asiento no cuadra:");
                System.out.printf("  Total del Debe:  %.2f%n", totalDebito);
                System.out.printf("  Total del Haber: %.2f%n", totalCredito);
                System.out.printf("  Diferencia:      %.2f%n", Math.abs(totalDebito - totalCredito));
                System.out.println("Un asiento debe tener el total del Debe igual al total del Haber.");
            }

            String pregunta = cuadrado
                    ? "Agregar otra partida? (S/N): "
                    : "Desea agregar otra partida para cuadrar? (S/N): ";
            if (confirmar(pregunta)) continue;
            if (!cuadrado) {
                System.out.println("Asiento cancelado. No se guardo nada.");
                return;
            }
            break;
        }

        System.out.println("\n--- RESUMEN DEL ASIENTO ---");
        System.out.println("Fecha: " + fecha);
        System.out.println("Descripcion: " + descripcion);
        mostrarPartidas(cods, tips, mons);
        System.out.println("-".repeat(70));
        System.out.printf("%-54s %12.2f%n", "TOTAL DEBE:", totalDebito);
        System.out.printf("%-54s %12.2f%n", "TOTAL HABER:", totalCredito);

        if (!confirmar("Guardar el asiento? (S/N): ")) {
            System.out.println("Asiento cancelado. No se guardo nada.");
            return;
        }

        asientoFechas.add(fecha);
        asientoDescripciones.add(descripcion);
        asientoTotales.add(totalDebito);
        partidaCodigos.add(cods);
        partidaTipos.add(tips);
        partidaMontos.add(mons);

        actualizarSaldos(cods, tips, mons);
        guardarAsientos();
        guardarCuentas();
        System.out.println("Asiento registrado exitosamente.");
    }

    static String pedirTipoPartida() {
        System.out.println("  Lado de la partida:");
        System.out.println("    1. Debe (debito)");
        System.out.println("    2. Haber (credito)");
        int opcion = leerInt("  Seleccione 1 o 2: ", 1, 2);
        return opcion == 1 ? "Debito" : "Credito";
    }

    static boolean confirmar(String mensaje) {
        while (true) {
            String respuesta = leerString(mensaje);
            if (respuesta.equalsIgnoreCase("S") || respuesta.equalsIgnoreCase("SI")) return true;
            if (respuesta.equalsIgnoreCase("N") || respuesta.equalsIgnoreCase("NO")) return false;
            System.out.println("Responda S (si) o N (no).");
        }
    }

    static void actualizarSaldos(List<String> cods, List<String> tips, List<Double> mons) {
        actualizarSaldos(cods, tips, mons, new ArrayList<>());
    }

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
        if (tipoCuenta.equals("Activo") || tipoCuenta.equals("Costo") || tipoCuenta.equals("Gasto")) {
            return debito ? monto : -monto;
        }
        return debito ? -monto : monto;
    }

    static void recalcularSaldos() {
        for (int i = 0; i < cuentaSaldos.size(); i++) {
            cuentaSaldos.set(i, 0.0);
        }

        List<String> avisados = new ArrayList<>();
        for (int i = 0; i < asientoFechas.size(); i++) {
            actualizarSaldos(partidaCodigos.get(i), partidaTipos.get(i), partidaMontos.get(i), avisados);
        }
    }

    static String fechaActual() {
        return LocalDate.now().format(FORMATO_FECHA);
    }

    static String leerFecha(String mensaje) {
        while (true) {
            System.out.print(mensaje + " (dd/MM/yyyy, Enter = hoy): ");
            String entrada = scanner.nextLine().trim();
            if (entrada.isEmpty()) return fechaActual();

            String fecha = convertirFecha(entrada);
            if (fecha != null) return fecha;
        }
    }

    static String convertirFecha(String entrada) {
        try {
            LocalDate fecha = LocalDate.parse(entrada,
                    DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));
            if (fecha.getYear() < 1900 || fecha.getYear() > 2100) {
                System.out.println("Ano invalido. Debe estar entre 1900 y 2100.");
                return null;
            }
            return fecha.format(FORMATO_FECHA);
        } catch (DateTimeParseException e) {
            System.out.println("Fecha invalida. Use dd/MM/yyyy y una fecha real.");
            return null;
        }
    }

    static String fechaOrdenable(String fecha) {
        String[] partes = fecha.split("/");
        if (partes.length == 3) {
            return partes[2] + partes[1] + partes[0];
        }
        return "99999999";
    }

    static void ordenarIndicesPorFecha(List<Integer> indices) {
        for (int i = 0; i < indices.size() - 1; i++) {
            for (int j = 0; j < indices.size() - 1 - i; j++) {
                String fechaA = fechaOrdenable(asientoFechas.get(indices.get(j)));
                String fechaB = fechaOrdenable(asientoFechas.get(indices.get(j + 1)));
                if (fechaB.compareTo(fechaA) < 0) {
                    int temporal = indices.get(j);
                    indices.set(j, indices.get(j + 1));
                    indices.set(j + 1, temporal);
                }
            }
        }
    }

    static void ordenarMovimientos(List<Integer> asientos, List<Integer> partidas) {
        for (int i = 0; i < asientos.size() - 1; i++) {
            for (int j = 0; j < asientos.size() - 1 - i; j++) {
                String fechaA = fechaOrdenable(asientoFechas.get(asientos.get(j)));
                String fechaB = fechaOrdenable(asientoFechas.get(asientos.get(j + 1)));
                if (fechaB.compareTo(fechaA) < 0) {
                    int tempA = asientos.get(j);
                    asientos.set(j, asientos.get(j + 1));
                    asientos.set(j + 1, tempA);
                    int tempP = partidas.get(j);
                    partidas.set(j, partidas.get(j + 1));
                    partidas.set(j + 1, tempP);
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
        System.out.println("\n=== BALANZA DE COMPROBACION ===");
        System.out.println("Historico completo");
        System.out.println("(Verificacion: total de debitos = total de creditos)");
        System.out.println();
        System.out.printf("%-8s %-35s %12s %12s %12s%n", "Codigo", "Nombre", "Debitos", "Creditos", "Saldo");
        System.out.println("-".repeat(85));

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

        double totalDebitos = 0;
        double totalCreditos = 0;
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            if (debitos[i] > 0 || creditos[i] > 0) {
                System.out.printf("%-8s %-35s %12.2f %12.2f %12.2f%n",
                        cuentaCodigos.get(i), cuentaNombres.get(i), debitos[i], creditos[i], cuentaSaldos.get(i));
                totalDebitos += debitos[i];
                totalCreditos += creditos[i];
            }
        }

        System.out.println("-".repeat(85));
        System.out.printf("%-44s %12.2f %12.2f%n", "TOTALES:", totalDebitos, totalCreditos);
        System.out.println();

        if (Math.abs(totalDebitos - totalCreditos) < 0.01) {
            System.out.println("LA BALANZA DE COMPROBACION ESTA CUADRADA.");
        } else {
            System.out.println("ADVERTENCIA: La balanza de comprobacion NO cuadra.");
            System.out.printf("Diferencia: %.2f%n", totalDebitos - totalCreditos);
        }
    }

    static void verLibroDiario() {
        System.out.println("\n=== LIBRO DIARIO ===");
        System.out.println("Historico completo");
        System.out.println("(Ordenado por fecha - No. = numero de asiento original)");
        System.out.println();

        if (asientoFechas.isEmpty()) {
            System.out.println("No hay asientos registrados.");
            return;
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < asientoFechas.size(); i++) {
            indices.add(i);
        }
        ordenarIndicesPorFecha(indices);

        System.out.printf("%-5s %-20s %-30s %12s%n", "No.", "Fecha", "Descripcion", "Total");
        System.out.println("-".repeat(75));
        for (int k = 0; k < indices.size(); k++) {
            int i = indices.get(k);
            System.out.printf("%-5d %-20s %-30s %12.2f%n",
                    i + 1, asientoFechas.get(i), asientoDescripciones.get(i), asientoTotales.get(i));
        }
        System.out.println("-".repeat(75));
        System.out.printf("Total de asientos: %d%n", indices.size());
    }

    static void verLibroMayor() {
        System.out.println("\n=== LIBRO MAYOR ===");
        System.out.println("Historico completo");
        System.out.println("(Movimientos por cuenta individual)");
        System.out.println("(As. = numero de asiento)");
        System.out.println();

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

        List<Integer> asientosMov = new ArrayList<>();
        List<Integer> partidasMov = new ArrayList<>();
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
        System.out.println("\n=== CUENTA T ===");
        System.out.println("Seleccione la cuenta que desea consultar.");
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
        if (mostradas == 0) {
            System.out.println("No hay cuentas con movimientos registrados.");
        } else {
            System.out.println("\nTotal de cuentas con movimientos: " + mostradas);
        }
    }

    static void verTodasCuentasT() {
        System.out.println("\n=== CUENTAS T - CATALOGO COMPLETO ===");
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            mostrarCuentaT(i);
        }
        System.out.println("\nTotal de cuentas: " + cuentaCodigos.size());
    }

    static boolean cuentaTieneMovimientos(int idxCuenta) {
        String codigo = cuentaCodigos.get(idxCuenta);
        for (int i = 0; i < asientoFechas.size(); i++) {
            List<String> cods = partidaCodigos.get(i);
            for (int j = 0; j < cods.size(); j++) {
                if (cods.get(j).equals(codigo)) return true;
            }
        }
        return false;
    }

    static void mostrarCuentaT(int idxCuenta) {
        if (idxCuenta < 0 || idxCuenta >= cuentaCodigos.size()) return;

        String codigo = cuentaCodigos.get(idxCuenta);
        List<Integer> asientosMov = new ArrayList<>();
        List<Integer> partidasMov = new ArrayList<>();
        cargarMovimientosCuenta(codigo, asientosMov, partidasMov);

        List<String> filasDebe = new ArrayList<>();
        List<String> filasHaber = new ArrayList<>();
        double totalDebe = 0;
        double totalHaber = 0;

        for (int k = 0; k < asientosMov.size(); k++) {
            int i = asientosMov.get(k);
            int j = partidasMov.get(k);
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

        System.out.println();
        System.out.println("=".repeat(95));
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
        String etiqueta;
        if (saldo > 0.005) {
            etiqueta = "Deudor";
        } else if (saldo < -0.005) {
            etiqueta = "Acreedor";
        } else {
            etiqueta = "Cero";
            saldo = 0;
        }
        System.out.printf("%63s SALDO: %.2f (%s)%n", "", Math.abs(saldo), etiqueta);
        System.out.println("=".repeat(95));
    }

    static String recortar(String texto, int ancho) {
        if (texto == null) return "";
        if (texto.length() <= ancho) return texto;
        return texto.substring(0, ancho);
    }

    static void verEstadoResultados() {
        System.out.println("\n=== ESTADO DE RESULTADOS ===");
        System.out.println("Historico completo");
        System.out.printf("%-8s %-35s %12s%n", "Codigo", "Nombre", "Saldo");
        System.out.println("-".repeat(60));

        double totalIngresos = mostrarSeccion("Ingreso", "INGRESOS", cuentaSaldos);
        double totalCostos = mostrarSeccion("Costo", "COSTOS", cuentaSaldos);
        double totalGastos = mostrarSeccion("Gasto", "GASTOS", cuentaSaldos);

        double utilidadBruta = totalIngresos - totalCostos;
        double utilidadNeta = utilidadBruta - totalGastos;

        System.out.println("\n" + "-".repeat(60));
        System.out.printf("Utilidad bruta:  %.2f%n", utilidadBruta);
        System.out.printf("Utilidad neta:   %.2f%n", utilidadNeta);
    }

    static void generarBalance() {
        System.out.println("\n=== BALANCE GENERAL ===");
        System.out.println("Balance al " + fechaActual());
        System.out.printf("%-8s %-35s %12s%n", "Codigo", "Nombre", "Saldo");
        System.out.println("-".repeat(60));

        double totalActivo = mostrarSeccion("Activo", "ACTIVO", cuentaSaldos);
        double totalPasivo = mostrarSeccion("Pasivo", "PASIVO", cuentaSaldos);
        double totalPatrimonio = mostrarSeccion("Patrimonio", "PATRIMONIO", cuentaSaldos);
        double resultado = resultadoEjercicio(cuentaSaldos);
        double patrimonioConResultado = totalPatrimonio + resultado;

        System.out.println("\n-- RESULTADO DEL EJERCICIO --");
        System.out.printf("%-44s %12.2f%n", "Resultado del ejercicio:", resultado);

        System.out.println("\n" + "-".repeat(60));
        System.out.printf("Total Pasivo + Patrimonio + Resultado: %.2f%n",
                totalPasivo + patrimonioConResultado);
        System.out.printf("Diferencia: %.2f%n", totalActivo - (totalPasivo + patrimonioConResultado));

        if (Math.abs(totalActivo - (totalPasivo + patrimonioConResultado)) < 0.01) {
            System.out.println("EL BALANCE CUADRA CORRECTAMENTE.");
        } else {
            System.out.println("ADVERTENCIA: El balance NO cuadra.");
        }

        guardarBalance(fechaActual(), totalActivo, totalPasivo, patrimonioConResultado);
    }

    static double mostrarSeccion(String tipo, String titulo, List<Double> saldos) {
        System.out.println("\n-- " + titulo + " --");
        double total = 0;
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            if (cuentaTipos.get(i).equals(tipo)) {
                System.out.printf("%-8s %-35s %12.2f%n",
                        cuentaCodigos.get(i), cuentaNombres.get(i), saldos.get(i));
                total += saldos.get(i);
            }
        }
        System.out.printf("%-44s %12.2f%n", "TOTAL " + titulo + ":", total);
        return total;
    }

    static double totalPorTipo(List<Double> saldos, String tipo) {
        double total = 0;
        for (int i = 0; i < cuentaTipos.size() && i < saldos.size(); i++) {
            if (cuentaTipos.get(i).equals(tipo)) {
                total += saldos.get(i);
            }
        }
        return total;
    }

    static double resultadoEjercicio(List<Double> saldos) {
        return totalPorTipo(saldos, "Ingreso") - totalPorTipo(saldos, "Costo")
                - totalPorTipo(saldos, "Gasto");
    }

    static void guardarBalance(String fecha, double activo, double pasivo, double patrimonio) {
        String contenido = fecha + "|" + activo + "|" + pasivo + "|" + patrimonio
                + System.lineSeparator();
        escribirArchivoAtomico(ARCHIVO_BALANCE, contenido);
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
        for (int k = 0; k < encontradas.size(); k++) {
            int i = indiceCuenta(encontradas.get(k));
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
        if (indice < 0) return "(cuenta no existe)";
        return cuentaNombres.get(indice);
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
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    static void mostrarCuentaEncontrada(int indice) {
        System.out.printf("    Cuenta seleccionada: %s - %s (%s)%n",
                cuentaCodigos.get(indice), cuentaNombres.get(indice), cuentaTipos.get(indice));
    }

    static String pedirCuenta(String contexto, String accionCancelar) {
        while (true) {
            System.out.println("  " + contexto);
            System.out.println("    Escriba el codigo (ej: 1000) o parte del nombre (ej: caja).");
            System.out.println("    ? = ver el catalogo | 0 = " + accionCancelar);
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
            System.out.println("   0) Cancelar");
            System.out.println("   V) Volver a buscar");

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
        System.out.println("\n--- BUSCAR ASIENTOS ---");
        System.out.println("1. Por fecha exacta");
        System.out.println("2. Por texto en la descripcion");
        System.out.println("0. Volver");
        int opcion = leerInt("Seleccione una opcion (0-2): ", 0, 2);
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

        ordenarIndicesPorFecha(indices);
        for (int k = 0; k < indices.size(); k++) {
            int i = indices.get(k);
            System.out.println("\nResultado #" + (k + 1) + " (Asiento #" + (i + 1) + ")");
            mostrarAsiento(i);
        }
    }

    static void respaldarInformacion() {
        System.out.println("\n--- RESPALDAR INFORMACION ---");
        String marca = LocalDateTime.now().format(FORMATO_TIMESTAMP);
        File carpeta = new File("backup/manual_" + marca);
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
            if (hijos != null) {
                for (File hijo : hijos) hijo.delete();
            }
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

    static int leerInt(String mensaje) {
        while (true) {
            try {
                System.out.print(mensaje);
                String entrada = scanner.nextLine().trim();
                if (entrada.isEmpty()) {
                    System.out.println("La entrada no puede estar vacia.");
                    continue;
                }
                return Integer.parseInt(entrada);
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un numero entero valido.");
            }
        }
    }

    static int leerInt(String mensaje, int minimo, int maximo) {
        while (true) {
            int valor = leerInt(mensaje);
            if (valor >= minimo && valor <= maximo) return valor;
            System.out.println("Opcion invalida. Debe estar entre " + minimo + " y " + maximo + ".");
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
                    System.out.println("Monto invalido: un punto con tres decimales es ambiguo (ej: 1.800).");
                    System.out.println("Escriba 1800 o use el formato 1.800,00.");
                } else {
                    System.out.println("Monto invalido. Use solo numeros y coma o punto decimal (ej: 1800,50).");
                }
            }
        }
    }

    static double parsearMonto(String entrada) {
        String valor = entrada.trim().replace(" ", "");
        int coma = valor.lastIndexOf(',');
        int punto = valor.lastIndexOf('.');

        if (coma >= 0 && punto >= 0) {
            if (coma > punto) {
                valor = valor.replace(".", "").replace(',', '.');
            } else {
                valor = valor.replace(",", "");
            }
        } else if (coma >= 0) {
            valor = valor.replace(',', '.');
        } else if (punto >= 0) {
            int primerPunto = valor.indexOf('.');
            int decimales = valor.length() - punto - 1;
            if (primerPunto == punto && decimales == 3) {
                throw new NumberFormatException("monto ambiguo");
            }
            if (primerPunto != punto) {
                valor = valor.replace(".", "");
            }
        }

        return Double.parseDouble(valor);
    }

    static void salir() {
        System.out.println("\nGuardando datos y saliendo...");
        guardarAsientos();
        guardarCuentas();
        System.out.println("Hasta luego.");
    }
}
