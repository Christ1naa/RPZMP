param([string]$DocumentPath = (Join-Path $PSScriptRoot 'PW05_Report.docx'), [string]$PdfPath = (Join-Path $PSScriptRoot 'PW05_Report.pdf'))
$ErrorActionPreference='Stop'
$word=$null
$report=$null
try {
    $word=New-Object -ComObject Word.Application
    $word.Visible=$false
    $word.DisplayAlerts=0
    $report=$word.Documents.Open([System.IO.Path]::GetFullPath($DocumentPath),$false,$false)
    $report.Repaginate()
    foreach($contents in $report.TablesOfContents) { $contents.Update() }
    $null=$report.Fields.Update()
    $report.Repaginate()
    foreach($contents in $report.TablesOfContents) { $contents.UpdatePageNumbers() }
    foreach($story in $report.StoryRanges) {
        $currentStory=$story
        while($null -ne $currentStory) {
            $currentStory.Font.Color=0
            $currentStory=$currentStory.NextStoryRange
        }
    }
    $report.Save()
    $report.ExportAsFixedFormat([System.IO.Path]::GetFullPath($PdfPath),17)
    Write-Output ('Word export; pages: '+$report.ComputeStatistics(2))
} finally {
    if($null -ne $report) { $report.Close(0) }
    if($null -ne $word) { $word.Quit() }
    if($null -ne $report) { [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($report) }
    if($null -ne $word) { [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($word) }
}

