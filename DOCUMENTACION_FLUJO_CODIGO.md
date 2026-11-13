# Documentación de Flujo de Código por Opción de Menú

## Sistema Contable Empresarial - Versión 1.0

---

## Arquitectura General

- **4 módulos `static`** en `src/`:
  - `Contabilidad.java` — `main()`, menús, validación de entrada (`leerEntrada`, `leerInt`, `leerDouble`, `leerFecha`, `confirmar`), `salir()`
  - `Datos.java` — Listas paralelas (cuentas, asientos, partidas), catálogo, persistencia, utilidades (fechas, montos, texto), búsqueda de cuentas
  - `Asientos.java` — Registro de asientos, partida doble, cálculo de saldos, movimientos por cuenta, validaciones contables
  - `Reportes.java` — 6 reportes: Cuenta T, Libro Diario, Libro Mayor, Balanza, Estado Resultados, Balance General

- **Estructura de datos**: Listas paralelas primitivas (`List<String>`, `List<Double>`, `List<List<String>>`, `List<List<Double>>`) — **sin objetos de dominio**, sin `Map`, sin `Object[]`

- **Persistencia**: Archivos de texto plano en `data/` con separador `|`, escritura atómica (`.tmp` + `Files.move`)

- **Punto de entrada**: `Contabilidad.main()` → `Datos.cargarDatos()` → bucle menú principal

---

## Opción 1: Administrar Cuentas

### 1.1 Ver Catálogo Completo
```
Contabilidad.menuAdministrarCuentas()
  → case 1: Datos.mostrarCatalogo()
      → Recorre cuentaCodigos (75 items)
      → Para cada i:
          - saldoNaturaleza = Asientos.saldoNaturaleza(i, cuentaSaldos.get(i))
          - contrario = Asientos.esSaldoContrario(i, saldoNaturaleza)
          - Imprime: código, nombre, tipo, naturaleza, saldoNaturaleza, "(contrario)" si aplica
      → Nota final: "los saldos en el lado contrario a la naturaleza se marcan en la columna Estado"
```

**Métodos clave:**
- `Asientos.saldoNaturaleza(idx, saldoTipo)` — Convierte saldo tipo-deudor a convención naturaleza (Mixta = directo; Acreedora = -saldoDebito; Deudora = saldoDebito)
- `Asientos.esSaldoContrario(idx, saldoNaturaleza)` — `!esMixta && saldoNaturaleza < -TOLERANCIA`

---

### 1.2 Crear Cuenta
```
Datos.crearCuenta()
  → if (datosInvalidos) return
  → Paso 1: leerTextoPersistible("Nombre") → valida sin '|'
  → Paso 2: pedirTipoCuenta() → menú 1-6 desde TIPOS_CUENTA[]
  → Paso 3: pedirNaturalezaCuenta(tipo)
      → menú 1=Deudora, 2=Acreedora, 3=Mixta
      → sugiere: tipoDeudor(tipo) ? "Deudora" : "Acreedora"
  → Paso 4: leerEntrada("Codigo")
      → valida: no "0", no "?", no '|', no duplicado (indiceCuenta)
  → agregarCuenta(codigo, nombre, tipo, naturaleza) → 5 listas paralelas
  → guardarCuentas() → escritura atómica data/cuentas.txt
```

**Validaciones:** código único, sin caracteres reservados, tipo válido, naturaleza válida.

---

### 1.3 Buscar Cuenta
```
Datos.buscarCuenta()
  → pedirCuenta("Cuenta a buscar")
      → while true:
          - leerEntrada() → si null/0 → return null; si "?" → mostrarCatalogo(); continue
          - indiceCuenta(entrada) → si exacto → mostrarCuentaEncontrada() → return codigo
          - buscarCuentas(entrada) → List<Integer> índices
              → normalizarTexto() → NFD, sin diacríticos, lowerCase, colapsa espacios
              → solo dígitos → prefijo de código
              → multi-término → todos deben contenerse en (codigo + " " + nombre + " " + tipo + " " + naturaleza)
          - 0 coincidencias → "La cuenta no existe..." → continue
          - 1 coincidencia → auto-selecciona
          - 2-12 → lista numerada → leerEntrada("Elija 1-N, V, ?, 0")
              → "V" → break (vuelve a buscar)
              → "?" → mostrarCatalogo()
              → número válido → return cuenta
          - >12 → "Escriba más caracteres..." → continue
```

**Retorna:** Código de cuenta seleccionado o `null` si canceló.

---

## Opción 2: Registrar Operación Contable

```
Asientos.registrarAsiento()
  → if (Datos.datosInvalidos) return
  → Paso 1: Contabilidad.leerFecha("Fecha del asiento")
      → Enter = hoy (Datos.fechaActual())
      → Datos.parsearFecha() → regex dd/MM/yyyy + LocalDate.parse(FORMATO_FECHA_ESTRICTA) + rango 1900-2100
  → Paso 2: leerTextoPersistible("Descripción") → sin '|'
  → Paso 3: Bucle partidas
      → saldosSimulados = saldosNaturaleza() (copia local para validar antes de guardar)
      → pedirPartida(codigos, tipos, montos, totales, saldosSimulados)
          → Datos.pedirCuenta("Cuenta de la partida N") → usa 1.3
          → pedirTipoPartida(tipoCuenta, nombre, naturaleza)
              → menú: 1=Debe (débito), 2=Haber (crédito)
              → pista: "aumenta con el Debe/Haber" según naturaleza
          → leerDouble("Monto") → Datos.parsearMonto()
              → acepta: 1800, 1800.50, 1800,50, 1.800,50, 1,800.50
              → rechaza: 1.800 (ambiguo), 0.001 (3 decimales), 1e3, negativos, 0, NaN, Infinity
          → efecto = efectoSaldoNaturaleza(idx, tipo, monto)
          → resultante = saldosSimulados[idx] + efecto
          → if (!esMixta && resultante < -TOLERANCIA) → RECHAZA: "PARTIDA RECHAZADA... saldo contrario"
          → agrega a listas locales, actualiza totales Debe/Haber
          → muestra acumulado, diferencia si no cuadra
      → if (codigos.size() < 2) → "Faltan partidas..." → continue
      → if (!cuadra(totales)) → "El asiento no cuadra... Desea agregar otra? S/N" → if N → cancelar todo
      → if (cuadra) → "Desea agregar otra? S/N" → if N → break
  → REVISIÓN FINAL: muestra fecha, descripción, partidas, totales → confirmar S/N
  → agregarAsiento(fecha, descripcion, codigos, tipos, montos, totales[0])
  → recalcularSaldos() → Datos.cuentaSaldos = Asientos.saldos()
  → guardarAsientos() + guardarCuentas() → escritura atómica
```

**Métodos clave:**
- `efectoSaldoNaturaleza(idx, tipoPartida, monto)` — `debito ? +/-monto : -/+monto` según naturaleza (Acreedora invierte; Mixta usa tipoDeudor)
- `cuadra(debe, haber)` — `Math.abs(debe - haber) <= TOLERANCIA + EPSILON` (0.005)
- `recalcularSaldos()` — recalcula TODOS los saldos desde partidas (fuente de verdad)

---

## Opción 3: Consultar Cuentas T

```
Reportes.verCuentaT()
  → Datos.pedirCuenta("Cuenta a consultar") → usa 1.3
  → if (null) return
  → mostrarCuentaT(idx)
      → Asientos.cargarMovimientosCuenta(codigo, asientos[], partidas[])
          → recorre todos los asientos, filtra partidas de esa cuenta
          → ordenarMovimientos() → burbuja por fechaOrdenable(aaaammdd)
      → Separa en filasDebe / filasHaber con: fecha, concepto(truncado), monto
      → Imprime encabezado 2 columnas: "Fecha Concepto Monto | Fecha Concepto Monto"
      → Totales Debe/Haber
      → saldoDebito = totalDebe - totalHaber
      → saldoTipo = tipoDeudor(tipo) ? saldoDebito : -saldoDebito
      → saldoNaturaleza = Asientos.saldoNaturaleza(idx, saldoTipo)
      → contrario = Asientos.esSaldoContrario(idx, saldoNaturaleza)
      → SALDO: monto (etiquetaSaldoCuenta) + ", contrario" si aplica
      → if (contrario) → "ADVERTENCIA: saldo contrario a la naturaleza X de la cuenta"
```

---

## Opción 4: Generar Libro Diario

```
Reportes.verLibroDiario()
  → indices = 0..asientoFechas.size()-1
  → if empty → "No hay asientos registrados"
  → Asientos.ordenarMovimientos(indices, null) → por fecha
  → Formato clásico: "Cta | Detalle | Parcial | Debe | Haber"
  → Para cada asiento ordenado:
      - totalGeneralDebe += totalLado(tipos, montos, "Debito")
      - totalGeneralHaber += totalLado(tipos, montos, "Credito")
      - Imprime "Asiento #N"
      - Partidas Debito primero → mostrarLineaDiario(codigo, monto, false)
      - Partidas Credito → mostrarLineaDiario(codigo, monto, true) (prefijo "A " en detalle)
      - Fecha y descripción al pie
      - Separador "-"
  → TOTAL GENERAL: Debe | Haber
  → "Total de asientos: X"
```

---

## Opción 5: Generar Libro Mayor

```
Reportes.verLibroMayor()
  → Datos.pedirCuenta("Cuenta a consultar") → 1.3
  → Asientos.cargarMovimientosCuenta() + ordenarMovimientos()
  → Imprime: "As. | Fecha | Concepto | Lado | Monto | Saldo"
  → saldo = 0
  → Para cada movimiento:
      - lado = partidaTipos[i][j]
      - monto = partidaMontos[i][j]
      - saldo += efectoSaldoNaturaleza(idx, lado, monto)
      - Imprime: N° asiento, fecha, concepto(24), lado(Debe/Haber), monto, saldo
  → Final: saldo final con etiquetaSaldoCuenta + ", contrario" si esSaldoContrario()
  → if (contrario) → ADVERTENCIA
```

---

## Opción 6: Balanza de Comprobación

```
Reportes.verBalanceComprobacion()
  → Asientos.movimientos() → double[][] {debitos[75], creditos[75]}
  → Recorre todas las cuentas:
      - if (debitos[i]<=0 && creditos[i]<=0) continue
      - saldo = debitos[i] - creditos[i]
      - deudor = saldo > TOL ? saldo : 0
      - acreedor = saldo < -TOL ? -saldo : 0
      - Imprime: codigo, nombre, debitos, creditos, deudor, acreedor
      - Acumula totales
  → TOTALES: Debitos, Creditos, Deudor, Acreedor
  → if (cuadra(totalDebe, totalHaber)) → "LA BALANZA DE COMPROBACION ESTA CUADRADA."
    else → ADVERTENCIA + diferencia
```

---

## Opción 7: Estado de Resultados

```
Reportes.verEstadoResultados()
  → saldos = Asientos.saldos() (tipo-deudor: Activo/Costo/Gasto = D-H; Pasivo/Patrimonio/Ingreso = H-D)
  → totalIngresos = mostrarSeccion("Ingreso", "INGRESOS", saldos)
  → totalCostos = mostrarSeccion("Costo", "COSTOS", saldos)
  → totalGastos = mostrarSeccion("Gasto", "GASTOS", saldos)
  → utilidadBruta = totalIngresos - totalCostos
  → utilidadNeta = utilidadBruta - totalGastos
  → Imprime: Utilidad bruta, Utilidad neta, Resultado (monto + "Utilidad"/"Perdida"/"Cero")
```

**mostrarSeccion(tipo, titulo, saldos):**
- Filtra cuentas por tipo, imprime código, nombre, saldo(tipo-deudor)
- Retorna suma de saldos (en convención tipo-deudor)

---

## Opción 8: Balance General

```
Reportes.generarBalance()
  → saldos = Asientos.saldos()
  → totalActivo = mostrarSeccion("Activo", "ACTIVO", saldos)
  → totalPasivo = mostrarSeccion("Pasivo", "PASIVO", saldos)
  → totalPatrimonio = mostrarSeccion("Patrimonio", "PATRIMONIO", saldos)
  → resultado = Asientos.resultadoDeSaldos(saldos)
      → Suma: Ingreso (+), Costo (-), Gasto (-) en convención tipo-deudor
  → patrimonioConResultado = totalPatrimonio + resultado
  → Imprime: "Resultado acumulado: X"
  → avisarDobleConteo(saldos, resultado)
      → if |resultado| > TOL && (|saldo3110|>TOL || |saldo3120|>TOL)
          → "ADVERTENCIA: posible doble conteo... 3110/3120 también contienen el resultado"
  → Total Pasivo + Patrimonio + Resultado
  → Diferencia = totalActivo - (totalPasivo + patrimonioConResultado)
  → if cuadra → "EL BALANCE CUADRA CORRECTAMENTE." else ADVERTENCIA
  → Datos.guardarBalance(fechaActual, totalActivo, totalPasivo, patrimonioConResultado)
      → data/balance.txt: fecha|activo|pasivo|patrimonio+resultado
```

---

## Opción 9: Consultar Movimientos

```
Asientos.mostrarAsientos()
  → if empty → "No hay asientos registrados"
  → Para i = 0..size-1:
      - "Asiento #N"
      - "Fecha: X"
      - "Descripción: Y"
      - mostrarPartidas(partidaCodigos[i], partidaTipos[i], partidaMontos[i])
          → tabla: Codigo | Nombre | Lado | Monto
      - "Total del asiento: Z"
```

---

## Opción 10: Respaldar Información

```
Datos.respaldarInformacion()
  → carpeta = "backup/manual_YYYYMMDD_HHmmss_SSS"
  → mkdirs()
  → Para cada archivo en {cuentas.txt, asientos.txt, balance.txt}:
      - if (exists) Files.copy(..., REPLACE_EXISTING)
      - contador++
  → if (copiados==0) delete carpeta → "No hay datos para respaldar"
  → "Respaldo creado en: ruta (N archivos)"
```

---

## Opción 11 / 0: Salir

```
Contabilidad.salir()
  → "Guardando y saliendo..."
  → Datos.guardarDatos()
      → if (datosInvalidos) → "No se guardaron cambios..." → return
      → guardarAsientos() && guardarCuentas()
  → "Hasta luego."
  → System.exit(0) implícito (return de main)
```

---

## Métodos Transversales Clave

| Módulo | Método | Descripción |
|--------|--------|-------------|
| `Datos` | `cargarDatos()` | Inicio: mkdirs data/, `cargarCuentasTexto()` (o inicializa catálogo), `cargarAsientosTexto()`, `Asientos.recalcularSaldos()` |
| `Datos` | `cargarCuentasTexto()` | Lee `cuentas.txt` 5 campos (migra 4→5 infiriendo naturaleza), valida, `datosInvalidos` si hay errores |
| `Datos` | `cargarAsientosTexto()` | Bloques `ASIENTO` + `PARTIDA*`, valida fecha real, 4 campos, ≥2 partidas, cuentas existen, tipo válido, monto válido, Debe=Haber, sin saldo contrario → `Asientos.agregarAsiento()` |
| `Datos` | `guardarCuentas()` / `guardarAsientos()` | `StringBuilder` + `escribirArchivoAtomico()` |
| `Datos` | `escribirArchivoAtomico(ruta, contenido)` | Escribe `.tmp`, `Files.move(ATOMIC_MOVE)` con fallback sin atómico |
| `Datos` | `parsearFecha(texto)` | Regex `dd/MM/yyyy` + `LocalDate.parse(FORMATO_FECHA_ESTRICTA)` + rango 1900-2100 |
| `Datos` | `parsearMonto(texto)` | `replace(',', '.')` + valida ≤2 decimales, rechaza ambiguos (`1.800`), científica, <0.01, NaN, Infinity |
| `Datos` | `normalizarTexto(texto)` | NFD + sin diacríticos + lowerCase + trim + colapsa espacios |
| `Datos` | `buscarCuentas(texto)` | Multi-término normalizado, prefijo si solo dígitos, límite 12 resultados |
| `Datos` | `fechaOrdenable(fecha)` | `aaaammdd` para ordenamiento |
| `Asientos` | `efectoSaldoNaturaleza(idx, tipo, monto)` | +/- monto según naturaleza y Debe/Haber |
| `Asientos` | `saldoNaturaleza(idx, saldoTipo)` | Convierte saldo tipo-deudor → convención naturaleza |
| `Asientos` | `esSaldoContrario(idx, saldoNaturaleza)` | `!Mixta && saldo < -TOLERANCIA` |
| `Asientos` | `etiquetaSaldoCuenta(idx, saldoNaturaleza)` | "Deudor"/"Acreedor"/"Cero" según naturaleza (o tipo para Mixta) |
| `Asientos` | `tipoDeudor(tipo)` | `Activo || Costo || Gasto` |
| `Asientos` | `cuadra(debe, haber)` | `|debe-haber| <= TOLERANCIA + EPSILON` |
| `Asientos` | `recalcularSaldos()` | `Datos.cuentaSaldos = saldos()` (recalcula TODO desde partidas) |
| `Contabilidad` | `leerEntrada/leerInt/leerDouble/leerFecha/confirmar` | Validación robusta, `entradaCerrada` para EOF |

---

## Archivos de Datos

| Archivo | Formato | Descripción |
|---------|---------|-------------|
| `data/cuentas.txt` | `codigo|nombre|tipo|naturaleza|saldo` | 75 líneas, saldos en convención tipo-deudor |
| `data/asientos.txt` | `ASIENTO|fecha|desc|total`<br>`PARTIDA|codigo|tipo|monto` | Bloques contiguos, total = suma Débitos |
| `data/balance.txt` | `fecha|activo|pasivo|patrimonio+resultado` | Último balance generado |
| `data/catalogo_estandar.txt` | `codigo|nombre|tipo` | 75 cuentas base para inicialización |
| `data/demo/cuentas.txt` | Igual que cuentas.txt | Ejercicio demo con 37 saldos ≠ 0 |
| `data/demo/asientos.txt` | Igual que asientos.txt | 26 asientos demo (fecha 12/11/2026) |
| `data/demo/balance.txt` | Igual que balance.txt | Balance del demo |
| `backup/manual_*/` | Copias de los 3 archivos | Respaldo manual (opción 10) |

---

## Constantes Globales

```java
Datos.TOLERANCIA = 0.005          // Comparación montos (partida doble, saldos)
Datos.EPSILON = 1e-9              // Precisión double
Datos.ANIO_MIN = 1900, ANIO_MAX = 2100
Datos.TIPOS_CUENTA = {Activo, Pasivo, Patrimonio, Ingreso, Costo, Gasto}
Datos.NATURALEZAS = {Deudora, Acreedora, Mixta}
Datos.FORMATO_FECHA = dd/MM/yyyy
Datos.FORMATO_FECHA_ESTRICTA = dd/MM/uuuu con ResolverStyle.STRICT
```

---

## Flujo de Inicio Completo

```
main()
  → println "=== SISTEMA CONTABLE EMPRESARIAL ==="
  → Datos.cargarDatos()
      → new File("data").mkdirs()
      → if (!cargarCuentasTexto())  // archivo no existe o 0 válidas
            → "Inicializando catálogo..."
            → inicializarCuentas()  // lee catalogo_estandar.txt
            → if (!datosInvalidos) guardarCuentas()
        else if (cuentaCodigos.isEmpty())
            → "ADVERTENCIA: no hay cuentas válidas..."
            → inicializarCuentas(); datosInvalidos=true
      → cargarAsientosTexto()
      → Asientos.recalcularSaldos()
  → while (true)
      → mostrarMenu()
      → opcion = leerInt("Seleccione (0-11): ", 0, 11)
      → if (entradaCerrada) { salir(); return; }
      → switch(opcion) { case 1..11 }
```

---

## Manejo de Errores y Estados Especiales

| Situación | Comportamiento |
|-----------|----------------|
| `datosInvalidos = true` | Bloquea `guardarDatos()`, `crearCuenta()`, `registrarAsiento()`; permite solo consultas |
| EOF (`scanner.hasNextLine() == false`) | `entradaCerrada = true`, retorna valores seguros (`null`, `min`, `0.0`), cancela operación actual |
| Línea inválida en `cuentas.txt` | `ADVERTENCIA... linea omitida`, `datosInvalidos=true`, continua con siguiente |
| Asiento inválido en `asientos.txt` | `ADVERTENCIA... asiento omitido`, `datosInvalidos=true`, no carga ese bloque |
| Saldo contrario al cargar | Asiento omitido completo, `datosInvalidos=true` |
| Error escritura atómica | Fallback sin `ATOMIC_MOVE`; si falla → `.tmp` borrado, archivo original intacto |
| `cuentas.txt` vacío | Catalogo estándar en memoria, `datosInvalidos=true`, no guarda hasta corregir |

---

## Validaciones de Entrada (Usuario)

| Campo | Método | Reglas |
|-------|--------|--------|
| Menú | `leerInt(msg, min, max)` | Entero en rango, muestra rango en error |
| Monto | `leerDouble(msg)` → `parsearMonto` | ≥0.01, ≤2 decimales, sin ambigüedad, sin científica, sin negativos |
| Fecha | `leerFecha(msg)` → `parsearFecha` | `dd/MM/yyyy` real, 1900-2100, Enter = hoy |
| Texto persistible | `leerTextoPersistible(msg, campo)` | Rechaza `|` |
| Confirmación | `confirmar(msg)` | Solo `S/SI` o `N/NO` (case-insensitive) |
| Código cuenta | `leerEntrada` en crear/buscar | No `0`, no `?`, no `|`, único |

---

## Referencias Rápidas: ¿Dónde está cada cosa?

| Qué buscas | Archivo / Método |
|------------|------------------|
| Menú principal | `Contabilidad.mostrarMenu()` |
| Registro asiento | `Asientos.registrarAsiento()` |
| Partida doble | `Asientos.cuadra()` + `efectoSaldoNaturaleza()` |
| Saldos | `Asientos.saldos()` (tipo-deudor) / `saldosNaturaleza()` (naturaleza) |
| Cuenta T | `Reportes.mostrarCuentaT()` |
| Libro Diario | `Reportes.verLibroDiario()` |
| Libro Mayor | `Reportes.verLibroMayor()` |
| Balanza | `Reportes.verBalanceComprobacion()` |
| Estado Resultados | `Reportes.verEstadoResultados()` |
| Balance General | `Reportes.generarBalance()` |
| Búsqueda cuentas | `Datos.buscarCuentas()` / `pedirCuenta()` |
| Carga/Guardado | `Datos.cargarDatos()` / `guardarDatos()` |
| Validación montos | `Datos.parsearMonto()` |
| Validación fechas | `Datos.parsearFecha()` |
| Escritura atómica | `Datos.escribirArchivoAtomico()` |
| Inicio programa | `Contabilidad.main()` → `Datos.cargarDatos()` |

---

## Notas de Implementación Importantes

1. **No hay objetos de dominio** — Todo son índices en listas paralelas (`int idx = indiceCuenta(codigo)`)
2. **Fuente de verdad = partidas** — `cuentaSaldos` se recalcula siempre desde `partidaCodigos/Tipos/Montos`
3. **Convención de saldos**:
   - Archivo/guardado: **tipo-deudor** (Activo/Costo/Gasto = Deudor positivo; Pasivo/Patrimonio/Ingreso = Acreedor positivo)
   - Reportes/visualización: **naturaleza** (positivo = lado normal de la cuenta)
4. **Tolerancia 0.005** — Para comparaciones de double (partida doble, saldos cero, cuadres)
5. **Escritura atómica** — Nunca trunca archivo original; usa `.tmp` + `Files.move`
6. **75 cuentas fijas** — Catálogo estándar inmutable en `catalogo_estandar.txt`
7. **Sin cierre contable automático** — Balance suma resultado a patrimonio; aviso si 3110/3120 tienen saldo
8. **EOF handling** — `entradaCerrada` propaga cancelación limpia sin excepciones

---

*Documento generado automáticamente a partir del código fuente. Última actualización: 12/11/2026*