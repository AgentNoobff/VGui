param(
    [Parameter(Mandatory = $true)]
    [string] $WikiDirectory
)

$ErrorActionPreference = 'Stop'

$repositoryRoot = Split-Path -Parent $PSScriptRoot
$sourceDirectory = Join-Path $repositoryRoot 'docs\wiki'
$resolvedWiki = (Resolve-Path -LiteralPath $WikiDirectory).Path

if (-not (Test-Path -LiteralPath (Join-Path $resolvedWiki '.git'))) {
    throw "WikiDirectory must be a cloned Git repository: $resolvedWiki"
}

$utf8WithoutBom = [System.Text.UTF8Encoding]::new($false)

Get-ChildItem -LiteralPath $sourceDirectory -File -Filter '*.md' | ForEach-Object {
    $content = [System.IO.File]::ReadAllText($_.FullName)
    $content = [regex]::Replace(
        $content,
        '\(([A-Za-z0-9_-]+)\.md(#[^)]+)?\)',
        '($1$2)'
    )
    $destination = Join-Path $resolvedWiki $_.Name
    [System.IO.File]::WriteAllText($destination, $content, $utf8WithoutBom)
}

Write-Host "Synchronized Wiki pages to $resolvedWiki"
