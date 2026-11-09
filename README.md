# Sistema Contable Empresarial

Aplicacion de consola en Java para registrar operaciones contables por partida
doble, administrar periodos, consultar el catalogo de cuentas, revisar cuentas T
y generar los reportes basicos. Los datos se guardan en archivos de texto plano
dentro de `data/`.

## Requisitos

- Java 11 o superior (`java` y `javac` en el PATH).
- Windows PowerShell para el script de pruebas incluido.

El enunciado original pide C#, pero el docente autorizo el uso de Java para esta
entrega. Conservar esa autorizacion para la defensa.

## Compilar y ejecutar

Desde la carpeta del proyecto:

```powershell
javac -d bin (Get-ChildItem src\*.java).FullName
java -cp bin Contabilidad
```

Si se elimino o renombro una clase del codigo, borre `bin/` antes de compilar
para que no queden clases viejas de versiones anteriores.

El programa debe ejecutarse desde la carpeta que contiene `data/`, porque usa
rutas relativas. Si `data/` no existe, se crea automaticamente con el catalogo
de 75 cuentas y un periodo predeterminado.

## Estructura del codigo

El codigo esta separado en nueve modulos `static`. Ninguno se instancia: todos
se usan mediante `Modulo.metodo()`.

```text
src/
  Contabilidad.java   main, menus, opcion 12 y salida
  Estado.java         listas paralelas, rango de reportes y tolerancias
  Consola.java        Scanner, menus y lecturas validadas
  Fechas.java         fechas y ordenamiento
  Formato.java        montos, texto y etiquetas
  Persistencia.java   archivos, bloqueos, carga/guardado y respaldos
  Cuentas.java        catalogo, tipos, creacion y busqueda de cuentas
  Asientos.java       registro, saldos, movimientos y busquedas
  Reportes.java       balanza, diario, mayor, cuentas T y balances
```

La logica contable sigue usando listas paralelas en `Estado` y variables
primitivas. No se usan objetos de dominio, herencia, polimorfismo ni
interfaces.

## Menu

| Opcion | Funcion |
|--------|---------|
| 1 | Administrar cuentas |
| 2 | Registrar operacion contable |
| 3 | Consultar cuentas T |
| 4 | Generar libro diario |
| 5 | Generar libro mayor |
| 6 | Generar balanza de comprobacion |
| 7 | Generar estado de resultados |
| 8 | Generar balance general |
| 9 | Consultar movimientos |
| 10 | Respaldar informacion |
| 11 | Salir (tambien acepta `0`) |
| 12 | Gestionar periodos contables |

La opcion 12 se agrega para controlar el periodo activo sin quitar ninguna de las
11 opciones de la consigna.

### Submenus

**1. Administrar cuentas**

1. Ver catalogo
2. Crear cuenta
3. Buscar cuenta
4. Reconstruir catalogo estandar
0. Volver

**3. Consultar cuentas T**

1. Consultar una cuenta
2. Cuentas con movimientos
3. Cuentas T de todo el catalogo
0. Volver

**9. Consultar movimientos**

1. Ver asientos registrados
2. Buscar asientos
0. Volver

**12. Gestionar periodos contables**

1. Ver periodo actual
2. Configurar periodo
3. Cerrar periodo y trasladar resultado
4. Reportes: periodo actual / historico
0. Volver

La opcion 10 crea el respaldo directamente, sin submenu.

## Recuperar un catalogo vacio o invalido

Si `data/cuentas.txt` existe pero no tiene ninguna cuenta valida (por ejemplo,
quedo vacio o se borro a mano), el programa arranca avisando la situacion y
**bloquea** la creacion de cuentas y el registro de asientos, porque no puede
saber si un saldo pertenece a una cuenta real.

La opcion **1.4 Reconstruir catalogo estandar** recupera la situacion:

- Si el archivo tiene asientos que usan cuentas ajenas al catalogo estandar
  (por ejemplo `A100`), la reconstruccion se **rechaza** y se informa cuales son
  las cuentas afectadas. Hay que corregir o eliminar esos asientos primero.
- Si el catalogo actual tiene cuentas, pide confirmacion y avisa que se
  perderan las cuentas personalizadas.
- Guarda una copia del archivo actual en
  `backup/cuentas_antes_reconstruir_YYYYMMDD_HHmmss_SSS.txt`.
- Vuelve a cargar las 75 cuentas estandar, guarda `cuentas.txt` y recarga
  `asientos.txt` para que los saldos queden correctos.
- Si la recarga de `asientos.txt` falla o no se puede guardar el respaldo, el
  programa bloquea el guardado de asientos y pide reiniciar; no sobrescribe
  `asientos.txt`.

Mientras el catalogo este vacio, el programa **no sobrescribe
`data/asientos.txt`**, aunque los asientos no se puedan cargar (porque sus
cuentas no existen todavia). Asi no se pierden los datos originales: al
reconstruir el catalogo, los asientos se recuperan intactos.

## Fin de la entrada

Si la entrada termina (por ejemplo al piping de un archivo o al presionar
Ctrl+Z en Windows), el programa no se cae: avisa `No hay mas entrada
disponible`, cancela la operacion en curso, guarda los datos y termina con
`Hasta luego.`

## Periodos contables

El periodo activo se guarda en `data/periodo.txt` con el formato
`inicio|fin`. Las dos fechas son inclusivas y usan `dd/MM/yyyy`.

Si el archivo no existe, el programa crea uno con el ano actual y lo amplia
para cubrir la fecha mas antigua y la mas reciente de los asientos existentes.
Si el archivo existe pero es invalido, avisa y lo reemplaza por ese periodo
predeterminado.

### Ver periodo actual (12.1)

Muestra el inicio, el fin, el estado (`ABIERTO` o `CERRADO (solo consulta)`), la
vista de reportes activa y cuantos asientos estan dentro y fuera del periodo.

Un periodo esta cerrado cuando contiene un asiento marcado como cierre. No se
guarda una bandera aparte: el cierre se deduce del asiento.

### Configurar periodo (12.2)

Pide una fecha de inicio y una fecha de fin, valida que sean fechas reales y que
el fin no sea anterior al inicio. Antes de guardar pide confirmacion `S/N`.

El rango puede ser mensual, anual o personalizado. Si el rango contiene un
asiento de cierre, queda como periodo de consulta: se pueden ver sus reportes,
pero no se pueden registrar asientos nuevos ni volver a cerrarlo.

Al configurar un periodo, el programa tambien avisa si:

- hay asientos fuera del rango elegido;
- hay resultados de periodos anteriores que todavia no se cerraron.

Los asientos fuera del periodo no se borran. Simplemente no aparecen en los
reportes del periodo actual; si aparecen en la vista historica.

### Cerrar periodo (12.3)

El cierre solo se permite en un periodo abierto. El programa toma los saldos
acumulados de las cuentas de tipo `Ingreso`, `Costo` y `Gasto` hasta la fecha de
fin del periodo, muestra un resumen y pide confirmacion `S/N`.

Si se confirma, crea un asiento de cierre con fecha igual al fin del periodo:

- deja en cero las cuentas de Ingreso, Costo y Gasto;
- traslada el resultado a la cuenta
  `3200 Resultados de ejercicios anteriores`;
- si hubo utilidad, acredita `3200`; si hubo perdida, la debita;
- guarda el asiento en `asientos.txt` con la marca `|CIERRE`.

El cierre usa los saldos acumulados hasta la fecha de fin. Si hay periodos
anteriores sin cerrar, el asiento incluye tambien esos resultados y el programa
lo advierte antes de continuar. Lo recomendable es cerrar los periodos en orden.

Si no hay saldos pendientes en las cuentas de resultado, el programa informa
`No hay resultados pendientes de cierre en el periodo.` y no crea nada.

Despues de cerrar, el programa avanza automaticamente el periodo:

- un ano completo pasa al mismo rango del ano siguiente;
- un mes completo pasa al mes siguiente;
- un rango personalizado pasa a un rango de la misma cantidad de dias,
  comenzando el dia siguiente al fin anterior.

El nuevo periodo se guarda en `data/periodo.txt`. Se puede ajustar con la
opcion 12.2.

### Vista de reportes (12.4)

La opcion 12.4 alterna entre:

- **Periodo actual:** los reportes y consultas usan solo el periodo configurado
  (con el comportamiento acumulado que se explica en la seccion Reportes).
- **Historico completo:** los reportes y consultas usan todos los asientos.

La vista no cambia el periodo activo ni los datos. Solo cambia lo que se muestra.
El registro de asientos siempre usa el periodo configurado, aunque la vista este
en historico.

## Registro de operaciones

La opcion 2 pide fecha, descripcion y al menos dos partidas. Cada partida tiene
cuenta, lado y monto. El lado se elige con un menu (1 = Debe (debito), 2 = Haber
(credito)). Despues de cada partida se muestra el acumulado del Debe y del Haber;
despues de la segunda se puede agregar otra con `S/N`.

La fecha debe estar dentro del periodo activo. Si el periodo esta cerrado, el
registro se bloquea y se debe configurar un periodo abierto en la opcion 12.

Si el asiento no cuadra, se muestra la diferencia entre el Debe y el Haber y se
ofrece agregar otra partida para corregirlo o descartar el asiento. Antes de
guardar se muestra un resumen con las cuentas, los montos y los totales. Se debe
confirmar con `S/N`. Si se rechaza, el asiento no se guarda.

Las descripciones, nombres y codigos no pueden contener el caracter `|`. Los
codigos `0` y `?` estan reservados para cancelar y ver el catalogo.

## Busquedas

Las busquedas de cuentas y descripciones **no distinguen mayusculas ni tildes**.
Por ejemplo, `depreciacion`, `DEPRECIACIÓN` y `Depreciación` encuentran la misma
cuenta. Tambien se puede buscar por una parte del codigo o del nombre.

En las opciones que piden una cuenta:

- Escriba el codigo (por ejemplo, `1000`) o parte del nombre (por ejemplo, `caja`).
- Si hay una sola coincidencia, se muestra y se usa directamente.
- Si hay varias, se muestra una lista numerada para elegir.
- En esa lista, escriba `V` para volver a buscar sin cancelar la operacion.
- Escriba `?` para ver el catalogo completo.
- Escriba `0` para cancelar la operacion.

La busqueda de asientos permite buscar por fecha exacta o por texto en la
descripcion. Respeta la vista activa: en modo periodo solo busca dentro del
periodo; en modo historico busca en todo el archivo.

## Montos y fechas

Los montos aceptan coma o punto decimal. Escriba solo numeros y separadores;
no use simbolos de moneda como `$`.

```text
1800
1800,50
1800.50
1.800,50
1,800.50
```

No se aceptan montos en cero, negativos, `NaN`, infinito ni cantidades ambiguas
como `1.800` (tres decimales con un solo punto). En ese caso use `1800` o
`1.800,00`.

Las fechas usan el formato `dd/MM/yyyy`. En los campos de fecha, Enter usa la
fecha actual.

## Saldos en el lado contrario

Antes de aceptar cada partida, el programa calcula como quedaria el saldo de la
cuenta. Si la cuenta pasa de su lado normal al lado contrario, muestra una
advertencia con el saldo actual, la partida y el saldo resultante, y pide
confirmacion `S/N`.

Por ejemplo, si Inventarios tiene 2,000 en el Debe y se registra un credito de
65,000, el programa avisa que la cuenta quedaria con saldo de -63,000. Si se
confirma, la cuenta T y el Balance General marcan el saldo con un aviso.

Esto no bloquea automaticamente las cuentas de naturaleza contraria, como
`1790 Depreciacion acumulada`, `1890 Amortizacion acumulada` o
`3200 Resultados de ejercicios anteriores`, que pueden tener saldo negativo de
forma legitima. Al iniciar, el programa tambien avisa si alguna cuenta quedo con
saldo en el lado contrario.

## Cuentas T

La opcion 3 muestra cada cuenta en dos columnas, **Debe** y **Haber**, con fecha,
concepto y monto de cada movimiento. Al final presenta los totales de cada lado
y el saldo de la cuenta (Deudor, Acreedor o Cero).

En modo periodo, la cuenta T muestra el saldo inicial al dia anterior al inicio
del periodo y los movimientos del periodo. En modo historico muestra todo el
historico. Si el saldo queda en el lado contrario al normal para el tipo de
cuenta (por ejemplo, un activo con saldo acreedor), la cuenta T muestra un aviso.
Puede ser una cuenta de naturaleza contraria o un error de registro.

Se puede consultar una cuenta, solo las cuentas con movimientos o el catalogo
completo.

## Reportes

La vista activa se cambia en la opcion 12.4. Por defecto se usa el periodo
actual.

- **Libro diario (4):** formato clasico `Cta | Detalle | Parcial | Debe | Haber`.
  Cada asiento se numera, muestra primero las partidas del Debe y luego las del
  Haber con el prefijo `A `, deja vacia la columna `Parcial` y coloca la fecha y
  la descripcion al pie. Los asientos se ordenan por fecha y conservan su numero
  original. Al final se muestra un unico `TOTAL GENERAL`. En modo periodo solo
  incluye los asientos del periodo; los asientos de cierre se marcan como
  `(cierre)`.
- **Libro mayor (5):** movimientos de una cuenta ordenados por fecha y saldo
  acumulado. En modo periodo muestra el saldo inicial antes del periodo y los
  movimientos del periodo.
- **Balanza de comprobacion (6):** debitos, creditos, deudor y acreedor por
  cuenta; verifica que el total de debitos sea igual al de creditos. En modo
  periodo muestra los movimientos acumulados hasta el fin del periodo.
- **Estado de resultados (7):** ingresos, costos y gastos; utilidad bruta y
  utilidad neta. En modo periodo usa solo los movimientos del periodo. Los
  asientos de cierre no se cuentan para no duplicar el resultado. En modo
  historico acumula todos los movimientos de resultado, tambien sin contar los
  cierres.
- **Balance general (8):** activo, pasivo, patrimonio y resultado del periodo;
  verifica `Activo = Pasivo + Patrimonio + Resultado`. En modo periodo usa los
  saldos acumulados hasta la fecha de fin, incluyendo los asientos de cierre. Si
  el periodo ya fue cerrado, las cuentas de resultado quedan en cero y el
  resultado aparece en `3200`; por eso el renglon `Resultado del periodo` sera
  cero. Antes del cierre muestra el resultado pendiente. En modo historico usa
  todo el historico.

El balance general guarda el balance en `data/balance.txt`. En modo periodo el
primer campo es la fecha de fin del periodo; en modo historico es la fecha de
generacion. El cuarto campo es el patrimonio mas el resultado.

### Criterios contables

- El resultado del periodo se calcula como `Ingresos - Costos - Gastos`.
- El balance general verifica `Activo = Pasivo + Patrimonio + Resultado`.
- El cierre automatico usa la cuenta
  `3200 Resultados de ejercicios anteriores`. No registre el resultado a mano en
  `3110` ni `3120` para evitar un doble conteo.
- Las cuentas contrarias estandar `1790`, `1890`, `3120`, `3200` y `3500` no
  generan el aviso de saldo en el lado contrario. Una cuenta contraria
  personalizada puede mostrar ese aviso, pero los calculos siguen siendo
  correctos.

## Respaldos

La opcion 10 crea una copia manual en `backup/manual_YYYYMMDD_HHmmss_SSS/` con
los archivos de datos que existan, incluido `periodo.txt`. No hay respaldos
automaticos periodicos ni restauracion desde el programa.

Si al iniciar se detectan lineas invalidas en `cuentas.txt` o asientos invalidos
en `asientos.txt`, se guarda una copia del archivo original como
`backup/cuentas_invalidas_....txt` o `backup/asientos_invalidos_....txt`. Si no
se puede crear la copia de `asientos.txt`, el programa conserva el archivo y no
permite registrar asientos hasta corregirlo.

Para recuperar un respaldo, cierre el programa y copie manualmente
`cuentas.txt`, `asientos.txt`, `balance.txt` y `periodo.txt` desde la carpeta del
respaldo hacia `data/`. Si el respaldo es de una version anterior y no tiene
`periodo.txt`, el programa creara uno predeterminado al iniciar.

## Archivos de datos

```text
data/
  cuentas.txt    Catalogo: codigo|nombre|tipo|saldo
  asientos.txt   Asientos y partidas (fuente de verdad)
  periodo.txt    Periodo activo: inicio|fin
  balance.txt    Ultimo balance generado: fecha|activo|pasivo|patrimonio+resultado
```

- `asientos.txt` es la fuente de verdad; los saldos se recalculan desde ahi al
  iniciar.
- Un asiento normal usa cuatro campos:
  `ASIENTO|fecha|descripcion|total`. Un asiento de cierre agrega un quinto campo:
  `ASIENTO|fecha|descripcion|total|CIERRE`.
- Las partidas usan `PARTIDA|codigo|Debito o Credito|monto`.
- Al iniciar, `asientos.txt` se valida: fecha real, al menos dos partidas (una
  al Debe y una al Haber), cuentas existentes, montos validos y Debe = Haber con
  tolerancia de 0.005. Si un asiento es invalido, se omite completo y se informa
  el motivo. Una linea desconocida dentro de un asiento tambien lo invalida.
- Si `asientos.txt` no se puede leer, el programa informa el error y bloquea el
  guardado para no sobrescribir el archivo.
- `cuentas.txt` exige codigo y nombre no vacios, tipo valido y saldo numerico.
- `periodo.txt` exige dos fechas validas con fin mayor o igual al inicio.
- Si se omiten cuentas o asientos invalidos, el programa guarda una copia del
  archivo original en `backup/` antes de reescribirlo.
- No edite `cuentas.txt` ni `balance.txt` a mano salvo que sepa lo que hace.
- Si edita los archivos con el programa abierto, los cambios no se recargan:
  el proximo guardado sobrescribira esas ediciones.

## Pruebas

El script `tests/pruebas_fase_d.ps1` compila el proyecto y ejecuta pruebas sobre
copias temporales de los datos. No toca `data/` ni `backup/` reales. Incluye
pruebas del libro diario clasico, la carga de asientos invalidos, la tolerancia
de 0.005, los montos estrictos, las lineas desconocidas dentro de un asiento, la
exencion de cuentas contrarias como `1790`, las columnas Deudor/Acreedor de la
balanza, el bloqueo de guardado cuando falla la lectura de asientos, la etiqueta
de cuentas T acreedoras, el funcionamiento de los periodos, el cierre con
traslado a `3200`, el avance automatico del periodo, el bloqueo de asientos en
periodos cerrados, el rechazo de fechas fuera del periodo y el respaldo de
`periodo.txt`.

Tambien cubre los dos casos de estabilidad agregados en el ultimo cambio:

- **fin de entrada:** seis escenarios (registrar asiento, consultar cuenta T,
  configurar periodo, crear cuenta, buscar asientos y respaldar) verifican que
  el programa cierra con `Hasta luego.` y sin `Exception in thread`;
- **recuperacion del catalogo:** catalogo vacio que se reconstruye y vuelve a
  ser operativo, preservacion de `asientos.txt` durante la recuperacion y
  rechazo de la reconstruccion cuando hay una cuenta huerfana.

```powershell
powershell -ExecutionPolicy Bypass -File tests/pruebas_fase_d.ps1
```

Use `-Keep` para conservar el directorio temporal y revisar las salidas:

```powershell
powershell -ExecutionPolicy Bypass -File tests/pruebas_fase_d.ps1 -Keep
```
