param(
    [switch]$Keep
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$temp = Join-Path ([System.IO.Path]::GetTempPath()) ("contab_pruebas_" + [Guid]::NewGuid().ToString("N"))
$acento = [char]0x00F3

function Write-Utf8 {
    param([string]$Path, [string]$Content)
    [System.IO.File]::WriteAllText($Path, $Content, [System.Text.UTF8Encoding]::new($false))
}

function Assert-Contains {
    param([string]$Text, [string]$Expected, [string]$Name)
    if (-not $Text.Contains($Expected)) {
        throw "FALLO: $Name. No se encontro: $Expected"
    }
    Write-Host "OK: $Name"
}

function New-TestDir {
    param([string]$Name)
    $dir = Join-Path $temp $Name
    New-Item -ItemType Directory -Path (Join-Path $dir "data") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $dir "backup") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $dir "bin") -Force | Out-Null
    Copy-Item (Join-Path $temp "bin\Contabilidad.class") (Join-Path $dir "bin\Contabilidad.class") -Force
    return $dir
}

function Invoke-App {
    param([string]$Dir, [string]$InputFile)
    Push-Location $Dir
    try {
        cmd /c "java -Dfile.encoding=UTF-8 -cp bin Contabilidad < $InputFile > output.txt 2>&1"
        if ($LASTEXITCODE -ne 0) {
            throw "Java termino con codigo $LASTEXITCODE en $Dir"
        }
        return [System.IO.File]::ReadAllText((Join-Path $Dir "output.txt"))
    } finally {
        Pop-Location
    }
}

function Write-Input {
    param([string]$Dir, [string]$Name, [string[]]$Lines)
    $path = Join-Path $Dir $Name
    Write-Utf8 $path (($Lines -join "`r`n") + "`r`n")
    return $path
}

try {
    New-Item -ItemType Directory -Path (Join-Path $temp "bin") -Force | Out-Null

    Write-Host "Compilando..."
    & javac -d (Join-Path $temp "bin") (Join-Path $root "src\Contabilidad.java")
    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo compilar src\Contabilidad.java"
    }
    Write-Host "OK: compilacion"

    # ------------------------------------------------------------------
    # Flujo completo: cuentas, asientos, reportes, busqueda y respaldo
    # ------------------------------------------------------------------
    $dir = New-TestDir "flujo"
    $cuentas = @(
        "1000|Caja|Activo|0.0",
        "1100|Bancos|Activo|0.0",
        ("1790|Depreciaci" + $acento + "n acumulada|Activo|0.0"),
        "4000|Ingresos|Ingreso|0.0",
        "4010|Ventas|Ingreso|0.0",
        "6010|Gastos de venta|Gasto|0.0",
        "A100|Cuenta especial|Activo|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dir "data\cuentas.txt") ($cuentas + "`r`n")

    $asientos = @(
        "ASIENTO|01/01/2026|Venta de mercaderia|1000.0",
        "PARTIDA|1000|Debito|1000.0",
        "PARTIDA|4010|Credito|1000.0",
        "ASIENTO|15/01/2026|Gasto de renta|200.0",
        "PARTIDA|6010|Debito|200.0",
        "PARTIDA|1000|Credito|200.0",
        "ASIENTO|01/02/2026|Venta de servicios|500.0",
        "PARTIDA|1100|Debito|500.0",
        "PARTIDA|4010|Credito|500.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dir "data\asientos.txt") ($asientos + "`r`n")

    $input = Write-Input $dir "input.txt" @(
        "1", "3", "depreciacion", "0",
        "1", "2", "Cuenta|mala", "Cuenta buena", "1", "0", "1020", "0",
        "2", "", "Prueba de registro", "1000", "1", "1.800,50", "4010", "2", "1800,50", "N", "S",
        "2", "", "Prueba descuadre", "1000", "1", "100", "4010", "2", "50", "S", "4010", "2", "50", "N", "S",
        "2", "", "Prueba cancelada", "1000", "1", "300", "4010", "2", "300", "N", "N",
        "2", "", "Venta|mala", "Venta buena", "1000", "1", "100", "4010", "2", "100", "N", "S",
        "3", "1", "1000", "0",
        "4",
        "5", "1000",
        "6",
        "7",
        "8",
        "9", "1", "2", "2", "renta", "0",
        "10",
        "11"
    )
    $out = Invoke-App $dir $input

    Assert-Contains $out "1790" "busqueda de cuenta sin tilde"
    Assert-Contains $out "El campo nombre no puede contener el caracter |." "nombre con pipe rechazado"
    Assert-Contains $out "El codigo 0 esta reservado para cancelar." "codigo 0 reservado"
    Assert-Contains $out "Cuenta creada exitosamente: 1020 - Cuenta buena (Activo)." "cuenta creada"
    Assert-Contains $out "El campo descripcion no puede contener el caracter |." "descripcion con pipe rechazada"
    Assert-Contains $out "Asiento registrado exitosamente." "asiento registrado"
    Assert-Contains $out "El asiento no cuadra:" "aviso de descuadre"
    Assert-Contains $out "Desea agregar otra partida para cuadrar?" "opcion para corregir descuadre"
    Assert-Contains $out "Asiento cancelado. No se guardo nada." "asiento cancelado"
    Assert-Contains $out "CUENTA T: 1000 - Caja (Activo)" "cuenta T"
    Assert-Contains $out "Total de asientos: 6" "libro diario"
    Assert-Contains $out "Codigo Cuenta" "libro diario detallado"
    Assert-Contains $out "TOTAL GENERAL" "total general del libro diario"
    Assert-Contains $out "CUENTA: 1000 - Caja" "libro mayor"
    Assert-Contains $out "LA BALANZA DE COMPROBACION ESTA CUADRADA." "balanza cuadrada"
    Assert-Contains $out "Utilidad neta:" "estado de resultados"
    Assert-Contains $out "EL BALANCE CUADRA CORRECTAMENTE." "balance general"
    Assert-Contains $out "Gasto de renta" "busqueda de asientos"
    Assert-Contains $out "Respaldo creado en: backup\manual_" "respaldo manual"

    $contenidoCuentas = Get-Content (Join-Path $dir "data\cuentas.txt") -Raw
    if (-not $contenidoCuentas.Contains("1020|Cuenta buena|Activo")) {
        throw "FALLO: no se guardo la cuenta creada"
    }
    if ($contenidoCuentas.Contains("Cuenta|mala")) {
        throw "FALLO: se guardo un nombre con pipe"
    }
    Write-Host "OK: cuentas guardadas sin caracteres invalidos"

    $contenidoAsientos = Get-Content (Join-Path $dir "data\asientos.txt") -Raw
    if ($contenidoAsientos.Contains("Venta|mala")) {
        throw "FALLO: se guardo una descripcion con pipe"
    }
    if (-not $contenidoAsientos.Contains("ASIENTO|") -or -not $contenidoAsientos.Contains("PARTIDA|")) {
        throw "FALLO: el formato de asientos no usa el delimitador |"
    }
    $cantidadAsientos = (Get-Content (Join-Path $dir "data\asientos.txt") | Select-String -Pattern "^ASIENTO").Count
    if ($cantidadAsientos -ne 6) {
        throw "FALLO: se esperaban 6 asientos y hay $cantidadAsientos"
    }
    $cantidadPartidas = (Get-Content (Join-Path $dir "data\asientos.txt") | Select-String -Pattern "^PARTIDA").Count
    if ($cantidadPartidas -ne 13) {
        throw "FALLO: se esperaban 13 partidas y hay $cantidadPartidas"
    }
    Write-Host "OK: asientos guardados y descuadre corregido"

    $balance = Get-Content (Join-Path $dir "data\balance.txt") -Raw
    if (($balance.Trim().Split("|")).Count -ne 4) {
        throw "FALLO: balance.txt no tiene 4 campos"
    }
    Write-Host "OK: balance.txt generado"

    $respaldo = Get-ChildItem (Join-Path $dir "backup") -Directory -Filter "manual_*" | Select-Object -First 1
    if ($null -eq $respaldo) {
        throw "FALLO: no se creo la carpeta de respaldo manual"
    }
    if (-not (Test-Path (Join-Path $respaldo.FullName "cuentas.txt"))) {
        throw "FALLO: el respaldo no contiene cuentas.txt"
    }
    if (-not (Test-Path (Join-Path $respaldo.FullName "asientos.txt"))) {
        throw "FALLO: el respaldo no contiene asientos.txt"
    }
    Write-Host "OK: respaldo manual completo"

    # ------------------------------------------------------------------
    # Persistencia: volver a abrir el programa conserva los asientos
    # ------------------------------------------------------------------
    $input2 = Write-Input $dir "input2.txt" @("4", "11")
    $out2 = Invoke-App $dir $input2
    Assert-Contains $out2 "Total de asientos: 6" "persistencia de asientos"

    # ------------------------------------------------------------------
    # Validacion de asientos invalidos al cargar
    # ------------------------------------------------------------------
    $dirInvalid = New-TestDir "invalidos"
    $cuentasInvalidas = @(
        "1000|Caja|Activo|0.0",
        "4010|Ventas|Ingreso|0.0",
        "1001||Activo|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirInvalid "data\cuentas.txt") ($cuentasInvalidas + "`r`n")

    $asientosInvalidos = @(
        "ASIENTO|31/02/2026|Fecha invalida|100.0",
        "PARTIDA|1000|Debito|100.0",
        "PARTIDA|4010|Credito|100.0",
        "ASIENTO|01/01/2026|Descuadrado|100.0",
        "PARTIDA|1000|Debito|100.0",
        "PARTIDA|4010|Credito|50.0",
        "ASIENTO|01/01/2026|Cuenta inexistente|100.0",
        "PARTIDA|9999|Debito|100.0",
        "PARTIDA|4010|Credito|100.0",
        "ASIENTO|01/01/2026|Una partida|100.0",
        "PARTIDA|1000|Debito|100.0",
        "ASIENTO|01/01/2026|Valido|100.0",
        "PARTIDA|1000|Debito|100.0",
        "PARTIDA|4010|Credito|100.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirInvalid "data\asientos.txt") ($asientosInvalidos + "`r`n")

    $inputInvalid = Write-Input $dirInvalid "input.txt" @("4", "11")
    $outInvalid = Invoke-App $dirInvalid $inputInvalid

    Assert-Contains $outInvalid "nombre de cuenta vacio" "cuenta con nombre vacio rechazada"
    Assert-Contains $outInvalid "fecha invalida" "asiento con fecha invalida rechazado"
    Assert-Contains $outInvalid "no cuadra" "asiento descuadrado rechazado"
    Assert-Contains $outInvalid "la cuenta 9999 no existe" "asiento con cuenta inexistente rechazado"
    Assert-Contains $outInvalid "minimo 2" "asiento con una partida rechazado"
    Assert-Contains $outInvalid "Carga de asientos: 1 validos, 4 omitidos." "resumen de asientos omitidos"
    Assert-Contains $outInvalid "Valido" "asiento valido conservado"
    Assert-Contains $outInvalid "TOTAL GENERAL" "libro diario de asientos validos"

    $respaldoAsientos = Get-ChildItem (Join-Path $dirInvalid "backup") -File -Filter "asientos_invalidos_*.txt" | Select-Object -First 1
    if ($null -eq $respaldoAsientos) {
        throw "FALLO: no se creo el respaldo de asientos invalidos"
    }
    $contenidoRespaldo = Get-Content $respaldoAsientos.FullName -Raw
    if (-not $contenidoRespaldo.Contains("Fecha invalida") -or -not $contenidoRespaldo.Contains("Cuenta inexistente")) {
        throw "FALLO: el respaldo no conserva los asientos originales"
    }
    Write-Host "OK: respaldo de asientos invalidos"

    $cantidadAsientosInvalidos = (Get-Content (Join-Path $dirInvalid "data\asientos.txt") | Select-String -Pattern "^ASIENTO").Count
    if ($cantidadAsientosInvalidos -ne 1) {
        throw "FALLO: se esperaba 1 asiento valido y hay $cantidadAsientosInvalidos"
    }
    Write-Host "OK: asientos invalidos omitidos y archivo limpio"

    # ------------------------------------------------------------------
    # Advertencia por saldo en el lado contrario
    # ------------------------------------------------------------------
    $dirSaldo = New-TestDir "saldo"
    $cuentasSaldo = @(
        "1100|Bancos|Activo|0.0",
        "1300|Inventarios|Activo|0.0",
        "3000|Capital social|Patrimonio|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirSaldo "data\cuentas.txt") ($cuentasSaldo + "`r`n")

    $asientosSaldo = @(
        "ASIENTO|01/01/2026|Aporte de capital|10000.0",
        "PARTIDA|1100|Debito|10000.0",
        "PARTIDA|3000|Credito|10000.0",
        "ASIENTO|02/01/2026|Compra de mercaderia|2000.0",
        "PARTIDA|1300|Debito|2000.0",
        "PARTIDA|1100|Credito|2000.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirSaldo "data\asientos.txt") ($asientosSaldo + "`r`n")

    $inputRechazo = Write-Input $dirSaldo "input_rechazo.txt" @(
        "2", "", "Venta rechazada", "1300", "2", "65000", "N", "0", "11"
    )
    $outRechazo = Invoke-App $dirSaldo $inputRechazo
    Assert-Contains $outRechazo "la cuenta quedaria con saldo en el lado contrario al normal" "advertencia de saldo contrario"
    Assert-Contains $outRechazo "Partida descartada." "partida descartada por saldo contrario"
    Assert-Contains $outRechazo "Asiento cancelado." "asiento cancelado tras rechazo"
    $cantidadTrasRechazo = (Get-Content (Join-Path $dirSaldo "data\asientos.txt") | Select-String -Pattern "^ASIENTO").Count
    if ($cantidadTrasRechazo -ne 2) {
        throw "FALLO: el asiento rechazado se guardo. Hay $cantidadTrasRechazo asientos."
    }
    Write-Host "OK: asiento con saldo contrario rechazado"

    $inputAcepta = Write-Input $dirSaldo "input_acepta.txt" @(
        "2", "", "Venta aceptada", "1300", "2", "65000", "S",
        "1100", "1", "65000", "N", "S", "11"
    )
    $outAcepta = Invoke-App $dirSaldo $inputAcepta
    Assert-Contains $outAcepta "Desea continuar? (S/N):" "confirmacion de saldo contrario"
    Assert-Contains $outAcepta "Asiento registrado exitosamente." "asiento aceptado con saldo contrario"
    $cantidadTrasAceptar = (Get-Content (Join-Path $dirSaldo "data\asientos.txt") | Select-String -Pattern "^ASIENTO").Count
    if ($cantidadTrasAceptar -ne 3) {
        throw "FALLO: se esperaban 3 asientos y hay $cantidadTrasAceptar"
    }
    Write-Host "OK: asiento con saldo contrario confirmado"

    $inputReportes = Write-Input $dirSaldo "input_reportes.txt" @(
        "3", "1", "1300", "0", "8", "11"
    )
    $outReportes = Invoke-App $dirSaldo $inputReportes
    Assert-Contains $outReportes "AVISO: cuentas con saldo en el lado contrario a su tipo:" "aviso de saldo contrario al iniciar"
    Assert-Contains $outReportes "1300 - Inventarios (Activo)" "cuenta con saldo contrario listada"
    Assert-Contains $outReportes "lado contrario al normal para una cuenta de tipo Activo" "nota en cuenta T"
    Assert-Contains $outReportes "Saldo en el lado contrario al normal" "nota en balance general"
    Write-Host "OK: reportes marcan el saldo contrario"

    Write-Host ""
    Write-Host "TODAS LAS PRUEBAS PASARON."
} finally {
    if (-not $Keep -and (Test-Path $temp)) {
        Remove-Item -Path $temp -Recurse -Force
    }
}
