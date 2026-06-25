param(
    [string]$ReportDate = (Get-Date).ToUniversalTime().ToString("yyyy-MM-dd"),
    [int]$FailureTimeoutSeconds = 180,
    [int]$SuccessTimeoutSeconds = 180,
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

function Query-AirflowMetaDb {
    param([string]$Sql)

    docker exec airflow-postgres psql `
        -U airflow `
        -d airflow `
        -t `
        -A `
        -c $Sql
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

function Wait-ClickHouseReady {
    $deadline = (Get-Date).AddSeconds($SuccessTimeoutSeconds)

    while ((Get-Date) -lt $deadline) {
        try {
            Query-ClickHouse "SELECT 1" | Out-Null
            Write-Host "clickhouse is ready"
            return
        } catch {
            Write-Host "waiting clickhouse ready"
            Start-Sleep -Seconds $PollSeconds
        }
    }

    throw "timeout waiting clickhouse ready"
}

function Wait-AirflowTaskFailure {
    param(
        [string]$DagId,
        [string]$TaskId,
        [string]$RunId
    )

    $deadline = (Get-Date).AddSeconds($FailureTimeoutSeconds)

    while ((Get-Date) -lt $deadline) {
        $state = Query-AirflowMetaDb "
            SELECT state
            FROM task_instance
            WHERE dag_id = '$DagId'
              AND task_id = '$TaskId'
              AND run_id = '$RunId'
            ORDER BY start_date DESC NULLS LAST
            LIMIT 1;
        "

        if ($state -eq "up_for_retry" -or $state -eq "failed") {
            Write-Host "airflow task failure detected dag=$DagId task=$TaskId state=$state"
            return
        }

        Write-Host "waiting airflow failure dag=$DagId task=$TaskId state=$state"
        Start-Sleep -Seconds $PollSeconds
    }

    throw "timeout waiting airflow task failure dag=$DagId task=$TaskId runId=$RunId"
}

function Wait-MartSuccess {
    param(
        [string]$MartName,
        [string]$RunId
    )

    $deadline = (Get-Date).AddSeconds($SuccessTimeoutSeconds)

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

            throw "mart failed unexpectedly after clickhouse recovery: $MartName error=$errorMessage"
        }

        Write-Host "waiting mart=$MartName status=$status"
        Start-Sleep -Seconds $PollSeconds
    }

    throw "timeout waiting mart success: $MartName runId=$RunId"
}

$testId = [Guid]::NewGuid().ToString("N").Substring(0, 12)

$campaignFailureRunId = "portfolio_campaign_clickhouse_outage_$testId"
$campaignRecoveryRunId = "portfolio_campaign_clickhouse_recovery_$testId"
$segmentRecoveryRunId = "portfolio_segment_clickhouse_recovery_$testId"

Write-Host "airflow clickhouse outage/retry test id=$testId"
Write-Host "report date=$ReportDate"

try {
    Write-Host ""
    Write-Host "STEP 1. stop clickhouse"
    docker stop clickhouse-settlement | Out-Host

    Write-Host ""
    Write-Host "STEP 2. trigger DAGs while clickhouse is down"
    Trigger-Dag `
        -DagId "daily_campaign_performance_mart" `
        -RunId $campaignFailureRunId `
        -ReportDate $ReportDate

    Wait-AirflowTaskFailure `
        -DagId "daily_campaign_performance_mart" `
        -TaskId "rebuild_daily_campaign_mart" `
        -RunId $campaignFailureRunId
} finally {
    Write-Host ""
    Write-Host "STEP 3. start clickhouse"
    docker start clickhouse-settlement | Out-Host
    Wait-ClickHouseReady
}

Write-Host ""
Write-Host "STEP 4. trigger DAGs again after clickhouse recovery"
Trigger-Dag `
    -DagId "daily_campaign_performance_mart" `
    -RunId $campaignRecoveryRunId `
    -ReportDate $ReportDate

Trigger-Dag `
    -DagId "daily_segment_performance_mart" `
    -RunId $segmentRecoveryRunId `
    -ReportDate $ReportDate

Wait-MartSuccess `
    -MartName "daily_campaign_performance_mart" `
    -RunId $campaignRecoveryRunId

Wait-MartSuccess `
    -MartName "daily_segment_performance_mart" `
    -RunId $segmentRecoveryRunId

Write-Host ""
Write-Host "airflow failure task states"
Query-AirflowMetaDb "
    SELECT dag_id, task_id, run_id, state, try_number
    FROM task_instance
    WHERE run_id = '$campaignFailureRunId'
    ORDER BY dag_id, task_id;
"

Write-Host ""
Write-Host "daily_mart_batch_runs recovery records"
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
    WHERE startsWith(batch_run_id, '$campaignRecoveryRunId')
       OR startsWith(batch_run_id, '$segmentRecoveryRunId')
    ORDER BY created_at
"

Write-Host ""
Write-Host "latest campaign mart"
Query-ClickHouse "
    SELECT
        report_date,
        campaign_id,
        impression_count,
        click_count,
        conversion_count,
        total_deducted_amount,
        total_conversion_amount,
        roas,
        batch_run_id
    FROM daily_campaign_performance_latest
    ORDER BY report_date DESC
    LIMIT 5
"

Write-Host ""
Write-Host "latest segment mart"
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
        roas,
        batch_run_id
    FROM daily_segment_performance_latest
    ORDER BY report_date DESC, segment_id
    LIMIT 5
"

Write-Host ""
Write-Host "airflow clickhouse outage/retry test passed"
