@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set CAMPAIGN_ID=%1
if "%CAMPAIGN_ID%"=="" set CAMPAIGN_ID=exactly-once-%RANDOM%-%RANDOM%

set CHARGE_EVENT_ID=%CAMPAIGN_ID%-charge
set DEDUCT_EVENT_ID=%CAMPAIGN_ID%-deduct

echo [1] 테스트 campaignId: %CAMPAIGN_ID%

echo [2] 예산 1000 충전
echo %CAMPAIGN_ID%#{"eventId":"%CHARGE_EVENT_ID%","campaignId":"%CAMPAIGN_ID%","amount":1000,"currency":"KRW","type":"CHARGE"} | docker exec -i kafka-1 /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka-1:9092 --topic budget-events --property parse.key=true --property key.separator=#

timeout /t 5 >nul

echo [3] 차감 이벤트 1회 발행
echo %CAMPAIGN_ID%#{"eventId":"%DEDUCT_EVENT_ID%","campaignId":"%CAMPAIGN_ID%","amount":300,"currency":"KRW","type":"DEDUCT"} | docker exec -i kafka-1 /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka-1:9092 --topic budget-events --property parse.key=true --property key.separator=#

echo [4] ad-streams 재시작
docker compose restart ad-streams

timeout /t 8 >nul

echo [5] 같은 eventId 차감 이벤트 재발행
echo %CAMPAIGN_ID%#{"eventId":"%DEDUCT_EVENT_ID%","campaignId":"%CAMPAIGN_ID%","amount":300,"currency":"KRW","type":"DEDUCT"} | docker exec -i kafka-1 /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka-1:9092 --topic budget-events --property parse.key=true --property key.separator=#

timeout /t 8 >nul

echo [6] budget-results 확인
docker exec kafka-1 /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka-1:9092 --topic budget-results --from-beginning --timeout-ms 5000 --property print.key=true 2>nul | findstr "%DEDUCT_EVENT_ID%"

echo.
echo 성공 기준:
echo 위 결과에서 %DEDUCT_EVENT_ID% 가 1개만 나와야 함
echo remainingBudget 이 700이어야 함