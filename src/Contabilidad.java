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
    static final String ARCHIVO_CUENTAS = "data/cuentas.txt";
    static final String ARCHIVO_ASIENTOS = "data/asientos.txt";
    static final String ARCHIVO_BALANCE = "data/balance.txt";
    static final DateTimeFormatter FORMATO_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    static boolean bloquearGuardadoAsientos = false;
    static boolean bloquearGuardadoCuentas = false;
    static boolean saldoContrarioEnReporte = false;

    // ============================================================
    // MAIN Y MENUS
    // ============================================================
    public static void main(String[] args) {
        System.out.println("=== SISTEMA CONTABLE EMPRESARIAL ===");
        cargarDatos();
        while (true) {
            mostrarMenu();
            int opcion = Consola.leerInt("Seleccione una opcion (0-12): ", 0, 12);
            if (Consola.entradaCerrada) { salir(); return; }
            switch (opcion) {
                case 1: menuAdministrarCuentas(); break;
                case 2: registrarAsiento(); break;
                case 3: menuConsultarCuentasT(); break;
                case 4: verLibroDiario(); break;
                case 5: verLibroMayor(); break;
                case 6: verBalanceComprobacion(); break;
                case 7: verEstadoResultados(); break;
                case 8: generarBalance(); break;
                case 9: menuConsultarMovimientos(); break;
                case 10: respaldarInformacion(); break;
                case 11: case 0: salir(); return;
                case 12: configurarRangoReportes(); break;
            }
        }
    }

    static boolean cuadra(double debe, double haber) { return Math.abs(debe - haber) <= Estado.TOLERANCIA + Estado.EPSILON; }
    static boolean tipoDeudor(String tipoCuenta) { return tipoCuenta.equals("Activo") || tipoCuenta.equals("Costo") || tipoCuenta.equals("Gasto"); }
    static boolean esCuentaContraria(String codigo) {
        return codigo.equals("1790") || codigo.equals("1890") || codigo.equals("3120")
                || codigo.equals("3200") || codigo.equals("3500");
    }
    static double totalLado(List<String> tips, List<Double> mons, String lado) {
        double total = 0;
        for (int i = 0; i < tips.size() && i < mons.size(); i++)
            if (tips.get(i).equalsIgnoreCase(lado)) total += mons.get(i);
        return total;
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
                case 1: mostrarAsientos(); break; case 2: buscarAsientos(); break; case 0: return;
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
    // CARGA Y PERSISTENCIA
    // ============================================================
    static void cargarDatos() {
        File dir = new File("data");
        if (!dir.exists()) dir.mkdirs();
        if (!cargarCuentasTexto(ARCHIVO_CUENTAS)) {
            bloquearGuardadoCuentas = true;
        } else if (Estado.cuentaCodigos.isEmpty()) {
            if (new File(ARCHIVO_CUENTAS).exists()) {
                Consola.linea("ADVERTENCIA: " + ARCHIVO_CUENTAS + " existe pero no tiene cuentas validas.",
                        "Use la opcion 1.4 (Reconstruir catalogo estandar) para volver a cargar las 75 cuentas.",
                        "no se inicializara el catalogo para no sobrescribir el archivo.");
                bloquearGuardadoCuentas = true;
            } else {
                System.out.println("Inicializando catalogo de cuentas...");
                Cuentas.inicializarCuentas();
            }
        }
        if (!cargarAsientosTexto(ARCHIVO_ASIENTOS)) bloquearGuardadoAsientos = true;
        else if (bloquearGuardadoCuentas && !Estado.asientoFechas.isEmpty()) {
            System.out.println("ADVERTENCIA: hay asientos pero el catalogo esta vacio. Restaure cuentas.txt desde backup/ y reinicie.");
            bloquearGuardadoAsientos = true; // evita sobrescribir asientos.txt al salir
        }
        recalcularSaldos();
        advertirSaldosContrarios();
    }

    static boolean guardarCuentas() {
        if (bloquearGuardadoCuentas) { System.out.println("No se guardaron las cuentas: corrija " + ARCHIVO_CUENTAS + " y reinicie el programa."); return false; }
        StringBuilder contenido = new StringBuilder();
        for (int i = 0; i < Estado.cuentaCodigos.size(); i++) {
            contenido.append(Formato.limpiarCampo(Estado.cuentaCodigos.get(i))).append('|')
                    .append(Formato.limpiarCampo(Estado.cuentaNombres.get(i))).append('|')
                    .append(Formato.limpiarCampo(Estado.cuentaTipos.get(i))).append('|')
                    .append(Formato.formatearMonto(Estado.cuentaSaldos.get(i))).append(System.lineSeparator());
        }
        return escribirArchivoAtomico(ARCHIVO_CUENTAS, contenido.toString());
    }

    static boolean escribirArchivoAtomico(String ruta, String contenido) {
        File temporal = new File(ruta + ".tmp");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(temporal))) {
            writer.write(contenido);
        } catch (IOException e) {
            System.out.println("Error escribiendo " + ruta + ": " + e.getMessage()); temporal.delete(); return false;
        }
        try {
            try {
                Files.move(temporal.toPath(), new File(ruta).toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporal.toPath(), new File(ruta).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            System.out.println("Error escribiendo " + ruta + ": " + e.getMessage()); temporal.delete(); return false;
        }
    }

    static void advertir(String archivo, int numeroLinea, String mensaje) {
        System.out.println("ADVERTENCIA " + archivo + " linea " + numeroLinea + ": " + mensaje);
    }

    static boolean cargarCuentasTexto(String archivo) {
        Estado.cuentaCodigos.clear(); Estado.cuentaNombres.clear(); Estado.cuentaTipos.clear(); Estado.cuentaSaldos.clear();
        File file = new File(archivo);
        if (!file.exists()) return true;
        boolean huboDescarte = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String linea; int numeroLinea = 0;
            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1);
                if (linea.trim().isEmpty()) continue;
                String[] partes = linea.split("\\|", -1);
                if (partes.length != 4) { advertir(archivo, numeroLinea, "se esperaban 4 campos. Linea omitida."); huboDescarte = true; continue; }
                String codigo = partes[0].trim(), nombre = partes[1].trim(), tipo = partes[2].trim();
                String error = codigo.isEmpty() ? "codigo de cuenta vacio"
                        : nombre.isEmpty() ? "nombre de cuenta vacio"
                        : !Cuentas.tipoValido(tipo) ? "tipo de cuenta invalido ('" + tipo + "')" : null;
                if (error != null) { advertir(archivo, numeroLinea, error + ". Linea omitida."); huboDescarte = true; continue; }
                double saldo = 0;
                try { saldo = Double.parseDouble(partes[3].trim()); if (!Double.isFinite(saldo)) throw new NumberFormatException(); }
                catch (NumberFormatException e) { advertir(archivo, numeroLinea, "saldo invalido ('" + partes[3].trim() + "'). Se usara 0.0."); huboDescarte = true; }
                if (Cuentas.existeCuenta(codigo) || Cuentas.existeCodigoNormalizado(codigo)) {
                    advertir(archivo, numeroLinea, "codigo duplicado " + codigo + ". Se conserva la primera aparicion."); huboDescarte = true; continue;
                }
                Estado.cuentaCodigos.add(codigo); Estado.cuentaNombres.add(nombre); Estado.cuentaTipos.add(tipo); Estado.cuentaSaldos.add(saldo);
            }
        } catch (IOException e) { System.out.println("Error leyendo " + archivo + ": " + e.getMessage()); return false; }
        if (huboDescarte) {
            System.out.println("ADVERTENCIA: " + archivo + " contiene lineas invalidas. Se cargaron solo las cuentas validas.");
            if (!respaldarArchivoInvalido(archivo, "cuentas_invalidas")) {
                System.out.println("No se sobrescribira " + archivo + " hasta corregirlo."); return false;
            }
        }
        return true;
    }

    static boolean respaldarArchivoInvalido(String archivo, String prefijo) {
        File origen = new File(archivo);
        if (!origen.exists()) return true;
        File dir = new File("backup");
        if (!dir.exists() && !dir.mkdirs()) { System.out.println("ADVERTENCIA: no se pudo crear la carpeta backup."); return false; }
        File destino = new File(dir, prefijo + "_" + LocalDateTime.now().format(FORMATO_TIMESTAMP) + ".txt");
        try {
            Files.copy(origen.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Se guardo una copia del archivo original en: " + destino.getPath()); return true;
        } catch (IOException e) {
            System.out.println("ADVERTENCIA: no se pudo guardar la copia de seguridad de " + archivo + ": " + e.getMessage()); return false;
        }
    }

    static boolean guardarAsientos() {
        if (bloquearGuardadoAsientos) { System.out.println("No se guardaron los asientos: corrija " + ARCHIVO_ASIENTOS + " y reinicie el programa."); return false; }
        StringBuilder contenido = new StringBuilder();
        for (int i = 0; i < Estado.asientoFechas.size(); i++) {
            contenido.append("ASIENTO|").append(Formato.limpiarCampo(Estado.asientoFechas.get(i))).append('|')
                    .append(Formato.limpiarCampo(Estado.asientoDescripciones.get(i))).append('|').append(Formato.formatearMonto(Estado.asientoTotales.get(i)));
            if (esCierre(i)) contenido.append("|CIERRE");
            contenido.append(System.lineSeparator());
            List<String> cods = Estado.partidaCodigos.get(i), tips = Estado.partidaTipos.get(i);
            List<Double> mons = Estado.partidaMontos.get(i);
            for (int j = 0; j < cods.size(); j++)
                contenido.append("PARTIDA|").append(Formato.limpiarCampo(cods.get(j))).append('|')
                        .append(Formato.limpiarCampo(tips.get(j))).append('|').append(Formato.formatearMonto(mons.get(j))).append(System.lineSeparator());
        }
        return escribirArchivoAtomico(ARCHIVO_ASIENTOS, contenido.toString());
    }

    static boolean cargarAsientosTexto(String archivo) {
        Estado.asientoFechas.clear(); Estado.asientoDescripciones.clear(); Estado.asientoTotales.clear(); Estado.asientoTipos.clear();
        Estado.partidaCodigos.clear(); Estado.partidaTipos.clear(); Estado.partidaMontos.clear();
        File file = new File(archivo);
        if (!file.exists()) return true;
        int validos = 0, omitidos = 0;
        boolean enAsiento = false;
        String fecha = null, descripcion = null, totalTexto = null, tipoAsiento = "NORMAL";
        List<String> cods = null, tips = null;
        List<Double> mons = null; boolean bloqueInvalido = false;
        List<String> errores = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String linea; int numeroLinea = 0;
            while ((linea = reader.readLine()) != null) {
                numeroLinea++;
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1);
                if (linea.trim().isEmpty()) continue;
                String[] partes = linea.split("\\|", -1);
                String etiqueta = partes[0].trim();
                if (etiqueta.equals("ASIENTO")) {
                    if (enAsiento) { if (cerrarAsientoCargado(fecha, descripcion, totalTexto, tipoAsiento, cods, tips, mons, bloqueInvalido, errores)) validos++; else omitidos++; }
                    enAsiento = true; lineaAsiento = numeroLinea;
                    fecha = null; descripcion = null; totalTexto = null; tipoAsiento = "NORMAL";
                    cods = new ArrayList<>(); tips = new ArrayList<>(); mons = new ArrayList<>();
                    bloqueInvalido = false; errores.clear();
                    if (partes.length != 4 && partes.length != 5) {
                        bloqueInvalido = true; errores.add("se esperaban 4 o 5 campos en ASIENTO");
                    } else {
                        fecha = partes[1].trim(); descripcion = partes[2].trim(); totalTexto = partes[3].trim();
                        if (partes.length == 5) {
                            tipoAsiento = partes[4].trim().toUpperCase(Locale.ROOT);
                            if (!tipoAsiento.equals("NORMAL") && !tipoAsiento.equals("CIERRE")) {
                                bloqueInvalido = true; errores.add("tipo de asiento invalido");
                            }
                        }
                    }
                } else if (etiqueta.equals("PARTIDA")) {
                    if (!enAsiento) { advertir(archivo, numeroLinea, "PARTIDA sin ASIENTO previo"); }
                    else if (!bloqueInvalido) {
                        String error = agregarPartidaCargada(partes, numeroLinea, cods, tips, mons);
                        if (error != null) { bloqueInvalido = true; errores.add(error); }
                    }
                } else if (enAsiento) {
                    bloqueInvalido = true; errores.add("linea desconocida en linea " + numeroLinea + " ('" + linea.trim() + "')");
                } else { advertir(archivo, numeroLinea, "linea desconocida"); }
            }
            if (enAsiento) { if (cerrarAsientoCargado(fecha, descripcion, totalTexto, tipoAsiento, cods, tips, mons, bloqueInvalido, errores)) validos++; else omitidos++; }
        } catch (IOException e) { System.out.println("Error leyendo " + archivo + ": " + e.getMessage()); return false; }
        if (omitidos > 0) {
            System.out.println("Carga de asientos: " + validos + " validos, " + omitidos + " omitidos.");
            System.out.println("ADVERTENCIA: " + archivo + " contiene asientos invalidos.");
            System.out.println("Se cargaron solo los asientos validos.");
            if (!respaldarArchivoInvalido(archivo, "asientos_invalidos")) {
                bloquearGuardadoAsientos = true;
                Consola.linea("ADVERTENCIA: no se pudo guardar la copia de seguridad de " + archivo + " en backup/.",
                        "No se sobrescribira ese archivo para no perder los asientos originales.",
                        "Corrija la carpeta backup/ y reinicie el programa.");
            }
        }
        return true;
    }
    static int lineaAsiento = 0;

    static boolean cerrarAsientoCargado(String fecha, String descripcion, String totalTexto, String tipoAsiento,
            List<String> cods, List<String> tips, List<Double> mons, boolean bloqueInvalido, List<String> errores) {
        if (bloqueInvalido) { for (String e : errores) advertir(ARCHIVO_ASIENTOS, lineaAsiento, e + ". Asiento omitido."); return false; }
        String fechaNormalizada = null;
        if (fecha == null || fecha.isEmpty()) errores.add("fecha vacia");
        else {
            LocalDate f = Fechas.parsearFecha(fecha);
            if (f == null) errores.add("fecha invalida");
            else if (f.getYear() < 1900 || f.getYear() > 2100) errores.add("ano fuera de rango");
            else fechaNormalizada = f.format(Fechas.FORMATO_FECHA);
        }
        if (descripcion == null || descripcion.isEmpty()) errores.add("descripcion vacia");
        int cantidad = cods == null ? 0 : cods.size();
        if (cantidad < 2) errores.add("minimo 2 partidas");
        else {
            boolean hayDebe = false, hayHaber = false;
            for (int i = 0; i < cantidad; i++) { if (tips.get(i).equalsIgnoreCase("Debito")) hayDebe = true; else hayHaber = true; }
            if (!hayDebe || !hayHaber) errores.add("debe tener al menos una partida al Debe y una al Haber");
            else {
                double totalDebe = totalLado(tips, mons, "Debito"), totalHaber = totalLado(tips, mons, "Credito");
                if (!Double.isFinite(totalDebe) || !Double.isFinite(totalHaber)) errores.add("suma no finita");
                else if (!cuadra(totalDebe, totalHaber)) errores.add("no cuadra: Debe " + totalDebe + ", Haber " + totalHaber);
            }
        }
        for (String e : errores) advertir(ARCHIVO_ASIENTOS, lineaAsiento, e + ". Asiento omitido.");
        if (!errores.isEmpty()) return false;
        agregarAsiento(fechaNormalizada, descripcion, cods, tips, mons, totalLado(tips, mons, "Debito"), tipoAsiento);
        return true;
    }

    static String agregarPartidaCargada(String[] partes, int numeroLinea, List<String> cods,
            List<String> tips, List<Double> mons) {
        if (partes.length != 4) return "PARTIDA con " + partes.length + " campos";
        String codigo = partes[1].trim(), tipoTexto = Formato.normalizarTexto(partes[2]);
        String tipo = tipoTexto.equals("debito") ? "Debito" : tipoTexto.equals("credito") ? "Credito" : null;
        if (tipo == null) return "tipo de partida invalido";
        if (codigo.isEmpty()) return "codigo vacio";
        if (!Cuentas.existeCuenta(codigo)) return "la cuenta " + codigo + " no existe";
        try {
            double monto = Consola.parsearMontoSimple(partes[3]);
            if (!Double.isFinite(monto) || monto <= 0) throw new NumberFormatException();
            cods.add(codigo); tips.add(tipo); mons.add(monto);
            return null;
        } catch (NumberFormatException e) { return "monto invalido"; }
    }

    // ============================================================
    // REGISTRO DE ASIENTOS (OPCION 2)
    // ============================================================
    static void registrarAsiento() {
        if (bloquearGuardadoAsientos || bloquearGuardadoCuentas) {
            if (bloquearGuardadoCuentas)
                Consola.linea("No se pueden registrar asientos: el catalogo o el guardado estan bloqueados.",
                        "Corrija los archivos de datos y reinicie el programa.");
            else
                System.out.println("No se pueden registrar asientos: corrija " + ARCHIVO_ASIENTOS + " y reinicie.");
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
        if (!guardarAsientos()) { quitarUltimoAsiento(); recalcularSaldos(); System.out.println("Error guardando."); return; }
        if (!guardarCuentas()) System.out.println("Asiento guardado, pero no se actualizo " + ARCHIVO_CUENTAS + ".");
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

    static void verBalanceComprobacion() {
        String titulo = Estado.descripcionRango();
        Consola.linea("\n=== BALANZA DE COMPROBACION ===",
                titulo,
                "Cargos (Debitos) y abonos (Creditos) por cuenta.",
                "Los totales de Debitos y Creditos deben ser iguales.", "");
        System.out.printf("%-8s %-35s %12s %12s %12s %12s%n", "Codigo", "Nombre", "Debitos", "Creditos", "Deudor", "Acreedor");
        System.out.println("-".repeat(96));
        double[][] mov = movimientosRango(Fechas.FECHA_MIN, Estado.vistaFin());
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
        if (cuadra(tDeb, tCre)) System.out.println("LA BALANZA DE COMPROBACION ESTA CUADRADA.");
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
        ordenarMovimientos(idx, null);
        System.out.println();
        System.out.printf("%-10s %-42s %12s %12s %12s%n", "Cta", "Detalle", "Parcial", "Debe", "Haber");
        System.out.println("=".repeat(92));
        double tGDeb = 0, tGHab = 0;
        for (int k = 0; k < idx.size(); k++) {
            int i = idx.get(k);
            List<String> cods = Estado.partidaCodigos.get(i), tips = Estado.partidaTipos.get(i);
            List<Double> mons = Estado.partidaMontos.get(i);
            tGDeb += totalLado(tips, mons, "Debito"); tGHab += totalLado(tips, mons, "Credito");
            System.out.println("Asiento #" + (i + 1) + (esCierre(i) ? " (cierre)" : ""));
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
        double salIni = saldoDebitoAntes(cod, Estado.vistaInicio());
        if (!tipoDeudor(tipoCta)) salIni = -salIni;
        System.out.println("\nCUENTA: " + Estado.cuentaCodigos.get(idx) + " - " + Estado.cuentaNombres.get(idx));
        System.out.println("Tipo: " + tipoCta);
        List<Integer> aMov = new ArrayList<>(), pMov = new ArrayList<>();
        cargarMovimientosCuenta(cod, aMov, pMov);
        System.out.printf("Saldo inicial: %.2f (%s)%n", Math.abs(salIni), etiquetaSaldo(salIni, tipoCta));
        if (aMov.isEmpty() && Math.abs(salIni) <= Estado.TOLERANCIA_SALDO) { System.out.println("Sin movimientos."); return; }
        System.out.printf("%-5s %-12s %-24s %-8s %12s %12s%n", "As.", "Fecha", "Concepto", "Lado", "Monto", "Saldo");
        System.out.println("-".repeat(78));
        double sal = salIni;
        for (int k = 0; k < aMov.size(); k++) {
            int i = aMov.get(k), j = pMov.get(k);
            String tipo = Estado.partidaTipos.get(i).get(j); double monto = Estado.partidaMontos.get(i).get(j);
            sal += efectoSaldo(tipoCta, tipo, monto);
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
        cargarMovimientosCuenta(Estado.cuentaCodigos.get(idx), a, p);
        return !a.isEmpty();
    }

    static void mostrarCuentaT(int idxCta) {
        if (idxCta < 0 || idxCta >= Estado.cuentaCodigos.size()) return;
        String cod = Estado.cuentaCodigos.get(idxCta), tipoCta = Estado.cuentaTipos.get(idxCta);
        List<Integer> aMov = new ArrayList<>(), pMov = new ArrayList<>();
        cargarMovimientosCuenta(cod, aMov, pMov);
        double salIni = saldoDebitoAntes(cod, Estado.vistaInicio());
        double salIniNorm = tipoDeudor(tipoCta) ? salIni : -salIni;
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
        double salNorm = tipoDeudor(tipoCta) ? sal : -sal;
        System.out.printf("%63s SALDO: %.2f (%s)%n", "", Math.abs(sal), etiquetaSaldo(salNorm, tipoCta));
        if (!esCuentaContraria(cod) && salNorm < -Estado.TOLERANCIA_SALDO)
            System.out.println("  AVISO: saldo en el lado contrario al normal para una cuenta de tipo " + tipoCta + ".");
        System.out.println("=".repeat(95));
    }

    static String etiquetaSaldo(double s, String tipoCta) {
        if (Math.abs(s) <= Estado.TOLERANCIA_SALDO) return "Cero";
        return (s > 0) == tipoDeudor(tipoCta) ? "Deudor" : "Acreedor";
    }

    static void verEstadoResultados() {
        Consola.linea("\n=== ESTADO DE RESULTADOS ===",
                Estado.descripcionRango(),
                "Ingresos - Costos - Gastos = Utilidad (o perdida).");
        System.out.printf("%-8s %-35s %12s%n", "Codigo", "Nombre", "Saldo");
        System.out.println("-".repeat(60));
        saldoContrarioEnReporte = false;
        List<Double> saldos = saldosRango(Estado.vistaInicio(), Estado.vistaFin());
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
        List<Double> saldos = saldosRango(Fechas.FECHA_MIN, fin);
        double tAct = mostrarSeccion("Activo", "ACTIVO", saldos);
        double tPas = mostrarSeccion("Pasivo", "PASIVO", saldos), tPat = mostrarSeccion("Patrimonio", "PATRIMONIO", saldos);
        double resultado = resultadoDeSaldos(saldos), patConRes = tPat + resultado;
        System.out.println("\n-- RESULTADO --");
        if (Estado.rangoDesde.isEmpty()) {
            System.out.printf("%-44s %12.2f%n", "Resultado acumulado:", resultado);
        } else {
            double resRango = resultadoDeSaldos(saldosRango(Estado.vistaInicio(), Estado.vistaFin()));
            System.out.printf("%-44s %12.2f%n", "Resultado acumulado:", resultado);
            System.out.printf("%-44s %12.2f%n", "Resultado del rango (" + Estado.rangoDesde + " a " + Estado.rangoHasta + "):", resRango);
        }
        System.out.println("\n" + "-".repeat(60));
        System.out.printf("Total Pasivo + Patrimonio + Resultado: %.2f%n", tPas + patConRes);
        System.out.printf("Diferencia: %.2f%n", tAct - (tPas + patConRes));
        if (cuadra(tAct, tPas + patConRes)) System.out.println("EL BALANCE CUADRA CORRECTAMENTE.");
        else { System.out.println("ADVERTENCIA: NO cuadra."); System.out.printf("Diferencia: %.2f%n", tAct - (tPas + patConRes)); }
        mostrarNotaSaldosContrarios();
        if (!guardarBalance(Estado.rangoDesde.isEmpty() ? Fechas.fechaActual() : fin, tAct, tPas, patConRes))
            System.out.println("ADVERTENCIA: no se guardo " + ARCHIVO_BALANCE + ".");
    }

    static double mostrarSeccion(String tipo, String titulo, List<Double> saldos) {
        System.out.println("\n-- " + titulo + " --");
        double total = 0;
        for (int i = 0; i < Estado.cuentaCodigos.size(); i++) {
            if (!Estado.cuentaTipos.get(i).equals(tipo)) continue;
            double s = saldos.get(i);
            String marca = "";
            if (!esCuentaContraria(Estado.cuentaCodigos.get(i)) && s < -Estado.TOLERANCIA_SALDO) { marca = " *"; saldoContrarioEnReporte = true; }
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

    static boolean guardarBalance(String fecha, double act, double pas, double pat) {
        return escribirArchivoAtomico(ARCHIVO_BALANCE, fecha + "|" + Formato.formatearMonto(act) + "|" + Formato.formatearMonto(pas) + "|" + Formato.formatearMonto(pat) + System.lineSeparator());
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
    // RESPALDO (OPCION 10)
    // ============================================================
    static void respaldarInformacion() {
        System.out.println("\n--- RESPALDO ---");
        System.out.println("Se copiaran los archivos de datos a una carpeta nueva en backup/.");
        File dir = new File("backup/manual_" + LocalDateTime.now().format(FORMATO_TIMESTAMP));
        if (!dir.mkdirs()) { System.out.println("No se pudo crear carpeta."); return; }
        String[] arch = {ARCHIVO_CUENTAS, ARCHIVO_ASIENTOS, ARCHIVO_BALANCE};
        int n = 0;
        try {
            for (String a : arch) {
                File o = new File(a);
                if (o.exists()) { Files.copy(o.toPath(), new File(dir, o.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING); n++; }
            }
        } catch (IOException e) { System.out.println("Error: " + e.getMessage()); for (File f : dir.listFiles()) f.delete(); dir.delete(); return; }
        if (n == 0) { System.out.println("Sin datos."); dir.delete(); return; }
        System.out.println("Respaldo creado en: " + dir.getPath() + " (" + n + " archivos).");
    }

    // ============================================================
    // SALIDA
    // ============================================================
    static void salir() {
        System.out.println("\nGuardando y saliendo...");
        if (bloquearGuardadoAsientos || bloquearGuardadoCuentas) {
            System.out.println("No se guardaron cambios: corrija los archivos de datos y reinicie el programa.");
        } else {
            if (!guardarAsientos() || !guardarCuentas()) System.out.println("Error guardando.");
        }
        System.out.println("Hasta luego.");
    }

    // ============================================================
    // HELPERS LEGACY (para leer archivos con |CIERRE)
    // ============================================================
    static boolean esCierre(int i) { return i >= 0 && i < Estado.asientoTipos.size() && Estado.asientoTipos.get(i).equals("CIERRE"); }
}