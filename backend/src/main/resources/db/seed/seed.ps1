[CmdletBinding()]
param(
    [ValidateSet('Auto', 'Docker', 'Local')]
    [string]$Mode = 'Auto'
)

$ErrorActionPreference = 'Stop'
$originalOutputEncoding = $OutputEncoding
$originalPgPassword = $env:PGPASSWORD
$script:LASTEXITCODE = 0

function Read-DotEnv {
    param([string]$Path)

    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $trimmed = $line.Trim()
        if (-not $trimmed -or $trimmed.StartsWith('#') -or -not $trimmed.Contains('=')) {
            continue
        }

        $separator = $trimmed.IndexOf('=')
        $key = $trimmed.Substring(0, $separator).Trim()
        $value = $trimmed.Substring($separator + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $values[$key] = $value
    }
    return $values
}

try {
    $OutputEncoding = [System.Text.UTF8Encoding]::new($false)
    $projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..\..\..')).Path
    $envPath = Join-Path $projectRoot '.env'
    $sqlPath = Join-Path $PSScriptRoot 'seed.sql'

    if (-not (Test-Path -LiteralPath $envPath)) {
        throw "Missing $envPath. Copy .env.example to .env and configure the database first."
    }

    $config = Read-DotEnv -Path $envPath
    $requiredKeys = @('DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASSWORD')
    foreach ($key in $requiredKeys) {
        if (-not $config.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($config[$key])) {
            throw "Missing $key in $envPath."
        }
    }

    $selectedMode = $Mode
    if ($selectedMode -eq 'Auto') {
        if (Get-Command docker -ErrorAction SilentlyContinue) {
            $containerId = & docker compose --project-directory $projectRoot ps --status running -q postgres 2>$null
            if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace(($containerId -join ''))) {
                $selectedMode = 'Docker'
            }
        }
        if ($selectedMode -eq 'Auto') {
            $selectedMode = 'Local'
        }
    }

    $sql = Get-Content -Raw -LiteralPath $sqlPath -Encoding UTF8
    Write-Host "Seeding $($config['DB_NAME']) using $selectedMode mode..." -ForegroundColor Cyan

    if ($selectedMode -eq 'Docker') {
        if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
            throw 'Docker CLI was not found.'
        }

        $containerId = & docker compose --project-directory $projectRoot ps --status running -q postgres
        if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace(($containerId -join ''))) {
            throw 'The postgres Compose service is not running. Run: docker compose up -d postgres backend'
        }

        $sql | & docker compose --project-directory $projectRoot exec -T postgres `
            psql --set=ON_ERROR_STOP=on --username $config['DB_USER'] --dbname $config['DB_NAME']
    }
    else {
        if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
            throw 'psql was not found and the Compose postgres service is not running. Start Docker or install PostgreSQL client tools.'
        }

        $env:PGPASSWORD = $config['DB_PASSWORD']
        $sql | & psql --set=ON_ERROR_STOP=on `
            --host $config['DB_HOST'] `
            --port $config['DB_PORT'] `
            --username $config['DB_USER'] `
            --dbname $config['DB_NAME']
    }

    if ($LASTEXITCODE -ne 0) {
        throw "Seed command failed with exit code $LASTEXITCODE. PostgreSQL rolled back the transaction."
    }

    if ($selectedMode -eq 'Docker') {
        $redisContainerId = & docker compose --project-directory $projectRoot ps --status running -q redis 2>$null
        if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace(($redisContainerId -join ''))) {
            & docker compose --project-directory $projectRoot exec -T redis redis-cli FLUSHDB | Out-Null
            if ($LASTEXITCODE -ne 0) {
                Write-Warning 'Database seed succeeded, but the Redis cache could not be cleared. Restart the backend if stale data appears.'
            }
        }
    }

    Write-Host 'Seed completed: 30 records were created in each seeded table.' -ForegroundColor Green
}
finally {
    $OutputEncoding = $originalOutputEncoding
    $env:PGPASSWORD = $originalPgPassword
}
