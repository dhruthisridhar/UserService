# User Service

A user management service built with **Java 17** and **Vert.x 5**.

## Prerequisites
```
For Local Development
- Java 17
- Gradle

For Docker Deployment
- Docker (installed and running)

For Testing
- curl or Postman for testing the API
```

## Getting Start

### Build and Run with Docker
```
docker build -t user-crud .
docker run -p 8080:8080 user-crud
```

Server starts on port 8080.

## API

### Create User
```
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Dhruthi SM","email":"dhruthism@example.com"}'
```

Response:
```
{
  "id": "{UUID}",
  "name": "Dhruthi SM",
  "email": "dhruthism@example.com"
}
```

### Get User
```
curl http://localhost:8080/users/{UUID}
```

### Update Email
```
curl -X PUT http://localhost:8080/users/{UUID}/email \
  -H "Content-Type: application/json" \
  -d '{"email":"dhruthinew@example.com"}'
```

### Delete User
```
curl -X DELETE http://localhost:8080/users/{UUID}
```


