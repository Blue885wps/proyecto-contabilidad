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
  Datos.java          listas paralelas, catalogo, fechas y persistencia
  Asientos.java       registro, partida doble, saldos y movimientos
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

La opcion 1 abre un submenu con dos funciones: ver el catalogo y crear una
cuenta. La opcion 10 crea el respaldo directamente, sin submenu.

## Registrar una operacion (opcion 2)

1. Fecha en formato `dd/MM/yyyy`. Enter usa la fecha actual.
2. Descripcion del asiento. No se permite el caracter `|`.
3. Cuenta de cada partida. Se escribe el codigo exacto; `0` cancela.
4. Lado de la partida: `1` Debe (debito) o `2` Haber (credito).
5. Monto mayor que cero, con coma o punto decimal (`1800`, `1800.50`,
   `1800,50`). Se rechazan montos ambiguos como `1.800`.

Se exigen al menos dos partidas y que el total del Debe sea igual al del Haber
(tolerancia de 0.005). Si el asiento no cuadra, el programa muestra la diferencia
y permite agregar otra partida o descartarlo. Antes de guardar se muestra un
resumen y se pide confirmacion `S/N`.

## Consultas y reportes

- **Cuentas T (opcion 3):** pide el codigo de una cuenta y muestra sus cargos y
  abonos en dos columnas, con totales y saldo deudor/acreedor/cero.
- **Libro diario (opcion 4):** muestra todos los asientos en orden de fecha con
  el formato clasico `Cta | Detalle | Parcial | Debe | Haber`, con totales por
  asiento y total general.
- **Libro mayor (opcion 5):** pide una cuenta y muestra sus movimientos con el
  saldo acumulado.
- **Balanza de comprobacion (opcion 6):** muestra debitos, creditos, deudor y
  acreedor por cuenta, y verifica que la balanza cuadre.
- **Estado de resultados (opcion 7):** calcula utilidad bruta (ingresos menos
  costos) y utilidad neta (utilidad bruta menos gastos).
- **Balance general (opcion 8):** verifica
  `Activo = Pasivo + Patrimonio + Resultado` y guarda el resultado en
  `data/balance.txt`.
- **Consultar movimientos (opcion 9):** lista todos los asientos registrados.

Todos los reportes usan el historico completo. No hay filtros por periodo ni
rangos de fechas.

## Archivos de datos

El separador de campos es `|`. No edite los archivos con el programa abierto:
el proximo guardado sobrescribe los cambios externos.

### `data/cuentas.txt`

Cuatro campos por linea: `codigo|nombre|tipo|saldo`.

```text
1000|Caja|Activo|11500.00
3000|Capital social|Patrimonio|10000.00
```

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

- Al iniciar se validan las cuentas y los asientos. Una linea invalida o un
  asiento descuadrado se omite completo y se informa por consola.
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
```

Las pruebas compilan en un directorio temporal, cubren menu, catalogo, registro,
validaciones, reportes, respaldo y archivos invalidos, y no modifican `data/` ni
`backup/` reales.
