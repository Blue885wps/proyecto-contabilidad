import java.io.*;
import java.nio.file.*;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.*;

public class Datos {
    static final double TOLERANCIA = 0.005, EPSILON = 1e-9;
    static final int ANIO_MIN = 1900, ANIO_MAX = 2100;
    static final String ARCHIVO_CUENTAS = "data/cuentas.txt", ARCHIVO_ASIENTOS = "data/asientos.txt", ARCHIVO_BALANCE = "data/balance.txt";
    static final String ARCHIVO_CATALOGO = "data/catalogo_estandar.txt";
    static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final DateTimeFormatter FORMATO_FECHA_ESTRICTA = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    static final DateTimeFormatter FORMATO_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    static final String[] TIPOS_CUENTA = {"Activo", "Pasivo", "Patrimonio", "Ingreso", "Costo", "Gasto"};
    static final String[] NATURALEZAS = {"Deudora", "Acreedora", "Mixta"};

    static boolean datosInvalidos = false;
    static List<String> cuentaCodigos = new ArrayList<>(), cuentaNombres = new ArrayList<>(), cuentaTipos = new ArrayList<>(), cuentaNaturalezas = new ArrayList<>();
    static List<Double> cuentaSaldos = new ArrayList<>();
    static List<String> asientoFechas = new ArrayList<>(), asientoDescripciones = new ArrayList<>();
    static List<Double> asientoTotales = new ArrayList<>();
    static List<List<String>> partidaCodigos = new ArrayList<>(), partidaTipos = new ArrayList<>();
    static List<List<Double>> partidaMontos = new ArrayList<>();
    static double[] saldosCarga = new double[0];

    // FECHAS, MONTOS Y TEXTO
    static String fechaActual() { return LocalDate.now().format(FORMATO_FECHA); }

    static LocalDate parsearFecha(String texto) {
        if (texto == null) return null;
        String limpio = texto.trim();
        if (!limpio.matches("\\d{2}/\\d{2}/\\d{4}")) return null;
        try {
            LocalDate fecha = LocalDate.parse(limpio, FORMATO_FECHA_ESTRICTA);
            return fecha.getYear() < ANIO_MIN || fecha.getYear() > ANIO_MAX ? null : fecha;
        } catch (DateTimeParseException e) { return null; }
    }

    static String fechaOrdenable(String fecha) {
        String[] p = fecha.split("/");
        return p.length == 3 ? p[2] + p[1] + p[0] : "99999999";
    }

    static Double parsearMonto(String texto) {
        if (texto == null) return null;
        String s = texto.trim();
        if (s.isEmpty()) return null;
        if (s.matches("\\d+[.,]\\d{3}")) return null;
        if (s.matches(".*[eE][+-]?\\d+")) return null;
        boolean tienePunto = s.indexOf('.') >= 0;
        boolean tieneComa = s.indexOf(',') >= 0;
        String normalizado;
        if (tienePunto && tieneComa) {
            if (s.lastIndexOf('.') > s.lastIndexOf(',')) {
                normalizado = s.replace(",", "");
            } else {
                normalizado = s.replace(".", "").replace(',', '.');
            }
        } else if (tienePunto) {
            if (contarCaracter(s, '.') > 1) normalizado = s.replace(".", "");
            else normalizado = s;
        } else if (tieneComa) {
            if (contarCaracter(s, ',') > 1) normalizado = s.replace(",", "");
            else normalizado = s.replace(',', '.');
        } else {
            normalizado = s;
        }
        if (normalizado.indexOf('.') >= 0) {
            String[] partes = normalizado.split("\\.", 2);
            if (partes[1].length() > 2) return null;
        }
        try {
            double valor = Double.parseDouble(normalizado);
            if (!Double.isFinite(valor) || valor < 0.01) return null;
            return valor;
        } catch (NumberFormatException e) { return null; }
    }

    static String formatearMonto(double valor) {
        if (Math.abs(valor) < TOLERANCIA) valor = 0;
        return String.format(Locale.ROOT, "%.2f", valor);
    }

    static String normalizarTexto(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    static int contarCaracter(String texto, char caracter) {
        int total = 0;
        for (int i = 0; i < texto.length(); i++) if (texto.charAt(i) == caracter) total++;
        return total;
    }

    static String recortar(String texto, int ancho) { return texto == null ? "" : (texto.length() <= ancho ? texto : texto.substring(0, ancho)); }
    static String etiquetaLado(String tipo) { return tipo.startsWith("D") ? "Debe" : "Haber"; }

    // CUENTAS
    static void inicializarCuentas() {
        cuentaCodigos.clear(); cuentaNombres.clear(); cuentaTipos.clear(); cuentaNaturalezas.clear(); cuentaSaldos.clear();
        List<String[]> lineas = cargarArchivo(ARCHIVO_CATALOGO, 3, "catalogo_estandar.txt");
        if (lineas.isEmpty()) return;
        for (String[] p : lineas) {
            agregarCuenta(p[0], p[1], p[2], naturalezaEstandar(p[0], p[2]));
        }
    }

    static void agregarCuenta(String codigo, String nombre, String tipo, String naturaleza) {
        cuentaCodigos.add(codigo); cuentaNombres.add(nombre); cuentaTipos.add(tipo);
        cuentaNaturalezas.add(naturaleza); cuentaSaldos.add(0.0);
    }

    static boolean tipoValido(String tipo) { return Arrays.asList(TIPOS_CUENTA).contains(tipo); }

    static String normalizarNaturaleza(String valor) {
        if (valor == null) return null;
        for (String naturaleza : NATURALEZAS)
            if (naturaleza.equalsIgnoreCase(valor.trim())) return naturaleza;
        return null;
    }

    static String naturalezaSugerida(String tipo) {
        return Asientos.tipoDeudor(tipo) ? "Deudora" : "Acreedora";
    }

    static String naturalezaEstandar(String codigo, String tipo) {
        if (codigo.equals("1790") || codigo.equals("1890")) return "Acreedora";
        if (codigo.equals("3120") || codigo.equals("3500")) return "Deudora";
        if (codigo.equals("3200")) return "Mixta";
        return naturalezaSugerida(tipo);
    }

    static int indiceCuenta(String codigo) {
        for (int i = 0; i < cuentaCodigos.size(); i++) if (cuentaCodigos.get(i).equals(codigo)) return i;
        return -1;
    }

    static String nombreCuenta(String codigo) {
        int i = indiceCuenta(codigo);
        return i < 0 ? "(no existe)" : cuentaNombres.get(i);
    }

    static List<Integer> buscarCuentas(String texto) {
        String[] terminos = normalizarTexto(texto).split("\\s+");
        boolean soloDigitos = terminos.length == 1 && terminos[0].matches("\\d+");
        List<Integer> encontradas = new ArrayList<>();
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            boolean coincide;
            if (soloDigitos) {
                coincide = normalizarTexto(cuentaCodigos.get(i)).startsWith(terminos[0]);
            } else {
                String campo = normalizarTexto(cuentaCodigos.get(i) + " " + cuentaNombres.get(i)
                        + " " + cuentaTipos.get(i) + " " + cuentaNaturalezas.get(i));
                coincide = true;
                for (String termino : terminos) if (!campo.contains(termino)) { coincide = false; break; }
            }
            if (coincide) encontradas.add(i);
        }
        return encontradas;
    }

    static void mostrarCuentaEncontrada(int i) {
        System.out.println("Seleccionada: " + cuentaCodigos.get(i) + " - " + cuentaNombres.get(i)
                + " (" + cuentaTipos.get(i) + ", " + cuentaNaturalezas.get(i) + ").");
    }

    static String pedirCuenta(String contexto) {
        while (true) {
            String entrada = Contabilidad.leerEntrada(contexto + " (codigo exacto o texto a buscar; ? = catalogo, 0 = cancelar): ");
            if (entrada == null || entrada.equals("0")) return null;
            if (entrada.equals("?")) { mostrarCatalogo(); continue; }
            int exacto = indiceCuenta(entrada);
            if (exacto >= 0) { mostrarCuentaEncontrada(exacto); return cuentaCodigos.get(exacto); }
            List<Integer> encontradas = buscarCuentas(entrada);
            if (encontradas.isEmpty()) {
                System.out.println("La cuenta no existe. El texto no coincide con ninguna cuenta.");
                System.out.println("Escriba otro codigo o palabra, ? para ver el catalogo o 0 para cancelar.");
                continue;
            }
            if (encontradas.size() == 1) {
                int i = encontradas.get(0);
                mostrarCuentaEncontrada(i);
                return cuentaCodigos.get(i);
            }
            if (encontradas.size() > 12) {
                System.out.println("Hay " + encontradas.size() + " coincidencias. Escriba mas caracteres para reducir la lista.");
                continue;
            }
            System.out.println("Coincidencias encontradas:");
            for (int k = 0; k < encontradas.size(); k++) {
                int i = encontradas.get(k);
                System.out.printf("  %d. %s - %s (%s, %s)%n", k + 1, cuentaCodigos.get(i),
                        cuentaNombres.get(i), cuentaTipos.get(i), cuentaNaturalezas.get(i));
            }
            while (true) {
                String seleccion = Contabilidad.leerEntrada("Elija una cuenta por numero (1-" + encontradas.size()
                        + "), V para buscar de nuevo, ? para el catalogo, 0 para cancelar: ");
                if (seleccion == null || seleccion.equals("0")) return null;
                if (seleccion.equalsIgnoreCase("V")) break;
                if (seleccion.equals("?")) { mostrarCatalogo(); continue; }
                try {
                    int opcion = Integer.parseInt(seleccion);
                    if (opcion >= 1 && opcion <= encontradas.size()) {
                        int i = encontradas.get(opcion - 1);
                        mostrarCuentaEncontrada(i);
                        return cuentaCodigos.get(i);
                    }
                } catch (NumberFormatException e) { }
                System.out.println("Opcion invalida. Escriba el numero de la lista, V, ? o 0.");
            }
        }
    }

    static void buscarCuenta() {
        System.out.println("\n--- BUSCAR CUENTA ---");
        System.out.println("Escriba el codigo exacto o parte del nombre, tipo o naturaleza.");
        System.out.println("Ejemplos: 1000, caja, inventarios, activo acreedora.");
        pedirCuenta("Cuenta a buscar");
    }

    static void mostrarCatalogo() {
        System.out.println("\n=== CATALOGO DE CUENTAS ===");
        System.out.println("El saldo se muestra en la convencion de la naturaleza: positivo en su lado normal, negativo si es contrario.");
        System.out.printf("%-8s %-32s %-11s %-11s %12s %-12s%n", "Codigo", "Nombre", "Tipo", "Naturaleza", "Saldo", "Estado");
        System.out.println("-".repeat(92));
        for (int i = 0; i < cuentaCodigos.size(); i++) {
            double saldoNaturaleza = Asientos.saldoNaturaleza(i, cuentaSaldos.get(i));
            boolean contrario = Asientos.esSaldoContrario(i, saldoNaturaleza);
            System.out.printf("%-8s %-32s %-11s %-11s %12.2f %-12s%n", cuentaCodigos.get(i), cuentaNombres.get(i),
                    cuentaTipos.get(i), cuentaNaturalezas.get(i), saldoNaturaleza, contrario ? "(contrario)" : "");
        }
        System.out.println("Nota: los saldos en el lado contrario a la naturaleza se marcan en la columna Estado.");
    }

    static void crearCuenta() {
        if (datosInvalidos) { System.out.println("No se pueden crear cuentas: hay datos invalidos en los archivos."); return; }
        System.out.println("\n--- NUEVA CUENTA ---");
        System.out.println("Se pediran 4 datos: nombre, tipo, naturaleza y codigo.");
        System.out.println("Paso 1 de 4: nombre de la cuenta.");
        String nombre = Contabilidad.leerTextoPersistible("Nombre (ej: Caja chica): ", "nombre");
        if (Contabilidad.entradaCerrada || nombre == null) return;
        System.out.println("Paso 2 de 4: tipo de cuenta.");
        String tipo = pedirTipoCuenta();
        if (Contabilidad.entradaCerrada) return;
        System.out.println("Paso 3 de 4: naturaleza de la cuenta.");
        String naturaleza = pedirNaturalezaCuenta(tipo);
        if (Contabilidad.entradaCerrada) return;
        System.out.println("Paso 4 de 4: codigo unico de la cuenta.");
        String codigo;
        while (true) {
            codigo = Contabilidad.leerEntrada("Codigo (ej: 1010): ");
            if (codigo == null) return;
            if (codigo.equals("0") || codigo.equals("?") || codigo.contains("|")) {
                System.out.println("Codigo invalido. No use 0, ? ni el caracter |. Escriba otro codigo."); continue;
            }
            if (indiceCuenta(codigo) >= 0) { System.out.println("El codigo ya existe. Escriba otro codigo."); continue; }
            break;
        }
        agregarCuenta(codigo, nombre, tipo, naturaleza);
        if (!guardarCuentas()) {
            int u = cuentaCodigos.size() - 1;
            cuentaCodigos.remove(u); cuentaNombres.remove(u); cuentaTipos.remove(u);
            cuentaNaturalezas.remove(u); cuentaSaldos.remove(u);
            System.out.println("No se pudo guardar la cuenta."); return;
        }
        System.out.println("Cuenta creada: " + codigo + " - " + nombre + " (" + tipo + ", " + naturaleza + ").");
    }

    static String pedirTipoCuenta() {
        System.out.println("Tipos disponibles:");
        for (int i = 0; i < TIPOS_CUENTA.length; i++) System.out.println("  " + (i + 1) + ". " + TIPOS_CUENTA[i]);
        return TIPOS_CUENTA[Contabilidad.leerInt("Seleccione el tipo (1-6): ", 1, 6) - 1];
    }

    static String pedirNaturalezaCuenta(String tipo) {
        System.out.println("Naturalezas disponibles:");
        System.out.println("  1. Deudora   -> aumenta con el Debe y no puede quedar con saldo Acreedor.");
        System.out.println("  2. Acreedora -> aumenta con el Haber y no puede quedar con saldo Deudor.");
        System.out.println("  3. Mixta     -> permite saldo Deudor o Acreedor.");
        System.out.println("Sugerida para el tipo " + tipo + ": " + naturalezaSugerida(tipo) + ".");
        int opcion = Contabilidad.leerInt("Seleccione la naturaleza (1-3): ", 1, 3);
        return NATURALEZAS[opcion - 1];
    }

    // PERSISTENCIA - Helpers genéricos
    static List<String[]> cargarArchivo(String ruta, int camposEsperados, String nombreArchivo) {
        List<String[]> resultado = new ArrayList<>();
        File archivo = new File(ruta);
        if (!archivo.exists()) return resultado;
        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea; int numero = 0;
            while ((linea = reader.readLine()) != null) {
                numero++;
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1);
                if (linea.trim().isEmpty()) continue;
                String[] p = linea.split("\\|", -1);
                if (camposEsperados > 0 && p.length != camposEsperados) {
                    System.out.println("ADVERTENCIA " + nombreArchivo + " linea " + numero + ": se esperaban " + camposEsperados + " campos. Linea omitida.");
                    datosInvalidos = true;
                    continue;
                }
                for (int i = 0; i < p.length; i++) p[i] = p[i].trim();
                resultado.add(p);
            }
        } catch (IOException e) {
            System.out.println("Error leyendo " + nombreArchivo + ": " + e.getMessage());
            datosInvalidos = true;
        }
        return resultado;
    }

    static String[] migrarSiViejo(String[] p, String codigo, String tipo) {
        if (p.length == 5) return p;
        String[] nuevo = new String[5];
        nuevo[0] = p[0]; nuevo[1] = p[1]; nuevo[2] = p[2];
        nuevo[3] = naturalezaEstandar(codigo, tipo);
        nuevo[4] = p[3];
        return nuevo;
    }

    static void cargarDatos() {
        new File("data").mkdirs();
        boolean catalogoVacio = !cargarCuentasTexto();
        if (catalogoVacio && new File(ARCHIVO_CUENTAS).exists()) {
            System.out.println("ADVERTENCIA: no hay cuentas validas en " + ARCHIVO_CUENTAS + ".");
            System.out.println("Se usara el catalogo estandar en memoria; no se guardara hasta corregir el archivo.");
            inicializarCuentas();
            datosInvalidos = true;
        } else if (catalogoVacio) {
            System.out.println("Inicializando catalogo de cuentas...");
            inicializarCuentas();
            if (!datosInvalidos) guardarCuentas();
        }
        cargarAsientosTexto();
        Asientos.recalcularSaldos();
    }

    static boolean cargarCuentasTexto() {
        cuentaCodigos.clear(); cuentaNombres.clear(); cuentaTipos.clear(); cuentaNaturalezas.clear(); cuentaSaldos.clear();
        File archivo = new File(ARCHIVO_CUENTAS);
        if (!archivo.exists()) return false;
        List<String[]> lineas = cargarArchivo(ARCHIVO_CUENTAS, -1, "cuentas.txt");
        int validos = 0;
        for (String[] p : lineas) {
            if (p.length != 4 && p.length != 5) { continue; }
            String codigo = p[0], nombre = p[1], tipo = p[2];
            if (codigo.isEmpty() || nombre.isEmpty()) { continue; }
            if (!tipoValido(tipo)) { continue; }
            String naturaleza;
            String saldoTexto;
            if (p.length == 5) {
                naturaleza = normalizarNaturaleza(p[3]);
                saldoTexto = p[4];
            } else {
                naturaleza = naturalezaEstandar(codigo, tipo);
                saldoTexto = p[3];
            }
            String nat = normalizarNaturaleza(naturaleza);
            if (nat == null) {
                System.out.println("ADVERTENCIA " + ARCHIVO_CUENTAS + ": naturaleza invalida (" + naturaleza + ") en cuenta " + codigo + ". Linea omitida.");
                datosInvalidos = true;
                continue;
            }
            double saldo;
            try {
                saldo = Double.parseDouble(saldoTexto);
                if (!Double.isFinite(saldo)) continue;
            } catch (NumberFormatException e) { continue; }
            if (indiceCuenta(codigo) >= 0) { continue; }
            agregarCuenta(codigo, nombre, tipo, nat);
            cuentaSaldos.set(cuentaSaldos.size() - 1, saldo);
            validos++;
        }
        if (validos == 0 && lineas.isEmpty()) {
            System.out.println("ADVERTENCIA: no hay cuentas validas en " + ARCHIVO_CUENTAS + ".");
        }
        return validos > 0;
    }

    static void cargarAsientosTexto() {
        asientoFechas.clear(); asientoDescripciones.clear(); asientoTotales.clear();
        partidaCodigos.clear(); partidaTipos.clear(); partidaMontos.clear();
        saldosCarga = new double[cuentaCodigos.size()];
        File archivo = new File(ARCHIVO_ASIENTOS);
        if (!archivo.exists()) return;
        List<String> bloque = new ArrayList<>();
        int lineaInicio = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
            String linea; int numero = 0;
            while ((linea = reader.readLine()) != null) {
                numero++;
                if (linea.startsWith("\uFEFF")) linea = linea.substring(1);
                if (linea.trim().isEmpty()) continue;
                String etiqueta = linea.split("\\|", -1)[0].trim();
                if (etiqueta.equals("ASIENTO")) {
                    if (!bloque.isEmpty()) cargarBloque(bloque, lineaInicio);
                    bloque.clear(); bloque.add(linea); lineaInicio = numero;
                } else if (!bloque.isEmpty()) {
                    bloque.add(linea);
                } else {
                    System.out.println("ADVERTENCIA " + ARCHIVO_ASIENTOS + " linea " + numero + ": linea desconocida. Omitida.");
                    datosInvalidos = true;
                }
            }
            if (!bloque.isEmpty()) cargarBloque(bloque, lineaInicio);
        } catch (IOException e) {
            System.out.println("Error leyendo " + ARCHIVO_ASIENTOS + ": " + e.getMessage());
            datosInvalidos = true;
        }
    }

    static void cargarBloque(List<String> bloque, int numero) {
        String[] encabezado = bloque.get(0).split("\\|", -1);
        String error = encabezado.length != 4 ? "se esperaban 4 campos en ASIENTO" : null;
        String fecha = encabezado.length > 1 ? encabezado[1].trim() : "";
        String descripcion = encabezado.length > 2 ? encabezado[2].trim() : "";
        if (error == null && (fecha.isEmpty() || descripcion.isEmpty())) error = "fecha o descripcion vacia";
        LocalDate fechaValida = null;
        if (error == null) {
            fechaValida = parsearFecha(fecha);
            if (fechaValida == null) error = "fecha invalida";
        }
        List<String> codigos = new ArrayList<>(), tipos = new ArrayList<>();
        List<Double> montos = new ArrayList<>();
        for (int i = 1; i < bloque.size() && error == null; i++) {
            String[] p = bloque.get(i).split("\\|", -1);
            if (!p[0].trim().equals("PARTIDA") || p.length != 4) { error = "linea invalida dentro del asiento"; break; }
            String codigo = p[1].trim(), tipo = p[2].trim();
            if (tipo.equalsIgnoreCase("Debito")) tipo = "Debito";
            else if (tipo.equalsIgnoreCase("Credito")) tipo = "Credito";
            else { error = "tipo de partida invalido"; break; }
            if (indiceCuenta(codigo) < 0) { error = "la cuenta " + codigo + " no existe"; break; }
            Double monto = parsearMonto(p[3].trim());
            if (monto == null) { error = "monto invalido (minimo 0.01)"; break; }
            codigos.add(codigo); tipos.add(tipo); montos.add(monto);
        }
        if (error == null && codigos.size() < 2) error = "minimo 2 partidas";
        if (error == null) {
            double debe = Asientos.totalLado(tipos, montos, "Debito"), haber = Asientos.totalLado(tipos, montos, "Credito");
            if (debe <= 0 || haber <= 0) error = "debe haber partidas al Debe y al Haber";
            else if (!Asientos.cuadra(debe, haber)) error = "no cuadra: Debe " + formatearMonto(debe) + ", Haber " + formatearMonto(haber);
        }
        if (error == null) {
            String err = Asientos.validarSaldoContrarioEnCarga(codigos, tipos, montos, saldosCarga, cuentaNaturalezas, cuentaNombres);
            if (err != null) error = err;
        }
        if (error != null) {
            System.out.println("ADVERTENCIA " + ARCHIVO_ASIENTOS + " linea " + numero + ": " + error + ". Asiento omitido.");
            datosInvalidos = true; return;
        }
        for (int i = 0; i < codigos.size(); i++) {
            int idx = indiceCuenta(codigos.get(i));
            saldosCarga[idx] += Asientos.efectoSaldoNaturaleza(idx, tipos.get(i), montos.get(i));
        }
        Asientos.agregarAsiento(fechaValida.format(FORMATO_FECHA), descripcion, codigos, tipos, montos, Asientos.totalLado(tipos, montos, "Debito"));
    }

    static void guardarDatos() {
        if (datosInvalidos) { System.out.println("No se guardaron cambios: hay datos invalidos en los archivos."); return; }
        if (!guardarAsientos() || !guardarCuentas()) System.out.println("Error guardando.");
    }

    static boolean guardarCuentas() {
        StringBuilder contenido = new StringBuilder();
        for (int i = 0; i < cuentaCodigos.size(); i++)
            contenido.append(cuentaCodigos.get(i)).append('|').append(cuentaNombres.get(i)).append('|')
                    .append(cuentaTipos.get(i)).append('|').append(cuentaNaturalezas.get(i)).append('|')
                    .append(formatearMonto(cuentaSaldos.get(i))).append(System.lineSeparator());
        return escribirArchivoAtomico(ARCHIVO_CUENTAS, contenido.toString());
    }

    static boolean guardarAsientos() {
        StringBuilder contenido = new StringBuilder();
        for (int i = 0; i < asientoFechas.size(); i++) {
            contenido.append("ASIENTO|").append(asientoFechas.get(i)).append('|')
                    .append(asientoDescripciones.get(i)).append('|').append(formatearMonto(asientoTotales.get(i))).append(System.lineSeparator());
            List<String> codigos = partidaCodigos.get(i), tipos = partidaTipos.get(i);
            List<Double> montos = partidaMontos.get(i);
            for (int j = 0; j < codigos.size(); j++)
                contenido.append("PARTIDA|").append(codigos.get(j)).append('|').append(tipos.get(j)).append('|')
                        .append(formatearMonto(montos.get(j))).append(System.lineSeparator());
        }
        return escribirArchivoAtomico(ARCHIVO_ASIENTOS, contenido.toString());
    }

    static boolean escribirArchivoAtomico(String ruta, String contenido) {
        File temporal = new File(ruta + ".tmp");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(temporal))) {
            writer.write(contenido);
        } catch (IOException e) {
            System.out.println("Error escribiendo " + ruta + ": " + e.getMessage()); temporal.delete(); return false;
        }
        try {
            Files.move(temporal.toPath(), new File(ruta).toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (AtomicMoveNotSupportedException e) {
            try { Files.move(temporal.toPath(), new File(ruta).toPath(), StandardCopyOption.REPLACE_EXISTING); return true; }
            catch (IOException ex) { System.out.println("Error escribiendo " + ruta + ": " + ex.getMessage()); temporal.delete(); return false; }
        } catch (IOException e) {
            System.out.println("Error escribiendo " + ruta + ": " + e.getMessage()); temporal.delete(); return false;
        }
    }

    static boolean guardarBalance(String fecha, double activo, double pasivo, double patrimonio) {
        return escribirArchivoAtomico(ARCHIVO_BALANCE, fecha + "|" + formatearMonto(activo) + "|" + formatearMonto(pasivo) + "|" + formatearMonto(patrimonio) + System.lineSeparator());
    }

    static void respaldarInformacion() {
        System.out.println("\n--- RESPALDO ---");
        File carpeta = new File("backup/manual_" + LocalDateTime.now().format(FORMATO_TIMESTAMP));
        if (!carpeta.mkdirs()) { System.out.println("No se pudo crear la carpeta de respaldo."); return; }
        String[] archivos = {ARCHIVO_CUENTAS, ARCHIVO_ASIENTOS, ARCHIVO_BALANCE};
        int copiados = 0;
        try {
            for (String ruta : archivos) {
                File origen = new File(ruta);
                if (origen.exists()) {
                    Files.copy(origen.toPath(), new File(carpeta, origen.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    copiados++;
                }
            }
        } catch (IOException e) {
            System.out.println("Error creando respaldo: " + e.getMessage());
            File[] creados = carpeta.listFiles();
            if (creados != null) for (File f : creados) f.delete();
            carpeta.delete(); return;
        }
        if (copiados == 0) { carpeta.delete(); System.out.println("No hay datos para respaldar."); return; }
        System.out.println("Respaldo creado en: " + carpeta.getPath() + " (" + copiados + " archivos).");
    }
}