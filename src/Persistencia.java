import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Persistencia {
    // ============================================================
    // CONFIGURACION Y CONSTANTES
    // ============================================================
    static final String ARCHIVO_CUENTAS = "data/cuentas.txt";
    static final String ARCHIVO_ASIENTOS = "data/asientos.txt";
    static final String ARCHIVO_BALANCE = "data/balance.txt";
    static final DateTimeFormatter FORMATO_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    static boolean bloquearGuardadoAsientos = false;
    static boolean bloquearGuardadoCuentas = false;

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
        Asientos.recalcularSaldos();
        Asientos.advertirSaldosContrarios();
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
            if (Asientos.esCierre(i)) contenido.append("|CIERRE");
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
                double totalDebe = Asientos.totalLado(tips, mons, "Debito"), totalHaber = Asientos.totalLado(tips, mons, "Credito");
                if (!Double.isFinite(totalDebe) || !Double.isFinite(totalHaber)) errores.add("suma no finita");
                else if (!Asientos.cuadra(totalDebe, totalHaber)) errores.add("no cuadra: Debe " + totalDebe + ", Haber " + totalHaber);
            }
        }
        for (String e : errores) advertir(ARCHIVO_ASIENTOS, lineaAsiento, e + ". Asiento omitido.");
        if (!errores.isEmpty()) return false;
        Asientos.agregarAsiento(fechaNormalizada, descripcion, cods, tips, mons, Asientos.totalLado(tips, mons, "Debito"), tipoAsiento);
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

    static boolean guardarBalance(String fecha, double act, double pas, double pat) {
        return escribirArchivoAtomico(ARCHIVO_BALANCE, fecha + "|" + Formato.formatearMonto(act) + "|" + Formato.formatearMonto(pas) + "|" + Formato.formatearMonto(pat) + System.lineSeparator());
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
}
