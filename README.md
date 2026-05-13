# Team Task Manager

A full-stack Spring Boot web app for managing team projects, members, tasks, and progress tracking with JWT authentication and project-level role-based access control.

## Features

- User signup and login with JWT authentication
- Project creation and project membership management
- Project roles: `ADMIN` and `MEMBER`
- Task creation, assignment, editing, and status tracking
- Dashboard stats for total, todo, in-progress, done, and overdue tasks
- Validation for auth requests
- Integration tests for authentication, RBAC, task assignment, and dashboard flows
- Basic frontend served from Spring Boot static resources

## Tech Stack

- Java 17
- Spring Boot 3
- Spring Web
- Spring Security
- JWT
- Spring Data JPA
- PostgreSQL
- H2 for integration tests
- Maven
- HTML, CSS, and JavaScript

## Frontend

The app includes a basic browser UI served by Spring Boot from `src/main/resources/static`.

Local URL:

```text
http://localhost:8080
```

## API Endpoints

### Auth

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/auth/signup` | Register a new user |
| `POST` | `/api/auth/login` | Login and receive JWT |

### Projects

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/projects` | Create a project |
| `GET` | `/api/projects` | Get current user's projects |
| `GET` | `/api/projects/{id}` | Get project by ID |
| `POST` | `/api/projects/{id}/members` | Add a member to a project |
| `GET` | `/api/projects/{id}/members` | Get project members |

### Tasks

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/tasks` | Create a task |
| `GET` | `/api/tasks/my` | Get tasks assigned to current user |
| `GET` | `/api/tasks/project/{projectId}` | Get tasks in a project |
| `PUT` | `/api/tasks/{id}` | Update task details |
| `PUT` | `/api/tasks/{id}/status` | Update task status |
| `GET` | `/api/tasks/dashboard` | Get dashboard task statistics |

## Role-Based Access Control

- Project creator is automatically added as project `ADMIN`.
- Only project admins can add members.
- Only project members can view project details, members, and project tasks.
- Only project admins can create, edit, or reassign tasks.
- Task assignees and project admins can update task status.
- Tasks can only be assigned to users who are members of the project.

## Local Setup

### Prerequisites

- Java 17
- Maven or included Maven wrapper
- PostgreSQL

### Database

Create a PostgreSQL database:

```sql
CREATE DATABASE taskmanager;
```

Update `src/main/resources/application.properties` with your local database credentials if needed.

### Run The App

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The API runs on:

```text
http://localhost:8080
```

## Testing

The project includes integration tests using `MockMvc` and an in-memory H2 database. These tests cover authentication, project membership, RBAC, task assignment, task status updates, and dashboard counts.

Run tests:

```bash
./mvnw test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Latest local test result:

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Example Request Flow

1. Signup or login to receive a JWT.
2. Create a project.
3. Add another user as a project member.
4. Create a task and assign it to the member.
5. Login as the member and view assigned tasks.
6. Update task status.
7. Check dashboard statistics.

Use the JWT in protected requests:

```text
Authorization: Bearer <token>
```

## Deployment

Deployment target: Railway.

Live URL:

```text
https://taskmanager-production-4c87.up.railway.app
```

Required production environment variables:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
JWT_EXPIRATION
PORT
```

## Submission

- Live URL: https://taskmanager-production-4c87.up.railway.app
- GitHub Repo: https://github.com/shimitpathak77/taskmanager
