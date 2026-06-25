param(
    [string]$ReportDate = (Get-Date).ToUniversalTime().ToString("yyyy-MM-dd"),
    [int]$TimeoutSeconds = 120,
    [int]$PollSeconds = 5
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

$testId = [Guid]::NewGuid().ToString("N").Substring(0, 12)
$campaignRunId = "portfolio_campaign_mart_test_$testId"
$segmentRunId = "portfolio_segment_mart_test_$testId"

Write-Host "airflow mart test id=$testId"
Write-Host "report date=$ReportDate"

Trigger-Dag `
    -DagId "daily_campaign_performance_mart" `
    -RunId $campaignRunId `
    -ReportDate $ReportDate

Trigger-Dag `
    -DagId "daily_segment_performance_mart" `
    -RunId $segmentRunId `
    -ReportDate $ReportDate

Wait-MartSuccess `
    -MartName "daily_campaign_performance_mart" `
    -RunId $campaignRunId

Wait-MartSuccess `
    -MartName "daily_segment_performance_mart" `
    -RunId $segmentRunId

Write-Host ""
Write-Host "daily_mart_batch_runs"
Query-ClickHouse "
    SELECT
        mart_name,
        report_date,
        batch_run_id,
        status,
        source_count,
        mart_count,
        error_message
    FROM daily_mart_batch_runs
    WHERE startsWith(batch_run_id, '$campaignRunId')
       OR startsWith(batch_run_id, '$segmentRunId')
    ORDER BY created_at
"

Write-Host ""
Write-Host "daily_campaign_performance_latest"
Query-ClickHouse "
    SELECT
        report_date,
        campaign_id,
        impression_count,
        click_count,
        conversion_count,
        total_deducted_amount,
        total_conversion_amount,
        ctr,
        cvr,
        roas,
        batch_run_id
    FROM daily_campaign_performance_latest
    ORDER BY report_date DESC
    LIMIT 10
"

Write-Host ""
Write-Host "daily_segment_performance_latest"
Query-ClickHouse "
    SELECT
        report_date,
        segment_id,
        campaign_id,
        impression_count,
        click_count,
        conversion_count,
        total_deducted_amount,
        total_conversion_amount,
        ctr,
        cvr,
        roas,
        batch_run_id
    FROM daily_segment_performance_latest
    ORDER BY report_date DESC, segment_id
    LIMIT 10
"

Write-Host ""
Write-Host "airflow mart test passed"
