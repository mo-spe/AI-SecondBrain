$ErrorActionPreference = 'Stop'
$docxPath = 'D:\AI-SecondBrain\course-project\docs\stage-two-program-design.docx'
$renderDirectory = 'D:\AI-SecondBrain\course-project\artifacts\stage-two-program-design\render'
$pdfPath = Join-Path $renderDirectory 'stage-two-program-design.pdf'
New-Item -ItemType Directory -Path $renderDirectory -Force | Out-Null
$wordApp = $null
$document = $null
try {
    $wordApp = New-Object -ComObject Word.Application
    $wordApp.Visible = $false
    $wordApp.DisplayAlerts = 0
    $wordApp.AutomationSecurity = 3
    $document = $wordApp.Documents.Open($docxPath, $false, $true)
    $document.Repaginate()
    $document.Fields.Update() | Out-Null
    $document.ExportAsFixedFormat($pdfPath, 17)
    Write-Output ('Rendered pages: ' + $document.ComputeStatistics(2))
    Write-Output $pdfPath
} finally {
    if ($null -ne $document) { $document.Close(0) }
    if ($null -ne $wordApp) { $wordApp.Quit() }
}
