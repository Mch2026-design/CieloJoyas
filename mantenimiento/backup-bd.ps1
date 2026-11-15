<#
.SYNOPSIS
    Respaldo lógico preventivo de la base de datos PostgreSQL / Supabase mediante pg_dump (solo lectura).
.PARAMETER DryRun
    Valida la configuración y muestra el comando pg_dump saneado (sin exponer contraseñas) sin ejecutar el volcado.
.PARAMETER Destino
    Carpeta donde se guardarán los respaldos .dump (por defecto: mantenimiento/backups).
#>
param(
    [switch]$DryRun,
    [string]$Destino = (Join-Path $PSScriptRoot 'backups')
)

$ErrorActionPreference = 'Stop'
$raizProyecto = Split-Path -Parent $PSScriptRoot
$archivoEnv = Join-Path $raizProyecto '.env'

if (Test-Path $archivoEnv) {
    Get-Content $archivoEnv | Where-Object { $_ -and -not $_.TrimStart().StartsWith('#') } | ForEach-Object {
        $partes = $_ -split '=', 2
        if ($partes.Length -eq 2 -and -not [Environment]::GetEnvironmentVariable($partes[0].Trim())) {
            [Environment]::SetEnvironmentVariable($partes[0].Trim(), $partes[1].Trim(), 'Process')
        }
    }
}

$dbUrl = if ($env:DB_URL) { $env:DB_URL } else { 'jdbc:postgresql://localhost:5432/maricielojoyas' }
$dbUser = if ($env:DB_USER) { $env:DB_USER } else { 'postgres' }
$dbPass = $env:DB_PASSWORD

if ($dbUrl -notmatch '^jdbc:postgresql://([^:/?]+)(?::(\d+))?/([^?]+)') {
    Write-Error 'DB_URL no tiene el formato esperado jdbc:postgresql://host:puerto/base.'
    exit 1
}

$pgHost = $Matches[1]
$pgPort = if ($Matches[2]) { $Matches[2] } else { '5432' }
$pgDatabase = $Matches[3]
$marcaTiempo = Get-Date -Format 'yyyyMMdd-HHmmss'
$archivoSalida = Join-Path $Destino "cielojoyas-$marcaTiempo.dump"

$pgDump = Get-Command 'pg_dump' -ErrorAction SilentlyContinue
if (-not $pgDump -and -not $DryRun) {
    Write-Error 'No se encontró pg_dump en PATH. Instala PostgreSQL Client Tools (pg_dump 17) o usa -DryRun para validar parámetros.'
    exit 1
}

$argumentos = @(
    '--format=custom',
    '--no-owner',
    '--no-privileges',
    "--host=$pgHost",
    "--port=$pgPort",
    "--username=$dbUser",
    "--dbname=$pgDatabase",
    "--file=$archivoSalida"
)

if ($DryRun) {
    Write-Host "[DryRun] Archivo destino: $archivoSalida"
    Write-Host "[DryRun] Comando: pg_dump $($argumentos -join ' ') (PGPASSWORD protegido en memoria)"
    exit 0
}

if (-not (Test-Path $Destino)) {
    New-Item -ItemType Directory -Path $Destino -Force | Out-Null
}

try {
    if ($dbPass) { $env:PGPASSWORD = $dbPass }
    & $pgDump.Source @argumentos
    if ($LASTEXITCODE -ne 0) {
        Write-Error "pg_dump finalizó con código de salida $LASTEXITCODE."
        exit $LASTEXITCODE
    }
    Write-Host "Respaldo generado correctamente en: $archivoSalida"
} finally {
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
}
