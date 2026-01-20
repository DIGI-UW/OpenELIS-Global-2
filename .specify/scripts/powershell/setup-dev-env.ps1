#!/usr/bin/env pwsh
<#
.SYNOPSIS
Setup SpecKit tooling for this repo (PowerShell)

.PARAMETER Target
Install target: cursor, claude, or all (default).

.PARAMETER Yes
Skip confirmation prompt.
#>
[CmdletBinding()]
param(
    [ValidateSet('cursor','claude','all')]
    [string]$Target = 'all',
    [switch]$Yes,
    [switch]$Help
)

if ($Help) {
    Write-Host "Usage: ./setup-dev-env.ps1 [-Yes] [cursor|claude|all]"
    exit 0
}

$ErrorActionPreference = 'Stop'

. "$PSScriptRoot/common.ps1"

$repoRoot = Get-RepoRoot
$commandsDir = Join-Path $repoRoot '.specify/core/commands'

if (-not (Test-Path $commandsDir)) {
    Write-Error "Error: $commandsDir not found. Ensure .specify/core/commands exists."
    exit 1
}

Write-Host "SpecKit setup (PowerShell)"
Write-Host "Repo: $repoRoot"
Write-Host "Installing slash commands to: $Target"
Write-Host ""

$installArgs = @()
if ($Yes) { $installArgs += '-Yes' }
if ($Target) { $installArgs += $Target }

& "$PSScriptRoot/install-commands.ps1" @installArgs

Write-Host ""
Write-Host "Next steps:"
Write-Host "  - Use /speckit.specify to create a new feature"
Write-Host "  - Use /speckit.plan and /speckit.tasks for planning"
Write-Host "  - If not using git, set SPECIFY_FEATURE=<NNN-feature-name>"
