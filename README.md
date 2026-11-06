# Sistema Contable Empresarial

Aplicacion de consola en Java para registrar operaciones contables por partida
doble, consultar el catalogo de cuentas, revisar cuentas T y generar los
reportes basicos. Los datos se guardan en archivos de texto plano dentro de
`data/`.

## Requisitos

- Java 11 o superior (`java` y `javac` en el PATH).
- Windows PowerShell para el script de pruebas incluido.

## Compilar y ejecutar

Desde la carpeta del proyecto:

```powershell
javac -d bin src/Contabilidad.java
java -cp bin Contabilidad
```

El programa debe ejecutarse desde la carpeta que contiene `data/`, porque usa
rutas relativas. Si `data/` no existe, se crea automaticamente con el catalogo
de 75 cuentas.

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

### Submenus

**1. Administrar cuentas**

1. Ver catalogo
2. Crear cuenta
3. Buscar cuenta
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

La opcion 10 crea el respaldo directamente, sin submenu.

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
descripcion.

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

## Registro de operaciones

La opcion 2 pide fecha, descripcion y al menos dos partidas. Cada partida tiene
cuenta, lado y monto. El lado se elige con un menu (1 = Debe (debito), 2 = Haber
(credito)). Despues de cada partida se muestra el acumulado del Debe y del Haber;
despues de la segunda se puede agregar otra con `S/N`.

Si el asiento no cuadra, se muestra la diferencia entre el Debe y el Haber y se
ofrece agregar otra partida para corregirlo o descartar el asiento. Antes de
guardar se muestra un resumen con las cuentas, los montos y los totales. Se debe
confirmar con `S/N`. Si se rechaza, el asiento no se guarda.

Las descripciones, nombres y codigos no pueden contener el caracter `|`. Los
codigos `0` y `?` estan reservados para cancelar y ver el catalogo.

## Cuentas T

La opcion 3 muestra cada cuenta en dos columnas, **Debe** y **Haber**, con fecha,
concepto y monto de cada movimiento. Al final presenta los totales de cada lado
y el saldo de la cuenta (Deudor, Acreedor o Cero).

Se puede consultar una cuenta, solo las cuentas con movimientos o el catalogo
completo.

## Reportes

Todos los reportes usan el historico completo de asientos; no hay periodo
configurable.

- **Libro diario (4):** asientos ordenados por fecha, con su numero original.
- **Libro mayor (5):** movimientos de una cuenta ordenados por fecha y saldo
  acumulado.
- **Balanza de comprobacion (6):** debitos, creditos y saldo por cuenta; verifica
  que el total de debitos sea igual al de creditos.
- **Estado de resultados (7):** ingresos, costos y gastos; utilidad bruta y
  utilidad neta.
- **Balance general (8):** activo, pasivo, patrimonio y resultado del ejercicio;
  verifica `Activo = Pasivo + Patrimonio + Resultado`.

El balance general guarda la fecha de generacion en `data/balance.txt`.

## Respaldos

La opcion 10 crea una copia manual en `backup/manual_YYYYMMDD_HHmmss_SSS/` con
los archivos de datos que existan. No hay respaldos automaticos ni restauracion
desde el programa.

Para recuperar un respaldo, cierre el programa y copie manualmente
`cuentas.txt`, `asientos.txt` y `balance.txt` desde la carpeta del respaldo
hacia `data/`.

## Archivos de datos

```text
data/
  cuentas.txt    Catalogo: codigo|nombre|tipo|saldo
  asientos.txt   Asientos y partidas (fuente de verdad)
  balance.txt    Ultimo balance generado: fecha|activo|pasivo|patrimonio
```

- `asientos.txt` es la fuente de verdad; los saldos se recalculan desde ahi al
  iniciar.
- No edite `cuentas.txt` ni `balance.txt` a mano salvo que sepa lo que hace.
- Si edita los archivos con el programa abierto, los cambios no se recargan:
  el proximo guardado sobrescribira esas ediciones.

## Pruebas

El script `tests/pruebas_fase_d.ps1` compila el proyecto y ejecuta pruebas sobre
copias temporales de los datos. No toca `data/` ni `backup/` reales.

```powershell
powershell -ExecutionPolicy Bypass -File tests/pruebas_fase_d.ps1
```

Use `-Keep` para conservar el directorio temporal y revisar las salidas:

```powershell
powershell -ExecutionPolicy Bypass -File tests/pruebas_fase_d.ps1 -Keep
```
