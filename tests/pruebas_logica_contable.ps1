param(
    [switch]$Keep
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$temp = Join-Path ([System.IO.Path]::GetTempPath()) ("contab_logica_" + [Guid]::NewGuid().ToString("N"))

function Write-Utf8 {
    param([string]$Path, [string]$Content)
    [System.IO.File]::WriteAllText($Path, $Content, [System.Text.UTF8Encoding]::new($false))
}

function Assert-Contains {
    param([string]$Text, [string]$Expected, [string]$Name)
    if (-not $Text.Contains($Expected)) { throw "FALLO: $Name. No se encontro: $Expected" }
    Write-Host "OK: $Name"
}

function Assert-NotContains {
    param([string]$Text, [string]$Unexpected, [string]$Name)
    if ($Text.Contains($Unexpected)) { throw "FALLO: $Name. Se encontro: $Unexpected" }
    Write-Host "OK: $Name"
}

function Assert-Match {
    param([string]$Text, [string]$Pattern, [string]$Name)
    if ($Text -notmatch $Pattern) { throw "FALLO: $Name. No se encontro el patron: $Pattern" }
    Write-Host "OK: $Name"
}

function Assert-Count {
    param([string]$Text, [string]$Needle, [int]$Expected, [string]$Name)
    $count = ([regex]::Matches($Text, [regex]::Escape($Needle))).Count
    if ($count -ne $Expected) { throw "FALLO: $Name. Esperado $Expected, encontrado $count." }
    Write-Host "OK: $Name"
}

function New-TestDir {
    param([string]$Name)
    $dir = Join-Path $temp $Name
    New-Item -ItemType Directory -Path (Join-Path $dir "data") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $dir "backup") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $dir "bin") -Force | Out-Null
    Copy-Item (Join-Path $temp "bin\*.class") (Join-Path $dir "bin\") -Force
    return $dir
}

function Write-Input {
    param([string]$Dir, [string]$Name, [string[]]$Lines)
    $path = Join-Path $Dir $Name
    Write-Utf8 $path (($Lines -join "`r`n") + "`r`n")
    return $path
}

function Invoke-App {
    param([string]$Dir, [string]$InputFile)
    Push-Location $Dir
    try {
        cmd /c "java -Dfile.encoding=UTF-8 -Duser.language=es -Duser.country=ES -cp bin Contabilidad < $InputFile > output.txt 2>&1"
        if ($LASTEXITCODE -ne 0) { throw "Java termino con codigo $LASTEXITCODE en $Dir" }
        return [System.IO.File]::ReadAllText((Join-Path $Dir "output.txt"))
    } finally {
        Pop-Location
    }
}

function Invoke-Steps {
    param([string]$Dir, [string[]]$Lines)
    $input = Write-Input $Dir "entrada.txt" $Lines
    return Invoke-App $Dir $input
}

try {
    New-Item -ItemType Directory -Path (Join-Path $temp "bin") -Force | Out-Null
    Write-Host "Compilando..."
    & javac -d (Join-Path $temp "bin") (Get-ChildItem (Join-Path $root "src\*.java")).FullName
    if ($LASTEXITCODE -ne 0) { throw "No se pudo compilar src\*.java" }
    Write-Host "OK: compilacion"

    # ------------------------------------------------------------------
    # Catalogo, naturalezas y archivo de 5 campos
    # ------------------------------------------------------------------
    $dir = New-TestDir "catalogo"
    $out = Invoke-Steps $dir @("1", "1", "0", "11")
    Assert-Contains $out "=== CATALOGO DE CUENTAS ===" "catalogo visible"
    Assert-Contains $out "Naturaleza" "columna naturaleza"
    Assert-Match $out "1000\s+Caja\s+Activo\s+Deudora" "1000 Deudora"
    Assert-Match $out "1790\s+Depreciacion acumulada\s+Activo\s+Acreedora" "1790 Acreedora"
    Assert-Match $out "3120\s+Perdida del ejercicio\s+Patrimonio\s+Deudora" "3120 Deudora"
    Assert-Match $out "3200\s+Resultados de ejercicios anteriores\s+Patrimonio\s+Mixta" "3200 Mixta"
    $cuentas = Get-Content (Join-Path $dir "data\cuentas.txt") -Raw
    Assert-Contains $cuentas "1000|Caja|Activo|Deudora|0.00" "1000 persistida con naturaleza"
    Assert-Contains $cuentas "1790|Depreciacion acumulada|Activo|Acreedora|0.00" "1790 persistida Acreedora"
    Assert-Contains $cuentas "3200|Resultados de ejercicios anteriores|Patrimonio|Mixta|0.00" "3200 persistida Mixta"
    $lineas = (Get-Content (Join-Path $dir "data\cuentas.txt")).Count
    if ($lineas -ne 75) { throw "FALLO: el catalogo debe tener 75 cuentas, tiene $lineas." }
    Write-Host "OK: catalogo de 75 cuentas"
    if (-not (Test-Path (Join-Path $dir "data\asientos.txt"))) { throw "FALLO: no se creo asientos.txt vacio" }
    Write-Host "OK: asientos.txt vacio creado"

    # ------------------------------------------------------------------
    # Busqueda inteligente de cuentas
    # ------------------------------------------------------------------
    $dir = New-TestDir "busqueda"
    $perdida = "P" + [char]0x00C9 + "RDIDA"
    $depreciacion = "depreciaci" + [char]0x00F3 + "n"
    $out = Invoke-Steps $dir @(
        "1",
        "3", "inventarios",
        "3", "depreciacion", "1",
        "3", $perdida,
        "3", "activo acreedora", "1",
        "3", "zzz", "caja",
        "3", "?", "0",
        "3", "depreciacion", "V", "inventarios",
        "3", "depreciacion", "9", "1",
        "3", "activo", "caja",
        "3", "13", "1",
        "0", "11"
    )
    Assert-Contains $out "Seleccionada: 1300 - Inventarios (Activo, Deudora)." "busqueda por nombre unico"
    Assert-Contains $out "1. 1790 - Depreciacion acumulada (Activo, Acreedora)" "lista de coincidencias 1790"
    Assert-Contains $out "2. 6080 - Depreciacion (Gasto, Deudora)" "lista de coincidencias 6080"
    Assert-Contains $out "Seleccionada: 3120 - Perdida del ejercicio (Patrimonio, Deudora)." "busqueda con tilde y mayusculas"
    Assert-Contains $out "1. 1790 - Depreciacion acumulada (Activo, Acreedora)" "busqueda por tipo y naturaleza"
    Assert-Contains $out "2. 1890 - Amortizacion acumulada (Activo, Acreedora)" "segunda cuenta acreedora"
    Assert-Contains $out "La cuenta no existe." "busqueda sin coincidencias"
    Assert-Contains $out "=== CATALOGO DE CUENTAS ===" "catalogo bajo demanda"
    Assert-Contains $out "Hay 22 coincidencias." "limite de coincidencias"
    Assert-Contains $out "Opcion invalida." "seleccion fuera de rango"
    Assert-Contains $out "Seleccionada: 1300 - Inventarios (Activo, Deudora)." "busqueda por codigo parcial"
    Assert-NotContains $out "PARTIDA RECHAZADA" "busqueda sin rechazos contables"

    # ------------------------------------------------------------------
    # Montos: formatos validos y rechazos estrictos
    # ------------------------------------------------------------------
    $dir = New-TestDir "montos"
    $out = Invoke-Steps $dir @(
        "2", "05/11/2026", "Aporte capital", "1000", "1", "100000", "3000", "2", "100000", "N", "S",
        "2", "06/11/2026", "Prueba montos", "1000", "1", "1.800", "1.234", "0.001", "1e3", "-5", "0", "NaN", "Infinity", "0,01", "3000", "2", "0,01", "N", "S",
        "2", "07/11/2026", "Formato miles punto", "1000", "1", "1.800,50", "3000", "2", "1.800,50", "N", "S",
        "2", "08/11/2026", "Formato miles coma", "1000", "1", "1,800.50", "3000", "2", "1,800.50", "N", "S",
        "2", "09/11/2026", "Formato un decimal", "1000", "1", "1800,5", "3000", "2", "1800,5", "N", "S",
        "11"
    )
    Assert-Count $out "Monto invalido." 8 "ocho montos invalidos rechazados"
    $asientos = Get-Content (Join-Path $dir "data\asientos.txt") -Raw
    Assert-Contains $asientos "PARTIDA|1000|Debito|0.01" "monto minimo 0.01"
    Assert-Contains $asientos "ASIENTO|07/11/2026|Formato miles punto|1800.50" "formato 1.800,50"
    Assert-Contains $asientos "ASIENTO|08/11/2026|Formato miles coma|1800.50" "formato 1,800.50"
    Assert-Contains $asientos "ASIENTO|09/11/2026|Formato un decimal|1800.50" "formato 1800,5"
    Assert-NotContains $asientos "0.001" "no se guarda 0.001"
    Assert-NotContains $asientos "1e3" "no se guarda notacion cientifica"
    Assert-NotContains $asientos "1.800" "no se guarda monto ambiguo"
    $out = Invoke-Steps $dir @("4", "11")
    Assert-NotContains $out "ADVERTENCIA" "recarga sin advertencias"
    Assert-Match $out "TOTAL GENERAL\s+105401,51\s+105401,51" "totales tras recarga"

    # ------------------------------------------------------------------
    # Fechas: formato, fecha real, rango y Enter = hoy
    # ------------------------------------------------------------------
    $dir = New-TestDir "fechas"
    $hoy = Get-Date -Format "dd/MM/yyyy"
    $out = Invoke-Steps $dir @(
        "2", "31/02/2026", "29/02/2025", "31/04/2026", "1/1/2026", "31/12/1899", "01/01/2101", "29/02/2024", "Prueba fecha", "1000", "1", "100", "3000", "2", "100", "N", "S",
        "2", "", "Fecha de hoy", "1000", "1", "50", "3000", "2", "50", "N", "S",
        "11"
    )
    Assert-Count $out "Fecha invalida." 6 "fechas invalidas rechazadas"
    $asientos = Get-Content (Join-Path $dir "data\asientos.txt") -Raw
    Assert-Contains $asientos "ASIENTO|29/02/2024|Prueba fecha|100.00" "fecha bisiesta valida"
    Assert-Contains $asientos "ASIENTO|$hoy|Fecha de hoy|50.00" "Enter usa la fecha actual"
    $out = Invoke-Steps $dir @("4", "11")
    Assert-NotContains $out "ADVERTENCIA" "recarga de fechas sin advertencias"

    # ------------------------------------------------------------------
    # Partida doble
    # ------------------------------------------------------------------
    $dir = New-TestDir "partida_doble"
    $out = Invoke-Steps $dir @(
        "2", "05/11/2026", "Solo debe", "1000", "1", "100", "0",
        "2", "05/11/2026", "Solo haber", "3000", "2", "100", "0",
        "2", "05/11/2026", "Descuadre", "1000", "1", "100", "3000", "2", "99.99", "N",
        "2", "05/11/2026", "Descuadre2", "1000", "1", "100", "3000", "2", "100.01", "N",
        "2", "05/11/2026", "Cuadrado", "1000", "1", "100", "3000", "2", "100", "N", "S",
        "11"
    )
    Assert-Count $out "Faltan partidas" 5 "minimo dos partidas"
    Assert-Count $out "El asiento no cuadra todavia." 2 "descuadres rechazados"
    Assert-Count $out "Diferencia 0,01." 2 "diferencia de un centavo"
    $asientos = Get-Content (Join-Path $dir "data\asientos.txt") -Raw
    Assert-Contains $asientos "ASIENTO|05/11/2026|Cuadrado|100.00" "asiento cuadrado persistido"
    Assert-NotContains $asientos "Solo debe" "solo debe no persistido"
    Assert-NotContains $asientos "Solo haber" "solo haber no persistido"
    Assert-NotContains $asientos "Descuadre" "descuadres no persistidos"

    # ------------------------------------------------------------------
    # Naturalezas: inventario 20.000/60.000 y otros saldos contrarios
    # ------------------------------------------------------------------
    $dir = New-TestDir "naturaleza"
    $out = Invoke-Steps $dir @(
        "2", "01/11/2026", "Aporte", "1000", "1", "100000", "3000", "2", "100000", "N", "S",
        "2", "02/11/2026", "Compra inventario", "1300", "1", "20000", "2000", "2", "20000", "N", "S",
        "2", "03/11/2026", "Inventario sobregirado", "1000", "1", "60000", "1300", "2", "60000", "0",
        "2", "04/11/2026", "Caja sobregirada", "1300", "1", "200000", "1000", "2", "200000", "0",
        "2", "05/11/2026", "Proveedor sobregirado", "2000", "1", "30000", "1000", "2", "30000", "0",
        "2", "06/11/2026", "Venta", "1210", "1", "5000", "4010", "2", "5000", "N", "S",
        "2", "07/11/2026", "Venta sobregirada", "4010", "1", "6000", "1000", "2", "6000", "0",
        "2", "08/11/2026", "Costo", "5010", "1", "3000", "1300", "2", "3000", "N", "S",
        "2", "09/11/2026", "Costo sobregirado", "1000", "1", "4000", "5010", "2", "4000", "0",
        "2", "10/11/2026", "Renta", "6060", "1", "1000", "1000", "2", "1000", "N", "S",
        "2", "11/11/2026", "Gasto sobregirado", "1000", "1", "2000", "6060", "2", "2000", "0",
        "3", "1300", "3", "2000", "3", "4010", "3", "5010", "3", "6060", "8",
        "11"
    )
    Assert-Count $out "PARTIDA RECHAZADA" 6 "seis partidas con saldo contrario rechazadas"
    Assert-Contains $out "Saldo actual: 20000,00 (Deudor)." "saldo actual del inventario"
    Assert-Count $out "Saldo resultante: 40000,00 (Acreedor)." 1 "inventario sobregirado rechazado"
    Assert-Count $out "Saldo resultante: 100000,00 (Acreedor)." 1 "caja sobregirada rechazada"
    Assert-Contains $out "Saldo actual: 20000,00 (Acreedor)." "saldo actual del proveedor"
    Assert-Contains $out "Saldo resultante: 10000,00 (Deudor)." "proveedor rechazado"
    Assert-Contains $out "Saldo actual: 5000,00 (Acreedor)." "saldo actual de ventas"
    Assert-Contains $out "Saldo resultante: 1000,00 (Deudor)." "venta rechazada"
    Assert-Contains $out "Saldo actual: 3000,00 (Deudor)." "saldo actual del costo"
    Assert-Contains $out "Saldo actual: 1000,00 (Deudor)." "saldo actual del gasto"
    Assert-Count $out "Saldo resultante: 1000,00 (Acreedor)." 2 "costo y gasto rechazados"
    Assert-Contains $out "SALDO: 17000,00 (Deudor)" "inventario final"
    Assert-Contains $out "SALDO: 20000,00 (Acreedor)" "proveedores final"
    Assert-Contains $out "SALDO: 5000,00 (Acreedor)" "ventas final"
    Assert-Contains $out "SALDO: 3000,00 (Deudor)" "costo final"
    Assert-Contains $out "SALDO: 1000,00 (Deudor)" "gasto final"
    Assert-Match $out "TOTAL ACTIVO:\s+121000,00" "activo final"
    Assert-Match $out "TOTAL PASIVO:\s+20000,00" "pasivo final"
    Assert-Match $out "TOTAL PATRIMONIO:\s+100000,00" "patrimonio final"
    Assert-Match $out "Resultado acumulado:\s+1000,00" "resultado final"
    Assert-Contains $out "Total Pasivo + Patrimonio + Resultado: 121000,00" "balance final"
    Assert-Contains $out "EL BALANCE CUADRA CORRECTAMENTE." "balance cuadrado"
    $asientos = Get-Content (Join-Path $dir "data\asientos.txt") -Raw
    Assert-NotContains $asientos "sobregirad" "intentos rechazados no persistidos"
    Assert-Contains $asientos "ASIENTO|02/11/2026|Compra inventario|20000.00" "compra valida persistida"

    # ------------------------------------------------------------------
    # Cuentas personalizadas: Activo/Acreedora e Ingreso/Deudora
    # ------------------------------------------------------------------
    $dir = New-TestDir "personalizadas"
    $out = Invoke-Steps $dir @(
        "1", "2", "Depreciacion vehiculos", "1", "2", "1791",
        "2", "Devoluciones sobre ventas", "4", "1", "4080", "0",
        "2", "01/11/2026", "Aporte", "1000", "1", "100000", "3000", "2", "100000", "N", "S",
        "2", "02/11/2026", "Compra vehiculo", "1420", "1", "20000", "1000", "2", "20000", "N", "S",
        "2", "03/11/2026", "Depreciacion vehiculo", "6080", "1", "1000", "1791", "2", "1000", "N", "S",
        "3", "1791", "5", "1791",
        "2", "04/11/2026", "Depreciacion sobregirada", "1791", "1", "1500", "1000", "2", "1500", "0",
        "2", "05/11/2026", "Venta", "1210", "1", "1000", "4010", "2", "1000", "N", "S",
        "2", "06/11/2026", "Devolucion", "4080", "1", "200", "1000", "2", "200", "N", "S",
        "3", "4080", "7",
        "2", "07/11/2026", "Devolucion sobregirada", "4080", "2", "300", "1000", "1", "300", "0",
        "2", "08/11/2026", "Reverso depreciacion", "1791", "1", "1000", "1000", "2", "1000", "N", "S",
        "2", "09/11/2026", "Reverso devolucion", "4080", "2", "200", "1000", "1", "200", "N", "S",
        "3", "1791", "3", "4080",
        "11"
    )
    Assert-Contains $out "Cuenta creada: 1791 - Depreciacion vehiculos (Activo, Acreedora)." "cuenta personalizada acreedora"
    Assert-Contains $out "Cuenta creada: 4080 - Devoluciones sobre ventas (Ingreso, Deudora)." "cuenta personalizada deudora"
    Assert-Contains $out "SALDO: 1000,00 (Acreedor)" "depreciacion personalizada con saldo acreedor"
    Assert-Contains $out "SALDO: 200,00 (Deudor)" "devoluciones con saldo deudor"
    Assert-Count $out "PARTIDA RECHAZADA" 2 "dos rechazos personalizados"
    Assert-Contains $out "Saldo resultante: 500,00 (Deudor)." "depreciacion personalizada rechazada"
    Assert-Contains $out "Saldo resultante: 100,00 (Acreedor)." "devolucion personalizada rechazada"
    $cuentas = Get-Content (Join-Path $dir "data\cuentas.txt") -Raw
    Assert-Match $cuentas "1791\|Depreciacion vehiculos\|Activo\|Acreedora\|0\.00" "naturaleza personalizada persistida"
    Assert-Match $cuentas "4080\|Devoluciones sobre ventas\|Ingreso\|Deudora\|0\.00" "contra ingreso persistido"
    $asientos = Get-Content (Join-Path $dir "data\asientos.txt") -Raw
    Assert-NotContains $asientos "sobregirad" "rechazos personalizados no persistidos"
    $out = Invoke-Steps $dir @("3", "1791", "3", "4080", "11")
    Assert-Contains $out "Naturaleza: Acreedora" "naturaleza acreedora tras reinicio"
    Assert-Contains $out "Naturaleza: Deudora" "naturaleza deudora tras reinicio"

    # ------------------------------------------------------------------
    # Cuentas Mixtas: 3200 estandar y 3201 personalizada
    # ------------------------------------------------------------------
    $dir = New-TestDir "mixta"
    $out = Invoke-Steps $dir @(
        "1", "2", "Resultados especiales", "3", "3", "3201", "0",
        "2", "01/11/2026", "Aporte", "1000", "1", "100000", "3000", "2", "100000", "N", "S",
        "2", "02/11/2026", "Debito 3200", "3200", "1", "1000", "1000", "2", "1000", "N", "S",
        "2", "03/11/2026", "Credito 3200", "1000", "1", "1500", "3200", "2", "1500", "N", "S",
        "2", "04/11/2026", "Debito 3201", "3201", "1", "1000", "1000", "2", "1000", "N", "S",
        "2", "05/11/2026", "Credito 3201", "1000", "1", "1500", "3201", "2", "1500", "N", "S",
        "3", "3200", "3", "3201",
        "11"
    )
    Assert-NotContains $out "PARTIDA RECHAZADA" "cuentas mixtas no se bloquean"
    Assert-Contains $out "SALDO: 500,00 (Acreedor)" "saldo de cuenta mixta"
    Assert-Match $out "3200 - Resultados de ejercicios anteriores" "cuenta 3200 mixta"
    Assert-Match $out "3201 - Resultados especiales" "cuenta 3201 mixta"
    $cuentas = Get-Content (Join-Path $dir "data\cuentas.txt") -Raw
    Assert-Match $cuentas "3201\|Resultados especiales\|Patrimonio\|Mixta\|500\.00" "mixta personalizada persistida"

    # ------------------------------------------------------------------
    # Depreciacion y amortizacion estandar, reportes con valores exactos
    # ------------------------------------------------------------------
    $dir = New-TestDir "depreciacion"
    $out = Invoke-Steps $dir @(
        "2", "01/11/2026", "Aporte", "1000", "1", "100000", "3000", "2", "100000", "N", "S",
        "2", "02/11/2026", "Compra equipo", "1410", "1", "30000", "1000", "2", "30000", "N", "S",
        "2", "03/11/2026", "Compra intangible", "1800", "1", "5000", "1000", "2", "5000", "N", "S",
        "2", "04/11/2026", "Depreciacion", "6080", "1", "1000", "1790", "2", "1000", "N", "S",
        "2", "05/11/2026", "Amortizacion", "6090", "1", "500", "1890", "2", "500", "N", "S",
        "3", "1790", "3", "1890", "5", "1790", "4", "6", "7", "8",
        "11"
    )
    Assert-Contains $out "SALDO: 1000,00 (Acreedor)" "depreciacion acumulada acreedora"
    Assert-Contains $out "SALDO: 500,00 (Acreedor)" "amortizacion acumulada acreedora"
    Assert-Contains $out "Saldo final: 1000,00 (Acreedor)" "libro mayor de depreciacion"
    Assert-Match $out "TOTAL GENERAL\s+136500,00\s+136500,00" "totales del libro diario"
    Assert-Match $out "TOTALES:\s+136500,00\s+136500,00" "totales de la balanza"
    Assert-Match $out "Utilidad bruta:\s+0,00" "utilidad bruta sin ingresos"
    Assert-Match $out "Utilidad neta:\s+-1500,00" "perdida por depreciacion y amortizacion"
    Assert-Contains $out "Resultado:       1500,00 (Perdida)" "resultado negativo"
    Assert-Match $out "TOTAL ACTIVO:\s+98500,00" "activo neto de valuacion"
    Assert-Match $out "TOTAL PASIVO:\s+0,00" "pasivo cero"
    Assert-Match $out "TOTAL PATRIMONIO:\s+100000,00" "patrimonio"
    Assert-Match $out "Resultado acumulado:\s+-1500,00" "resultado acumulado negativo"
    Assert-Contains $out "Total Pasivo + Patrimonio + Resultado: 98500,00" "balance cuadrado"
    Assert-Contains $out "EL BALANCE CUADRA CORRECTAMENTE." "balance de depreciacion cuadrado"
    Assert-Match $out "1790\s+Depreciacion acumulada\s+-1000,00" "1790 resta del activo"
    Assert-Match $out "1890\s+Amortizacion acumulada\s+-500,00" "1890 resta del activo"
    $balance = Get-Content (Join-Path $dir "data\balance.txt") -Raw
    Assert-Match $balance "^\d{2}/\d{2}/\d{4}\|98500\.00\|0\.00\|98500\.00" "balance.txt de depreciacion"

    # ------------------------------------------------------------------
    # Ciclo contable completo con valores exactos
    # ------------------------------------------------------------------
    $dir = New-TestDir "ciclo"
    $out = Invoke-Steps $dir @(
        "2", "01/11/2026", "Aporte", "1000", "1", "100000", "3000", "2", "100000", "N", "S",
        "2", "02/11/2026", "Compra inventario", "1300", "1", "20000", "2000", "2", "20000", "N", "S",
        "2", "03/11/2026", "Venta", "1210", "1", "35000", "4010", "2", "35000", "N", "S",
        "2", "04/11/2026", "Costo de venta", "5010", "1", "12000", "1300", "2", "12000", "N", "S",
        "2", "05/11/2026", "Renta", "6060", "1", "2000", "1000", "2", "2000", "N", "S",
        "2", "06/11/2026", "Cobro clientes", "1000", "1", "20000", "1210", "2", "20000", "N", "S",
        "2", "07/11/2026", "Pago proveedores", "2000", "1", "10000", "1000", "2", "10000", "N", "S",
        "4", "6", "7", "8", "3", "1300", "5", "1000",
        "11"
    )
    Assert-NotContains $out "PARTIDA RECHAZADA" "ciclo sin rechazos"
    Assert-NotContains $out "ADVERTENCIA" "ciclo sin advertencias"
    Assert-Match $out "TOTAL GENERAL\s+199000,00\s+199000,00" "libro diario del ciclo"
    Assert-Match $out "TOTALES:\s+199000,00\s+199000,00" "balanza del ciclo"
    Assert-Match $out "Utilidad bruta:\s+23000,00" "utilidad bruta del ciclo"
    Assert-Match $out "Utilidad neta:\s+21000,00" "utilidad neta del ciclo"
    Assert-Contains $out "Resultado:       21000,00 (Utilidad)" "utilidad del ciclo"
    Assert-Match $out "TOTAL ACTIVO:\s+131000,00" "activo del ciclo"
    Assert-Match $out "TOTAL PASIVO:\s+10000,00" "pasivo del ciclo"
    Assert-Match $out "TOTAL PATRIMONIO:\s+100000,00" "patrimonio del ciclo"
    Assert-Match $out "Resultado acumulado:\s+21000,00" "resultado del ciclo"
    Assert-Contains $out "Total Pasivo + Patrimonio + Resultado: 131000,00" "balance del ciclo"
    Assert-Contains $out "Diferencia: 0,00" "diferencia cero"
    Assert-Contains $out "EL BALANCE CUADRA CORRECTAMENTE." "balance del ciclo cuadrado"
    Assert-Contains $out "SALDO: 8000,00 (Deudor)" "inventario final del ciclo"
    Assert-Contains $out "Saldo final: 108000,00 (Deudor)" "caja final del ciclo"
    $balance = Get-Content (Join-Path $dir "data\balance.txt") -Raw
    Assert-Match $balance "^\d{2}/\d{2}/\d{4}\|131000\.00\|10000\.00\|121000\.00" "balance.txt del ciclo"

    # ------------------------------------------------------------------
    # Reportes con datos vacios
    # ------------------------------------------------------------------
    $dir = New-TestDir "vacio"
    $out = Invoke-Steps $dir @("3", "1000", "4", "5", "1000", "6", "7", "8", "11")
    Assert-Contains $out "Esta cuenta no tiene movimientos." "cuenta T vacia"
    Assert-Contains $out "SALDO: 0,00 (Cero)" "saldo cero en cuenta T"
    Assert-Contains $out "No hay asientos registrados." "libro diario vacio"
    Assert-Contains $out "Esta cuenta no tiene movimientos." "libro mayor vacio"
    Assert-Contains $out "LA BALANZA DE COMPROBACION ESTA CUADRADA." "balanza vacia cuadrada"
    Assert-Match $out "TOTALES:\s+0,00\s+0,00\s+0,00\s+0,00" "totales vacios"
    Assert-Match $out "Utilidad bruta:\s+0,00" "utilidad bruta vacia"
    Assert-Match $out "Utilidad neta:\s+0,00" "utilidad neta vacia"
    Assert-Contains $out "Resultado:       0,00 (Cero)" "resultado vacio"
    Assert-Contains $out "Total Pasivo + Patrimonio + Resultado: 0,00" "balance vacio"
    Assert-Contains $out "EL BALANCE CUADRA CORRECTAMENTE." "balance vacio cuadrado"
    $balance = Get-Content (Join-Path $dir "data\balance.txt") -Raw
    Assert-Match $balance "^\d{2}/\d{2}/\d{4}\|0\.00\|0\.00\|0\.00" "balance.txt vacio"

    # ------------------------------------------------------------------
    # Migracion de cuentas.txt de 4 a 5 campos
    # ------------------------------------------------------------------
    $dir = New-TestDir "migracion"
    $cuentasViejas = @(
        "1000|Caja|Activo|0.00",
        "1790|Depreciacion acumulada|Activo|0.00",
        "3120|Perdida del ejercicio|Patrimonio|0.00",
        "3200|Resultados de ejercicios anteriores|Patrimonio|0.00",
        "3500|Dividendos decretados|Patrimonio|0.00",
        "2000|Cuentas por pagar|Pasivo|0.00",
        "3000|Capital social|Patrimonio|0.00",
        "1010|Caja chica|Activo|0.00"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dir "data\cuentas.txt") ($cuentasViejas + "`r`n")
    Write-Utf8 (Join-Path $dir "data\asientos.txt") ""
    $out = Invoke-Steps $dir @("11")
    Assert-NotContains $out "ADVERTENCIA" "migracion sin advertencias"
    $cuentas = Get-Content (Join-Path $dir "data\cuentas.txt") -Raw
    Assert-Contains $cuentas "1000|Caja|Activo|Deudora|0.00" "migracion 1000"
    Assert-Contains $cuentas "1790|Depreciacion acumulada|Activo|Acreedora|0.00" "migracion 1790"
    Assert-Contains $cuentas "3120|Perdida del ejercicio|Patrimonio|Deudora|0.00" "migracion 3120"
    Assert-Contains $cuentas "3200|Resultados de ejercicios anteriores|Patrimonio|Mixta|0.00" "migracion 3200"
    Assert-Contains $cuentas "3500|Dividendos decretados|Patrimonio|Deudora|0.00" "migracion 3500"
    Assert-Contains $cuentas "2000|Cuentas por pagar|Pasivo|Acreedora|0.00" "migracion 2000"
    Assert-Contains $cuentas "1010|Caja chica|Activo|Deudora|0.00" "migracion cuenta personalizada"
    $lineas = (Get-Content (Join-Path $dir "data\cuentas.txt")).Count
    if ($lineas -ne 8) { throw "FALLO: la migracion debe conservar 8 cuentas, tiene $lineas." }
    Write-Host "OK: migracion conserva las cuentas"

    # ------------------------------------------------------------------
    # Naturaleza invalida en cuentas.txt: no se sobrescribe
    # ------------------------------------------------------------------
    $dir = New-TestDir "naturaleza_invalida"
    $contenido = "1000|Caja|Activo|Rara|0.00`r`n3000|Capital social|Patrimonio|Acreedora|0.00`r`n"
    Write-Utf8 (Join-Path $dir "data\cuentas.txt") $contenido
    Write-Utf8 (Join-Path $dir "data\asientos.txt") ""
    $antes = Get-Content (Join-Path $dir "data\cuentas.txt") -Raw
    $out = Invoke-Steps $dir @("11")
    Assert-Contains $out "naturaleza invalida" "aviso de naturaleza invalida"
    Assert-Contains $out "Linea omitida." "linea de cuenta omitida"
    Assert-Contains $out "No se guardaron cambios: hay datos invalidos" "no se guarda con naturaleza invalida"
    $despues = Get-Content (Join-Path $dir "data\cuentas.txt") -Raw
    if ($antes -ne $despues) { throw "FALLO: cuentas.txt cambio con una naturaleza invalida." }
    Write-Host "OK: cuentas.txt no se sobrescribe"
    Assert-Contains $despues "Rara" "la linea invalida permanece"

    # ------------------------------------------------------------------
    # Asientos invalidos: saldo contrario, fecha y monto; no se sobrescribe
    # ------------------------------------------------------------------
    $dir = New-TestDir "asientos_invalidos"
    $contenido = @(
        "ASIENTO|01/11/2026|Aporte|10000.00",
        "PARTIDA|1000|Debito|10000.00",
        "PARTIDA|3000|Credito|10000.00",
        "ASIENTO|02/11/2026|Inventario sobregirado|60000.00",
        "PARTIDA|1300|Debito|20000.00",
        "PARTIDA|1300|Credito|60000.00",
        "PARTIDA|3000|Debito|40000.00",
        "ASIENTO|31/12/1899|Fecha fuera de rango|100.00",
        "PARTIDA|1000|Debito|100.00",
        "PARTIDA|3000|Credito|100.00",
        "ASIENTO|03/11/2026|Monto con tres decimales|0.001",
        "PARTIDA|1000|Debito|0.001",
        "PARTIDA|3000|Credito|0.001"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dir "data\asientos.txt") ($contenido + "`r`n")
    $antes = Get-Content (Join-Path $dir "data\asientos.txt") -Raw
    $out = Invoke-Steps $dir @("2", "11")
    Assert-Contains $out "saldo contrario" "aviso de saldo contrario al cargar"
    Assert-Contains $out "fecha invalida" "aviso de fecha invalida al cargar"
    Assert-Contains $out "monto invalido" "aviso de monto invalido al cargar"
    Assert-Contains $out "No se pueden registrar asientos" "datos invalidos bloquean registro"
    $despues = Get-Content (Join-Path $dir "data\asientos.txt") -Raw
    if ($antes -ne $despues) { throw "FALLO: asientos.txt cambio al cargar asientos invalidos." }
    Write-Host "OK: asientos.txt no se sobrescribe"

    # ------------------------------------------------------------------
    # cuentas.txt vacio: catalogo en memoria y sin sobrescritura
    # ------------------------------------------------------------------
    $dir = New-TestDir "cuentas_vacias"
    Write-Utf8 (Join-Path $dir "data\cuentas.txt") ""
    Write-Utf8 (Join-Path $dir "data\asientos.txt") ""
    $out = Invoke-Steps $dir @("11")
    Assert-Contains $out "no hay cuentas validas" "aviso de catalogo vacio"
    Assert-Contains $out "no se guardara hasta corregir el archivo" "catalogo vacio no se guarda"
    $tamano = (Get-Item (Join-Path $dir "data\cuentas.txt")).Length
    if ($tamano -ne 0) { throw "FALLO: cuentas.txt vacio fue sobrescrito." }
    Write-Host "OK: cuentas.txt vacio permanece intacto"

    # ------------------------------------------------------------------
    # Fin de entrada y caracter pipe
    # ------------------------------------------------------------------
    $dir = New-TestDir "eof1"
    $out = Invoke-Steps $dir @("2")
    Assert-Contains $out "No hay mas entrada. El programa se cerrara." "EOF en fecha"
    Assert-Contains $out "Asiento cancelado." "EOF cancela asiento"
    Assert-Contains $out "Hasta luego." "EOF cierra el programa"

    $dir = New-TestDir "eof2"
    $out = Invoke-Steps $dir @("3")
    Assert-Contains $out "Consulta cancelada." "EOF cancela consulta"
    Assert-Contains $out "Hasta luego." "EOF cierra tras consulta"

    $dir = New-TestDir "eof3"
    $out = Invoke-Steps $dir @("2", "05/11/2026", "Prueba", "1000", "1")
    Assert-Contains $out "No hay mas entrada. El programa se cerrara." "EOF en monto"
    Assert-Contains $out "Asiento cancelado." "EOF en monto cancela asiento"
    Assert-Contains $out "Hasta luego." "EOF en monto cierra el programa"

    $dir = New-TestDir "pipe"
    $out = Invoke-Steps $dir @(
        "1", "2", "Caja|chica", "Caja chica", "1", "1", "1010", "0",
        "2", "05/11/2026", "Compra|papel", "Compra papel", "0",
        "11"
    )
    Assert-Count $out "no puede contener el caracter |" 2 "pipe rechazado en nombre y descripcion"
    Assert-Contains $out "Cuenta creada: 1010 - Caja chica (Activo, Deudora)." "cuenta creada tras corregir pipe"
    $cuentas = Get-Content (Join-Path $dir "data\cuentas.txt") -Raw
    Assert-Contains $cuentas "1010|Caja chica|Activo|Deudora|0.00" "cuenta sin pipe persistida"
    $asientos = Get-Content (Join-Path $dir "data\asientos.txt") -Raw
    Assert-NotContains $asientos "Compra|papel" "descripcion con pipe no persistida"

    # ------------------------------------------------------------------
    # Respaldo manual
    # ------------------------------------------------------------------
    $dir = New-TestDir "respaldo"
    Write-Utf8 (Join-Path $dir "data\asientos.txt") ""
    $out = Invoke-Steps $dir @("10", "11")
    Assert-Contains $out "Respaldo creado en: backup\manual_" "respaldo manual creado"
    $carpeta = Get-ChildItem (Join-Path $dir "backup") -Directory -Filter "manual_*" | Select-Object -First 1
    if ($null -eq $carpeta) { throw "FALLO: no se creo la carpeta de respaldo manual" }
    foreach ($archivo in @("cuentas.txt", "asientos.txt")) {
        if (-not (Test-Path (Join-Path $carpeta.FullName $archivo))) { throw "FALLO: el respaldo no contiene $archivo" }
    }
    Write-Host "OK: respaldo manual completo"

    Write-Host ""
    Write-Host "TODAS LAS PRUEBAS DE LOGICA CONTABLE PASARON."
} finally {
    if ($Keep) { Write-Host "Directorio de pruebas: $temp" }
    else { Remove-Item $temp -Recurse -Force -ErrorAction SilentlyContinue }
}
