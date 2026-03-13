$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$androidHome = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
if (!(Test-Path $androidHome)) {
  throw "No encontre el SDK en $androidHome."
}

$env:ANDROID_HOME = $androidHome
$env:ANDROID_SDK_ROOT = $androidHome
$env:Path = "$androidHome\platform-tools;$androidHome\cmdline-tools\latest\bin;$env:Path"

$gradleVersion = '8.7'
$gradleBase = Join-Path $env:USERPROFILE '.gradle-local'
$gradleHome = Join-Path $gradleBase "gradle-$gradleVersion"
$gradleZip = Join-Path $gradleBase "gradle-$gradleVersion-bin.zip"
$gradleBat = Join-Path $gradleHome 'bin\gradle.bat'

New-Item -ItemType Directory -Force -Path $gradleBase | Out-Null
if (!(Test-Path $gradleBat)) {
  Write-Host "==> Descargando Gradle $gradleVersion"
  Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip" -OutFile $gradleZip
  Write-Host "==> Descomprimiendo Gradle"
  Expand-Archive -LiteralPath $gradleZip -DestinationPath $gradleBase -Force
}

Write-Host "==> Compilando APK debug"
Push-Location $projectRoot
& $gradleBat --no-daemon assembleDebug
$exitCode = $LASTEXITCODE
Pop-Location
if ($exitCode -ne 0) { exit $exitCode }

$apk = Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (Test-Path $apk) {
  Write-Host "APK generada en: $apk"
} else {
  Write-Host 'La compilacion termino, pero no encontre la APK esperada.'
  exit 1
}
