param(
    [string]$BaseUrl = "http://localhost",
    [string]$CampaignId = "campaign-37e6055d-6217-4176-b601-029e7d1a948b",
    [string]$AdvertiserId = "advertiser-28aa3b71-077b-4d45-a80e-21a35c4186dd",
    [string]$UserId = "user-integration-test",
    [decimal]$ConversionAmount = 15000,
    [int]$WindowCloseDelaySeconds = 70,
    [int]$PerformanceEmitDelaySeconds = 75
)

$ErrorActionPreference = "Stop"

function Invoke-JsonPost {
    param(
        [string]$Uri,
        [object[]]$Body
    )

    $json = ConvertTo-Json -InputObject $Body -Depth 10 -Compress
    Invoke-RestMethod `
        -Method Post `
        -Uri $Uri `
        -ContentType "application/json" `
        -Body $json | Out-Null
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

$testId = [Guid]::NewGuid().ToString("N").Substring(0, 12)
$firstImpressionId = "impression-it-$testId-1"
$closingImpressionId = "impression-it-$testId-2"
$firstClickId = "click-it-$testId-1"
$closingClickId = "click-it-$testId-2"

Write-Host "integration test id=$testId"
Write-Host "campaignId=$CampaignId"

Send-TrackingBatch -ImpressionId $firstImpressionId -ClickId $firstClickId -BatchName "target-window"

Write-Host "wait $WindowCloseDelaySeconds seconds for next event-time window"
Start-Sleep -Seconds $WindowCloseDelaySeconds

Send-TrackingBatch -ImpressionId $closingImpressionId -ClickId $closingClickId -BatchName "window-closer"

Write-Host "wait $PerformanceEmitDelaySeconds seconds for campaign performance timer"
Start-Sleep -Seconds $PerformanceEmitDelaySeconds

Write-Host ""
Write-Host "campaign_budget_metrics"
Query-ClickHouse "SELECT campaign_id, window_start, window_end, deducted_count, total_deducted_amount FROM campaign_budget_metrics WHERE campaign_id = '$CampaignId' ORDER BY window_start DESC LIMIT 5"

Write-Host ""
Write-Host "ad_impression_metrics"
Query-ClickHouse "SELECT campaign_id, window_start, window_end, impression_count FROM ad_impression_metrics WHERE campaign_id = '$CampaignId' ORDER BY window_start DESC LIMIT 5"

Write-Host ""
Write-Host "ad_click_metrics"
Query-ClickHouse "SELECT campaign_id, window_start, window_end, click_count FROM ad_click_metrics WHERE campaign_id = '$CampaignId' ORDER BY window_start DESC LIMIT 5"

Write-Host ""
Write-Host "ad_conversion_metrics"
Query-ClickHouse "SELECT campaign_id, window_start, window_end, conversion_count, total_conversion_amount FROM ad_conversion_metrics WHERE campaign_id = '$CampaignId' ORDER BY window_start DESC LIMIT 5"

Write-Host ""
Write-Host "campaign_performance_metrics"
Query-ClickHouse "SELECT campaign_id, window_start, window_end, impression_count, click_count, conversion_count, total_deducted_amount, total_conversion_amount, ctr, cvr, roas, partial FROM campaign_performance_metrics WHERE campaign_id = '$CampaignId' ORDER BY window_start DESC LIMIT 10"

Write-Host ""
Write-Host "segment_performance_metrics"
Query-ClickHouse "SELECT segment_id, campaign_id, window_start, window_end, impression_count, click_count, conversion_count, total_deducted_amount, total_conversion_amount, ctr, cvr, roas, partial FROM segment_performance_metrics WHERE campaign_id = '$CampaignId' ORDER BY window_start DESC LIMIT 10"
