+ Spring Boot web application for managing library books, deployed on Railway with PostgreSQL.

# Deployment (Railway + PostgreSQL)

1. Push the source code to GitHub.
2. Create a new project in Railway and connect the GitHub repository.
3. Add a **PostgreSQL service** in the same Railway project.
4. Railway automatically generates database environment variables.
5. Deploy the Spring Boot service.

Railway automatically provides the following environment variables:

* DATABASE_URL
* PGHOST
* PGPORT
* PGDATABASE
* PGUSER
* PGPASSWORD
* PORT

# Application Configuration

- File location:

src/main/resources/application.properties

- Configuration used in this project:
server.port=${PORT:8080}
spring.datasource.url=${SPRING_DATASOURCE_URL:${DATABASE_URL:jdbc:postgresql://${PGHOST:localhost}:${PGPORT:5432}/${PGDATABASE:postgres}}}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:${PGUSER:postgres}}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:${PGPASSWORD:}}
spring.datasource.driver-class-name=org.postgresql.Driver

This configuration allows the application to:

* Use Railway environment variables in production
* Use local PostgreSQL settings during development

# Database Driver (Gradle)

PostgreSQL driver is included in `build.gradle`:

```
runtimeOnly 'org.postgresql:postgresql'
```

# Start Command (Railway)

The application is started using a **Procfile**.

Procfile:

web: java -jar build/libs/Test.jar

The Gradle `bootJar` task builds the application as:

```
build/libs/Test.jar
```

Railway runs this file to start the Spring Boot application.

## Run Locally

To start the application locally:

./gradlew bootRun

If running with a local PostgreSQL database, configure environment variables or use the default values provided in `application.properties`.

##Verify Deployment

1. Open Railway deployment logs.
2. Confirm the application starts without database errors.
3. Open the Railway application URL.
4. Test the web pages that create, read, update, or delete books to verify PostgreSQL integration.

## Project Purpose

This project demonstrates how to deploy a **Spring Boot + PostgreSQL** web application on Railway with environment-based configuration.

+ Tools Used :
* Java 17
* Spring Boot 3.2.5
* Gradle
* Spring Web
* Thymeleaf
* Spring JDBC
* PostgreSQL
* Railway (hosting + environment management)

## AI Usage
 + AI agents : ChatGPT, Claude, Codex
 + Prompt :
1. add some feature add Book class
2. add some feature on BookService,
3. so in this case can u adjust i will give u the sequence or u can adjust this sequence to make it in order
4. student login, add new book, book record i will let u see the photots, for the ui not like this u can digitize it with bootstrap
5. can u connect directory or adjust some part to fit with my templates aftet i add new some feature and inetegrate or connect it to main java config, controller and mdoel and more
6. Project prompt (list format):
Project Name: Digital Library Management System
Goal: Manage books, students, and issuing process in a simple web app
Scope: Core CRUD-like operations for books/students + issue tracking
Users: Admin/Librarian
-Features:
Login/Logout
Add Book
Book Record (list + search)
Add Student
Student Report
Issue Book
Issue Report
Error page handling
Main Flow:
Add Student
Add Book
View Book Record
Issue Book
View Issue Report
Logout
Data Entities:
books
users
book_issues
book_requests (optional)
Business Rules:
Required form validation
Student email must be unique
Cannot issue when stock is unavailable
Issuing reduces available copies
7. Analyze the following Spring Boot deployment issue. The application runs on Railway free tier with very limited RAM. It uses Spring Boot 3.2.5, Java 21, Hibernate 6.4.4, PostgreSQL,
and Spring Data JPA with 4 repositories. The application fails during startup with `java.lang.OutOfMemoryError: Metaspace`.
Explain why Metaspace exhaustion happens in small containers and what parts of Spring Boot and Hibernate contribute to class loading and dynamic bytecode generation.
8. Based on the previous analysis, propose configuration changes to reduce memory usage in a Spring Boot + Hibernate application. Focus on minimizing class loading, reducing Hibernate bytecode enhancement,
limiting repository scanning, and lowering connection pool size.
9. Generate an optimized `application.properties` configuration suitable for running a Spring Boot 3 application with JPA in a very small container (≤256MB RAM).
Include settings for Hibernate, HikariCP, JPA, and logging that help reduce startup memory usage.
10. Suggest JVM options optimized for small containers running Spring Boot on Java 21. The goal is to prevent Metaspace OOM while keeping heap usage small.
Include recommended values for `-Xmx`, `-Xms`, `MaxMetaspaceSize`, and garbage collector selection.
11. Provide code modifications for the main Spring Boot application class to restrict component scanning, entity scanning, and repository scanning to specific packages in order to reduce unnecessary class loading.
12. Review a typical Gradle dependency list for a Spring Boot project and recommend which Spring Boot starters or libraries can be safely removed to reduce memory footprint if they are not strictly required.
13. If the application still cannot run within the memory limits of Railway free tier, suggest architectural alternatives such as replacing Hibernate with JDBC, reducing repository usage, or switching deployment platforms.
