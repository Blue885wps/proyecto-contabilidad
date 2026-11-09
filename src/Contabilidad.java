import java.io.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.Locale;

public class Contabilidad {
    // ============================================================
    // MAIN Y MENUS
    // ============================================================
    public static void main(String[] args) {
        System.out.println("=== SISTEMA CONTABLE EMPRESARIAL ===");
        Persistencia.cargarDatos();
        while (true) {
            mostrarMenu();
            int opcion = Consola.leerInt("Seleccione una opcion (0-12): ", 0, 12);
            if (Consola.entradaCerrada) { salir(); return; }
            switch (opcion) {
                case 1: menuAdministrarCuentas(); break;
                case 2: Asientos.registrarAsiento(); break;
                case 3: menuConsultarCuentasT(); break;
                case 4: Reportes.verLibroDiario(); break;
                case 5: Reportes.verLibroMayor(); break;
                case 6: Reportes.verBalanceComprobacion(); break;
                case 7: Reportes.verEstadoResultados(); break;
                case 8: Reportes.generarBalance(); break;
                case 9: menuConsultarMovimientos(); break;
                case 10: Persistencia.respaldarInformacion(); break;
                case 11: case 0: salir(); return;
                case 12: configurarRangoReportes(); break;
            }
        }
    }

    static void mostrarMenu() {
        Consola.linea("\n===========================================",
                "       SISTEMA CONTABLE EMPRESARIAL",
                "===========================================",
                "1. Administrar cuentas (ver, crear o buscar cuentas)",
                "2. Registrar operacion contable (compra, venta o pago; Debe = Haber)",
                "3. Consultar cuentas T (movimientos al Debe y al Haber)",
                "4. Generar libro diario (todas las operaciones por fecha)",
                "5. Generar libro mayor (movimientos y saldo de una cuenta)",
                "6. Generar balanza de comprobacion (cargos y abonos; debe cuadrar)",
                "7. Generar estado de resultados (Ingresos - Costos - Gastos)",
                "8. Generar balance general (Activo = Pasivo + Patrimonio + Resultado)",
                "9. Consultar movimientos (ver o buscar asientos)",
                "10. Respaldar informacion (copia de seguridad en backup/)",
                "11. Salir (o 0)",
                "12. Rango de reportes (filtrar reportes por fechas)");
    }

    static void menuAdministrarCuentas() {
        while (true) {
            int opcion = Consola.menu("ADMINISTRAR CUENTAS", "Ver catalogo", "Crear cuenta", "Buscar cuenta", "Reconstruir catalogo estandar");
            if (Consola.entradaCerrada) return;
            switch (opcion) {
                case 1: Cuentas.mostrarCatalogo(); break; case 2: Cuentas.crearCuenta(); break;
                case 3: Cuentas.buscarCuenta(); break; case 4: Cuentas.reconstruirCatalogo(); break; case 0: return;
            }
        }
    }

    static void menuConsultarCuentasT() {
        while (true) {
            int opcion = Consola.menu("CONSULTAR CUENTAS T", "Consultar una cuenta", "Cuentas con movimientos");
            if (Consola.entradaCerrada) return;
            switch (opcion) {
                case 1: Reportes.verCuentaTIndividual(); break; case 2: Reportes.verCuentasTConMovimientos(); break; case 0: return;
            }
        }
    }

    static void menuConsultarMovimientos() {
        while (true) {
            int opcion = Consola.menu("CONSULTAR MOVIMIENTOS", "Ver asientos registrados", "Buscar asientos");
            if (Consola.entradaCerrada) return;
            switch (opcion) {
                case 1: Asientos.mostrarAsientos(); break; case 2: Asientos.buscarAsientos(); break; case 0: return;
            }
        }
    }

    // ============================================================
    // OPCION 12: RANGO DE REPORTES
    // ============================================================
    static void configurarRangoReportes() {
        System.out.println("\n--- RANGO DE REPORTES ---");
        System.out.println("El rango filtra los reportes de movimientos por fecha.");
        System.out.println("La balanza, el balance general y el catalogo usan la fecha final como corte.");
        System.out.println("Rango actual: " + (Estado.rangoDesde.isEmpty() ? "Historico completo" : Estado.rangoDesde + " al " + Estado.rangoHasta));
        System.out.println("Deje la fecha inicial vacia para ver todo el historico.");
        String desde = Consola.leerFechaRango("Fecha inicial (Enter = historico): ");
        if (desde == null) return;
        if (desde.isEmpty()) { Estado.rangoDesde = ""; Estado.rangoHasta = ""; System.out.println("Rango restablecido: historico completo."); return; }
        String hasta = Consola.leerFechaRango("Fecha final (Enter = hoy " + Fechas.fechaActual() + "): ");
        if (hasta == null) return;
        if (hasta.isEmpty()) hasta = Fechas.fechaActual();
        if (Fechas.fechaOrdenable(hasta).compareTo(Fechas.fechaOrdenable(desde)) < 0) {
            System.out.println("La fecha final no puede ser anterior a la inicial."); return;
        }
        Estado.rangoDesde = desde; Estado.rangoHasta = hasta;
        System.out.println("Rango configurado: " + Estado.rangoDesde + " al " + Estado.rangoHasta + ".");
    }

    // ============================================================
    // SALIDA
    // ============================================================
    static void salir() {
        System.out.println("\nGuardando y saliendo...");
        if (Persistencia.bloquearGuardadoAsientos || Persistencia.bloquearGuardadoCuentas) {
            System.out.println("No se guardaron cambios: corrija los archivos de datos y reinicie el programa.");
        } else {
            if (!Persistencia.guardarAsientos() || !Persistencia.guardarCuentas()) System.out.println("Error guardando.");
        }
        System.out.println("Hasta luego.");
    }
}
