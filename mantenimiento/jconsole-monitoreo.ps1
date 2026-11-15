<#
.SYNOPSIS
    Localiza jconsole.exe y detecta procesos Java activos para monitoreo JMX local de Cielo Joyas.
.PARAMETER DryRun
    Solo inspecciona jconsole.exe y lista los procesos Java sin abrir la ventana gráfica.
#>
param(
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'

function Buscar-HerramientaJdk {
    param([string]$NombreEjecutable)

    $enPath = Get-Command $NombreEjecutable -ErrorAction SilentlyContinue
    if ($enPath) { return $enPath.Source }

    $rutas = @()
    if ($env:JAVA_HOME) { $rutas += Join-Path $env:JAVA_HOME "bin\$NombreEjecutable" }
    $rutas += Get-ChildItem -Path 'C:\Program Files\Java', "$env:USERPROFILE\.jdks" -Filter $NombreEjecutable -Recurse -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty FullName

    return $rutas | Where-Object { Test-Path $_ } | Select-Object -First 1
}

$jconsole = Buscar-HerramientaJdk 'jconsole.exe'
$jps = Buscar-HerramientaJdk 'jps.exe'

if (-not $jconsole) {
    Write-Error 'No se encontró jconsole.exe en JAVA_HOME ni en las instalaciones locales del JDK.'
    exit 1
}

Write-Host "JConsole encontrado en: $jconsole"

$pidCielo = $null
if ($jps) {
    Write-Host "`nProcesos Java locales detectados:"
    $procesos = & $jps -l 2>$null
    $procesos | ForEach-Object { Write-Host "  $_" }
    $lineaCielo = $procesos | Where-Object { $_ -match 'MaricielojoyasApplication|maricielojoyas' } | Select-Object -First 1
    if ($lineaCielo) {
        $pidCielo = ($lineaCielo -split '\s+')[0]
        Write-Host "`nProceso de Cielo Joyas detectado con PID: $pidCielo"
    } else {
        Write-Host "`nNo se detectó MaricielojoyasApplication en ejecución (puede iniciar la app primero con .\mvnw.cmd spring-boot:run)."
    }
}

if ($DryRun) {
    if ($pidCielo) {
        Write-Host "[DryRun] Comando sugerido: `"$jconsole`" $pidCielo"
    } else {
        Write-Host "[DryRun] Comando sugerido: `"$jconsole`""
    }
    exit 0
}

if ($pidCielo) {
    Start-Process -FilePath $jconsole -ArgumentList $pidCielo
} else {
    Start-Process -FilePath $jconsole
}
