param(
    [string]$PackageName = "com.example.gemmacontrol"
)

$ErrorActionPreference = "Stop"

Write-Host "Reading debug-only spoken output from app-private cache." -ForegroundColor Yellow
Write-Host "Treat this as private local test evidence. Do not paste private WhatsApp text into docs, issues, PRs, or final reports." -ForegroundColor Yellow
Write-Host ""

adb exec-out run-as $PackageName cat cache/debug/last_spoken_output.txt
