@echo off
chcp 65001 >nul
setlocal

set CAMPAIGN_ID=state-restore-%RANDOM%-%RANDOM%
set CHARGE_ID=%CAMPAIGN_ID%-charge
set PRIMARY_DEDUCT_ID=%CAMPAIGN_ID%-primary-deduct
set DR_DEDUCT_ID=%CAMPAIGN_ID%-dr-deduct

echo [1] MirrorMaker 재시작
docker compose restart mirror-maker
timeout /t 15 /nobreak >nul

echo [2] Primary에 1000원 충전
echo %CAMPAIGN_ID%#{"eventId":"%CHARGE_ID%","campaignId":"%CAMPAIGN_ID%","amount":1000,"currency":"KRW","type":"CHARGE"} | docker exec -i kafka-1 /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka-1:9092 --topic budget-events --property parse.key=true --property key.separator=#

timeout /t 3 /nobreak >nul

echo [3] Primary에서 300원 차감
echo %CAMPAIGN_ID%#{"eventId":"%PRIMARY_DEDUCT_ID%","campaignId":"%CAMPAIGN_ID%","amount":300,"currency":"KRW","type":"DEDUCT"} | docker exec -i kafka-1 /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka-1:9092 --topic budget-events --property parse.key=true --property key.separator=#

timeout /t 5 /nobreak >nul

echo [4] Primary 잔액 700원 확인
docker exec kafka-1 /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka-1:9092 --topic budget-results --from-beginning --timeout-ms 5000 --property print.key=true 2>nul | findstr %PRIMARY_DEDUCT_ID%

echo [5] Changelog와 offset의 DR 복제 대기
timeout /t 70 /nobreak >nul

echo [6] Primary ad-streams 중지
docker compose stop ad-streams

echo [7] Primary Kafka 장애 발생
docker stop kafka-1 kafka-2 kafka-3

echo [8] ad-streams를 DR Kafka 설정으로 실행
docker compose -f docker-compose.yml -f docker-compose.dr.yml up -d --no-deps --force-recreate ad-streams

timeout /t 20 /nobreak >nul

echo [9] DR에서 200원 차감
echo %CAMPAIGN_ID%#{"eventId":"%DR_DEDUCT_ID%","campaignId":"%CAMPAIGN_ID%","amount":200,"currency":"KRW","type":"DEDUCT"} | docker exec -i kafka-dr-1 /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka-dr-1:9092 --topic budget-events --property parse.key=true --property key.separator=#

timeout /t 5 /nobreak >nul

echo [10] DR 복구 결과 확인
docker exec kafka-dr-1 /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka-dr-1:9092 --topic budget-results --from-beginning --timeout-ms 5000 --property print.key=true 2>nul | findstr %DR_DEDUCT_ID%

echo.
echo 성공 기준: remainingBudget가 500이면 State Store 복구 성공
echo 테스트 캠페인: %CAMPAIGN_ID%

endlocal