@echo off

echo [%time%] k6 부하 시작
start /b docker-compose run --rm k6 run /scripts/click-test.js

echo [%time%] 30초 대기
timeout /t 30 /nobreak >nul

echo [%time%] STREAMS KILL
curl -X POST http://localhost:3000/api/annotations -u admin:admin -H "Content-Type: application/json" -d "{\"text\":\"STREAMS KILL\",\"tags\":[\"chaos\"]}"

docker kill ad-streams

echo [%time%] 10초 대기
timeout /t 10 /nobreak >nul

echo [%time%] STREAMS RESTART
curl -X POST http://localhost:3000/api/annotations -u admin:admin -H "Content-Type: application/json" -d "{\"text\":\"STREAMS RESTART\",\"tags\":[\"chaos\"]}"

docker start ad-streams

echo [%time%] 완료