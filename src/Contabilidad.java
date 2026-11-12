import java.time.LocalDate;
import java.util.Scanner;

public class Contabilidad {
    static final Scanner scanner = new Scanner(System.in);
    static boolean entradaCerrada = false;

    public static void main(String[] args) {
        System.out.println("=== SISTEMA CONTABLE EMPRESARIAL ===");
        Datos.cargarDatos();
        while (true) {
            mostrarMenu();
            int opcion = leerInt("Seleccione una opcion (0-11): ", 0, 11);
            if (entradaCerrada) { salir(); return; }
            switch (opcion) {
                case 1: menuAdministrarCuentas(); break;
                case 2: Asientos.registrarAsiento(); break;
                case 3: Reportes.verCuentaT(); break;
                case 4: Reportes.verLibroDiario(); break;
                case 5: Reportes.verLibroMayor(); break;
                case 6: Reportes.verBalanceComprobacion(); break;
                case 7: Reportes.verEstadoResultados(); break;
                case 8: Reportes.generarBalance(); break;
                case 9: Asientos.mostrarAsientos(); break;
                case 10: Datos.respaldarInformacion(); break;
                case 11: case 0: salir(); return;
            }
        }
    }

    static void mostrarMenu() {
        linea("\n===========================================",
                "       SISTEMA CONTABLE EMPRESARIAL",
                "===========================================",
                "1. Administrar cuentas",
                "2. Registrar operacion contable",
                "3. Consultar cuentas T",
                "4. Generar libro diario",
                "5. Generar libro mayor",
                "6. Generar balanza de comprobacion",
                "7. Generar estado de resultados",
                "8. Generar balance general",
                "9. Consultar movimientos",
                "10. Respaldar informacion",
                "11. Salir (o 0)");
    }

    static void menuAdministrarCuentas() {
        while (true) {
            linea("\n--- ADMINISTRAR CUENTAS ---",
                    "1. Ver catalogo",
                    "2. Crear cuenta",
                    "0. Volver");
            int opcion = leerInt("Seleccione una opcion (0-2): ", 0, 2);
            if (entradaCerrada || opcion == 0) return;
            if (opcion == 1) Datos.mostrarCatalogo();
            else Datos.crearCuenta();
        }
    }

    static void salir() {
        System.out.println("\nGuardando y saliendo...");
        Datos.guardarDatos();
        System.out.println("Hasta luego.");
    }

    static void linea(String... textos) {
        for (String texto : textos) System.out.println(texto);
    }

    static String leerEntrada(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            if (!scanner.hasNextLine()) {
                entradaCerrada = true;
                System.out.println("No hay mas entrada.");
                return null;
            }
            String entrada = scanner.nextLine().trim();
            if (!entrada.isEmpty()) return entrada;
            System.out.println("No puede estar vacio.");
        }
    }

    static int leerInt(String mensaje, int minimo, int maximo) {
        while (true) {
            String texto = leerEntrada(mensaje);
            if (texto == null) return minimo;
            try {
                int valor = Integer.parseInt(texto);
                if (valor >= minimo && valor <= maximo) return valor;
                System.out.println("Entre " + minimo + " y " + maximo + ".");
            } catch (NumberFormatException e) {
                System.out.println("Entero invalido.");
            }
        }
    }

    static double leerDouble(String mensaje) {
        while (true) {
            String texto = leerEntrada(mensaje);
            if (texto == null) return 0.0;
            String limpio = texto.replace(',', '.');
            if (limpio.matches("\\d+[.,]\\d{3}")) {
                System.out.println("Monto ambiguo. Use 1800 o 1800,00 para evitar confusiones.");
                continue;
            }
            try {
                double valor = Double.parseDouble(limpio);
                if (Double.isFinite(valor) && valor > 0) return valor;
            } catch (NumberFormatException e) {
                // se informa abajo
            }
            System.out.println("Monto invalido. Use un numero mayor que cero (coma o punto decimal).");
        }
    }

    static String leerFecha(String mensaje) {
        String hoy = Datos.fechaActual();
        while (true) {
            System.out.print(mensaje + " (dd/MM/yyyy, Enter = hoy " + hoy + "): ");
            if (!scanner.hasNextLine()) {
                entradaCerrada = true;
                System.out.println("No hay mas entrada.");
                return null;
            }
            String entrada = scanner.nextLine().trim();
            if (entrada.isEmpty()) return hoy;
            LocalDate fecha = Datos.parsearFecha(entrada);
            if (fecha != null && fecha.getYear() >= 1900 && fecha.getYear() <= 2100)
                return fecha.format(Datos.FORMATO_FECHA);
            System.out.println("Fecha invalida. Use dd/MM/yyyy entre 1900 y 2100.");
        }
    }

    static String leerTextoPersistible(String mensaje, String campo) {
        while (true) {
            String texto = leerEntrada(mensaje);
            if (texto == null) return null;
            if (!texto.contains("|")) return texto;
            System.out.println("El campo " + campo + " no puede contener el caracter |.");
        }
    }

    static boolean confirmar(String mensaje) {
        while (true) {
            String respuesta = leerEntrada(mensaje);
            if (respuesta == null) return false;
            if (respuesta.equalsIgnoreCase("S") || respuesta.equalsIgnoreCase("SI")) return true;
            if (respuesta.equalsIgnoreCase("N") || respuesta.equalsIgnoreCase("NO")) return false;
            System.out.println("Responda S o N.");
        }
    }
}
