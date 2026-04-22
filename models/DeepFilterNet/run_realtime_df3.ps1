param(
    [string]$InputDevice,
    [string]$OutputDevice,
    [int]$BlockSize = 480,
    [int]$ContextBlocks = 20,
    [switch]$PostFilter,
    [switch]$ListDevices
)

$ErrorActionPreference = "Stop"

$repoRoot = $PSScriptRoot
$scriptPath = Join-Path $repoRoot "realtime_df3.py"

if (-not (Test-Path -LiteralPath $scriptPath)) {
    throw "Script not found: $scriptPath"
}

$args = @($scriptPath)

if ($ListDevices) {
    $args += "--list-devices"
}
else {
    if ([string]::IsNullOrWhiteSpace($InputDevice) -or [string]::IsNullOrWhiteSpace($OutputDevice)) {
        throw "Please provide -InputDevice and -OutputDevice, or use -ListDevices first."
    }
    $args += @(
        "--input-device", $InputDevice,
        "--output-device", $OutputDevice,
        "--blocksize", $BlockSize,
        "--context-blocks", $ContextBlocks
    )
    if ($PostFilter) {
        $args += "--pf"
    }
}

python @args
