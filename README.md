# Sistema Contable Empresarial

Aplicacion de consola en Java para registrar operaciones contables por partida
doble, administrar el catalogo de cuentas, consultar cuentas T y generar los
reportes basicos. Los datos se guardan en archivos de texto plano dentro de
`data/`.

El enunciado original pide C#, pero el docente autorizo el uso de Java para esta
entrega. Conservar esa autorizacion para la defensa.

## Requisitos

- Java 11 o superior (`java` y `javac` en el PATH).
- Windows PowerShell para el script de pruebas incluido.

## Compilar y ejecutar

Desde la carpeta del proyecto:

```powershell
javac -d bin (Get-ChildItem src\*.java).FullName
java -cp bin Contabilidad
```

Si se elimino o renombro un modulo, borre `bin/` antes de compilar para no dejar
clases viejas. Ejecute el programa desde la carpeta que contiene `data/`, porque
las rutas son relativas.

Si `data/` no existe, se crea automaticamente con el catalogo estandar de 75
cuentas. Si `cuentas.txt` no existe, el catalogo se guarda al iniciar.

## Estructura del codigo

El codigo esta separado en cuatro modulos `static`. Ninguno se instancia: todos
se usan mediante `Modulo.metodo()`.

```text
src/
  Contabilidad.java   main, menus, lecturas validadas y salida
  Datos.java          listas paralelas, catalogo, naturalezas, busqueda y persistencia
  Asientos.java       registro, partida doble, saldos por naturaleza y movimientos
  Reportes.java       cuentas T, diario, mayor, balanza, resultados y balance
```

La logica contable usa listas paralelas y variables primitivas. No se usan
objetos de dominio, herencia, polimorfismo ni interfaces.

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

La opcion 1 abre un submenu con tres funciones: ver el catalogo, crear una
cuenta y buscar una cuenta. La opcion 10 crea el respaldo directamente, sin
submenu.

## Naturaleza de las cuentas

Cada cuenta tiene un tipo (`Activo`, `Pasivo`, `Patrimonio`, `Ingreso`, `Costo`
o `Gasto`) y una naturaleza (`Deudora`, `Acreedora` o `Mixta`):

| Naturaleza | Aumenta con | Saldo permitido |
|------------|-------------|-----------------|
| Deudora | Debe | Deudor o cero |
| Acreedora | Haber | Acreedor o cero |
| Mixta | Cualquiera | Deudor o acreedor |

Al crear una cuenta se elige la naturaleza. La sugerencia depende del tipo:
Activo, Costo y Gasto sugieren `Deudora`; Pasivo, Patrimonio e Ingreso sugieren
`Acreedora`. El usuario puede cambiarla para representar cuentas de valuacion o
contra cuentas.

El catalogo estandar aplica estas excepciones:

- `1790 Depreciacion acumulada` y `1890 Amortizacion acumulada`: `Acreedora`.
- `3120 Perdida del ejercicio` y `3500 Dividendos decretados`: `Deudora`.
- `3200 Resultados de ejercicios anteriores`: `Mixta`.

Si una partida dejaria una cuenta `Deudora` con saldo acreedor o una
`Acreedora` con saldo deudor, el programa rechaza esa partida, muestra el saldo
actual, el monto intentado y el saldo resultante, y vuelve a pedir la partida.
Una cuenta `Mixta` no tiene esa restriccion.

El catalogo (opcion `1.1`) muestra el saldo en la convencion de la naturaleza:
positivo cuando esta en su lado normal y negativo cuando es contrario. Los
saldos contrarios se marcan con `(contrario)` en la columna `Estado`; las
cuentas `Mixta` nunca se marcan porque no tienen un lado natural. Las cuentas T
y el libro mayor usan la misma marca y agregan una advertencia.

## Registrar una operacion (opcion 2)

1. Fecha en formato `dd/MM/yyyy`, real y entre 1900 y 2100. Enter usa la fecha
   actual.
2. Descripcion del asiento. No se permite el caracter `|`.
3. Cuenta de cada partida. Se acepta el codigo exacto o una busqueda por codigo,
   nombre, tipo o naturaleza; `0` cancela.
4. Lado de la partida: `1` Debe (debito) o `2` Haber (credito).
5. Monto mayor o igual a `0.01`, con hasta dos decimales. Se aceptan `1800`,
   `1800.50`, `1800,50`, `1.800,50` y `1,800.50`. Se rechazan `1.800`, `1.234`,
   `0.001`, `1e3`, negativos, cero, `NaN` e infinito.

Se exigen al menos dos partidas y que el total del Debe sea igual al del Haber
(tolerancia de 0.005). Si el asiento no cuadra, el programa muestra la diferencia
y permite agregar otra partida o descartarlo. Antes de guardar se muestra un
resumen y se pide confirmacion `S/N`.

## Buscar cuentas

La busqueda esta disponible al registrar asientos, al consultar cuentas T, al
consultar el libro mayor y en la opcion `1.3 Buscar cuenta`.

- El codigo exacto se usa de inmediato.
- Una busqueda con solo digitos funciona como prefijo de codigo.
- El texto se compara sin distinguir mayusculas ni tildes.
- Se puede buscar por nombre, tipo o naturaleza, y combinar palabras en
  cualquier orden; por ejemplo `activo acreedora` o `gasto depreciacion`.
- Si hay una sola coincidencia se selecciona automaticamente.
- Si hay entre 2 y 12 coincidencias se muestra una lista numerada.
- Si hay mas de 12 se pide escribir un texto mas especifico.
- `?` muestra el catalogo completo y `0` cancela.

## Consultas y reportes

- **Cuentas T (opcion 3):** permite buscar una cuenta y muestra su tipo,
  naturaleza, cargos y abonos en dos columnas, con totales y saldo
  deudor/acreedor/cero. Un saldo contrario a la naturaleza se marca
  `(contrario)` y genera una advertencia.
- **Libro diario (opcion 4):** muestra todos los asientos en orden de fecha con
  el formato clasico `Cta | Detalle | Parcial | Debe | Haber`, con totales por
  asiento y total general.
- **Libro mayor (opcion 5):** permite buscar una cuenta y muestra su tipo,
  naturaleza, movimientos y saldo acumulado, con la misma marca de saldo
  contrario.
- **Balanza de comprobacion (opcion 6):** muestra debitos, creditos, deudor y
  acreedor por cuenta, y verifica que la balanza cuadre.
- **Estado de resultados (opcion 7):** calcula utilidad bruta (ingresos menos
  costos) y utilidad neta (utilidad bruta menos gastos).
- **Balance general (opcion 8):** verifica
  `Activo = Pasivo + Patrimonio + Resultado` y guarda el resultado en
  `data/balance.txt`. `3200 Resultados de ejercicios anteriores` forma parte del
  patrimonio y no se suma al resultado. Si hay resultado en las cuentas
  nominales y ademas saldo en `3110` o `3120`, muestra una advertencia de
  posible doble conteo; el programa no realiza cierre contable automatico.
- **Consultar movimientos (opcion 9):** lista todos los asientos registrados.

Todos los reportes usan el historico completo. No hay filtros por periodo ni
rangos de fechas.

## Archivos de datos

El separador de campos es `|`. No edite los archivos con el programa abierto:
el proximo guardado sobrescribe los cambios externos.

### `data/cuentas.txt`

Cinco campos por linea: `codigo|nombre|tipo|naturaleza|saldo`.

```text
1000|Caja|Activo|Deudora|11500.00
1790|Depreciacion acumulada|Activo|Acreedora|0.00
3200|Resultados de ejercicios anteriores|Patrimonio|Mixta|0.00
```

Los archivos antiguos de cuatro campos (`codigo|nombre|tipo|saldo`) se siguen
leyendo: la naturaleza se deduce del codigo y del tipo, y al guardar el archivo
se reescribe con los cinco campos.

Los saldos son derivados: se recalculan desde los asientos al iniciar. No se
deben editar a mano.

### `data/asientos.txt`

Cada asiento tiene un encabezado y sus partidas:

```text
ASIENTO|01/11/2026|Aporte de capital|10000.00
PARTIDA|1000|Debito|10000.00
PARTIDA|3000|Credito|10000.00
```

### `data/balance.txt`

Cuatro campos: `fecha|activo|pasivo|patrimonio_con_resultado`.

## Respaldos

La opcion 10 copia `cuentas.txt`, `asientos.txt` y `balance.txt` (si existen) a
`backup/manual_YYYYMMDD_HHmmss_SSS/`. No hay restauracion integrada: para
recuperar un respaldo, cierre el programa y copie los archivos desde la carpeta
del respaldo hacia `data/`.

## Validaciones y proteccion de archivos

- Al iniciar se validan las cuentas y los asientos. Una linea invalida, un
  asiento descuadrado, una fecha fuera de `dd/MM/yyyy` entre 1900 y 2100, un
  monto con mas de dos decimales o una partida que dejaria una cuenta no Mixta
  con saldo contrario se omite completo y se informa por consola.
- Si hay datos invalidos, el programa permite consultar pero **no guarda** ni
  permite crear cuentas o registrar asientos, para no sobrescribir el archivo
  original. Corrija el archivo y reinicie.
- Las escrituras usan un archivo temporal y un movimiento atomico: si falla el
  guardado, el archivo anterior queda intacto.
- Si `cuentas.txt` esta vacio o no tiene cuentas validas, se usa el catalogo
  estandar solo en memoria y se bloquea el guardado. Restaure un respaldo o
  corrija el archivo antes de continuar.
- Si la entrada de consola termina, el programa cancela la operacion en curso y
  sale sin excepciones.

## Pruebas de regresion

```powershell
powershell -ExecutionPolicy Bypass -File tests/pruebas_fase_d.ps1
powershell -ExecutionPolicy Bypass -File tests/pruebas_logica_contable.ps1
```

Las pruebas compilan en un directorio temporal y no modifican `data/` ni
`backup/` reales. La primera suite cubre menu, catalogo, registro, validaciones,
reportes, respaldo y archivos invalidos. La segunda cubre naturalezas, busqueda
de cuentas, montos y fechas estrictos, partida doble, saldos contrarios,
depreciacion y amortizacion, los seis reportes con valores exactos, el aviso de
doble conteo, la exencion de cuentas Mixtas, migracion de archivos de cuatro a
cinco campos, persistencia, datos invalidos y fin de entrada.
