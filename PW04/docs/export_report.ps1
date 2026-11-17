param(
    [string]$DocumentPath = (Join-Path $PSScriptRoot 'PW04_Report.docx'),
    [string]$PdfPath = (Join-Path $PSScriptRoot 'PW04_Report.pdf')
)
$ErrorActionPreference = 'Stop'
$word = $null
$report = $null
try {
    # A dedicated Word instance; open and save only this report, never the reference.
    $word = New-Object -ComObject Word.Application
    $word.Visible = $false
    $word.DisplayAlerts = 0
    $report = $word.Documents.Open([System.IO.Path]::GetFullPath($DocumentPath), $false, $false)
    $report.Repaginate()
    foreach ($contents in $report.TablesOfContents) { $contents.Update() }
    $null = $report.Fields.Update()
    $report.Repaginate()
    foreach ($contents in $report.TablesOfContents) { $contents.UpdatePageNumbers() }
    $report.Save()
    $report.ExportAsFixedFormat([System.IO.Path]::GetFullPath($PdfPath), 17)
    Write-Output ('Word export complete; pages: ' + $report.ComputeStatistics(2))
}
finally {
    if ($null -ne $report) { $report.Close(0) }
    if ($null -ne $word) { $word.Quit() }
    if ($null -ne $report) { [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($report) }
    if ($null -ne $word) { [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) }
}
