import java.util.*;

public class Asientos {
    static boolean cuadra(double debe, double haber) { return Math.abs(debe - haber) <= Estado.TOLERANCIA + Estado.EPSILON; }
    static boolean tipoDeudor(String tipoCuenta) { return tipoCuenta.equals("Activo") || tipoCuenta.equals("Costo") || tipoCuenta.equals("Gasto"); }
    static boolean esCuentaContraria(String codigo) {
        return codigo.equals("1790") || codigo.equals("1890") || codigo.equals("3120")
                || codigo.equals("3200") || codigo.equals("3500");
    }

    static void agregarAsiento(String fecha, String descripcion, List<String> cods,
            List<String> tips, List<Double> mons, double total, String tipo) {
        Estado.asientoFechas.add(fecha); Estado.asientoDescripciones.add(descripcion); Estado.asientoTotales.add(total);
        Estado.asientoTipos.add(tipo);
        Estado.partidaCodigos.add(cods); Estado.partidaTipos.add(tips); Estado.partidaMontos.add(mons);
    }

    static void quitarUltimoAsiento() {
        int ultimo = Estado.asientoFechas.size() - 1;
        if (ultimo < 0) return;
        Estado.asientoFechas.remove(ultimo); Estado.asientoDescripciones.remove(ultimo); Estado.asientoTotales.remove(ultimo);
        Estado.asientoTipos.remove(ultimo);
        Estado.partidaCodigos.remove(ultimo); Estado.partidaTipos.remove(ultimo); Estado.partidaMontos.remove(ultimo);
    }

    static void registrarAsiento() {
        if (Contabilidad.bloquearGuardadoAsientos || Contabilidad.bloquearGuardadoCuentas) {
            if (Contabilidad.bloquearGuardadoCuentas)
                Consola.linea("No se pueden registrar asientos: el catalogo o el guardado estan bloqueados.",
                        "Corrija los archivos de datos y reinicie el programa.");
            else
                System.out.println("No se pueden registrar asientos: corrija " + Contabilidad.ARCHIVO_ASIENTOS + " y reinicie.");
            return;
        }
        Consola.linea("\n--- REGISTRAR ASIENTO ---",
                "Una operacion contable registra un hecho economico (compra, venta o pago).",
                "Cada operacion tiene al menos dos lineas: una al Debe y otra al Haber,",
                "y ambas deben sumar el mismo monto.",
                "Ejemplo: compra de papeleria al contado",
                "  Debe  6020 Gastos de administracion  500,00",
                "  Haber 1000 Caja                      500,00",
                "Se pediran fecha, descripcion y las lineas del asiento.");
        String fecha;
        while (true) {
            fecha = Consola.leerFecha("Fecha del asiento");
            if (fecha == null) { System.out.println("Asiento cancelado. No se guardo nada."); return; }
            break; // ya no hay periodo que validar
        }
        String descripcion = Consola.leerTextoPersistible("Descripcion (ej: Compra de papeleria): ", "descripcion");
        if (Consola.entradaCerrada) return;
        List<String> cods = new ArrayList<>(), tips = new ArrayList<>();
        List<Double> mons = new ArrayList<>(), saldosSimulados = new ArrayList<>(saldosVista());
        double[] totales = {0, 0};
        while (true) {
            if (!pedirPartida(cods, tips, mons, saldosSimulados, totales)) { System.out.println("Asiento cancelado."); return; }
            if (cods.size() < 2) { System.out.println("Minimo 2 partidas. Ingrese la segunda."); continue; }
            boolean cuadrado = cuadra(totales[0], totales[1]);
            if (!cuadrado) {
                System.out.println("\nEl asiento no cuadra:");
                System.out.printf("  Total Debe:  %.2f%n  Total Haber: %.2f%n  Diferencia:  %.2f%n",
                        totales[0], totales[1], Math.abs(totales[0] - totales[1]));
                System.out.printf("  Para cuadrarlo, agregue una partida al %s por %.2f.%n",
                        totales[0] > totales[1] ? "Haber" : "Debe", Math.abs(totales[0] - totales[1]));
            }
            String pregunta = cuadrado ? "Agregar otra partida? (S/N): " : "Desea agregar otra partida para cuadrar? (S/N): ";
            if (Consola.confirmar(pregunta)) continue;
            if (!cuadrado) { System.out.println("Asiento cancelado."); return; }
            break;
        }
        if (totales[0] <= 0 || totales[1] <= 0) { System.out.println("Debe haber Debe y Haber."); return; }
        Consola.linea("\n--- RESUMEN ---", "Fecha: " + fecha, "Descripcion: " + descripcion);
        mostrarPartidas(cods, tips, mons);
        System.out.println("-".repeat(70));
        System.out.printf("%-54s %12.2f%n", "TOTAL DEBE:", totales[0]);
        System.out.printf("%-54s %12.2f%n", "TOTAL HABER:", totales[1]);
        if (!Consola.confirmar("Guardar? (S/N): ")) { System.out.println("Asiento cancelado. No se guardo nada."); return; }
        agregarAsiento(fecha, descripcion, cods, tips, mons, totales[0], "NORMAL");
        recalcularSaldos();
        if (!Contabilidad.guardarAsientos()) { quitarUltimoAsiento(); recalcularSaldos(); System.out.println("Error guardando."); return; }
        if (!Contabilidad.guardarCuentas()) System.out.println("Asiento guardado, pero no se actualizo " + Contabilidad.ARCHIVO_CUENTAS + ".");
        System.out.println("Asiento registrado exitosamente.");
    }

    static boolean pedirPartida(List<String> cods, List<String> tips, List<Double> mons,
            List<Double> saldosSimulados, double[] totales) {
        while (true) {
            String codigo = Cuentas.pedirCuenta("Cuenta de la partida " + (cods.size() + 1) + " (linea del asiento)", "cancelar");
            if (codigo == null) return false;
            int idx = Cuentas.indiceCuenta(codigo); String tipoCta = Estado.cuentaTipos.get(idx);
            String tipo = pedirTipoPartida(tipoCta, Estado.cuentaNombres.get(idx));
            double monto = Consola.leerDouble("  Monto (ej: 1500,00). Use coma o punto decimal: ");
            if (Consola.entradaCerrada) return false;
            double saldoAct = saldosSimulados.get(idx), nuevoSaldo = saldoAct + efectoSaldo(tipoCta, tipo, monto);
            if (!esCuentaContraria(codigo) && nuevoSaldo < -Estado.TOLERANCIA_SALDO) {
                System.out.println("\nAVISO: la cuenta quedaria con saldo en el lado contrario al normal.");
                System.out.printf("  %s - %s (%s) | Actual en el rango: %.2f | %s %.2f | Resultante: %.2f%n",
                        codigo, Estado.cuentaNombres.get(idx), tipoCta, saldoAct, Formato.etiquetaLado(tipo), monto, nuevoSaldo);
                if (!Consola.confirmar("Desea continuar? (S/N): ")) { System.out.println("Partida descartada."); continue; }
            }
            saldosSimulados.set(idx, nuevoSaldo);
            cods.add(codigo); tips.add(tipo); mons.add(monto);
            if (tipo.equalsIgnoreCase("Debito")) totales[0] += monto; else totales[1] += monto;
            System.out.printf("  Acumulado: Debe %.2f | Haber %.2f%n", totales[0], totales[1]);
            if (!cuadra(totales[0], totales[1]))
                System.out.printf("  Para cuadrar faltan %.2f en el %s.%n",
                        Math.abs(totales[0] - totales[1]), totales[0] > totales[1] ? "Haber" : "Debe");
            else
                System.out.println("  El asiento esta cuadrado: Debe = Haber.");
            return true;
        }
    }

    static String pedirTipoPartida(String tipoCuenta, String nombreCuenta) {
        Consola.linea("  Lado de la partida para " + nombreCuenta + " (" + tipoCuenta + "):");
        if (tipoCuenta.equals("Activo")) {
            Consola.linea("    1. Debe  (aumenta el saldo)", "    2. Haber (disminuye el saldo)");
        } else if (tipoCuenta.equals("Costo") || tipoCuenta.equals("Gasto")) {
            Consola.linea("    1. Debe  (aumenta el gasto o costo)", "    2. Haber (disminuye el gasto o costo)");
        } else {
            Consola.linea("    1. Debe  (disminuye el saldo)", "    2. Haber (aumenta el saldo)");
        }
        return Consola.leerInt("  Seleccione 1 o 2: ", 1, 2) == 1 ? "Debito" : "Credito";
    }

    static double efectoSaldo(String tipoCuenta, String tipoPartida, double monto) {
        boolean debito = tipoPartida.equalsIgnoreCase("Debito");
        return tipoDeudor(tipoCuenta) ? (debito ? monto : -monto) : (debito ? -monto : monto);
    }

    // ============================================================
    // CALCULOS DE SALDOS Y MOVIMIENTOS (core contable)
    // ============================================================
    static double[][] movimientosRango(String desde, String hasta) {
        double[] debitos = new double[Estado.cuentaCodigos.size()], creditos = new double[Estado.cuentaCodigos.size()];
        for (int a = 0; a < Estado.asientoFechas.size(); a++) {
            if (!Fechas.fechaEnRango(Estado.asientoFechas.get(a), desde, hasta)) continue;
            if (esCierre(a)) continue; // excluir cierres legacy
            List<String> cods = Estado.partidaCodigos.get(a), tips = Estado.partidaTipos.get(a);
            List<Double> mons = Estado.partidaMontos.get(a);
            for (int p = 0; p < cods.size(); p++) {
                int i = Cuentas.indiceCuenta(cods.get(p));
                if (i < 0) continue;
                if (tips.get(p).equalsIgnoreCase("Debito")) debitos[i] += mons.get(p); else creditos[i] += mons.get(p);
            }
        }
        return new double[][] {debitos, creditos};
    }

    static List<Double> saldosRango(String desde, String hasta) {
        double[][] mov = movimientosRango(desde, hasta);
        List<Double> saldos = new ArrayList<>();
        for (int i = 0; i < Estado.cuentaCodigos.size(); i++)
            saldos.add(tipoDeudor(Estado.cuentaTipos.get(i)) ? mov[0][i] - mov[1][i] : mov[1][i] - mov[0][i]);
        return saldos;
    }

    static List<Double> saldosVista() { return saldosRango(Estado.vistaInicio(), Estado.vistaFin()); }

    static double resultadoDeSaldos(List<Double> saldos) {
        double total = 0;
        for (int i = 0; i < Estado.cuentaTipos.size() && i < saldos.size(); i++) {
            String t = Estado.cuentaTipos.get(i);
            if (t.equals("Ingreso")) total += saldos.get(i);
            else if (t.equals("Costo") || t.equals("Gasto")) total -= saldos.get(i);
        }
        return total;
    }

    static double saldoDebitoAntes(String codigo, String fecha) {
        double saldo = 0;
        for (int a = 0; a < Estado.asientoFechas.size(); a++) {
            if (Fechas.fechaOrdenable(Estado.asientoFechas.get(a)).compareTo(Fechas.fechaOrdenable(fecha)) >= 0) continue;
            List<String> cods = Estado.partidaCodigos.get(a);
            for (int p = 0; p < cods.size(); p++)
                if (cods.get(p).equals(codigo))
                    saldo += Estado.partidaTipos.get(a).get(p).equalsIgnoreCase("Debito") ? Estado.partidaMontos.get(a).get(p) : -Estado.partidaMontos.get(a).get(p);
        }
        return saldo;
    }

    static void recalcularSaldos() {
        List<Double> saldos = saldosRango(Fechas.FECHA_MIN, Fechas.FECHA_MAX);
        for (int i = 0; i < Estado.cuentaSaldos.size(); i++) Estado.cuentaSaldos.set(i, saldos.get(i));
    }

    static void advertirSaldosContrarios() {
        List<Double> saldos = saldosVista(); boolean hay = false;
        for (int i = 0; i < Estado.cuentaCodigos.size(); i++) {
            double s = saldos.get(i);
            if (!esCuentaContraria(Estado.cuentaCodigos.get(i)) && s < -Estado.TOLERANCIA_SALDO) {
                if (!hay) { System.out.println("\nAVISO: cuentas con saldo en el lado contrario a su tipo:"); hay = true; }
                System.out.printf("  %s - %s (%s): %.2f%n", Estado.cuentaCodigos.get(i), Estado.cuentaNombres.get(i), Estado.cuentaTipos.get(i), s);
            }
        }
        if (hay) System.out.println("Verifique si son cuentas de naturaleza contraria o errores.");
    }

    // ============================================================
    // UTILIDADES DE FECHA Y ORDENAMIENTO
    // ============================================================
    static void ordenarMovimientos(List<Integer> asientos, List<Integer> partidas) {
        for (int i = 0; i < asientos.size() - 1; i++)
            for (int j = 0; j < asientos.size() - 1 - i; j++) {
                String fa = Fechas.fechaOrdenable(Estado.asientoFechas.get(asientos.get(j)));
                String fb = Fechas.fechaOrdenable(Estado.asientoFechas.get(asientos.get(j + 1)));
                if (fb.compareTo(fa) < 0) {
                    int ta = asientos.get(j); asientos.set(j, asientos.get(j + 1)); asientos.set(j + 1, ta);
                    if (partidas != null) {
                        int tp = partidas.get(j); partidas.set(j, partidas.get(j + 1)); partidas.set(j + 1, tp);
                    }
                }
            }
    }

    static void cargarMovimientosCuenta(String codigo, List<Integer> asientos, List<Integer> partidas) {
        asientos.clear(); partidas.clear();
        for (int i = 0; i < Estado.asientoFechas.size(); i++) {
            if (!Fechas.fechaEnRango(Estado.asientoFechas.get(i), Estado.vistaInicio(), Estado.vistaFin())) continue;
            List<String> cods = Estado.partidaCodigos.get(i);
            for (int j = 0; j < cods.size(); j++)
                if (cods.get(j).equals(codigo)) { asientos.add(i); partidas.add(j); }
        }
        ordenarMovimientos(asientos, partidas);
    }

    // ============================================================
    // REPORTES (opciones 4-8)
    // ============================================================
    static void mostrarAsientos() {
        System.out.println("\n=== ASIENTOS ===");
        System.out.println(Estado.descripcionRango());
        System.out.println("Cada asiento muestra sus partidas al Debe y al Haber.");
        boolean hay = false;
        for (int i = 0; i < Estado.asientoFechas.size(); i++) {
            if (!Fechas.fechaEnRango(Estado.asientoFechas.get(i), Estado.vistaInicio(), Estado.vistaFin())) continue;
            System.out.println("\nAsiento #" + (i + 1) + (esCierre(i) ? " (cierre)" : ""));
            mostrarAsiento(i); hay = true;
        }
        if (!hay) System.out.println("Sin asientos en la vista.");
    }

    static void mostrarAsiento(int i) {
        System.out.println("Fecha: " + Estado.asientoFechas.get(i));
        System.out.println("Descripcion: " + Estado.asientoDescripciones.get(i));
        if (esCierre(i)) System.out.println("Tipo: Asiento de cierre");
        mostrarPartidas(Estado.partidaCodigos.get(i), Estado.partidaTipos.get(i), Estado.partidaMontos.get(i));
        System.out.printf("  Total: %.2f%n", Estado.asientoTotales.get(i));
    }

    static void mostrarPartidas(List<String> cods, List<String> tips, List<Double> mons) {
        System.out.printf("  %-6s %-30s %-9s %12s%n", "Codigo", "Nombre", "Lado", "Monto");
        for (int j = 0; j < cods.size(); j++)
            System.out.printf("  %-6s %-30s %-9s %12.2f%n", cods.get(j), Cuentas.nombreCuenta(cods.get(j)), Formato.etiquetaLado(tips.get(j)), mons.get(j));
    }

    static void buscarAsientos() {
        int op = Consola.menu("BUSCAR ASIENTOS", "Por fecha exacta", "Por texto en descripcion");
        if (op == 0 || Consola.entradaCerrada) return;
        String fecha = "", term = "";
        if (op == 1) { fecha = Consola.leerFecha("Fecha a buscar"); if (fecha == null) { System.out.println("Cancelado."); return; } }
        if (op == 2) { String tx = Consola.leerString("Texto: "); if (tx == null) { Consola.entradaCerrada = true; System.out.println("Cancelado."); return; } term = Formato.normalizarTexto(tx); }
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < Estado.asientoFechas.size(); i++) {
            if (!Fechas.fechaEnRango(Estado.asientoFechas.get(i), Estado.vistaInicio(), Estado.vistaFin())) continue;
            boolean ok = op == 1 ? Estado.asientoFechas.get(i).equals(fecha) : Formato.normalizarTexto(Estado.asientoDescripciones.get(i)).contains(term);
            if (ok) idx.add(i);
        }
        System.out.println("\n=== RESULTADOS ===");
        System.out.println(Estado.descripcionRango());
        if (idx.isEmpty()) { System.out.println("Sin resultados."); return; }
        ordenarMovimientos(idx, null);
        for (int k = 0; k < idx.size(); k++) {
            int i = idx.get(k);
            System.out.println("\n#" + (k + 1) + " (Asiento " + (i + 1) + ")");
            mostrarAsiento(i);
        }
    }

    // ============================================================
    // HELPERS LEGACY (para leer archivos con |CIERRE)
    // ============================================================
    static boolean esCierre(int i) { return i >= 0 && i < Estado.asientoTipos.size() && Estado.asientoTipos.get(i).equals("CIERRE"); }
}
