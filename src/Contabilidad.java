import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;

public class Contabilidad {

    static final String ARCHIVO_CUENTAS = "data/cuentas.json";
    static final String ARCHIVO_ASIENTOS = "data/asientos.json";
    static final String ARCHIVO_BALANCE = "data/balance.json";

    static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    static final Scanner scanner = new Scanner(System.in);

    // Estructura: cada cuenta es Object[] {codigo, nombre, tipo, saldo}
    static List<Object[]> cuentas = new ArrayList<>();
    // Estructura: cada asiento es Object[] {fecha, descripcion, total, partidas}
    // donde partidas es List<Object[]> {codigo, tipo, monto}
    static List<Object[]> asientos = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("=== SISTEMA DE CONTABILIDAD ===");
        cargarDatos();

        while (true) {
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
                case 14: reiniciarCatalogo(); break;
                case 15: buscarAsientos(); break;
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
        System.out.println("14. Reiniciar catalogo de cuentas");
        System.out.println("15. Buscar asientos");
        System.out.println("0. Salir");
    }

    static void cargarDatos() {
        File dir = new File("data");
        if (!dir.exists()) dir.mkdirs();

        cuentas = leerCuentasJSON(ARCHIVO_CUENTAS);
        asientos = leerAsientosJSON(ARCHIVO_ASIENTOS);

        if (cuentas.isEmpty()) {
            System.out.println("Inicializando catalogo de cuentas...");
            inicializarCuentas();
        } else if (cuentas.size() < 75) {
            System.out.println("NOTA: Hay " + (75 - cuentas.size()) + " cuentas nuevas disponibles.");
            System.out.println("Use la opcion 14 para reiniciar el catalogo.");
        }
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
        Object[] cuenta = new Object[]{codigo, nombre, tipo, 0.0};
        cuentas.add(cuenta);
    }

    static void guardarCuentas() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        for (int i = 0; i < cuentas.size(); i++) {
            Object[] c = cuentas.get(i);
            json.append("  {\n");
            json.append("    \"codigo\": \"").append(c[0]).append("\",\n");
            json.append("    \"nombre\": \"").append(c[1]).append("\",\n");
            json.append("    \"tipo\": \"").append(c[2]).append("\",\n");
            json.append("    \"saldo\": ").append(c[3]).append("\n");
            json.append("  }");
            if (i < cuentas.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("]");
        escribirJSON(ARCHIVO_CUENTAS, json.toString());
    }

    static void guardarAsientos() {
        StringBuilder json = new StringBuilder();
        json.append("[\n");
        for (int i = 0; i < asientos.size(); i++) {
            Object[] a = asientos.get(i);
            json.append("  {\n");
            json.append("    \"fecha\": \"").append(a[0]).append("\",\n");
            json.append("    \"descripcion\": \"").append(a[1]).append("\",\n");
            json.append("    \"total\": ").append(a[2]).append(",\n");
            json.append("    \"partidas\": [\n");

            @SuppressWarnings("unchecked")
            List<Object[]> partidas = (List<Object[]>) a[3];
            for (int j = 0; j < partidas.size(); j++) {
                Object[] p = partidas.get(j);
                json.append("      {\n");
                json.append("        \"codigo\": \"").append(p[0]).append("\",\n");
                json.append("        \"tipo\": \"").append(p[1]).append("\",\n");
                json.append("        \"monto\": ").append(p[2]).append("\n");
                json.append("      }");
                if (j < partidas.size() - 1) json.append(",");
                json.append("\n");
            }

            json.append("    ]\n");
            json.append("  }");
            if (i < asientos.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("]");
        escribirJSON(ARCHIVO_ASIENTOS, json.toString());
    }

    static void mostrarCuentas() {
        System.out.println("\n=== CATALOGO DE CUENTAS ===");
        System.out.printf("%-8s %-35s %-15s %12s%n", "Codigo", "Nombre", "Tipo", "Saldo");
        System.out.println("-".repeat(75));
        for (Object[] c : cuentas) {
            System.out.printf("%-8s %-35s %-15s %12.2f%n", c[0], c[1], c[2], c[3]);
        }
    }

    static void crearCuenta() {
        System.out.println("\n--- NUEVA CUENTA ---");
        String codigo = leerString("Codigo: ");
        String nombre = leerString("Nombre: ");
        String tipo = leerString("Tipo (Activo/Pasivo/Patrimonio/Ingreso/Costo/Gasto): ");

        if (!tipoValido(tipo)) {
            System.out.println("Tipo invalido.");
            return;
        }

        if (existeCuenta(codigo)) {
            System.out.println("El codigo ya existe.");
            return;
        }

        agregarCuenta(codigo, nombre, tipo);
        guardarCuentas();
        System.out.println("Cuenta creada exitosamente.");
    }

    static boolean tipoValido(String tipo) {
        String[] tipos = {"Activo", "Pasivo", "Patrimonio", "Ingreso", "Costo", "Gasto"};
        for (String t : tipos) {
            if (t.equals(tipo)) return true;
        }
        return false;
    }

    static void registrarAsiento() {
        System.out.println("\n--- REGISTRAR ASIENTO ---");
        String fecha = leerFecha("Ingrese la fecha del asiento");
        String descripcion = leerString("Descripcion: ");
        int numPartidas = leerInt("Numero de partidas (minimo 2): ");

        if (numPartidas < 2) {
            System.out.println("ERROR: Un asiento debe tener al menos 2 partidas.");
            return;
        }

        List<Object[]> partidas = new ArrayList<>();
        double totalDebito = 0;
        double totalCredito = 0;

        for (int i = 0; i < numPartidas; i++) {
            System.out.println("Partida " + (i + 1) + ":");
            String codigo = leerString("  Codigo de cuenta: ");
            String tipo = leerString("  Debito/Credito: ");
            double monto = leerDouble("  Monto: ");

            if (!existeCuenta(codigo)) {
                System.out.println("  La cuenta no existe. Asiento cancelado.");
                return;
            }

            if (!tipo.equalsIgnoreCase("Debito") && !tipo.equalsIgnoreCase("Credito")) {
                System.out.println("  Tipo debe ser Debito o Credito. Asiento cancelado.");
                return;
            }

            Object[] partida = new Object[]{codigo, tipo, monto};
            partidas.add(partida);

            if (tipo.equalsIgnoreCase("Debito")) totalDebito += monto;
            else totalCredito += monto;
        }

        if (Math.abs(totalDebito - totalCredito) > 0.01) {
            System.out.println("ERROR: El asiento no cuadra. Debitos=" + totalDebito + " Creditos=" + totalCredito);
            return;
        }

        Object[] asiento = new Object[]{fecha, descripcion, totalDebito, partidas};
        asientos.add(asiento);

        actualizarSaldos(partidas);
        guardarCuentas();
        guardarAsientos();
        System.out.println("Asiento registrado exitosamente.");
    }

    static void actualizarSaldos(List<Object[]> partidas) {
        for (Object[] p : partidas) {
            String codigo = (String) p[0];
            String tipo = (String) p[1];
            double monto = (double) p[2];

            for (Object[] c : cuentas) {
                if (c[0].equals(codigo)) {
                    double saldo = (double) c[3];
                    String tipoCuenta = (String) c[2];

                    if (tipoCuenta.equals("Activo") || tipoCuenta.equals("Costo") || tipoCuenta.equals("Gasto")) {
                        saldo += tipo.equalsIgnoreCase("Debito") ? monto : -monto;
                    } else {
                        saldo += tipo.equalsIgnoreCase("Credito") ? monto : -monto;
                    }
                    c[3] = saldo;
                    break;
                }
            }
        }
    }

    static void mostrarAsientos() {
        System.out.println("\n=== ASIENTOS REGISTRADOS ===");
        for (int i = 0; i < asientos.size(); i++) {
            Object[] a = asientos.get(i);
            System.out.println("\nAsiento #" + (i + 1));
            System.out.println("Fecha: " + a[0]);
            System.out.println("Descripcion: " + a[1]);

            @SuppressWarnings("unchecked")
            List<Object[]> partidas = (List<Object[]>) a[3];
            for (Object[] p : partidas) {
                System.out.printf("  %s %-10s %10.2f%n", p[0], p[1], p[2]);
            }
            System.out.println("  Total: " + a[2]);
        }
    }

    static void generarBalance() {
        System.out.println("\n=== BALANCE GENERAL ===");
        System.out.printf("%-8s %-35s %12s%n", "Codigo", "Nombre", "Saldo");
        System.out.println("-".repeat(60));

        double totalActivo = 0;
        double totalPasivo = 0;
        double totalPatrimonio = 0;

        System.out.println("-- ACTIVO --");
        for (Object[] c : cuentas) {
            if (c[2].equals("Activo")) {
                System.out.printf("%-8s %-35s %12.2f%n", c[0], c[1], c[3]);
                totalActivo += (double) c[3];
            }
        }
        System.out.printf("%-44s %12.2f%n", "TOTAL ACTIVO:", totalActivo);

        System.out.println("\n-- PASIVO --");
        for (Object[] c : cuentas) {
            if (c[2].equals("Pasivo")) {
                System.out.printf("%-8s %-35s %12.2f%n", c[0], c[1], c[3]);
                totalPasivo += (double) c[3];
            }
        }
        System.out.printf("%-44s %12.2f%n", "TOTAL PASIVO:", totalPasivo);

        System.out.println("\n-- PATRIMONIO --");
        for (Object[] c : cuentas) {
            if (c[2].equals("Patrimonio")) {
                System.out.printf("%-8s %-35s %12.2f%n", c[0], c[1], c[3]);
                totalPatrimonio += (double) c[3];
            }
        }
        System.out.printf("%-44s %12.2f%n", "TOTAL PATRIMONIO:", totalPatrimonio);

        System.out.println("\n" + "-".repeat(60));
        System.out.printf("Total Pasivo + Patrimonio: %.2f%n", totalPasivo + totalPatrimonio);
        System.out.printf("Diferencia: %.2f%n", totalActivo - (totalPasivo + totalPatrimonio));

        if (Math.abs(totalActivo - (totalPasivo + totalPatrimonio)) < 0.01) {
            System.out.println("EL BALANCE CUADRADO CORRECTAMENTE.");
        } else {
            System.out.println("ADVERTENCIA: El balance NO cuadra.");
        }

        guardarBalance(totalActivo, totalPasivo, totalPatrimonio);
    }

    static void guardarBalance(double activo, double pasivo, double patrimonio) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"fecha\": \"").append(new Date().toString()).append("\",\n");
        json.append("  \"totalActivo\": ").append(activo).append(",\n");
        json.append("  \"totalPasivo\": ").append(pasivo).append(",\n");
        json.append("  \"totalPatrimonio\": ").append(patrimonio).append("\n");
        json.append("}");
        escribirJSON(ARCHIVO_BALANCE, json.toString());
    }

    static void verEstadoResultados() {
        System.out.println("\n=== ESTADO DE RESULTADOS ===");
        System.out.printf("%-8s %-35s %12s%n", "Codigo", "Nombre", "Saldo");
        System.out.println("-".repeat(60));

        double totalIngresos = 0;
        double totalCostos = 0;
        double totalGastos = 0;

        System.out.println("-- INGRESOS --");
        for (Object[] c : cuentas) {
            if (c[2].equals("Ingreso")) {
                System.out.printf("%-8s %-35s %12.2f%n", c[0], c[1], c[3]);
                totalIngresos += (double) c[3];
            }
        }
        System.out.printf("%-44s %12.2f%n", "TOTAL INGRESOS:", totalIngresos);

        System.out.println("\n-- COSTOS --");
        for (Object[] c : cuentas) {
            if (c[2].equals("Costo")) {
                System.out.printf("%-8s %-35s %12.2f%n", c[0], c[1], c[3]);
                totalCostos += (double) c[3];
            }
        }
        System.out.printf("%-44s %12.2f%n", "TOTAL COSTOS:", totalCostos);

        System.out.println("\n-- GASTOS --");
        for (Object[] c : cuentas) {
            if (c[2].equals("Gasto")) {
                System.out.printf("%-8s %-35s %12.2f%n", c[0], c[1], c[3]);
                totalGastos += (double) c[3];
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
        boolean encontrado = false;

        for (Object[] c : cuentas) {
            if (c[0].toString().contains(termino) ||
                c[1].toString().toLowerCase().contains(termino.toLowerCase())) {
                System.out.printf("%-8s %-35s %-15s %12.2f%n", c[0], c[1], c[2], c[3]);
                encontrado = true;
            }
        }
        if (!encontrado) System.out.println("No se encontraron resultados.");
    }

    static boolean existeCuenta(String codigo) {
        for (Object[] c : cuentas) {
            if (c[0].equals(codigo)) return true;
        }
        return false;
    }

    static List<Object[]> leerCuentasJSON(String archivo) {
        try (Reader reader = new FileReader(archivo)) {
            JsonElement elemento = JsonParser.parseReader(reader);
            if (!elemento.isJsonArray()) return new ArrayList<>();

            JsonArray array = elemento.getAsJsonArray();
            List<Object[]> resultado = new ArrayList<>();

            for (JsonElement e : array) {
                JsonObject obj = e.getAsJsonObject();
                Object[] cuenta = new Object[]{
                    obj.get("codigo").getAsString(),
                    obj.get("nombre").getAsString(),
                    obj.get("tipo").getAsString(),
                    obj.get("saldo").getAsDouble()
                };
                resultado.add(cuenta);
            }
            return resultado;
        } catch (FileNotFoundException e) {
            return new ArrayList<>();
        } catch (IOException e) {
            System.out.println("Error leyendo " + archivo + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    static List<Object[]> leerAsientosJSON(String archivo) {
        try (Reader reader = new FileReader(archivo)) {
            JsonElement elemento = JsonParser.parseReader(reader);
            if (!elemento.isJsonArray()) return new ArrayList<>();

            JsonArray array = elemento.getAsJsonArray();
            List<Object[]> resultado = new ArrayList<>();

            for (JsonElement e : array) {
                JsonObject obj = e.getAsJsonObject();

                List<Object[]> partidas = new ArrayList<>();
                JsonArray partidasArray = obj.getAsJsonArray("partidas");
                for (JsonElement pe : partidasArray) {
                    JsonObject p = pe.getAsJsonObject();
                    Object[] partida = new Object[]{
                        p.get("codigo").getAsString(),
                        p.get("tipo").getAsString(),
                        p.get("monto").getAsDouble()
                    };
                    partidas.add(partida);
                }

                Object[] asiento = new Object[]{
                    obj.get("fecha").getAsString(),
                    obj.get("descripcion").getAsString(),
                    obj.get("total").getAsDouble(),
                    partidas
                };
                resultado.add(asiento);
            }
            return resultado;
        } catch (FileNotFoundException e) {
            return new ArrayList<>();
        } catch (IOException e) {
            System.out.println("Error leyendo " + archivo + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    static void escribirJSON(String archivo, Object datos) {
        try (Writer writer = new FileWriter(archivo)) {
            if (datos instanceof String) {
                writer.write((String) datos);
            } else {
                gson.toJson(datos, writer);
            }
        } catch (IOException e) {
            System.out.println("Error escribiendo " + archivo + ": " + e.getMessage());
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
                double valor = Double.parseDouble(entrada);
                if (valor < 0) {
                    System.out.println("El monto no puede ser negativo.");
                    continue;
                }
                return valor;
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un monto numerico valido.");
            }
        }
    }

    static void verEstadoFlujoEfectivo() {
        System.out.println("\n=== ESTADO DE FLUJO DE EFECTIVO ===");
        System.out.println("(Metodo directo - basado en movimientos de Caja y Bancos)");
        System.out.println();

        double flujoOperacion = 0;
        double flujoInversion = 0;
        double flujoFinanciamiento = 0;

        // Analizar asientos para clasificar flujos
        for (Object[] a : asientos) {
            @SuppressWarnings("unchecked")
            List<Object[]> partidas = (List<Object[]>) a[3];
            for (Object[] p : partidas) {
                String codigo = (String) p[0];
                String tipo = (String) p[1];
                double monto = (double) p[2];

                // Clasificar por tipo de cuenta
                if (codigo.equals("1000") || codigo.equals("1100")) {
                    // Caja y Bancos - determinar si es entrada o salida
                    if (tipo.equalsIgnoreCase("Debito")) {
                        flujoOperacion += monto;
                    } else {
                        flujoOperacion -= monto;
                    }
                } else if (codigo.startsWith("1") && !codigo.equals("1000") && !codigo.equals("1100")) {
                    // Otros activos - inversion
                    if (tipo.equalsIgnoreCase("Debito")) {
                        flujoInversion -= monto;
                    } else {
                        flujoInversion += monto;
                    }
                } else if (codigo.startsWith("2")) {
                    // Pasivos - financiamiento
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
        System.out.println("\n=== ESTADO DE CAMBIOS EN EL PATRIMONIO ===");
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

        for (Object[] c : cuentas) {
            String codigo = (String) c[0];
            double saldo = (double) c[3];

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

        double patrimonioTotal = capitalSocial + capitalAportado + capitalGanado +
                                utilidadesRetenidas + utilidadEjercicio - perdidaEjercicio +
                                resultadosEjercicio + reservasLegales + ajustesRevaluacion - dividendosDecretados;

        System.out.println("MOVIMIENTOS DEL PATRIMONIO");
        System.out.println("-".repeat(60));
        System.out.printf("%-40s %12s%n", "Capital social:", capitalSocial);
        System.out.printf("%-40s %12s%n", "Capital aportado:", capitalAportado);
        System.out.printf("%-40s %12s%n", "Capital ganado:", capitalGanado);
        System.out.printf("%-40s %12s%n", "Utilidades retenidas:", utilidadesRetenidas);
        System.out.printf("%-40s %12s%n", "Utilidad del ejercicio:", utilidadEjercicio);
        System.out.printf("%-40s %12s%n", "Perdida del ejercicio:", perdidaEjercicio);
        System.out.printf("%-40s %12s%n", "Resultados del ejercicio:", resultadosEjercicio);
        System.out.printf("%-40s %12s%n", "Reservas legales:", reservasLegales);
        System.out.printf("%-40s %12s%n", "Ajustes por revaluacion:", ajustesRevaluacion);
        System.out.printf("%-40s %12s%n", "Dividendos decretados:", dividendosDecretados);
        System.out.println("-".repeat(60));
        System.out.printf("%-40s %12s%n", "PATRIMONIO TOTAL:", patrimonioTotal);
    }

    static void verBalanceComprobacion() {
        System.out.println("\n=== BALANCE DE COMPROBACION ===");
        System.out.println("(Verificacion: Debitos = Creditos por cuenta)");
        System.out.println();
        System.out.printf("%-8s %-35s %12s %12s %12s%n", "Codigo", "Nombre", "Debitos", "Creditos", "Saldo");
        System.out.println("-".repeat(85));

        double totalDebitos = 0;
        double totalCreditos = 0;

        for (Object[] c : cuentas) {
            String codigo = (String) c[0];
            String nombre = (String) c[1];
            String tipo = (String) c[2];
            double saldo = (double) c[3];

            double debitos = 0;
            double creditos = 0;

            // Calcular debitos y creditos de esta cuenta desde los asientos
            for (Object[] a : asientos) {
                @SuppressWarnings("unchecked")
                List<Object[]> partidas = (List<Object[]>) a[3];
                for (Object[] p : partidas) {
                    if (p[0].equals(codigo)) {
                        if (p[1].toString().equalsIgnoreCase("Debito")) {
                            debitos += (double) p[2];
                        } else {
                            creditos += (double) p[2];
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
        System.out.println("(Registro cronologico de todos los asientos)");
        System.out.println();

        if (asientos.isEmpty()) {
            System.out.println("No hay asientos registrados.");
            return;
        }

        System.out.printf("%-5s %-20s %-30s %12s%n", "No.", "Fecha", "Descripcion", "Total");
        System.out.println("-".repeat(75));

        for (int i = 0; i < asientos.size(); i++) {
            Object[] a = asientos.get(i);
            System.out.printf("%-5d %-20s %-30s %12.2f%n", i + 1, a[0], a[1], a[2]);
        }

        System.out.println("-".repeat(75));
        System.out.printf("Total de asientos: %d%n", asientos.size());
    }

    static void verLibroMayor() {
        System.out.println("\n=== LIBRO MAYOR ===");
        System.out.println("(Movimientos por cuenta individual)");
        System.out.println();

        String codigo = leerString("Ingrese el codigo de cuenta: ");

        if (!existeCuenta(codigo)) {
            System.out.println("La cuenta no existe.");
            return;
        }

        // Buscar la cuenta
        Object[] cuentaEncontrada = null;
        for (Object[] c : cuentas) {
            if (c[0].equals(codigo)) {
                cuentaEncontrada = c;
                break;
            }
        }

        System.out.println();
        System.out.println("CUENTA: " + cuentaEncontrada[0] + " - " + cuentaEncontrada[1]);
        System.out.println("Tipo: " + cuentaEncontrada[2]);
        System.out.println();

        System.out.printf("%-5s %-20s %-10s %12s %12s%n", "No.", "Fecha", "Tipo", "Monto", "Saldo");
        System.out.println("-".repeat(65));

        double saldo = 0;
        int contador = 0;

        for (Object[] a : asientos) {
            @SuppressWarnings("unchecked")
            List<Object[]> partidas = (List<Object[]>) a[3];
            for (Object[] p : partidas) {
                if (p[0].equals(codigo)) {
                    contador++;
                    String tipo = (String) p[1];
                    double monto = (double) p[2];

                    String tipoCuenta = (String) cuentaEncontrada[2];
                    if (tipoCuenta.equals("Activo") || tipoCuenta.equals("Costo") || tipoCuenta.equals("Gasto")) {
                        saldo += tipo.equalsIgnoreCase("Debito") ? monto : -monto;
                    } else {
                        saldo += tipo.equalsIgnoreCase("Credito") ? monto : -monto;
                    }

                    System.out.printf("%-5d %-20s %-10s %12.2f %12.2f%n", contador, a[0], tipo, monto, saldo);
                }
            }
        }

        System.out.println("-".repeat(65));
        System.out.printf("Saldo final: %.2f%n", saldo);
    }

    static void revertirAsiento() {
        System.out.println("\n--- REVERTIR ASIENTO ---");
        System.out.println("(Se crea un asiento de reversa - no se borra el original)");
        System.out.println();

        if (asientos.isEmpty()) {
            System.out.println("No hay asientos para revertir.");
            return;
        }

        // Mostrar asientos
        for (int i = 0; i < asientos.size(); i++) {
            Object[] a = asientos.get(i);
            System.out.printf("%d. %s - %s (Total: %.2f)%n", i + 1, a[0], a[1], a[2]);
        }

        int seleccion = leerInt("Seleccione el numero de asiento a revertir (1-" + asientos.size() + "): ") - 1;

        if (seleccion < 0 || seleccion >= asientos.size()) {
            System.out.println("Seleccion invalida. Debe estar entre 1 y " + asientos.size() + ".");
            return;
        }

        Object[] asientoOriginal = asientos.get(seleccion);
        @SuppressWarnings("unchecked")
        List<Object[]> partidasOriginales = (List<Object[]>) asientoOriginal[3];

        // Crear partidas de reversa (invertir debitos y creditos)
        List<Object[]> partidasReversa = new ArrayList<>();
        for (Object[] p : partidasOriginales) {
            String tipoReversa = p[1].toString().equalsIgnoreCase("Debito") ? "Credito" : "Debito";
            Object[] partidaReversa = new Object[]{p[0], tipoReversa, p[2]};
            partidasReversa.add(partidaReversa);
        }

        // Crear asiento de reversa
        Object[] asientoReversa = new Object[]{
            new Date().toString(),
            "REVERSO: " + asientoOriginal[1],
            asientoOriginal[2],
            partidasReversa
        };

        asientos.add(asientoReversa);
        actualizarSaldos(partidasReversa);
        guardarCuentas();
        guardarAsientos();

        System.out.println("Asiento revertido exitosamente.");
        System.out.println("Se ha creado un asiento de reversa que anula el original.");
    }

    static void reiniciarCatalogo() {
        System.out.println("\n--- REINICIAR CATALOGO ---");
        System.out.println("ADVERTENCIA: Esto reemplazara el catalogo actual con las 75 cuentas estandar.");
        System.out.println("Los saldos actuales de las cuentas se perderan.");
        String confirmacion = leerString("Esta seguro? (Si/No): ");

        if (!confirmacion.equalsIgnoreCase("Si")) {
            System.out.println("Operacion cancelada.");
            return;
        }

        // Limpiar catalogo actual
        cuentas.clear();

        // Reinicializar con las 75 cuentas
        inicializarCuentas();

        System.out.println("Catalogo reiniciado exitosamente con " + cuentas.size() + " cuentas.");
    }

    static String leerFecha(String mensaje) {
        while (true) {
            System.out.print(mensaje + " (dd/MM/yyyy): ");
            String entrada = scanner.nextLine().trim();
            
            if (entrada.isEmpty()) {
                System.out.println("La fecha no puede estar vacia.");
                continue;
            }
            
            // Validar formato dd/MM/yyyy
            String[] partes = entrada.split("/");
            if (partes.length != 3) {
                System.out.println("Formato invalido. Use dd/MM/yyyy.");
                continue;
            }
            
            try {
                int dia = Integer.parseInt(partes[0]);
                int mes = Integer.parseInt(partes[1]);
                int anio = Integer.parseInt(partes[2]);
                
                if (dia < 1 || dia > 31) {
                    System.out.println("Dia invalido. Debe estar entre 1 y 31.");
                    continue;
                }
                if (mes < 1 || mes > 12) {
                    System.out.println("Mes invalido. Debe estar entre 1 y 12.");
                    continue;
                }
                if (anio < 1900 || anio > 2100) {
                    System.out.println("Año invalido. Debe estar entre 1900 y 2100.");
                    continue;
                }
                
                // Formatear fecha
                return String.format("%02d/%02d/%04d", dia, mes, anio);
            } catch (NumberFormatException e) {
                System.out.println("Ingrese una fecha valida en formato dd/MM/yyyy.");
            }
        }
    }

    static void buscarAsientos() {
        System.out.println("\n--- BUSCAR ASIENTOS ---");
        System.out.println("1. Por fecha");
        System.out.println("2. Por descripcion");
        System.out.println("3. Por cuenta");
        
        int opcion = leerInt("Seleccione opcion de busqueda: ");
        
        List<Object[]> resultados = new ArrayList<>();
        
        switch (opcion) {
            case 1:
                String fecha = leerFecha("Ingrese la fecha a buscar");
                for (Object[] a : asientos) {
                    if (a[0].equals(fecha)) {
                        resultados.add(a);
                    }
                }
                break;
            case 2:
                String termino = leerString("Ingrese el termino a buscar").toLowerCase();
                for (Object[] a : asientos) {
                    if (a[1].toString().toLowerCase().contains(termino)) {
                        resultados.add(a);
                    }
                }
                break;
            case 3:
                String codigo = leerString("Ingrese el codigo de cuenta");
                for (Object[] a : asientos) {
                    @SuppressWarnings("unchecked")
                    List<Object[]> partidas = (List<Object[]>) a[3];
                    for (Object[] p : partidas) {
                        if (p[0].equals(codigo)) {
                            resultados.add(a);
                            break;
                        }
                    }
                }
                break;
            default:
                System.out.println("Opcion invalida.");
                return;
        }
        
        System.out.println("\n=== RESULTADOS DE BUSQUEDA ===");
        if (resultados.isEmpty()) {
            System.out.println("No se encontraron asientos.");
            return;
        }
        
        for (int i = 0; i < resultados.size(); i++) {
            Object[] a = resultados.get(i);
            System.out.println("\nResultado #" + (i + 1));
            System.out.println("Fecha: " + a[0]);
            System.out.println("Descripcion: " + a[1]);
            
            @SuppressWarnings("unchecked")
            List<Object[]> partidas = (List<Object[]>) a[3];
            for (Object[] p : partidas) {
                System.out.printf("  %s %-10s %10.2f%n", p[0], p[1], p[2]);
            }
            System.out.println("  Total: " + a[2]);
        }
    }

    static void salir() {
        System.out.println("\nGuardando datos y saliendo...");
        guardarCuentas();
        guardarAsientos();
        System.out.println("Hasta luego.");
    }
}
