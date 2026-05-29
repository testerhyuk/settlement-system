@echo off

echo [%time%] k6 부하 시작
start /b docker-compose run --rm k6 run /scripts/click-test.js

echo [%time%] 30초 대기
timeout /t 30 /nobreak >nul

echo [%time%] DB DOWN
curl -X POST http://localhost:3000/api/annotations -u admin:admin -H "Content-Type: application/json" -d "{\"text\":\"DB DOWN\",\"tags\":[\"chaos\"]}"

docker run --rm -v /var/run/docker.sock:/var/run/docker.sock gaiaadm/pumba pause --duration 300s postgres-settlement

echo [%time%] DB UP
curl -X POST http://localhost:3000/api/annotations -u admin:admin -H "Content-Type: application/json" -d "{\"text\":\"DB UP\",\"tags\":[\"chaos\"]}"

echo [%time%] 완료