$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$settingsFile = Join-Path $projectRoot "settings.gradle.kts"
$composeAppDirectory = Join-Path $projectRoot "composeApp"
$gradleInvoker = Join-Path $PSScriptRoot "invoke-gradle-wrapper.ps1"
$distributionDirectory = Join-Path $projectRoot "composeApp\build\dist\wasmJs\productionExecutable"
$cloudflareSourceDirectory = Join-Path $projectRoot "web\cloudflare"
$cloudflareDirectory = Join-Path $projectRoot "cloudflare-dist"

if (-not (Test-Path -LiteralPath $settingsFile) -or
    -not (Test-Path -LiteralPath $composeAppDirectory)) {
    throw "Execute este script dentro do projeto Escala ICI KMP."
}

& $gradleInvoker :composeApp:wasmJsTest
if ($LASTEXITCODE -ne 0) {
    throw "Os testes Web falharam com código $LASTEXITCODE."
}

& $gradleInvoker :composeApp:composeCompatibilityBrowserDistribution
if ($LASTEXITCODE -ne 0) {
    throw "A distribuição Web falhou com código $LASTEXITCODE."
}

if (-not (Test-Path -LiteralPath $distributionDirectory -PathType Container)) {
    throw "Distribuição Web não encontrada: $distributionDirectory"
}

if (Test-Path -LiteralPath $cloudflareDirectory) {
    $resolvedCloudflareDirectory = (Resolve-Path -LiteralPath $cloudflareDirectory).Path
    $expectedCloudflareDirectory = [System.IO.Path]::GetFullPath($cloudflareDirectory)
    if ($resolvedCloudflareDirectory -ne $expectedCloudflareDirectory -or
        -not $resolvedCloudflareDirectory.StartsWith($projectRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Pasta temporária fora da raiz esperada: $resolvedCloudflareDirectory"
    }
    Remove-Item -LiteralPath $resolvedCloudflareDirectory -Recurse -Force
}

New-Item -ItemType Directory -Path $cloudflareDirectory | Out-Null
Copy-Item -Path (Join-Path $distributionDirectory "*") -Destination $cloudflareDirectory -Recurse -Force

foreach ($cloudflareFile in @("_headers", "_redirects")) {
    $source = Join-Path $cloudflareSourceDirectory $cloudflareFile
    if (Test-Path -LiteralPath $source) {
        Copy-Item -LiteralPath $source -Destination $cloudflareDirectory -Force
    }
}

$indexFile = Join-Path $cloudflareDirectory "index.html"
$javascriptFile = Join-Path $cloudflareDirectory "composeApp.js"
$wasmFiles = @(Get-ChildItem -LiteralPath $cloudflareDirectory -Recurse -Filter "*.wasm" -File)

if (-not (Test-Path -LiteralPath $indexFile -PathType Leaf)) {
    throw "cloudflare-dist/index.html não foi gerado."
}
if (-not (Test-Path -LiteralPath $javascriptFile -PathType Leaf)) {
    throw "cloudflare-dist/composeApp.js não foi gerado."
}
if ($wasmFiles.Count -eq 0) {
    throw "Nenhum arquivo Wasm foi gerado em cloudflare-dist."
}

Write-Host "Distribuição WEB_WASM pronta em: $cloudflareDirectory"
Get-ChildItem -LiteralPath $cloudflareDirectory -Recurse -File |
    Select-Object FullName, Length
