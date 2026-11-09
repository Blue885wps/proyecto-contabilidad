import java.time.LocalDate;
import java.util.Scanner;

public class Consola {
    static final Scanner scanner = new Scanner(System.in);
    static boolean entradaCerrada = false;

    static void linea(String... textos) { for (String texto : textos) System.out.println(texto); }

    static int menu(String titulo, String... opciones) {
        System.out.println("\n--- " + titulo + " ---");
        for (int i = 0; i < opciones.length; i++) System.out.println((i + 1) + ". " + opciones[i]);
        System.out.println("0. Volver");
        return leerInt("Seleccione una opcion (0-" + opciones.length + "): ", 0, opciones.length);
    }

    static String leerFecha(String mensaje) {
        String hoy = Fechas.fechaActual();
        while (true) {
            System.out.print(mensaje + " (dd/MM/yyyy, Enter = hoy " + hoy + "): ");
            if (!scanner.hasNextLine()) { entradaCerrada = true; System.out.println("No hay mas entrada."); return null; }
            String e = scanner.nextLine().trim();
            if (e.isEmpty()) return hoy;
            LocalDate f = Fechas.parsearFecha(e);
            if (f != null && f.getYear() >= 1900 && f.getYear() <= 2100) return f.format(Fechas.FORMATO_FECHA);
            System.out.println("Fecha invalida. dd/MM/yyyy, 1900-2100.");
        }
    }

    static String leerFechaRango(String mensaje) {
        while (true) {
            System.out.print(mensaje);
            if (!scanner.hasNextLine()) { entradaCerrada = true; System.out.println("No hay mas entrada disponible."); return null; }
            String entrada = scanner.nextLine().trim();
            if (entrada.isEmpty()) return "";
            LocalDate fecha = Fechas.parsearFecha(entrada);
            if (fecha != null && fecha.getYear() >= 1900 && fecha.getYear() <= 2100) return fecha.format(Fechas.FORMATO_FECHA);
            System.out.println("Fecha invalida. Use dd/MM/yyyy y una fecha real entre 1900 y 2100.");
        }
    }

    static boolean confirmar(String mensaje) {
        while (true) {
            String r = leerString(mensaje);
            if (r == null) { entradaCerrada = true; System.out.println("Entrada cerrada."); return false; }
            if (r.equalsIgnoreCase("S") || r.equalsIgnoreCase("SI")) return true;
            if (r.equalsIgnoreCase("N") || r.equalsIgnoreCase("NO")) return false;
            System.out.println("Responda S o N.");
        }
    }

    static String leerEntrada(String msg) {
        while (true) {
            System.out.print(msg);
            if (!scanner.hasNextLine()) { entradaCerrada = true; System.out.println("No hay mas entrada."); return null; }
            String e = scanner.nextLine().trim();
            if (!e.isEmpty()) return e;
            System.out.println("No puede estar vacio.");
        }
    }

    static String leerString(String msg) { return leerEntrada(msg); }

    static int leerInt(String msg, int min, int max) {
        while (true) {
            try {
                String t = leerEntrada(msg);
                if (t == null) { entradaCerrada = true; return min; }
                int v = Integer.parseInt(t);
                if (v >= min && v <= max) return v;
                System.out.println("Entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) { System.out.println("Entero invalido."); }
        }
    }

    static double leerDouble(String msg) {
        while (true) {
            try {
                String t = leerEntrada(msg);
                if (t == null) { entradaCerrada = true; return 0.0; }
                double v = parsearMontoSimple(t);
                if (!Double.isFinite(v) || v <= 0) { System.out.println("El monto debe ser un numero mayor que cero."); continue; }
                return v;
            } catch (NumberFormatException e) {
                if ("monto ambiguo".equals(e.getMessage())) {
                    linea("Monto invalido: hay mas de dos decimales o el separador es ambiguo.",
                            "Escriba 1800 o use el formato 1.800,00.");
                } else {
                    System.out.println("Monto invalido. Use solo numeros y coma o punto decimal");
                }
            }
        }
    }

    // Parseo simple: acepta coma o punto, 1.800,50 / 1,800.50 / 1800.50 / 1800,50
    static double parsearMontoSimple(String e) {
        String v = e == null ? "" : e.trim().replace(" ", "");
        if (v.isEmpty()) throw new NumberFormatException();
        // cuenta separadores
        int c = 0, p = 0;
        for (char ch : v.toCharArray()) { if (ch == ',') c++; else if (ch == '.') p++; }
        if (c > 1 || p > 1) throw new NumberFormatException(); // multiples separadores iguales -> error generico
        // normaliza: si tiene ambos, el ultimo es decimal
        int lc = v.lastIndexOf(','), lp = v.lastIndexOf('.');
        if (lc >= 0 && lp >= 0) {
            char dec = lc > lp ? ',' : '.';
            char mil = lc > lp ? '.' : ',';
            int pos = v.lastIndexOf(dec);
            String entera = v.substring(0, pos);
            String decimales = v.substring(pos + 1);
            String[] grupos = entera.split(mil == '.' ? "\\." : ",");
            if (grupos.length > 1) {
                if (grupos[0].isEmpty() || grupos[0].length() > 3) throw new NumberFormatException();
                for (int i = 1; i < grupos.length; i++) {
                    if (grupos[i].length() != 3) throw new NumberFormatException();
                }
                entera = entera.replace(String.valueOf(mil), "");
            }
            v = entera + (decimales.isEmpty() ? "" : "." + decimales);
        } else if (lc >= 0 || lp >= 0) {
            int pos = lc >= 0 ? lc : lp;
            String decimales = v.substring(pos + 1);
            if (decimales.length() > 2) throw new NumberFormatException("monto ambiguo");
            v = v.substring(0, pos) + "." + decimales;
        }
        // valida
        if (!v.matches("-?\\d+(\\.\\d{1,2})?")) throw new NumberFormatException("monto ambiguo");
        double d = Double.parseDouble(v);
        if (!Double.isFinite(d)) throw new NumberFormatException();
        return d;
    }

    static String leerTextoPersistible(String msg, String campo) {
        while (true) {
            String t = leerString(msg);
            if (t == null) { entradaCerrada = true; return ""; }
            if (!t.contains("|")) return t;
            System.out.println("El campo " + campo + " no puede contener el caracter |.");
        }
    }

    static String leerTextoOpcional(String msg) {
        System.out.print(msg);
        if (!scanner.hasNextLine()) { entradaCerrada = true; System.out.println("No hay mas entrada."); return ""; }
        return scanner.nextLine().trim();
    }
}
