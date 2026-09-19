param(
    [string]$SourceFolder,
    [string]$Workspace = "/mnt/parker-data/ingestion-prep",
    [string]$OwnerUrl = "http://127.0.0.1:8080",
    [string]$OwnerCookie = $env:PARKER_OWNER_COOKIE,
    [string]$Python = "python",
    [switch]$Import
)

$ErrorActionPreference = "Stop"
if (-not $SourceFolder) {
    Add-Type -AssemblyName System.Windows.Forms
    $picker = New-Object System.Windows.Forms.FolderBrowserDialog
    $picker.Description = "Select the source evidence folder (read-only)"
    if ($picker.ShowDialog() -ne [System.Windows.Forms.DialogResult]::OK) { throw "No source folder selected" }
    $SourceFolder = $picker.SelectedPath
}
if (-not (Test-Path -LiteralPath $SourceFolder -PathType Container)) { throw "Source folder does not exist: $SourceFolder" }
if (-not $OwnerCookie) { throw "PARKER_OWNER_COOKIE is required for authoritative case selection" }

$cases = Invoke-RestMethod -Uri "$OwnerUrl/owner/cases" -Headers @{ Cookie = $OwnerCookie } -Method Get
if (-not $cases.cases -or $cases.cases.Count -eq 0) { throw "Parker returned no selectable cases" }
Write-Host "Available Parker cases:"
for ($i = 0; $i -lt $cases.cases.Count; $i++) {
    Write-Host ("[{0}] {1} ({2})" -f ($i + 1), $cases.cases[$i].caseName, $cases.cases[$i].caseId)
}
$selection = Read-Host "Select exactly one case number"
$number = 0
if (-not [int]::TryParse($selection, [ref]$number) -or $number -lt 1 -or $number -gt $cases.cases.Count) { throw "A valid case selection is required" }
$case = $cases.cases[$number - 1]
if (-not $case.caseId -or $case.caseId -in @("unassigned", "unknown")) { throw "Selected case is not a valid authoritative Parker case" }
Write-Host "Case: $($case.caseName)"
Write-Host "case-id: $($case.caseId)"
Write-Host "Source: $SourceFolder"

$output = & $Python tools/parker_ingestion_prep.py $SourceFolder --workspace $Workspace --case-id $case.caseId 2>&1
$prepExit = $LASTEXITCODE
if ($prepExit -notin @(0, 2)) { throw "Parker Ingestion Prep did not produce a handoff (exit $prepExit)" }
$prep = ($output -join "`n") | ConvertFrom-Json
$jobRoot = $prep.jobRoot
Write-Host ("Source files: {0}; ready: {1}; duplicates: {2}; review: {3}; failed: {4}; unaccounted: {5}" -f `
    $prep.reconciliation.sourceFilesDiscovered, $prep.handoff.readyItemCount, $prep.handoff.duplicateCount,
    $prep.handoff.reviewRequiredCount, $prep.handoff.failedCount, $prep.handoff.unaccountedCount)
Write-Host "Prep status: $($prep.handoff.handoffStatus)"
Write-Host "Prep complete. Handoff: $jobRoot/reports/handoff.json"
if ($Import) {
    $env:PARKER_OWNER_COOKIE = $OwnerCookie
    $importOutput = & $Python tools/parker_ingestion_handoff.py "$jobRoot/reports/handoff.json" --owner-url $OwnerUrl
    $importExit = $LASTEXITCODE
    if ($importExit -ne 0) { throw "Parker handoff import did not complete cleanly (exit $importExit)" }
    $import = ($importOutput -join "`n") | ConvertFrom-Json
    Write-Host "Parker import status: $($import.status)"
    Write-Host ("New evidence: {0}; existing content reused: {1}; associations created: {2}; occurrences recorded: {3}; failed: {4}" -f `
        $import.importedNewCount, $import.reusedExistingContentCount, $import.associationsCreatedCount,
        $import.occurrencesCreatedCount, $import.failedCount)
} else {
    Write-Host "No Parker import requested. Re-run with -Import after reviewing the handoff."
}
