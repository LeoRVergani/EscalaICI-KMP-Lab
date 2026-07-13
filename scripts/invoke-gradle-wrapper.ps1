param(
    [Parameter(
        Mandatory = $false,
        ValueFromRemainingArguments = $true
    )]
    [string[]] $GradleArgs
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$settingsFile = Join-Path $projectRoot "settings.gradle.kts"
$composeAppDirectory = Join-Path $projectRoot "composeApp"
$wrapperJar = Join-Path $projectRoot "gradle\wrapper\gradle-wrapper.jar"

if (-not (Test-Path -LiteralPath $settingsFile) -or
    -not (Test-Path -LiteralPath $composeAppDirectory)) {
    throw "Raiz do projeto Escala ICI KMP não encontrada: $projectRoot"
}

if (-not (Test-Path -LiteralPath $wrapperJar)) {
    throw "gradle-wrapper.jar não encontrado: $wrapperJar"
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java não foi encontrado no PATH."
}

Push-Location $projectRoot
try {
    & java `
        -classpath $wrapperJar `
        org.gradle.wrapper.GradleWrapperMain `
        @GradleArgs
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

exit $exitCode
