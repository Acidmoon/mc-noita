[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$ServerDirectory = Join-Path $ProjectRoot "build/smoke-server"
$LogPath = Join-Path $ServerDirectory "smoke-server.log"
$MinecraftLogPath = Join-Path $ServerDirectory "logs/latest.log"
$DiagnosticPath = Join-Path $ServerDirectory "smoke-server-error.log"
$TracePath = Join-Path $ServerDirectory "smoke-server-trace.log"
$Timeout = [TimeSpan]::FromSeconds(75)
$StopTimeout = [TimeSpan]::FromSeconds(20)
$RconPort = 25576
$RconPassword = "mc-noita-g00-smoke"
$ReadyPattern = 'Done \([0-9.]+s\)! For help, type "help"'
$FailurePatterns = @(
    "net\.minecraft\.client\.",
    "Mixin apply failed",
    "MixinApplyError",
    "Duplicate registration",
    "Registry.*already contains",
    "Exception in thread",
    "Entrypoint.*threw",
    "Could not execute entrypoint"
)

trap {
    [System.IO.Directory]::CreateDirectory($ServerDirectory) | Out-Null
    [System.IO.File]::WriteAllText($DiagnosticPath, ($_ | Out-String), [System.Text.UTF8Encoding]::new($false))
    exit 1
}

[System.IO.Directory]::CreateDirectory($ServerDirectory) | Out-Null
[System.IO.Directory]::CreateDirectory((Split-Path -Parent $MinecraftLogPath)) | Out-Null
[System.IO.File]::WriteAllText((Join-Path $ServerDirectory "eula.txt"), "eula=true`n", [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText((Join-Path $ServerDirectory "server.properties"), @"
online-mode=false
server-port=25566
enable-rcon=true
rcon.port=$RconPort
rcon.password=$RconPassword
"@, [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText($LogPath, "", [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText($MinecraftLogPath, "", [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText($DiagnosticPath, "", [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText($TracePath, "launching" + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))

function Read-ExactBytes {
    param([System.IO.Stream]$Stream, [int]$Length)

    $buffer = New-Object byte[] $Length
    $offset = 0
    while ($offset -lt $Length) {
        $read = $Stream.Read($buffer, $offset, $Length - $offset)
        if ($read -le 0) {
            throw "RCON stream closed unexpectedly."
        }
        $offset += $read
    }
    return $buffer
}

function Write-RconPacket {
    param([System.IO.Stream]$Stream, [int]$RequestId, [int]$Type, [string]$Payload)

    $payloadBytes = [System.Text.Encoding]::UTF8.GetBytes($Payload)
    $packetLength = 10 + $payloadBytes.Length
    $packet = New-Object byte[] (4 + $packetLength)
    [System.BitConverter]::GetBytes($packetLength).CopyTo($packet, 0)
    [System.BitConverter]::GetBytes($RequestId).CopyTo($packet, 4)
    [System.BitConverter]::GetBytes($Type).CopyTo($packet, 8)
    $payloadBytes.CopyTo($packet, 12)
    $Stream.Write($packet, 0, $packet.Length)
    $Stream.Flush()
}

function Read-RconPacket {
    param([System.IO.Stream]$Stream)

    $length = [System.BitConverter]::ToInt32((Read-ExactBytes $Stream 4), 0)
    if ($length -lt 10 -or $length -gt 4096) {
        throw "RCON response has an invalid packet length $length."
    }
    $body = Read-ExactBytes $Stream $length
    return [pscustomobject]@{
        RequestId = [System.BitConverter]::ToInt32($body, 0)
        Type = [System.BitConverter]::ToInt32($body, 4)
    }
}

function Send-RconStop {
    $deadline = [DateTime]::UtcNow.AddSeconds(10)
    $lastError = $null
    while ([DateTime]::UtcNow -lt $deadline) {
        $client = [System.Net.Sockets.TcpClient]::new()
        try {
            $client.Connect("127.0.0.1", $RconPort)
            $stream = $client.GetStream()
            Write-RconPacket $stream 1 3 $RconPassword
            $authentication = Read-RconPacket $stream
            if ($authentication.RequestId -ne 1) {
                throw "RCON authentication failed."
            }
            Write-RconPacket $stream 2 2 "stop"
            return
        } catch {
            $lastError = $_
            Start-Sleep -Milliseconds 250
        } finally {
            $client.Dispose()
        }
    }
    throw $lastError
}

$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = "cmd.exe"
$startInfo.Arguments = "/c `"$ProjectRoot\gradlew.bat runSmokeDedicatedServer --no-daemon`""
$startInfo.WorkingDirectory = $ProjectRoot
$startInfo.UseShellExecute = $false
$startInfo.RedirectStandardInput = $false
$startInfo.RedirectStandardOutput = $false
$startInfo.RedirectStandardError = $false
$startInfo.CreateNoWindow = $true

$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $startInfo

if (-not $process.Start()) {
    throw "Could not start the dedicated-server smoke process. Log retained at $LogPath"
}
[System.IO.File]::AppendAllText($TracePath, "process started" + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))

$deadline = [DateTime]::UtcNow + $Timeout
$started = $false
while (([DateTime]::UtcNow -lt $deadline) -and (-not ($process.HasExited))) {
    Start-Sleep -Milliseconds 250
    $log = if (Test-Path -LiteralPath $MinecraftLogPath) {
        Get-Content -Raw -LiteralPath $MinecraftLogPath
    } elseif (Test-Path -LiteralPath $LogPath) {
        Get-Content -Raw -LiteralPath $LogPath
    } else {
        ""
    }
    foreach ($pattern in $FailurePatterns) {
        if ($log -match $pattern) {
            $process.Kill()
            throw "Dedicated server smoke failed on '$pattern'. Full log retained at $LogPath"
        }
    }
    # The done marker is the server-ready contract. RCON has to be listening as
    # well, otherwise the graceful stop request races the listener startup.
    if ($log -match $ReadyPattern -and $log -match "RCON running on") {
        $started = $true
        [System.IO.File]::AppendAllText($TracePath, "ready detected" + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))
        break
    }
}

[System.IO.File]::AppendAllText($TracePath, "loop ended started=$started exited=$($process.HasExited)" + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))

if (-not $started) {
    if (-not ($process.HasExited)) {
        $process.Kill()
    }
    throw "Dedicated server did not reach its ready marker within $($Timeout.TotalSeconds) seconds. Full log retained at $LogPath"
}

Send-RconStop
[System.IO.File]::AppendAllText($TracePath, "stop requested" + [Environment]::NewLine, [System.Text.UTF8Encoding]::new($false))
$stopDeadline = [DateTime]::UtcNow + $StopTimeout
$serverStopped = $false
while ([DateTime]::UtcNow -lt $stopDeadline) {
    $stopLog = if (Test-Path -LiteralPath $MinecraftLogPath) { Get-Content -Raw -LiteralPath $MinecraftLogPath } else { "" }
    if ($stopLog -match "Stopping server" -and $stopLog -match "RCON Listener stopped") {
        $serverStopped = $true
        break
    }
    if ($process.HasExited) {
        break
    }
    Start-Sleep -Milliseconds 250
}
if (-not $serverStopped) {
    if (-not ($process.HasExited)) {
        $process.Kill()
    }
    throw "Dedicated server did not stop cleanly within $($StopTimeout.TotalSeconds) seconds. Full log retained at $LogPath"
}
if (-not ($process.HasExited)) {
    # Loom's development launcher can leave its Gradle parent alive after the
    # server has already completed the verified normal-stop path above.
    $process.Kill()
    $process.WaitForExit(5000) | Out-Null
}

$finalLog = @(
    Get-Content -Raw -LiteralPath $LogPath
    if (Test-Path -LiteralPath $MinecraftLogPath) { Get-Content -Raw -LiteralPath $MinecraftLogPath }
) -join [Environment]::NewLine
[System.IO.File]::WriteAllText($LogPath, $finalLog, [System.Text.UTF8Encoding]::new($false))
foreach ($pattern in $FailurePatterns) {
    if ($finalLog -match $pattern) {
        throw "Dedicated server smoke failed on '$pattern'. Full log retained at $LogPath"
    }
}
if ($process.HasExited -and $process.ExitCode -ne 0 -and -not $serverStopped) {
    throw "Dedicated server exited with code $($process.ExitCode). Full log retained at $LogPath"
}
