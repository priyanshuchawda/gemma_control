param(
    [string]$Serial = "",
    [string]$OutputFile = "",
    [switch]$ClearFirst
)

$ErrorActionPreference = "Stop"

$tags = @(
    "GemmaControl",
    "WhatsAppNotificationListener",
    "VoiceAssistantVM",
    "GemmaModelManager",
    "LiteRtGemmaEngine",
    "ModelRuntimeBenchmark",
    "XiaomiReliability"
)

function Invoke-Adb {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments
    )

    $adbArgs = @()
    if ($Serial.Trim().Length -gt 0) {
        $adbArgs += @("-s", $Serial.Trim())
    }
    $adbArgs += $Arguments

    & adb @adbArgs
    if ($LASTEXITCODE -ne 0) {
        throw "adb $($adbArgs -join ' ') failed"
    }
}

$filterArgs = @("logcat", "-v", "time")
foreach ($tag in $tags) {
    $filterArgs += "$tag`:V"
}
$filterArgs += "*:S"

Write-Host "Privacy boundary: filtered app logs only. Do not commit raw logs without manual review."
Write-Host "Tags: $($tags -join ', ')"

if ($ClearFirst) {
    Invoke-Adb -Arguments @("logcat", "-c")
}

if ($OutputFile.Trim().Length -gt 0) {
    $outputDirectory = Split-Path -Parent $OutputFile
    if ($outputDirectory.Trim().Length -gt 0) {
        New-Item -ItemType Directory -Force -Path $outputDirectory | Out-Null
    }
    Write-Host "Writing filtered logcat to $OutputFile"
    Invoke-Adb -Arguments $filterArgs | Tee-Object -FilePath $OutputFile
} else {
    Invoke-Adb -Arguments $filterArgs
}
