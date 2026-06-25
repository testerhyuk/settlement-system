param(
    [string]$Mode = "smoke",
    [int]$Rate = 20,
    [string]$Duration = "1m",
    [string]$BaseUrl = "http://nginx",
    [string]$CampaignId = "campaign-d85b5d29-6036-46a7-9cfd-373a4c0e3c99",
    [string]$FixedUserId = "user-integration-test",
    [int]$CpcAmount = 1,
    [int]$PostWaitSeconds = 90
)

$ErrorActionPreference = "Stop"

function Query-ClickHouse {
    param([string]$Sql)

    docker exec clickhouse-settlement clickhouse-client `
        -u hyuk `
        --password password `
        -d settlement_analytics `
        -q $Sql
}

function Show-ConsumerLag {
    param([string]$GroupId)

    Write-Host ""
    Write-Host "kafka lag: $GroupId"
    docker exec kafka-1 /opt/kafka/bin/kafka-consumer-groups.sh `
        --bootstrap-server kafka-1:9092 `
        --describe `
        --group $GroupId | Out-Host
}

function Show-DlqCount {
    param([string]$Topic)

    Write-Host ""
    Write-Host "dlq topic check: $Topic"

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"

    try {
        $messages = docker exec kafka-1 /opt/kafka/bin/kafka-console-consumer.sh `
            --bootstrap-server kafka-1:9092 `
            --topic $Topic `
            --from-beginning `
            --timeout-ms 3000 2>$null
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }

    if ($LASTEXITCODE -ne 0 -and $LASTEXITCODE -ne 1) {
        throw "failed to consume dlq topic=$Topic"
    }

    if (-not $messages) {
        Write-Host "no dlq messages"
        return
    }

    $messages | Select-Object -First 5 | Out-Host
}

Write-Host "k6 ad-analytics load test"
Write-Host "mode=$Mode rate=$Rate duration=$Duration baseUrl=$BaseUrl campaignId=$CampaignId fixedUserId=$FixedUserId cpcAmount=$CpcAmount"

docker run --rm `
    --network settlement-system_default `
    -v "${PWD}\k6:/scripts" `
    -e BASE_URL=$BaseUrl `
    -e TEST_MODE=$Mode `
    -e RATE=$Rate `
    -e DURATION=$Duration `
    -e CAMPAIGN_ID=$CampaignId `
    -e FIXED_USER_ID=$FixedUserId `
    -e CPC_AMOUNT=$CpcAmount `
    grafana/k6 run /scripts/ad-analytics-load-test.js

if ($LASTEXITCODE -ne 0) {
    throw "k6 load test failed"
}

Write-Host ""
Write-Host "wait $PostWaitSeconds seconds for Flink windows and ClickHouse sinks"
Start-Sleep -Seconds $PostWaitSeconds

Show-ConsumerLag "ad-analytics-impressions-group"
Show-ConsumerLag "ad-analytics-clicks-group"
Show-ConsumerLag "ad-analytics-conversions-group"
Show-ConsumerLag "ad-analytics-budget-results-group"

Write-Host ""
Write-Host "latest campaign_performance_metrics"
Query-ClickHouse "
    SELECT
        campaign_id,
        window_start,
        window_end,
        impression_count,
        click_count,
        conversion_count,
        total_deducted_amount,
        total_conversion_amount,
        ctr,
        cvr,
        roas,
        partial
    FROM campaign_performance_metrics
    WHERE campaign_id = '$CampaignId'
    ORDER BY created_at DESC
    LIMIT 10
"

Write-Host ""
Write-Host "latest segment_performance_metrics"
Query-ClickHouse "
    SELECT
        segment_id,
        campaign_id,
        window_start,
        window_end,
        impression_count,
        click_count,
        conversion_count,
        total_deducted_amount,
        total_conversion_amount,
        ctr,
        cvr,
        roas,
        partial
    FROM segment_performance_metrics
    WHERE campaign_id = '$CampaignId'
    ORDER BY created_at DESC
    LIMIT 10
"

Show-DlqCount "budget-results.parse-dlq"
Show-DlqCount "ad-impressions.parse-dlq"
Show-DlqCount "ad-clicks.parse-dlq"
Show-DlqCount "ad-conversions.parse-dlq"
Show-DlqCount "unknown-segments.dlq"

Write-Host ""
Write-Host "k6 ad-analytics load test completed"
