# Fetch spell data from Noita Wiki
$base = "https://noita.wiki.gg"
$listFile = "E:\MC-noita\docs\法术链接列表.txt"
$outFile = "E:\MC-noita\docs\法术数据.md"

$urls = Get-Content $listFile -Encoding UTF8 | Where-Object {
    $_ -match '^/zh/wiki/' -and
    $_ -notmatch '(伤害类型|世界观|地图|天赋|字母|宝箱|初始|可解锁|专家攻略|基础攻略|世界观的|条件要求)' -and
    $_ -notmatch '#'
} | ForEach-Object { $_.Trim() }

$total = $urls.Count
Write-Host "Fetching $total spell pages..."

$allData = @()
$success = 0
$fail = 0

foreach ($url in $urls) {
    $name = $url -replace '/zh/wiki/', '' -replace '##', ''
    $fullUrl = $base + $url

    try {
        $resp = Invoke-WebRequest -Uri $fullUrl -UseBasicParsing -TimeoutSec 15
        $html = $resp.Content

        # Extract spell data from the HTML
        $spellData = @{ Name = $name; URL = $fullUrl }

        # Look for infobox data
        if ($html -match 'description.*?>(.*?)</td' ) {
            $spellData.Description = $Matches[1].Trim() -replace '<[^>]+>', ''
        }
        if ($html -match 'mana.*?(\d+)') { $spellData.Mana = $Matches[1] }
        if ($html -match 'cast.?delay.*?([\+\-]\d+\.?\d*)') { $spellData.CastDelay = $Matches[1] }
        if ($html -match 'uses.*?(\d+)') { $spellData.Uses = $Matches[1] }
        if ($html -match 'speed.*?(\d+)') { $spellData.Speed = $Matches[1] }
        if ($html -match 'lifetime.*?(\d+)') { $spellData.Lifetime = $Matches[1] }
        if ($html -match 'spread.*?([\+\-]\d+\.?\d*)') { $spellData.Spread = $Matches[1] }
        if ($html -match 'explosion.*?radius.*?(\d+)') { $spellData.ExplosionRadius = $Matches[1] }
        if ($html -match 'critical.*?(\d+)') { $spellData.Critical = $Matches[1] }

        # Extract image URLs
        $imgs = [regex]::Matches($html, 'src="(/zh/images/[^"]+|https://noita\.wiki\.gg/images/[^"]+)"')
        $spellData.Images = ($imgs | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique) -join ', '

        $allData += $spellData
        $success++
    } catch {
        $fail++
    }

    if ($success % 20 -eq 0) {
        Write-Host "Progress: $success/$total fetched, $fail failed"
    }

    Start-Sleep -Milliseconds 500
}

Write-Host "`nDone! Success: $success, Failed: $fail"

# Build markdown output
$md = "# Noita 法术数据库`n`n"
$md += "## 统计`n`n"
$md += "- 总计: $total 个法术链接`n"
$md += "- 成功获取: $success`n"
$md += "- 失败: $fail`n`n"
$md += "---`n`n"

# Group by category (from the main page)
$categories = @{
    "投射物" = @()
    "静态投射物" = @()
    "投射修正" = @()
    "多重释放" = @()
    "材料" = @()
    "其他" = @()
    "实用" = @()
    "被动" = @()
}

# We already know categories from the main page fetch
# Just list all spells with their data
$md += "## 法术列表`n`n"

foreach ($spell in $allData | Sort-Object Name) {
    $md += "### $($spell.Name)`n`n"
    if ($spell.Description) { $md += "- **描述**: $($spell.Description)`n" }
    if ($spell.Mana) { $md += "- **法力消耗**: $($spell.Mana)`n" }
    if ($spell.CastDelay) { $md += "- **施法延迟**: $($spell.CastDelay)`n" }
    if ($spell.Uses) { $md += "- **使用次数**: $($spell.Uses)`n" }
    if ($spell.Speed) { $md += "- **速度**: $($spell.Speed)`n" }
    if ($spell.Lifetime) { $md += "- **持续时间**: $($spell.Lifetime)`n" }
    if ($spell.Spread) { $md += "- **散射**: $($spell.Spread)`n" }
    if ($spell.ExplosionRadius) { $md += "- **爆炸半径**: $($spell.ExplosionRadius)`n" }
    if ($spell.Critical) { $md += "- **暴击率**: $($spell.Critical)`n" }
    if ($spell.Images) { $md += "- **图片**: $($spell.Images)`n" }
    $md += "`n"
}

$md | Out-File $outFile -Encoding UTF8
Write-Host "Output written to $outFile"
