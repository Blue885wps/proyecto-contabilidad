import java.util.*;

public class Asientos {
    static boolean cuadra(double debe, double haber) { return Math.abs(debe - haber) <= Datos.TOLERANCIA + Datos.EPSILON; }

    static boolean tipoDeudor(String tipoCuenta) {
        return tipoCuenta.equals("Activo") || tipoCuenta.equals("Costo") || tipoCuenta.equals("Gasto");
    }

    static double efectoSaldo(String tipoCuenta, String tipoPartida, double monto) {
        boolean debito = tipoPartida.equalsIgnoreCase("Debito");
        return tipoDeudor(tipoCuenta) ? (debito ? monto : -monto) : (debito ? -monto : monto);
    }

    static double totalLado(List<String> tipos, List<Double> montos, String lado) {
        double total = 0;
        for (int i = 0; i < tipos.size() && i < montos.size(); i++)
            if (tipos.get(i).equalsIgnoreCase(lado)) total += montos.get(i);
        return total;
    }

    static void agregarAsiento(String fecha, String descripcion, List<String> codigos, List<String> tipos, List<Double> montos, double total) {
        Datos.asientoFechas.add(fecha); Datos.asientoDescripciones.add(descripcion); Datos.asientoTotales.add(total);
        Datos.partidaCodigos.add(codigos); Datos.partidaTipos.add(tipos); Datos.partidaMontos.add(montos);
    }

    static void quitarUltimoAsiento() {
        int u = Datos.asientoFechas.size() - 1;
        if (u < 0) return;
        Datos.asientoFechas.remove(u); Datos.asientoDescripciones.remove(u); Datos.asientoTotales.remove(u);
        Datos.partidaCodigos.remove(u); Datos.partidaTipos.remove(u); Datos.partidaMontos.remove(u);
    }

    static void registrarAsiento() {
        if (Datos.datosInvalidos) {
            System.out.println("No se pueden registrar asientos: hay datos invalidos en los archivos.");
            System.out.println("Corrija los archivos y reinicie el programa.");
            return;
        }
        System.out.println("\n--- REGISTRAR ASIENTO ---");
        System.out.println("Indique fecha, descripcion y al menos dos partidas. Debe = Haber.");
        String fecha = Contabilidad.leerFecha("Fecha del asiento");
        if (fecha == null) { System.out.println("Asiento cancelado."); return; }
        String descripcion = Contabilidad.leerTextoPersistible("Descripcion: ", "descripcion");
        if (Contabilidad.entradaCerrada || descripcion == null) { System.out.println("Asiento cancelado."); return; }
        List<String> codigos = new ArrayList<>(), tipos = new ArrayList<>();
        List<Double> montos = new ArrayList<>();
        double[] totales = {0, 0};
        while (true) {
            if (!pedirPartida(codigos, tipos, montos, totales)) { System.out.println("Asiento cancelado."); return; }
            if (codigos.size() < 2) { System.out.println("Minimo 2 partidas. Ingrese la segunda."); continue; }
            if (!cuadra(totales[0], totales[1])) {
                System.out.printf("El asiento no cuadra: Debe %.2f | Haber %.2f | Diferencia %.2f%n", totales[0], totales[1], Math.abs(totales[0] - totales[1]));
                if (!Contabilidad.confirmar("Agregar otra partida para cuadrar? (S/N): ")) { System.out.println("Asiento cancelado."); return; }
                continue;
            }
            if (!Contabilidad.confirmar("Agregar otra partida? (S/N): ")) break;
        }
        System.out.println("\n--- RESUMEN ---");
        System.out.println("Fecha: " + fecha);
        System.out.println("Descripcion: " + descripcion);
        mostrarPartidas(codigos, tipos, montos);
        System.out.println("-".repeat(70));
        System.out.printf("%-54s %12.2f%n", "TOTAL DEBE:", totales[0]);
        System.out.printf("%-54s %12.2f%n", "TOTAL HABER:", totales[1]);
        if (!Contabilidad.confirmar("Guardar? (S/N): ")) { System.out.println("Asiento cancelado."); return; }
        agregarAsiento(fecha, descripcion, codigos, tipos, montos, totales[0]);
        recalcularSaldos();
        if (!Datos.guardarAsientos()) {
            quitarUltimoAsiento(); recalcularSaldos();
            System.out.println("Error guardando."); return;
        }
        if (!Datos.guardarCuentas()) System.out.println("Asiento guardado, pero no se actualizo " + Datos.ARCHIVO_CUENTAS + ".");
        System.out.println("Asiento registrado exitosamente.");
    }

    static boolean pedirPartida(List<String> codigos, List<String> tipos, List<Double> montos, double[] totales) {
        String codigo = Datos.pedirCuenta("Cuenta de la partida " + (codigos.size() + 1));
        if (codigo == null) return false;
        int indice = Datos.indiceCuenta(codigo);
        String tipo = pedirTipoPartida(Datos.cuentaTipos.get(indice), Datos.cuentaNombres.get(indice));
        if (tipo == null) return false;
        double monto = Contabilidad.leerDouble("  Monto: ");
        if (Contabilidad.entradaCerrada) return false;
        codigos.add(codigo); tipos.add(tipo); montos.add(monto);
        if (tipo.equals("Debito")) totales[0] += monto; else totales[1] += monto;
        System.out.printf("  Acumulado: Debe %.2f | Haber %.2f%n", totales[0], totales[1]);
        if (!cuadra(totales[0], totales[1]))
            System.out.printf("  Para cuadrar faltan %.2f en el %s.%n", Math.abs(totales[0] - totales[1]), totales[0] > totales[1] ? "Haber" : "Debe");
        else System.out.println("  El asiento esta cuadrado: Debe = Haber.");
        return true;
    }

    static String pedirTipoPartida(String tipoCuenta, String nombreCuenta) {
        System.out.println("Lado de la partida para " + nombreCuenta + " (" + tipoCuenta + "):");
        System.out.println("  1. Debe (debito)   2. Haber (credito)");
        System.out.println("  " + (tipoDeudor(tipoCuenta) ? "Debe aumenta | Haber disminuye" : "Debe disminuye | Haber aumenta"));
        int opcion = Contabilidad.leerInt("Seleccione 1 o 2: ", 1, 2);
        if (Contabilidad.entradaCerrada) return null;
        return opcion == 1 ? "Debito" : "Credito";
    }

    static double[][] movimientos() {
        int n = Datos.cuentaCodigos.size();
        double[] debitos = new double[n], creditos = new double[n];
        for (int a = 0; a < Datos.asientoFechas.size(); a++) {
            List<String> codigos = Datos.partidaCodigos.get(a), tipos = Datos.partidaTipos.get(a);
            List<Double> montos = Datos.partidaMontos.get(a);
            for (int p = 0; p < codigos.size(); p++) {
                int i = Datos.indiceCuenta(codigos.get(p));
                if (i < 0) continue;
                if (tipos.get(p).equalsIgnoreCase("Debito")) debitos[i] += montos.get(p); else creditos[i] += montos.get(p);
            }
        }
        return new double[][] {debitos, creditos};
    }

    static List<Double> saldos() {
        double[][] mov = movimientos();
        List<Double> saldos = new ArrayList<>();
        for (int i = 0; i < Datos.cuentaCodigos.size(); i++)
            saldos.add(tipoDeudor(Datos.cuentaTipos.get(i)) ? mov[0][i] - mov[1][i] : mov[1][i] - mov[0][i]);
        return saldos;
    }

    static void recalcularSaldos() {
        List<Double> saldos = saldos();
        for (int i = 0; i < Datos.cuentaSaldos.size(); i++) Datos.cuentaSaldos.set(i, saldos.get(i));
    }

    static double resultadoDeSaldos(List<Double> saldos) {
        double total = 0;
        for (int i = 0; i < Datos.cuentaTipos.size() && i < saldos.size(); i++) {
            String tipo = Datos.cuentaTipos.get(i);
            if (tipo.equals("Ingreso")) total += saldos.get(i);
            else if (tipo.equals("Costo") || tipo.equals("Gasto")) total -= saldos.get(i);
        }
        return total;
    }

    static void ordenarMovimientos(List<Integer> asientos, List<Integer> partidas) {
        for (int i = 0; i < asientos.size() - 1; i++)
            for (int j = 0; j < asientos.size() - 1 - i; j++)
                if (Datos.fechaOrdenable(Datos.asientoFechas.get(asientos.get(j + 1))).compareTo(Datos.fechaOrdenable(Datos.asientoFechas.get(asientos.get(j)))) < 0) {
                    int aux = asientos.get(j); asientos.set(j, asientos.get(j + 1)); asientos.set(j + 1, aux);
                    if (partidas != null) { int auxPartida = partidas.get(j); partidas.set(j, partidas.get(j + 1)); partidas.set(j + 1, auxPartida); }
                }
    }

    static void cargarMovimientosCuenta(String codigo, List<Integer> asientos, List<Integer> partidas) {
        asientos.clear(); partidas.clear();
        for (int i = 0; i < Datos.asientoFechas.size(); i++) {
            List<String> codigos = Datos.partidaCodigos.get(i);
            for (int j = 0; j < codigos.size(); j++)
                if (codigos.get(j).equals(codigo)) { asientos.add(i); partidas.add(j); }
        }
        ordenarMovimientos(asientos, partidas);
    }

    static void mostrarAsientos() {
        System.out.println("\n=== ASIENTOS REGISTRADOS ===");
        if (Datos.asientoFechas.isEmpty()) { System.out.println("No hay asientos."); return; }
        for (int i = 0; i < Datos.asientoFechas.size(); i++) {
            System.out.println("\nAsiento #" + (i + 1));
            System.out.println("Fecha: " + Datos.asientoFechas.get(i));
            System.out.println("Descripcion: " + Datos.asientoDescripciones.get(i));
            mostrarPartidas(Datos.partidaCodigos.get(i), Datos.partidaTipos.get(i), Datos.partidaMontos.get(i));
            System.out.printf("  Total: %.2f%n", Datos.asientoTotales.get(i));
        }
    }

    static void mostrarPartidas(List<String> codigos, List<String> tipos, List<Double> montos) {
        System.out.printf("  %-6s %-30s %-9s %12s%n", "Codigo", "Nombre", "Lado", "Monto");
        for (int j = 0; j < codigos.size(); j++)
            System.out.printf("  %-6s %-30s %-9s %12.2f%n", codigos.get(j), Datos.nombreCuenta(codigos.get(j)), Datos.etiquetaLado(tipos.get(j)), montos.get(j));
    }
}
