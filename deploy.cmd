@echo off
set DOCKER_BUILDKIT=0
docker build -t quizandar-backend:latest -f Dockerfile .
if %errorlevel% neq 0 exit /b %errorlevel%
echo Stopping and removing old container...
docker stop quizandar-backend 2>nul
docker rm quizandar-backend 2>nul
echo Starting new container...
docker run -d -p 8080:8080 --name quizandar-backend quizandar-backend:latest
echo Done. Swagger UI: http://localhost:8080/swagger-ui/index.html
