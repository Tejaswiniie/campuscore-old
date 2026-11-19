# CampusCore - College Management System

Spring Boot 3.3.5, Java 21, Maven, Thymeleaf, Spring Security, Spring Data JPA, H2 (file based), JUnit 5.
CI/CD: Git/GitHub, Jenkins, Docker, Kubernetes.

## Roles and what they can do

| Role | Login | Can do |
|---|---|---|
| Admin | `/admin/login` (user `admin`) | Add, view, search, edit, delete students and faculty. Departments, sections and the Department > Sections > Students overview. Subjects and which faculty teaches which class. Read and answer complaints. |
| Student | `/login` (roll number) | Own profile, attendance and CGPA rings, assignments for own year and section with attachment download, faculty and idea mentors, Idea Hub, complaints, change password. |
| Faculty | `/login` (faculty ID) | Own profile, assigned classes and their students (read only), create/edit/delete own assignments with one optional attachment, review ideas (approve = become mentor, reject with reason), change password. |

First passwords: students and faculty start with their own ID as the password. Change it under "Change password".
The admin password is `admin123` for local development only. Set `ADMIN_PASSWORD` for anything else (it applies only the first time the database is created).

Attendance rings: 75 to 100 Excellent (green), 60 to below 75 Satisfactory (amber), below 60 Needs Improvement (red).
CGPA rings: 8 to 10 Excellent (green), 6 to below 8 Satisfactory (amber), below 6 Needs Improvement (red).
The ring fill is the stored value itself (attendance out of 100, CGPA out of 10). Values outside 0 to 100 and 0 to 10 are rejected when saved.

## Run locally

```bash
mvn spring-boot:run
# open http://localhost:8080/admin/login   (admin / admin123)
```
Data is stored in `./data/campuscore.mv.db` and survives restarts. Delete the `data` folder for a clean start.

Optional sample data (departments, sections, subjects, faculty, students, one assignment):
```bash
APP_DEMO_DATA=true mvn spring-boot:run          # Windows PowerShell: $env:APP_DEMO_DATA="true"; mvn spring-boot:run
```
Departments are fixed: CSM, CSE, ECE, EEE, CSC, CSD and MECH (created automatically). Year of study is chosen from 1st yr to 4th yr.
Sample logins: faculty `FAC001` / `FAC001`, students `CSE2A01` / `CSE2A01`. It only runs when no department exists yet.

### Student registration: Department > Academic year > Section

The four academic years (1st to 4th) are fixed. Sections are registered by the admin under a department and a year. In the student form the section list shows only the sections registered for the chosen department and year, shows "No sections available" when there are none, and is cleared whenever the department or year changes. The server checks the combination again on create and edit and refuses a mismatch.

Without sample data, set things up in this order as admin: Departments (open a department, then add sections) > Classes (add subjects) > Faculty > Students > Classes (assign a faculty member to subject + section).

## Assignment attachments

- A faculty member can attach one file to an assignment: PDF, DOCX, PPTX, XLSX or TXT, up to 10 MB (`UPLOAD_MAX_BYTES`, default 10485760). Empty, oversized and unsupported files are refused, and the real content must match the extension.
- Files are stored in `UPLOAD_DIR` (default `./data/uploads`; `/data/uploads` in Docker and Kubernetes) under random names, never under the uploaded name, and never under static resources. The original name, content type, size and stored name are kept in the `class_assignment` table (5 nullable columns added automatically).
- Downloads go through `/faculty/assignments/{id}/attachment` (the owner) and `/student/assignments/{id}/attachment` (students of the same section only). Anything else gets 403 and anonymous users go to the login page. The download keeps the original file name.
- Replacing or removing an attachment deletes only that assignment's old file. Deleting an assignment, a class or a faculty member also deletes the files that belong to them.
- The uploads are on the same persistent volume as the database. This keeps them across restarts but, like the H2 database, it is a single-replica setup, not high availability.

## Test, package

```bash
mvn clean test
mvn clean package          # produces target/campuscore.jar
java -jar target/campuscore.jar
```
Tests use their own in-memory database and never touch `./data`.

## Docker

```bash
mvn clean package
docker build -t campuscore:local .
docker run --rm -p 8080:8080 -v campuscore-data:/data -e ADMIN_PASSWORD='choose-one' campuscore:local
```

## Kubernetes

One replica and a PersistentVolumeClaim, because the file-based H2 database cannot be shared by several pods.

```bash
kubectl create secret generic campuscore-secrets --from-literal=ADMIN_PASSWORD='choose-a-strong-password'
kubectl apply -f k8s/pvc.yaml -f k8s/deployment.yaml -f k8s/service.yaml
kubectl rollout status deployment/campuscore
kubectl get deployments
kubectl get pods
kubectl get services
```
The Service is a NodePort on 30080. Minikube: `minikube service campuscore --url`. Docker Desktop: http://localhost:30080.
Local clusters need the image on the node: Docker Desktop shares local images; minikube needs `minikube image load campuscore:latest`; kind needs `kind load docker-image campuscore:latest`.

## Jenkins

`Jenkinsfile` stages: Checkout, Verify Java and Maven, Clean, Compile and Test, Package, Build Docker Image, Tag Docker Image, Push Docker Image (Docker Hub), Deploy to Kubernetes, Verify Deployment.
- The push stage needs a Jenkins credential with id `dockerhub` (kind Username with password; the password is a Docker Hub access token).
- It has no `triggers` block, so your existing SCM polling on the job is kept and not duplicated.
- Checkout uses the repository and branch configured on the job (`checkout scm`).
- The image is tagged `campuscore:<build number>`, `campuscore:<commit>` and `campuscore:latest`. The deploy step sets the manifest image to the build-number tag, and the verify step checks the deployed image equals it.
- Agent needs JDK 21, Maven, Docker and kubectl. If kubectl needs a kubeconfig, store it as a Jenkins "Secret file" credential and put its id in `KUBECONFIG_CREDENTIALS_ID`. For minikube or kind set `IMAGE_LOAD_COMMAND`.
- No secrets are in the file.

## Git / GitHub

```bash
git checkout -b feature/v13-attachments-rings
git add -A
git commit -m "CampusCore v13: assignment attachments, section filtering, progress rings, faculty portal"
git push -u origin feature/v13-attachments-rings
# open a pull request and merge to main; Jenkins polling then builds main
```
`.gitignore` excludes `target/`, `data/` and secret files.

## Notes

- Database tables and constraints are generated by Hibernate (`ddl-auto=update`). Section uniqueness is enforced by a database constraint on department + year + name as well as in code.
- Deleting a student removes their login, complaints and idea memberships. Deleting faculty removes their login, classes and assignments. Ideas keep their mentor name.
- Date of birth is optional. Only the admin enters or changes it; a student sees their own.
- Not included, by design: photos, qualifications, internal marks, student assignment submissions, automatic idea team allocation.

## Notes for macOS with Docker Desktop and Jenkins on port 8080

- Jenkins uses port 8080, so run the app locally on 8081: `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`, and use `-p 8081:8080` with `docker run`.
- Docker Desktop does not always expose NodePort 30080 on localhost. Use `kubectl port-forward svc/campuscore 8082:80` and open http://localhost:8082.
- Jenkins runs with a short PATH. The `environment` block in the Jenkinsfile adds the Java 21, Docker and Maven locations. Change the paths if your machine differs.
- Upgrading from V12 needs no cleanup: the Deployment, Service and volume have the same names, and the new `UPLOAD_DIR` setting points at `/data/uploads` on the same volume.
- To check that uploads persist: upload an attachment, run `kubectl delete pod -l app=campuscore`, wait for the new pod and download the file again.
