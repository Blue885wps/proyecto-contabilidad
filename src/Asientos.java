import java.util.*;

public class Asientos {
    static boolean cuadra(double debe, double haber) { return Math.abs(debe - haber) <= Datos.TOLERANCIA + Datos.EPSILON; }

    static boolean tipoDeudor(String tipoCuenta) {
        return tipoCuenta.equals("Activo") || tipoCuenta.equals("Costo") || tipoCuenta.equals("Gasto");
    }

    static boolean esNaturalezaAcreedora(int idxCuenta) {
        return Datos.cuentaNaturalezas.get(idxCuenta).equals("Acreedora");
    }

    static boolean esNaturalezaMixta(int idxCuenta) {
        return Datos.cuentaNaturalezas.get(idxCuenta).equals("Mixta");
    }

    static double efectoSaldoNaturaleza(int idxCuenta, String tipoPartida, double monto) {
        boolean debito = tipoPartida.equalsIgnoreCase("Debito");
        if (esNaturalezaAcreedora(idxCuenta)) return debito ? -monto : monto;
        if (esNaturalezaMixta(idxCuenta))
            return tipoDeudor(Datos.cuentaTipos.get(idxCuenta)) == debito ? monto : -monto;
        return debito ? monto : -monto;
    }

    static double saldoNaturaleza(int idxCuenta, double saldoTipo) {
        if (esNaturalezaMixta(idxCuenta)) return saldoTipo;
        double saldoDebito = tipoDeudor(Datos.cuentaTipos.get(idxCuenta)) ? saldoTipo : -saldoTipo;
        return esNaturalezaAcreedora(idxCuenta) ? -saldoDebito : saldoDebito;
    }

    static List<Double> saldosNaturaleza() {
        List<Double> saldosTipo = saldos();
        List<Double> resultado = new ArrayList<>();
        for (int i = 0; i < saldosTipo.size(); i++) resultado.add(saldoNaturaleza(i, saldosTipo.get(i)));
        return resultado;
    }

    static String etiquetaSaldoNaturaleza(double saldo, String naturaleza) {
        if (Math.abs(saldo) <= Datos.TOLERANCIA) return "Cero";
        if (naturaleza.equals("Deudora")) return saldo > 0 ? "Deudor" : "Acreedor";
        if (naturaleza.equals("Acreedora")) return saldo > 0 ? "Acreedor" : "Deudor";
        return saldo > 0 ? "Deudor" : "Acreedor";
    }

    static boolean esSaldoContrario(int idxCuenta, double saldoNaturaleza) {
        return !esNaturalezaMixta(idxCuenta) && saldoNaturaleza < -Datos.TOLERANCIA;
    }

    static String etiquetaSaldoCuenta(int idxCuenta, double saldoNaturaleza) {
        if (Math.abs(saldoNaturaleza) <= Datos.TOLERANCIA) return "Cero";
        if (!esNaturalezaMixta(idxCuenta))
            return etiquetaSaldoNaturaleza(saldoNaturaleza, Datos.cuentaNaturalezas.get(idxCuenta));
        boolean deudor = tipoDeudor(Datos.cuentaTipos.get(idxCuenta)) == (saldoNaturaleza > 0);
        return deudor ? "Deudor" : "Acreedor";
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
        System.out.println("Se pediran 3 datos: fecha, descripcion y partidas.");
        System.out.println("Cada asiento necesita al menos 2 partidas y el total del Debe debe ser igual al del Haber.");
        System.out.println("Paso 1 de 3: fecha del asiento.");
        String fecha = Contabilidad.leerFecha("Fecha del asiento");
        if (fecha == null) { System.out.println("Asiento cancelado."); return; }
        System.out.println("Paso 2 de 3: descripcion.");
        String descripcion = Contabilidad.leerTextoPersistible("Descripcion (ej: Compra de papeleria): ", "descripcion");
        if (Contabilidad.entradaCerrada || descripcion == null) { System.out.println("Asiento cancelado."); return; }
        System.out.println("Paso 3 de 3: partidas.");
        System.out.println("Para cada partida indique cuenta, lado (Debe/Haber) y monto.");
        List<String> codigos = new ArrayList<>(), tipos = new ArrayList<>();
        List<Double> montos = new ArrayList<>();
        double[] totales = {0, 0};
        List<Double> saldosLista = saldosNaturaleza();
        double[] saldos = new double[saldosLista.size()];
        for (int i = 0; i < saldosLista.size(); i++) saldos[i] = saldosLista.get(i);
        while (true) {
            if (!pedirPartida(codigos, tipos, montos, totales, saldos)) { System.out.println("Asiento cancelado."); return; }
            if (codigos.size() < 2) {
                System.out.println("Faltan partidas: el asiento necesita al menos 2. Ingrese la siguiente partida.");
                continue;
            }
            if (!cuadra(totales[0], totales[1])) {
                System.out.println("El asiento no cuadra todavia.");
                System.out.printf("Debe %.2f | Haber %.2f | Diferencia %.2f.%n", totales[0], totales[1], Math.abs(totales[0] - totales[1]));
                System.out.println("Debe agregar otra partida para igualar el Debe y el Haber. Si no, el asiento se descarta.");
                if (!Contabilidad.confirmar("Desea agregar otra partida para cuadrarlo? (S/N): ")) {
                    System.out.println("Asiento cancelado. No se guardo ningun dato.");
                    return;
                }
                continue;
            }
            if (!Contabilidad.confirmar("El asiento esta cuadrado. Desea agregar otra partida? (S/N): ")) break;
        }
        System.out.println("\n--- REVISION DEL ASIENTO ---");
        System.out.println("Revise fecha, descripcion, partidas y totales. Si algo esta mal, responda N para descartarlo completo.");
        mostrarPartidas(codigos, tipos, montos);
        System.out.println("-".repeat(70));
        System.out.printf("%-54s %12.2f%n", "TOTAL DEBE:", totales[0]);
        System.out.printf("%-54s %12.2f%n", "TOTAL HABER:", totales[1]);
        if (!Contabilidad.confirmar("Los datos son correctos. Guardar el asiento? (S/N): ")) {
            System.out.println("Asiento cancelado. No se guardo ningun dato.");
            return;
        }
        agregarAsiento(fecha, descripcion, codigos, tipos, montos, totales[0]);
        recalcularSaldos();
        if (!Datos.guardarAsientos()) {
            quitarUltimoAsiento(); recalcularSaldos();
            System.out.println("No se pudo guardar el asiento. No se modifico el archivo."); return;
        }
        if (!Datos.guardarCuentas()) System.out.println("Asiento guardado, pero no se actualizo " + Datos.ARCHIVO_CUENTAS + ".");
        System.out.println("Asiento registrado exitosamente.");
    }

    static boolean pedirPartida(List<String> codigos, List<String> tipos, List<Double> montos, double[] totales, double[] saldos) {
        while (true) {
            String codigo = Datos.pedirCuenta("Cuenta de la partida " + (codigos.size() + 1));
            if (codigo == null) return false;
            int indice = Datos.indiceCuenta(codigo);
            String naturaleza = Datos.cuentaNaturalezas.get(indice);
            String tipo = pedirTipoPartida(Datos.cuentaTipos.get(indice), Datos.cuentaNombres.get(indice), naturaleza);
            if (tipo == null) return false;
            double monto = Contabilidad.leerDouble("Monto de la partida " + (codigos.size() + 1) + " (0.01 o mas, hasta 2 decimales): ");
            if (Contabilidad.entradaCerrada) return false;
            double actual = saldos[indice];
            double efecto = efectoSaldoNaturaleza(indice, tipo, monto);
            double resultante = actual + efecto;
            if (!esNaturalezaMixta(indice) && resultante < -Datos.TOLERANCIA) {
                System.out.println("PARTIDA RECHAZADA: la cuenta " + codigo + " - " + Datos.cuentaNombres.get(indice) + " es " + naturaleza + ".");
                System.out.printf("  Saldo actual: %.2f (%s).%n", Math.abs(actual), etiquetaSaldoNaturaleza(actual, naturaleza));
                System.out.printf("  Monto al %s: %.2f. Saldo resultante: %.2f (%s).%n", Datos.etiquetaLado(tipo), monto,
                        Math.abs(resultante), etiquetaSaldoNaturaleza(resultante, naturaleza));
                System.out.println("  No se permite que una cuenta " + naturaleza + " quede con saldo contrario.");
                System.out.println("  La partida no se agrego. Elija otra cuenta, el lado contrario o un monto menor.");
                continue;
            }
            codigos.add(codigo); tipos.add(tipo); montos.add(monto);
            saldos[indice] = resultante;
            if (tipo.equals("Debito")) totales[0] += monto; else totales[1] += monto;
            System.out.printf("  Partida agregada: %s - %s, %s %.2f.%n", codigo, Datos.cuentaNombres.get(indice), Datos.etiquetaLado(tipo), monto);
            System.out.printf("  Acumulado del asiento: Debe %.2f | Haber %.2f.%n", totales[0], totales[1]);
            if (!cuadra(totales[0], totales[1]))
                System.out.printf("  Para cuadrar faltan %.2f en el %s.%n", Math.abs(totales[0] - totales[1]), totales[0] > totales[1] ? "Haber" : "Debe");
            else System.out.println("  El asiento esta cuadrado: Debe = Haber.");
            return true;
        }
    }

    static String pedirTipoPartida(String tipoCuenta, String nombreCuenta, String naturaleza) {
        System.out.println("Lado de la partida para " + nombreCuenta + " (" + tipoCuenta + ", " + naturaleza + "):");
        System.out.println("  1. Debe  (debito)");
        System.out.println("  2. Haber (credito)");
        System.out.println("  " + pistaNaturaleza(naturaleza));
        int opcion = Contabilidad.leerInt("Seleccione el lado (1 o 2): ", 1, 2);
        if (Contabilidad.entradaCerrada) return null;
        return opcion == 1 ? "Debito" : "Credito";
    }

    static String pistaNaturaleza(String naturaleza) {
        if (naturaleza.equals("Deudora")) return "Esta cuenta aumenta con el Debe y disminuye con el Haber.";
        if (naturaleza.equals("Acreedora")) return "Esta cuenta aumenta con el Haber y disminuye con el Debe.";
        return "Esta cuenta es Mixta: puede recibir partidas al Debe o al Haber.";
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
        System.out.println("Se muestran todos los asientos con su numero, fecha, descripcion y partidas.");
        if (Datos.asientoFechas.isEmpty()) { System.out.println("No hay asientos registrados."); return; }
        for (int i = 0; i < Datos.asientoFechas.size(); i++) {
            System.out.println("\nAsiento #" + (i + 1));
            System.out.println("Fecha: " + Datos.asientoFechas.get(i));
            System.out.println("Descripcion: " + Datos.asientoDescripciones.get(i));
            mostrarPartidas(Datos.partidaCodigos.get(i), Datos.partidaTipos.get(i), Datos.partidaMontos.get(i));
            System.out.printf("  Total del asiento: %.2f%n", Datos.asientoTotales.get(i));
        }
    }

    static void mostrarPartidas(List<String> codigos, List<String> tipos, List<Double> montos) {
        System.out.printf("  %-6s %-30s %-9s %12s%n", "Codigo", "Nombre", "Lado", "Monto");
        for (int j = 0; j < codigos.size(); j++)
            System.out.printf("  %-6s %-30s %-9s %12.2f%n", codigos.get(j), Datos.nombreCuenta(codigos.get(j)), Datos.etiquetaLado(tipos.get(j)), montos.get(j));
    }

    static String validarSaldoContrarioEnCarga(List<String> codigos, List<String> tipos, List<Double> montos,
                                               double[] saldosCarga, List<String> cuentaNaturalezas, List<String> cuentaNombres) {
        for (int i = 0; i < codigos.size(); i++) {
            int idx = Datos.indiceCuenta(codigos.get(i));
            String naturaleza = cuentaNaturalezas.get(idx);
            double efecto = efectoSaldoNaturaleza(idx, tipos.get(i), montos.get(i));
            double resultante = saldosCarga[idx] + efecto;
            if (!naturaleza.equals("Mixta") && resultante < -Datos.TOLERANCIA) {
                return "saldo contrario en " + codigos.get(i) + " " + cuentaNombres.get(idx)
                        + ": saldo actual " + String.format("%.2f", saldosCarga[idx])
                        + ", monto " + String.format("%.2f", montos.get(i))
                        + ", resultante " + String.format("%.2f", resultante);
            }
        }
        return null;
    }
}
