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
    '(?:\bworld|this\.getWorld\(\))\.raycast\(',
    'FallingBlockEntity\.spawnFromBlock\(',
    '\.requestTeleport\('
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

# Bomb aftermath and the temporary-light manager both perform bounded block
# inspection as well as writes. Keep their reads behind WorldQueryService so
# an innocent-looking range loop cannot inspect or load an arbitrary chunk.
$scopedWorldPolicySources = @(
    (Join-Path $entityRoot "BombEntity.java"),
    (Join-Path $ProjectRoot "src/main/java/com/mcnoita/world/NoitaTemporaryLightManager.java")
)
$worldReadBypasses = @()
foreach ($sourcePath in $scopedWorldPolicySources) {
    if (-not (Test-Path -LiteralPath $sourcePath)) {
        throw "Expected world-policy source is missing: $sourcePath"
    }
    $fileName = Split-Path -Leaf $sourcePath
    $lineNumber = 0
    Get-Content -LiteralPath $sourcePath | ForEach-Object {
        $lineNumber++
        $line = $_
        if ($line -match '\.getBlockState\(' -and
            $line -notmatch 'WorldMutationService\.' -and $line -notmatch 'WorldQueryService\.') {
            $worldReadBypasses += "{0}:{1}" -f $fileName, $lineNumber
        }
        if ($line -match '\.setBlockState\(' -and
            $line -notmatch 'WorldMutationService\.') {
            $worldReadBypasses += "{0}:{1}" -f $fileName, $lineNumber
        }
    }
}

if ($worldReadBypasses.Count -gt 0) {
    throw "Scoped spell world code bypasses WorldMutationService/WorldQueryService: $($worldReadBypasses -join ', ')"
}

# Damage and healing follow the same boundary rule. Entity implementations may
# choose a frozen profile or a legacy projectile projection, but only the
# services create DamageSource instances, reset Noita-style hurt immunity, or
# apply the owner/team policy.
$damageBypasses = @()
$damagePatterns = @(
    'NoitaSpellDamage\.apply\(',
    '\.indirectMagic\(',
    '\.heal\(',
    '\.damage\('
)
Get-ChildItem -File $entityRoot -Filter "*.java" | ForEach-Object {
    $fileName = $_.Name
    $lineNumber = 0
    Get-Content -LiteralPath $_.FullName | ForEach-Object {
        $lineNumber++
        $line = $_
        if (($damagePatterns | Where-Object { $line -match $_ }).Count -gt 0 -and
            $line -notmatch 'SpellDamageService\.' -and $line -notmatch 'HealingService\.heal\(' -and
            $line -notmatch '\b(?:payload|plan|profile)\.damage\(') {
            $damageBypasses += "{0}:{1}" -f $fileName, $lineNumber
        }
    }
}

if ($damageBypasses.Count -gt 0) {
    throw "Spell entity bypasses SpellDamageService/HealingService: $($damageBypasses -join ', ')"
}
