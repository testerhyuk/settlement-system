param(
    [int]$TimeoutMs = 10000
)

$ErrorActionPreference = "Stop"

function Send-BrokenJson {
    param(
        [string]$Topic,
        [string]$Key,
        [string]$Payload
    )

    Write-Host "send broken json topic=$Topic key=$Key"
    $Payload | docker exec -i kafka-1 /opt/kafka/bin/kafka-console-producer.sh `
        --bootstrap-server kafka-1:9092 `
        --topic $Topic `
        --property "parse.key=true" `
        --property "key.separator=|"

    if ($LASTEXITCODE -ne 0) {
        throw "failed to produce broken json topic=$Topic"
    }
}

function Read-Topic {
    param([string]$Topic)

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"

    try {
        $output = docker exec kafka-1 /opt/kafka/bin/kafka-console-consumer.sh `
            --bootstrap-server kafka-1:9092 `
            --topic $Topic `
            --from-beginning `
            --timeout-ms $TimeoutMs 2>$null
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne 1) {
        throw "failed to consume topic=$Topic"
    }

    return $output
}

function Assert-DlqContains {
    param(
        [string]$Topic,
        [string]$TestId
    )

    Write-Host "check dlq topic=$Topic testId=$TestId"
    $messages = Read-Topic -Topic $Topic
    $matched = $messages | Where-Object { $_ -like "*$TestId*" -and $_ -like "*errorMessage=*" }

    if (-not $matched) {
        throw "DLQ message not found topic=$Topic testId=$TestId"
    }

    $matched | Select-Object -Last 1 | ForEach-Object { Write-Host $_ }
}

$testId = [Guid]::NewGuid().ToString("N").Substring(0, 12)
$key = "parse-dlq-test-$testId"
$brokenPayload = "$key|{`"testId`":`"$testId`",`"broken`":"

Write-Host "parse dlq test id=$testId"

$cases = @(
    @{ SourceTopic = "budget-results"; DlqTopic = "budget-results.parse-dlq" },
    @{ SourceTopic = "ad-impressions"; DlqTopic = "ad-impressions.parse-dlq" },
    @{ SourceTopic = "ad-clicks"; DlqTopic = "ad-clicks.parse-dlq" },
    @{ SourceTopic = "ad-conversions"; DlqTopic = "ad-conversions.parse-dlq" },
    @{ SourceTopic = "user-segments"; DlqTopic = "user-segments.parse-dlq" }
)

foreach ($case in $cases) {
    Send-BrokenJson `
        -Topic $case.SourceTopic `
        -Key $key `
        -Payload $brokenPayload
}

Write-Host "wait for ad-analytics parse processing"
Start-Sleep -Seconds 5

foreach ($case in $cases) {
    Assert-DlqContains `
        -Topic $case.DlqTopic `
        -TestId $testId
}

Write-Host ""
Write-Host "parse dlq test passed"
