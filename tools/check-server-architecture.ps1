$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$offenders = Get-ChildItem -Recurse -File (Join-Path $ProjectRoot "src/main/java") -Filter "*.java" |
    Where-Object { (Get-Content -Raw -LiteralPath $_.FullName) -match "(?m)^import\s+net\.minecraft\.client\." }

if ($offenders.Count -gt 0) {
    $relative = $offenders | ForEach-Object { $_.FullName.Substring($ProjectRoot.Length + 1) }
    throw "Common source imports net.minecraft.client classes: $($relative -join ', ')"
}
