param(
    [string]$Serial = "",
    [string]$PackageName = "com.example.gemmacontrol",
    [string]$OutputDir = ".device-validation"
)

$ErrorActionPreference = "Stop"

function Invoke-Adb {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    $adbArgs = @()
    if ($Serial.Trim().Length -gt 0) {
        $adbArgs += @("-s", $Serial.Trim())
    }
    $adbArgs += $Arguments

    $output = & adb @adbArgs 2>&1
    if ($LASTEXITCODE -ne 0 -and -not $AllowFailure) {
        throw "adb $($adbArgs -join ' ') failed: $output"
    }
    return $output
}

function Add-CommandSection {
    param(
        [System.Collections.Generic.List[string]]$Lines,
        [string]$Title,
        [string[]]$Arguments,
        [switch]$AllowFailure
    )

    $Lines.Add("")
    $Lines.Add("## $Title")
    $Lines.Add("")
    $Lines.Add("````text")
    $Lines.Add((Invoke-Adb -Arguments $Arguments -AllowFailure:$AllowFailure) -join [Environment]::NewLine)
    $Lines.Add("````")
}

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$reportPath = Join-Path $OutputDir "device-safety-snapshot-$timestamp.md"
$lines = [System.Collections.Generic.List[string]]::new()

$lines.Add("# Device Safety Snapshot")
$lines.Add("")
$lines.Add("Generated: $(Get-Date -Format o)")
$lines.Add("Package: ``$PackageName``")
$lines.Add("Serial override: ``$Serial``")
$lines.Add("")
$lines.Add("Privacy boundary: this report avoids screenshots, raw notifications, WhatsApp message bodies, sender names, group names, phone numbers, and full logcat dumps.")

Add-CommandSection $lines "Connected Devices" @("devices", "-l")
Add-CommandSection $lines "Device Identity" @("shell", "getprop", "ro.product.manufacturer")
Add-CommandSection $lines "Device Model" @("shell", "getprop", "ro.product.model")
Add-CommandSection $lines "Android Release" @("shell", "getprop", "ro.build.version.release")
Add-CommandSection $lines "Android SDK" @("shell", "getprop", "ro.build.version.sdk")
Add-CommandSection $lines "Security Patch" @("shell", "getprop", "ro.build.version.security_patch")
Add-CommandSection $lines "HyperOS Version" @("shell", "getprop", "ro.mi.os.version.name") -AllowFailure
Add-CommandSection $lines "GemmaControl Package Path" @("shell", "pm", "path", $PackageName) -AllowFailure
Add-CommandSection $lines "WhatsApp Package Presence" @("shell", "pm", "list", "packages", "com.whatsapp") -AllowFailure
Add-CommandSection $lines "Notification Listener Setting" @("shell", "settings", "get", "secure", "enabled_notification_listeners") -AllowFailure
Add-CommandSection $lines "App Ops Summary" @("shell", "cmd", "appops", "get", $PackageName) -AllowFailure
Add-CommandSection $lines "Battery" @("shell", "dumpsys", "battery") -AllowFailure
Add-CommandSection $lines "Thermal Service" @("shell", "dumpsys", "thermalservice") -AllowFailure
Add-CommandSection $lines "App Process Id" @("shell", "pidof", $PackageName) -AllowFailure
Add-CommandSection $lines "App Memory" @("shell", "dumpsys", "meminfo", $PackageName) -AllowFailure

Set-Content -Path $reportPath -Value $lines -Encoding UTF8
Write-Host "Wrote privacy-safe device snapshot: $reportPath"
