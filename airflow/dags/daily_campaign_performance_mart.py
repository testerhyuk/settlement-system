import os
from datetime import timedelta, timezone

import clickhouse_connect
import pendulum
from airflow import DAG
from airflow.operators.python import PythonOperator


MART_NAME = "daily_campaign_performance_mart"


def get_clickhouse_client():
    return clickhouse_connect.get_client(
        host=os.environ["CLICKHOUSE_HOST"],
        port=int(os.environ["CLICKHOUSE_PORT"]),
        username=os.environ["CLICKHOUSE_USERNAME"],
        password=os.environ["CLICKHOUSE_PASSWORD"],
        database=os.environ["CLICKHOUSE_DATABASE"],
    )


def build_batch_run_id(context):
    return f"{context['run_id']}__try_{context['task_instance'].try_number}"


def escape_sql_string(value):
    return str(value).replace("'", "''")


def task_started_at_sql(context):
    started_at = context["task_instance"].start_date
    if started_at is None:
        return "now64(3)"

    started_at = started_at.astimezone(timezone.utc)
    started_at_text = started_at.strftime("%Y-%m-%d %H:%M:%S.%f")[:23]
    return f"toDateTime64('{started_at_text}', 3, 'UTC')"


def insert_batch_status(
    client,
    mart_name,
    report_date,
    batch_run_id,
    status,
    source_count,
    mart_count,
    started_at_sql="now64(3)",
    error_message=None,
):
    safe_error_message = None
    if error_message is not None:
        safe_error_message = escape_sql_string(error_message)[:1000]

    error_message_sql = "NULL" if safe_error_message is None else f"'{safe_error_message}'"
    finished_at_sql = "NULL" if status == "RUNNING" else "now64(3)"

    sql = f"""
        INSERT INTO settlement_analytics.daily_mart_batch_runs
        (
            mart_name,
            report_date,
            batch_run_id,
            status,
            source_count,
            mart_count,
            started_at,
            finished_at,
            error_message
        )
        VALUES
        (
            '{escape_sql_string(mart_name)}',
            '{escape_sql_string(report_date)}',
            '{escape_sql_string(batch_run_id)}',
            '{escape_sql_string(status)}',
            {source_count},
            {mart_count},
            {started_at_sql},
            {finished_at_sql},
            {error_message_sql}
        )
    """

    client.command(sql)


def record_batch_failure(context):
    report_date = context["data_interval_start"].date().isoformat()
    batch_run_id = build_batch_run_id(context)
    exception = context.get("exception")
    started_at_sql = task_started_at_sql(context)

    client = get_clickhouse_client()

    source_count_sql = f"""
        SELECT uniqExact(campaign_id)
        FROM settlement_analytics.campaign_performance_metrics
        WHERE toDate(window_start) = '{report_date}'
          AND partial = false
    """

    mart_count_sql = f"""
        SELECT count()
        FROM settlement_analytics.daily_campaign_performance_mart
        WHERE report_date = '{report_date}'
          AND batch_run_id = '{escape_sql_string(batch_run_id)}'
    """

    source_count = client.query(source_count_sql).result_rows[0][0]
    mart_count = client.query(mart_count_sql).result_rows[0][0]

    insert_batch_status(
        client=client,
        mart_name=MART_NAME,
        report_date=report_date,
        batch_run_id=batch_run_id,
        status="FAILED",
        source_count=source_count,
        mart_count=mart_count,
        started_at_sql=started_at_sql,
        error_message=str(exception) if exception is not None else "Task failed",
    )


def assert_aggregate_matches(source_row, mart_row, report_date, batch_run_id):
    metric_names = [
        "campaign_count",
        "impression_count",
        "click_count",
        "conversion_count",
        "total_deducted_amount",
        "total_conversion_amount",
    ]

    for index, metric_name in enumerate(metric_names):
        if source_row[index] != mart_row[index]:
            raise ValueError(
                f"Campaign mart validation failed. "
                f"report_date={report_date}, "
                f"batch_run_id={batch_run_id}, "
                f"metric={metric_name}, "
                f"source={source_row[index]}, "
                f"mart={mart_row[index]}"
            )


def rebuild_daily_campaign_mart(**context):
    report_date = context["dag_run"].conf.get(
        "report_date",
        context["data_interval_start"].date().isoformat(),
    )
    batch_run_id = build_batch_run_id(context)
    started_at_sql = task_started_at_sql(context)

    client = get_clickhouse_client()
    source_count = 0
    mart_count = 0

    source_aggregate_sql = f"""
        SELECT
            uniqExact(campaign_id) AS campaign_count,
            sum(impression_count) AS impression_count,
            sum(click_count) AS click_count,
            sum(conversion_count) AS conversion_count,
            sum(total_deducted_amount) AS total_deducted_amount,
            sum(total_conversion_amount) AS total_conversion_amount
        FROM settlement_analytics.campaign_performance_metrics
        WHERE toDate(window_start) = '{report_date}'
          AND partial = false
    """

    insert_sql = f"""
        INSERT INTO settlement_analytics.daily_campaign_performance_mart
        (
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
        )
        SELECT
            report_date,
            campaign_id,
            impression_count,
            click_count,
            conversion_count,
            total_deducted_amount,
            total_conversion_amount,
            if(impression_count = 0, 0, click_count / impression_count) AS ctr,
            if(click_count = 0, 0, conversion_count / click_count) AS cvr,
            if(total_deducted_amount = 0, 0, total_conversion_amount / total_deducted_amount) AS roas,
            '{batch_run_id}' AS batch_run_id
        FROM
        (
            SELECT
                toDate(window_start) AS report_date,
                campaign_id,
                sum(impression_count) AS impression_count,
                sum(click_count) AS click_count,
                sum(conversion_count) AS conversion_count,
                sum(total_deducted_amount) AS total_deducted_amount,
                sum(total_conversion_amount) AS total_conversion_amount
            FROM settlement_analytics.campaign_performance_metrics
            WHERE toDate(window_start) = '{report_date}'
              AND partial = false
            GROUP BY report_date, campaign_id
        )
    """

    mart_aggregate_sql = f"""
        SELECT
            count() AS campaign_count,
            sum(impression_count) AS impression_count,
            sum(click_count) AS click_count,
            sum(conversion_count) AS conversion_count,
            sum(total_deducted_amount) AS total_deducted_amount,
            sum(total_conversion_amount) AS total_conversion_amount
        FROM settlement_analytics.daily_campaign_performance_mart
        WHERE report_date = '{report_date}'
          AND batch_run_id = '{batch_run_id}'
    """

    insert_batch_status(
        client=client,
        mart_name=MART_NAME,
        report_date=report_date,
        batch_run_id=batch_run_id,
        status="RUNNING",
        source_count=0,
        mart_count=0,
        started_at_sql=started_at_sql,
    )

    source_row = client.query(source_aggregate_sql).result_rows[0]
    source_count = source_row[0]

    if source_count == 0:
        insert_batch_status(
            client=client,
            mart_name=MART_NAME,
            report_date=report_date,
            batch_run_id=batch_run_id,
            status="SUCCESS",
            source_count=0,
            mart_count=0,
            started_at_sql=started_at_sql,
        )
        print(f"No source campaign metrics for report_date={report_date}. skip mart build.")
        return

    client.command(insert_sql)

    mart_row = client.query(mart_aggregate_sql).result_rows[0]
    mart_count = mart_row[0]

    assert_aggregate_matches(source_row, mart_row, report_date, batch_run_id)

    insert_batch_status(
        client=client,
        mart_name=MART_NAME,
        report_date=report_date,
        batch_run_id=batch_run_id,
        status="SUCCESS",
        source_count=source_count,
        mart_count=mart_count,
        started_at_sql=started_at_sql,
    )


default_args = {
    "owner": "hyuk",
    "retries": 2,
    "retry_delay": timedelta(minutes=5),
}

with DAG(
    dag_id="daily_campaign_performance_mart",
    description="Build daily campaign performance mart from minute-level campaign metrics",
    start_date=pendulum.datetime(2026, 1, 1, tz="UTC"),
    schedule="@daily",
    catchup=False,
    default_args=default_args,
    tags=["settlement", "campaign", "mart"],
) as dag:
    rebuild_daily_campaign_mart_task = PythonOperator(
        task_id="rebuild_daily_campaign_mart",
        python_callable=rebuild_daily_campaign_mart,
        execution_timeout=timedelta(minutes=20),
        on_retry_callback=record_batch_failure,
        on_failure_callback=record_batch_failure,
    )
