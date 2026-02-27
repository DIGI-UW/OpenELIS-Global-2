#!/usr/bin/env pwsh
# Common PowerShell functions analogous to common.sh

function Get-RepoRoot {
    try {
        $result = git rev-parse --show-toplevel 2>$null
        if ($LASTEXITCODE -eq 0) {
            return $result
        }
    } catch {
        # Git command failed
    }
    
    # Fall back to script location for non-git repos
    return (Resolve-Path (Join-Path $PSScriptRoot "../../..")).Path
}

function Get-CurrentBranch {
    # First check if SPECIFY_FEATURE environment variable is set
    if ($env:SPECIFY_FEATURE) {
        return $env:SPECIFY_FEATURE
    }
    
    # Then check git if available
    try {
        $result = git rev-parse --abbrev-ref HEAD 2>$null
        if ($LASTEXITCODE -eq 0) {
            return $result
        }
    } catch {
        # Git command failed
    }
    
    # For non-git repos, try to find the latest feature directory
    $repoRoot = Get-RepoRoot
    $specsDir = Join-Path $repoRoot "specs"
    
    if (Test-Path $specsDir) {
        $latestFeature = ""
        $highest = 0
        
        Get-ChildItem -Path $specsDir -Directory | ForEach-Object {
            if ($_.Name -match '^(\d{3})-') {
                $num = [int]$matches[1]
                if ($num -gt $highest) {
                    $highest = $num
                    $latestFeature = $_.Name
                }
            }
        }
        
        if ($latestFeature) {
            return $latestFeature
        }
    }
    
    # Final fallback
    return "main"
}

function Test-HasGit {
    try {
        git rev-parse --show-toplevel 2>$null | Out-Null
        return ($LASTEXITCODE -eq 0)
    } catch {
        return $false
    }
}

function Test-FeatureBranch {
    param(
        [string]$Branch,
        [bool]$HasGit = $true
    )
    
    # For non-git repos, we can't enforce branch naming but still provide output
    if (-not $HasGit) {
        Write-Warning "[specify] Warning: Git repository not detected; skipped branch validation"
        return $true
    }
    
    $isValid = $false
    if ($Branch -match '(^|/)[0-9]{3}-') { $isValid = $true }
    elseif ($Branch -match '^(spec|feat|fix|hotfix)/[A-Z]+-[0-9]+-') { $isValid = $true }
    elseif ($Branch -match '^(spec|feat|fix|hotfix)/[0-9]{3}-') { $isValid = $true }

    if (-not $isValid) {
        Write-Output "ERROR: Not on a feature branch. Current branch: $Branch"
        Write-Output "Feature branches should match one of these patterns:"
        Write-Output ""
        Write-Output "  Legacy format:"
        Write-Output "    - 001-feature-name"
        Write-Output "    - fix/001-feature-name"
        Write-Output ""
        Write-Output "  Principle IX format (Jira: OGC-###, GitHub: ###):"
        Write-Output "    - spec/OGC-009-sidenav or spec/009-sidenav"
        Write-Output "    - feat/OGC-009-sidenav"
        Write-Output "    - feat/OGC-009-sidenav/m1-core (milestone)"
        Write-Output "    - hotfix/OGC-123-fix-login"
        Write-Output "    - fix/OGC-456-null-check"
        return $false
    }
    return $true
}

function Find-FeatureDirByPrefix {
    param(
        [string]$RepoRoot,
        [string]$BranchName
    )

    $specsDir = Join-Path $RepoRoot 'specs'
    $prefix = $null

    if ($BranchName -match '^(spec|feat|fix|hotfix)/[A-Z]+-([0-9]+)-') {
        $prefix = '{0:000}' -f [int]$matches[2]
    } elseif ($BranchName -match '^(spec|feat|fix|hotfix)/([0-9]{3})-') {
        $prefix = $matches[2]
    } elseif ($BranchName -match '(^|/)([0-9]{3})-') {
        $prefix = $matches[2]
    }

    if (-not $prefix) {
        return (Join-Path $specsDir $BranchName)
    }

    $matchesDirs = @()
    if (Test-Path $specsDir) {
        $numericMatches = Get-ChildItem -Path $specsDir -Directory -Filter "$prefix-*"
        $jiraMatches = Get-ChildItem -Path $specsDir -Directory -Filter "OGC-$prefix-*"
        foreach ($item in @($numericMatches + $jiraMatches)) {
            if ($item -and $item.Name) { $matchesDirs += $item.Name }
        }
    }

    if ($matchesDirs.Count -eq 0) {
        return (Join-Path $specsDir $BranchName)
    } elseif ($matchesDirs.Count -eq 1) {
        return (Join-Path $specsDir $matchesDirs[0])
    } else {
        Write-Output "ERROR: Multiple spec directories found with prefix '$prefix': $($matchesDirs -join ', ')"
        Write-Output "Please ensure only one spec directory exists per numeric prefix."
        return (Join-Path $specsDir $BranchName)
    }
}

function Get-FeaturePathsEnv {
    $repoRoot = Get-RepoRoot
    $currentBranch = Get-CurrentBranch
    $hasGit = Test-HasGit
    $featureDir = Find-FeatureDirByPrefix -RepoRoot $repoRoot -BranchName $currentBranch
    
    [PSCustomObject]@{
        REPO_ROOT     = $repoRoot
        CURRENT_BRANCH = $currentBranch
        HAS_GIT       = $hasGit
        FEATURE_DIR   = $featureDir
        FEATURE_SPEC  = Join-Path $featureDir 'spec.md'
        IMPL_PLAN     = Join-Path $featureDir 'plan.md'
        TASKS         = Join-Path $featureDir 'tasks.md'
        RESEARCH      = Join-Path $featureDir 'research.md'
        DATA_MODEL    = Join-Path $featureDir 'data-model.md'
        QUICKSTART    = Join-Path $featureDir 'quickstart.md'
        CONTRACTS_DIR = Join-Path $featureDir 'contracts'
    }
}

function Test-FileExists {
    param([string]$Path, [string]$Description)
    if (Test-Path -Path $Path -PathType Leaf) {
        Write-Output "  ✓ $Description"
        return $true
    } else {
        Write-Output "  ✗ $Description"
        return $false
    }
}

function Test-DirHasFiles {
    param([string]$Path, [string]$Description)
    if ((Test-Path -Path $Path -PathType Container) -and (Get-ChildItem -Path $Path -ErrorAction SilentlyContinue | Where-Object { -not $_.PSIsContainer } | Select-Object -First 1)) {
        Write-Output "  ✓ $Description"
        return $true
    } else {
        Write-Output "  ✗ $Description"
        return $false
    }
}

