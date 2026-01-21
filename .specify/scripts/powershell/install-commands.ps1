#!/usr/bin/env pwsh
<#
.SYNOPSIS
Install SpecKit slash commands to AI agent directories (PowerShell)

.PARAMETER Yes
Skip confirmation prompt.

.PARAMETER Target
Install target: cursor, claude, or all (default).
#>
[CmdletBinding()]
param(
    [switch]$Yes,
    [ValidateSet('cursor','claude','all')]
    [string]$Target = 'all',
    [switch]$Help
)

if ($Help) {
    Write-Host "Usage: ./install-commands.ps1 [-Yes] [cursor|claude|all]"
    exit 0
}

$ErrorActionPreference = 'Stop'

. "$PSScriptRoot/common.ps1"

$repoRoot = Get-RepoRoot
$coreDir = Join-Path $repoRoot '.specify/core/commands'
$oeDir = Join-Path $repoRoot '.specify/oe/commands'

if (-not (Test-Path $coreDir)) {
    Write-Error "Error: Core commands not found at $coreDir"
    Write-Error "Ensure .specify/core/commands/ exists"
    exit 1
}

$cmdFiles = Get-ChildItem -Path $coreDir -Filter 'speckit.*.md' -File -ErrorAction SilentlyContinue
$cmdCount = $cmdFiles.Count

function Show-Warning {
    Write-Host "=============================================================="
    Write-Host "             SpecKit Command Installation"
    Write-Host "=============================================================="
    Write-Host ""
    Write-Host "This script will OVERWRITE existing slash commands in your AI agent"
    Write-Host "directories with the latest compiled versions from:"
    Write-Host "  - .specify/core/commands/"
    Write-Host "  - .specify/oe/commands/ (if present)"
    Write-Host ""
    Write-Host "Core: $coreDir"
    Write-Host "OE:   $oeDir"
    Write-Host "Commands to install: $cmdCount"
    Write-Host ""
    Write-Host "Target directories:"
    switch ($Target) {
        'cursor' { Write-Host "  - .cursor/commands/" }
        'claude' { Write-Host "  - .claude/commands/" }
        default  { Write-Host "  - .cursor/commands/"; Write-Host "  - .claude/commands/" }
    }
    Write-Host ""
    Write-Host "WARNING: Any local modifications to these command files will be lost!"
    Write-Host ""
}

if (-not $Yes) {
    Show-Warning
    $reply = Read-Host "Do you want to proceed? [y/N]"
    if ($reply -notmatch '^[Yy]$') {
        Write-Host "Installation cancelled."
        exit 0
    }
    Write-Host ""
}

function Install-Commands {
    param([string]$Name, [string]$Dir)
    Write-Host "-> Installing to $Name..."
    New-Item -ItemType Directory -Path $Dir -Force | Out-Null
    $count = 0
    foreach ($file in $cmdFiles) {
        $baseName = $file.Name
        $coreContent = Get-Content -LiteralPath $file.FullName -Raw
        # Path rewrites: upstream paths → project .specify/ paths
        # Use placeholder to avoid double-rewrite of /templates/ → .specify/templates/ → .specify/.specify/templates/
        $coreContent = $coreContent.Replace('/templates/','__SPECIFY_TEMPLATES__')
        $coreContent = $coreContent.Replace('templates/','.specify/templates/')
        $coreContent = $coreContent.Replace('__SPECIFY_TEMPLATES__','.specify/templates/')
        $coreContent = $coreContent.Replace('scripts/','.specify/scripts/')
        $coreContent = $coreContent.Replace('/memory/','.specify/memory/')

        $mergedContent = $coreContent
        $oeFile = Join-Path $oeDir $baseName
        if (Test-Path $oeFile) {
            $oeContent = Get-Content -LiteralPath $oeFile -Raw
            $injectHeader = "`n`n## OpenELIS-Specific Requirements`n`n"
            $appendHeader = "`n`n---`n`n## OpenELIS-Specific Requirements`n`n"

            if ($baseName -eq 'speckit.tasks.md') {
                $needle = "## Task Generation Rules"
                $idx = $mergedContent.IndexOf($needle)
                if ($idx -ge 0) {
                    $mergedContent =
                        $mergedContent.Substring(0, $idx) +
                        $injectHeader + $oeContent + "`n`n" +
                        $mergedContent.Substring($idx)
                } else {
                    $mergedContent = $mergedContent + $appendHeader + $oeContent
                }
            } elseif ($baseName -eq 'speckit.implement.md') {
                $needle = "## User Input"
                $idx = $mergedContent.IndexOf($needle)
                if ($idx -ge 0) {
                    $mergedContent =
                        $mergedContent.Substring(0, $idx) +
                        $injectHeader + $oeContent + "`n`n" +
                        $mergedContent.Substring($idx)
                } else {
                    $mergedContent = $mergedContent + $appendHeader + $oeContent
                }
            } else {
                $mergedContent = $mergedContent + $appendHeader + $oeContent
            }
        }

        $dest = Join-Path $Dir $baseName
        Set-Content -LiteralPath $dest -Value $mergedContent
        $count++
    }
    Write-Host "  ✓ Installed $count command(s) to $Dir"
}

switch ($Target) {
    'cursor' { Install-Commands -Name 'Cursor' -Dir (Join-Path $repoRoot '.cursor/commands') }
    'claude' { Install-Commands -Name 'Claude Code' -Dir (Join-Path $repoRoot '.claude/commands') }
    default {
        Install-Commands -Name 'Cursor' -Dir (Join-Path $repoRoot '.cursor/commands')
        Install-Commands -Name 'Claude Code' -Dir (Join-Path $repoRoot '.claude/commands')
    }
}

Write-Host ""
Write-Host "Commands installed! Available slash commands:"
$cmdFiles | ForEach-Object { "/$($_.BaseName)" } | ForEach-Object { Write-Host "  $_" }
Write-Host ""
Write-Host "To use: Type /<command> in your AI agent (e.g., /speckit.specify)"
