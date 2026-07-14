[CmdletBinding()]
param(
    [switch]$Check
)

$ErrorActionPreference = "Stop"

$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$CatalogPath = Join-Path $ProjectRoot "docs/baseline/spell-catalog.json"
$ReportFileName = [string]::Concat([char]0x5B9E, [char]0x73B0, [char]0x8986, [char]0x76D6, [char]0x7387, ".md")
$ReportPath = Join-Path $ProjectRoot (Join-Path "docs" $ReportFileName)
$AllowlistPath = Join-Path $ProjectRoot "docs/baseline/resource-allowlist.json"
$ItemSourcePath = Join-Path $ProjectRoot "src/main/java/com/mcnoita/item/ModItems.java"
$AssetsRoot = Join-Path $ProjectRoot "src/main/resources/assets/mc-noita"
$ModelsRoot = Join-Path $AssetsRoot "models/item"
$TexturesRoot = Join-Path $AssetsRoot "textures/item"

$ExpectedCategoryCounts = [ordered]@{
    projectile = 122
    static_projectile = 45
    projectile_modifier = 143
    other = 16
    multicast = 1
    utility = 1
}

$NoitaIdAliases = @{
    ADD_MANA = "MANA_REDUCE"
}

$CatalogTestReferences = @{
    "mc-noita:spark_bolt" = @{ characterization = @("CHAR-001"); wiki_golden = @("WIKI-001"); game_test = @("GT-002"); manual = @() }
    "mc-noita:bomb" = @{ characterization = @("CHAR-002"); wiki_golden = @("WIKI-002"); game_test = @("GT-003"); manual = @() }
    "mc-noita:double_spell" = @{ characterization = @("CHAR-003"); wiki_golden = @("WIKI-003"); game_test = @("GT-004"); manual = @() }
    "mc-noita:damage" = @{ characterization = @("CHAR-004"); wiki_golden = @("WIKI-003"); game_test = @("GT-005"); manual = @() }
    "mc-noita:bullet_trigger" = @{ characterization = @("CHAR-005"); wiki_golden = @("WIKI-004"); game_test = @("GT-006"); manual = @() }
    "mc-noita:wand_refresh" = @{ characterization = @("CHAR-006"); wiki_golden = @("WIKI-005"); game_test = @("GT-007"); manual = @() }
    "mc-noita:alpha" = @{ characterization = @("CHAR-007"); wiki_golden = @("WIKI-006"); game_test = @("GT-008"); manual = @() }
    "mc-noita:gamma" = @{ characterization = @("CHAR-008"); wiki_golden = @("WIKI-007"); game_test = @("GT-009"); manual = @() }
    "mc-noita:tau" = @{ characterization = @("G04-GREEK"); wiki_golden = @("G04-GREEK"); game_test = @(); manual = @() }
    "mc-noita:omega" = @{ characterization = @("G04-GREEK"); wiki_golden = @("G04-GREEK"); game_test = @(); manual = @() }
    "mc-noita:mu" = @{ characterization = @("G04-GREEK"); wiki_golden = @("G04-GREEK"); game_test = @(); manual = @() }
    "mc-noita:phi" = @{ characterization = @("G04-GREEK"); wiki_golden = @("G04-GREEK"); game_test = @(); manual = @() }
    "mc-noita:sigma" = @{ characterization = @("G04-GREEK"); wiki_golden = @("G04-GREEK"); game_test = @(); manual = @() }
    "mc-noita:zeta" = @{ characterization = @("G04-ZETA"); wiki_golden = @("G04-ZETA"); game_test = @(); manual = @() }
    "mc-noita:divide_2" = @{ characterization = @("G04-DIVIDE"); wiki_golden = @("G04-DIVIDE"); game_test = @(); manual = @() }
    "mc-noita:divide_3" = @{ characterization = @("G04-DIVIDE"); wiki_golden = @("G04-DIVIDE"); game_test = @(); manual = @() }
    "mc-noita:divide_4" = @{ characterization = @("G04-DIVIDE"); wiki_golden = @("G04-DIVIDE"); game_test = @(); manual = @() }
    "mc-noita:divide_10" = @{ characterization = @("G04-DIVIDE"); wiki_golden = @("G04-DIVIDE"); game_test = @(); manual = @() }
    "mc-noita:add_trigger" = @{ characterization = @("G04-ADD-TRIGGER"); wiki_golden = @("G04-ADD-TRIGGER"); game_test = @("G04-GT-ADD-TRIGGER"); manual = @() }
    "mc-noita:add_timer" = @{ characterization = @("G04-ADD-TRIGGER"); wiki_golden = @("G04-ADD-TRIGGER"); game_test = @(); manual = @() }
    "mc-noita:add_death_trigger" = @{ characterization = @("G04-ADD-TRIGGER"); wiki_golden = @("G04-ADD-TRIGGER"); game_test = @(); manual = @() }
}

function Read-JsonObject {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return @{}
    }
    return ConvertTo-HashtableObject (Get-Content -Raw -LiteralPath $Path | ConvertFrom-Json)
}

function ConvertTo-HashtableObject {
    param([object]$Value)

    if ($null -eq $Value -or $Value -is [string] -or $Value -is [ValueType]) {
        return $Value
    }
    if ($Value -is [System.Collections.IDictionary]) {
        $result = [ordered]@{}
        foreach ($key in $Value.Keys) {
            $result[$key] = ConvertTo-HashtableObject $Value[$key]
        }
        return $result
    }
    if ($Value -is [System.Collections.IEnumerable]) {
        return @($Value | ForEach-Object { ConvertTo-HashtableObject $_ })
    }
    $result = [ordered]@{}
    foreach ($property in $Value.PSObject.Properties) {
        $result[$property.Name] = ConvertTo-HashtableObject $property.Value
    }
    return $result
}

function Get-JsonKeys {
    param([string]$Path)

    $keys = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)
    foreach ($match in [regex]::Matches((Get-Content -Raw -LiteralPath $Path), '(?m)^\s*"([^"]+)"\s*:')) {
        $keys.Add($match.Groups[1].Value) | Out-Null
    }
    return $keys
}

function Write-Utf8File {
    param([string]$Path, [string]$Content)

    $directory = Split-Path -Parent $Path
    [System.IO.Directory]::CreateDirectory($directory) | Out-Null
    $encoding = [System.Text.UTF8Encoding]::new($false)
    [System.IO.File]::WriteAllText($Path, $Content, $encoding)
}

function Convert-TextureReference {
    param([object]$TextureReference)

    if ($TextureReference -isnot [string] -or [string]::IsNullOrWhiteSpace($TextureReference)) {
        return $null
    }
    if ($TextureReference.StartsWith("mc-noita:")) {
        return $TextureReference.Substring("mc-noita:".Length) + ".png"
    }
    return $null
}

function Get-SpellDeclarations {
    param([string]$Source)

    $prefix = "public static final "
    $start = $Source.IndexOf($prefix, [System.StringComparison]::Ordinal)
    $declarations = [System.Collections.Generic.List[hashtable]]::new()
    while ($start -ge 0) {
        $next = $Source.IndexOf($prefix, $start + $prefix.Length, [System.StringComparison]::Ordinal)
        if ($next -lt 0) {
            $next = $Source.IndexOf("private ModItems()", $start, [System.StringComparison]::Ordinal)
        }
        if ($next -lt 0) {
            $next = $Source.Length
        }
        $declaration = $Source.Substring($start, $next - $start)
        $header = [regex]::Match($declaration, "public static final (NoitaProjectileSpellItem|NoitaSpellItem)\s+([A-Z0-9_]+)\s*=")
        if ($header.Success -and ($declaration.Contains("registerProjectile") -or $declaration.Contains("registerStaticProjectile") -or $declaration.Contains("registerModifier") -or $declaration.Contains('register("'))) {
            $declarations.Add(@{
                field = $header.Groups[2].Value
                text = $declaration
            })
        }
        $start = $Source.IndexOf($prefix, $start + $prefix.Length, [System.StringComparison]::Ordinal)
    }
    return $declarations
}

function New-SpellCatalog {
    $source = Get-Content -Raw -LiteralPath $ItemSourcePath
    # The catalog only needs language keys. Scanning keys keeps this verifier useful
    # even when an in-progress translation value contains malformed user-owned text.
    $englishKeys = Get-JsonKeys (Join-Path $AssetsRoot "lang/en_us.json")
    $chineseKeys = Get-JsonKeys (Join-Path $AssetsRoot "lang/zh_cn.json")
    $spells = [System.Collections.Generic.List[hashtable]]::new()

    foreach ($declaration in Get-SpellDeclarations $source) {
        $field = $declaration.field
        $text = $declaration.text
        if ($text.Contains("registerProjectile")) {
            $category = "projectile"
        } elseif ($text.Contains("registerStaticProjectile")) {
            $category = "static_projectile"
        } elseif ($text.Contains("registerModifier")) {
            $category = "projectile_modifier"
        } else {
            $typeMatch = [regex]::Match($text, "NoitaSpellType\.([A-Z_]+)")
            $category = if ($typeMatch.Success) { $typeMatch.Groups[1].Value.ToLowerInvariant() } else { "other" }
        }

        $noitaId = if ($NoitaIdAliases.ContainsKey($field)) { $NoitaIdAliases[$field] } else { $field }
        $path = $null
        $projectileSpec = [regex]::Match($text, 'projectileSpec\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"')
        if ($projectileSpec.Success) {
            $noitaId = $projectileSpec.Groups[1].Value
            $path = $projectileSpec.Groups[2].Value
        } else {
            $pathMatch = [regex]::Match($text, 'register(?:Modifier)?\s*\(\s*"([^"]+)"')
            if ($pathMatch.Success) {
                $path = $pathMatch.Groups[1].Value
            }
        }
        if ([string]::IsNullOrWhiteSpace($path)) {
            throw "Cannot find registry path for $field"
        }

        $behaviorMatch = [regex]::Match($text, "NoitaProjectileBehavior\.([A-Z0-9_]+)")
        $behavior = if ($behaviorMatch.Success) { $behaviorMatch.Groups[1].Value } else { $null }
        $modifierEffects = @([regex]::Matches($text, "NoitaModifierEffect\.([A-Z0-9_]+)") | ForEach-Object { $_.Groups[1].Value })
        $modelFile = Join-Path $ModelsRoot "$path.json"
        $texturePath = $null
        $textureExists = $false
        $textureNote = $null
        if (Test-Path -LiteralPath $modelFile) {
            $model = Read-JsonObject $modelFile
            if ($null -ne $model.textures -and $null -ne $model.textures.layer0) {
                $texturePath = Convert-TextureReference $model.textures.layer0
            }
            if ($null -ne $texturePath) {
                $textureExists = Test-Path -LiteralPath (Join-Path $TexturesRoot $texturePath)
                if (-not $textureExists) {
                    $textureNote = "The model references $texturePath, which is missing from the current resource baseline."
                }
            } else {
                $textureNote = "The model uses a vanilla or shared texture and has no mc-noita:item layer0 resource."
            }
        } else {
            $textureNote = "The item model is missing, so no texture reference can be resolved."
        }

        $registryId = "mc-noita:$path"
        $verification = if ($CatalogTestReferences.ContainsKey($registryId)) {
            $CatalogTestReferences[$registryId]
        } else {
            @{ characterization = @(); wiki_golden = @(); game_test = @(); manual = @() }
        }
        $spells.Add([ordered]@{
            registry_id = $registryId
            noita_id = $noitaId
            category = $category
            wiki_source_url = "https://noita.wiki.gg/wiki/Spells"
            model = [ordered]@{
                path = "assets/mc-noita/models/item/$path.json"
                exists = (Test-Path -LiteralPath $modelFile)
            }
            texture = [ordered]@{
                path = if ($null -eq $texturePath) { $null } else { "assets/mc-noita/textures/$texturePath" }
                exists = $textureExists
                note = $textureNote
            }
            language = [ordered]@{
                english_key = "item.mc-noita.$path"
                english_exists = $englishKeys.Contains("item.mc-noita.$path")
                chinese_key = "item.mc-noita.$path"
                chinese_exists = $chineseKeys.Contains("item.mc-noita.$path")
            }
            implementation = [ordered]@{
                projectile_behavior = $behavior
                modifier_effects = $modifierEffects
                level = "approximate"
            }
            verification = $verification
            known_differences = "Baseline only: this is a Minecraft approximation and has not yet received spell-by-spell Wiki semantic verification."
            minecraft_adaptation = "Uses the current projectile, entity, or modifier executor; this baseline makes no pixel-level Noita fidelity claim."
            follow_up_goal = "G01"
        })
    }

    # Hashtable keys are not PowerShell object properties. Use an explicit
    # expression so catalog order is stable across processes and PS versions.
    $sortedSpells = @($spells | Sort-Object { $_["registry_id"] })
    $expectedSpellCount = ($ExpectedCategoryCounts.Values | Measure-Object -Sum).Sum
    if ($sortedSpells.Count -ne $expectedSpellCount) {
        throw "Expected $expectedSpellCount registered spells, found $($sortedSpells.Count) while parsing ModItems.java"
    }
    return [ordered]@{
        schema_version = 1
        generated_from = @(
            "src/main/java/com/mcnoita/item/ModItems.java",
            "src/main/resources/assets/mc-noita/lang/en_us.json",
            "src/main/resources/assets/mc-noita/lang/zh_cn.json"
        )
        generated_on = "deterministic-source-scan"
        spells = $sortedSpells
    }
}

function Write-SpellCoverageReport {
    param([hashtable]$Catalog)

    $lines = [System.Collections.Generic.List[string]]::new()
    $lines.Add("# Spell Implementation Coverage")
    $lines.Add("")
    $lines.Add("> Generated by `generateSpellCatalog` from `docs/baseline/spell-catalog.json`; do not edit manually.")
    $lines.Add("")
    $lines.Add("| Category | Registered | Approximate | Verified | Not implemented |")
    $lines.Add("|---|---:|---:|---:|---:|")
    foreach ($category in $ExpectedCategoryCounts.Keys) {
        $entries = @($Catalog.spells | Where-Object { $_.category -eq $category })
        $approximate = @($entries | Where-Object { $_.implementation.level -eq "approximate" }).Count
        $verified = @($entries | Where-Object { $_.implementation.level -eq "verified" }).Count
        $notImplemented = @($entries | Where-Object { $_.implementation.level -eq "not_implemented" }).Count
        $lines.Add("| $category | $($entries.Count) / $($ExpectedCategoryCounts[$category]) | $approximate | $verified | $notImplemented |")
    }
    $lines.Add("")
    $lines.Add("## Per-spell Baseline")
    $lines.Add("")
    $lines.Add("| Registry ID | Noita ID | Category | Level | Behavior / Effect | Verification IDs |")
    $lines.Add("|---|---|---|---|---|---|")
    foreach ($spell in $Catalog.spells) {
        $implementation = if ($null -ne $spell.implementation.projectile_behavior) {
            $spell.implementation.projectile_behavior
        } elseif ($spell.implementation.modifier_effects.Count -gt 0) {
            $spell.implementation.modifier_effects -join ", "
        } else {
            "-"
        }
        $verificationIds = @($spell.verification.characterization + $spell.verification.wiki_golden + $spell.verification.game_test + $spell.verification.manual) -join ", "
        $lines.Add("| $($spell.registry_id) | $($spell.noita_id) | $($spell.category) | $($spell.implementation.level) | $implementation | $verificationIds |")
    }
    Write-Utf8File $ReportPath (($lines -join [Environment]::NewLine) + [Environment]::NewLine)
}

function Test-SpellCatalog {
    Write-Verbose "Reading catalog"
    $catalog = Read-JsonObject $CatalogPath
    $spells = @($catalog.spells)
    $expectedSpellCount = ($ExpectedCategoryCounts.Values | Measure-Object -Sum).Sum
    if ($spells.Count -ne $expectedSpellCount) {
        throw "Spell catalog must contain $expectedSpellCount entries, found $($spells.Count)"
    }
    if (@($spells.registry_id | Select-Object -Unique).Count -ne $spells.Count) {
        throw "Spell catalog contains duplicate registry IDs"
    }
    foreach ($category in $ExpectedCategoryCounts.Keys) {
        $actual = @($spells | Where-Object { $_.category -eq $category }).Count
        if ($actual -ne $ExpectedCategoryCounts[$category]) {
            throw "Expected $($ExpectedCategoryCounts[$category]) $category spells, found $actual"
        }
    }
    foreach ($spell in $spells) {
        if (-not $spell.wiki_source_url.StartsWith("https://noita.wiki.gg/")) {
            throw "$($spell.registry_id) lacks a Noita Wiki source URL"
        }
        if (-not $spell.model.exists) {
            throw "$($spell.registry_id) lacks an item model"
        }
        if (-not $spell.language.english_exists -or -not $spell.language.chinese_exists) {
            throw "$($spell.registry_id) does not have paired English and Chinese language keys"
        }
        if (-not $spell.texture.exists -and [string]::IsNullOrWhiteSpace($spell.texture.note)) {
            throw "$($spell.registry_id) has no texture and no shared/missing texture explanation"
        }
        if ($spell.implementation.level -notin @("visual_only", "approximate", "verified", "not_implemented")) {
            throw "$($spell.registry_id) has an unknown implementation level $($spell.implementation.level)"
        }
    }
    Write-Verbose "Checking numeric values"
    if ((Get-Content -Raw -LiteralPath $CatalogPath) -match "(?i)(?:nan|infinity)") {
        throw "Spell catalog contains NaN or infinite numeric values"
    }
    if ((Get-Content -Raw -LiteralPath $ItemSourcePath) -match "(?:Float|Double)\.(?:NaN|POSITIVE_INFINITY|NEGATIVE_INFINITY)") {
        throw "ModItems.java declares a non-finite numeric value"
    }

    Write-Verbose "Checking allowlist"
    $allowlist = Read-JsonObject $AllowlistPath
    $allowedModels = @($allowlist.non_spell_models)
    $allowedTextures = @($allowlist.non_spell_textures)
    $catalogModels = @($spells | ForEach-Object { Split-Path -Leaf $_.model.path })
    $catalogTextures = @($spells | Where-Object { $null -ne $_.texture.path } | ForEach-Object { Split-Path -Leaf $_.texture.path })
    $orphanModels = @(Get-ChildItem -File $ModelsRoot -Filter "*.json" | Where-Object { $_.Name -notin $catalogModels } | ForEach-Object Name | Sort-Object)
    $orphanTextures = @(Get-ChildItem -File $TexturesRoot -Filter "*.png" | Where-Object { $_.Name -notin $catalogTextures } | ForEach-Object Name | Sort-Object)
    $missingAllowedModels = @($orphanModels | Where-Object { $_ -notin $allowedModels })
    $missingAllowedTextures = @($orphanTextures | Where-Object { $_ -notin $allowedTextures })
    $staleAllowedModels = @($allowedModels | Where-Object { $_ -notin $orphanModels })
    $staleAllowedTextures = @($allowedTextures | Where-Object { $_ -notin $orphanTextures })
    if ($missingAllowedModels.Count -gt 0 -or $missingAllowedTextures.Count -gt 0) {
        throw "Orphan resources must be explicitly allowlisted. Models=$($missingAllowedModels -join ','); textures=$($missingAllowedTextures -join ',')"
    }
    if ($staleAllowedModels.Count -gt 0 -or $staleAllowedTextures.Count -gt 0) {
        throw "Resource allowlist contains stale entries. Models=$($staleAllowedModels -join ','); textures=$($staleAllowedTextures -join ',')"
    }
}

if ($Check) {
    Test-SpellCatalog
    exit 0
}

$catalog = New-SpellCatalog
Write-Utf8File $CatalogPath ((ConvertTo-Json $catalog -Depth 12) + [Environment]::NewLine)
Write-SpellCoverageReport $catalog
