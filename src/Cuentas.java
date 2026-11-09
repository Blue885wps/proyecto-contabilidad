import java.io.*;
import java.util.*;

public class Cuentas {
    // ============================================================
    // CATALOGO Y TIPOS DE CUENTA
    // ============================================================
    static final String[] TIPOS_CUENTA = {"Activo", "Pasivo", "Patrimonio", "Ingreso", "Costo", "Gasto"};
    static final List<String> TIPOS = Arrays.asList(TIPOS_CUENTA);

    // Catalogo estandar de 75 cuentas: codigo|nombre|tipo, separadas por ;
    static final String[] CATALOGO_ESTANDAR = {
        "1000|Caja|Activo;1100|Bancos|Activo;1110|Inversiones temporales|Activo;1200|Cuentas por cobrar|Activo;1210|Clientes|Activo;1220|Documentos por cobrar|Activo;1230|Deudores diversos|Activo;1300|Inventarios|Activo;1310|Materias primas|Activo;1320|Productos terminados|Activo;1400|Activos fijos|Activo;1410|Mobiliario y equipo|Activo;1420|Equipo de transporte|Activo;1430|Equipo de computo|Activo;1500|Activos diferidos|Activo;1510|Gastos pagados por anticipado|Activo;1520|Seguros pagados por anticipado|Activo;1600|Cuentas por cobrar a largo plazo|Activo;1700|Propiedad, planta y equipo|Activo;1790|Depreciacion acumulada|Activo;1800|Activos intangibles|Activo;1890|Amortizacion acumulada|Activo",
        "2000|Cuentas por pagar|Pasivo;2010|Proveedores|Pasivo;2020|Acreedores diversos|Pasivo;2100|Prestamos bancarios|Pasivo;2110|Prestamos a corto plazo|Pasivo;2120|Prestamos a largo plazo|Pasivo;2200|Impuestos por pagar|Pasivo;2210|IVA por pagar|Pasivo;2220|ISR por pagar|Pasivo;2300|Pasivos diferidos|Pasivo;2310|Anticipos de clientes|Pasivo;2320|Intereses cobrados por anticipado|Pasivo;2400|Pasivos por impuestos diferidos|Pasivo;2500|Obligaciones financieras a largo plazo|Pasivo;2600|Pasivos por beneficios a empleados|Pasivo",
        "3000|Capital social|Patrimonio;3010|Capital aportado|Patrimonio;3020|Capital ganado|Patrimonio;3100|Utilidades retenidas|Patrimonio;3110|Utilidad del ejercicio|Patrimonio;3120|Perdida del ejercicio|Patrimonio;3200|Resultados de ejercicios anteriores|Patrimonio;3300|Reservas legales|Patrimonio;3400|Ajustes por revaluacion|Patrimonio;3500|Dividendos decretados|Patrimonio",
        "4000|Ingresos|Ingreso;4010|Ventas|Ingreso;4020|Ingresos por servicios|Ingreso;4030|Ingresos por intereses|Ingreso;4040|Otros ingresos|Ingreso;4050|Ingresos por alquileres|Ingreso;4060|Ingresos por comisiones|Ingreso;4070|Ingresos por dividendos|Ingreso",
        "5000|Costos|Costo;5010|Costo de ventas|Costo;5020|Costo de produccion|Costo;5030|Costo de lo vendido|Costo",
        "6000|Gastos|Gasto;6010|Gastos de venta|Gasto;6020|Gastos de administracion|Gasto;6030|Gastos financieros|Gasto;6040|Gastos de operacion|Gasto;6050|Sueldos y salarios|Gasto;6060|Renta|Gasto;6070|Servicios publicos|Gasto;6080|Depreciacion|Gasto;6090|Amortizacion|Gasto;6100|Gastos de publicidad y mercadeo|Gasto;6110|Gastos de mantenimiento|Gasto;6120|Gastos de seguros|Gasto;6130|Gastos de impuestos|Gasto;6140|Gastos de capacitacion|Gasto;6150|Gastos de viajes y representacion|Gasto"
    };

    static void inicializarCuentas() {
        for (String grupo : CATALOGO_ESTANDAR)
            for (String linea : grupo.split(";")) {
                String[] partes = linea.split("\\|"); agregarCuenta(partes[0], partes[1], partes[2]);
            }
        if (!Contabilidad.guardarCuentas()) System.out.println("ADVERTENCIA: no se pudo guardar el catalogo inicial.");
    }

    static List<String> codigosCatalogoEstandar() {
        List<String> codigos = new ArrayList<>();
        for (String grupo : CATALOGO_ESTANDAR)
            for (String linea : grupo.split(";")) codigos.add(linea.split("\\|")[0].trim());
        return codigos;
    }

    static List<String> codigosUsadosNoEstandar() {
        List<String> huerfanos = new ArrayList<>();
        List<String> estandar = codigosCatalogoEstandar();
        File file = new File(Contabilidad.ARCHIVO_ASIENTOS);
        if (!file.exists()) return huerfanos;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1);
                String[] partes = linea.split("\\|", -1);
                if (partes.length < 4 || !partes[0].trim().equals("PARTIDA")) continue;
                String codigo = partes[1].trim();
                if (!codigo.isEmpty() && !estandar.contains(codigo) && !huerfanos.contains(codigo)) huerfanos.add(codigo);
            }
        } catch (IOException e) {
            System.out.println("ADVERTENCIA: no se pudo revisar " + Contabilidad.ARCHIVO_ASIENTOS + ": " + e.getMessage());
            huerfanos.add("(no se pudo revisar " + Contabilidad.ARCHIVO_ASIENTOS + ")");
        }
        return huerfanos;
    }

    static void reconstruirCatalogo() {
        System.out.println("\n--- RECONSTRUIR CATALOGO ESTANDAR ---");
        List<String> huerfanos = codigosUsadosNoEstandar();
        if (!huerfanos.isEmpty()) {
            Consola.linea("No se puede reconstruir el catalogo: hay asientos que usan cuentas ajenas al catalogo estandar.",
                    "Cuentas afectadas: " + String.join(", ", huerfanos),
                    "Corrija o elimine esos asientos antes de continuar.");
            return;
        }
        if (!Estado.cuentaCodigos.isEmpty()) {
            Consola.linea("Esta accion reemplaza el catalogo actual (" + Estado.cuentaCodigos.size() + " cuentas) por las 75 cuentas estandar.",
                    "Se perderan las cuentas personalizadas y sus saldos.",
                    "Se guardara una copia del archivo actual en la carpeta backup/.");
            if (!Consola.confirmar("Desea continuar? (S/N): ")) { System.out.println("Operacion cancelada."); return; }
        } else {
            System.out.println("El catalogo esta vacio o no tiene cuentas validas. Se creara el catalogo estandar de 75 cuentas.");
        }
        if (!Contabilidad.respaldarArchivoInvalido(Contabilidad.ARCHIVO_CUENTAS, "cuentas_antes_reconstruir")) {
            System.out.println("No se pudo guardar la copia de seguridad. No se reconstruyo el catalogo.");
            return;
        }
        boolean estabaBloqueado = Contabilidad.bloquearGuardadoCuentas;
        Contabilidad.bloquearGuardadoCuentas = false;
        Estado.cuentaCodigos.clear(); Estado.cuentaNombres.clear(); Estado.cuentaTipos.clear(); Estado.cuentaSaldos.clear();
        inicializarCuentas();
        Contabilidad.bloquearGuardadoAsientos = false;
        if (!Contabilidad.cargarAsientosTexto(Contabilidad.ARCHIVO_ASIENTOS)) {
            Contabilidad.bloquearGuardadoAsientos = true;
            Contabilidad.bloquearGuardadoCuentas = true;
            Consola.linea("No se pudieron recargar los asientos.",
                    "No se guardara " + Contabilidad.ARCHIVO_ASIENTOS + " ni se haran otros cambios hasta reiniciar.",
                    "Los reportes pueden estar incompletos. Corrija el archivo y reinicie.");
            return;
        }
        Asientos.recalcularSaldos();
        if (Contabilidad.bloquearGuardadoAsientos)
            System.out.println("ADVERTENCIA: no se pudo respaldar " + Contabilidad.ARCHIVO_ASIENTOS + "; no se guardara hasta corregir la carpeta backup/.");
        if (estabaBloqueado) System.out.println("El bloqueo de guardado del catalogo fue levantado.");
        System.out.println("Catalogo reconstruido con " + Estado.cuentaCodigos.size() + " cuentas estandar.");
        System.out.println("Asientos recargados: " + Estado.asientoFechas.size() + ".");
    }

    static void agregarCuenta(String codigo, String nombre, String tipo) {
        Estado.cuentaCodigos.add(codigo); Estado.cuentaNombres.add(nombre); Estado.cuentaTipos.add(tipo); Estado.cuentaSaldos.add(0.0);
    }

    static void buscarCuenta() {
        System.out.println("\n--- BUSCAR CUENTA ---");
        System.out.println("Busca por codigo o parte del nombre, sin distinguir tildes ni mayusculas.");
        String t = Consola.leerString("Codigo o parte del nombre: ");
        if (t == null) { Consola.entradaCerrada = true; System.out.println("Cancelado."); return; }
        System.out.println("\n=== RESULTADOS ===");
        List<String> enc = buscarCuentas(t);
        if (enc.isEmpty()) { System.out.println("Sin resultados."); return; }
        System.out.println(Estado.rangoDesde.isEmpty() ? "Saldo = historico completo." : "Saldo = movimientos dentro del rango configurado.");
        System.out.printf("%-8s %-35s %-15s %12s%n", "Codigo", "Nombre", "Tipo", "Saldo");
        System.out.println("-".repeat(75));
        List<Double> saldos = Asientos.saldosVista();
        for (String c : enc) {
            int i = indiceCuenta(c);
            System.out.printf("%-8s %-35s %-15s %12.2f%n", Estado.cuentaCodigos.get(i), Estado.cuentaNombres.get(i), Estado.cuentaTipos.get(i), saldos.get(i));
        }
    }

    static void mostrarCatalogo() {
        System.out.println("\n=== CATALOGO DE CUENTAS ===");
        System.out.println(Estado.descripcionRango());
        System.out.println(Estado.rangoDesde.isEmpty() ? "Saldo = historico completo." : "Saldo = movimientos dentro del rango configurado.");
        List<Double> saldos = Asientos.saldosVista();
        System.out.printf("%-8s %-35s %-15s %12s%n", "Codigo", "Nombre", "Tipo", "Saldo");
        System.out.println("-".repeat(75));
        for (int i = 0; i < Estado.cuentaCodigos.size(); i++) {
            System.out.printf("%-8s %-35s %-15s %12.2f%n",
                    Estado.cuentaCodigos.get(i), Estado.cuentaNombres.get(i), Estado.cuentaTipos.get(i), saldos.get(i));
        }
    }

    static boolean tipoValido(String tipo) { return TIPOS.contains(tipo); }

    static void crearCuenta() {
        if (Contabilidad.bloquearGuardadoCuentas) {
            Consola.linea("No se pueden crear cuentas: el catalogo o el guardado estan bloqueados.",
                    "Corrija los archivos de datos y reinicie el programa.");
            return;
        }
        Consola.linea("\n--- NUEVA CUENTA ---",
                "Una cuenta es el nombre con el que se registra un movimiento.",
                "Se pediran: nombre, tipo y codigo.");
        String nombre = Consola.leerTextoPersistible("Nombre (ej: Caja chica): ", "nombre");
        if (Consola.entradaCerrada) return;
        String tipo = pedirTipoCuenta();
        if (Consola.entradaCerrada) return;
        String sugerido = sugerirCodigoCuenta(tipo);
        String codigo;
        while (true) {
            if (sugerido.isEmpty()) codigo = Consola.leerString("Codigo de la cuenta (numero que la identifica, ej: 1010): ");
            else codigo = Consola.leerTextoOpcional("Codigo de la cuenta (ej: " + sugerido + "). Enter para usar el sugerido: ");
            if (Consola.entradaCerrada) return;
            if (codigo.isEmpty() && !sugerido.isEmpty()) codigo = sugerido;
            String err = codigo.equals("0") ? "El codigo 0 esta reservado para cancelar. Ingrese otro."
                    : codigo.equals("?") || codigo.contains("|") ? "El codigo no puede ser ? ni contener el caracter |."
                    : existeCuenta(codigo) || existeCodigoNormalizado(codigo) ? "El codigo ya existe. Ingrese otro." : null;
            if (err != null) { System.out.println(err); continue; }
            break;
        }
        agregarCuenta(codigo, nombre, tipo);
        if (!Contabilidad.guardarCuentas()) { Estado.cuentaCodigos.remove(Estado.cuentaCodigos.size()-1); Estado.cuentaNombres.remove(Estado.cuentaNombres.size()-1); Estado.cuentaTipos.remove(Estado.cuentaTipos.size()-1); Estado.cuentaSaldos.remove(Estado.cuentaSaldos.size()-1); System.out.println("Error guardando."); return; }
        System.out.println("Cuenta creada exitosamente: " + codigo + " - " + nombre + " (" + tipo + ").");
    }

    static String pedirTipoCuenta() {
        System.out.println("Tipo de cuenta (segun lo que representa):");
        String[] ayudas = {
            "lo que la empresa TIENE (caja, bancos, mercancia, equipo)",
            "lo que la empresa DEBE (proveedores, prestamos)",
            "aportes y resultados de los duenos",
            "ventas y otros ingresos",
            "costo de lo vendido o producido",
            "renta, sueldos, servicios, publicidad"
        };
        for (int i = 0; i < TIPOS_CUENTA.length; i++)
            System.out.println("  " + (i + 1) + ". " + TIPOS_CUENTA[i] + " - " + ayudas[i]);
        return TIPOS_CUENTA[Consola.leerInt("Seleccione (1-" + TIPOS_CUENTA.length + "): ", 1, TIPOS_CUENTA.length) - 1];
    }

    static String sugerirCodigoCuenta(String tipo) {
        int idx = TIPOS.indexOf(tipo);
        if (idx < 0) return "";
        int base = (idx + 1) * 1000, mayor = base - 10;
        for (String c : Estado.cuentaCodigos) try {
            int n = Integer.parseInt(c);
            if (n >= base && n <= base + 999 && n > mayor) mayor = n;
        } catch (NumberFormatException ignored) {}
        int sig = mayor + 10;
        return sig > base + 999 ? "" : String.format("%04d", sig);
    }

    static boolean existeCuenta(String cod) { return indiceCuenta(cod) >= 0; }

    static boolean existeCodigoNormalizado(String cod) {
        String n = Formato.normalizarTexto(cod);
        for (int i = 0; i < Estado.cuentaCodigos.size(); i++) if (Formato.normalizarTexto(Estado.cuentaCodigos.get(i)).equals(n)) return true;
        return false;
    }

    static int indiceCuenta(String cod) {
        for (int i = 0; i < Estado.cuentaCodigos.size(); i++) if (Estado.cuentaCodigos.get(i).equals(cod)) return i;
        return -1;
    }

    static String nombreCuenta(String cod) {
        int i = indiceCuenta(cod); return i < 0 ? "(no existe)" : Estado.cuentaNombres.get(i);
    }

    static List<String> buscarCuentas(String term) {
        List<String> r = new ArrayList<>();
        String q = Formato.normalizarTexto(term);
        for (int i = 0; i < Estado.cuentaCodigos.size(); i++)
            if (Formato.normalizarTexto(Estado.cuentaCodigos.get(i)).contains(q) || Formato.normalizarTexto(Estado.cuentaNombres.get(i)).contains(q))
                r.add(Estado.cuentaCodigos.get(i));
        return r;
    }

    static void mostrarCuentaEncontrada(int idx) {
        System.out.printf("    %s - %s (%s)%n", Estado.cuentaCodigos.get(idx), Estado.cuentaNombres.get(idx), Estado.cuentaTipos.get(idx));
    }

    static String pedirCuenta(String ctx, String accionCancelar) {
        while (true) {
            Consola.linea("  " + ctx, "    Codigo o parte del nombre. ? = catalogo | 0 = " + accionCancelar);
            String t = Consola.leerString("    Cuenta: ");
            if (t == null) { Consola.entradaCerrada = true; return null; }
            if (t.equals("0")) return null;
            if (t.equals("?")) { mostrarCatalogo(); continue; }
            int ex = indiceCuenta(t);
            if (ex >= 0) { mostrarCuentaEncontrada(ex); return Estado.cuentaCodigos.get(ex); }
            List<String> c = buscarCuentas(t);
            if (c.isEmpty()) {
                Consola.linea("  No se encontro ninguna cuenta con ese codigo o nombre.",
                        "  Pruebe con otra palabra, ? para ver el catalogo o 0 para cancelar.");
                continue;
            }
            if (c.size() == 1) { int i = indiceCuenta(c.get(0)); mostrarCuentaEncontrada(i); return c.get(0); }
            System.out.println("  Coincidencias:");
            for (int i = 0; i < c.size(); i++) {
                int idx = indiceCuenta(c.get(i));
                System.out.printf("  %2d) %-6s %-35s (%s)%n", i + 1, Estado.cuentaCodigos.get(idx), Estado.cuentaNombres.get(idx), Estado.cuentaTipos.get(idx));
            }
            Consola.linea("   0) Cancelar", "   V) Volver a buscar");
            while (true) {
                String sel = Consola.leerString("  Numero (0=Cancelar, V=Volver): ");
                if (sel == null) { Consola.entradaCerrada = true; return null; }
                if (sel.equalsIgnoreCase("V")) break;
                if (sel.equals("0")) return null;
                try {
                    int n = Integer.parseInt(sel);
                    if (n >= 1 && n <= c.size()) { int idx = indiceCuenta(c.get(n - 1)); mostrarCuentaEncontrada(idx); return c.get(n - 1); }
                } catch (NumberFormatException ignored) {}
                System.out.println("  Invalido.");
            }
        }
    }
}
