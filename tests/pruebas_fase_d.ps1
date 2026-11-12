param(
    [switch]$Keep
)

$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$temp = Join-Path ([System.IO.Path]::GetTempPath()) ("contab_pruebas_" + [Guid]::NewGuid().ToString("N"))

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
    Copy-Item (Join-Path $temp "bin\*.class") (Join-Path $dir "bin\") -Force
    return $dir
}

function Copy-Data {
    param([string]$Dir)
    Copy-Item (Join-Path $root "data\cuentas.txt") (Join-Path $Dir "data\") -Force
    Copy-Item (Join-Path $root "data\asientos.txt") (Join-Path $Dir "data\") -Force
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
        if ($LASTEXITCODE -ne 0) {
            throw "Java termino con codigo $LASTEXITCODE en $Dir"
        }
        return [System.IO.File]::ReadAllText((Join-Path $Dir "output.txt"))
    } finally {
        Pop-Location
    }
}

try {
    New-Item -ItemType Directory -Path (Join-Path $temp "bin") -Force | Out-Null

    Write-Host "Compilando..."
    & javac -d (Join-Path $temp "bin") (Get-ChildItem (Join-Path $root "src\*.java")).FullName
    if ($LASTEXITCODE -ne 0) {
        throw "No se pudo compilar src\*.java"
    }
    Write-Host "OK: compilacion"

    # ------------------------------------------------------------------
    # Menu principal: 11 opciones, sin opcion 12
    # ------------------------------------------------------------------
    $dir = New-TestDir "menu"
    $input = Write-Input $dir "entrada.txt" @("11")
    $out = Invoke-App $dir $input
    Assert-Contains $out "1. Administrar cuentas" "menu opcion 1"
    Assert-Contains $out "10. Respaldar informacion" "menu opcion 10"
    Assert-Contains $out "11. Salir (o 0)" "menu opcion 11"
    Assert-NotContains $out "12. Rango" "sin opcion 12"
    Assert-NotContains $out "Gestionar periodos" "sin periodos"

    # ------------------------------------------------------------------
    # Catalogo y creacion de cuentas
    # ------------------------------------------------------------------
    $dir = New-TestDir "cuentas"
    $input = Write-Input $dir "entrada.txt" @("1", "1", "0", "11")
    $out = Invoke-App $dir $input
    Assert-Contains $out "=== CATALOGO DE CUENTAS ===" "catalogo visible"
    Assert-Contains $out "1000" "codigo 1000 en catalogo"
    Assert-Contains $out "Caja" "cuenta Caja en catalogo"

    $dir = New-TestDir "crear"
    $input = Write-Input $dir "entrada.txt" @("1", "2", "Caja chica", "1", "1010", "0", "11")
    $out = Invoke-App $dir $input
    Assert-Contains $out "Cuenta creada: 1010 - Caja chica (Activo)." "crear cuenta"
    $cuentas = Get-Content (Join-Path $dir "data\cuentas.txt") -Raw
    Assert-Contains $cuentas "1010|Caja chica|Activo|0.00" "cuenta persistida"

    $dir = New-TestDir "duplicada"
    $input = Write-Input $dir "entrada.txt" @("1", "2", "Duplicada", "1", "1000", "1011", "0", "11")
    $out = Invoke-App $dir $input
    Assert-Contains $out "El codigo ya existe." "rechazo de codigo duplicado"
    Assert-Contains $out "Cuenta creada: 1011 - Duplicada (Activo)." "codigo libre tras duplicado"

    # ------------------------------------------------------------------
    # Registro de asientos y persistencia
    # ------------------------------------------------------------------
    $dir = New-TestDir "registro"
    Copy-Data $dir
    $input = Write-Input $dir "entrada.txt" @("2", "05/11/2026", "Compra de suministros", "6020", "1", "250", "1000", "2", "250", "N", "S", "11")
    $out = Invoke-App $dir $input
    Assert-Contains $out "Asiento registrado exitosamente." "registrar asiento cuadrado"
    $asientos = Get-Content (Join-Path $dir "data\asientos.txt") -Raw
    Assert-Contains $asientos "ASIENTO|05/11/2026|Compra de suministros|250.00" "asiento persistido"

    $input = Write-Input $dir "consulta.txt" @("9", "11")
    $out = Invoke-App $dir $input
    Assert-Contains $out "Asiento #5" "asiento recuperado al reiniciar"
    Assert-Contains $out "Compra de suministros" "descripcion recuperada"

    # ------------------------------------------------------------------
    # Asiento descuadrado
    # ------------------------------------------------------------------
    $dir = New-TestDir "descuadrado"
    Copy-Data $dir
    $input = Write-Input $dir "entrada.txt" @("2", "05/11/2026", "Asiento descuadrado", "1000", "1", "100", "3000", "2", "50", "N", "11")
    $out = Invoke-App $dir $input
    Assert-Contains $out "El asiento no cuadra" "rechazo de asiento descuadrado"
    Assert-Contains $out "Asiento cancelado." "cancelacion del asiento descuadrado"
    $asientos = Get-Content (Join-Path $dir "data\asientos.txt") -Raw
    Assert-NotContains $asientos "Asiento descuadrado" "asiento descuadrado no persistido"

    # ------------------------------------------------------------------
    # Validaciones de fecha, cuenta y monto
    # ------------------------------------------------------------------
    $dir = New-TestDir "fecha"
    Copy-Data $dir
    $input = Write-Input $dir "entrada.txt" @("2", "31/02/2026", "05/11/2026", "Prueba fecha", "0", "11")
    $out = Invoke-App $dir $input
    Assert-Contains $out "Fecha invalida." "rechazo de fecha invalida"
    Assert-Contains $out "Asiento cancelado." "cancelacion tras fecha invalida"

    $dir = New-TestDir "validaciones"
    Copy-Data $dir
    $input = Write-Input $dir "entrada.txt" @("2", "05/11/2026", "Prueba validaciones", "9999", "1000", "1", "abc", "100", "3000", "2", "100", "N", "S", "11")
    $out = Invoke-App $dir $input
    Assert-Contains $out "La cuenta no existe." "rechazo de cuenta inexistente"
    Assert-Contains $out "Monto invalido." "rechazo de monto invalido"
    Assert-Contains $out "Asiento registrado exitosamente." "registro tras corregir entradas"

    # ------------------------------------------------------------------
    # Reportes con los datos demo
    # ------------------------------------------------------------------
    $dir = New-TestDir "reportes"
    Copy-Data $dir
    $input = Write-Input $dir "entrada.txt" @("3", "1000", "4", "5", "1000", "6", "7", "8", "11")
    $out = Invoke-App $dir $input
    Assert-Contains $out "SALDO: 11500,00 (Deudor)" "cuenta T de Caja"
    Assert-Contains $out "TOTAL GENERAL" "libro diario"
    Assert-Match $out "TOTAL GENERAL\s+14500,00\s+14500,00" "totales del libro diario"
    Assert-Contains $out "Saldo final: 11500,00 (Deudor)" "libro mayor de Caja"
    Assert-Contains $out "LA BALANZA DE COMPROBACION ESTA CUADRADA." "balanza cuadrada"
    Assert-Match $out "TOTALES:\s+14500,00\s+14500,00" "totales de la balanza"
    Assert-Match $out "Utilidad bruta:\s+3000,00" "utilidad bruta"
    Assert-Match $out "Utilidad neta:\s+1500,00" "utilidad neta"
    Assert-Contains $out "EL BALANCE CUADRA CORRECTAMENTE." "balance general cuadrado"
    Assert-Contains $out "Total Pasivo + Patrimonio + Resultado: 11500,00" "total del balance"
    $balance = Get-Content (Join-Path $dir "data\balance.txt") -Raw
    Assert-Match $balance "^\d{2}/\d{2}/\d{4}\|11500\.00\|0\.00\|11500\.00" "contenido de balance.txt"

    # ------------------------------------------------------------------
    # Respaldo manual (opcion 10)
    # ------------------------------------------------------------------
    $dir = New-TestDir "respaldo"
    Copy-Data $dir
    $input = Write-Input $dir "entrada.txt" @("10", "11")
    $out = Invoke-App $dir $input
    Assert-Contains $out "Respaldo creado en: backup\manual_" "respaldo manual"
    $carpeta = Get-ChildItem (Join-Path $dir "backup") -Directory -Filter "manual_*" | Select-Object -First 1
    if ($null -eq $carpeta) {
        throw "FALLO: no se creo la carpeta de respaldo manual"
    }
    foreach ($archivo in @("cuentas.txt", "asientos.txt")) {
        if (-not (Test-Path (Join-Path $carpeta.FullName $archivo))) {
            throw "FALLO: el respaldo no contiene $archivo"
        }
    }
    Write-Host "OK: respaldo manual completo"

    # ------------------------------------------------------------------
    # Archivo invalido: no se sobrescribe al salir
    # ------------------------------------------------------------------
    $dir = New-TestDir "invalidos"
    Copy-Data $dir
    $rutaAsientos = Join-Path $dir "data\asientos.txt"
    $contenido = Get-Content $rutaAsientos -Raw
    Write-Utf8 $rutaAsientos ("BASURA|linea invalida`r`n" + $contenido)
    $input = Write-Input $dir "entrada.txt" @("11")
    $out = Invoke-App $dir $input
    Assert-Contains $out "linea desconocida" "aviso de linea desconocida"
    $contenidoFinal = Get-Content $rutaAsientos -Raw
    Assert-Contains $contenidoFinal "BASURA|linea invalida" "asientos.txt no se sobrescribe"

    Write-Host ""
    Write-Host "TODAS LAS PRUEBAS PASARON."
} finally {
    if ($Keep) {
        Write-Host "Directorio de pruebas: $temp"
    } else {
        Remove-Item $temp -Recurse -Force -ErrorAction SilentlyContinue
    }
}
