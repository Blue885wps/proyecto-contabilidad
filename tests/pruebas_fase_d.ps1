param(
    [switch]$Keep
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$temp = Join-Path ([System.IO.Path]::GetTempPath()) ("contab_pruebas_fase_d_" + [Guid]::NewGuid().ToString("N"))
$acento = [char]0x00ED
$acentoO = [char]0x00F3
$acentoU = [char]0x00FA

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

function Assert-Match {
    param([string]$Text, [string]$Pattern, [string]$Name)
    if (-not [System.Text.RegularExpressions.Regex]::IsMatch($Text, $Pattern)) {
        throw "FALLO: $Name. No coincide con: $Pattern"
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
    # Busquedas, montos, fechas y revision del asiento
    # ------------------------------------------------------------------
    $dir = New-TestDir "busquedas"
    $cuentas = @(
        "1000|Caja|Activo|0.0",
        ("1790|Depreciaci" + $acentoO + "n acumulada|Activo|0.0"),
        "4000|Ingresos|Ingreso|0.0",
        "4010|Ventas|Ingreso|0.0",
        "6000|Gastos|Gasto|0.0",
        "A100|Cuenta especial|Activo|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dir "data\cuentas.txt") ($cuentas + "`r`n")

    $asientos = @(
        ("ASIENTO|01/01/2026|Venta de mercader" + $acento + "a|1000.0"),
        "PARTIDA|1000|Debito|1000.0",
        "PARTIDA|4010|Credito|1000.0",
        "ASIENTO|15/01/2026|Gasto de renta|200.0",
        "PARTIDA|6000|Debito|200.0",
        "PARTIDA|1000|Credito|200.0",
        "ASIENTO|01/02/2026|Venta de servicios|500.0",
        "PARTIDA|1000|Debito|500.0",
        "PARTIDA|4010|Credito|500.0",
        ("ASIENTO|15/02/2026|Gasto de servicios p" + $acentoU + "blicos|100.0"),
        "PARTIDA|6000|Debito|100.0",
        "PARTIDA|1000|Credito|100.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dir "data\asientos.txt") ($asientos + "`r`n")

    $input = Write-Input $dir "input.txt" @(
        "1", "3", "depreciacion",
        "1", "3", ("DEPRECIACI" + $acentoO + "N"),
        "1", "3", "CAJA",
        "1", "3", "a100",
        "1", "3", "zzz",
        "0",
        "9", "2", "2", "MERCADERIA",
        "2", "2", "RENTA",
        "0",
        "2", "", "Prueba de acentos", "caja", "1", "1.800,50",
        "ingresos", "2", "1800,50", "N", "N",
        "2", "", "Prueba monto ambiguo", "1000", "1", "1.800", "1800",
        "ingresos", "2", "1800", "N", "N",
        "4", "11"
    )
    $out = Invoke-App $dir $input
    $hoy = Get-Date -Format "dd/MM/yyyy"

    Assert-Contains $out "1790" "busqueda sin tilde encuentra cuenta"
    Assert-Contains $out "Cuenta especial" "busqueda de codigo no numerico"
    Assert-Contains $out "No se encontraron resultados." "busqueda sin coincidencias"
    Assert-Contains $out "Monto invalido" "monto ambiguo rechazado"
    Assert-Contains $out "Fecha: $hoy" "Enter en fecha usa la fecha actual"
    Assert-Contains $out "Asiento cancelado. No se guardo nada." "revision permite cancelar"
    Assert-Contains $out "Total de asientos: 4" "los asientos cancelados no se guardan"
    $cantidadAsientos = (Get-Content (Join-Path $dir "data\asientos.txt") | Select-String -Pattern "^ASIENTO").Count
    if ($cantidadAsientos -ne 4) {
        throw "FALLO: se esperaban 4 asientos en disco y hay $cantidadAsientos"
    }
    Write-Host "OK: archivo de asientos sin cambios"

    # ------------------------------------------------------------------
    # Reportes, cuentas T y busqueda por rango (historico completo)
    # ------------------------------------------------------------------
    $dir = New-TestDir "reportes"
    $cuentas = @(
        "1000|Caja|Activo|0.0",
        "4000|Ingresos|Ingreso|0.0",
        "4010|Ventas|Ingreso|0.0",
        "6000|Gastos|Gasto|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dir "data\cuentas.txt") ($cuentas + "`r`n")
    Write-Utf8 (Join-Path $dir "data\asientos.txt") ($asientos + "`r`n")

    $input = Write-Input $dir "input.txt" @(
        "8",
        "7",
        "6",
        "4",
        "5", "1000",
        "3", "1", "1000",
        "2",
        "0",
        "9", "2", "4", "01/01/2026", "31/01/2026",
        "0",
        "11"
    )
    $out = Invoke-App $dir $input

    Assert-Contains $out "Balance al $hoy" "balance general sin periodo"
    Assert-Match   $out "Utilidad neta:\s+1200,00" "resultado acumulado"
    Assert-Match   $out "TOTAL ACTIVO:\s+1200,00" "activo acumulado"
    Assert-Contains $out "Total de asientos: 4" "libro diario completo"
    Assert-Contains $out "EL BALANCE DE COMPROBACION ESTA CUADRADO." "comprobacion cuadrada"
    Assert-Contains $out "CUENTA T: 1000 - Caja (Activo)" "cuenta T individual"
    Assert-Contains $out "TOTAL DEBE" "totales del debe"
    Assert-Contains $out "TOTAL HABER" "totales del haber"
    Assert-Contains $out "SALDO: 1200,00 (Deudor)" "saldo deudor de caja"
    Assert-Contains $out "CUENTA T: 4010 - Ventas (Ingreso)" "cuentas T con movimientos"
    Assert-Contains $out "Resultado #1 (Asiento #1)" "busqueda por rango: primer asiento"
    Assert-Contains $out "Resultado #2 (Asiento #2)" "busqueda por rango: segundo asiento"

    # ------------------------------------------------------------------
    # Restauracion de respaldos
    # ------------------------------------------------------------------
    $dir = New-TestDir "restauracion"
    $backupDir = Join-Path $dir "backup\reinicio_99990101_000000_000"
    New-Item -ItemType Directory -Path $backupDir -Force | Out-Null

    $cuentas = @(
        "1000|Caja|Activo|0.0",
        "4000|Ingresos|Ingreso|0.0",
        "4010|Ventas|Ingreso|0.0",
        "6000|Gastos|Gasto|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dir "data\cuentas.txt") ($cuentas + "`r`n")
    Write-Utf8 (Join-Path $backupDir "cuentas.txt") ($cuentas + "`r`n")

    $viejos = @(
        "ASIENTO|01/01/2026|Venta inicial|1000.0",
        "PARTIDA|1000|Debito|1000.0",
        "PARTIDA|4010|Credito|1000.0",
        "ASIENTO|15/01/2026|Gasto inicial|200.0",
        "PARTIDA|6000|Debito|200.0",
        "PARTIDA|1000|Credito|200.0"
    ) -join "`r`n"
    $nuevos = $viejos + "`r`n" + (@(
        "ASIENTO|01/02/2026|Venta posterior|500.0",
        "PARTIDA|1000|Debito|500.0",
        "PARTIDA|4010|Credito|500.0",
        "ASIENTO|15/02/2026|Gasto posterior|100.0",
        "PARTIDA|6000|Debito|100.0",
        "PARTIDA|1000|Credito|100.0"
    ) -join "`r`n")

    Write-Utf8 (Join-Path $dir "data\asientos.txt") ($nuevos + "`r`n")
    Write-Utf8 (Join-Path $backupDir "asientos.txt") ($viejos + "`r`n")
    (Get-Item $backupDir).LastWriteTime = [DateTime]"2099-01-01"

    $input = Write-Input $dir "input.txt" @("10", "2", "1", "RESTAURAR", "0", "11")
    $out = Invoke-App $dir $input

    Assert-Contains $out "Datos restaurados desde:" "restauracion completada"
    Assert-Contains $out "Respaldo de seguridad:" "respaldo previo creado"
    $cantidadAsientos = (Get-Content (Join-Path $dir "data\asientos.txt") | Select-String -Pattern "^ASIENTO").Count
    if ($cantidadAsientos -ne 2) {
        throw "FALLO: la restauracion debia dejar 2 asientos y dejo $cantidadAsientos"
    }
    Write-Host "OK: datos restaurados al estado anterior"

    $seguridad = @(Get-ChildItem (Join-Path $dir "backup") -Directory |
        Where-Object { $_.Name.StartsWith("previo_restauracion_") })
    if ($seguridad.Count -lt 1) {
        throw "FALLO: no se encontro la carpeta previo_restauracion_"
    }
    Write-Host "OK: carpeta de respaldo de seguridad presente"

    # ------------------------------------------------------------------
    # Respaldo manual
    # ------------------------------------------------------------------
    $dir = New-TestDir "respaldo_manual"
    $cuentas = @(
        "1000|Caja|Activo|0.0",
        "4010|Ventas|Ingreso|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dir "data\cuentas.txt") ($cuentas + "`r`n")
    Write-Utf8 (Join-Path $dir "data\asientos.txt") ""

    $input = Write-Input $dir "input.txt" @("10", "1", "0", "11")
    $out = Invoke-App $dir $input

    Assert-Contains $out "Respaldo creado en: backup\manual_" "respaldo manual creado"
    $manuales = @(Get-ChildItem (Join-Path $dir "backup") -Directory |
        Where-Object { $_.Name.StartsWith("manual_") })
    if ($manuales.Count -lt 1) {
        throw "FALLO: no se encontro la carpeta manual_"
    }
    if (-not (Test-Path (Join-Path $manuales[0].FullName "cuentas.txt"))) {
        throw "FALLO: el respaldo manual no contiene cuentas.txt"
    }
    Write-Host "OK: respaldo manual contiene los datos"

    # ------------------------------------------------------------------
    # Carga de cuentas con codigos duplicados normalizados
    # ------------------------------------------------------------------
    $dir = New-TestDir "duplicados"
    $cuentas = @(
        "1000|Caja|Activo|0.0",
        "ABC1|Cuenta uno|Activo|0.0",
        "abc1|Cuenta duplicada|Activo|0.0"
    ) -join "`r`n"
    Write-Utf8 (Join-Path $dir "data\cuentas.txt") ($cuentas + "`r`n")
    Write-Utf8 (Join-Path $dir "data\asientos.txt") ""

    $input = Write-Input $dir "input.txt" @("11")
    $out = Invoke-App $dir $input
    Assert-Contains $out "codigo duplicado" "carga detecta codigo duplicado normalizado"

    $lineas = @(Get-Content (Join-Path $dir "data\cuentas.txt") | Where-Object { $_.Trim() -ne "" })
    if ($lineas.Count -ne 2) {
        throw "FALLO: se esperaban 2 cuentas tras descartar el duplicado y hay $($lineas.Count)"
    }
    Write-Host "OK: se conserva la primera aparicion"

    Write-Host ""
    Write-Host "TODAS LAS PRUEBAS PASARON."
} finally {
    if ($Keep) {
        Write-Host "Directorio temporal conservado: $temp"
    } else {
        Remove-Item $temp -Recurse -Force -ErrorAction SilentlyContinue
    }
}
