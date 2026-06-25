param(
    [string]$BaseUrl = "http://localhost",
    [string]$CampaignId = "campaign-37e6055d-6217-4176-b601-029e7d1a948b",
    [string]$AdvertiserId = "advertiser-28aa3b71-077b-4d45-a80e-21a35c4186dd",
    [int]$TimeoutMs = 10000
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

function Assert-UnknownSegmentDlqContains {
    param(
        [string]$UserId,
        [string]$EventType
    )

    $messages = Read-Topic -Topic "unknown-segments.dlq"
    $matched = $messages | Where-Object {
        $_ -like "*$UserId*" `
            -and $_ -like "*$EventType*" `
            -and $_ -like "*USER_SEGMENT_STATE_NOT_FOUND*"
    }

    if (-not $matched) {
        throw "unknown segment DLQ message not found userId=$UserId eventType=$EventType"
    }

    $matched | Select-Object -Last 1 | ForEach-Object { Write-Host $_ }
}

$testId = [Guid]::NewGuid().ToString("N").Substring(0, 12)
$userId = "unknown-segment-user-$testId"
$impressionId = "unknown-segment-impression-$testId"
$clickId = "unknown-segment-click-$testId"

Write-Host "unknown segment dlq test id=$testId"
Write-Host "userId=$userId"

Write-Host "send impression with unknown segment user"
Invoke-JsonPost `
    -Uri "$BaseUrl/api/ad-serving/impressions" `
    -Body @(
        @{
            impressionId = $impressionId
            campaignId = $CampaignId
            advertiserId = $AdvertiserId
            userId = $userId
        }
    )

Write-Host "send click with unknown segment user"
Invoke-JsonPost `
    -Uri "$BaseUrl/api/ad-serving/clicks" `
    -Body @(
        @{
            clickId = $clickId
            impressionId = $impressionId
            campaignId = $CampaignId
            advertiserId = $AdvertiserId
            userId = $userId
            cpcAmount = 700
            currency = "KRW"
        }
    )

Write-Host "send conversion with unknown segment user"
Invoke-JsonPost `
    -Uri "$BaseUrl/api/ad-serving/conversions" `
    -Body @(
        @{
            clickId = $clickId
            impressionId = $impressionId
            campaignId = $CampaignId
            advertiserId = $AdvertiserId
            userId = $userId
            conversionAmount = 15000
        }
    )

Write-Host "wait for segment enrichment"
Start-Sleep -Seconds 5

Assert-UnknownSegmentDlqContains -UserId $userId -EventType "IMPRESSION"
Assert-UnknownSegmentDlqContains -UserId $userId -EventType "CLICK"
Assert-UnknownSegmentDlqContains -UserId $userId -EventType "CONVERSION"

Write-Host ""
Write-Host "unknown segment dlq test passed"
