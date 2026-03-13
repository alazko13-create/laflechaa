$ErrorActionPreference = 'Stop'
$projectDir = Split-Path -Parent $PSScriptRoot
Set-Location $projectDir

$gradleZip = 'C:\Users\StarNet-Network\Desktop\android\gradle-6.7.1-all.zip'
$gradleHome = 'C:\Users\StarNet-Network\Desktop\android\gradle-6.7.1'
$sdkDir = 'C:\Users\StarNet-Network\Desktop\android\Sdk\Sdk'

if (!(Test-Path "$sdkDir\platform-tools")) {
    Write-Error "No encuentro el SDK en $sdkDir"
}

if (!(Test-Path "$gradleHome\bin\gradle.bat")) {
    if (!(Test-Path $gradleZip)) {
        Write-Error "No encuentro Gradle zip en $gradleZip"
    }
    Expand-Archive -Path $gradleZip -DestinationPath 'C:\Users\StarNet-Network\Desktop\android' -Force
}

$env:ANDROID_HOME = $sdkDir
$env:ANDROID_SDK_ROOT = $sdkDir
$env:JAVA_HOME = ''
$env:GRADLE_OPTS = '-Djava.net.useSystemProxies=false -Djava.net.preferIPv4Stack=true'
$env:JAVA_TOOL_OPTIONS = ''
$env:_JAVA_OPTIONS = ''

& "$gradleHome\bin\gradle.bat" --no-daemon assembleDebug
