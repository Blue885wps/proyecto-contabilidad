import java.io.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.Locale;

public class Contabilidad {
    // ============================================================
    // CONFIGURACION Y CONSTANTES
    // ============================================================
    static boolean saldoContrarioEnReporte = false;

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
                case 4: verLibroDiario(); break;
                case 5: verLibroMayor(); break;
                case 6: verBalanceComprobacion(); break;
                case 7: verEstadoResultados(); break;
                case 8: generarBalance(); break;
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
                case 1: verCuentaTIndividual(); break; case 2: verCuentasTConMovimientos(); break; case 0: return;
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

    static void verBalanceComprobacion() {
        String titulo = Estado.descripcionRango();
        Consola.linea("\n=== BALANZA DE COMPROBACION ===",
                titulo,
                "Cargos (Debitos) y abonos (Creditos) por cuenta.",
                "Los totales de Debitos y Creditos deben ser iguales.", "");
        System.out.printf("%-8s %-35s %12s %12s %12s %12s%n", "Codigo", "Nombre", "Debitos", "Creditos", "Deudor", "Acreedor");
        System.out.println("-".repeat(96));
        double[][] mov = Asientos.movimientosRango(Fechas.FECHA_MIN, Estado.vistaFin());
        double[] deb = mov[0], cre = mov[1];
        double tDeb = 0, tCre = 0, tDeu = 0, tAcr = 0;
        for (int i = 0; i < Estado.cuentaCodigos.size(); i++) {
            if (deb[i] <= 0 && cre[i] <= 0) continue;
            double sal = deb[i] - cre[i];
            double deu = sal > Estado.TOLERANCIA_SALDO ? sal : 0, acr = sal < -Estado.TOLERANCIA_SALDO ? -sal : 0;
            System.out.printf("%-8s %-35s %12.2f %12.2f %12.2f %12.2f%n",
                    Estado.cuentaCodigos.get(i), Estado.cuentaNombres.get(i), deb[i], cre[i], deu, acr);
            tDeb += deb[i]; tCre += cre[i]; tDeu += deu; tAcr += acr;
        }
        System.out.println("-".repeat(96));
        System.out.printf("%-44s %12.2f %12.2f %12.2f %12.2f%n", "TOTALES:", tDeb, tCre, tDeu, tAcr);
        System.out.println();
        if (Asientos.cuadra(tDeb, tCre)) System.out.println("LA BALANZA DE COMPROBACION ESTA CUADRADA.");
        else { System.out.println("ADVERTENCIA: NO cuadra."); System.out.printf("Diferencia: %.2f%n", tDeb - tCre); }
    }

    static void verLibroDiario() {
        Consola.linea("\n=== LIBRO DIARIO ===",
                Estado.descripcionRango(),
                "Todas las operaciones en orden de fecha.",
                "Cada asiento muestra primero el Debe y luego el Haber.",
                "Formato: Cta | Detalle | Parcial | Debe | Haber");
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < Estado.asientoFechas.size(); i++)
            if (Fechas.fechaEnRango(Estado.asientoFechas.get(i), Estado.vistaInicio(), Estado.vistaFin())) idx.add(i);
        if (idx.isEmpty()) { System.out.println("Sin asientos."); return; }
        Asientos.ordenarMovimientos(idx, null);
        System.out.println();
        System.out.printf("%-10s %-42s %12s %12s %12s%n", "Cta", "Detalle", "Parcial", "Debe", "Haber");
        System.out.println("=".repeat(92));
        double tGDeb = 0, tGHab = 0;
        for (int k = 0; k < idx.size(); k++) {
            int i = idx.get(k);
            List<String> cods = Estado.partidaCodigos.get(i), tips = Estado.partidaTipos.get(i);
            List<Double> mons = Estado.partidaMontos.get(i);
            tGDeb += Asientos.totalLado(tips, mons, "Debito"); tGHab += Asientos.totalLado(tips, mons, "Credito");
            System.out.println("Asiento #" + (i + 1) + (Asientos.esCierre(i) ? " (cierre)" : ""));
            for (int j = 0; j < cods.size(); j++) if (tips.get(j).equalsIgnoreCase("Debito")) mostrarLineaDiario(cods.get(j), mons.get(j), false);
            for (int j = 0; j < cods.size(); j++) if (tips.get(j).equalsIgnoreCase("Credito")) mostrarLineaDiario(cods.get(j), mons.get(j), true);
            System.out.printf("%-10s %s%n", Estado.asientoFechas.get(i), Estado.asientoDescripciones.get(i));
            System.out.println("-".repeat(92));
        }
        System.out.println("=".repeat(92));
        System.out.printf("%-66s %12.2f %12.2f%n", "TOTAL GENERAL", tGDeb, tGHab);
        System.out.println("Total de asientos: " + idx.size());
    }

    static void mostrarLineaDiario(String cod, double monto, boolean credito) {
        String det = Formato.recortar((credito ? "A " : "") + Cuentas.nombreCuenta(cod), 42);
        String d = credito ? "" : String.format("%12.2f", monto), h = credito ? String.format("%12.2f", monto) : "";
        System.out.printf("%-10s %-42s %12s %12s %12s%n", cod, det, "", d, h);
    }

    static void verLibroMayor() {
        Consola.linea("\n=== LIBRO MAYOR ===",
                Estado.descripcionRango(),
                "Movimientos de una cuenta en orden de fecha y su saldo acumulado.",
                "(As. = numero de asiento)");
        String cod = Cuentas.pedirCuenta("Cuenta", "cancelar");
        if (cod == null) { System.out.println("Cancelado."); return; }
        int idx = Cuentas.indiceCuenta(cod); String tipoCta = Estado.cuentaTipos.get(idx);
        double salIni = Asientos.saldoDebitoAntes(cod, Estado.vistaInicio());
        if (!Asientos.tipoDeudor(tipoCta)) salIni = -salIni;
        System.out.println("\nCUENTA: " + Estado.cuentaCodigos.get(idx) + " - " + Estado.cuentaNombres.get(idx));
        System.out.println("Tipo: " + tipoCta);
        List<Integer> aMov = new ArrayList<>(), pMov = new ArrayList<>();
        Asientos.cargarMovimientosCuenta(cod, aMov, pMov);
        System.out.printf("Saldo inicial: %.2f (%s)%n", Math.abs(salIni), etiquetaSaldo(salIni, tipoCta));
        if (aMov.isEmpty() && Math.abs(salIni) <= Estado.TOLERANCIA_SALDO) { System.out.println("Sin movimientos."); return; }
        System.out.printf("%-5s %-12s %-24s %-8s %12s %12s%n", "As.", "Fecha", "Concepto", "Lado", "Monto", "Saldo");
        System.out.println("-".repeat(78));
        double sal = salIni;
        for (int k = 0; k < aMov.size(); k++) {
            int i = aMov.get(k), j = pMov.get(k);
            String tipo = Estado.partidaTipos.get(i).get(j); double monto = Estado.partidaMontos.get(i).get(j);
            sal += Asientos.efectoSaldo(tipoCta, tipo, monto);
            System.out.printf("%-5d %-12s %-24s %-8s %12.2f %12.2f%n",
                    i + 1, Estado.asientoFechas.get(i), Formato.recortar(Estado.asientoDescripciones.get(i), 24), Formato.etiquetaLado(tipo), monto, sal);
        }
        System.out.println("-".repeat(78));
        System.out.printf("Saldo final: %.2f (%s)%n", Math.abs(sal), etiquetaSaldo(sal, tipoCta));
    }

    static void verCuentaTIndividual() {
        Consola.linea("\n=== CUENTA T ===",
                "La cuenta T muestra los movimientos al Debe (izquierda) y al Haber (derecha).",
                "El saldo es Deudor si el Debe es mayor; Acreedor si el Haber es mayor.");
        String cod = Cuentas.pedirCuenta("Cuenta", "cancelar");
        if (cod == null) { System.out.println("Cancelado."); return; }
        mostrarCuentaT(Cuentas.indiceCuenta(cod));
    }

    static void verCuentasTConMovimientos() {
        System.out.println("\n=== CUENTAS T CON MOVIMIENTOS ===");
        int n = 0;
        for (int i = 0; i < Estado.cuentaCodigos.size(); i++)
            if (cuentaTieneMovimientos(i)) { mostrarCuentaT(i); n++; }
        if (n == 0) System.out.println("Sin movimientos.");
        else System.out.println("\nTotal: " + n);
    }

    static boolean cuentaTieneMovimientos(int idx) {
        List<Integer> a = new ArrayList<>(), p = new ArrayList<>();
        Asientos.cargarMovimientosCuenta(Estado.cuentaCodigos.get(idx), a, p);
        return !a.isEmpty();
    }

    static void mostrarCuentaT(int idxCta) {
        if (idxCta < 0 || idxCta >= Estado.cuentaCodigos.size()) return;
        String cod = Estado.cuentaCodigos.get(idxCta), tipoCta = Estado.cuentaTipos.get(idxCta);
        List<Integer> aMov = new ArrayList<>(), pMov = new ArrayList<>();
        Asientos.cargarMovimientosCuenta(cod, aMov, pMov);
        double salIni = Asientos.saldoDebitoAntes(cod, Estado.vistaInicio());
        double salIniNorm = Asientos.tipoDeudor(tipoCta) ? salIni : -salIni;
        List<String> fDeb = new ArrayList<>(), fHab = new ArrayList<>();
        double tDeb = 0, tHab = 0;
        for (int k = 0; k < aMov.size(); k++) {
            int i = aMov.get(k), j = pMov.get(k);
            double monto = Estado.partidaMontos.get(i).get(j);
            String fila = String.format("%-10s %-22s %12.2f", Estado.asientoFechas.get(i), Formato.recortar(Estado.asientoDescripciones.get(i), 22), monto);
            if (Estado.partidaTipos.get(i).get(j).equalsIgnoreCase("Debito")) { fDeb.add(fila); tDeb += monto; }
            else { fHab.add(fila); tHab += monto; }
        }
        System.out.println("\n" + "=".repeat(95));
        System.out.printf(" CUENTA T: %s - %s (%s)%n", cod, Estado.cuentaNombres.get(idxCta), tipoCta);
        System.out.printf(" Vista: %s | Saldo inicial: %.2f (%s)%n", Estado.descripcionRango(), Math.abs(salIni), etiquetaSaldo(salIniNorm, tipoCta));
        System.out.println(" Izquierda = Debe, derecha = Haber.");
        System.out.println("=".repeat(95));
        System.out.printf("%-10s %-22s %12s | %-10s %-22s %12s%n", "Fecha", "Concepto", "Monto", "Fecha", "Concepto", "Monto");
        System.out.println("-".repeat(95));
        int filas = Math.max(fDeb.size(), fHab.size());
        if (filas == 0) System.out.println("Sin movimientos.");
        else for (int f = 0; f < filas; f++) {
            String izq = f < fDeb.size() ? fDeb.get(f) : "", der = f < fHab.size() ? fHab.get(f) : "";
            System.out.printf("%-46s | %-46s%n", izq, der);
        }
        System.out.println("-".repeat(95));
        System.out.printf("%-10s %-22s %12.2f | %-10s %-22s %12.2f%n", "TOTAL DEBE", "", tDeb, "TOTAL HABER", "", tHab);
        double sal = salIni + tDeb - tHab;
        double salNorm = Asientos.tipoDeudor(tipoCta) ? sal : -sal;
        System.out.printf("%63s SALDO: %.2f (%s)%n", "", Math.abs(sal), etiquetaSaldo(salNorm, tipoCta));
        if (!Asientos.esCuentaContraria(cod) && salNorm < -Estado.TOLERANCIA_SALDO)
            System.out.println("  AVISO: saldo en el lado contrario al normal para una cuenta de tipo " + tipoCta + ".");
        System.out.println("=".repeat(95));
    }

    static String etiquetaSaldo(double s, String tipoCta) {
        if (Math.abs(s) <= Estado.TOLERANCIA_SALDO) return "Cero";
        return (s > 0) == Asientos.tipoDeudor(tipoCta) ? "Deudor" : "Acreedor";
    }

    static void verEstadoResultados() {
        Consola.linea("\n=== ESTADO DE RESULTADOS ===",
                Estado.descripcionRango(),
                "Ingresos - Costos - Gastos = Utilidad (o perdida).");
        System.out.printf("%-8s %-35s %12s%n", "Codigo", "Nombre", "Saldo");
        System.out.println("-".repeat(60));
        saldoContrarioEnReporte = false;
        List<Double> saldos = Asientos.saldosRango(Estado.vistaInicio(), Estado.vistaFin());
        double tIng = mostrarSeccion("Ingreso", "INGRESOS", saldos);
        double tCos = mostrarSeccion("Costo", "COSTOS", saldos), tGas = mostrarSeccion("Gasto", "GASTOS", saldos);
        double uBruta = tIng - tCos, uNeta = uBruta - tGas;
        System.out.println("\n" + "-".repeat(60));
        System.out.printf("Utilidad bruta:  %.2f%n", uBruta);
        System.out.printf("Utilidad neta:   %.2f%n", uNeta);
        System.out.printf("Resultado:       %.2f (%s)%n", Math.abs(uNeta),
                Math.abs(uNeta) <= Estado.TOLERANCIA ? "Cero" : uNeta > 0 ? "Utilidad" : "Perdida");
        mostrarNotaSaldosContrarios();
    }

    static void generarBalance() {
        String fin = Estado.vistaFin();
        Consola.linea("\n=== BALANCE GENERAL ===",
                Estado.descripcionRango(),
                "Activo = Pasivo + Patrimonio + Resultado.",
                Estado.rangoDesde.isEmpty() ? "Generado: " + Fechas.fechaActual() : "Corte: " + fin);
        System.out.printf("%-8s %-35s %12s%n", "Codigo", "Nombre", "Saldo");
        System.out.println("-".repeat(60));
        saldoContrarioEnReporte = false;
        List<Double> saldos = Asientos.saldosRango(Fechas.FECHA_MIN, fin);
        double tAct = mostrarSeccion("Activo", "ACTIVO", saldos);
        double tPas = mostrarSeccion("Pasivo", "PASIVO", saldos), tPat = mostrarSeccion("Patrimonio", "PATRIMONIO", saldos);
        double resultado = Asientos.resultadoDeSaldos(saldos), patConRes = tPat + resultado;
        System.out.println("\n-- RESULTADO --");
        if (Estado.rangoDesde.isEmpty()) {
            System.out.printf("%-44s %12.2f%n", "Resultado acumulado:", resultado);
        } else {
            double resRango = Asientos.resultadoDeSaldos(Asientos.saldosRango(Estado.vistaInicio(), Estado.vistaFin()));
            System.out.printf("%-44s %12.2f%n", "Resultado acumulado:", resultado);
            System.out.printf("%-44s %12.2f%n", "Resultado del rango (" + Estado.rangoDesde + " a " + Estado.rangoHasta + "):", resRango);
        }
        System.out.println("\n" + "-".repeat(60));
        System.out.printf("Total Pasivo + Patrimonio + Resultado: %.2f%n", tPas + patConRes);
        System.out.printf("Diferencia: %.2f%n", tAct - (tPas + patConRes));
        if (Asientos.cuadra(tAct, tPas + patConRes)) System.out.println("EL BALANCE CUADRA CORRECTAMENTE.");
        else { System.out.println("ADVERTENCIA: NO cuadra."); System.out.printf("Diferencia: %.2f%n", tAct - (tPas + patConRes)); }
        mostrarNotaSaldosContrarios();
        if (!Persistencia.guardarBalance(Estado.rangoDesde.isEmpty() ? Fechas.fechaActual() : fin, tAct, tPas, patConRes))
            System.out.println("ADVERTENCIA: no se guardo " + Persistencia.ARCHIVO_BALANCE + ".");
    }

    static double mostrarSeccion(String tipo, String titulo, List<Double> saldos) {
        System.out.println("\n-- " + titulo + " --");
        double total = 0;
        for (int i = 0; i < Estado.cuentaCodigos.size(); i++) {
            if (!Estado.cuentaTipos.get(i).equals(tipo)) continue;
            double s = saldos.get(i);
            String marca = "";
            if (!Asientos.esCuentaContraria(Estado.cuentaCodigos.get(i)) && s < -Estado.TOLERANCIA_SALDO) { marca = " *"; saldoContrarioEnReporte = true; }
            System.out.printf("%-8s %-35s %12.2f%s%n", Estado.cuentaCodigos.get(i), Estado.cuentaNombres.get(i), s, marca);
            total += s;
        }
        System.out.printf("%-44s %12.2f%n", "TOTAL " + titulo + ":", total);
        return total;
    }

    static void mostrarNotaSaldosContrarios() {
        if (!saldoContrarioEnReporte) return;
        Consola.linea("\n* Saldo en el lado contrario al normal. Verifique si es cuenta de naturaleza contraria o error.");
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
