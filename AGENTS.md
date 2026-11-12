# AGENTS.md - Proyecto Contabilidad

## Descripcion general

Sistema de contabilidad en Java con interfaz de consola para registro de asientos
contables bajo el metodo de partida doble, con los seis reportes basicos de la
consigna y menu de 11 opciones. Los datos se persisten en archivos de texto plano
(`cuentas.txt`, `asientos.txt`, `balance.txt`) utilizando las librerias nativas
de Java (`java.io` / `java.nio`). Todos los reportes usan el historico completo.

## Estructura del proyecto

```
proyecto contabilidad/
├── src/
│   ├── Contabilidad.java    # main, menus, lecturas validadas y salida
│   ├── Datos.java           # listas paralelas, catalogo, fechas y persistencia
│   ├── Asientos.java        # registro, partida doble, saldos y movimientos
│   └── Reportes.java        # cuentas T, diario, mayor, balanza, resultados y balance
├── data/
│   ├── cuentas.txt          # Catalogo de cuentas con saldos
│   ├── asientos.txt         # Asientos contables registrados
│   └── balance.txt          # Ultimo balance generado
├── bin/
│   └── *.class              # Clases compiladas de los cuatro modulos
├── tests/
│   ├── pruebas_fase_d.ps1          # Pruebas de regresion en directorio temporal
│   └── pruebas_logica_contable.ps1 # Pruebas integrales de logica contable
└── AGENTS.md                       # Este archivo
```

> Desde el Cambio 35 la aplicacion se organiza en cuatro modulos `static`. El
> dominio sigue representado por las listas paralelas de `Datos` y no se
> instancian clases del proyecto.

## Reglas de codigo

### Estructura de datos

- Usar exclusivamente **ArrayList**, **Listas paralelas primitivas** (`List<String>`, `List<Double>`), **Colas**, **Matrices**, **Vectores**, **Pilas**
- **PROHIBIDO** usar `Object[]` o arreglos heterogéneos
- NO usar **Map**, **HashMap**, **LinkedHashMap** ni otras colecciones
  como estructura de datos principal del dominio

### Programacion Orientada a Objetos (POO) y modularizacion

- La logica contable y el dominio se implementan con estructuras de datos
  basicas (ArrayList, listas paralelas, colas, matrices, vectores, pilas) y
  variables primitivas. No se representan cuentas, asientos, partidas ni
  resultados con objetos de dominio.
- Se permite crear clases y archivos `.java` adicionales **unicamente** como
  modulos de organizacion del codigo. Cada modulo contiene metodos y constantes
  `static`; no se usa para crear objetos de dominio.
- NO usar herencia, polimorfismo ni interfaces.
- NO instanciar clases del proyecto para representar el dominio. Las clases
  adicionales se usan solo mediante sus metodos `static`.
- Las llamadas a metodos permitidas son las de clases estandar de Java y las
  de los modulos `static` del proyecto.
- Cada cuenta se representa mediante listas paralelas: `cuentaCodigos`,
  `cuentaNombres`, `cuentaTipos`, `cuentaSaldos`.
- Cada asiento se representa mediante listas paralelas y anidadas:
  `asientoFechas`, `asientoDescripciones`, `asientoTotales`, `partidaCodigos`,
  `partidaTipos`, `partidaMontos`.

### Patrones de codigo

- Variables globales declaradas como `static`
- Lectura de datos desde archivos de texto plano (`.txt`) al iniciar
- Escritura de archivos de texto plano despues de cada modificacion
- Nombres de metodos en camelCase
- Mensajes de interfaz en espanol
- Uso de `printf` con formato alineado para tablas

### Persistencia en Archivos de Texto

- **PROHIBIDO** usar Gson, librerías externas de JSON o serialización JSON
- Usar exclusivamente clases de `java.io` (`BufferedReader`, `PrintWriter`, `FileReader`, `FileWriter`) y `java.nio`
- Almacenar registros con formato de texto estructurado delimitado por tuberías (`|`)

### Validaciones

- Verificar que el tipo de cuenta sea valido antes de crear
- Verificar que no exista duplicidad de codigos
- Verificar que la cuenta exista antes de registrar un asiento
- Verificar que el tipo de partida sea "Debito" o "Credito"
- Verificar que debitos sean iguales a creditos (partida doble)
- Verificar que la fecha del asiento sea una fecha real en formato `dd/MM/yyyy`
- Verificar que la fecha este entre 1900 y 2100
- Tolerancia de 0.005 para comparaciones de montos

## Reglas contables

### Clasificacion de cuentas

| Tipo | Efecto Debito | Efecto Credito |
|------|---------------|----------------|
| Activo | Aumenta | Disminuye |
| Pasivo | Disminuye | Aumenta |
| Patrimonio | Disminuye | Aumenta |
| Ingreso | Disminuye | Aumenta |
| Costo | Aumenta | Disminuye |
| Gasto | Aumenta | Disminuye |

### Partida doble

- Cada asiento debe tener al menos 2 partidas
- El total de debitos debe ser igual al total de creditos
- Si no cuadra, el asiento se rechaza y no se registra

### Balance general

- Formula: Activo = Pasivo + Patrimonio + Resultado
- El sistema verifica esta igualdad al generar el balance

### Estado de resultados

- Utilidad bruta = Ingresos - Costos
- Utilidad neta = Utilidad bruta - Gastos
- Se calcula con los saldos de las cuentas de Ingreso, Costo y Gasto.

## Catalogo de cuentas (75 cuentas)

### Activo (22 cuentas)
1000 Caja, 1100 Bancos, 1110 Inversiones temporales, 1200 Cuentas por cobrar,
1210 Clientes, 1220 Documentos por cobrar, 1230 Deudores diversos,
1300 Inventarios, 1310 Materias primas, 1320 Productos terminados,
1400 Activos fijos, 1410 Mobiliario y equipo, 1420 Equipo de transporte,
1430 Equipo de computo, 1500 Activos diferidos, 1510 Gastos pagados por anticipado,
1520 Seguros pagados por anticipado, 1600 Cuentas por cobrar a largo plazo,
1700 Propiedad, planta y equipo, 1790 Depreciacion acumulada,
1800 Activos intangibles, 1890 Amortizacion acumulada

### Pasivo (15 cuentas)
2000 Cuentas por pagar, 2010 Proveedores, 2020 Acreedores diversos,
2100 Prestamos bancarios, 2110 Prestamos a corto plazo, 2120 Prestamos a largo plazo,
2200 Impuestos por pagar, 2210 IVA por pagar, 2220 ISR por pagar,
2300 Pasivos diferidos, 2310 Anticipos de clientes, 2320 Intereses cobrados por anticipado,
2400 Pasivos por impuestos diferidos, 2500 Obligaciones financieras a largo plazo,
2600 Pasivos por beneficios a empleados

### Patrimonio (10 cuentas)
3000 Capital social, 3010 Capital aportado, 3020 Capital ganado,
3100 Utilidades retenidas, 3110 Utilidad del ejercicio, 3120 Perdida del ejercicio,
3200 Resultados de ejercicios anteriores, 3300 Reservas legales, 3400 Ajustes por revaluacion,
3500 Dividendos decretados

### Ingreso (8 cuentas)
4000 Ingresos, 4010 Ventas, 4020 Ingresos por servicios,
4030 Ingresos por intereses, 4040 Otros ingresos, 4050 Ingresos por alquileres,
4060 Ingresos por comisiones, 4070 Ingresos por dividendos

### Costo (4 cuentas)
5000 Costos, 5010 Costo de ventas, 5020 Costo de produccion,
5030 Costo de lo vendido

### Gasto (16 cuentas)
6000 Gastos, 6010 Gastos de venta, 6020 Gastos de administracion,
6030 Gastos financieros, 6040 Gastos de operacion, 6050 Sueldos y salarios,
6060 Renta, 6070 Servicios publicos, 6080 Depreciacion, 6090 Amortizacion,
6100 Gastos de publicidad y mercadeo, 6110 Gastos de mantenimiento,
6120 Gastos de seguros, 6130 Gastos de impuestos, 6140 Gastos de capacitacion,
6150 Gastos de viajes y representacion

## Cambios realizados

### Historial resumido (Cambios 1-11)

- Cambio 1: Reemplazo de Map por Object[] (obsoleto por el Cambio 12).
- Cambio 2: Ampliacion del catalogo de cuentas a 60 cuentas.
- Cambio 3: Agregado estado de resultados (opcion 7).
- Cambio 4: Validacion de tipo de cuenta con tipoValido().
- Cambio 5: Validacion de duplicados de codigos de cuenta.
- Cambio 6: Expansion del catalogo de cuentas a 75 cuentas.
- Cambio 7: Estados financieros y reportes adicionales (flujo de efectivo, cambios en el patrimonio, balance de comprobacion, libro diario, libro mayor y reversion de asientos).
- Cambio 8: Validacion robusta de entradas (leerInt, leerDouble, leerString).
- Cambio 9: Eliminacion completa de Map (obsoleto por el Cambio 12).
- Cambio 10: Opcion 14 para reiniciar el catalogo de cuentas.
- Cambio 11: Validacion de fechas (leerFecha) y busqueda avanzada de asientos.

### Cambio 12: Migracion a listas paralelas y archivos de texto plano

**Razon:** El usuario prohibio el uso de `Object[]` y de la libreria Gson/JSON.
Se requería reemplazar las estructuras heterogeneas por listas paralelas
primitivas y la persistencia JSON por archivos de texto plano nativos.

**Impacto:**
- `src/Contabilidad.java`: se eliminaron los imports de Gson y los `List<Object[]>`;
  ahora se usan `cuentaCodigos`, `cuentaNombres`, `cuentaTipos`, `cuentaSaldos`,
  `asientoFechas`, `asientoDescripciones`, `asientoTotales` y las listas anidadas
  `partidaCodigos`, `partidaTipos`, `partidaMontos`.
- Persistencia: los metodos de lectura/escritura usan `BufferedReader`,
  `PrintWriter`, `FileReader` y `FileWriter` con formato delimitado por `|`.
- `data/`: `cuentas.json`, `asientos.json` y `balance.json` fueron migrados a
  `cuentas.txt`, `asientos.txt` y `balance.txt` (los originales quedaron como
  respaldo en `backup/json/` y el programa no los utiliza).
- `examples/`: los ejemplos ahora son `cuentas.txt.ejemplo`, `asientos.txt.ejemplo`
  y `balance.txt.ejemplo`.
- `AGENTS.md`: reglas actualizadas (prohibicion de `Object[]` y de JSON/Gson).
- `lib/gson.jar`: ya no se utiliza; puede eliminarse de forma segura.

### Cambio 13: Busqueda de cuentas por codigo o nombre en la opcion 3

**Razon:** El usuario no siempre recuerda los codigos de las 75 cuentas del
catalogo. El registro de asientos exigia escribir el codigo exacto y cancelaba
todo el asiento ante un error de tipeo.

**Impacto:**
- `src/Contabilidad.java`: en `registrarAsiento()` el ingreso directo del codigo
  se reemplazo por `pedirCuenta()`, que permite buscar por codigo o por nombre.
- Se agregaron los metodos `buscarCuentas()`, `indiceCuenta()`,
  `normalizarTexto()`, `mostrarCuentaEncontrada()` y `pedirCuenta()`.
- Si hay una sola coincidencia se usa directamente; si hay varias se muestra una
  lista numerada para elegir; si no hay coincidencias se vuelve a preguntar sin
  cancelar el asiento.
- Escribir `?` en el campo de cuenta muestra el catalogo completo y regresa a la
  busqueda.
- No se modifico el formato de los archivos de `data/` ni la logica de partida
  doble.

### Cambio 14: Reinicio completo de datos de prueba (opcion 14)

**Razon:** En fase beta se mezclaban datos de ejercicios y pruebas. La opcion 14
anterior solo restauraba el catalogo de cuentas, dejaba los asientos registrados
y no guardaba de inmediato, por lo que los reportes podian quedar inconsistentes.

**Impacto:**
- `src/Contabilidad.java`: `reiniciarCatalogo()` fue reemplazado por
  `reiniciarDatos()`.
- La opcion 14 ahora borra todos los asientos, pone los saldos en cero, restaura
  las 75 cuentas estandar y elimina `data/balance.txt`.
- Antes de borrar, `respaldarDatos()` copia `cuentas.txt`, `asientos.txt` y
  `balance.txt` a `backup/reinicio_YYYYMMDD_HHMMSS_SSS/`.
- Si el respaldo falla, el reinicio se cancela y no se modifica ningun dato.
- La confirmacion requiere escribir `REINICIAR`.
- Los archivos `cuentas.txt` y `asientos.txt` se guardan inmediatamente despues
  del reinicio.
- No se modifico el formato de los archivos de `data/`.

### Cambio 15: Integridad de saldos, resultado y fechas

**Razon:** Los saldos se mantenian por separado de los asientos. Si una operacion
se guardaba a medias o se editaba `cuentas.txt`, los reportes quedaban
desincronizados. Ademas, los reversos guardaban fechas largas de
`Date.toString()` y `leerDouble()` aceptaba `NaN`, `Infinity` y montos en cero.

**Impacto:**
- `src/Contabilidad.java`: `recalcularSaldos()` recalcula todos los saldos desde
  los asientos, que pasan a ser la fuente de verdad.
- `aplicarAjustesAutomaticos()` se ejecuta al iniciar, detecta diferencias y crea
  `backup/ajuste_.../` antes de guardar. Si el respaldo falla, revierte el ajuste
  en memoria y no modifica los archivos.
- Nueva opcion 16: `recalcularSaldosManual()`. Crea `backup/recalculo_.../` y
  guarda el ajuste manualmente.
- `generarBalance()` incluye el resultado del ejercicio:
  `Activo = Pasivo + Patrimonio + Resultado`. `balance.txt` guarda el patrimonio
  con el resultado incluido.
- `verEstadoCambiosPatrimonio()` tambien muestra e incluye el resultado.
- `revertirAsiento()` usa `fechaActual()` en formato `dd/MM/yyyy`.
- `normalizarFechasAsientos()` migra las fechas largas existentes a `dd/MM/yyyy`.
- `leerFecha()` valida fechas reales con `LocalDate`, incluidos los anos
  bisiestos.
- `leerDouble()` rechaza `NaN`, `Infinity`, cero y montos negativos.
- Los cargadores toleran un BOM UTF-8 al inicio de los archivos.
- Se guarda primero `asientos.txt` y luego `cuentas.txt`.
- `respaldarDatos()` ahora recibe una etiqueta (`reinicio`, `ajuste`,
  `recalculo`) para nombrar la carpeta de respaldo.
- No se modifico el formato de los archivos de `data/`.

### Cambio 16: Blindaje de datos (Fase A)

**Razon:** `guardarCuentas()`, `guardarAsientos()` y `guardarBalance()` escribian
directamente sobre el archivo destino: un corte a mitad de guardado podia dejar
`asientos.txt` (fuente de verdad) truncado. Ademas, una linea malformada en
`cuentas.txt` o una `PARTIDA` con monto invalido en `asientos.txt` desincronizaba
las listas paralelas y podia tumbar el programa. Tampoco habia respaldo si el
usuario no usaba las opciones 14 o 16.

**Impacto:**
- `src/Contabilidad.java`: nuevo `escribirArchivoAtomico()`: escribe en
  `archivo.tmp` y mueve con `Files.move` (`REPLACE_EXISTING` + `ATOMIC_MOVE`,
  con reintento sin movimiento atomico). Si falla, borra el `.tmp` y el archivo
  original queda intacto. Lo usan `guardarCuentas()`, `guardarAsientos()` y
  `guardarBalance()`.
- Carga blindada: `cargarCuentasTexto()` y `cargarAsientosTexto()` validan los
  campos antes de agregarlos a las listas, de modo que una linea invalida ya no
  las desalinea. Se avisa por linea, por saldo no valido, por total de asiento
  no valido y por codigo de cuenta duplicado (se conserva la primera aparicion).
- Nueva `revisarAsientos()`: avisa asientos con menos de 2 partidas o
  descuadrados (se conservan tal cual, el balance lo refleja) y corrige
  `asientoTotales` desde las partidas si no coincide.
  `aplicarAjustesAutomaticos()` y la opcion 16 aplican la correccion y la
  guardan con su respaldo; si el respaldo falla, revierten saldos, fechas y
  totales en memoria.
- `actualizarSaldos()` avisa una sola vez por codigo de cuenta inexistente
  durante el recalculo.
- Nuevo `respaldarDiario()`: copia los datos a `backup/diario_YYYYMMDD/` una vez
  al dia al iniciar. `podarRespaldosDiarios()` conserva los ultimos 10 y elimina
  los mas antiguos. Los respaldos `ajuste_`, `recalculo_` y `reinicio_` no se
  podan.
- `respaldarDatos()` usa ahora `copiarArchivosDatos()` y borra la carpeta
  parcial si el respaldo falla a mitad.
- No se modifico el formato de los archivos de `data/`.

### Cambio 17: Recarga de datos y orden cronologico (Fase B)

**Razon:** El programa cargaba los archivos una sola vez al iniciar: si se
editaban por fuera, el siguiente guardado borraba esas ediciones sin avisar. Los
reportes "cronologicos" (libro diario, libro mayor y busquedas) mostraban el
orden de insercion de los asientos, no el de fecha. Ademas, los reportes usaban
`cuentaSaldos` de memoria sin verificar que siguiera coincidiendo con los
asientos.

**Impacto:**
- `src/Contabilidad.java`: nuevas marcas `ultimaModCuentas` y
  `ultimaModAsientos` basadas en `lastModified()`. Se actualizan al cargar, al
  recargar y despues de cada guardado exitoso.
- `revisarCambiosExternos()` se ejecuta al inicio de cada vuelta del menu y
  avisa una sola vez si `cuentas.txt` o `asientos.txt` cambiaron fuera del
  programa; sugiere la opcion 17 y advierte que el proximo guardado
  sobrescribira esas ediciones.
- Nueva opcion 17 `recargarDatosDesdeArchivos()`: pide confirmacion `S/N`,
  vuelve a leer ambos archivos, inicializa el catalogo si quedo vacio y aplica
  el ajuste automatico (con respaldo `ajuste_...` si hay diferencias).
- `verificarSaldosAntesDeReporte()` se ejecuta en las opciones 5, 7, 9 y 10:
  recalcula los saldos en memoria antes de mostrar el reporte; si habia
  diferencias, avisa y quedan guardadas al salir.
- Nuevos `fechaOrdenable()` y `ordenarIndicesPorFecha()`: `verLibroDiario()` y
  `verLibroMayor()` se ordenan por fecha. El libro diario muestra el numero de
  asiento original y el libro mayor usa la columna `As.`. `buscarAsientos()`
  tambien ordena por fecha y muestra "Asiento #X".
- `mostrarAsientos()` y `revertirAsiento()` conservan el orden de archivo para
  que la numeracion con la que el usuario elige un asiento no cambie.
- No se modifico el formato de los archivos de `data/`.

### Cambio 18: Seleccion de cuentas sin memorizar codigos

**Razon:** Aunque el registro de asientos ya permitia buscar por nombre, el libro
mayor y la busqueda de asientos por cuenta seguian exigiendo el codigo exacto.
Ademas el registro obligaba a escribir "Debito/Credito" y a saber de antemano
cuantas partidas se iban a cargar, y los detalles de los asientos mostraban solo
codigos.

**Impacto:**
- `src/Contabilidad.java`: `pedirCuenta()` acepta `0` para cancelar y se
  reutiliza en las opciones 12 (libro mayor) y 15.3 (buscar asientos por
  cuenta). Permite busqueda parcial por codigo o nombre, muestra una lista
  numerada cuando hay varias coincidencias, `?` abre el catalogo y reintenta si
  no encuentra nada.
- `registrarAsiento()` carga partidas dinamicamente: despues de la segunda
  pregunta `Agregar otra partida? (S/N)`. Exige minimo 2 partidas, valida la
  cuenta elegida y reemplaza el texto "Debito/Credito" por un menu 1/2. Se
  mantiene la validacion de partida doble.
- Nuevos `nombreCuenta()` y `resumenCuentas()`: las opciones 4 (ver asientos),
  13 (lista de reversion) y 15 (resultados de busqueda) muestran
  `codigo - nombre`.
- `crearCuenta()` pide nombre y tipo con menu numerado, sugiere el siguiente
  codigo libre de la decena en el rango del tipo (Activo 1000, Pasivo 2000,
  Patrimonio 3000, Ingreso 4000, Costo 5000, Gasto 6000) y acepta Enter para
  usarlo. Si el codigo ya existe, vuelve a preguntar.
- Nuevo `TIPOS_CUENTA`: fuente unica de los seis tipos validos; lo usan
  `tipoValido()`, `pedirTipoCuenta()` y `sugerirCodigoCuenta()`.
- No se modifico el formato de los archivos de `data/`.

### Cambio 19: Busquedas sin acentos, montos flexibles y revision del asiento

**Razon:** Las busquedas de cuentas y descripciones dependian de tildes y
mayusculas, el ingreso de montos exigia punto decimal y el asiento se guardaba
sin una ultima revision. Esto provocaba busquedas fallidas y errores de tipeo.

**Impacto:**
- `src/Contabilidad.java`: `normalizarTexto()` ahora usa
  `Normalizer.Form.NFD`, elimina marcas diacriticas, convierte a minusculas con
  `Locale.ROOT`, colapsa espacios y tolera `null`. Se aplica a codigos, nombres
  y descripciones.
- `buscarCuentas()` normaliza codigo y nombre; la opcion 6 reutiliza esta
  funcion. `existeCodigoNormalizado()` evita duplicados que solo difieren en
  mayusculas o tildes al crear cuentas y al cargar `cuentas.txt` (se conserva
  la primera aparicion).
- La opcion 15.2 busca descripciones sin distinguir tildes ni mayusculas.
- `leerDouble()` usa `parsearMonto()`: acepta coma o punto decimal
  (`1800,50`, `1800.50`, `1.800,50`, `1,800.50`). Rechaza montos ambiguos de
  tres decimales con un solo punto (`1.800`), `NaN`, infinito, cero y negativos.
- `leerFecha()` acepta Enter para usar la fecha actual. `leerFechaOpcional()`
  acepta Enter para "sin limite" y `convertirFecha()` valida fechas reales.
- `registrarAsiento()` muestra fecha, descripcion, partidas, totales de debito
  y credito, y pide confirmacion `S/N` antes de guardar. Si se rechaza, no se
  modifica ninguna lista ni archivo.

### Cambio 20: Reportes por periodo configurable (opcion 18)

**Razon:** Los reportes siempre mostraban todo el historico. No habia forma de
consultar un mes o un rango de fechas sin editar archivos.

**Impacto:**
- `src/Contabilidad.java`: nuevas variables `periodoDesde` y `periodoHasta`
  (vacias = todo el historico) y nueva opcion 18 para configurarlas.
- `calcularSaldosRango()`, `calcularSaldosHasta()`, `aplicarPartidaSaldo()` y
  `fechaEnRango()` calculan saldos directamente desde los asientos para el
  periodo elegido, sin modificar `cuentaSaldos` ni `data/`.
- El balance general usa el saldo acumulado hasta la fecha de corte. Los estados
  de resultados, cambios en el patrimonio y flujo de efectivo usan solo los
  movimientos del rango. El balance de comprobacion, libro diario y libro mayor
  filtran por fecha; el libro mayor muestra el saldo inicial al `desde`.
- La opcion 15 agrega busqueda `4. Por rango de fechas`.
- `balance.txt` conserva cuatro campos, pero el primero ahora es la fecha de
  corte del balance (o la fecha actual si es todo el historico).

### Cambio 21: Restauracion de respaldos (opcion 19)

**Razon:** Los respaldos se creaban pero no habia forma de restaurarlos desde el
programa; recuperar un estado anterior requeria copiar archivos manualmente.

**Impacto:**
- `src/Contabilidad.java`: nueva opcion 19 `restaurarRespaldo()`.
- Lista `diario_`, `ajuste_`, `recalculo_`, `reinicio_` y
  `previo_restauracion_`, ordenados por fecha de modificacion descendente.
- Antes de restaurar crea `backup/previo_restauracion_.../` con los datos
  actuales. Si no puede crear ese respaldo, cancela la operacion.
- Verifica que los archivos del respaldo sean legibles y copia cada uno byte por
  byte con `copiarArchivoAtomico()` (temporal + movimiento atomico), sin
  decodificarlo como texto. Si falla, intenta recuperar el estado anterior desde
  el respaldo de seguridad.
- Si el respaldo no incluye `balance.txt`, elimina el balance actual para no
  dejar un reporte viejo. Luego recarga cuentas y asientos y aplica el ajuste
  automatico con su respaldo correspondiente.
- La confirmacion exige escribir `RESTAURAR`.

### Cambio 22: Manual de usuario y pruebas de regresion

**Razon:** El proyecto no tenia documentacion para el usuario ni una forma
repetible de comprobar las busquedas, los montos, los periodos y la restauracion.

**Impacto:**
- `README.md`: manual de instalacion, menu, busquedas, formato de montos y
  fechas, periodos, respaldos, restauracion y ubicacion de los datos.
- `tests/pruebas_fase_d.ps1`: crea un directorio temporal, compila el proyecto,
  ejecuta escenarios de busqueda, montos, revision de asientos, reportes por
  periodo y restauracion, y verifica los resultados. No modifica `data/` real.
- `AGENTS.md`: se documentan los cambios 19 a 22.

### Cambio 23: Cierre de Fase E, limpieza y entrega

**Razon:** El proyecto ya tenia la Fase D completa y probada, pero quedaban
artefactos de pruebas antiguos, la libreria Gson sin uso, ejemplos JSON
obsoletos y respaldos de migracion que no forman parte del sistema actual.
Tambien faltaba ignorar los respaldos de ejecucion para evitar que se mezclen
con el codigo.

**Impacto:**
- Se eliminaron `TEST_REPORT.md`, `test_output.txt` y `test_broken_output.txt`.
- Se elimino `lib/gson.jar` y la carpeta `lib/` quedo vacia.
- Se eliminaron los ejemplos JSON antiguos de `examples/` y se conservaron los
  ejemplos en texto plano.
- Se elimino `examples/asientos.txt.ejemplo` por estar vacio y no aportar un
  caso de ejemplo util.
- Se elimino `backup/json/` con los respaldos de la migracion anterior.
- Se conservo `backup/reinicio_20261019_195251_530/` como respaldo historico de
  los 7 asientos previos al reinicio intencional.
- `.gitignore`: se agregaron `backup/` y `*.tmp` para no versionar respaldos ni
  temporales de escritura atomica.
- `opencode.json` queda fuera del commit por ser configuracion del entorno.
- No se modifico el formato de los archivos de `data/`.

### Cambio 24: Adaptacion a la consigna del curso

**Razon:** El docente autorizo Java y pidio un menu de 11 opciones, cuentas T en
dos columnas, respaldo manual y la eliminacion de funciones que no forman parte
de la consigna. El menu anterior tenia 19 opciones y mezclaba reportes
adicionales, recarga de datos y configuracion de periodos.

**Impacto:**
- `src/Contabilidad.java`: menu principal reducido a 11 opciones:
  1 Administrar cuentas, 2 Registrar operacion, 3 Consultar cuentas T,
  4 Libro diario, 5 Libro mayor, 6 Balanza de comprobacion, 7 Estado de
  resultados, 8 Balance general, 9 Consultar movimientos, 10 Respaldar
  informacion, 11 Salir. La opcion `0` tambien sale.
- Nuevos submenus `menuAdministrarCuentas`, `menuConsultarCuentasT`,
  `menuConsultarMovimientos` y `menuRespaldarInformacion` para conservar las
  funciones utiles sin inflar el menu principal.
- Cuentas T: `verCuentaTIndividual`, `verCuentasTConMovimientos`,
  `verTodasCuentasT`, `cuentaTieneMovimientos`, `mostrarCuentaT` y `recortar`.
  Cada cuenta se muestra en dos columnas (Debe/Haber) con fecha, concepto,
  monto, totales y saldo deudor/acreedor/cero.
- Respaldo manual: `respaldarInformacion()` crea `backup/manual_.../` y
  `restaurarRespaldo()` acepta el prefijo `manual_`.
- Se eliminaron `verEstadoFlujoEfectivo`, `verEstadoCambiosPatrimonio`,
  `recargarDatosDesdeArchivos`, `recalcularSaldosManual`,
  `configurarPeriodoReportes`, `descripcionPeriodo`, `fechaEnPeriodo`,
  `saldoCuentaAntes`, `totalPorTipo(String)` y `calcularResultadoEjercicio`.
- Se eliminaron `periodoDesde` y `periodoHasta`: todos los reportes usan el
  historico completo. `balance.txt` guarda como primer campo la fecha de
  generacion (`fechaActual()`), no una fecha de corte configurable.
- Se conservan `fechaEnRango()` y `leerFechaOpcional()` para la busqueda de
  asientos por rango (opcion 9.2.4). Alli Enter significa "sin fecha inicial" o
  "sin fecha final" segun el campo.
- Se conservan los respaldos automaticos (`diario_`, `ajuste_`), el reinicio
  (opcion 1.4), la reversion de asientos (opcion 9.3) y la restauracion
  (opcion 10.2). La restauracion sigue listando `recalculo_` por
  compatibilidad con respaldos antiguos, aunque ya no se genera.
- `tests/pruebas_fase_d.ps1`: se remapearon las entradas a los nuevos menus y
  submenus; se agrego la prueba del respaldo manual y se ajustaron las
  expectativas de los reportes historicos y de las cuentas T.
- `README.md`: se reescribio el manual con el menu nuevo, los submenus, las
  cuentas T, el respaldo manual y la eliminacion del periodo de reportes.
- No se modifico el formato de los archivos de `data/`.

### Cambio 25: Claridad de mensajes y proteccion de la captura

**Razon:** Varios mensajes de consola eran ambiguos: el prompt de cuenta no
explicaba que se podia buscar por codigo o nombre ni como cancelar, el registro
pedia "Tipo de partida" sin relacion con el Debe/Haber, los menus no mostraban
ni validaban el rango de opciones, un asiento descuadrado se perdia completo y
la reversion se ejecutaba sin confirmacion. Ademas, un `|` en un nombre,
descripcion o codigo rompia el formato de `data/`, y el reporte se llamaba
"Balance de comprobacion" en lugar de "Balanza de comprobacion".

**Impacto:**
- `src/Contabilidad.java`: todos los menus usan `leerInt(mensaje, minimo, maximo)`
  y muestran el rango o la opcion `0` para volver. El menu principal indica
  "11. Salir (o 0)".
- `pedirCuenta(contexto, accionCancelar)` reemplaza al prompt ambiguo: indica que
  se acepta el codigo o parte del nombre, `?` para ver el catalogo y `0` para
  cancelar. Se usa en el registro de asientos, el libro mayor, la cuenta T y la
  busqueda por cuenta. En la lista de coincidencias `0` cancela y `V` vuelve a
  buscar, de modo que `0` ya no significa "buscar de nuevo".
- `registrarAsiento()` guia la carga (fecha, descripcion, partidas), muestra el
  acumulado del Debe y del Haber tras cada partida y, si el asiento no cuadra,
  informa totales y diferencia y ofrece agregar otra partida o descartarlo en
  lugar de perder todo. El resumen final usa las etiquetas Debe/Haber y pide
  confirmacion antes de guardar.
- `pedirTipoPartida()` ahora se presenta como "Lado de la partida" con las
  opciones "1. Debe (debito)" y "2. Haber (credito)".
- `crearCuenta()` anuncia los tres datos que pedira, muestra ejemplos de nombre y
  codigo y explica el codigo sugerido con Enter. Rechaza los codigos `0`, `?` y
  los que contienen `|`.
- Nuevo `leerTextoPersistible(mensaje, campo)`: rechaza `|` en nombres de cuenta
  y descripciones de asientos para no corromper el formato delimitado.
- Nuevo `etiquetaLado(tipo)`: las columnas "Lado" de asientos, resumen, libro
  mayor y busquedas muestran Debe/Haber en lugar de Debito/Credito. El libro
  mayor aclara que "As." es el numero de asiento.
- `mostrarAsientos()` avisa cuando no hay asientos, agrega encabezado de columnas
  y muestra los totales con dos decimales. La busqueda de asientos agrega la
  opcion `0. Volver`, encabezados y descripciones de busqueda mas claras.
- `restaurarRespaldo()` advierte que reemplazara los datos actuales, que creara
  un respaldo de seguridad y que eliminara `balance.txt` si el respaldo no lo
  incluye; explica los prefijos de respaldo y valida la seleccion.
  `reiniciarDatos()` enumera los asientos, cuentas personalizadas y balance que
  borra, y aclara que restaura las 75 cuentas estandar.
- `revertirAsiento()` acepta `0` para cancelar, informa la fecha de la reversa y
  pide confirmacion `S/N` antes de crearla.
- La opcion 6 se titula "Balanza de comprobacion" y el mensaje final es
  "LA BALANZA DE COMPROBACION ESTA CUADRADA.".
- `leerFechaOpcional(mensaje, textoSinFecha)` muestra "sin fecha inicial" o
  "sin fecha final" en la busqueda por rango, en lugar de "sin limite".
- `README.md`: se actualizaron menus, busquedas, montos y fechas, registro de
  asientos, respaldos y reinicio para reflejar los textos y validaciones nuevas.
- `tests/pruebas_fase_d.ps1`: se ajusto la asercion de la balanza y se agregaron
  pruebas de codigo `0` reservado, nombre y descripcion con `|`, seleccion con
  `V` y correccion de un asiento descuadrado con una tercera partida.
- No se modifico el formato de los archivos de `data/` ni la logica contable.

### Cambio 26: Simplificacion alineada con la consigna del curso

**Razon:** La consigna pide un sistema de consola con 11 opciones y los reportes
contables basicos. El programa habia acumulado funciones de produccion
(restauracion, reversion, reinicio, respaldos automaticos, deteccion de cambios
externos, auto-ajuste de saldos y busquedas avanzadas) que no forman parte de la
rubrica y hacian mas dificil explicar y mantener el codigo.

**Impacto:**
- `src/Contabilidad.java`: el archivo paso de 2.275 a 1.429 lineas y el menu
  principal conserva las 11 opciones (1 Administrar cuentas, 2 Registrar
  operacion, 3 Consultar cuentas T, 4 Libro diario, 5 Libro mayor, 6 Balanza de
  comprobacion, 7 Estado de resultados, 8 Balance general, 9 Consultar
  movimientos, 10 Respaldar informacion, 11 Salir; `0` tambien sale).
- Submenus reducidos: el 1 tiene 3 opciones (ver catalogo, crear cuenta, buscar
  cuenta), el 3 tiene 3 (una cuenta, cuentas con movimientos, catalogo
  completo) y el 9 tiene 2 (ver asientos, buscar asientos). La opcion 10 crea el
  respaldo directamente, sin submenu.
- Se eliminaron `menuRespaldarInformacion`, la restauracion (`restaurarRespaldo`,
  `restaurarArchivosDesdeCarpeta`, `copiarArchivoAtomico`), la reversion
  (`revertirAsiento`), el reinicio (`reiniciarDatos`), los respaldos automaticos
  (`respaldarDatos`, `respaldarDiario`, `podarRespaldosDiarios`,
  `copiarArchivosDatos`, `borrarCarpeta`), la deteccion de cambios externos
  (`marcaArchivo`, `actualizarMarcasArchivos`, `revisarCambiosExternos`), el
  auto-ajuste (`aplicarAjustesAutomaticos`, `contarSaldosDiferentes`,
  `revisarAsientos`, `verificarSaldosAntesDeReporte`, `recargarMemoriaDesdeArchivos`,
  `saldosEnCero`) y la migracion de fechas (`normalizarFecha`,
  `normalizarFechasAsientos`).
- La busqueda de asientos (9.2) quedo limitada a fecha exacta o texto en la
  descripcion. Se eliminaron la busqueda por cuenta, por rango de fechas y los
  helpers `fechaEnRango`, `leerFechaOpcional`, `aplicarPartidaSaldo`,
  `calcularSaldosRango` y `calcularSaldosHasta`.
- Se compacto el codigo con helpers como `mostrarAsiento`, `mostrarPartidas`,
  `mostrarSeccion`, `efectoSaldo`, `ordenarMovimientos`, `nombreCuenta` y
  `etiquetaLado`. `resumenCuentas` fue reemplazado por `nombreCuenta`.
- Se conservaron la busqueda de cuentas por codigo o nombre, la validacion de
  partida doble, el rechazo de `|` en campos persistidos, la escritura atomica,
  el respaldo manual en `backup/manual_YYYYMMDD_HHmmss_SSS/`, los seis reportes,
  las cuentas T y el formato de `data/`.
- `tests/pruebas_fase_d.ps1`: se reescribio para el flujo nuevo (validaciones,
  asiento descuadrado y corregido, reportes, busqueda y respaldo manual). Todas
  las pruebas pasan.
- `README.md`: se reescribio el manual con el menu nuevo, los submenus, las
  cuentas T, el respaldo manual y la aclaracion de que la restauracion se hace
  copiando archivos a mano.
- No se modifico el formato de los archivos de `data/` ni la logica contable.

### Cambio 27: Simplificacion moderada del codigo

**Razon:** El usuario pidio acortar los metodos mas extensos y reducir variables
locales sin quitar funciones ni la escritura atomica. La idea fue mantener el
mismo comportamiento contable y simplificar la implementacion.

**Impacto:**
- `src/Contabilidad.java`: el archivo paso de 1.429 a 1.346 lineas.
- Nuevo `cargarMovimientosCuenta(codigo, asientos, partidas)`: concentra la
  busqueda y el ordenamiento de los movimientos de una cuenta. Lo usan
  `verLibroMayor()` y `mostrarCuentaT()`.
- `mostrarCuentaT()`: las seis listas locales (`fechasDebe`, `conceptosDebe`,
  `montosDebe` y las tres de Haber) se reemplazaron por dos listas de filas ya
  formateadas (`filasDebe`, `filasHaber`). Se conserva la salida en dos
  columnas con totales y saldo.
- `verBalanceComprobacion()`: ahora acumula debitos y creditos en una sola
  pasada usando dos vectores primitivos `double[]` y `indiceCuenta()`. La salida
  y la verificacion de cuadre son las mismas.
- `cargarAsientosTexto()`: se compacto el analizador. Las listas de partidas se
  agregan a las listas globales en cuanto se lee un encabezado `ASIENTO` valido,
  sin bloque de volcado al final. Se conservan la validacion de campos, el BOM,
  los tipos `Debito/Credito`, los montos finitos y positivos, y los avisos por
  linea. Los errores de campos faltantes o numeros invalidos se informan con un
  mensaje general por linea.
- `registrarAsiento()`: cuando el asiento no cuadra se pregunta directamente si
  se agrega otra partida para cuadrarlo, sin la pregunta intermedia "Agregar
  otra partida?". Si el asiento esta cuadrado, se mantiene la pregunta para
  agregar mas partidas.
- `crearCuenta()`: se eliminaron la validacion redundante de `tipoValido()` y la
  comprobacion de codigo vacio; se unifico el rechazo de `?` y `|`.
- `cargarCuentasTexto()`: se compacto la lectura y validacion del saldo.
- `buscarAsientos()`: la busqueda por fecha y por descripcion comparte un solo
  recorrido de asientos.
- `escribirArchivoAtomico()` se conserva sin cambios: escribe en un temporal y
  mueve el archivo con `Files.move`.
- `tests/pruebas_fase_d.ps1`: se ajusto la secuencia de entradas del asiento
  descuadrado a la nueva pregunta unica. Todas las pruebas pasan.
- No se modifico el formato de `data/` ni la logica contable.

### Cambio 28: Libro diario detallado, validacion de carga y saldos contrarios

**Razon:** El libro diario solo mostraba fecha, descripcion y total, sin las
partidas individuales. Ademas, `asientos.txt` se cargaba sin validar fechas ni
consistencia contable, y el sistema permitia registrar un credito mayor que el
saldo de una cuenta de activo sin avisar, dejando, por ejemplo, Inventarios con
saldo acreedor.

**Impacto:**
- `src/Contabilidad.java`: `verLibroDiario()` muestra cada asiento con fecha,
  descripcion, cuentas y columnas separadas de Debe y Haber, mas totales por
  asiento y total general. Nuevo `mostrarPartidasDebeHaber()`.
- `cargarAsientosTexto()` valida cada asiento como bloque: fecha real, 4 campos,
  descripcion, al menos 2 partidas, cuentas existentes, tipo valido, montos
  finitos y positivos, y Debe = Haber con tolerancia de 0.01. El total del
  encabezado se recalcula desde las partidas. Un asiento invalido se omite
  completo y se informa por linea.
- `parsearFecha()` centraliza la validacion estricta `dd/MM/yyyy` y
  `FORMATO_FECHA_ESTRICTA` evita crear formateadores en cada llamada.
- `cargarDatos()` carga o inicializa las cuentas antes de los asientos.
- `cargarCuentasTexto()` exige codigo y nombre no vacios; usa `split("\\|", -1)`
  y recorta los campos.
- `respaldarArchivoInvalido()` copia `cuentas.txt` o `asientos.txt` a
  `backup/cuentas_invalidas_....txt` o `backup/asientos_invalidos_....txt` antes
  de reescribirlos. Si falla la copia de asientos, `bloquearGuardadoAsientos`
  impide guardar y registrar asientos hasta corregir el archivo.
- `registrarAsiento()` simula los saldos del asiento en `saldosSimulados`. Si una
  partida deja la cuenta en el lado contrario, muestra el saldo resultante y
  pide confirmacion `S/N`; si se rechaza, descarta la partida y vuelve a pedirla.
- `advertirSaldosContrarios()` avisa al iniciar si alguna cuenta quedo negativa,
  sin omitir asientos, porque puede ser una cuenta de naturaleza contraria.
- `mostrarCuentaT()` y `mostrarSeccion()` marcan los saldos en el lado contrario;
  `mostrarNotaSaldosContrarios()` agrega la nota en Estado de Resultados y
  Balance General.
- `salir()` no guarda si `bloquearGuardadoAsientos` esta activo.
- `tests/pruebas_fase_d.ps1`: agrega pruebas de libro diario detallado, asientos
  invalidos (fecha, descuadre, cuenta inexistente, una partida), respaldo del
  archivo rechazado y advertencia/confirmacion por saldo contrario.
- `README.md`: documenta el libro diario detallado, la validacion de archivos,
  los respaldos de archivos invalidos y los saldos en el lado contrario.
- No se modifico el formato de `data/`.

### Cambio 29: Refactor compacto, libro diario clasico y auditoria

**Razon:** `src/Contabilidad.java` habia crecido a 1.723 lineas y el libro diario
mostraba un formato propio. Se pidio conservar todas las funciones y la logica
contable, reducir el codigo a cerca de 1.200 lineas y adoptar el formato clasico
`Cta | Detalle | Parcial | Debe | Haber`. Ademas se revisaron casos limite de
montos, tolerancia, carga de archivos y cuentas contrarias.

**Impacto:**
- `src/Contabilidad.java`: paso de 1.723 a 1.219 lineas sin eliminar opciones ni
  reportes. Se compactaron metodos largos, declaraciones y salidas; se agrego
  `TIPOS` (`Arrays.asList(TIPOS_CUENTA)`) para centralizar la validacion y la
  sugerencia de codigos.
- `verLibroDiario()` usa el formato clasico: un solo encabezado, asiento
  numerado, partidas del Debe primero, partidas del Haber con prefijo `A `,
  columna `Parcial` vacia, fecha y descripcion al pie y un unico `TOTAL GENERAL`.
- `parsearMonto()` valida estrictamente los formatos `1800`, `1800.50`,
  `1800,50`, `1.800,50` y `1,800.50`; rechaza separadores ambiguos, mas de dos
  decimales, `NaN`, infinito, cero y negativos.
- La tolerancia de partida doble es uniforme: 0.01 con `cuadra()`, tanto al
  registrar como al cargar `asientos.txt`.
- La carga de asientos invalida el bloque completo ante una linea desconocida;
  no se cargan partidas parciales.
- Si falla la lectura de `asientos.txt`, `bloquearGuardadoAsientos` impide
  sobrescribir el archivo al salir.
- Los fallos de escritura se informan; si no se puede guardar un asiento nuevo,
  se revierte de las listas y se recalculan los saldos.
- `verBalanceComprobacion()` muestra columnas `Debitos`, `Creditos`, `Deudor` y
  `Acreedor`, y conserva la verificacion de cuadre.
- `esCuentaContraria()` exime de la advertencia de lado contrario a `1790`,
  `1890`, `3120` y `3500`.
- `tests/pruebas_fase_d.ps1`: se agregaron pruebas de montos malformados y
  formatos validos, tolerancia exacta de 0.01 y rechazo de 0.02, asiento solo
  al Debe, linea desconocida dentro de un asiento, cuenta `1790` sin aviso
  falso, columnas Deudor/Acreedor de la balanza y fallo de lectura sin
  sobrescritura.
- `README.md`: se documento el libro diario clasico, las columnas de la balanza,
  la tolerancia de 0.01, la invalidacion por lineas desconocidas y el bloqueo
  por fallo de lectura.
- No se modifico el formato de `data/` ni la logica contable.

### Cambio 30: Datos demo coherentes, tolerancia estricta y pruebas numericas

**Razon:** El asiento de ejemplo decia "compra de papeleria" pero debitaba Caja
y acreditaba Capital, lo que en realidad es un aporte de capital. Ademas, la
tolerancia de 0.01 aceptaba un descuadre de un centavo, el balance se etiquetaba
"al dia de hoy" aunque incluye todo el historico, y las pruebas verificaban
textos pero no los valores contables.

**Impacto:**
- `data/asientos.txt`: se reemplazo el asiento unico por cuatro asientos
  coherentes: aporte de capital, compra de papeleria, venta de servicios y pago
  de renta. Los saldos se recalculan desde los asientos.
- `src/Contabilidad.java`: `TOLERANCIA` paso de `0.01` a `0.005`; con ello se
  rechaza cualquier diferencia real de un centavo y se conserva el margen para
  el error de `double`.
- `src/Contabilidad.java`: el balance general se etiqueta como
  `Historico completo` y muestra la fecha de generacion por separado.
- `tests/pruebas_fase_d.ps1`: se agrego `Assert-Match` y comprobaciones
  numericas exactas de ingresos, gastos, utilidad bruta, utilidad neta, activo,
  pasivo, patrimonio, resultado, diferencia, totales de la balanza, saldo de
  Caja y campos de `balance.txt`. Se fijo el locale de las pruebas en espanol
  para que el separador decimal sea estable. El caso de tolerancia de 0.01
  ahora se rechaza y el conteo esperado es `1 validos, 4 omitidos`.
- `README.md`: se actualizaron la tolerancia, el significado del tercer campo de
  `balance.txt`, los criterios contables y la autorizacion de Java.
- `AGENTS.md`: se actualizaron la tolerancia y la formula del balance, y se
  documento este cambio.
- No se modifico el formato de `data/` ni las formulas contables.

### Cambio 31: Periodos contables, cierre del resultado y reportes filtrados

**Razon:** El sistema solo trabajaba con el historico completo. No habia forma
de definir el periodo en curso, impedir asientos con fechas de otro ejercicio,
cerrar las cuentas de resultado ni consultar los reportes de un periodo
especifico. Se pidio conservar las 11 opciones de la consigna y agregar el
control de periodos como una opcion independiente.

**Impacto:**
- `src/Contabilidad.java`: se agregaron `ARCHIVO_PERIODO`
  (`data/periodo.txt`), `FECHA_MIN`, `FECHA_MAX`,
  `CUENTA_RESULTADOS_ANTERIORES` (`3200`), las variables `periodoInicio`,
  `periodoFin` y `verHistorico`, y la lista paralela `asientoTipos`.
- Menu principal: se agrego la opcion 12 `Gestionar periodos contables`; las
  opciones 1 a 11 se conservan. El submenu 12 tiene ver periodo, configurar,
  cerrar y alternar la vista periodo/historico.
- Periodo: `cargarPeriodo()`, `periodoPredeterminado()`, `guardarPeriodo()`,
  `configurarPeriodo()`, `verPeriodoActual()` y `avisarAsientosFuera()`. El
  archivo `data/periodo.txt` guarda `inicio|fin` con fechas inclusivas. Si no
  existe, se usa el ano actual ampliado para cubrir la fecha mas antigua y la
  mas reciente de los asientos. Si es invalido, se avisa y se reemplaza por el
  periodo predeterminado.
- Cierre: `cerrarPeriodo()` calcula los saldos acumulados de Ingreso, Costo y
  Gasto hasta el fin del periodo, crea un asiento de cierre con esa fecha,
  deja las cuentas nominales en cero y traslada el resultado a
  `3200 Resultados de ejercicios anteriores`. Si hay utilidad, acredita `3200`;
  si hay perdida, la debita. Pide confirmacion `S/N` y guarda el asiento con la
  marca `|CIERRE`.
- `periodoContieneCierre()` deduce el estado del periodo a partir de los
  asientos de cierre. `registrarAsiento()` bloquea el registro si el periodo
  actual esta cerrado y rechaza fechas fuera del periodo.
- `avanzarPeriodo()` avanza automaticamente despues del cierre: ano completo al
  ano siguiente, mes completo al mes siguiente y rango personalizado a otro de
  igual cantidad de dias. El nuevo periodo se guarda de inmediato.
- Reportes: `movimientosRango()`, `saldosRango()`, `saldosVista()`,
  `resultadoDeSaldos()`, `hayNominalesConSaldo()` y `saldoDebitoAntes()` calculan
  los saldos desde los asientos segun la vista activa. Libro diario, libro
  mayor, cuentas T, balanza, estado de resultados, balance general, catalogo,
  busquedas y consulta de movimientos respetan el periodo o el historico.
- El estado de resultados excluye los asientos de cierre para no duplicar el
  resultado. El balance general los incluye: en un periodo cerrado las cuentas
  nominales quedan en cero y el resultado aparece en `3200`.
- `data/asientos.txt`: los asientos normales conservan cuatro campos
  (`ASIENTO|fecha|descripcion|total`); los de cierre agregan un quinto campo
  `|CIERRE`. `cargarAsientosTexto()` acepta `NORMAL` y `CIERRE` y valida el
  bloque completo.
- `data/balance.txt`: en modo periodo el primer campo es la fecha de fin del
  periodo; en modo historico es la fecha de generacion. El cuarto campo sigue
  siendo patrimonio mas resultado.
- `3200` se renombro de `Resultados del ejercicio` a
  `Resultados de ejercicios anteriores` en `CATALOGO_ESTANDAR` y en
  `data/cuentas.txt`. `esCuentaContraria()` ahora tambien exime a `3200` del
  aviso de saldo en el lado contrario.
- `respaldarInformacion()` copia tambien `periodo.txt` cuando existe.
- `src/Contabilidad.java` se compacto de aproximadamente 1.516 a 1.400 lineas
  sin quitar funciones ni reportes.
- `mostrarCuentaT()` convierte el saldo de convencion deudora a la naturaleza de
  la cuenta antes de etiquetarlo, de modo que una cuenta de Patrimonio o Pasivo
  con saldo acreedor se muestra como `Acreedor` y no genera un aviso falso.
- `tests/pruebas_fase_d.ps1`: se agrego una prueba de cuenta T de Patrimonio con
  saldo acreedor para verificar la etiqueta y la ausencia del aviso falso.
- `data/periodo.txt`: se creo con `01/01/2026|31/12/2026` para los datos demo
  de noviembre de 2026.
- `tests/pruebas_fase_d.ps1`: se agregaron escenarios de periodo configurado,
  reportes por periodo, cierre con traslado a `3200`, avance automatico,
  bloqueo de asientos en periodo cerrado, rechazo de fechas fuera del periodo,
  vista historica, marca `|CIERRE`, saldo de `3200`, `periodo.txt` y respaldo
  manual con `periodo.txt`.
- `README.md`: se reescribio el manual con la opcion 12, el cierre, la vista
  periodo/historico, las restricciones de registro, el formato de
  `asientos.txt` y `periodo.txt`, los respaldos y las pruebas nuevas.
- No se modifico el formato de los asientos normales ni la logica de partida
  doble.

### Cambio 32: Estabilidad de entrada y recuperacion de un catalogo invalido

**Razon:** Al revisar el flujo completo se detectaron dos problemas graves. El
primero: al agregar una guarda `scanner.hasNext()` en las lecturas, se devolvia
`null` sin proteger a los llamadores, y el programa terminaba con
`NullPointerException` en tres rutas (registro de asiento, consulta de cuenta T
y configuracion de periodo); `leerDouble()` ademas entraba en un bucle
infinito. El segundo: si `cuentas.txt` existia pero no tenia ninguna cuenta
valida, el programa se quedaba bloqueado sin salida posible desde el menu y,
peor aun, al salir reescribia `asientos.txt` sin los asientos originales
porque sus cuentas no existian todavia.

**Impacto:**
- `src/Contabilidad.java`: nueva bandera global `entradaCerrada`. `leerEntrada()`,
  `leerInt()`, `leerDouble()`, `leerFecha()`, `leerTextoOpcional()`,
  `leerTextoPersistible()` y `confirmar()` la activan cuando se agota la
  entrada y devuelven un valor seguro. Los llamadores que antes fallaron
  (`registrarAsiento()`, `configurarPeriodo()`, `pedirCuenta()`, `buscarCuenta()`,
  `buscarAsientos()`, `crearCuenta()`, `pedirPartida()`) validan `null` y
  cancelan la operacion. El menu principal y los cuatro submenus cierran el
  programa con `salir()` cuando la entrada se agota.
- `src/Contabilidad.java`: nueva opcion **1.4 Reconstruir catalogo estandar**,
  con los metodos `reconstruirCatalogo()`, `codigosCatalogoEstandar()` y
  `codigosUsadosNoEstandar()`. Reconstruye las 75 cuentas, guarda una copia del
  archivo anterior en `backup/cuentas_antes_reconstruir_*.txt` y recarga
  `asientos.txt`. La reconstruccion se rechaza si algun asiento usa una cuenta
  ajena al catalogo estandar.
- `src/Contabilidad.java`: nueva bandera `bloqueoPorCatalogo` y el contador
  `asientosEnArchivo`. Mientras el catalogo este vacio, `data/asientos.txt` no
  se sobrescribe, de modo que ningun asiento original se pierde. Los mensajes
  de `crearCuenta()`, `registrarAsiento()`, `guardarAsientos()` y `salir()`
  apuntan ahora a la opcion 1.4.
- `bin/`: se eliminaron las clases obsoletas de la version POO anterior
  (`Data.class`, `MotorContable.class`, `Persistencia.class`, `Principal.class`)
  y se recompilo `Contabilidad.class`.
- `tests/pruebas_fase_d.ps1`: se agregaron seis escenarios de fin de entrada
  (registro, cuenta T, periodo, creacion de cuenta, busqueda y respaldo) que
  verifican que no hay excepcion y que el programa cierra con `Hasta luego.`,
  mas tres escenarios de recuperacion del catalogo (reconstruccion y uso
  posterior, preservacion de `asientos.txt` y rechazo por cuenta huerfana).
- `README.md`: se documento la opcion 1.4, la proteccion de `asientos.txt`
  durante la recuperacion, el comportamiento ante el fin de la entrada y la
  necesidad de limpiar `bin/` al recompilar.
- `.gitignore`: se permitio versionar `AGENTS.md` para no perder el historial de
  cambios del proyecto.
- No se modifico el formato de los archivos de `data/` ni la logica contable.

### Cambio 33: Bloqueo seguro en la opcion 1.4 y reglas de modularizacion

**Razon:** La opcion 1.4 podia desbloquear el guardado de asientos despues de
un fallo de recarga o de respaldo, con riesgo de sobrescribir `asientos.txt`
con datos parciales. Ademas, el usuario autorizo crear clases y archivos
adicionales unicamente para organizar el codigo, sin cambiar el modelo de
listas paralelas ni introducir POO de dominio.

**Impacto:**
- `src/Contabilidad.java`: `reconstruirCatalogo()` desbloquea antes de
  recargar, vuelve a bloquear asientos y cuentas si la recarga falla, conserva
  el bloqueo si falla el respaldo y avisa cuando no se guardara
  `asientos.txt`.
- `cargarAsientosTexto()`: el mensaje de fallo de respaldo ya no sugiere la
  opcion 1.4; indica corregir `backup/` y reiniciar.
- `registrarAsiento()` y `crearCuenta()`: mensajes genericos cuando el catalogo
  o el guardado estan bloqueados, sin afirmar que el catalogo esta vacio.
- `AGENTS.md`: se permite crear clases y archivos `.java` adicionales como
  modulos `static` de organizacion. Se mantienen prohibidos los objetos de
  dominio, la herencia, el polimorfismo y las interfaces.
- `README.md`: se documenta el bloqueo de guardado cuando falla la recarga o el
  respaldo en la opcion 1.4.
- `tests/pruebas_fase_d.ps1`: se agrega una prueba de fallo de respaldo de
  asientos invalidos que verifica que `asientos.txt` no se sobrescribe.
- No se modifica el formato de `data/` ni la logica contable.
- La separacion fisica en modulos se hara en una fase posterior; este cambio
  solo deja las reglas y el arreglo de 1.4.

### Cambio 34: Separacion fisica en nueve modulos `static`

**Razon:** `src/Contabilidad.java` concentraba toda la aplicacion en un solo
archivo de aproximadamente 1.374 lineas. Se pidio separarlo en nueve modulos de
organizacion sin cambiar el comportamiento, los mensajes, las 12 opciones ni el
formato de `data/`, manteniendo el modelo de listas paralelas y sin introducir
objetos de dominio.

**Impacto:**
- `src/Contabilidad.java`: quedo con 120 lineas. Conserva `main`, los menus
  principales y de administracion, la opcion 12 (rango de reportes), la
  configuracion del rango y `salir()`. Las llamadas a los demas modulos se
  califican con `Modulo.metodo()`.
- `src/Estado.java`: centraliza las listas paralelas de cuentas, asientos y
  partidas, las tolerancias contables y el rango de reportes
  (`rangoDesde`, `rangoHasta`, `vistaInicio()`, `vistaFin()` y
  `descripcionRango()`).
- `src/Consola.java`: centraliza el `Scanner`, la bandera `entradaCerrada`, los
  menus, las lecturas validadas (`leerString`, `leerInt`, `leerDouble`,
  `leerFecha`, `leerFechaRango`, `leerTextoPersistible`, `leerTextoOpcional`,
  `confirmar`, `leerEntrada`), `linea()` y `parsearMontoSimple()`.
- `src/Fechas.java`: concentra `FORMATO_FECHA`, `FECHA_MIN`, `FECHA_MAX`,
  `fechaActual()`, `parsearFecha()`, `fechaOrdenable()` y `fechaEnRango()`.
- `src/Formato.java`: concentra el formateo y parseo de montos, la limpieza de
  campos, `normalizarTexto()`, `recortar()` y `etiquetaLado()`.
- `src/Persistencia.java`: concentra las rutas `ARCHIVO_*`, las banderas
  `bloquearGuardadoAsientos` y `bloquearGuardadoCuentas`, la carga inicial,
  `guardarCuentas()`, `guardarAsientos()`, `guardarBalance()`, la escritura
  atomica, los respaldos de archivos invalidos y el respaldo manual.
- `src/Cuentas.java`: concentra el catalogo estandar de 75 cuentas, los tipos,
  la creacion, la busqueda, la validacion de codigos y la reconstruccion del
  catalogo.
- `src/Asientos.java`: concentra el registro de asientos, la partida doble, el
  calculo de saldos, los movimientos por cuenta, las busquedas y las utilidades
  `cuadra()`, `tipoDeudor()`, `esCuentaContraria()`, `efectoSaldo()` y
  `totalLado()`.
- `src/Reportes.java`: concentra la balanza de comprobacion, el libro diario, el
  libro mayor, las cuentas T, el estado de resultados y el balance general,
  incluido el indicador `saldoContrarioEnReporte`.
- No se agrego, elimino ni renombro ningun metodo: el inventario de 100 metodos
  es identico al de la linea base. No hay `new` de clases del proyecto,
  herencia, polimorfismo ni interfaces; todos los metodos y campos son
  `static`.
- Se conservaron los mensajes de consola, las 12 opciones, la logica contable,
  la tolerancia de 0.005, la partida doble, las validaciones y los formatos de
  `data/`.
- `tests/pruebas_fase_d.ps1`: compila todos los `src/*.java` y la suite completa
  pasa despues de cada etapa de la separacion.
- `README.md`: se agrego la estructura de los nueve modulos y se actualizo la
  nota de compilacion.
- `AGENTS.md`: se actualizo la estructura, el comando de compilacion y esta
  entrada del historial.
- Los archivos de `data/` no se modificaron: los hashes SHA-256 son identicos a
  la linea base.
- La rama de trabajo fue `refactor/modularizacion-9`, con el tag
  `pre-modularizacion` como punto de recuperacion; al final se integro en
  `main`.

## Regla de comunicacion

Siempre incluir un resumen de los cambios realizados y la razon de cada uno.
El resumen debe incluir: que se modifico, en que archivo, y por que se hizo
el cambio. Usar un formato claro y conciso con viñetas o secciones.

## Compilacion y ejecucion

```powershell
# Compilar todos los modulos
javac -d bin (Get-ChildItem src\*.java).FullName

# Ejecutar
java -cp bin Contabilidad
```

Si se elimina o renombra un modulo, conviene borrar los `.class` de `bin/`
antes de recompilar para no dejar clases de versiones anteriores.

## Notas para el desarrollador

1. Si se agrega un nuevo tipo de cuenta, actualizar:
   - El arreglo `TIPOS_CUENTA` en `Datos.java` (fuente unica para `tipoValido()`
     y `pedirTipoCuenta()`)
   - `naturalezaSugerida()` en `Datos.java` para definir la naturaleza inicial
   - La logica de `efectoSaldoNaturaleza()`, `tipoDeudor()` y
     `saldoNaturaleza()` en `Asientos.java` para definir el efecto del
     debito/credito y el saldo normal
   - La logica de balance y estado de resultados en `Reportes.java`

2. Las listas paralelas deben mantenerse sincronizadas: el indice `i` representa
   la misma cuenta o asiento en todas las listas. No desincronizar sus tamaños.

3. Los archivos `.txt` en `data/` se generan automaticamente si no existen.
   No es necesario crearlos manualmente. El separador de campos es `|`.

4. El sistema es de un solo usuario, no concurrencia ni bloqueo de archivos.

5. Los archivos JSON antiguos y `lib/gson.jar` fueron eliminados en el Cambio 23.
   No deben volver a implementarse en el codigo.

6. Los saldos son derivados: se recalculan desde `asientos.txt` al iniciar.
   `cuentas.txt` guarda el resultado del calculo, pero no es la fuente de verdad.
   No se deben editar saldos manualmente en ese archivo. `cuentas.txt` usa cinco
   campos (`codigo|nombre|tipo|naturaleza|saldo`); los archivos antiguos de
   cuatro campos se leen infiriendo la naturaleza y se migran al guardar.
   `data/asientos.txt` debe conservarse aunque este vacio: es la fuente de verdad
   y un estado valido cuando no hay asientos registrados.

7. Solo existe el respaldo manual de la opcion 10. Si al iniciar se detectan
   lineas invalidas en `cuentas.txt` o asientos invalidos en `asientos.txt`, se
   activa `datosInvalidos`, se informa por consola y no se sobrescribe ningun
   archivo. No se crean respaldos automaticos ni copias de archivos invalidos.
   El archivo original queda intacto para corregirlo a mano o restaurar un
   respaldo.

8. El respaldo manual se crea con la opcion 10 en
   `backup/manual_YYYYMMDD_HHmmss_SSS/` y copia `cuentas.txt`, `asientos.txt` y
   `balance.txt` si existen. La opcion no tiene submenu ni pide confirmacion
   adicional.

9. No hay restauracion integrada. Para recuperar un respaldo, cierre el
   programa y copie manualmente `cuentas.txt`, `asientos.txt` y `balance.txt`
   desde la carpeta del respaldo hacia `data/`.

10. Todos los reportes usan el historico completo: no hay periodos, rangos de
    fechas ni vista periodo/historico. Tampoco existe deteccion de cambios
    externos: si se editan los archivos con el programa abierto, el proximo
    guardado sobrescribe esas ediciones. La opcion 9 solo lista los asientos;
    no hay busqueda por fecha, cuenta ni descripcion.

11. Para ejecutar las pruebas de regresion:
    `powershell -ExecutionPolicy Bypass -File tests/pruebas_fase_d.ps1` y
    `powershell -ExecutionPolicy Bypass -File tests/pruebas_logica_contable.ps1`.
    Usan un directorio temporal y no modifican `data/` ni `backup/` reales.

12. Al iniciar, `asientos.txt` se valida por bloques y los asientos invalidos se
    omiten completos. No se debe volver a cargar una partida invalida
    parcialmente. Se rechazan fechas fuera de `dd/MM/yyyy` entre 1900 y 2100,
    montos con mas de dos decimales y partidas que dejarian una cuenta no Mixta
    con saldo contrario. Si hay lineas invalidas, asientos descuadrados o falla
    la lectura, `datosInvalidos` queda activo y bloquea guardar, crear cuentas y
    registrar asientos hasta corregir el archivo y reiniciar. El libro diario y
    los reportes usan los saldos calculados desde las partidas, no desde
    `asientoTotales`.

13. `asientos.txt` usa cuatro campos por asiento (`ASIENTO|fecha|descripcion|
    total`) y cuatro por partida (`PARTIDA|codigo|tipo|monto`). No se admiten
    marcas de cierre ni campos adicionales.

14. Si `cuentas.txt` queda vacio o sin lineas validas, se usa el catalogo
    estandar solo en memoria y `datosInvalidos` queda activo: no se sobrescribe
    `cuentas.txt` ni `asientos.txt`, porque las cuentas de los asientos no
    existirian. Para recuperar, restaure un respaldo o corrija el archivo y
    reinicie. No hay reconstruccion integrada del catalogo.

15. Todas las lecturas de la consola pasan por `leerEntrada()`. Cuando se agota
    la entrada, esa funcion activa `entradaCerrada` y devuelve `null`. Todo
    llamador debe validar el retorno: `leerInt()` devuelve el minimo,
    `leerDouble()` devuelve `0.0` y `leerFecha()` devuelve `null`. Ningun metodo
    debe llamar `equals`, `split` o `contains` sobre esos retornos sin
    verificarlos.

16. Los cuatro archivos de `src/` son modulos de organizacion con metodos y
    campos `static`. No representan cuentas, asientos ni partidas y no se
    instancian. La simplificacion se completo en el Cambio 35; cualquier archivo
    nuevo debe compilarse junto con los demas.

17. Cada cuenta tiene una naturaleza (`Deudora`, `Acreedora` o `Mixta`) guardada
    en `cuentaNaturalezas`. Una cuenta no Mixta no puede quedar con saldo
    contrario: la validacion se aplica al registrar partidas y al cargar
    `asientos.txt`. Las excepciones del catalogo estandar son `1790` y `1890`
    (`Acreedora`), `3120` y `3500` (`Deudora`) y `3200` (`Mixta`).

18. La busqueda de cuentas vive en `Datos.buscarCuentas()` y `Datos.pedirCuenta()`.
    El codigo exacto se resuelve primero; una consulta de solo digitos busca por
    prefijo de codigo; el resto compara sin tildes ni mayusculas contra codigo,
    nombre, tipo y naturaleza. Con una coincidencia se selecciona sola, con 2 a
    12 se muestra una lista y con mas de 12 se pide un texto mas especifico.

### Cambio 35: Simplificacion segun la rubrica del curso

**Razon:** El proyecto conservaba funciones que no forman parte de la consigna
(periodos, rangos de reportes, cierre automatico, reconstruccion del catalogo,
advertencias de saldos contrarios, busquedas avanzadas y respaldos automaticos)
y el codigo seguia repartido en nueve modulos. Se acordo una simplificacion
completa para dejar solo las 11 opciones y los seis reportes pedidos, con cuatro
modulos y proteccion simple de archivos.

**Impacto:**
- `src/Contabilidad.java`: conserva `main`, el menu de 11 opciones, el submenu
  de administracion (ver catalogo y crear cuenta), las lecturas validadas, la
  validacion de montos y fechas, y `salir()`.
- `src/Datos.java`: concentra las listas paralelas, el catalogo estandar de 75
  cuentas, las fechas, el formateo, la carga/guardado atomico, la bandera
  `datosInvalidos` y el respaldo manual.
- `src/Asientos.java`: concentra el registro de asientos, la partida doble, el
  calculo de saldos, los movimientos por cuenta y el listado de asientos.
- `src/Reportes.java`: concentra cuentas T, libro diario, libro mayor, balanza de
  comprobacion, estado de resultados y balance general.
- Se eliminaron `Consola.java`, `Cuentas.java`, `Estado.java`, `Fechas.java`,
  `Formato.java` y `Persistencia.java`. Quedan cuatro modulos `static`, sin
  objetos de dominio, herencia, polimorfismo ni interfaces.
- Se elimino la opcion 12 y todo el control de periodos y rangos: reportes
  filtrados, cierre automatico, marca `|CIERRE`, vista periodo/historico, avance
  de periodo y bloqueo de fechas fuera del periodo.
- Se elimino la opcion 1.4 (reconstruir catalogo), la busqueda avanzada de
  cuentas y asientos, los submenus de cuentas T y movimientos, y el submenu de
  respaldos.
- Se eliminaron los respaldos automaticos, la restauracion integrada, la
  reversion de asientos, la deteccion de cambios externos, el autoajuste de
  saldos y las advertencias de saldos contrarios.
- La persistencia conserva el formato de cuatro campos de `cuentas.txt` y
  `asientos.txt`, y el de cuatro campos de `balance.txt` (en el Cambio 36
  `cuentas.txt` paso a cinco campos). La escritura sigue siendo atomica
  (archivo `.tmp` + movimiento) y la bandera `datosInvalidos` impide guardar si
  la carga encontro lineas invalidas.
- Se mantiene un guardian simple de montos ambiguos: `1.800` se rechaza y se
  pide `1800` o `1800,00`.
- `data/periodo.txt` y `MANUAL_CIERRE_MANUAL.md` se eliminaron por obsoletos. No
  se modificaron los datos demo de `cuentas.txt`, `asientos.txt` ni
  `balance.txt`.
- `tests/pruebas_fase_d.ps1` se reescribio para el flujo nuevo: menu, catalogo,
  creacion de cuentas, registro, validaciones, reportes con valores numericos,
  respaldo manual y archivos invalidos. Todas las pruebas pasan y usan un
  directorio temporal.
- `README.md` se reescribio con las 11 opciones, los cuatro modulos, los formatos
  de archivo y la recuperacion manual de respaldos.
- `src/` quedo en 847 lineas fisicas (antes 1.374), sin contar `bin/`.

### Cambio 36: Naturalezas, validacion estricta y busqueda inteligente

**Razon:** El sistema permitia que una cuenta de naturaleza deudora quedara con
saldo acreedor; por ejemplo, Inventarios con 20.000 al Debe podia recibir un
credito de 60.000 y quedar en -40.000. Ademas, los montos aceptaban mas de dos
decimales o notacion cientifica, la carga de asientos no validaba el rango de
fechas ni la precision de los montos, y la busqueda de cuentas exigia recordar
el codigo exacto o mostraba listas demasiado largas.

**Impacto:**
- `src/Datos.java`: nueva lista paralela `cuentaNaturalezas` y constantes
  `NATURALEZAS`, `ANIO_MIN` y `ANIO_MAX`. Las 75 cuentas estandar reciben una
  naturaleza: Activo/Costo/Gasto `Deudora`; Pasivo/Patrimonio/Ingreso
  `Acreedora`; `1790` y `1890` `Acreedora`; `3120` y `3500` `Deudora`; `3200`
  `Mixta`.
- `cuentas.txt` pasa a cinco campos (`codigo|nombre|tipo|naturaleza|saldo`). Los
  archivos de cuatro campos se leen infiriendo la naturaleza con
  `naturalezaEstandar()` y se migran al guardar. Una naturaleza desconocida
  invalida la linea y activa `datosInvalidos`.
- `src/Datos.java`: `parsearMonto()` centraliza el parseo estricto. Acepta
  `1800`, `1800.50`, `1800,50`, `1.800,50` y `1,800.50`; rechaza `1.800`,
  `1.234`, `0.001`, `1e3`, negativos, cero, `NaN` e infinito. La carga de
  asientos usa el mismo parser, por lo que un monto con mas de dos decimales
  omite el bloque completo.
- `src/Datos.java`: `parsearFecha()` exige `dd/MM/yyyy` real y el rango
  1900-2100; `cargarBloque()` valida la fecha al cargar `asientos.txt`.
- `src/Datos.java`: nuevas `normalizarTexto()`, `buscarCuentas()`,
  `mostrarCuentaEncontrada()` y `pedirCuenta()`. La busqueda compara sin tildes
  ni mayusculas contra codigo, nombre, tipo y naturaleza, admite varios terminos
  en cualquier orden, usa prefijo de codigo para consultas de solo digitos y
  limita la lista a 12 coincidencias. `buscarCuenta()` agrega la opcion
  `1.3 Buscar cuenta`.
- `src/Asientos.java`: nuevas `efectoSaldoNaturaleza()`, `saldoNaturaleza()`,
  `saldosNaturaleza()`, `esNaturalezaAcreedora()`, `esNaturalezaMixta()` y
  `etiquetaSaldoNaturaleza()`. `registrarAsiento()` simula los saldos por
  naturaleza y rechaza una partida que dejaria una cuenta no Mixta con saldo
  contrario, mostrando saldo actual, monto y saldo resultante antes de volver a
  pedirla. La carga de `asientos.txt` aplica la misma regla y omite el asiento
  completo.
- `src/Asientos.java`: los prompts explican la naturaleza de la cuenta y si
  aumenta con el Debe o con el Haber.
- `src/Reportes.java`: cuentas T y libro mayor muestran tipo y naturaleza; el
  saldo acumulado del libro mayor se calcula por naturaleza. Los totales de los
  reportes siguen siendo por tipo, de modo que las cuentas de valuacion como
  `1790` y `1890` restan dentro del Activo.
- `src/Contabilidad.java`: `leerDouble()` usa `Datos.parsearMonto()`;
  `leerFecha()` usa `Datos.parsearFecha()` y explica el formato y el rango. El
  submenu de administracion agrega `3. Buscar una cuenta`.
- `tests/pruebas_fase_d.ps1`: se actualizaron las entradas de creacion de
  cuentas (naturaleza), los mensajes y el formato de cinco campos. La suite
  sigue pasando.
- `tests/pruebas_logica_contable.ps1`: nueva suite integral que cubre catalogo
  y naturalezas, busqueda, montos y fechas estrictos, partida doble, rechazo de
  saldos contrarios (incluido Inventarios 20.000/60.000), cuentas personalizadas
  Deudora/Acreedora, cuentas Mixtas, depreciacion y amortizacion, los seis
  reportes con valores exactos, migracion de cuatro a cinco campos, persistencia,
  archivos invalidos, respaldo manual, fin de entrada y caracter `|`.
- `data/cuentas.txt` se migro a cinco campos despues de respaldar los datos en
  `backup/pre_naturaleza_20261111_203703_613/`. Los hashes de `asientos.txt` y
  `balance.txt` no cambiaron.
- `README.md` y `AGENTS.md` documentan naturalezas, busqueda, formatos y pruebas.
- No se modifico el formato de `asientos.txt` ni la logica de partida doble.

