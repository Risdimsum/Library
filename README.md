# Digital Library Management System

A Spring Boot web application for managing library books, students, and book issuing — deployed on Railway with PostgreSQL.

---

## Project Purpose

Manage books, students, and the issuing process in a simple web app for Admin/Librarian use.

---

## Features

- Login / Logout
- Add Book
- Book Record (list + search)
- Add Student
- Student Report
- Issue Book
- Issue Report
- Error page handling

---

## Main Flow

1. Add Student
2. Add Book
3. View Book Record
4. Issue Book
5. View Issue Report
6. Logout

---

## Data Entities

- `books`
- `users`
- `book_issues`
- `book_requests` (optional)

---

## Business Rules

- Required form validation
- Student email must be unique
- Cannot issue when stock is unavailable
- Issuing reduces available copies

---

## Tools Used

- Java 21
- Spring Boot 4.0.2
- Gradle
- Spring Web
- Thymeleaf
- Spring Data JPA
- PostgreSQL
- Railway (hosting + environment management)

---

## Deployment (Railway + PostgreSQL)

1. Push the source code to GitHub.
2. Create a new project in Railway and connect the GitHub repository.
3. Add a **PostgreSQL service** in the same Railway project.
4. Railway automatically generates database environment variables.
5. Deploy the Spring Boot service.

Railway automatically provides the following environment variables:

- `DATABASE_URL`
- `PGHOST`
- `PGPORT`
- `PGDATABASE`
- `PGUSER`
- `PGPASSWORD`
- `PORT`

---

## Application Configuration

File location: `src/main/resources/application.properties`

```properties
server.port=${PORT:8080}
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://${PGHOST:localhost}:${PGPORT:5432}/${PGDATABASE:postgres}}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:${PGUSER:postgres}}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:${PGPASSWORD:}}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.main.lazy-initialization=true
spring.datasource.hikari.maximum-pool-size=2
server.tomcat.threads.max=5
```

This configuration allows the application to:
- Use Railway environment variables in production
- Use local PostgreSQL settings during development

---

## Database Driver (Gradle)

PostgreSQL driver is included in `build.gradle`:

```groovy
runtimeOnly 'org.postgresql:postgresql'
```

---

## Start Command (Procfile)

```
web: java -XX:+UseSerialGC -Xms32m -Xmx192m -XX:MaxMetaspaceSize=256m -XX:ReservedCodeCacheSize=64m -jar build/libs/Test.jar
```

---

## Run Locally

```bash
./gradlew bootRun
```

---

## Verify Deployment

1. Open Railway deployment logs.
2. Confirm the application starts without database errors.
3. Open the Railway application URL.
4. Test create, read, update, delete operations to verify PostgreSQL integration.

---

## AI Usage

**AI Agents used:** ChatGPT, Claude, Codex

**Prompts used:**

1. Add some feature — add Book class
2. Add some feature on BookService
3. Adjust sequence to make it in order
4. Student login, add new book, book record — digitize UI with Bootstrap
5. Connect directory and adjust parts to fit with templates after adding new features
6. Project prompt (list format):
   - Project Name: Digital Library Management System
   - Goal: Manage books, students, and issuing process in a simple web app
   - Scope: Core CRUD-like operations for books/students + issue tracking
   - Users: Admin/Librarian
7. Analyze Spring Boot deployment OOM: Metaspace issue on Railway free tier
8. Propose configuration changes to reduce memory usage in Spring Boot + Hibernate
9. Generate optimized `application.properties` for small containers (≤256MB RAM)
10. Suggest JVM options optimized for small containers running Spring Boot on Java 21
11. Modify main Spring Boot class to restrict component/entity/repository scanning
12. Review Gradle dependencies and recommend removals to reduce memory footprint
13. Suggest architectural alternatives if app cannot run within Railway free tier limits
