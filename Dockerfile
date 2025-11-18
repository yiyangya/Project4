# Use a Linix image with Tomcat 10
# FROM tomcat:10.1.0-M5-jdk16-openjdk-slim-bullseye

# Use Tomcat 9 with JDK 8 (compatible with javax.servlet)
FROM tomcat:9-jdk8

# Expose port 8080
EXPOSE 8080

# Copy in our ROOT.war to the right place in the container
COPY ROOT.war /usr/local/tomcat/webapps/
