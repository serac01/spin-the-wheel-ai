# Spin the Wheel (Prototype)

Full-stack prototype with:

- **Backend**: Spring Boot (Java) API + SSE streaming endpoints
- **Frontend**: Angular app consuming the API (generated OpenAPI client)

## Repo layout

- `spin-the-wheel/` — Spring Boot backend
- `spin-the-wheel-ngapp/` — Angular frontend

## Prerequisites

- **JDK 20+** (the Maven compiler plugin targets Java 20)
- **Node.js 18.19+ / 20+** (Angular 19)
- (Optional) **Firebase service account JSON** if you want Firestore seed lookup

## Quick start (local dev)

### 1) Backend (Spring Boot)

From the backend folder:

```powershell
cd .\spin-the-wheel
mvn -DskipTests package; java -jar .\target\demo-0.0.1-SNAPSHOT.jar
```

If you have a working Maven Wrapper setup (i.e. `spin-the-wheel/.mvn/wrapper/*` exists), you can also run:

```powershell
cd .\spin-the-wheel
.\mvnw.cmd spring-boot:run
```

Backend defaults to:

- [http://localhost:8080](http://localhost:8080)
- OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- Swagger UI: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

### 2) Frontend (Angular)

In a separate terminal:

```powershell
cd .\spin-the-wheel-ngapp
npm install --legacy-peer-deps
npm run start
```

Frontend defaults to:

- [http://localhost:4200](http://localhost:4200)

> CORS is configured in the backend to allow `http://localhost:4200`.

## Configuration (.env)

The backend reads environment variables from the OS **or** from a `.env` file located in the backend working directory (`spin-the-wheel/.env`).

Create `spin-the-wheel/.env` with:

```dotenv
# Required for text/image generation
HUGGINGFACE_API_TOKEN=hf_...

# Optional: enable Firestore seed lookup (otherwise the app runs with Firestore disabled)
GOOGLE_APPLICATION_CREDENTIALS=C:\\path\\to\\service-account.json
FIREBASE_PROJECT_ID=your-firebase-project-id
```

Notes:

- If `GOOGLE_APPLICATION_CREDENTIALS` and/or `FIREBASE_PROJECT_ID` are missing, the backend logs that Firestore is disabled and continues using a fallback seed.

## API overview

Base paths:

- `GET /api/parameterization/genders`
- `GET /api/parameterization/times`
- `GET /api/parameterization/places`

Spin endpoints:

- `POST /api/spin/story`
- `POST /api/spin/story/stream` (Server-Sent Events)
- `POST /api/spin/image` (returns an image)
- `POST /api/spin/compare-scenarios`
- `POST /api/spin/compare-scenarios/stream` (Server-Sent Events)

## Regenerate Angular API client (OpenAPI)

The Angular app is set up to regenerate API stubs from the running backend:

```powershell
cd .\spin-the-wheel-ngapp
npm run stubs
```

This expects the backend to be running at `http://localhost:8080/v3/api-docs`.

## Build

Backend JAR:

```powershell
cd .\spin-the-wheel
mvn clean package
```

Frontend production build:

```powershell
cd .\spin-the-wheel-ngapp
npm run build
```
