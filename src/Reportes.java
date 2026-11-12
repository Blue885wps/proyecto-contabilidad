import java.util.*;

public class Reportes {
    static void verCuentaT() {
        System.out.println("\n=== CUENTA T ===");
        System.out.println("Seleccione una cuenta para ver sus cargos al Debe y sus abonos al Haber.");
        System.out.println("Puede escribir el codigo exacto o parte del nombre.");
        String codigo = Datos.pedirCuenta("Cuenta a consultar");
        if (codigo == null) { System.out.println("Consulta cancelada."); return; }
        mostrarCuentaT(Datos.indiceCuenta(codigo));
    }

    static void mostrarCuentaT(int idxCta) {
        if (idxCta < 0 || idxCta >= Datos.cuentaCodigos.size()) return;
        String codigo = Datos.cuentaCodigos.get(idxCta), tipo = Datos.cuentaTipos.get(idxCta);
        String naturaleza = Datos.cuentaNaturalezas.get(idxCta);
        List<Integer> asientos = new ArrayList<>(), partidas = new ArrayList<>();
        Asientos.cargarMovimientosCuenta(codigo, asientos, partidas);
        List<String> filasDebe = new ArrayList<>(), filasHaber = new ArrayList<>();
        double totalDebe = 0, totalHaber = 0;
        for (int k = 0; k < asientos.size(); k++) {
            int i = asientos.get(k), j = partidas.get(k);
            double monto = Datos.partidaMontos.get(i).get(j);
            String fila = String.format("%-10s %-22s %12.2f", Datos.asientoFechas.get(i), Datos.recortar(Datos.asientoDescripciones.get(i), 22), monto);
            if (Datos.partidaTipos.get(i).get(j).equalsIgnoreCase("Debito")) { filasDebe.add(fila); totalDebe += monto; }
            else { filasHaber.add(fila); totalHaber += monto; }
        }
        System.out.println("\n" + "=".repeat(95));
        System.out.printf(" CUENTA T: %s - %s%n", codigo, Datos.cuentaNombres.get(idxCta));
        System.out.println(" Tipo: " + tipo + " | Naturaleza: " + naturaleza);
        System.out.println(" La columna izquierda es el Debe y la derecha es el Haber.");
        System.out.println("=".repeat(95));
        System.out.printf("%-10s %-22s %12s | %-10s %-22s %12s%n", "Fecha", "Concepto", "Monto", "Fecha", "Concepto", "Monto");
        System.out.println("-".repeat(95));
        int filas = Math.max(filasDebe.size(), filasHaber.size());
        if (filas == 0) System.out.println("Esta cuenta no tiene movimientos.");
        else for (int f = 0; f < filas; f++)
            System.out.printf("%-46s | %-46s%n", f < filasDebe.size() ? filasDebe.get(f) : "", f < filasHaber.size() ? filasHaber.get(f) : "");
        System.out.println("-".repeat(95));
        System.out.printf("%-10s %-22s %12.2f | %-10s %-22s %12.2f%n", "TOTAL DEBE", "", totalDebe, "TOTAL HABER", "", totalHaber);
        double saldoDebito = totalDebe - totalHaber;
        double saldoNaturaleza = naturaleza.equals("Acreedora") ? -saldoDebito : saldoDebito;
        System.out.printf("%63s SALDO: %.2f (%s)%n", "", Math.abs(saldoDebito),
                Asientos.etiquetaSaldoNaturaleza(saldoNaturaleza, naturaleza));
        System.out.println("=".repeat(95));
    }

    static void verLibroMayor() {
        System.out.println("\n=== LIBRO MAYOR ===");
        System.out.println("Seleccione una cuenta para ver sus movimientos en orden de fecha y su saldo acumulado.");
        String codigo = Datos.pedirCuenta("Cuenta a consultar");
        if (codigo == null) { System.out.println("Consulta cancelada."); return; }
        int idx = Datos.indiceCuenta(codigo);
        String tipo = Datos.cuentaTipos.get(idx), naturaleza = Datos.cuentaNaturalezas.get(idx);
        List<Integer> asientos = new ArrayList<>(), partidas = new ArrayList<>();
        Asientos.cargarMovimientosCuenta(codigo, asientos, partidas);
        System.out.println("\nCUENTA: " + codigo + " - " + Datos.cuentaNombres.get(idx));
        System.out.println("Tipo: " + tipo + " | Naturaleza: " + naturaleza);
        if (asientos.isEmpty()) { System.out.println("Esta cuenta no tiene movimientos."); return; }
        System.out.printf("%-5s %-12s %-24s %-8s %12s %12s%n", "As.", "Fecha", "Concepto", "Lado", "Monto", "Saldo");
        System.out.println("-".repeat(78));
        double saldo = 0;
        for (int k = 0; k < asientos.size(); k++) {
            int i = asientos.get(k), j = partidas.get(k);
            String lado = Datos.partidaTipos.get(i).get(j);
            double monto = Datos.partidaMontos.get(i).get(j);
            saldo += Asientos.efectoSaldoNaturaleza(idx, lado, monto);
            System.out.printf("%-5d %-12s %-24s %-8s %12.2f %12.2f%n", i + 1, Datos.asientoFechas.get(i),
                    Datos.recortar(Datos.asientoDescripciones.get(i), 24), Datos.etiquetaLado(lado), monto, saldo);
        }
        System.out.println("-".repeat(78));
        System.out.printf("Saldo final: %.2f (%s)%n", Math.abs(saldo), Asientos.etiquetaSaldoNaturaleza(saldo, naturaleza));
    }

    static void verLibroDiario() {
        System.out.println("\n=== LIBRO DIARIO ===");
        System.out.println("Todas las operaciones en orden de fecha, con sus partidas al Debe y al Haber.");
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < Datos.asientoFechas.size(); i++) indices.add(i);
        if (indices.isEmpty()) { System.out.println("No hay asientos registrados."); return; }
        Asientos.ordenarMovimientos(indices, null);
        System.out.println();
        System.out.printf("%-10s %-42s %12s %12s %12s%n", "Cta", "Detalle", "Parcial", "Debe", "Haber");
        System.out.println("=".repeat(92));
        double totalGeneralDebe = 0, totalGeneralHaber = 0;
        for (int k = 0; k < indices.size(); k++) {
            int i = indices.get(k);
            List<String> codigos = Datos.partidaCodigos.get(i), tipos = Datos.partidaTipos.get(i);
            List<Double> montos = Datos.partidaMontos.get(i);
            totalGeneralDebe += Asientos.totalLado(tipos, montos, "Debito");
            totalGeneralHaber += Asientos.totalLado(tipos, montos, "Credito");
            System.out.println("Asiento #" + (i + 1));
            for (int j = 0; j < codigos.size(); j++)
                if (tipos.get(j).equalsIgnoreCase("Debito")) mostrarLineaDiario(codigos.get(j), montos.get(j), false);
            for (int j = 0; j < codigos.size(); j++)
                if (tipos.get(j).equalsIgnoreCase("Credito")) mostrarLineaDiario(codigos.get(j), montos.get(j), true);
            System.out.printf("%-10s %s%n", Datos.asientoFechas.get(i), Datos.asientoDescripciones.get(i));
            System.out.println("-".repeat(92));
        }
        System.out.println("=".repeat(92));
        System.out.printf("%-66s %12.2f %12.2f%n", "TOTAL GENERAL", totalGeneralDebe, totalGeneralHaber);
        System.out.println("Total de asientos: " + indices.size());
    }

    static void mostrarLineaDiario(String codigo, double monto, boolean credito) {
        String detalle = Datos.recortar((credito ? "A " : "") + Datos.nombreCuenta(codigo), 42);
        String debe = credito ? "" : String.format("%12.2f", monto), haber = credito ? String.format("%12.2f", monto) : "";
        System.out.printf("%-10s %-42s %12s %12s %12s%n", codigo, detalle, "", debe, haber);
    }

    static void verBalanceComprobacion() {
        System.out.println("\n=== BALANZA DE COMPROBACION ===");
        System.out.println("Muestra los cargos y abonos de cada cuenta con movimiento.");
        System.out.println("Si el total de Debitos es igual al total de Creditos, la balanza esta cuadrada.");
        System.out.printf("%-8s %-35s %12s %12s %12s %12s%n", "Codigo", "Nombre", "Debitos", "Creditos", "Deudor", "Acreedor");
        System.out.println("-".repeat(96));
        double[][] mov = Asientos.movimientos();
        double[] debitos = mov[0], creditos = mov[1];
        double totalDebe = 0, totalHaber = 0, totalDeudor = 0, totalAcreedor = 0;
        for (int i = 0; i < Datos.cuentaCodigos.size(); i++) {
            if (debitos[i] <= 0 && creditos[i] <= 0) continue;
            double saldo = debitos[i] - creditos[i], deudor = saldo > Datos.TOLERANCIA ? saldo : 0, acreedor = saldo < -Datos.TOLERANCIA ? -saldo : 0;
            System.out.printf("%-8s %-35s %12.2f %12.2f %12.2f %12.2f%n", Datos.cuentaCodigos.get(i), Datos.cuentaNombres.get(i), debitos[i], creditos[i], deudor, acreedor);
            totalDebe += debitos[i]; totalHaber += creditos[i]; totalDeudor += deudor; totalAcreedor += acreedor;
        }
        System.out.println("-".repeat(96));
        System.out.printf("%-44s %12.2f %12.2f %12.2f %12.2f%n", "TOTALES:", totalDebe, totalHaber, totalDeudor, totalAcreedor);
        System.out.println();
        if (Asientos.cuadra(totalDebe, totalHaber)) System.out.println("LA BALANZA DE COMPROBACION ESTA CUADRADA.");
        else { System.out.println("ADVERTENCIA: LA BALANZA NO CUADRA."); System.out.printf("Diferencia: %.2f%n", totalDebe - totalHaber); }
    }

    static void verEstadoResultados() {
        System.out.println("\n=== ESTADO DE RESULTADOS ===");
        System.out.println("Formula: Ingresos - Costos - Gastos = Utilidad neta.");
        System.out.println("Si la utilidad neta es negativa, el resultado es una perdida.");
        System.out.printf("%-8s %-35s %12s%n", "Codigo", "Nombre", "Saldo");
        System.out.println("-".repeat(60));
        List<Double> saldos = Asientos.saldos();
        double totalIngresos = mostrarSeccion("Ingreso", "INGRESOS", saldos);
        double totalCostos = mostrarSeccion("Costo", "COSTOS", saldos);
        double totalGastos = mostrarSeccion("Gasto", "GASTOS", saldos);
        double utilidadBruta = totalIngresos - totalCostos, utilidadNeta = utilidadBruta - totalGastos;
        System.out.println("\n" + "-".repeat(60));
        System.out.printf("Utilidad bruta:  %.2f%n", utilidadBruta);
        System.out.printf("Utilidad neta:   %.2f%n", utilidadNeta);
        System.out.printf("Resultado:       %.2f (%s)%n", Math.abs(utilidadNeta),
                Math.abs(utilidadNeta) <= Datos.TOLERANCIA ? "Cero" : utilidadNeta > 0 ? "Utilidad" : "Perdida");
    }

    static void generarBalance() {
        System.out.println("\n=== BALANCE GENERAL ===");
        System.out.println("Formula: Activo = Pasivo + Patrimonio + Resultado.");
        System.out.println("El resultado del ejercicio se suma al patrimonio para verificar la igualdad.");
        System.out.println("Generado: " + Datos.fechaActual());
        System.out.printf("%-8s %-35s %12s%n", "Codigo", "Nombre", "Saldo");
        System.out.println("-".repeat(60));
        List<Double> saldos = Asientos.saldos();
        double totalActivo = mostrarSeccion("Activo", "ACTIVO", saldos);
        double totalPasivo = mostrarSeccion("Pasivo", "PASIVO", saldos);
        double totalPatrimonio = mostrarSeccion("Patrimonio", "PATRIMONIO", saldos);
        double resultado = Asientos.resultadoDeSaldos(saldos);
        double patrimonioConResultado = totalPatrimonio + resultado;
        System.out.println("\n-- RESULTADO --");
        System.out.printf("%-44s %12.2f%n", "Resultado acumulado:", resultado);
        System.out.println("\n" + "-".repeat(60));
        System.out.printf("Total Pasivo + Patrimonio + Resultado: %.2f%n", totalPasivo + patrimonioConResultado);
        System.out.printf("Diferencia: %.2f%n", totalActivo - (totalPasivo + patrimonioConResultado));
        if (Asientos.cuadra(totalActivo, totalPasivo + patrimonioConResultado)) System.out.println("EL BALANCE CUADRA CORRECTAMENTE.");
        else { System.out.println("ADVERTENCIA: EL BALANCE NO CUADRA."); System.out.printf("Diferencia: %.2f%n", totalActivo - (totalPasivo + patrimonioConResultado)); }
        if (!Datos.guardarBalance(Datos.fechaActual(), totalActivo, totalPasivo, patrimonioConResultado))
            System.out.println("ADVERTENCIA: no se guardo " + Datos.ARCHIVO_BALANCE + ".");
    }

    static double mostrarSeccion(String tipo, String titulo, List<Double> saldos) {
        System.out.println("\n-- " + titulo + " --");
        double total = 0;
        for (int i = 0; i < Datos.cuentaCodigos.size(); i++) {
            if (!Datos.cuentaTipos.get(i).equals(tipo)) continue;
            System.out.printf("%-8s %-35s %12.2f%n", Datos.cuentaCodigos.get(i), Datos.cuentaNombres.get(i), saldos.get(i));
            total += saldos.get(i);
        }
        System.out.printf("%-44s %12.2f%n", "TOTAL " + titulo + ":", total);
        return total;
    }
}
