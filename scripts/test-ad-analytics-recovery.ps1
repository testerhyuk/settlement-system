param(
    [string]$BaseUrl = "http://localhost",
    [string]$CampaignId = "campaign-37e6055d-6217-4176-b601-029e7d1a948b",
    [string]$AdvertiserId = "advertiser-28aa3b71-077b-4d45-a80e-21a35c4186dd",
    [string]$UserId = "user-integration-test",
    [decimal]$ConversionAmount = 15000,
    [int]$WindowCloseDelaySeconds = 70,
    [int]$TimeoutSeconds = 180,
    [int]$PollSeconds = 5
)

$ErrorActionPreference = "Stop"

function Invoke-JsonPost {
    param(
        [string]$Uri,
        [object[]]$Body
    )

    $json = ConvertTo-Json -InputObject $Body -Depth 10 -Compress
    try {
        Invoke-RestMethod `
            -Method Post `
            -Uri $Uri `
            -ContentType "application/json" `
            -Body $json | Out-Null
    } catch {
        Write-Host "HTTP request failed uri=$Uri"
        Write-Host "body=$json"

        if ($_.Exception.Response -ne $null) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            Write-Host "response=$responseBody"
        }

        throw
    }
}

function Send-TrackingBatch {
    param(
        [string]$ImpressionId,
        [string]$ClickId,
        [string]$BatchName
    )

    Write-Host "[$BatchName] send impression"
    Invoke-JsonPost `
        -Uri "$BaseUrl/api/ad-serving/impressions" `
        -Body @(
            @{
                impressionId = $ImpressionId
                campaignId = $CampaignId
                advertiserId = $AdvertiserId
                userId = $UserId
            }
        )

    Write-Host "[$BatchName] send click"
    Invoke-JsonPost `
        -Uri "$BaseUrl/api/ad-serving/clicks" `
        -Body @(
            @{
                clickId = $ClickId
                impressionId = $ImpressionId
                campaignId = $CampaignId
                advertiserId = $AdvertiserId
                userId = $UserId
                cpcAmount = 700
                currency = "KRW"
            }
        )

    Write-Host "[$BatchName] send conversion clickId=$ClickId"
    Invoke-JsonPost `
        -Uri "$BaseUrl/api/ad-serving/conversions" `
        -Body @(
            @{
                clickId = $ClickId
                impressionId = $ImpressionId
                campaignId = $CampaignId
                advertiserId = $AdvertiserId
                userId = $UserId
                conversionAmount = $ConversionAmount
            }
        )

    Write-Host "[$BatchName] send budget deduct"
    Invoke-RestMethod `
        -Method Post `
        -Uri "$BaseUrl/api/v1/advertisement/ad-campaign/click/$CampaignId" | Out-Null
}

function Query-ClickHouse {
    param([string]$Sql)

    docker exec clickhouse-settlement clickhouse-client `
        -u hyuk `
        --password password `
        -d settlement_analytics `
        -q $Sql
}

function Wait-UntilMetricExists {
    param(
        [string]$TableName,
        [string]$CampaignId,
        [int]$BaselineCount
    )

    Write-Host "baseline table=$TableName count=$BaselineCount"

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)

    while ((Get-Date) -lt $deadline) {
        $count = Query-ClickHouse "
            SELECT count()
            FROM $TableName
            WHERE campaign_id = '$CampaignId'
        "

        if ([int]$count -gt $BaselineCount) {
            Write-Host "$TableName recovered count=$count baseline=$BaselineCount"
            return
        }

        Write-Host "waiting table=$TableName campaignId=$CampaignId"
        Start-Sleep -Seconds $PollSeconds
    }

    throw "timeout waiting metric table=$TableName campaignId=$CampaignId"
}

$testId = [Guid]::NewGuid().ToString("N").Substring(0, 12)
$impressionId = "recovery-impression-$testId"
$clickId = "recovery-click-$testId"
$closingImpressionId = "recovery-impression-$testId-closer"
$closingClickId = "recovery-click-$testId-closer"

Write-Host "ad-analytics recovery test id=$testId"
Write-Host ""
Write-Host "STEP 1. Stop ad-analytics in IntelliJ or Gradle terminal now."
Write-Host "Then press Enter here after ad-analytics is stopped."
Read-Host | Out-Null

Write-Host "send events while ad-analytics is stopped"

Send-TrackingBatch `
    -ImpressionId $impressionId `
    -ClickId $clickId `
    -BatchName "backlog-while-stopped"

Write-Host ""
Write-Host "capture recent ClickHouse baseline while ad-analytics is stopped"

$impressionBaselineSql = "
    SELECT count()
    FROM ad_impression_metrics
    WHERE campaign_id = '$CampaignId'
"

$clickBaselineSql = "
    SELECT count()
    FROM ad_click_metrics
    WHERE campaign_id = '$CampaignId'
"

$conversionBaselineSql = "
    SELECT count()
    FROM ad_conversion_metrics
    WHERE campaign_id = '$CampaignId'
"

$impressionCountBefore = Query-ClickHouse $impressionBaselineSql
$clickCountBefore = Query-ClickHouse $clickBaselineSql
$conversionCountBefore = Query-ClickHouse $conversionBaselineSql

Write-Host "recent impression metric count before restart=$impressionCountBefore"
Write-Host "recent click metric count before restart=$clickCountBefore"
Write-Host "recent conversion metric count before restart=$conversionCountBefore"
Write-Host ""
Write-Host "STEP 2. Start ad-analytics again in IntelliJ or Gradle terminal."
Write-Host "Then press Enter here after Flink job is running."
Read-Host | Out-Null

Write-Host "wait $WindowCloseDelaySeconds seconds, then send window closer event"
Start-Sleep -Seconds $WindowCloseDelaySeconds

Send-TrackingBatch `
    -ImpressionId $closingImpressionId `
    -ClickId $closingClickId `
    -BatchName "window-closer-after-restart"

Write-Host "wait for Kafka backlog consumption and ClickHouse sink"

Wait-UntilMetricExists `
    -TableName "ad_impression_metrics" `
    -CampaignId $CampaignId `
    -BaselineCount ([int]$impressionCountBefore)

Wait-UntilMetricExists `
    -TableName "ad_click_metrics" `
    -CampaignId $CampaignId `
    -BaselineCount ([int]$clickCountBefore)

Wait-UntilMetricExists `
    -TableName "ad_conversion_metrics" `
    -CampaignId $CampaignId `
    -BaselineCount ([int]$conversionCountBefore)

Write-Host ""
Write-Host "recent campaign_performance_metrics"
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
    ORDER BY window_start DESC
    LIMIT 5
"

Write-Host ""
Write-Host "ad-analytics recovery test completed"
