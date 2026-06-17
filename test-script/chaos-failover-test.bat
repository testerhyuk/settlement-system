@echo off
chcp 65001 >nul
echo ============================================
echo [Failover Test - Application-Level Routing]
echo ============================================

echo [%time%] STEP 1: Normal load on Cluster A (1min)
start /b docker-compose run --rm k6 run /scripts/click-test.js
echo [%time%] Wait 60s (normal load)
timeout /t 60 /nobreak >nul


echo [%time%] STEP 2: Stop Primary Cluster
curl -s -X POST http://localhost:3000/api/annotations -u admin:admin -H "Content-Type: application/json" -d "{\"text\":\"PRIMARY CLUSTER DOWN\",\"tags\":[\"failover\"]}"
docker stop kafka-1 kafka-2 kafka-3
echo [%time%] Wait 10s (outage)
timeout /t 10 /nobreak >nul


echo [%time%] STEP 4: Load on DR (1min)
start /b docker-compose run --rm k6 run /scripts/click-test.js
echo [%time%] Wait 60s (DR load)
timeout /t 60 /nobreak >nul


echo [%time%] STEP 5: Cluster A recovered
curl -s -X POST http://localhost:3000/api/annotations -u admin:admin -H "Content-Type: application/json" -d "{\"text\":\"PRIMARY RECOVERED\",\"tags\":[\"failover\"]}"
docker start kafka-1 kafka-2 kafka-3

:WAIT_PRIMARY_INIT
@REM Primary Kafka Broker Metadata 헬스 체크 질의 수행
docker exec kafka-1 /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list >nul 2>&1
if %errorlevel% neq 0 (
    timeout /t 2 /nobreak >nul
    goto WAIT_PRIMARY_INIT
)

@REM echo [%time%] Wait 60s (MirrorMaker sync)
echo [%time%] Primary Cluster Metadata Stabilized. Waiting for MirrorMaker sync...
timeout /t 60 /nobreak >nul

echo [%time%] STEP 6: Switch back to Primary
curl -s -X POST http://localhost:3000/api/annotations -u admin:admin -H "Content-Type: application/json" -d "{\"text\":\"SWITCH TO PRIMARY\",\"tags\":[\"failover\"]}"
curl -s -X POST http://localhost/api/v1/ops/failover/switch-to-primary
echo.
echo [%time%] Switched to Primary


echo [%time%] STEP 7: Load on Primary (1min)
start /b docker-compose run --rm k6 run /scripts/click-test.js
echo [%time%] Wait 60s (primary load)
timeout /t 60 /nobreak >nul

echo [%time%] DONE
echo ============================================