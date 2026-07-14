$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$offenders = Get-ChildItem -Recurse -File (Join-Path $ProjectRoot "src/main/java") -Filter "*.java" |
    Where-Object { (Get-Content -Raw -LiteralPath $_.FullName) -match "(?m)^import\s+net\.minecraft\.client\." }

if ($offenders.Count -gt 0) {
    $relative = $offenders | ForEach-Object { $_.FullName.Substring($ProjectRoot.Length + 1) }
    throw "Common source imports net.minecraft.client classes: $($relative -join ', ')"
}

# WorldMutationService is the sole low-level spell world-mutation boundary.
# Keep the large legacy projectile entities from gradually reintroducing raw
# explosions, block writes, entity spawns, or unbounded entity scans.
$entityRoot = Join-Path $ProjectRoot "src/main/java/com/mcnoita/entity"
$worldBypasses = @()
$worldMutationPatterns = @(
    '\.createExplosion\(',
    '\.breakBlock\(',
    '\.setBlockState\(',
    '\.spawnEntity\(',
    '\.getOtherEntities\(',
    'FallingBlockEntity\.spawnFromBlock\('
)
Get-ChildItem -File $entityRoot -Filter "*.java" | ForEach-Object {
    $fileName = $_.Name
    $lineNumber = 0
    Get-Content -LiteralPath $_.FullName | ForEach-Object {
        $lineNumber++
        $line = $_
        if (($worldMutationPatterns | Where-Object { $line -match $_ }).Count -gt 0 -and
            $line -notmatch 'WorldMutationService\.' -and $line -notmatch 'WorldQueryService\.') {
            $worldBypasses += "{0}:{1}" -f $fileName, $lineNumber
        }
    }
}

if ($worldBypasses.Count -gt 0) {
    throw "Spell entity bypasses WorldMutationService/WorldQueryService: $($worldBypasses -join ', ')"
}
