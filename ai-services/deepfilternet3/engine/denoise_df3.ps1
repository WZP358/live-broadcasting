param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$InputFile,

    [Parameter(Position = 1)]
    [string]$OutputDir = "",

    [switch]$PostFilter
)

$ErrorActionPreference = "Stop"

$repoRoot = $PSScriptRoot
$projectDir = Join-Path $repoRoot "DeepFilterNet"
$modelDir = Join-Path $repoRoot "models\\DeepFilterNet3"

if (-not (Test-Path -LiteralPath $InputFile)) {
    throw "Input file not found: $InputFile"
}

if (-not (Test-Path -LiteralPath $modelDir)) {
    throw "Model directory not found: $modelDir"
}

if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Split-Path -Parent (Resolve-Path -LiteralPath $InputFile)
}

if (-not (Test-Path -LiteralPath $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

$resolvedInput = (Resolve-Path -LiteralPath $InputFile).Path
$resolvedOutput = (Resolve-Path -LiteralPath $OutputDir).Path

$args = @(
    "-m", "df.enhance",
    "-m", $modelDir,
    "-o", $resolvedOutput,
    $resolvedInput
)

if ($PostFilter) {
    $args += "--pf"
}

Push-Location $projectDir
try {
    python @args
}
finally {
    Pop-Location
}
