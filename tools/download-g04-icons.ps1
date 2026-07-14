[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$textureRoot = Join-Path $projectRoot "src/main/resources/assets/mc-noita/textures/item"
$sources = [ordered]@{
    "tau.png" = "https://noita.wiki.gg/wiki/Special:Redirect/file/Spell%20tau.png"
    "omega.png" = "https://noita.wiki.gg/wiki/Special:Redirect/file/Spell%20omega.png"
    "mu.png" = "https://noita.wiki.gg/wiki/Special:Redirect/file/Spell%20mu.png"
    "phi.png" = "https://noita.wiki.gg/wiki/Special:Redirect/file/Spell%20phi.png"
    "sigma.png" = "https://noita.wiki.gg/wiki/Special:Redirect/file/Spell%20sigma.png"
    "zeta.png" = "https://noita.wiki.gg/wiki/Special:Redirect/file/Spell%20zeta.png"
    "divide_2.png" = "https://noita.wiki.gg/wiki/Special:Redirect/file/Spell%20divide%202.png"
    "divide_3.png" = "https://noita.wiki.gg/wiki/Special:Redirect/file/Spell%20divide%203.png"
    "divide_4.png" = "https://noita.wiki.gg/wiki/Special:Redirect/file/Spell%20divide%204.png"
    "divide_10.png" = "https://noita.wiki.gg/wiki/Special:Redirect/file/Spell%20divide%2010.png"
    "add_trigger.png" = "https://noita.wiki.gg/wiki/Special:Redirect/file/Spell%20trigger.png"
    "add_timer.png" = "https://noita.wiki.gg/wiki/Special:Redirect/file/Spell%20timer.png"
    "add_death_trigger.png" = "https://noita.wiki.gg/wiki/Special:Redirect/file/Spell%20death%20trigger.png"
}

foreach ($entry in $sources.GetEnumerator()) {
    $destination = [System.IO.Path]::GetFullPath((Join-Path $textureRoot $entry.Key))
    if (-not $destination.StartsWith([System.IO.Path]::GetFullPath($textureRoot), [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing path outside the texture directory: $destination"
    }
    if (Test-Path -LiteralPath $destination) {
        throw "Refusing to overwrite existing texture: $destination"
    }
    $temporary = "$destination.download"
    try {
        Invoke-WebRequest -Uri $entry.Value -OutFile $temporary -MaximumRedirection 8 -UseBasicParsing
        $bytes = [System.IO.File]::ReadAllBytes($temporary)
        $validSignature = $bytes.Length -ge 8 -and
            $bytes[0] -eq 0x89 -and $bytes[1] -eq 0x50 -and $bytes[2] -eq 0x4E -and $bytes[3] -eq 0x47 -and
            $bytes[4] -eq 0x0D -and $bytes[5] -eq 0x0A -and $bytes[6] -eq 0x1A -and $bytes[7] -eq 0x0A
        if (-not $validSignature) {
            throw "Downloaded file is not a PNG: $($entry.Value)"
        }
        Move-Item -LiteralPath $temporary -Destination $destination
    } finally {
        if (Test-Path -LiteralPath $temporary) {
            Remove-Item -LiteralPath $temporary
        }
    }
    Start-Sleep -Milliseconds 400
}
