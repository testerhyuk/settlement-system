@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set KEY=dlq-test-%RANDOM%-%RANDOM%

echo [1] 깨진 JSON을 budget-results에 발행
echo %KEY%#{"broken": | docker exec -i kafka-1 /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka-1:9092 --topic budget-results --property parse.key=true --property key.separator=#

timeout /t 8 >nul

echo [2] budget-results.DLT 확인
docker exec kafka-1 /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka-1:9092 --topic budget-results.DLT --from-beginning --timeout-ms 5000 --property print.key=true 2>nul | findstr "%KEY%"

echo.
echo [3] DB에 저장된 최신 PENDING DLQ 조회
for /f "tokens=* delims=" %%i in ('docker exec postgres-settlement psql -U hyuk -d settlement-system -t -A -c "SELECT dlq_message_id FROM dlq_message_entity WHERE dlq_status = 'PENDING' ORDER BY created_at DESC LIMIT 1;"') do set DLQ_ID=%%i

echo DLQ_ID=%DLQ_ID%

echo.
echo [4] DLQ 목록 API 확인
curl -s http://localhost/api/dlq
echo.

echo.
echo [5] replay 요청
curl -s -X POST http://localhost/api/dlq/%DLQ_ID%/replay
echo.

timeout /t 8 >nul

echo.
echo [6] replay 실패 결과 확인
docker exec postgres-settlement psql -U hyuk -d settlement-system -c "SELECT dlq_message_id, dlq_status, retry_count, raw_message FROM dlq_message_entity WHERE dlq_message_id = '%DLQ_ID%';"

echo.
echo 성공 기준:
echo dlq_status = REPLAY_FAILED
echo retry_count = 1