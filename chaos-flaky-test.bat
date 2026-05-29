@echo off

echo [%time%] k6 부하 시작
start /b docker-compose run --rm k6 run /scripts/click-test.js

echo [%time%] 30초 대기
timeout /t 30 /nobreak >nul

echo [%time%] FLAKY DB 시작
curl -X POST http://localhost:3000/api/annotations -u admin:admin -H "Content-Type: application/json" -d "{\"text\":\"FLAKY START\",\"tags\":[\"chaos\"]}"

REM 5초 다운 + 3초 복구를 38회 반복 (약 5분)
for /l %%i in (1,1,38) do (
    docker run --rm -v /var/run/docker.sock:/var/run/docker.sock gaiaadm/pumba pause --duration 5s postgres-settlement
    timeout /t 3 /nobreak >nul
)

curl -X POST http://localhost:3000/api/annotations -u admin:admin -H "Content-Type: application/json" -d "{\"text\":\"FLAKY END\",\"tags\":[\"chaos\"]}"

echo [%time%] 완료