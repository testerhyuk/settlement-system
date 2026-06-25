param(
    [string]$BaseUrl = "http://localhost",
    [string]$CampaignId = "campaign-37e6055d-6217-4176-b601-029e7d1a948b",
    [string]$AdvertiserId = "advertiser-28aa3b71-077b-4d45-a80e-21a35c4186dd",
    [string]$UserId = "user-integration-test",
    [decimal]$CpcAmount = 700,
    [decimal]$ConversionAmount = 15000,
    [int]$TargetEventCount = 2,
    [int]$WindowCloseDelaySeconds = 70,
    [int]$PerformanceEmitDelaySeconds = 75,
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

function Query-ClickHouse {
    param([string]$Sql)

    docker exec clickhouse-settlement clickhouse-client `
        -u hyuk `
        --password password `
        -d settlement_analytics `
        -q $Sql
}

function Trigger-Dag {
    param(
        [string]$DagId,
        [string]$RunId,
        [string]$ReportDate
    )

    Write-Host "trigger dag=$DagId runId=$RunId reportDate=$ReportDate"

    $pythonCommand = "import os; from airflow.api.common.trigger_dag import trigger_dag; trigger_dag(dag_id=os.getenv('DAG_ID'), run_id=os.getenv('DAG_RUN_ID'), conf={'report_date': os.getenv('REPORT_DATE')})"

    docker exec `
        -e DAG_ID=$DagId `
        -e DAG_RUN_ID=$RunId `
        -e REPORT_DATE=$ReportDate `
        airflow-scheduler `
        python -c $pythonCommand | Out-Host

    if ($LASTEXITCODE -ne 0) {
        throw "failed to trigger dag=$DagId runId=$RunId"
    }
}

function Wait-MartSuccess {
    param(
        [string]$MartName,
        [string]$RunId
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)

    while ((Get-Date) -lt $deadline) {
        $status = Query-ClickHouse "
            SELECT status
            FROM daily_mart_batch_runs
            WHERE mart_name = '$MartName'
              AND startsWith(batch_run_id, '$RunId')
            ORDER BY created_at DESC
            LIMIT 1
        "

        if ($status -eq "SUCCESS") {
            Write-Host "mart success: $MartName"
            return
        }

        if ($status -eq "FAILED") {
            $errorMessage = Query-ClickHouse "
                SELECT ifNull(error_message, '')
                FROM daily_mart_batch_runs
                WHERE mart_name = '$MartName'
                  AND startsWith(batch_run_id, '$RunId')
                ORDER BY created_at DESC
                LIMIT 1
            "

            throw "mart failed: $MartName error=$errorMessage"
        }

        Write-Host "waiting mart=$MartName status=$status"
        Start-Sleep -Seconds $PollSeconds
    }

    throw "timeout waiting mart success: $MartName runId=$RunId"
}

function Assert-Equal {
    param(
        [string]$Name,
        [object]$Expected,
        [object]$Actual
    )

    if ("$Expected" -ne "$Actual") {
        throw "assertion failed: $Name expected=$Expected actual=$Actual"
    }

    Write-Host "OK $Name=$Actual"
}

function Assert-DecimalClose {
    param(
        [string]$Name,
        [decimal]$Expected,
        [decimal]$Actual,
        [decimal]$Tolerance = 0.0001
    )

    $diff = [Math]::Abs($Expected - $Actual)
    if ($diff -gt $Tolerance) {
        throw "assertion failed: $Name expected=$Expected actual=$Actual diff=$diff"
    }

    Write-Host "OK $Name=$Actual"
}

function Wait-SafeTargetWindow {
    while ($true) {
        $utcNow = (Get-Date).ToUniversalTime()
        if ($utcNow.Second -lt 45) {
            Write-Host "safe target window start utc=$($utcNow.ToString("yyyy-MM-dd HH:mm:ss"))"
            return
        }

        $waitSeconds = 65 - $utcNow.Second
        Write-Host "near minute boundary. wait $waitSeconds seconds before sending target events"
        Start-Sleep -Seconds $waitSeconds
    }
}

function Send-TrackingEvent {
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
                cpcAmount = $CpcAmount
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

function Wait-CampaignPerformanceRow {
    param(
        [int]$BaselineCount
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)

    while ((Get-Date) -lt $deadline) {
        $count = Query-ClickHouse "
            SELECT count()
            FROM campaign_performance_metrics
            WHERE campaign_id = '$CampaignId'
              AND impression_count = $TargetEventCount
              AND click_count = $TargetEventCount
              AND conversion_count = $TargetEventCount
              AND total_deducted_amount = $($CpcAmount * $TargetEventCount)
              AND total_conversion_amount = $($ConversionAmount * $TargetEventCount)
              AND partial = false
        "

        if ([int]$count -gt $BaselineCount) {
            return Query-ClickHouse "
                SELECT
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
                  AND impression_count = $TargetEventCount
                  AND click_count = $TargetEventCount
                  AND conversion_count = $TargetEventCount
                  AND total_deducted_amount = $($CpcAmount * $TargetEventCount)
                  AND total_conversion_amount = $($ConversionAmount * $TargetEventCount)
                  AND partial = false
                ORDER BY created_at DESC
                LIMIT 1
                FORMAT TabSeparated
            "
        }

        Write-Host "waiting campaign performance consistency row count=$count baseline=$BaselineCount"
        Start-Sleep -Seconds $PollSeconds
    }

    throw "timeout waiting campaign performance consistency row"
}

function Wait-SegmentPerformanceRows {
    param(
        [int]$BaselineCount
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)

    while ((Get-Date) -lt $deadline) {
        $count = Query-ClickHouse "
            SELECT count()
            FROM segment_performance_metrics
            WHERE campaign_id = '$CampaignId'
              AND segment_id IN ('segment-game', 'segment-high-value')
              AND impression_count = $TargetEventCount
              AND click_count = $TargetEventCount
              AND conversion_count = $TargetEventCount
              AND total_deducted_amount = $($CpcAmount * $TargetEventCount)
              AND total_conversion_amount = $($ConversionAmount * $TargetEventCount)
              AND partial = false
        "

        if ([int]$count -ge ($BaselineCount + 2)) {
            $rows = Query-ClickHouse "
                SELECT
                    segment_id,
                    window_start,
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
                  AND segment_id IN ('segment-game', 'segment-high-value')
                  AND impression_count = $TargetEventCount
                  AND click_count = $TargetEventCount
                  AND conversion_count = $TargetEventCount
                  AND total_deducted_amount = $($CpcAmount * $TargetEventCount)
                  AND total_conversion_amount = $($ConversionAmount * $TargetEventCount)
                  AND partial = false
                ORDER BY created_at DESC
                LIMIT 2
                FORMAT TabSeparated
            "

            return @($rows -split "`r?`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        }

        Write-Host "waiting segment performance consistency rows count=$count baseline=$BaselineCount"
        Start-Sleep -Seconds $PollSeconds
    }

    throw "timeout waiting segment performance consistency rows"
}

function Get-CampaignConsistencyBaselineCount {
    return [int](Query-ClickHouse "
        SELECT count()
        FROM campaign_performance_metrics
        WHERE campaign_id = '$CampaignId'
          AND impression_count = $TargetEventCount
          AND click_count = $TargetEventCount
          AND conversion_count = $TargetEventCount
          AND total_deducted_amount = $($CpcAmount * $TargetEventCount)
          AND total_conversion_amount = $($ConversionAmount * $TargetEventCount)
          AND partial = false
    ")
}

function Get-SegmentConsistencyBaselineCount {
    return [int](Query-ClickHouse "
        SELECT count()
        FROM segment_performance_metrics
        WHERE campaign_id = '$CampaignId'
          AND segment_id IN ('segment-game', 'segment-high-value')
          AND impression_count = $TargetEventCount
          AND click_count = $TargetEventCount
          AND conversion_count = $TargetEventCount
          AND total_deducted_amount = $($CpcAmount * $TargetEventCount)
          AND total_conversion_amount = $($ConversionAmount * $TargetEventCount)
          AND partial = false
    ")
}

function Assert-MartMatchesSource {
    param(
        [string]$SourceTable,
        [string]$MartTable,
        [string]$ReportDate,
        [string]$BatchRunId,
        [string]$CountExpression
    )

    $source = Query-ClickHouse "
        SELECT
            $CountExpression,
            sum(impression_count),
            sum(click_count),
            sum(conversion_count),
            sum(total_deducted_amount),
            sum(total_conversion_amount)
        FROM $SourceTable
        WHERE toDate(window_start) = '$ReportDate'
          AND partial = false
        FORMAT TabSeparated
    "

    $mart = Query-ClickHouse "
        SELECT
            count(),
            sum(impression_count),
            sum(click_count),
            sum(conversion_count),
            sum(total_deducted_amount),
            sum(total_conversion_amount)
        FROM $MartTable
        WHERE report_date = '$ReportDate'
          AND batch_run_id = '$BatchRunId'
        FORMAT TabSeparated
    "

    $sourceValues = $source -split "`t"
    $martValues = $mart -split "`t"
    $metricNames = @(
        "group_count",
        "impression_count",
        "click_count",
        "conversion_count",
        "total_deducted_amount",
        "total_conversion_amount"
    )

    for ($i = 0; $i -lt $metricNames.Count; $i++) {
        Assert-Equal `
            -Name "$MartTable.$($metricNames[$i])" `
            -Expected $sourceValues[$i] `
            -Actual $martValues[$i]
    }
}

$testId = [Guid]::NewGuid().ToString("N").Substring(0, 12)
$reportDate = (Get-Date).ToUniversalTime().ToString("yyyy-MM-dd")
$campaignRunId = "portfolio_campaign_consistency_$testId"
$segmentRunId = "portfolio_segment_consistency_$testId"

$expectedCost = $CpcAmount * $TargetEventCount
$expectedRevenue = $ConversionAmount * $TargetEventCount
$expectedCtr = [decimal]1
$expectedCvr = [decimal]1
$expectedRoas = [decimal]($expectedRevenue / $expectedCost)

Write-Host "data consistency test id=$testId"
Write-Host "report date=$reportDate"
Write-Host "campaignId=$CampaignId"

Wait-SafeTargetWindow

$campaignBaselineCount = Get-CampaignConsistencyBaselineCount
$segmentBaselineCount = Get-SegmentConsistencyBaselineCount

Write-Host "campaign consistency baseline count=$campaignBaselineCount"
Write-Host "segment consistency baseline count=$segmentBaselineCount"

for ($i = 1; $i -le $TargetEventCount; $i++) {
    Send-TrackingEvent `
        -ImpressionId "consistency-impression-$testId-$i" `
        -ClickId "consistency-click-$testId-$i" `
        -BatchName "target-window-$i"
}

Write-Host "wait $WindowCloseDelaySeconds seconds for next event-time window"
Start-Sleep -Seconds $WindowCloseDelaySeconds

Send-TrackingEvent `
    -ImpressionId "consistency-impression-$testId-closer" `
    -ClickId "consistency-click-$testId-closer" `
    -BatchName "window-closer"

Write-Host "wait $PerformanceEmitDelaySeconds seconds for performance timer"
Start-Sleep -Seconds $PerformanceEmitDelaySeconds

Write-Host ""
Write-Host "validate campaign performance"
$campaignRow = Wait-CampaignPerformanceRow -BaselineCount $campaignBaselineCount
$campaignValues = $campaignRow -split "`t"
$windowStart = $campaignValues[0]

if ([string]::IsNullOrWhiteSpace($windowStart)) {
    throw "campaign performance window_start is empty"
}
Write-Host "OK campaign.window_start=$windowStart"
Assert-Equal -Name "campaign.impression_count" -Expected $TargetEventCount -Actual $campaignValues[2]
Assert-Equal -Name "campaign.click_count" -Expected $TargetEventCount -Actual $campaignValues[3]
Assert-Equal -Name "campaign.conversion_count" -Expected $TargetEventCount -Actual $campaignValues[4]
Assert-Equal -Name "campaign.total_deducted_amount" -Expected $expectedCost -Actual $campaignValues[5]
Assert-Equal -Name "campaign.total_conversion_amount" -Expected $expectedRevenue -Actual $campaignValues[6]
Assert-DecimalClose -Name "campaign.ctr" -Expected $expectedCtr -Actual ([decimal]$campaignValues[7])
Assert-DecimalClose -Name "campaign.cvr" -Expected $expectedCvr -Actual ([decimal]$campaignValues[8])
Assert-DecimalClose -Name "campaign.roas" -Expected $expectedRoas -Actual ([decimal]$campaignValues[9])
Assert-Equal -Name "campaign.partial" -Expected "false" -Actual $campaignValues[10]

Write-Host ""
Write-Host "validate segment performance"
$segmentRows = Wait-SegmentPerformanceRows -BaselineCount $segmentBaselineCount
foreach ($segmentRow in $segmentRows) {
    $segmentValues = $segmentRow -split "`t"
    $segmentId = $segmentValues[0]

    Assert-Equal -Name "$segmentId.window_start" -Expected $windowStart -Actual $segmentValues[1]
    Assert-Equal -Name "$segmentId.impression_count" -Expected $TargetEventCount -Actual $segmentValues[2]
    Assert-Equal -Name "$segmentId.click_count" -Expected $TargetEventCount -Actual $segmentValues[3]
    Assert-Equal -Name "$segmentId.conversion_count" -Expected $TargetEventCount -Actual $segmentValues[4]
    Assert-Equal -Name "$segmentId.total_deducted_amount" -Expected $expectedCost -Actual $segmentValues[5]
    Assert-Equal -Name "$segmentId.total_conversion_amount" -Expected $expectedRevenue -Actual $segmentValues[6]
    Assert-DecimalClose -Name "$segmentId.ctr" -Expected $expectedCtr -Actual ([decimal]$segmentValues[7])
    Assert-DecimalClose -Name "$segmentId.cvr" -Expected $expectedCvr -Actual ([decimal]$segmentValues[8])
    Assert-DecimalClose -Name "$segmentId.roas" -Expected $expectedRoas -Actual ([decimal]$segmentValues[9])
    Assert-Equal -Name "$segmentId.partial" -Expected "false" -Actual $segmentValues[10]
}

Write-Host ""
Write-Host "trigger airflow marts and validate mart aggregates"
Trigger-Dag `
    -DagId "daily_campaign_performance_mart" `
    -RunId $campaignRunId `
    -ReportDate $reportDate

Trigger-Dag `
    -DagId "daily_segment_performance_mart" `
    -RunId $segmentRunId `
    -ReportDate $reportDate

Wait-MartSuccess `
    -MartName "daily_campaign_performance_mart" `
    -RunId $campaignRunId

Wait-MartSuccess `
    -MartName "daily_segment_performance_mart" `
    -RunId $segmentRunId

$campaignBatchRunId = Query-ClickHouse "
    SELECT batch_run_id
    FROM daily_mart_batch_runs
    WHERE mart_name = 'daily_campaign_performance_mart'
      AND startsWith(batch_run_id, '$campaignRunId')
      AND status = 'SUCCESS'
    ORDER BY created_at DESC
    LIMIT 1
"

$segmentBatchRunId = Query-ClickHouse "
    SELECT batch_run_id
    FROM daily_mart_batch_runs
    WHERE mart_name = 'daily_segment_performance_mart'
      AND startsWith(batch_run_id, '$segmentRunId')
      AND status = 'SUCCESS'
    ORDER BY created_at DESC
    LIMIT 1
"

Assert-MartMatchesSource `
    -SourceTable "campaign_performance_metrics" `
    -MartTable "daily_campaign_performance_mart" `
    -ReportDate $reportDate `
    -BatchRunId $campaignBatchRunId `
    -CountExpression "uniqExact(campaign_id)"

Assert-MartMatchesSource `
    -SourceTable "segment_performance_metrics" `
    -MartTable "daily_segment_performance_mart" `
    -ReportDate $reportDate `
    -BatchRunId $segmentBatchRunId `
    -CountExpression "uniqExact(tuple(segment_id, campaign_id))"

Write-Host ""
Write-Host "data consistency test passed"
