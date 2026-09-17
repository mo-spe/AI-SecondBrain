param(
    [Parameter(Mandatory = $true)][string]$Serial,
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
)

$ErrorActionPreference = 'Stop'
$artifactDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../artifacts/ux-review'))
New-Item -ItemType Directory -Force -Path $artifactDirectory | Out-Null
$outputPath = Join-Path $artifactDirectory 'device-tests.txt'
$errorPath = Join-Path $artifactDirectory 'device-tests-stderr.txt'
$arguments = @('-s', ('"' + $Serial + '"'), 'shell', 'am', 'instrument', '-w', '-e', 'class',
    'com.secondbrain.android.ui.MobileFlowsTest', 'com.secondbrain.android.test/androidx.test.runner.AndroidJUnitRunner')
$runner = Start-Process -FilePath $Adb -ArgumentList $arguments -WindowStyle Hidden -PassThru -RedirectStandardOutput $outputPath -RedirectStandardError $errorPath
$deadline = [DateTime]::UtcNow.AddMinutes(5)
try {
    while (-not $runner.HasExited -and [DateTime]::UtcNow -lt $deadline) {
        Start-Sleep -Seconds 3
        $runner.Refresh()
        if ($runner.HasExited) { break }
        $top = (& $Adb -s $Serial shell dumpsys activity activities | Select-String 'topResumedActivity=') -join ''
        # 部分手机阻止测试进程从桌面拉起 Activity；复用测试框架的确切 Intent，不更改系统权限。
        if ($top -notmatch 'com.secondbrain.android') {
            & $Adb -s $Serial shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -f 0x10008000 -n com.secondbrain.android/androidx.activity.ComponentActivity | Out-Null
        }
    }
    if (-not $runner.HasExited) {
        & $Adb -s $Serial shell am force-stop com.secondbrain.android
        throw '设备测试超过 5 分钟，已停止测试应用。'
    }
    $result = Get-Content -LiteralPath $outputPath -Raw
    Write-Output $result
    if ($result -notmatch 'OK \(8 tests\)') { throw "设备测试未全部通过，查看 $outputPath" }
} finally {
    if (-not $runner.HasExited) { Stop-Process -Id $runner.Id }
}
