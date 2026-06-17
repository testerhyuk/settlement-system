@echo off

echo [%time%] k6 부하 시작
start /b docker-compose run --rm k6 run /scripts/click-test.js

echo [%time%] 30초 대기
timeout /t 30 /nobreak >nul

echo [%time%] BROKER DOWN
curl -X POST http://localhost:3000/api/annotations -u admin:admin -H "Content-Type: application/json" -d "{\"text\":\"BROKER DOWN\",\"tags\":[\"chaos\"]}"

docker stop kafka-3

echo [%time%] 1분 대기 (브로커 다운 상태)
timeout /t 60 /nobreak >nul

echo [%time%] BROKER UP
curl -X POST http://localhost:3000/api/annotations -u admin:admin -H "Content-Type: application/json" -d "{\"text\":\"BROKER UP\",\"tags\":[\"chaos\"]}"

docker start kafka-3

echo [%time%] 완료 (부하는 5분까지 계속됨)