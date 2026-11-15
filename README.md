# CampusCore (smoke-test version)

Demo logins (password = ID): admin / admin123, 22CS001 (student), FAC001 (faculty)

Run locally (port 8081 so it does not clash with Jenkins on 8080):
    mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

Build and test:        mvn clean package
Build image:           docker build -t campuscore:latest .
Deploy manually:       kubectl apply -f k8s/
Open the app:          http://localhost:30080   (or: kubectl port-forward svc/campuscore-svc 8081:80)
