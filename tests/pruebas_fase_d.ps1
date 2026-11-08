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

function Assert-NotContains {
    param([string]$Text, [string]$Unexpected, [string]$Name)
    if ($Text.Contains($Unexpected)) {
        throw "FALLO: $Name. Se encontro: $Unexpected"
    }
    Write-Host "OK: $Name"
}

function Assert-Match {
    param([string]$Text, [string]$Pattern, [string]$Name)
    if ($Text -notmatch $Pattern) {
        throw "FALLO: $Name. No se encontro el patron: $Pattern"
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
        cmd /c "java -Dfile.encoding=UTF-8 -Duser.language=es -Duser.country=ES -cp bin Contabilidad < $InputFile > output.txt 2>&1"
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
    Assert-Contains $out "Detalle" "libro diario detallado"
    Assert-Contains $out "Parcial" "columna parcial del libro diario"
    Assert-Contains $out "A Ventas" "credito con prefijo A en libro diario"
    Assert-Contains $out "01/01/2026 Venta de mercaderia" "fecha y descripcion al pie del asiento"
    Assert-Contains $out "TOTAL GENERAL" "total general del libro diario"
    Assert-Contains $out "CUENTA: 1000 - Caja" "libro mayor"
    Assert-Contains $out "LA BALANZA DE COMPROBACION ESTA CUADRADA." "balanza cuadrada"
    Assert-Contains $out "Debitos" "columna debitos de la balanza"
    Assert-Contains $out "Creditos" "columna creditos de la balanza"
    Assert-Contains $out "Deudor" "columna deudor de la balanza"
    Assert-Contains $out "Acreedor" "columna acreedor de la balanza"
    $lineaBalanza = ($out -split "`r?`n") | Where-Object {
        $_ -match '^1000\s+Caja\s' -and ([regex]::Matches($_, '\d+[.,]\d{2}\b')).Count -eq 4
    }
    if (-not $lineaBalanza) {
        throw "FALLO: la balanza no muestra los totales de Caja en Deudor/Acreedor"
    }
    Write-Host "OK: columnas Deudor y Acreedor con totales"
    Assert-Contains $out "Utilidad neta:" "estado de resultados"
    Assert-Match $out 'TOTAL INGRESOS:\s+3500[.,]50' "total de ingresos exacto"
    Assert-Match $out 'TOTAL GASTOS:\s+200[.,]00' "total de gastos exacto"
    Assert-Match $out 'Utilidad bruta:\s+3500[.,]50' "utilidad bruta exacta"
    Assert-Match $out 'Utilidad neta:\s+3300[.,]50' "utilidad neta exacta"
    Assert-Match $out 'TOTAL ACTIVO:\s+3300[.,]50' "total activo exacto"
    Assert-Match $out 'TOTAL PASIVO:\s+0[.,]00' "total pasivo exacto"
    Assert-Match $out 'TOTAL PATRIMONIO:\s+0[.,]00' "total patrimonio exacto"
    Assert-Match $out 'Resultado del periodo:\s+3300[.,]50' "resultado del periodo exacto"
    Assert-Match $out 'Total Pasivo \+ Patrimonio \+ Resultado:\s+3300[.,]50' "ecuacion del balance exacta"
    Assert-Match $out 'Diferencia:\s+0[.,]00' "diferencia del balance en cero"
    Assert-Match $out 'TOTALES:\s+3700[.,]50\s+3700[.,]50' "totales de la balanza exactos"
    Assert-Match $out 'Saldo final:\s+2800[.,]50' "saldo final de caja exacto"
    Assert-Contains $out "EL BALANCE CUADRA CORRECTAMENTE." "balance general"
    Assert-Contains $out "Gasto de renta" "busqueda de asientos"
    Assert-Contains $out "Respaldo creado en: backup\manual_" "respaldo manual"

    # ------------------------------------------------------------------
    # Cuenta T de naturaleza acreedora: saldo y etiqueta correctos
    # ------------------------------------------------------------------
    $dirCuentaT = New-TestDir "cuenta_t_acreedor"
    $cuentasT = @("1000|Caja|Activo|0.0", "3000|Capital social|Patrimonio|0.0") -join "`r`n"
    Write-Utf8 (Join-Path $dirCuentaT "data\cuentas.txt") ($cuentasT + "`r`n")
    $asientosT = @(
        "ASIENTO|01/01/2026|Aporte de capital|1000.0",
        "PARTIDA|1000|Debito|1000.0",
        "PARTIDA|3000|Credito|1000.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirCuentaT "data\asientos.txt") ($asientosT + "`r`n")
    $inputCuentaT = Write-Input $dirCuentaT "input.txt" @("3", "1", "3000", "0", "11")
    $outCuentaT = Invoke-App $dirCuentaT $inputCuentaT
    Assert-Contains $outCuentaT "CUENTA T: 3000 - Capital social (Patrimonio)" "cuenta T de patrimonio"
    Assert-Contains $outCuentaT "SALDO: 1000,00 (Acreedor)" "saldo acreedor en cuenta T"
    Assert-NotContains $outCuentaT "lado contrario al normal para una cuenta de tipo Patrimonio" "sin aviso falso en cuenta T de patrimonio"
    Write-Host "OK: cuenta T de naturaleza acreedora"

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
    $camposBalance = $balance.Trim().Split("|")
    if ($camposBalance.Count -ne 4) {
        throw "FALLO: balance.txt no tiene 4 campos"
    }
    if ($camposBalance[0] -notmatch '^\d{2}/\d{2}/\d{4}$') {
        throw "FALLO: balance.txt no guarda una fecha valida"
    }
    $inv = [System.Globalization.CultureInfo]::InvariantCulture
    $activoBalance = [double]::Parse($camposBalance[1], $inv)
    $pasivoBalance = [double]::Parse($camposBalance[2], $inv)
    $patrimonioBalance = [double]::Parse($camposBalance[3], $inv)
    if ([Math]::Abs($activoBalance - 3300.5) -gt 0.001) {
        throw "FALLO: balance.txt guardo un activo incorrecto: $activoBalance"
    }
    if ([Math]::Abs($pasivoBalance - 0.0) -gt 0.001) {
        throw "FALLO: balance.txt guardo un pasivo incorrecto: $pasivoBalance"
    }
    if ([Math]::Abs($patrimonioBalance - 3300.5) -gt 0.001) {
        throw "FALLO: balance.txt guardo un patrimonio+resultado incorrecto: $patrimonioBalance"
    }
    Write-Host "OK: balance.txt generado con valores exactos"

    $periodo = Get-Content (Join-Path $dir "data\periodo.txt") -Raw
    if ($periodo.Trim() -notmatch '^\d{2}/\d{2}/\d{4}\|\d{2}/\d{2}/\d{4}$') {
        throw "FALLO: periodo.txt no tiene el formato inicio|fin"
    }
    Write-Host "OK: periodo.txt creado con formato inicio|fin"

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
    if (-not (Test-Path (Join-Path $respaldo.FullName "periodo.txt"))) {
        throw "FALLO: el respaldo no contiene periodo.txt"
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

    # ------------------------------------------------------------------
    # Montos estrictos: formatos aceptados y rechazados
    # ------------------------------------------------------------------
    $dirMontos = New-TestDir "montos"
    $cuentasMontos = @(
        "1000|Caja|Activo|0.0",
        "4010|Ventas|Ingreso|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirMontos "data\cuentas.txt") ($cuentasMontos + "`r`n")
    Write-Utf8 (Join-Path $dirMontos "data\asientos.txt") ""

    $inputMontos = Write-Input $dirMontos "input.txt" @(
        "2", "", "Montos estrictos", "1000", "1", "1.800", "1.2.3", "1800",
        "4010", "2", "1,800.505", "1800", "N", "S",
        "2", "", "Formato con coma de miles", "1000", "1", "1,800.50",
        "4010", "2", "1800.50", "N", "S", "11"
    )
    $outMontos = Invoke-App $dirMontos $inputMontos
    Assert-Contains $outMontos "Monto invalido: hay mas de dos decimales o el separador es ambiguo." "monto con tres decimales rechazado"
    Assert-Contains $outMontos "Monto invalido. Use solo numeros y coma o punto decimal" "monto con separadores invalidos rechazado"
    Assert-Contains $outMontos "Asiento registrado exitosamente." "montos validos aceptados"
    $cantidadMontos = (Get-Content (Join-Path $dirMontos "data\asientos.txt") | Select-String -Pattern "^ASIENTO").Count
    if ($cantidadMontos -ne 2) {
        throw "FALLO: se esperaban 2 asientos con montos validos y hay $cantidadMontos"
    }
    $contenidoMontos = Get-Content (Join-Path $dirMontos "data\asientos.txt") -Raw
    if (-not $contenidoMontos.Contains("1800.50")) {
        throw "FALLO: no se guardaron los montos normalizados a dos decimales"
    }
    Write-Host "OK: formatos de monto aceptados y rechazados"

    # ------------------------------------------------------------------
    # Monto grande: se guarda con dos decimales, sin notacion cientifica
    # ------------------------------------------------------------------
    $dirGrande = New-TestDir "monto_grande"
    $cuentasGrande = @(
        "1000|Caja|Activo|0.0",
        "4010|Ventas|Ingreso|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirGrande "data\cuentas.txt") ($cuentasGrande + "`r`n")
    Write-Utf8 (Join-Path $dirGrande "data\asientos.txt") ""

    $inputGrande = Write-Input $dirGrande "input.txt" @(
        "2", "01/06/2026", "Venta de monto grande", "1000", "1", "10000000",
        "4010", "2", "10000000", "N", "S", "11"
    )
    $outGrande = Invoke-App $dirGrande $inputGrande
    Assert-Contains $outGrande "Asiento registrado exitosamente." "monto grande registrado"
    $contenidoGrande = Get-Content (Join-Path $dirGrande "data\asientos.txt") -Raw
    if ($contenidoGrande.Contains("E7") -or -not $contenidoGrande.Contains("10000000.00")) {
        throw "FALLO: el monto grande no se guardo con dos decimales"
    }
    Write-Host "OK: monto grande persistido sin notacion cientifica"

    $inputGrande2 = Write-Input $dirGrande "input2.txt" @("4", "5", "1000", "0", "11")
    $outGrande2 = Invoke-App $dirGrande $inputGrande2
    Assert-Contains $outGrande2 "Total de asientos: 1" "asiento con monto grande recargado"
    Assert-Match $outGrande2 'Saldo final:\s+10000000[.,]00' "monto grande recargado"
    Write-Host "OK: monto grande recargado desde el archivo"

    # ------------------------------------------------------------------
    # Tolerancia 0.005, asiento solo al Debe, linea desconocida y 1790
    # ------------------------------------------------------------------
    $dirAuditoria = New-TestDir "auditoria"
    $cuentasAuditoria = @(
        "1000|Caja|Activo|0.0",
        "1790|Depreciacion acumulada|Activo|0.0",
        "4010|Ventas|Ingreso|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirAuditoria "data\cuentas.txt") ($cuentasAuditoria + "`r`n")

    $asientosAuditoria = @(
        "ASIENTO|01/01/2026|Diferencia de un centavo|100.01",
        "PARTIDA|1000|Debito|100.01",
        "PARTIDA|4010|Credito|100.00",
        "ASIENTO|02/01/2026|Diferencia de dos centavos|100.00",
        "PARTIDA|1000|Debito|100.02",
        "PARTIDA|4010|Credito|100.00",
        "ASIENTO|03/01/2026|Solo debito|200.00",
        "PARTIDA|1000|Debito|100.00",
        "PARTIDA|4010|Debito|100.00",
        "ASIENTO|04/01/2026|Linea desconocida|100.00",
        "PARTIDA|1000|Debito|100.00",
        "BASURA|esto no pertenece al asiento",
        "PARTIDA|4010|Credito|100.00",
        "ASIENTO|05/01/2026|Depreciacion|100.00",
        "PARTIDA|1000|Debito|100.00",
        "PARTIDA|1790|Credito|100.00"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirAuditoria "data\asientos.txt") ($asientosAuditoria + "`r`n")

    $inputAuditoria = Write-Input $dirAuditoria "input.txt" @("4", "11")
    $outAuditoria = Invoke-App $dirAuditoria $inputAuditoria

    Assert-Contains $outAuditoria "Carga de asientos: 1 validos, 4 omitidos." "tolerancia y errores de carga"
    Assert-NotContains $outAuditoria "Diferencia de un centavo" "asiento con diferencia de 0.01 rechazado"
    Assert-Contains $outAuditoria "no cuadra" "asientos descuadrados rechazados"
    Assert-Contains $outAuditoria "debe tener al menos una partida al Debe y una al Haber" "asiento solo al Debe rechazado"
    Assert-Contains $outAuditoria "linea desconocida en linea 12" "linea desconocida invalida el asiento"
    Assert-NotContains $outAuditoria "Diferencia de dos centavos" "asiento con diferencia de 0.02 omitido"
    Assert-NotContains $outAuditoria "AVISO: cuentas con saldo en el lado contrario a su tipo:" "cuenta 1790 sin aviso falso"
    Write-Host "OK: auditoria de tolerancia, solo Debe, linea desconocida y 1790"

    # ------------------------------------------------------------------
    # Fallo de lectura de asientos.txt: no se sobrescribe el archivo
    # ------------------------------------------------------------------
    $dirLectura = New-TestDir "lectura"
    $cuentasLectura = @(
        "1000|Caja|Activo|0.0",
        "4010|Ventas|Ingreso|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirLectura "data\cuentas.txt") ($cuentasLectura + "`r`n")
    New-Item -ItemType Directory -Path (Join-Path $dirLectura "data\asientos.txt") -Force | Out-Null

    $inputLectura = Write-Input $dirLectura "input.txt" @("11")
    $outLectura = Invoke-App $dirLectura $inputLectura
    Assert-Contains $outLectura "Error leyendo" "fallo de lectura reportado"
    Assert-Contains $outLectura "No se guardaron cambios" "guardado bloqueado por fallo de lectura"
    if (-not (Test-Path (Join-Path $dirLectura "data\asientos.txt") -PathType Container)) {
        throw "FALLO: el archivo con fallo de lectura fue reemplazado"
    }
    Write-Host "OK: asientos.txt intacto tras fallo de lectura"

    # ------------------------------------------------------------------
    # Periodos contables: cierre, traslado del resultado y filtros
    # ------------------------------------------------------------------
    $dirPeriodo = New-TestDir "periodos"
    $cuentasPeriodo = @(
        "1000|Caja|Activo|0.0",
        "3000|Capital social|Patrimonio|0.0",
        "3200|Resultados de ejercicios anteriores|Patrimonio|0.0",
        "4010|Ventas|Ingreso|0.0",
        "6010|Gastos de venta|Gasto|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirPeriodo "data\cuentas.txt") ($cuentasPeriodo + "`r`n")
    Write-Utf8 (Join-Path $dirPeriodo "data\periodo.txt") ("01/01/2026|31/12/2026" + "`r`n")

    $asientosPeriodo = @(
        "ASIENTO|10/01/2026|Aporte de capital|1000.0",
        "PARTIDA|1000|Debito|1000.0",
        "PARTIDA|3000|Credito|1000.0",
        "ASIENTO|15/01/2026|Venta del periodo|500.0",
        "PARTIDA|1000|Debito|500.0",
        "PARTIDA|4010|Credito|500.0",
        "ASIENTO|20/01/2026|Gasto del periodo|200.0",
        "PARTIDA|6010|Debito|200.0",
        "PARTIDA|1000|Credito|200.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirPeriodo "data\asientos.txt") ($asientosPeriodo + "`r`n")

    $inputPeriodo = Write-Input $dirPeriodo "input_reporte.txt" @(
        "7", "8", "12", "1", "0", "11"
    )
    $outPeriodo = Invoke-App $dirPeriodo $inputPeriodo
    Assert-Contains $outPeriodo "Periodo: 01/01/2026 al 31/12/2026" "periodo configurado"
    Assert-Contains $outPeriodo "Estado: ABIERTO" "periodo abierto"
    Assert-Contains $outPeriodo "Asientos en el periodo: 3" "conteo de asientos del periodo"
    Assert-Match $outPeriodo 'TOTAL INGRESOS:\s+500[.,]00' "ingresos del periodo"
    Assert-Match $outPeriodo 'TOTAL GASTOS:\s+200[.,]00' "gastos del periodo"
    Assert-Match $outPeriodo 'Resultado del periodo:\s+300[.,]00' "resultado del periodo antes del cierre"
    Assert-Match $outPeriodo 'TOTAL ACTIVO:\s+1300[.,]00' "activo del periodo"
    Assert-Match $outPeriodo 'TOTAL PATRIMONIO:\s+1000[.,]00' "patrimonio antes del cierre"
    Assert-Match $outPeriodo 'Total Pasivo \+ Patrimonio \+ Resultado:\s+1300[.,]00' "balance antes del cierre"
    Assert-Contains $outPeriodo "EL BALANCE CUADRA CORRECTAMENTE." "balance del periodo cuadra"

    $inputCierre = Write-Input $dirPeriodo "input_cierre.txt" @(
        "12", "3", "S", "0", "11"
    )
    $outCierre = Invoke-App $dirPeriodo $inputCierre
    Assert-Contains $outCierre "Periodo cerrado. Resultado trasladado a 3200." "cierre del periodo"
    Assert-Match $outCierre 'Resultado:\s+300[.,]00 \(Utilidad\)' "resultado del cierre"
    Assert-Contains $outCierre "Nuevo periodo: 01/01/2027 al 31/12/2027" "avance automatico del periodo"

    $asientosCerrados = Get-Content (Join-Path $dirPeriodo "data\asientos.txt") -Raw
    if (-not $asientosCerrados.Contains("|CIERRE")) {
        throw "FALLO: el asiento de cierre no se marco con |CIERRE"
    }
    if (-not $asientosCerrados.Contains("PARTIDA|3200|Credito|300.00")) {
        throw "FALLO: el resultado no se traslado a la cuenta 3200"
    }
    Write-Host "OK: asiento de cierre guardado y resultado trasladado"

    $periodoGuardado = (Get-Content (Join-Path $dirPeriodo "data\periodo.txt") -Raw).Trim()
    if ($periodoGuardado -ne "01/01/2027|31/12/2027") {
        throw "FALLO: periodo.txt no avanzo al siguiente periodo: $periodoGuardado"
    }
    Write-Host "OK: periodo.txt guardado con el periodo siguiente"

    $cuentasCerradas = Get-Content (Join-Path $dirPeriodo "data\cuentas.txt") -Raw
    if (-not $cuentasCerradas.Contains("1000|Caja|Activo|1300.00")) {
        throw "FALLO: Caja no quedo con el saldo esperado"
    }
    if (-not $cuentasCerradas.Contains("3200|Resultados de ejercicios anteriores|Patrimonio|300.00")) {
        throw "FALLO: la cuenta 3200 no quedo con el resultado del cierre"
    }
    if (-not $cuentasCerradas.Contains("4010|Ventas|Ingreso|0.00")) {
        throw "FALLO: Ventas no quedo en cero tras el cierre"
    }
    if (-not $cuentasCerradas.Contains("6010|Gastos de venta|Gasto|0.00")) {
        throw "FALLO: Gastos no quedo en cero tras el cierre"
    }
    Write-Host "OK: cuentas de resultado cerradas y saldo trasladado"

    $inputPosterior = Write-Input $dirPeriodo "input_posterior.txt" @(
        "7", "8", "4", "12", "1", "0", "11"
    )
    $outPosterior = Invoke-App $dirPeriodo $inputPosterior
    Assert-Contains $outPosterior "Periodo: 01/01/2027 al 31/12/2027" "periodo siguiente"
    Assert-Contains $outPosterior "Asientos en el periodo: 0" "periodo siguiente sin asientos"
    Assert-Contains $outPosterior "Asientos fuera del periodo: 4" "asientos fuera del periodo siguiente"
    Assert-Contains $outPosterior "No hay asientos registrados en la vista actual." "libro diario filtrado por periodo"
    Assert-Match $outPosterior 'TOTAL INGRESOS:\s+0[.,]00' "ingresos del periodo siguiente"
    Assert-Match $outPosterior 'TOTAL GASTOS:\s+0[.,]00' "gastos del periodo siguiente"
    Assert-Match $outPosterior 'Resultado del periodo:\s+0[.,]00' "resultado del periodo siguiente"
    Assert-Match $outPosterior 'TOTAL ACTIVO:\s+1300[.,]00' "activo despues del cierre"
    Assert-Match $outPosterior 'TOTAL PATRIMONIO:\s+1300[.,]00' "patrimonio despues del cierre"
    Assert-Match $outPosterior 'Total Pasivo \+ Patrimonio \+ Resultado:\s+1300[.,]00' "balance despues del cierre"
    Assert-Contains $outPosterior "EL BALANCE CUADRA CORRECTAMENTE." "balance posterior al cierre"

    $inputCerrado = Write-Input $dirPeriodo "input_cerrado.txt" @(
        "12", "2", "01/01/2026", "31/12/2026", "S", "0",
        "2", "0", "11"
    )
    $outCerrado = Invoke-App $dirPeriodo $inputCerrado
    Assert-Contains $outCerrado "El periodo ya tiene un cierre." "aviso de periodo cerrado"
    Assert-Contains $outCerrado "El periodo actual esta cerrado." "registro bloqueado en periodo cerrado"
    $cantidadCerrada = (Get-Content (Join-Path $dirPeriodo "data\asientos.txt") | Select-String -Pattern "^ASIENTO").Count
    if ($cantidadCerrada -ne 4) {
        throw "FALLO: se registro un asiento en un periodo cerrado. Hay $cantidadCerrada asientos."
    }
    Write-Host "OK: periodo cerrado sin asientos nuevos"

    $inputFuera = Write-Input $dirPeriodo "input_fuera.txt" @(
        "12", "2", "01/01/2027", "31/12/2027", "S", "0",
        "2", "01/02/2026", "01/02/2027", "Asiento fuera de periodo", "0",
        "11"
    )
    $outFuera = Invoke-App $dirPeriodo $inputFuera
    Assert-Contains $outFuera "La fecha esta fuera del periodo actual." "fecha fuera del periodo rechazada"
    Assert-Contains $outFuera "Asiento cancelado." "asiento fuera de periodo cancelado"
    $cantidadFuera = (Get-Content (Join-Path $dirPeriodo "data\asientos.txt") | Select-String -Pattern "^ASIENTO").Count
    if ($cantidadFuera -ne 4) {
        throw "FALLO: se guardo un asiento fuera del periodo. Hay $cantidadFuera asientos."
    }
    Write-Host "OK: fecha fuera del periodo no registrada"

    $inputHistorico = Write-Input $dirPeriodo "input_historico.txt" @(
        "12", "2", "01/01/2026", "31/12/2026", "S", "4", "0",
        "4", "6", "8", "0"
    )
    $outHistorico = Invoke-App $dirPeriodo $inputHistorico
    Assert-Contains $outHistorico "Vista de reportes: Historico completo" "cambio a vista historica"
    Assert-Contains $outHistorico "Asiento #4 (cierre)" "asiento de cierre en libro diario"
    Assert-Contains $outHistorico "Total de asientos: 4" "libro diario historico completo"
    Assert-Contains $outHistorico "LA BALANZA DE COMPROBACION ESTA CUADRADA." "balanza historica cuadrada"
    Assert-Match $outHistorico 'TOTALES:\s+2200[.,]00\s+2200[.,]00' "totales historicos de la balanza"
    Assert-Contains $outHistorico "Resultado acumulado pendiente:" "etiqueta de resultado acumulado en historico"

    # ------------------------------------------------------------------
    # Cierre con centavos: montos redondeados a dos decimales
    # ------------------------------------------------------------------
    $dirCentavos = New-TestDir "cierre_centavos"
    $cuentasCentavos = @(
        "1000|Caja|Activo|0.0",
        "3200|Resultados de ejercicios anteriores|Patrimonio|0.0",
        "4010|Ventas|Ingreso|0.0",
        "6010|Gastos de venta|Gasto|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirCentavos "data\cuentas.txt") ($cuentasCentavos + "`r`n")
    Write-Utf8 (Join-Path $dirCentavos "data\periodo.txt") ("01/01/2026|31/12/2026" + "`r`n")
    $asientosCentavos = @(
        "ASIENTO|10/01/2026|Venta de diez centavos|0.10",
        "PARTIDA|1000|Debito|0.10",
        "PARTIDA|4010|Credito|0.10",
        "ASIENTO|11/01/2026|Venta de veinte centavos|0.20",
        "PARTIDA|1000|Debito|0.20",
        "PARTIDA|4010|Credito|0.20",
        "ASIENTO|12/01/2026|Gasto de diez centavos|0.10",
        "PARTIDA|6010|Debito|0.10",
        "PARTIDA|1000|Credito|0.10"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirCentavos "data\asientos.txt") ($asientosCentavos + "`r`n")

    $inputCentavos = Write-Input $dirCentavos "input.txt" @("12", "3", "S", "0", "11")
    $outCentavos = Invoke-App $dirCentavos $inputCentavos
    Assert-Match $outCentavos 'Resultado:\s+0[.,]20 \(Utilidad\)' "resultado con centavos"
    $asientosCentavosCierre = Get-Content (Join-Path $dirCentavos "data\asientos.txt") -Raw
    if (-not $asientosCentavosCierre.Contains("PARTIDA|3200|Credito|0.20")) {
        throw "FALLO: el resultado con centavos no se traslado redondeado a 3200"
    }
    if (-not $asientosCentavosCierre.Contains("PARTIDA|4010|Debito|0.30")) {
        throw "FALLO: las ventas no se cerraron con el total redondeado"
    }
    $cuentasCentavosCierre = Get-Content (Join-Path $dirCentavos "data\cuentas.txt") -Raw
    if (-not $cuentasCentavosCierre.Contains("3200|Resultados de ejercicios anteriores|Patrimonio|0.20")) {
        throw "FALLO: la cuenta 3200 no quedo con el resultado redondeado"
    }
    Write-Host "OK: cierre con centavos redondeado a dos decimales"

    # ------------------------------------------------------------------
    # Cierre secuencial: no se cierra 2026 con 2025 pendiente
    # ------------------------------------------------------------------
    $dirOrden = New-TestDir "cierre_orden"
    $cuentasOrden = @(
        "1000|Caja|Activo|0.0",
        "3000|Capital social|Patrimonio|0.0",
        "3200|Resultados de ejercicios anteriores|Patrimonio|0.0",
        "4010|Ventas|Ingreso|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirOrden "data\cuentas.txt") ($cuentasOrden + "`r`n")
    Write-Utf8 (Join-Path $dirOrden "data\periodo.txt") ("01/01/2026|31/12/2026" + "`r`n")
    $asientosOrden = @(
        "ASIENTO|01/06/2025|Venta 2025|100.00",
        "PARTIDA|1000|Debito|100.00",
        "PARTIDA|4010|Credito|100.00",
        "ASIENTO|01/06/2026|Venta 2026|200.00",
        "PARTIDA|1000|Debito|200.00",
        "PARTIDA|4010|Credito|200.00"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirOrden "data\asientos.txt") ($asientosOrden + "`r`n")

    $inputBloqueo = Write-Input $dirOrden "input_bloqueo.txt" @("12", "3", "0", "8", "11")
    $outBloqueo = Invoke-App $dirOrden $inputBloqueo
    Assert-Contains $outBloqueo "No se puede cerrar: hay resultados de periodos anteriores sin cerrar." "cierre 2026 bloqueado por 2025 abierto"
    Assert-Match $outBloqueo 'Resultado acumulado pendiente:\s+300[.,]00' "resultado acumulado pendiente"
    $cierresBloqueo = (Get-Content (Join-Path $dirOrden "data\asientos.txt") | Select-String -Pattern "CIERRE").Count
    if ($cierresBloqueo -ne 0) {
        throw "FALLO: se creo un cierre fuera de orden"
    }
    Write-Host "OK: cierre fuera de orden bloqueado"

    $inputOrden = Write-Input $dirOrden "input_orden.txt" @(
        "12", "2", "01/01/2025", "31/12/2025", "S", "3", "S",
        "3", "S", "0", "11"
    )
    $outOrden = Invoke-App $dirOrden $inputOrden
    Assert-Contains $outOrden "Periodo cerrado. Resultado trasladado a 3200." "cierre de 2025"
    Assert-Contains $outOrden "Nuevo periodo: 01/01/2026 al 31/12/2026" "avance a 2026"
    Assert-Contains $outOrden "Nuevo periodo: 01/01/2027 al 31/12/2027" "avance a 2027"
    $cierresOrden = (Get-Content (Join-Path $dirOrden "data\asientos.txt") | Select-String -Pattern "^ASIENTO.*\|CIERRE").Count
    if ($cierresOrden -ne 2) {
        throw "FALLO: se esperaban 2 asientos de cierre y hay $cierresOrden"
    }
    $linea3200 = (Get-Content (Join-Path $dirOrden "data\cuentas.txt") | Where-Object { $_ -like "3200|*" })
    if ($linea3200 -ne "3200|Resultados de ejercicios anteriores|Patrimonio|300.00") {
        throw "FALLO: la cuenta 3200 no acumulo exactamente 300.00: $linea3200"
    }
    Write-Host "OK: cierres secuenciales acumulan 300.00 en 3200"

    $inputPosterior = Write-Input $dirOrden "input_posterior.txt" @(
        "12", "2", "01/01/2024", "31/12/2024", "S", "3", "0", "11"
    )
    $outPosterior = Invoke-App $dirOrden $inputPosterior
    Assert-Contains $outPosterior "No se puede cerrar: ya existe un cierre posterior." "cierre anterior bloqueado por cierre posterior"
    Write-Host "OK: no se cierra un periodo anterior a uno ya cerrado"

    # ------------------------------------------------------------------
    # Cierre con 3200 ausente o de tipo incorrecto
    # ------------------------------------------------------------------
    $dir3200 = New-TestDir "cierre_3200"
    $cuentas3200 = @(
        "1000|Caja|Activo|0.0",
        "3200|Resultados de ejercicios anteriores|Activo|0.0",
        "4010|Ventas|Ingreso|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dir3200 "data\cuentas.txt") ($cuentas3200 + "`r`n")
    Write-Utf8 (Join-Path $dir3200 "data\periodo.txt") ("01/01/2026|31/12/2026" + "`r`n")
    $asientos3200 = @(
        "ASIENTO|10/01/2026|Venta del periodo|100.00",
        "PARTIDA|1000|Debito|100.00",
        "PARTIDA|4010|Credito|100.00"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dir3200 "data\asientos.txt") ($asientos3200 + "`r`n")

    $input3200 = Write-Input $dir3200 "input.txt" @("12", "3", "0", "11")
    $out3200 = Invoke-App $dir3200 $input3200
    Assert-Contains $out3200 "La cuenta 3200 no existe o no es de tipo Patrimonio." "cierre rechazado por tipo de 3200"
    $cierres3200 = (Get-Content (Join-Path $dir3200 "data\asientos.txt") | Select-String -Pattern "CIERRE").Count
    if ($cierres3200 -ne 0) {
        throw "FALLO: se cerro el periodo con 3200 de tipo incorrecto"
    }
    Write-Host "OK: cierre rechazado por 3200 de tipo incorrecto"

    $cuentasSin3200 = @(
        "1000|Caja|Activo|0.0",
        "4010|Ventas|Ingreso|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dir3200 "data\cuentas.txt") ($cuentasSin3200 + "`r`n")
    $input3200b = Write-Input $dir3200 "input2.txt" @("12", "3", "0", "11")
    $out3200b = Invoke-App $dir3200 $input3200b
    Assert-Contains $out3200b "La cuenta 3200 no existe o no es de tipo Patrimonio." "cierre rechazado por 3200 ausente"
    Write-Host "OK: cierre rechazado por 3200 ausente"

    # ------------------------------------------------------------------
    # Fin de entrada: el programa cierra limpio, sin excepciones
    # ------------------------------------------------------------------
    $dirEof = New-TestDir "eof"
    Write-Utf8 (Join-Path $dirEof "data\cuentas.txt") (("1000|Caja|Activo|0.0", "4010|Ventas|Ingreso|0.0") -join "`r`n")
    Write-Utf8 (Join-Path $dirEof "data\asientos.txt") ""

    $casosEof = [ordered]@{
        "registro" = @("2")
        "cuenta_t"  = @("3", "1")
        "periodo"   = @("12", "2")
        "crear"     = @("1", "2")
        "busqueda"  = @("9", "2", "2")
        "respaldo"  = @("10")
    }
    foreach ($nombre in $casosEof.Keys) {
        $inputEof = Write-Input $dirEof ("in_" + $nombre + ".txt") $casosEof[$nombre]
        $outEof = Invoke-App $dirEof $inputEof
        Assert-NotContains $outEof "Exception in thread" ("sin excepcion al agotar la entrada ($nombre)")
        Assert-Contains $outEof "Hasta luego." ("cierre ordenado al agotar la entrada ($nombre)")
    }
    $outEofRegistro = Invoke-App $dirEof (Write-Input $dirEof "in_registro2.txt" @("2"))
    Assert-Contains $outEofRegistro "Asiento cancelado. No se guardo nada." "asiento cancelado al agotar la entrada"
    $outEofPeriodo = Invoke-App $dirEof (Write-Input $dirEof "in_periodo2.txt" @("12", "2"))
    Assert-Contains $outEofPeriodo "No se cambio el periodo." "periodo sin cambios al agotar la entrada"
    Write-Host "OK: fin de entrada cierra el programa sin excepciones"

    # ------------------------------------------------------------------
    # Catalogo vacio: se puede reconstruir y seguir operando
    # ------------------------------------------------------------------
    $dirCatalogo = New-TestDir "catalogo_vacio"
    Write-Utf8 (Join-Path $dirCatalogo "data\cuentas.txt") ""
    Write-Utf8 (Join-Path $dirCatalogo "data\asientos.txt") ""
    Write-Utf8 (Join-Path $dirCatalogo "data\periodo.txt") ("01/01/2020|31/12/2099" + "`r`n")

    $inputCatalogo = Write-Input $dirCatalogo "input.txt" @(
        "1", "4", "0",
        "1", "2", "Cuenta de prueba", "1", "1010", "0",
        "2", "", "Prueba tras recuperar", "1000", "1", "100", "4020", "2", "100", "N", "S",
        "11"
    )
    $outCatalogo = Invoke-App $dirCatalogo $inputCatalogo
    Assert-Contains $outCatalogo "Use la opcion 1.4 (Reconstruir catalogo estandar)" "aviso para recuperar el catalogo"
    Assert-Contains $outCatalogo "Catalogo reconstruido con 75 cuentas estandar." "catalogo reconstruido"
    Assert-Contains $outCatalogo "Cuenta creada exitosamente: 1010 - Cuenta de prueba (Activo)." "cuenta creada tras recuperar"
    Assert-Contains $outCatalogo "Asiento registrado exitosamente." "asiento registrado tras recuperar"

    $lineasCatalogo = (Get-Content (Join-Path $dirCatalogo "data\cuentas.txt") | Where-Object { $_.Trim() -ne "" }).Count
    if ($lineasCatalogo -ne 76) {
        throw "FALLO: se esperaban 76 cuentas (75 estandar + 1 creada) y hay $lineasCatalogo"
    }
    $asientosCatalogo = (Get-Content (Join-Path $dirCatalogo "data\asientos.txt") | Select-String -Pattern "^ASIENTO").Count
    if ($asientosCatalogo -ne 1) {
        throw "FALLO: se esperaba 1 asiento y hay $asientosCatalogo"
    }
    $respaldoCatalogo = Get-ChildItem (Join-Path $dirCatalogo "backup") -File -Filter "cuentas_antes_reconstruir_*.txt"
    if ($null -eq $respaldoCatalogo) {
        throw "FALLO: no se guardo la copia del catalogo antes de reconstruir"
    }
    Write-Host "OK: catalogo vacio reconstruido y operativo"

    # ------------------------------------------------------------------
    # Catalogo vacio con asientos validos: el archivo no se sobrescribe
    # ------------------------------------------------------------------
    $dirProteccion = New-TestDir "catalogo_protegido"
    Write-Utf8 (Join-Path $dirProteccion "data\cuentas.txt") ""
    $asientosProteccion = @(
        "ASIENTO|01/03/2026|Venta protegida|100.00",
        "PARTIDA|1000|Debito|100.00",
        "PARTIDA|4020|Credito|100.00"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirProteccion "data\asientos.txt") $asientosProteccion

    $inputProteccion = Write-Input $dirProteccion "input.txt" @("1", "4", "0", "4", "11")
    $outProteccion = Invoke-App $dirProteccion $inputProteccion
    Assert-Contains $outProteccion "no se sobrescribira data/asientos.txt" "aviso de proteccion del archivo de asientos"
    Assert-Contains $outProteccion "Asientos recargados: 1" "asientos recargados tras reconstruir"
    Assert-Contains $outProteccion "Total de asientos: 1" "libro diario con los asientos recuperados"
    $contenidoProteccion = Get-Content (Join-Path $dirProteccion "data\asientos.txt") -Raw
    if (-not $contenidoProteccion.Contains("Venta protegida")) {
        throw "FALLO: se perdieron los asientos originales al reconstruir el catalogo"
    }
    Write-Host "OK: asientos originales preservados durante la recuperacion"

    # ------------------------------------------------------------------
    # Cuenta huerfana: la reconstruccion se rechaza
    # ------------------------------------------------------------------
    $dirHuerfano = New-TestDir "catalogo_huerfano"
    Write-Utf8 (Join-Path $dirHuerfano "data\cuentas.txt") ""
    $asientosHuerfano = @(
        "ASIENTO|01/03/2026|Uso de cuenta propia|100.00",
        "PARTIDA|A100|Debito|100.00",
        "PARTIDA|4020|Credito|100.00"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dirHuerfano "data\asientos.txt") $asientosHuerfano

    $inputHuerfano = Write-Input $dirHuerfano "input.txt" @("1", "4", "0", "11")
    $outHuerfano = Invoke-App $dirHuerfano $inputHuerfano
    Assert-Contains $outHuerfano "No se puede reconstruir el catalogo" "reconstruccion bloqueada por cuenta huerfana"
    Assert-Contains $outHuerfano "Cuentas afectadas: A100" "codigo huerfano identificado"
    Assert-NotContains $outHuerfano "Catalogo reconstruido" "no se reconstruye con cuentas huerfanas"
    $contenidoHuerfano = Get-Content (Join-Path $dirHuerfano "data\asientos.txt") -Raw
    if (-not $contenidoHuerfano.Contains("PARTIDA|A100|Debito|100.00")) {
        throw "FALLO: se perdio el archivo de asientos con una cuenta huerfana"
    }
    Write-Host "OK: reconstruccion rechazada por cuenta huerfana"

    Write-Host ""
    Write-Host "TODAS LAS PRUEBAS PASARON."
} finally {
    if (-not $Keep -and (Test-Path $temp)) {
        Remove-Item -Path $temp -Recurse -Force
    }
}
