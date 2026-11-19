# CampusCore runtime image.
# Build the JAR first (Jenkins does this in its Package stage):  mvn clean package
FROM eclipse-temurin:21-jre

WORKDIR /app
COPY target/campuscore.jar /app/campuscore.jar

# File-based H2 lives in /data and assignment attachments in /data/uploads. Mount a volume at /data to keep both.
# Numeric user so no extra packages are needed; Kubernetes sets fsGroup to the same id.
RUN mkdir -p /data/uploads && chown -R 10001:10001 /data
USER 10001:10001
VOLUME ["/data"]

ENV DB_URL=jdbc:h2:file:/data/campuscore \
    UPLOAD_DIR=/data/uploads \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/campuscore.jar"]
