$ErrorActionPreference = 'Stop'

$AndroidSdk = "$env:LOCALAPPDATA\Android\Sdk"
$CmdlineRoot = Join-Path $AndroidSdk 'cmdline-tools'
$CmdlineLatest = Join-Path $CmdlineRoot 'latest'
$ZipPath = Join-Path $env:TEMP 'commandlinetools-win.zip'
$DownloadUrl = 'https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip'

Write-Host "==> Verificando Java 17"
try {
    java -version
} catch {
    Write-Host 'Java no esta instalado o no esta en PATH.'
    Write-Host 'Instala Microsoft OpenJDK 17 o Temurin 17 y vuelve a correr este script.'
    Write-Host 'Ejemplo con winget:'
    Write-Host 'winget install Microsoft.OpenJDK.17'
    exit 1
}

New-Item -ItemType Directory -Force -Path $CmdlineRoot | Out-Null

if (!(Test-Path (Join-Path $CmdlineLatest 'bin\sdkmanager.bat'))) {
    Write-Host "==> Descargando Android Command-line Tools"
    Invoke-WebRequest -Uri $DownloadUrl -OutFile $ZipPath
    $TempExtract = Join-Path $env:TEMP 'android_cmdline_extract'
    if (Test-Path $TempExtract) { Remove-Item $TempExtract -Recurse -Force }
    Expand-Archive -Path $ZipPath -DestinationPath $TempExtract -Force
    if (Test-Path $CmdlineLatest) { Remove-Item $CmdlineLatest -Recurse -Force }
    Move-Item -Path (Join-Path $TempExtract 'cmdline-tools') -Destination $CmdlineLatest
}

$env:ANDROID_SDK_ROOT = $AndroidSdk
$env:ANDROID_HOME = $AndroidSdk
$env:Path = "$CmdlineLatest\bin;$AndroidSdk\platform-tools;$env:Path"

Write-Host "==> Instalando paquetes requeridos del SDK"
& "$CmdlineLatest\bin\sdkmanager.bat" --sdk_root=$AndroidSdk "platform-tools" "platforms;android-34" "build-tools;34.0.0" | Out-Host

Write-Host "==> Aceptando licencias"
cmd /c "for /L %i in (1,1,20) do @echo y" | & "$CmdlineLatest\bin\sdkmanager.bat" --sdk_root=$AndroidSdk --licenses | Out-Host

Write-Host ''
Write-Host 'Listo. Ahora abre otra terminal en VS Code y ejecuta:'
Write-Host '.\scripts\build-debug.ps1'
