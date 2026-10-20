import java.io.*;
import java.nio.file.*;
import java.text.*;
import java.time.*;
import java.util.*;

public class Contabilidad {

    static final String ARCHIVO_CUENTAS = "data/cuentas.txt";
    static final String ARCHIVO_ASIENTOS = "data/asientos.txt";
    static final String ARCHIVO_BALANCE = "data/balance.txt";

    static final Scanner scanner = new Scanner(System.in);

    static final String[] TIPOS_CUENTA = {"Activo", "Pasivo", "Patrimonio", "Ingreso", "Costo", "Gasto"};

    // Estructuras de datos (listas paralelas primitivas - sin arreglos heterogeneos, Maps ni POO)
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

    // Marcas de modificacion para detectar cambios externos en los archivos
    static long ultimaModCuentas = 0;
    static long ultimaModAsientos = 0;

    // Periodo configurable para los reportes (vacio = sin limite)
    static String periodoDesde = "";
    static String periodoHasta = "";

    public static void main(String[] args) {
        System.out.println("=== SISTEMA DE CONTABILIDAD ===");
        cargarDatos();

        while (true) {
            revisarCambiosExternos();
            mostrarMenu();
            int opcion = leerInt("Seleccione una opcion: ");

            switch (opcion) {
                case 1: mostrarCuentas(); break;
                case 2: crearCuenta(); break;
                case 3: registrarAsiento(); break;
                case 4: mostrarAsientos(); break;
                case 5: generarBalance(); break;
                case 6: buscarCuenta(); break;
                case 7: verEstadoResultados(); break;
                case 8: verEstadoFlujoEfectivo(); break;
                case 9: verEstadoCambiosPatrimonio(); break;
                case 10: verBalanceComprobacion(); break;
                case 11: verLibroDiario(); break;
                case 12: verLibroMayor(); break;
                case 13: revertirAsiento(); break;
                case 14: reiniciarDatos(); break;
                case 15: buscarAsientos(); break;
                case 16: recalcularSaldosManual(); break;
                case 17: recargarDatosDesdeArchivos(); break;
                case 18: configurarPeriodoReportes(); break;
                case 19: restaurarRespaldo(); break;
                case 0: salir(); return;
                default: System.out.println("Opcion invalida.");
            }
        }
    }

    static void mostrarMenu() {
        System.out.println("\n--- MENU PRINCIPAL ---");
        System.out.println("1. Ver catalogo de cuentas");
        System.out.println("2. Crear nueva cuenta");
        System.out.println("3. Registrar asiento contable");
        System.out.println("4. Ver asientos registrados");
        System.out.println("5. Generar balance general");
        System.out.println("6. Buscar cuenta");
        System.out.println("7. Ver estado de resultados");
        System.out.println("8. Ver estado de flujo de efectivo");
        System.out.println("9. Ver estado de cambios en el patrimonio");
        System.out.println("10. Ver balance de comprobacion");
        System.out.println("11. Ver libro diario");
        System.out.println("12. Ver libro mayor");
        System.out.println("13. Revertir asiento");
        System.out.println("14. Reiniciar datos de prueba (borra asientos y saldos)");
        System.out.println("15. Buscar asientos");
        System.out.println("16. Recalcular saldos desde asientos");
        System.out.println("17. Recargar datos desde archivos");
        System.out.println("18. Configurar periodo de reportes");
        System.out.println("19. Restaurar respaldo");
        System.out.println("0. Salir");
    }

    static void cargarDatos() {
        File dir = new File("data");
        if (!dir.exists()) dir.mkdirs();

        cargarCuentasTexto(ARCHIVO_CUENTAS);
        cargarAsientosTexto(ARCHIVO_ASIENTOS);

        if (cuentaCodigos.isEmpty()) {
            System.out.println("Inicializando catalogo de cuentas...");
            inicializarCuentas();
        } else if (cuentaCodigos.size() < 75) {
            System.out.println("NOTA: Hay " + (75 - cuentaCodigos.size()) + " cuentas nuevas disponibles.");
            System.out.println("Use la opcion 14 para reiniciar los datos y restaurar el catalogo.");
        }

        respaldarDiario();

        aplicarAjustesAutomaticos();

        actualizarMarcasArchivos();
    }

    static void inicializarCuentas() {
        // ACTIVO
        agregarCuenta("1000", "Caja", "Activo");
        agregarCuenta("1100", "Bancos", "Activo");
        agregarCuenta("1110", "Inversiones temporales", "Activo");
        agregarCuenta("1200", "Cuentas por cobrar", "Activo");
        agregarCuenta("1210", "Clientes", "Activo");
        agregarCuenta("1220", "Documentos por cobrar", "Activo");
        agregarCuenta("1230", "Deudores diversos", "Activo");
        agregarCuenta("1300", "Inventarios", "Activo");
        agregarCuenta("1310", "Materias primas", "Activo");
        agregarCuenta("1320", "Productos terminados", "Activo");
        agregarCuenta("1400", "Activos fijos", "Activo");
        agregarCuenta("1410", "Mobiliario y equipo", "Activo");
        agregarCuenta("1420", "Equipo de transporte", "Activo");
        agregarCuenta("1430", "Equipo de computo", "Activo");
        agregarCuenta("1500", "Activos diferidos", "Activo");
        agregarCuenta("1510", "Gastos pagados por anticipado", "Activo");
        agregarCuenta("1520", "Seguros pagados por anticipado", "Activo");
        agregarCuenta("1600", "Cuentas por cobrar a largo plazo", "Activo");
        agregarCuenta("1700", "Propiedad, planta y equipo", "Activo");
        agregarCuenta("1790", "Depreciacion acumulada", "Activo");
        agregarCuenta("1800", "Activos intangibles", "Activo");
        agregarCuenta("1890", "Amortizacion acumulada", "Activo");

        // PASIVO
        agregarCuenta("2000", "Cuentas por pagar", "Pasivo");
        agregarCuenta("2010", "Proveedores", "Pasivo");
        agregarCuenta("2020", "Acreedores diversos", "Pasivo");
        agregarCuenta("2100", "Prestamos bancarios", "Pasivo");
        agregarCuenta("2110", "Prestamos a corto plazo", "Pasivo");
        agregarCuenta("2120", "Prestamos a largo plazo", "Pasivo");
        agregarCuenta("2200", "Impuestos por pagar", "Pasivo");
        agregarCuenta("2210", "IVA por pagar", "Pasivo");
        agregarCuenta("2220", "ISR por pagar", "Pasivo");
        agregarCuenta("2300", "Pasivos diferidos", "Pasivo");
        agregarCuenta("2310", "Anticipos de clientes", "Pasivo");
        agregarCuenta("2320", "Intereses cobrados por anticipado", "Pasivo");
        agregarCuenta("2400", "Pasivos por impuestos diferidos", "Pasivo");
        agregarCuenta("2500", "Obligaciones financieras a largo plazo", "Pasivo");
        agregarCuenta("2600", "Pasivos por beneficios a empleados", "Pasivo");

        // PATRIMONIO
        agregarCuenta("3000", "Capital social", "Patrimonio");
        agregarCuenta("3010", "Capital aportado", "Patrimonio");
        agregarCuenta("3020", "Capital ganado", "Patrimonio");
        agregarCuenta("3100", "Utilidades retenidas", "Patrimonio");
        agregarCuenta("3110", "Utilidad del ejercicio", "Patrimonio");
        agregarCuenta("3120", "Perdida del ejercicio", "Patrimonio");
        agregarCuenta("3200", "Resultados del ejercicio", "Patrimonio");
        agregarCuenta("3300", "Reservas legales", "Patrimonio");
        agregarCuenta("3400", "Ajustes por revaluacion", "Patrimonio");
        agregarCuenta("3500", "Dividendos decretados", "Patrimonio");

        // INGRESOS
        agregarCuenta("4000", "Ingresos", "Ingreso");
        agregarCuenta("4010", "Ventas", "Ingreso");
        agregarCuenta("4020", "Ingresos por servicios", "Ingreso");
        agregarCuenta("4030", "Ingresos por intereses", "Ingreso");
        agregarCuenta("4040", "Otros ingresos", "Ingreso");
        agregarCuenta("4050", "Ingresos por alquileres", "Ingreso");
        agregarCuenta("4060", "Ingresos por comisiones", "Ingreso");
        agregarCuenta("4070", "Ingresos por dividendos", "Ingreso");

        // COSTOS
        agregarCuenta("5000", "Costos", "Costo");
        agregarCuenta("5010", "Costo de ventas", "Costo");
        agregarCuenta("5020", "Costo de produccion", "Costo");
        agregarCuenta("5030", "Costo de lo vendido", "Costo");

        // GASTOS
        agregarCuenta("6000", "Gastos", "Gasto");
        agregarCuenta("6010", "Gastos de venta", "Gasto");
        agregarCuenta("6020", "Gastos de administracion", "Gasto");
        agregarCuenta("6030", "Gastos financieros", "Gasto");
        agregarCuenta("6040", "Gastos de operacion", "Gasto");
        agregarCuenta("6050", "Sueldos y salarios", "Gasto");
        agregarCuenta("6060", "Renta", "Gasto");
        agregarCuenta("6070", "Servicios publicos", "Gasto");
        agregarCuenta("6080", "Depreciacion", "Gasto");
        agregarCuenta("6090", "Amortizacion", "Gasto");
        agregarCuenta("6100", "Gastos de publicidad y mercadeo", "Gasto");
        agregarCuenta("6110", "Gastos de mantenimiento", "Gasto");
        agregarCuenta("6120", "Gastos de seguros", "Gasto");
        agregarCuenta("6130", "Gastos de impuestos", "Gasto");
        agregarCuenta("6140", "Gastos de capacitacion", "Gasto");
        agregarCuenta("6150", "Gastos de viajes y representacion", "Gasto");

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
            contenido.append(limpiarCampo(cuentaCodigos.get(i))).append("|")
                    .append(limpiarCampo(cuentaNombres.get(i))).append("|")
                    .append(limpiarCampo(cuentaTipos.get(i))).append("|")
                    .append(cuentaSaldos.get(i)).append(System.lineSeparator());
        }
        if (escribirArchivoAtomico(ARCHIVO_CUENTAS, contenido.toString())) {
            ultimaModCuentas = marcaArchivo(ARCHIVO_CUENTAS);
        }
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
                Files.move(temporal.toPath(), new File(ruta).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            System.out.println("Error escribiendo " + ruta + ": " + e.getMessage());
            temporal.delete();
            return false;
        }
    }

    static String limpiarCampo(String valor) {
        if (valor == null) return "";
        return valor.replace("|", "/").replace("\r", " ").replace("\n", " ");
    }

    static long marcaArchivo(String ruta) {
        File archivo = new File(ruta);
        if (!archivo.exists()) return 0;
        return archivo.lastModified();
    }

    static void actualizarMarcasArchivos() {
        ultimaModCuentas = marcaArchivo(ARCHIVO_CUENTAS);
        ultimaModAsientos = marcaArchivo(ARCHIVO_ASIENTOS);
    }

    static void revisarCambiosExternos() {
        boolean cambios = false;

        if (marcaArchivo(ARCHIVO_CUENTAS) != ultimaModCuentas) {
            System.out.println("ATENCION: " + ARCHIVO_CUENTAS + " fue modificado fuera del programa.");
            cambios = true;
        }
        if (marcaArchivo(ARCHIVO_ASIENTOS) != ultimaModAsientos) {
            System.out.println("ATENCION: " + ARCHIVO_ASIENTOS + " fue modificado fuera del programa.");
            cambios = true;
        }

        if (cambios) {
            System.out.println("Los cambios externos no estan en memoria. Use la opcion 17 para recargar");
            System.out.println("o continue: el proximo guardado sobrescribira esas ediciones.");
            actualizarMarcasArchivos();
        }
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
                    System.out.println("ADVERTENCIA " + archivo + " linea " + numeroLinea
                            + ": se esperaban 4 campos. Linea omitida.");
                    continue;
                }

                double saldo = 0.0;
                try {
                    saldo = Double.parseDouble(partes[3]);
                } catch (NumberFormatException e) {
                    System.out.println("ADVERTENCIA " + archivo + " linea " + numeroLinea
                            + ": saldo invalido ('" + partes[3] + "'). Se usara 0.0 y se recalculara desde los asientos.");
                }
                if (!Double.isFinite(saldo)) {
                    System.out.println("ADVERTENCIA " + archivo + " linea " + numeroLinea
                            + ": saldo no finito. Se usara 0.0.");
                    saldo = 0.0;
                }

                if (existeCuenta(partes[0]) || existeCodigoNormalizado(partes[0])) {
                    System.out.println("ADVERTENCIA " + archivo + " linea " + numeroLinea
                            + ": codigo duplicado " + partes[0] + ". Se conserva la primera aparicion.");
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
            contenido.append("ASIENTO|").append(limpiarCampo(asientoFechas.get(i))).append("|")
                    .append(limpiarCampo(asientoDescripciones.get(i))).append("|")
                    .append(asientoTotales.get(i)).append(System.lineSeparator());
            List<String> cods = partidaCodigos.get(i);
            List<String> tips = partidaTipos.get(i);
            List<Double> mons = partidaMontos.get(i);
            for (int j = 0; j < cods.size(); j++) {
                contenido.append("PARTIDA|").append(limpiarCampo(cods.get(j))).append("|")
                        .append(limpiarCampo(tips.get(j))).append("|")
                        .append(mons.get(j)).append(System.lineSeparator());
            }
        }
        if (escribirArchivoAtomico(ARCHIVO_ASIENTOS, contenido.toString())) {
            ultimaModAsientos = marcaArchivo(ARCHIVO_ASIENTOS);
        }
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
            List<String> currentCods = null;
            List<String> currentTips = null;
            List<Double> currentMons = null;

            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1);
                if (linea.trim().isEmpty()) continue;
                String[] partes = linea.split("\\|");

                if (partes.length >= 2 && partes[0].equals("ASIENTO")) {
                    if (currentCods != null) {
                        partidaCodigos.add(currentCods);
                        partidaTipos.add(currentTips);
                        partidaMontos.add(currentMons);
                        currentCods = null;
                        currentTips = null;
                        currentMons = null;
                    }

                    if (partes.length >= 4) {
                        double total = 0.0;
                        boolean totalValido = true;
                        try {
                            total = Double.parseDouble(partes[3]);
                        } catch (NumberFormatException e) {
                            totalValido = false;
                        }

                        if (!totalValido || !Double.isFinite(total)) {
                            System.out.println("ADVERTENCIA " + archivo + " linea " + numeroLinea
                                    + ": total de asiento invalido. Asiento omitido.");
                        } else {
                            asientoFechas.add(partes[1]);
                            asientoDescripciones.add(partes[2]);
                            asientoTotales.add(total);
                            currentCods = new ArrayList<>();
                            currentTips = new ArrayList<>();
                            currentMons = new ArrayList<>();
                        }
                    } else {
                        System.out.println("ADVERTENCIA " + archivo + " linea " + numeroLinea
                                + ": encabezado ASIENTO con campos incompletos. Asiento omitido.");
                    }
                } else if (partes.length >= 2 && partes[0].equals("PARTIDA")) {
                    if (currentCods == null) {
                        System.out.println("ADVERTENCIA " + archivo + " linea " + numeroLinea
                                + ": PARTIDA sin ASIENTO previo. Linea omitida.");
                        continue;
                    }
                    if (partes.length < 4) {
                        System.out.println("ADVERTENCIA " + archivo + " linea " + numeroLinea
                                + ": PARTIDA con campos incompletos. Linea omitida.");
                        continue;
                    }

                    String tipoPartida = partes[2];
                    double montoPartida;
                    try {
                        montoPartida = Double.parseDouble(partes[3]);
                    } catch (NumberFormatException e) {
                        System.out.println("ADVERTENCIA " + archivo + " linea " + numeroLinea
                                + ": monto de partida invalido. Partida omitida.");
                        continue;
                    }

                    if (!tipoPartida.equalsIgnoreCase("Debito") && !tipoPartida.equalsIgnoreCase("Credito")) {
                        System.out.println("ADVERTENCIA " + archivo + " linea " + numeroLinea
                                + ": tipo de partida invalido ('" + tipoPartida + "'). Partida omitida.");
                        continue;
                    }

                    if (!Double.isFinite(montoPartida) || montoPartida <= 0) {
                        System.out.println("ADVERTENCIA " + archivo + " linea " + numeroLinea
                                + ": el monto de la partida debe ser mayor que cero. Partida omitida.");
                        continue;
                    }

                    currentCods.add(partes[1]);
                    currentTips.add(partes[2]);
                    currentMons.add(montoPartida);
                }
            }

            if (currentCods != null) {
                partidaCodigos.add(currentCods);
                partidaTipos.add(currentTips);
                partidaMontos.add(currentMons);
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
            System.out.printf("%-8s %-35s %-15s %12.2f%n", cuentaCodigos.get(i), cuentaNombres.get(i), cuentaTipos.get(i), cuentaSaldos.get(i));
        }
    }

    static void crearCuenta() {
        System.out.println("\n--- NUEVA CUENTA ---");
        String nombre = leerString("Nombre: ");
        String tipo = pedirTipoCuenta();
        if (!tipoValido(tipo)) {
            System.out.println("Tipo invalido.");
            return;
        }
        String sugerido = sugerirCodigoCuenta(tipo);

        String codigo;
        while (true) {
            if (sugerido.isEmpty()) {
                codigo = leerString("Codigo: ");
            } else {
                codigo = leerTextoOpcional("Codigo (Enter = " + sugerido + "): ");
                if (codigo.isEmpty()) codigo = sugerido;
            }

            if (codigo.isEmpty()) {
                System.out.println("Debe ingresar un codigo.");
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
        while (true) {
            System.out.println("Tipo de cuenta:");
            for (int i = 0; i < TIPOS_CUENTA.length; i++) {
                System.out.println("  " + (i + 1) + ". " + TIPOS_CUENTA[i]);
            }
            int opcion = leerInt("Seleccione el tipo (1-" + TIPOS_CUENTA.length + "): ");
            if (opcion >= 1 && opcion <= TIPOS_CUENTA.length) {
                return TIPOS_CUENTA[opcion - 1];
            }
            System.out.println("Opcion invalida. Intente de nuevo.");
        }
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

    static boolean tipoValido(String tipo) {
        for (int i = 0; i < TIPOS_CUENTA.length; i++) {
            if (TIPOS_CUENTA[i].equals(tipo)) return true;
        }
        return false;
    }

    static void registrarAsiento() {
        System.out.println("\n--- REGISTRAR ASIENTO ---");
        System.out.println("Puede buscar cada cuenta por codigo o nombre. Escriba ? para ver el catalogo.");
        System.out.println("Escriba 0 en la cuenta para cancelar el asiento.");
        String fecha = leerFecha("Ingrese la fecha del asiento");
        String descripcion = leerString("Descripcion: ");

        List<String> cods = new ArrayList<>();
        List<String> tips = new ArrayList<>();
        List<Double> mons = new ArrayList<>();
        double totalDebito = 0;
        double totalCredito = 0;

        while (true) {
            System.out.println("Partida " + (cods.size() + 1) + ":");
            String codigo = pedirCuenta();
            if (codigo == null) {
                System.out.println("Asiento cancelado.");
                return;
            }

            String tipo = pedirTipoPartida();
            double monto = leerDouble("  Monto: ");

            cods.add(codigo);
            tips.add(tipo);
            mons.add(monto);

            if (tipo.equalsIgnoreCase("Debito")) totalDebito += monto;
            else totalCredito += monto;

            if (cods.size() >= 2) {
                if (!confirmar("Agregar otra partida? (S/N): ")) {
                    break;
                }
            } else {
                System.out.println("Un asiento necesita al menos 2 partidas. Cargue la siguiente.");
            }
        }

        if (Math.abs(totalDebito - totalCredito) > 0.01) {
            System.out.println("ERROR: El asiento no cuadra. Debitos=" + totalDebito + " Creditos=" + totalCredito);
            return;
        }

        System.out.println("\n--- RESUMEN DEL ASIENTO ---");
        System.out.println("Fecha: " + fecha);
        System.out.println("Descripcion: " + descripcion);
        System.out.printf("%-8s %-35s %-10s %12s%n", "Codigo", "Nombre", "Tipo", "Monto");
        System.out.println("-".repeat(70));
        for (int i = 0; i < cods.size(); i++) {
            System.out.printf("%-8s %-35s %-10s %12.2f%n",
                    cods.get(i), nombreCuenta(cods.get(i)), tips.get(i), mons.get(i));
        }
        System.out.println("-".repeat(70));
        System.out.printf("%-54s %12.2f%n", "TOTAL DEBITOS:", totalDebito);
        System.out.printf("%-54s %12.2f%n", "TOTAL CREDITOS:", totalCredito);

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
        while (true) {
            System.out.println("  Tipo de partida:");
            System.out.println("  1. Debito");
            System.out.println("  2. Credito");
            int opcion = leerInt("  Seleccione (1-2): ");
            if (opcion == 1) return "Debito";
            if (opcion == 2) return "Credito";
            System.out.println("  Opcion invalida. Intente de nuevo.");
        }
    }

    static boolean confirmar(String mensaje) {
        while (true) {
            String respuesta = leerString(mensaje);
            if (respuesta.equalsIgnoreCase("S") || respuesta.equalsIgnoreCase("SI")) return true;
            if (respuesta.equalsIgnoreCase("N") || respuesta.equalsIgnoreCase("NO")) return false;
            System.out.println("Responda S o N.");
        }
    }

    static void actualizarSaldos(List<String> cods, List<String> tips, List<Double> mons) {
        actualizarSaldos(cods, tips, mons, new ArrayList<>());
    }

    static void actualizarSaldos(List<String> cods, List<String> tips, List<Double> mons, List<String> avisados) {
        int cantidad = Math.min(cods.size(), Math.min(tips.size(), mons.size()));
        for (int i = 0; i < cantidad; i++) {
            String codigo = cods.get(i);
            String tipo = tips.get(i);
            double monto = mons.get(i);

            int j = indiceCuenta(codigo);
            if (j < 0) {
                if (!avisados.contains(codigo)) {
                    avisados.add(codigo);
                    System.out.println("ADVERTENCIA: La cuenta " + codigo + " no existe. Partida omitida.");
                }
                continue;
            }

            double saldo = cuentaSaldos.get(j);
            String tipoCuenta = cuentaTipos.get(j);

            if (tipoCuenta.equals("Activo") || tipoCuenta.equals("Costo") || tipoCuenta.equals("Gasto")) {
                saldo += tipo.equalsIgnoreCase("Debito") ? monto : -monto;
            } else {
                saldo += tipo.equalsIgnoreCase("Credito") ? monto : -monto;
            }
            cuentaSaldos.set(j, saldo);
        }
    }

    static boolean recalcularSaldos() {
        List<Double> anteriores = new ArrayList<>(cuentaSaldos);
        for (int i = 0; i < cuentaSaldos.size(); i++) {
            cuentaSaldos.set(i, 0.0);
        }

        List<String> avisados = new ArrayList<>();
        for (int i = 0; i < asientoFechas.size(); i++) {
            actualizarSaldos(partidaCodigos.get(i), partidaTipos.get(i), partidaMontos.get(i), avisados);
        }

        return contarSaldosDiferentes(anteriores, cuentaSaldos) > 0;
    }

    static int contarSaldosDiferentes(List<Double> anteriores, List<Double> actuales) {
        int cambios = 0;
        for (int i = 0; i < actuales.size(); i++) {
            if (Math.abs(anteriores.get(i) - actuales.get(i)) > 0.01) {
                cambios++;
            }
        }
        return cambios;
    }

    static boolean normalizarFechasAsientos() {
        boolean cambios = false;
        for (int i = 0; i < asientoFechas.size(); i++) {
            String original = asientoFechas.get(i);
            String normalizada = normalizarFecha(original);
            if (!normalizada.equals(original)) {
                asientoFechas.set(i, normalizada);
                cambios = true;
            }
        }
        return cambios;
    }

    static String normalizarFecha(String fecha) {
        if (fecha.matches("\\d{2}/\\d{2}/\\d{4}")) {
            return fecha;
        }

        try {
            SimpleDateFormat formatoLargo = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
            formatoLargo.setLenient(false);
            Date fechaLarga = formatoLargo.parse(fecha);
            return new SimpleDateFormat("dd/MM/yyyy").format(fechaLarga);
        } catch (ParseException e) {
            System.out.println("ADVERTENCIA: No se pudo normalizar la fecha: " + fecha);
            return fecha;
        }
    }

    static String fechaActual() {
        return new SimpleDateFormat("dd/MM/yyyy").format(new Date());
    }

    static int revisarAsientos() {
        int totalesCorregidos = 0;

        for (int i = 0; i < asientoFechas.size(); i++) {
            List<String> cods = partidaCodigos.get(i);
            List<String> tips = partidaTipos.get(i);
            List<Double> mons = partidaMontos.get(i);

            int cantidad = Math.min(cods.size(), Math.min(tips.size(), mons.size()));
            if (cods.size() != cantidad || tips.size() != cantidad || mons.size() != cantidad) {
                System.out.println("ADVERTENCIA: El asiento #" + (i + 1)
                        + " tiene partidas desalineadas. Se recortaron a " + cantidad + ".");
                while (cods.size() > cantidad) cods.remove(cods.size() - 1);
                while (tips.size() > cantidad) tips.remove(tips.size() - 1);
                while (mons.size() > cantidad) mons.remove(mons.size() - 1);
            }

            if (cods.size() < 2) {
                System.out.println("ADVERTENCIA: El asiento #" + (i + 1) + " (" + asientoFechas.get(i)
                        + ") tiene menos de 2 partidas.");
            }

            double totalDebito = 0;
            double totalCredito = 0;
            for (int j = 0; j < cods.size(); j++) {
                if (tips.get(j).equalsIgnoreCase("Credito")) {
                    totalCredito += mons.get(j);
                } else {
                    totalDebito += mons.get(j);
                }
            }

            if (Math.abs(totalDebito - totalCredito) > 0.01) {
                System.out.printf("ADVERTENCIA: El asiento #%d (%s) no cuadra: Debitos=%.2f Creditos=%.2f. Se conserva.%n",
                        i + 1, asientoFechas.get(i), totalDebito, totalCredito);
            }

            if (Math.abs(totalDebito - asientoTotales.get(i)) > 0.01) {
                System.out.printf("Correccion: El total del asiento #%d era %.2f y se ajusto a %.2f.%n",
                        i + 1, asientoTotales.get(i), totalDebito);
                asientoTotales.set(i, totalDebito);
                totalesCorregidos++;
            }
        }

        return totalesCorregidos;
    }

    static void aplicarAjustesAutomaticos() {
        List<Double> saldosAntes = new ArrayList<>(cuentaSaldos);
        List<String> fechasAntes = new ArrayList<>(asientoFechas);
        List<Double> totalesAntes = new ArrayList<>(asientoTotales);

        int totalesCorregidos = revisarAsientos();
        boolean cambiosSaldos = recalcularSaldos();
        boolean cambiosFechas = normalizarFechasAsientos();

        if (!cambiosSaldos && !cambiosFechas && totalesCorregidos == 0) {
            return;
        }

        String respaldo = respaldarDatos("ajuste");
        if (respaldo == null) {
            for (int i = 0; i < cuentaSaldos.size(); i++) {
                cuentaSaldos.set(i, saldosAntes.get(i));
            }
            for (int i = 0; i < asientoFechas.size(); i++) {
                asientoFechas.set(i, fechasAntes.get(i));
            }
            for (int i = 0; i < asientoTotales.size(); i++) {
                asientoTotales.set(i, totalesAntes.get(i));
            }
            System.out.println("ADVERTENCIA: Se detectaron diferencias, pero no se aplico el ajuste");
            System.out.println("porque no se pudo crear el respaldo. Use la opcion 16 para reintentar.");
            return;
        }

        guardarAsientos();
        guardarCuentas();

        int cuentasCambiadas = contarSaldosDiferentes(saldosAntes, cuentaSaldos);
        System.out.println("Ajuste automatico aplicado.");
        if (cambiosSaldos) {
            System.out.println("- Saldos recalculados desde los asientos (" + cuentasCambiadas + " cuentas).");
        }
        if (cambiosFechas) {
            System.out.println("- Fechas de asientos normalizadas a dd/MM/yyyy.");
        }
        if (totalesCorregidos > 0) {
            System.out.println("- Totales de asientos corregidos (" + totalesCorregidos + ").");
        }
        System.out.println("Respaldo: " + respaldo);
    }

    static void recalcularSaldosManual() {
        System.out.println("\n--- RECALCULAR SALDOS ---");
        List<Double> saldosAntes = new ArrayList<>(cuentaSaldos);
        List<String> fechasAntes = new ArrayList<>(asientoFechas);
        List<Double> totalesAntes = new ArrayList<>(asientoTotales);

        int totalesCorregidos = revisarAsientos();
        boolean cambiosSaldos = recalcularSaldos();
        boolean cambiosFechas = normalizarFechasAsientos();

        if (!cambiosSaldos && !cambiosFechas && totalesCorregidos == 0) {
            System.out.println("Los saldos, las fechas y los totales ya estan correctos.");
            return;
        }

        String respaldo = respaldarDatos("recalculo");
        if (respaldo == null) {
            for (int i = 0; i < cuentaSaldos.size(); i++) {
                cuentaSaldos.set(i, saldosAntes.get(i));
            }
            for (int i = 0; i < asientoFechas.size(); i++) {
                asientoFechas.set(i, fechasAntes.get(i));
            }
            for (int i = 0; i < asientoTotales.size(); i++) {
                asientoTotales.set(i, totalesAntes.get(i));
            }
            System.out.println("Recalculo cancelado: no se pudo crear el respaldo.");
            return;
        }

        guardarAsientos();
        guardarCuentas();

        int cuentasCambiadas = contarSaldosDiferentes(saldosAntes, cuentaSaldos);
        System.out.println("Recalculo aplicado y guardado.");
        if (cambiosSaldos) {
            System.out.println("- Cuentas con saldo corregido: " + cuentasCambiadas);
        }
        if (cambiosFechas) {
            System.out.println("- Fechas de asientos normalizadas a dd/MM/yyyy.");
        }
        if (totalesCorregidos > 0) {
            System.out.println("- Totales de asientos corregidos: " + totalesCorregidos);
        }
        System.out.println("Respaldo: " + respaldo);
    }

    static void recargarDatosDesdeArchivos() {
        System.out.println("\n--- RECARGAR DATOS DESDE ARCHIVOS ---");
        System.out.println("Se descartara el estado en memoria y se volvera a leer de disco.");
        String confirmacion = leerString("Confirme la recarga (S/N): ");

        if (!confirmacion.equalsIgnoreCase("S") && !confirmacion.equalsIgnoreCase("SI")) {
            System.out.println("Recarga cancelada. No se modifico ningun dato.");
            return;
        }

        cargarCuentasTexto(ARCHIVO_CUENTAS);
        cargarAsientosTexto(ARCHIVO_ASIENTOS);

        if (cuentaCodigos.isEmpty()) {
            System.out.println("El catalogo estaba vacio. Inicializando 75 cuentas estandar...");
            inicializarCuentas();
        }

        aplicarAjustesAutomaticos();
        actualizarMarcasArchivos();
        System.out.println("Datos recargados desde los archivos.");
    }

    static void restaurarRespaldo() {
        System.out.println("\n--- RESTAURAR RESPALDO ---");
        File dir = new File("backup");
        File[] entradas = dir.listFiles();
        if (entradas == null) {
            System.out.println("No existe la carpeta de respaldos.");
            return;
        }

        List<File> respaldos = new ArrayList<>();
        for (File entrada : entradas) {
            if (!entrada.isDirectory()) continue;
            String nombre = entrada.getName();
            if (nombre.startsWith("diario_") || nombre.startsWith("ajuste_")
                    || nombre.startsWith("recalculo_") || nombre.startsWith("reinicio_")
                    || nombre.startsWith("previo_restauracion_")) {
                respaldos.add(entrada);
            }
        }

        if (respaldos.isEmpty()) {
            System.out.println("No hay respaldos disponibles.");
            return;
        }

        for (int i = 0; i < respaldos.size() - 1; i++) {
            for (int j = i + 1; j < respaldos.size(); j++) {
                long modA = respaldos.get(i).lastModified();
                long modB = respaldos.get(j).lastModified();
                if (modB > modA || (modB == modA
                        && respaldos.get(j).getName().compareTo(respaldos.get(i).getName()) > 0)) {
                    File temporal = respaldos.get(i);
                    respaldos.set(i, respaldos.get(j));
                    respaldos.set(j, temporal);
                }
            }
        }

        SimpleDateFormat formatoFecha = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("Respaldos disponibles (mas recientes primero):");
        for (int i = 0; i < respaldos.size(); i++) {
            File carpeta = respaldos.get(i);
            System.out.printf("%2d. %-42s %s%n", i + 1, carpeta.getName(),
                    formatoFecha.format(new Date(carpeta.lastModified())));
        }

        int seleccion = leerInt("Seleccione el respaldo (0 = cancelar): ");
        if (seleccion == 0) {
            System.out.println("Restauracion cancelada.");
            return;
        }
        if (seleccion < 1 || seleccion > respaldos.size()) {
            System.out.println("Seleccion invalida.");
            return;
        }

        File elegido = respaldos.get(seleccion - 1);
        File archivoCuentas = new File(elegido, "cuentas.txt");
        File archivoAsientos = new File(elegido, "asientos.txt");
        File archivoBalance = new File(elegido, "balance.txt");

        if (!archivoCuentas.exists() && !archivoAsientos.exists()) {
            System.out.println("El respaldo no contiene cuentas.txt ni asientos.txt.");
            System.out.println("No se puede restaurar.");
            return;
        }

        System.out.println("\nRespaldo seleccionado: " + elegido.getName());
        if (!archivoCuentas.exists()) System.out.println("AVISO: no contiene cuentas.txt; se conservara el actual.");
        if (!archivoAsientos.exists()) System.out.println("AVISO: no contiene asientos.txt; se conservara el actual.");
        System.out.println("Se creara un respaldo de seguridad antes de restaurar.");

        String confirmacion = leerString("Escriba RESTAURAR para confirmar: ");
        if (!confirmacion.equalsIgnoreCase("RESTAURAR")) {
            System.out.println("Operacion cancelada. No se modifico ningun dato.");
            return;
        }

        if ((archivoCuentas.exists() && !Files.isReadable(archivoCuentas.toPath()))
                || (archivoAsientos.exists() && !Files.isReadable(archivoAsientos.toPath()))
                || (archivoBalance.exists() && !Files.isReadable(archivoBalance.toPath()))) {
            System.out.println("No se pudo leer el respaldo. No se modifico ningun dato.");
            return;
        }

        String seguridad = respaldarDatos("previo_restauracion");
        if (seguridad == null) {
            System.out.println("Restauracion cancelada: no se pudo crear el respaldo de seguridad.");
            return;
        }

        try {
            if (archivoCuentas.exists()
                    && !copiarArchivoAtomico(archivoCuentas.getPath(), ARCHIVO_CUENTAS)) {
                throw new IOException("No se pudo restaurar " + ARCHIVO_CUENTAS);
            }
            if (archivoAsientos.exists()
                    && !copiarArchivoAtomico(archivoAsientos.getPath(), ARCHIVO_ASIENTOS)) {
                throw new IOException("No se pudo restaurar " + ARCHIVO_ASIENTOS);
            }
            if (archivoBalance.exists()) {
                if (!copiarArchivoAtomico(archivoBalance.getPath(), ARCHIVO_BALANCE)) {
                    throw new IOException("No se pudo restaurar " + ARCHIVO_BALANCE);
                }
            } else {
                Files.deleteIfExists(new File(ARCHIVO_BALANCE).toPath());
            }
        } catch (IOException e) {
            System.out.println("ERROR al restaurar: " + e.getMessage());
            System.out.println("Intentando recuperar el estado anterior desde: " + seguridad);
            try {
                restaurarArchivosDesdeCarpeta(new File(seguridad));
                System.out.println("Estado anterior recuperado.");
            } catch (IOException e2) {
                System.out.println("No se pudo recuperar automaticamente: " + e2.getMessage());
                System.out.println("Restaure manualmente desde: " + seguridad);
            }
            recargarMemoriaDesdeArchivos();
            return;
        }

        recargarMemoriaDesdeArchivos();
        System.out.println("Datos restaurados desde: " + elegido.getPath());
        System.out.println("Respaldo de seguridad: " + seguridad);
    }

    static void recargarMemoriaDesdeArchivos() {
        cargarCuentasTexto(ARCHIVO_CUENTAS);
        cargarAsientosTexto(ARCHIVO_ASIENTOS);

        if (cuentaCodigos.isEmpty()) {
            System.out.println("El catalogo quedo vacio. Inicializando 75 cuentas estandar...");
            inicializarCuentas();
        }

        aplicarAjustesAutomaticos();
        actualizarMarcasArchivos();
    }

    static boolean copiarArchivoAtomico(String origen, String destino) {
        File temporal = new File(destino + ".tmp");
        try {
            Files.copy(new File(origen).toPath(), temporal.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            try {
                Files.move(temporal.toPath(), new File(destino).toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal.toPath(), new File(destino).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            System.out.println("Error copiando " + origen + " a " + destino + ": " + e.getMessage());
            temporal.delete();
            return false;
        }
    }

    static void restaurarArchivosDesdeCarpeta(File carpeta) throws IOException {
        String[] archivos = {ARCHIVO_CUENTAS, ARCHIVO_ASIENTOS, ARCHIVO_BALANCE};
        for (String archivo : archivos) {
            File origen = new File(carpeta, new File(archivo).getName());
            if (!origen.exists()) continue;

            if (!copiarArchivoAtomico(origen.getPath(), archivo)) {
                throw new IOException("No se pudo escribir " + archivo);
            }
        }
    }

    static void mostrarAsientos() {
        System.out.println("\n=== ASIENTOS REGISTRADOS ===");
        for (int i = 0; i < asientoFechas.size(); i++) {
            System.out.println("\nAsiento #" + (i + 1));
            System.out.println("Fecha: " + asientoFechas.get(i));
            System.out.println("Descripcion: " + asientoDescripciones.get(i));

            List<String> cods = partidaCodigos.get(i);
            List<String> tips = partidaTipos.get(i);
            List<Double> mons = partidaMontos.get(i);
            for (int j = 0; j < cods.size(); j++) {
                System.out.printf("  %-6s %-30s %-9s %12.2f%n", cods.get(j), nombreCuenta(cods.get(j)), tips.get(j), mons.get(j));
            }
            System.out.println("  Total: " + asientoTotales.get(i));
        }
    }

    static void verificarSaldosAntesDeReporte() {
        if (recalcularSaldos()) {
            System.out.println("AVISO: Los saldos en memoria no coincidian con los asientos.");
            System.out.println("Se recalcularon para este reporte y se guardaran al salir.");
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

    static boolean fechaEnRango(String fecha, String desde, String hasta) {
        String f = fechaOrdenable(fecha);
        if (!desde.isEmpty() && f.compareTo(fechaOrdenable(desde)) < 0) return false;
        if (!hasta.isEmpty() && f.compareTo(fechaOrdenable(hasta)) > 0) return false;
        return true;
    }

    static boolean fechaEnPeriodo(String fecha) {
        return fechaEnRango(fecha, periodoDesde, periodoHasta);
    }

    static List<Double> saldosEnCero() {
        List<Double> saldos = new ArrayList<>();
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            saldos.add(0.0);
        }
        return saldos;
    }

    static void aplicarPartidaSaldo(List<Double> saldos, String codigo, String tipo, double monto) {
        int j = indiceCuenta(codigo);
        if (j < 0 || j >= saldos.size()) return;

        String tipoCuenta = cuentaTipos.get(j);
        double saldo = saldos.get(j);

        if (tipoCuenta.equals("Activo") || tipoCuenta.equals("Costo") || tipoCuenta.equals("Gasto")) {
            saldo += tipo.equalsIgnoreCase("Debito") ? monto : -monto;
        } else {
            saldo += tipo.equalsIgnoreCase("Credito") ? monto : -monto;
        }
        saldos.set(j, saldo);
    }

    static List<Double> calcularSaldosRango(String desde, String hasta) {
        List<Double> saldos = saldosEnCero();

        for (int i = 0; i < asientoFechas.size(); i++) {
            if (!fechaEnRango(asientoFechas.get(i), desde, hasta)) continue;

            List<String> cods = partidaCodigos.get(i);
            List<String> tips = partidaTipos.get(i);
            List<Double> mons = partidaMontos.get(i);
            int cantidad = Math.min(cods.size(), Math.min(tips.size(), mons.size()));

            for (int j = 0; j < cantidad; j++) {
                aplicarPartidaSaldo(saldos, cods.get(j), tips.get(j), mons.get(j));
            }
        }
        return saldos;
    }

    static List<Double> calcularSaldosHasta(String hasta) {
        return calcularSaldosRango("", hasta);
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
        return totalPorTipo(saldos, "Ingreso") - totalPorTipo(saldos, "Costo") - totalPorTipo(saldos, "Gasto");
    }

    static double saldoCuentaAntes(String codigo, String fechaLimite) {
        if (fechaLimite == null || fechaLimite.isEmpty()) return 0;

        int idxCuenta = indiceCuenta(codigo);
        if (idxCuenta < 0) return 0;

        String limite = fechaOrdenable(fechaLimite);
        double saldo = 0;
        String tipoCuenta = cuentaTipos.get(idxCuenta);

        for (int i = 0; i < asientoFechas.size(); i++) {
            if (fechaOrdenable(asientoFechas.get(i)).compareTo(limite) >= 0) continue;

            List<String> cods = partidaCodigos.get(i);
            List<String> tips = partidaTipos.get(i);
            List<Double> mons = partidaMontos.get(i);
            int cantidad = Math.min(cods.size(), Math.min(tips.size(), mons.size()));

            for (int j = 0; j < cantidad; j++) {
                if (!cods.get(j).equals(codigo)) continue;
                double monto = mons.get(j);
                if (tipoCuenta.equals("Activo") || tipoCuenta.equals("Costo") || tipoCuenta.equals("Gasto")) {
                    saldo += tips.get(j).equalsIgnoreCase("Debito") ? monto : -monto;
                } else {
                    saldo += tips.get(j).equalsIgnoreCase("Credito") ? monto : -monto;
                }
            }
        }
        return saldo;
    }

    static String descripcionPeriodo(boolean soloCorte) {
        String desde = soloCorte ? "" : periodoDesde;
        String hasta = periodoHasta;

        if (desde.isEmpty() && hasta.isEmpty()) return "Todo el historico";
        if (desde.isEmpty()) return "Hasta " + hasta;
        if (hasta.isEmpty()) return "Desde " + desde;
        return "Del " + desde + " al " + hasta;
    }

    static void configurarPeriodoReportes() {
        System.out.println("\n--- PERIODO DE REPORTES ---");
        System.out.println("Periodo actual: " + descripcionPeriodo(false));
        System.out.println("Deje vacio para no limitar.");
        System.out.println("El balance general usa solo la fecha de corte (hasta).");

        String desde = leerFechaOpcional("Desde");
        String hasta = leerFechaOpcional("Hasta");

        if (!desde.isEmpty() && !hasta.isEmpty()
                && fechaOrdenable(desde).compareTo(fechaOrdenable(hasta)) > 0) {
            System.out.println("ERROR: La fecha desde no puede ser posterior a la fecha hasta.");
            System.out.println("El periodo no se modifico.");
            return;
        }

        periodoDesde = desde;
        periodoHasta = hasta;
        System.out.println("Periodo actualizado: " + descripcionPeriodo(false));
        System.out.println("Los reportes 5, 7, 8, 9, 10, 11 y 12 usaran este periodo.");
    }

    static void generarBalance() {
        verificarSaldosAntesDeReporte();
        List<Double> saldos = calcularSaldosHasta(periodoHasta);

        System.out.println("\n=== BALANCE GENERAL ===");
        System.out.println("Periodo: " + descripcionPeriodo(true));
        System.out.printf("%-8s %-35s %12s%n", "Codigo", "Nombre", "Saldo");
        System.out.println("-".repeat(60));

        double totalActivo = 0;
        double totalPasivo = 0;
        double totalPatrimonio = 0;

        System.out.println("-- ACTIVO --");
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            if (cuentaTipos.get(i).equals("Activo")) {
                System.out.printf("%-8s %-35s %12.2f%n", cuentaCodigos.get(i), cuentaNombres.get(i), saldos.get(i));
                totalActivo += saldos.get(i);
            }
        }
        System.out.printf("%-44s %12.2f%n", "TOTAL ACTIVO:", totalActivo);

        System.out.println("\n-- PASIVO --");
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            if (cuentaTipos.get(i).equals("Pasivo")) {
                System.out.printf("%-8s %-35s %12.2f%n", cuentaCodigos.get(i), cuentaNombres.get(i), saldos.get(i));
                totalPasivo += saldos.get(i);
            }
        }
        System.out.printf("%-44s %12.2f%n", "TOTAL PASIVO:", totalPasivo);

        System.out.println("\n-- PATRIMONIO --");
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            if (cuentaTipos.get(i).equals("Patrimonio")) {
                System.out.printf("%-8s %-35s %12.2f%n", cuentaCodigos.get(i), cuentaNombres.get(i), saldos.get(i));
                totalPatrimonio += saldos.get(i);
            }
        }
        System.out.printf("%-44s %12.2f%n", "TOTAL PATRIMONIO:", totalPatrimonio);

        double resultado = resultadoEjercicio(saldos);
        double patrimonioConResultado = totalPatrimonio + resultado;

        System.out.println("\n-- RESULTADO DEL EJERCICIO --");
        System.out.printf("%-44s %12.2f%n", "Resultado del ejercicio:", resultado);

        System.out.println("\n" + "-".repeat(60));
        System.out.printf("Total Pasivo + Patrimonio + Resultado: %.2f%n", totalPasivo + patrimonioConResultado);
        System.out.printf("Diferencia: %.2f%n", totalActivo - (totalPasivo + patrimonioConResultado));

        if (Math.abs(totalActivo - (totalPasivo + patrimonioConResultado)) < 0.01) {
            System.out.println("EL BALANCE CUADRADO CORRECTAMENTE.");
        } else {
            System.out.println("ADVERTENCIA: El balance NO cuadra.");
        }

        String fechaCorte = periodoHasta.isEmpty() ? fechaActual() : periodoHasta;
        guardarBalance(fechaCorte, totalActivo, totalPasivo, patrimonioConResultado);
    }

    static double totalPorTipo(String tipo) {
        return totalPorTipo(cuentaSaldos, tipo);
    }

    static double calcularResultadoEjercicio() {
        return resultadoEjercicio(cuentaSaldos);
    }

    static void guardarBalance(String fecha, double activo, double pasivo, double patrimonio) {
        String contenido = fecha + "|" + activo + "|" + pasivo + "|" + patrimonio
                + System.lineSeparator();
        escribirArchivoAtomico(ARCHIVO_BALANCE, contenido);
    }

    static void verEstadoResultados() {
        verificarSaldosAntesDeReporte();
        List<Double> saldos = calcularSaldosRango(periodoDesde, periodoHasta);

        System.out.println("\n=== ESTADO DE RESULTADOS ===");
        System.out.println("Periodo: " + descripcionPeriodo(false));
        System.out.printf("%-8s %-35s %12s%n", "Codigo", "Nombre", "Saldo");
        System.out.println("-".repeat(60));

        double totalIngresos = 0;
        double totalCostos = 0;
        double totalGastos = 0;

        System.out.println("-- INGRESOS --");
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            if (cuentaTipos.get(i).equals("Ingreso")) {
                System.out.printf("%-8s %-35s %12.2f%n", cuentaCodigos.get(i), cuentaNombres.get(i), saldos.get(i));
                totalIngresos += saldos.get(i);
            }
        }
        System.out.printf("%-44s %12.2f%n", "TOTAL INGRESOS:", totalIngresos);

        System.out.println("\n-- COSTOS --");
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            if (cuentaTipos.get(i).equals("Costo")) {
                System.out.printf("%-8s %-35s %12.2f%n", cuentaCodigos.get(i), cuentaNombres.get(i), saldos.get(i));
                totalCostos += saldos.get(i);
            }
        }
        System.out.printf("%-44s %12.2f%n", "TOTAL COSTOS:", totalCostos);

        System.out.println("\n-- GASTOS --");
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            if (cuentaTipos.get(i).equals("Gasto")) {
                System.out.printf("%-8s %-35s %12.2f%n", cuentaCodigos.get(i), cuentaNombres.get(i), saldos.get(i));
                totalGastos += saldos.get(i);
            }
        }
        System.out.printf("%-44s %12.2f%n", "TOTAL GASTOS:", totalGastos);

        double utilidadBruta = totalIngresos - totalCostos;
        double utilidadNeta = utilidadBruta - totalGastos;

        System.out.println("\n" + "-".repeat(60));
        System.out.printf("Utilidad bruta:  %.2f%n", utilidadBruta);
        System.out.printf("Utilidad neta:   %.2f%n", utilidadNeta);
    }

    static void buscarCuenta() {
        String termino = leerString("Ingrese codigo o nombre a buscar: ");
        System.out.println("\n=== RESULTADOS ===");
        List<String> encontradas = buscarCuentas(termino);

        if (encontradas.isEmpty()) {
            System.out.println("No se encontraron resultados.");
            return;
        }

        for (int k = 0; k < encontradas.size(); k++) {
            int i = indiceCuenta(encontradas.get(k));
            System.out.printf("%-8s %-35s %-15s %12.2f%n", cuentaCodigos.get(i), cuentaNombres.get(i), cuentaTipos.get(i), cuentaSaldos.get(i));
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

    static String resumenCuentas(int indiceAsiento) {
        StringBuilder resumen = new StringBuilder();
        List<String> cods = partidaCodigos.get(indiceAsiento);
        for (int j = 0; j < cods.size(); j++) {
            if (j > 0) resumen.append(", ");
            resumen.append(cods.get(j)).append(" ").append(nombreCuenta(cods.get(j)));
        }
        return resumen.toString();
    }

    static List<String> buscarCuentas(String termino) {
        List<String> encontradas = new ArrayList<>();
        String busqueda = normalizarTexto(termino);

        for (int i = 0; i < cuentaCodigos.size(); i++) {
            if (normalizarTexto(cuentaCodigos.get(i)).contains(busqueda) ||
                normalizarTexto(cuentaNombres.get(i)).contains(busqueda)) {
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
        System.out.printf("  -> %s - %s (%s)%n",
                cuentaCodigos.get(indice),
                cuentaNombres.get(indice),
                cuentaTipos.get(indice));
    }

    static String pedirCuenta() {
        while (true) {
            String termino = leerString("  Cuenta (codigo o nombre, ? = catalogo, 0 = cancelar): ");

            if (termino.equals("0")) {
                return null;
            }

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
                        i + 1,
                        cuentaCodigos.get(indice),
                        cuentaNombres.get(indice),
                        cuentaTipos.get(indice));
            }
            System.out.println("   0) Buscar de nuevo");

            while (true) {
                int seleccion = leerInt("  Seleccione una opcion: ");
                if (seleccion == 0) break;
                if (seleccion >= 1 && seleccion <= coincidencias.size()) {
                    int indice = indiceCuenta(coincidencias.get(seleccion - 1));
                    mostrarCuentaEncontrada(indice);
                    return coincidencias.get(seleccion - 1);
                }
                System.out.println("  Opcion invalida. Intente de nuevo.");
            }
        }
    }

    static String leerString(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            String entrada = scanner.nextLine().trim();
            if (!entrada.isEmpty()) {
                return entrada;
            }
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
                    System.out.println("Monto invalido. Use coma o punto decimal (ej: 1800,50).");
                }
            }
        }
    }

    static double parsearMonto(String entrada) {
        String valor = entrada.trim().replace(" ", "");
        int ultimaComa = valor.lastIndexOf(',');
        int ultimoPunto = valor.lastIndexOf('.');

        if (ultimaComa >= 0 && ultimoPunto >= 0) {
            if (ultimaComa > ultimoPunto) {
                valor = valor.replace(".", "").replace(',', '.');
            } else {
                valor = valor.replace(",", "");
            }
        } else if (ultimaComa >= 0) {
            valor = valor.replace(',', '.');
        } else if (ultimoPunto >= 0) {
            int primerPunto = valor.indexOf('.');
            int decimales = valor.length() - ultimoPunto - 1;

            if (primerPunto == ultimoPunto && decimales == 3) {
                throw new NumberFormatException("monto ambiguo");
            }
            if (primerPunto != ultimoPunto) {
                valor = valor.replace(".", "");
            }
        }

        return Double.parseDouble(valor);
    }

    static void verEstadoFlujoEfectivo() {
        System.out.println("\n=== ESTADO DE FLUJO DE EFECTIVO ===");
        System.out.println("Periodo: " + descripcionPeriodo(false));
        System.out.println("(Metodo directo - basado en movimientos de Caja y Bancos)");
        System.out.println();

        double flujoOperacion = 0;
        double flujoInversion = 0;
        double flujoFinanciamiento = 0;

        for (int i = 0; i < asientoFechas.size(); i++) {
            if (!fechaEnPeriodo(asientoFechas.get(i))) continue;

            List<String> cods = partidaCodigos.get(i);
            List<String> tips = partidaTipos.get(i);
            List<Double> mons = partidaMontos.get(i);
            for (int j = 0; j < cods.size(); j++) {
                String codigo = cods.get(j);
                String tipo = tips.get(j);
                double monto = mons.get(j);

                if (codigo.equals("1000") || codigo.equals("1100")) {
                    if (tipo.equalsIgnoreCase("Debito")) {
                        flujoOperacion += monto;
                    } else {
                        flujoOperacion -= monto;
                    }
                } else if (codigo.startsWith("1") && !codigo.equals("1000") && !codigo.equals("1100")) {
                    if (tipo.equalsIgnoreCase("Debito")) {
                        flujoInversion -= monto;
                    } else {
                        flujoInversion += monto;
                    }
                } else if (codigo.startsWith("2")) {
                    if (tipo.equalsIgnoreCase("Credito")) {
                        flujoFinanciamiento += monto;
                    } else {
                        flujoFinanciamiento -= monto;
                    }
                }
            }
        }

        System.out.println("FLUJO DE EFECTIVO POR ACTIVIDAD");
        System.out.println("-".repeat(50));
        System.out.printf("%-40s %12s%n", "Actividades de operacion:", flujoOperacion);
        System.out.printf("%-40s %12s%n", "Actividades de inversion:", flujoInversion);
        System.out.printf("%-40s %12s%n", "Actividades de financiamiento:", flujoFinanciamiento);
        System.out.println("-".repeat(50));
        System.out.printf("%-40s %12s%n", "Flujo neto total:", flujoOperacion + flujoInversion + flujoFinanciamiento);
    }

    static void verEstadoCambiosPatrimonio() {
        verificarSaldosAntesDeReporte();
        List<Double> saldos = calcularSaldosRango(periodoDesde, periodoHasta);

        System.out.println("\n=== ESTADO DE CAMBIOS EN EL PATRIMONIO ===");
        System.out.println("Periodo: " + descripcionPeriodo(false));
        System.out.println();

        double capitalSocial = 0;
        double capitalAportado = 0;
        double capitalGanado = 0;
        double utilidadesRetenidas = 0;
        double utilidadEjercicio = 0;
        double perdidaEjercicio = 0;
        double resultadosEjercicio = 0;
        double reservasLegales = 0;
        double ajustesRevaluacion = 0;
        double dividendosDecretados = 0;

        for (int i = 0; i < cuentaCodigos.size(); i++) {
            String codigo = cuentaCodigos.get(i);
            double saldo = saldos.get(i);

            switch (codigo) {
                case "3000": capitalSocial = saldo; break;
                case "3010": capitalAportado = saldo; break;
                case "3020": capitalGanado = saldo; break;
                case "3100": utilidadesRetenidas = saldo; break;
                case "3110": utilidadEjercicio = saldo; break;
                case "3120": perdidaEjercicio = saldo; break;
                case "3200": resultadosEjercicio = saldo; break;
                case "3300": reservasLegales = saldo; break;
                case "3400": ajustesRevaluacion = saldo; break;
                case "3500": dividendosDecretados = saldo; break;
            }
        }

        double resultadoEjercicio = resultadoEjercicio(saldos);
        double patrimonioTotal = capitalSocial + capitalAportado + capitalGanado +
                                 utilidadesRetenidas + utilidadEjercicio - perdidaEjercicio +
                                 resultadosEjercicio + reservasLegales + ajustesRevaluacion -
                                 dividendosDecretados + resultadoEjercicio;

        System.out.println("MOVIMIENTOS DEL PATRIMONIO EN EL PERIODO");
        System.out.println("-".repeat(60));
        System.out.printf("%-40s %12s%n", "Capital social:", capitalSocial);
        System.out.printf("%-40s %12s%n", "Capital aportado:", capitalAportado);
        System.out.printf("%-40s %12s%n", "Capital ganado:", capitalGanado);
        System.out.printf("%-40s %12s%n", "Utilidades retenidas:", utilidadesRetenidas);
        System.out.printf("%-40s %12s%n", "Utilidad del ejercicio:", utilidadEjercicio);
        System.out.printf("%-40s %12s%n", "Perdida del ejercicio:", perdidaEjercicio);
        System.out.printf("%-40s %12s%n", "Resultados del ejercicio:", resultadosEjercicio);
        System.out.printf("%-40s %12s%n", "Resultado del ejercicio:", resultadoEjercicio);
        System.out.printf("%-40s %12s%n", "Reservas legales:", reservasLegales);
        System.out.printf("%-40s %12s%n", "Ajustes por revaluacion:", ajustesRevaluacion);
        System.out.printf("%-40s %12s%n", "Dividendos decretados:", dividendosDecretados);
        System.out.println("-".repeat(60));
        System.out.printf("%-40s %12s%n", "PATRIMONIO TOTAL:", patrimonioTotal);
    }

    static void verBalanceComprobacion() {
        verificarSaldosAntesDeReporte();
        List<Double> saldosPeriodo = calcularSaldosRango(periodoDesde, periodoHasta);

        System.out.println("\n=== BALANCE DE COMPROBACION ===");
        System.out.println("Periodo: " + descripcionPeriodo(false));
        System.out.println("(Verificacion: Debitos = Creditos por cuenta)");
        System.out.println();
        System.out.printf("%-8s %-35s %12s %12s %12s%n", "Codigo", "Nombre", "Debitos", "Creditos", "Saldo");
        System.out.println("-".repeat(85));

        double totalDebitos = 0;
        double totalCreditos = 0;

        for (int i = 0; i < cuentaCodigos.size(); i++) {
            String codigo = cuentaCodigos.get(i);
            String nombre = cuentaNombres.get(i);
            double saldo = saldosPeriodo.get(i);

            double debitos = 0;
            double creditos = 0;

            for (int a = 0; a < asientoFechas.size(); a++) {
                if (!fechaEnPeriodo(asientoFechas.get(a))) continue;

                List<String> cods = partidaCodigos.get(a);
                List<String> tips = partidaTipos.get(a);
                List<Double> mons = partidaMontos.get(a);
                for (int p = 0; p < cods.size(); p++) {
                    if (cods.get(p).equals(codigo)) {
                        if (tips.get(p).equalsIgnoreCase("Debito")) {
                            debitos += mons.get(p);
                        } else {
                            creditos += mons.get(p);
                        }
                    }
                }
            }

            if (debitos > 0 || creditos > 0) {
                System.out.printf("%-8s %-35s %12.2f %12.2f %12.2f%n", codigo, nombre, debitos, creditos, saldo);
                totalDebitos += debitos;
                totalCreditos += creditos;
            }
        }

        System.out.println("-".repeat(85));
        System.out.printf("%-44s %12.2f %12.2f%n", "TOTALES:", totalDebitos, totalCreditos);
        System.out.println();

        if (Math.abs(totalDebitos - totalCreditos) < 0.01) {
            System.out.println("EL BALANCE DE COMPROBACION ESTA CUADRADO.");
        } else {
            System.out.println("ADVERTENCIA: El balance de comprobacion NO cuadra.");
            System.out.printf("Diferencia: %.2f%n", totalDebitos - totalCreditos);
        }
    }

    static void verLibroDiario() {
        System.out.println("\n=== LIBRO DIARIO ===");
        System.out.println("Periodo: " + descripcionPeriodo(false));
        System.out.println("(Ordenado por fecha - No. = numero de asiento original)");
        System.out.println();

        if (asientoFechas.isEmpty()) {
            System.out.println("No hay asientos registrados.");
            return;
        }

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < asientoFechas.size(); i++) {
            if (fechaEnPeriodo(asientoFechas.get(i))) {
                indices.add(i);
            }
        }

        if (indices.isEmpty()) {
            System.out.println("No hay asientos en el periodo seleccionado.");
            return;
        }

        ordenarIndicesPorFecha(indices);

        System.out.printf("%-5s %-20s %-30s %12s%n", "No.", "Fecha", "Descripcion", "Total");
        System.out.println("-".repeat(75));

        for (int k = 0; k < indices.size(); k++) {
            int i = indices.get(k);
            System.out.printf("%-5d %-20s %-30s %12.2f%n", i + 1, asientoFechas.get(i), asientoDescripciones.get(i), asientoTotales.get(i));
        }

        System.out.println("-".repeat(75));
        System.out.printf("Total de asientos: %d%n", indices.size());
    }

    static void verLibroMayor() {
        System.out.println("\n=== LIBRO MAYOR ===");
        System.out.println("Periodo: " + descripcionPeriodo(false));
        System.out.println("(Movimientos por cuenta individual)");
        System.out.println();

        String codigo = pedirCuenta();
        if (codigo == null) {
            System.out.println("Operacion cancelada.");
            return;
        }

        int idxCuenta = indiceCuenta(codigo);
        double saldoInicial = saldoCuentaAntes(codigo, periodoDesde);

        System.out.println();
        System.out.println("CUENTA: " + cuentaCodigos.get(idxCuenta) + " - " + cuentaNombres.get(idxCuenta));
        System.out.println("Tipo: " + cuentaTipos.get(idxCuenta));
        if (!periodoDesde.isEmpty()) {
            System.out.printf("Saldo inicial al %s: %.2f%n", periodoDesde, saldoInicial);
        }
        System.out.println();

        List<Integer> asientosMov = new ArrayList<>();
        List<Integer> partidasMov = new ArrayList<>();

        for (int i = 0; i < asientoFechas.size(); i++) {
            if (!fechaEnPeriodo(asientoFechas.get(i))) continue;

            List<String> cods = partidaCodigos.get(i);
            for (int j = 0; j < cods.size(); j++) {
                if (cods.get(j).equals(codigo)) {
                    asientosMov.add(i);
                    partidasMov.add(j);
                }
            }
        }

        for (int i = 0; i < asientosMov.size() - 1; i++) {
            for (int j = 0; j < asientosMov.size() - 1 - i; j++) {
                String fechaA = fechaOrdenable(asientoFechas.get(asientosMov.get(j)));
                String fechaB = fechaOrdenable(asientoFechas.get(asientosMov.get(j + 1)));
                if (fechaB.compareTo(fechaA) < 0) {
                    int tempA = asientosMov.get(j);
                    asientosMov.set(j, asientosMov.get(j + 1));
                    asientosMov.set(j + 1, tempA);
                    int tempP = partidasMov.get(j);
                    partidasMov.set(j, partidasMov.get(j + 1));
                    partidasMov.set(j + 1, tempP);
                }
            }
        }

        System.out.printf("%-5s %-20s %-10s %12s %12s%n", "As.", "Fecha", "Tipo", "Monto", "Saldo");
        System.out.println("-".repeat(65));

        double saldo = saldoInicial;

        for (int k = 0; k < asientosMov.size(); k++) {
            int i = asientosMov.get(k);
            int j = partidasMov.get(k);
            String tipo = partidaTipos.get(i).get(j);
            double monto = partidaMontos.get(i).get(j);

            String tipoCuenta = cuentaTipos.get(idxCuenta);
            if (tipoCuenta.equals("Activo") || tipoCuenta.equals("Costo") || tipoCuenta.equals("Gasto")) {
                saldo += tipo.equalsIgnoreCase("Debito") ? monto : -monto;
            } else {
                saldo += tipo.equalsIgnoreCase("Credito") ? monto : -monto;
            }

            System.out.printf("%-5d %-20s %-10s %12.2f %12.2f%n", i + 1, asientoFechas.get(i), tipo, monto, saldo);
        }

        System.out.println("-".repeat(65));
        System.out.printf("Saldo final: %.2f%n", saldo);
    }

    static void revertirAsiento() {
        System.out.println("\n--- REVERTIR ASIENTO ---");
        System.out.println("(Se crea un asiento de reversa - no se borra el original)");
        System.out.println();

        if (asientoFechas.isEmpty()) {
            System.out.println("No hay asientos para revertir.");
            return;
        }

        for (int i = 0; i < asientoFechas.size(); i++) {
            System.out.printf("%d. %s - %s (Total: %.2f)%n", i + 1, asientoFechas.get(i), asientoDescripciones.get(i), asientoTotales.get(i));
            System.out.println("   Cuentas: " + resumenCuentas(i));
        }

        int seleccion = leerInt("Seleccione el numero de asiento a revertir (1-" + asientoFechas.size() + "): ") - 1;

        if (seleccion < 0 || seleccion >= asientoFechas.size()) {
            System.out.println("Seleccion invalida. Debe estar entre 1 y " + asientoFechas.size() + ".");
            return;
        }

        List<String> codsOrig = partidaCodigos.get(seleccion);
        List<String> tipsOrig = partidaTipos.get(seleccion);
        List<Double> monsOrig = partidaMontos.get(seleccion);

        List<String> codsRev = new ArrayList<>(codsOrig);
        List<String> tipsRev = new ArrayList<>();
        List<Double> monsRev = new ArrayList<>(monsOrig);

        for (String t : tipsOrig) {
            tipsRev.add(t.equalsIgnoreCase("Debito") ? "Credito" : "Debito");
        }

        asientoFechas.add(fechaActual());
        asientoDescripciones.add("REVERSO: " + asientoDescripciones.get(seleccion));
        asientoTotales.add(asientoTotales.get(seleccion));
        partidaCodigos.add(codsRev);
        partidaTipos.add(tipsRev);
        partidaMontos.add(monsRev);

        actualizarSaldos(codsRev, tipsRev, monsRev);
        guardarAsientos();
        guardarCuentas();

        System.out.println("Asiento revertido exitosamente.");
        System.out.println("Se ha creado un asiento de reversa que anula el original.");
    }

    static void reiniciarDatos() {
        System.out.println("\n--- REINICIAR DATOS DE PRUEBA (BETA) ---");
        System.out.println("ADVERTENCIA: Se borraran TODOS los asientos registrados.");
        System.out.println("Tambien se pondran en cero los saldos y se restauraran las 75 cuentas estandar.");
        System.out.println("Asientos actuales: " + asientoFechas.size());
        System.out.println("Cuentas actuales: " + cuentaCodigos.size());
        System.out.println("Se creara un respaldo automatico antes de borrar.");
        String confirmacion = leerString("Escriba REINICIAR para confirmar: ");

        if (!confirmacion.equalsIgnoreCase("REINICIAR")) {
            System.out.println("Operacion cancelada. No se modifico ningun dato.");
            return;
        }

        String respaldo = respaldarDatos("reinicio");
        if (respaldo == null) {
            System.out.println("Reinicio cancelado: no se pudo crear el respaldo.");
            return;
        }

        asientoFechas.clear();
        asientoDescripciones.clear();
        asientoTotales.clear();
        partidaCodigos.clear();
        partidaTipos.clear();
        partidaMontos.clear();

        cuentaCodigos.clear();
        cuentaNombres.clear();
        cuentaTipos.clear();
        cuentaSaldos.clear();
        inicializarCuentas();

        guardarAsientos();
        guardarCuentas();

        try {
            Files.deleteIfExists(new File(ARCHIVO_BALANCE).toPath());
        } catch (IOException e) {
            System.out.println("No se pudo eliminar " + ARCHIVO_BALANCE + ": " + e.getMessage());
        }

        System.out.println("Datos reiniciados exitosamente.");
        System.out.println("Respaldo guardado en: " + respaldo);
        System.out.println("Catalogo restaurado con " + cuentaCodigos.size() + " cuentas.");
    }

    static String respaldarDatos(String etiqueta) {
        String marca = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        String base = "backup/" + etiqueta + "_" + marca;
        File carpeta = new File(base);
        int intento = 1;

        while (carpeta.exists()) {
            carpeta = new File(base + "_" + intento);
            intento++;
        }

        try {
            if (!carpeta.mkdirs()) {
                System.out.println("No se pudo crear la carpeta de respaldo: " + carpeta.getPath());
                return null;
            }

            copiarArchivosDatos(carpeta);
            return carpeta.getPath();
        } catch (IOException e) {
            System.out.println("Error creando el respaldo: " + e.getMessage());
            borrarCarpeta(carpeta);
            return null;
        }
    }

    static void copiarArchivosDatos(File carpeta) throws IOException {
        String[] archivos = {ARCHIVO_CUENTAS, ARCHIVO_ASIENTOS, ARCHIVO_BALANCE};
        for (String archivo : archivos) {
            File origen = new File(archivo);
            if (origen.exists()) {
                Files.copy(origen.toPath(),
                        new File(carpeta, origen.getName()).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    static void respaldarDiario() {
        if (!new File(ARCHIVO_CUENTAS).exists() && !new File(ARCHIVO_ASIENTOS).exists()) {
            return;
        }

        String marca = new SimpleDateFormat("yyyyMMdd").format(new Date());
        File carpeta = new File("backup/diario_" + marca);
        if (carpeta.exists()) {
            return;
        }

        try {
            if (!carpeta.mkdirs()) {
                System.out.println("ADVERTENCIA: No se pudo crear el respaldo diario.");
                return;
            }

            copiarArchivosDatos(carpeta);
            System.out.println("Respaldo diario creado: " + carpeta.getPath());
            podarRespaldosDiarios(10);
        } catch (IOException e) {
            System.out.println("ADVERTENCIA: No se pudo crear el respaldo diario: " + e.getMessage());
            borrarCarpeta(carpeta);
        }
    }

    static void podarRespaldosDiarios(int maximo) {
        File dir = new File("backup");
        File[] entradas = dir.listFiles();
        if (entradas == null) return;

        List<File> diarios = new ArrayList<>();
        for (File entrada : entradas) {
            if (entrada.isDirectory() && entrada.getName().startsWith("diario_")) {
                diarios.add(entrada);
            }
        }

        for (int i = 0; i < diarios.size() - 1; i++) {
            for (int j = i + 1; j < diarios.size(); j++) {
                if (diarios.get(j).getName().compareTo(diarios.get(i).getName()) < 0) {
                    File temporal = diarios.get(i);
                    diarios.set(i, diarios.get(j));
                    diarios.set(j, temporal);
                }
            }
        }

        for (int i = 0; i < diarios.size() - maximo; i++) {
            System.out.println("Eliminando respaldo diario antiguo: " + diarios.get(i).getName());
            borrarCarpeta(diarios.get(i));
        }
    }

    static void borrarCarpeta(File carpeta) {
        File[] hijos = carpeta.listFiles();
        if (hijos != null) {
            for (File hijo : hijos) {
                borrarCarpeta(hijo);
            }
        }
        carpeta.delete();
    }

    static String leerFecha(String mensaje) {
        while (true) {
            System.out.print(mensaje + " (dd/MM/yyyy, Enter = hoy): ");
            String entrada = scanner.nextLine().trim();
            if (entrada.isEmpty()) {
                return fechaActual();
            }

            String fecha = convertirFecha(entrada);
            if (fecha != null) return fecha;
        }
    }

    static String leerFechaOpcional(String mensaje) {
        while (true) {
            System.out.print(mensaje + " (dd/MM/yyyy, Enter = sin limite): ");
            String entrada = scanner.nextLine().trim();
            if (entrada.isEmpty()) {
                return "";
            }

            String fecha = convertirFecha(entrada);
            if (fecha != null) return fecha;
        }
    }

    static String convertirFecha(String entrada) {
        String[] partes = entrada.split("/");
        if (partes.length != 3) {
            System.out.println("Formato invalido. Use dd/MM/yyyy.");
            return null;
        }

        try {
            int dia = Integer.parseInt(partes[0]);
            int mes = Integer.parseInt(partes[1]);
            int anio = Integer.parseInt(partes[2]);

            if (dia < 1 || dia > 31) {
                System.out.println("Dia invalido. Debe estar entre 1 y 31.");
                return null;
            }
            if (mes < 1 || mes > 12) {
                System.out.println("Mes invalido. Debe estar entre 1 y 12.");
                return null;
            }
            if (anio < 1900 || anio > 2100) {
                System.out.println("Ano invalido. Debe estar entre 1900 y 2100.");
                return null;
            }

            LocalDate.of(anio, mes, dia);
            return String.format("%02d/%02d/%04d", dia, mes, anio);
        } catch (NumberFormatException e) {
            System.out.println("Ingrese una fecha valida en formato dd/MM/yyyy.");
        } catch (DateTimeException e) {
            System.out.println("La fecha no existe en el calendario. Verifique el dia y el mes.");
        }
        return null;
    }

    static void buscarAsientos() {
        System.out.println("\n--- BUSCAR ASIENTOS ---");
        System.out.println("1. Por fecha");
        System.out.println("2. Por descripcion");
        System.out.println("3. Por cuenta");
        System.out.println("4. Por rango de fechas");

        int opcion = leerInt("Seleccione opcion de busqueda: ");

        List<Integer> indicesResultados = new ArrayList<>();

        switch (opcion) {
            case 1:
                String fecha = leerFecha("Ingrese la fecha a buscar");
                for (int i = 0; i < asientoFechas.size(); i++) {
                    if (asientoFechas.get(i).equals(fecha)) {
                        indicesResultados.add(i);
                    }
                }
                break;
            case 2:
                String termino = normalizarTexto(leerString("Ingrese el termino a buscar"));
                for (int i = 0; i < asientoDescripciones.size(); i++) {
                    if (normalizarTexto(asientoDescripciones.get(i)).contains(termino)) {
                        indicesResultados.add(i);
                    }
                }
                break;
            case 3:
                String codigo = pedirCuenta();
                if (codigo == null) {
                    System.out.println("Busqueda cancelada.");
                    return;
                }
                for (int i = 0; i < asientoFechas.size(); i++) {
                    List<String> cods = partidaCodigos.get(i);
                    for (String pCod : cods) {
                        if (pCod.equals(codigo)) {
                            indicesResultados.add(i);
                            break;
                        }
                    }
                }
                break;
            case 4:
                String desde = leerFechaOpcional("Desde");
                String hasta = leerFechaOpcional("Hasta");
                if (!desde.isEmpty() && !hasta.isEmpty()
                        && fechaOrdenable(desde).compareTo(fechaOrdenable(hasta)) > 0) {
                    System.out.println("ERROR: La fecha desde no puede ser posterior a la fecha hasta.");
                    return;
                }
                for (int i = 0; i < asientoFechas.size(); i++) {
                    if (fechaEnRango(asientoFechas.get(i), desde, hasta)) {
                        indicesResultados.add(i);
                    }
                }
                break;
            default:
                System.out.println("Opcion invalida.");
                return;
        }

        System.out.println("\n=== RESULTADOS DE BUSQUEDA ===");
        if (indicesResultados.isEmpty()) {
            System.out.println("No se encontraron asientos.");
            return;
        }

        ordenarIndicesPorFecha(indicesResultados);

        for (int k = 0; k < indicesResultados.size(); k++) {
            int i = indicesResultados.get(k);
            System.out.println("\nResultado #" + (k + 1) + " (Asiento #" + (i + 1) + ")");
            System.out.println("Fecha: " + asientoFechas.get(i));
            System.out.println("Descripcion: " + asientoDescripciones.get(i));

            List<String> cods = partidaCodigos.get(i);
            List<String> tips = partidaTipos.get(i);
            List<Double> mons = partidaMontos.get(i);
            for (int j = 0; j < cods.size(); j++) {
                System.out.printf("  %-6s %-30s %-9s %12.2f%n", cods.get(j), nombreCuenta(cods.get(j)), tips.get(j), mons.get(j));
            }
            System.out.println("  Total: " + asientoTotales.get(i));
        }
    }

    static void salir() {
        System.out.println("\nGuardando datos y saliendo...");
        guardarAsientos();
        guardarCuentas();
        System.out.println("Hasta luego.");
    }
}
