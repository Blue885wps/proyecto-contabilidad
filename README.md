# Sistema de Contabilidad

Aplicacion de consola en Java para registrar asientos contables por partida doble,
consultar el catalogo de cuentas y generar reportes. Los datos se guardan en
archivos de texto plano dentro de `data/`.

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
| 1 | Ver catalogo de cuentas |
| 2 | Crear una cuenta |
| 3 | Registrar un asiento contable |
| 4 | Ver asientos registrados |
| 5 | Generar balance general |
| 6 | Buscar cuenta por codigo o nombre |
| 7 | Ver estado de resultados |
| 8 | Ver estado de flujo de efectivo |
| 9 | Ver estado de cambios en el patrimonio |
| 10 | Ver balance de comprobacion |
| 11 | Ver libro diario |
| 12 | Ver libro mayor |
| 13 | Revertir un asiento |
| 14 | Reiniciar datos de prueba |
| 15 | Buscar asientos |
| 16 | Recalcular saldos desde los asientos |
| 17 | Recargar datos desde los archivos |
| 18 | Configurar periodo de reportes |
| 19 | Restaurar un respaldo |
| 0 | Guardar y salir |

## Busquedas

Las busquedas de cuentas y descripciones **no distinguen mayusculas ni tildes**.
Por ejemplo, `depreciacion`, `DEPRECIACIÓN` y `Depreciación` encuentran la misma
cuenta. Tambien se puede buscar por una parte del codigo o del nombre.

En las opciones que piden una cuenta:

- Escriba un codigo o parte del nombre.
- Si hay una sola coincidencia, se usa directamente.
- Si hay varias, se muestra una lista numerada para elegir.
- Escriba `?` para ver el catalogo completo.
- Escriba `0` para cancelar.

## Montos y fechas

Los montos aceptan coma o punto decimal:

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
fecha actual. En la busqueda por rango y en la configuracion del periodo, Enter
significa "sin limite".

## Registro de asientos

La opcion 3 pide fecha, descripcion y al menos dos partidas. Despues de la
segunda partida se puede agregar otra con `S/N`. El tipo de partida se elige con
un menu (1 = Debito, 2 = Credito).

Antes de guardar se muestra un resumen con las cuentas, los montos y los totales.
Se debe confirmar con `S/N`. Si se rechaza, el asiento no se guarda. El sistema
verifica que el total de debitos sea igual al total de creditos; si no cuadra,
rechaza el asiento.

## Reportes por periodo

La opcion 18 permite definir un rango de fechas para los reportes:

- **Balance general (5):** saldo acumulado hasta la fecha de corte (`hasta`).
  Si no hay fecha de corte, muestra todo el historico.
- **Estado de resultados (7):** ingresos, costos y gastos del rango.
- **Flujo de efectivo (8):** movimientos del rango.
- **Cambios en el patrimonio (9):** movimientos del rango y resultado del rango.
- **Balance de comprobacion (10):** debitos y creditos del rango.
- **Libro diario (11):** asientos del rango.
- **Libro mayor (12):** movimientos del rango y saldo inicial al `desde`.

Para volver a todo el historico, en la opcion 18 deje ambos campos vacios.

## Respaldos

El programa crea respaldos en `backup/`:

- `diario_YYYYMMDD/`: una vez al dia al iniciar. Se conservan los ultimos 10.
- `ajuste_.../`: cuando corrige saldos, fechas o totales automaticamente.
- `recalculo_.../`: al usar la opcion 16.
- `reinicio_.../`: antes de reiniciar los datos (opcion 14).
- `previo_restauracion_.../`: antes de restaurar un respaldo (opcion 19).

La opcion 19 lista los respaldos disponibles, pide el numero y exige escribir
`RESTAURAR`. Antes de restaurar crea un respaldo de seguridad. Si la restauracion
falla, intenta recuperar automaticamente el estado anterior.

## Archivos de datos

```text
data/
  cuentas.txt    Catalogo: codigo|nombre|tipo|saldo
  asientos.txt   Asientos y partidas (fuente de verdad)
  balance.txt    Ultimo balance generado: fecha_corte|activo|pasivo|patrimonio
```

- `asientos.txt` es la fuente de verdad; los saldos se recalculan desde ahi.
- No edite `cuentas.txt` ni `balance.txt` a mano salvo que sepa lo que hace.
- Si edita archivos por fuera del programa, use la opcion 17 para recargarlos.
- El programa avisa si detecta cambios externos, pero no recarga solo.

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

## Reinicio de datos

La opcion 14 borra todos los asientos, deja las 75 cuentas estandar con saldo
cero y elimina `balance.txt`. Antes crea un respaldo `reinicio_.../` y exige
escribir `REINICIAR`. Use esta opcion solo en pruebas o cuando quiera empezar de
cero.
