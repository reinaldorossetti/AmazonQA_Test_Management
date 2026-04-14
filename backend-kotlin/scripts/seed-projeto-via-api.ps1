Param(
    [string]$ApiBaseUrl = "http://localhost:8080/api/v1",
    [string]$SeedFile = ".\scripts\api-seed-projeto-ficticio.json",
    [string]$Token = "admin-token"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Assert-True {
    Param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw "[VALIDACAO] $Message"
    }
}

function Get-SeedValue {
    Param(
        [object]$Object,
        [string]$Name
    )

    if ($null -eq $Object) {
        return $null
    }

    $property = $Object.PSObject.Properties[$Name]
    if ($null -ne $property) {
        return $property.Value
    }

    return $null
}

function Invoke-Api {
    Param(
        [ValidateSet("GET", "POST", "PATCH", "DELETE")]
        [string]$Method,
        [string]$Path,
        $Body = $null
    )

    $uri = "$ApiBaseUrl$Path"
    $headers = @{
        "Authorization" = "Bearer $Token"
        "Accept"        = "application/json"
    }

    try {
        if ($null -eq $Body) {
            $response = Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers
        }
        else {
            $jsonBody = $Body | ConvertTo-Json -Depth 20
            $utf8Body = [System.Text.Encoding]::UTF8.GetBytes($jsonBody)
            $response = Invoke-RestMethod -Method $Method -Uri $uri -Headers $headers -ContentType "application/json; charset=utf-8" -Body $utf8Body
        }

        return $response.data
    }
    catch {
        $statusCode = $null
        $responseBody = $null

        if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
            $statusCode = [int]$_.Exception.Response.StatusCode
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                if ($stream) {
                    $reader = New-Object System.IO.StreamReader($stream)
                    $responseBody = $reader.ReadToEnd()
                }
            }
            catch {
                $responseBody = $null
            }
        }

        throw "[API-ERRO] $Method $Path falhou. Status=$statusCode Body=$responseBody Erro=$($_.Exception.Message)"
    }
}

if (-not (Test-Path -Path $SeedFile)) {
    throw "Arquivo de seed nao encontrado: $SeedFile"
}

$seed = Get-Content -Path $SeedFile -Raw -Encoding UTF8 | ConvertFrom-Json

Write-Host "Iniciando seed via API..." -ForegroundColor Cyan

Assert-True ($null -ne $seed.project) "Objeto 'project' e obrigatorio no JSON"
$projectName = [string](Get-SeedValue -Object $seed.project -Name "name")
Assert-True (-not [string]::IsNullOrWhiteSpace($projectName)) "project.name e obrigatorio"

$createdProject = Invoke-Api -Method "POST" -Path "/projects" -Body @{ name = $projectName }
$projectId = $createdProject.id

Assert-True (-not [string]::IsNullOrWhiteSpace($projectId)) "API nao retornou id do projeto"
Write-Host "Projeto criado: $($createdProject.name) ($projectId)" -ForegroundColor Green

$requirementMap = @{}
$createdRequirements = @()
if ($seed.requirements) {
    foreach ($req in $seed.requirements) {
        $reqTitle = [string](Get-SeedValue -Object $req -Name "title")
        $reqKey = [string](Get-SeedValue -Object $req -Name "key")

        Assert-True (-not [string]::IsNullOrWhiteSpace($reqTitle)) "Requirement sem title"

        $createdReq = Invoke-Api -Method "POST" -Path "/projects/$projectId/requirements" -Body @{ title = $reqTitle }
        $createdRequirements += $createdReq

        if (-not [string]::IsNullOrWhiteSpace($reqKey)) {
            $requirementMap[$reqKey] = $createdReq.id
        }

        Write-Host "Requirement criado: $($createdReq.title)" -ForegroundColor Green
    }
}

$createdSuites = @()
if ($seed.suites) {
    foreach ($suite in $seed.suites) {
        $suiteName = [string](Get-SeedValue -Object $suite -Name "name")
        Assert-True (-not [string]::IsNullOrWhiteSpace($suiteName)) "Suite sem name"
        $createdSuite = Invoke-Api -Method "POST" -Path "/projects/$projectId/suites" -Body @{ name = $suiteName }
        $createdSuites += $createdSuite
        Write-Host "Suite criada: $($createdSuite.name)" -ForegroundColor Green
    }
}

$createdTestCases = @()
if ($seed.testCases) {
    foreach ($tc in $seed.testCases) {
        $tcTitle = [string](Get-SeedValue -Object $tc -Name "title")
        $tcTestId = [string](Get-SeedValue -Object $tc -Name "testId")
        $tcRequirementKey = [string](Get-SeedValue -Object $tc -Name "requirementKey")

        Assert-True (-not [string]::IsNullOrWhiteSpace($tcTitle)) "Test case sem title"

        $requirementLink = $null
        if ($tcRequirementKey -and $requirementMap.ContainsKey($tcRequirementKey)) {
            $requirementLink = $requirementMap[$tcRequirementKey]
        }

        $payload = @{
            title           = $tcTitle
            testId          = $tcTestId
            priority        = Get-SeedValue -Object $tc -Name "priority"
            bugSeverity     = Get-SeedValue -Object $tc -Name "bugSeverity"
            tagsKeywords    = Get-SeedValue -Object $tc -Name "tagsKeywords"
            requirementLink = $requirementLink
            executionType   = Get-SeedValue -Object $tc -Name "executionType"
            testCaseStatus  = Get-SeedValue -Object $tc -Name "testCaseStatus"
            platform        = Get-SeedValue -Object $tc -Name "platform"
            testEnvironment = Get-SeedValue -Object $tc -Name "testEnvironment"
            preconditions   = Get-SeedValue -Object $tc -Name "preconditions"
            actions         = Get-SeedValue -Object $tc -Name "actions"
            expectedResult  = Get-SeedValue -Object $tc -Name "expectedResult"
            executionStatus = Get-SeedValue -Object $tc -Name "executionStatus"
            notes           = Get-SeedValue -Object $tc -Name "notes"
            attachments     = Get-SeedValue -Object $tc -Name "attachments"
        }

        $createdTc = Invoke-Api -Method "POST" -Path "/projects/$projectId/test-cases" -Body $payload
        $createdTestCases += $createdTc
        Write-Host "Test case criado: $($createdTc.title) [$($createdTc.testId)]" -ForegroundColor Green
    }
}

Write-Host "Validando cadastro via API..." -ForegroundColor Cyan

$fetchedProject = Invoke-Api -Method "GET" -Path "/projects/$projectId"
Assert-True ($fetchedProject.name -eq $projectName) "Nome do projeto nao confere"

$fetchedRequirements = Invoke-Api -Method "GET" -Path "/projects/$projectId/requirements"
foreach ($req in $seed.requirements) {
    $reqTitle = [string](Get-SeedValue -Object $req -Name "title")
    $existsReq = $fetchedRequirements | Where-Object { $_.title -eq $reqTitle } | Select-Object -First 1
    Assert-True ($null -ne $existsReq) "Requirement nao encontrado apos cadastro: $reqTitle"
}

$fetchedSuites = Invoke-Api -Method "GET" -Path "/projects/$projectId/suites/tree"
foreach ($suite in $seed.suites) {
    $suiteName = [string](Get-SeedValue -Object $suite -Name "name")
    $existsSuite = $fetchedSuites | Where-Object { $_.name -eq $suiteName } | Select-Object -First 1
    Assert-True ($null -ne $existsSuite) "Suite nao encontrada apos cadastro: $suiteName"
}

$fetchedTestCases = Invoke-Api -Method "GET" -Path "/projects/$projectId/test-cases"
foreach ($tc in $seed.testCases) {
    $tcTitle = [string](Get-SeedValue -Object $tc -Name "title")
    $tcTestId = [string](Get-SeedValue -Object $tc -Name "testId")

    $existsTc = $fetchedTestCases | Where-Object { $_.title -eq $tcTitle -and $_.testId -eq $tcTestId } | Select-Object -First 1
    Assert-True ($null -ne $existsTc) "Test case nao encontrado apos cadastro: $tcTitle"

    $fetchedTc = Invoke-Api -Method "GET" -Path "/projects/$projectId/test-cases/$($existsTc.id)"
    Assert-True ($fetchedTc.title -eq $tcTitle) "Titulo do test case nao confere para $tcTestId"
    Assert-True ($fetchedTc.testId -eq $tcTestId) "testId do test case nao confere para $tcTitle"
}

Write-Host "`nSeed finalizado com sucesso!" -ForegroundColor Green
Write-Host "Projeto: $($fetchedProject.name)" -ForegroundColor Green
Write-Host "Project ID: $projectId" -ForegroundColor Green
Write-Host "Requirements criados: $($seed.requirements.Count)" -ForegroundColor Green
Write-Host "Suites criadas: $($seed.suites.Count)" -ForegroundColor Green
Write-Host "Test Cases criados: $($seed.testCases.Count)" -ForegroundColor Green
