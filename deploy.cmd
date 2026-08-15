docker build -t quizandar-backend:latest .
docker stop quizandar-backend
docker rm quizandar-backend
docker run -d -p 8080:8080 --name quizandar-backend quizandar-backend:latest
