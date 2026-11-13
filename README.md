# Sistema Web de Evaluación de Proyectos

Aplicación web para administrar proyectos académicos y centralizar su evaluación. El sistema organiza materias, secciones, subsecciones, grupos y alumnos; permite registrar empresas y proyectos, definir criterios y modalidades, invitar evaluadores, capturar calificaciones, calcular notas, publicar resultados y generar reportes.

> **Institución:** ITCA-FEPADE, Centro Regional Santa Ana
> **Carrera:** Técnico en Ingeniería de Desarrollo de Software
> **Módulo:** Utilización de Frameworks · **Ciclo:** 02 · **Año:** 2026

## Funcionalidades

- Autenticación con JWT y contraseñas cifradas con BCrypt.
- Control de acceso por roles: `ADMIN`, `DOCENTE`, `JURADO` y `ALUMNO`.
- Gestión de usuarios, roles, materias, secciones, subsecciones, matrículas y grupos.
- Registro de empresas y proyectos académicos.
- Criterios configurables por materia y por grupo, con ponderaciones.
- Esquemas para evaluación docente/jurado, autoevaluación y evaluación pública.
- Invitación y asignación de evaluadores.
- Evaluaciones en estado borrador o enviada.
- Cálculo de notas finales y consulta de resultados.
- Reportes, estadísticas y exportación a Excel/PDF.
- Recuperación de contraseña mediante código y soporte opcional de correo SMTP.
- Datos iniciales y conjunto de datos de demostración idempotente.

## Tecnologías

| Capa | Tecnología |
|---|---|
| Frontend | React 19, TypeScript 6, Vite 8 |
| Navegación y UI | React Router 7, Tailwind CSS, Lucide React |
| Gráficas | Recharts |
| Cliente HTTP | Axios |
| Backend | Java 17, Spring Boot 4.1.1 |
| Persistencia | Spring Data JPA, Hibernate |
| Seguridad | Spring Security, JWT (JJWT 0.12.6), BCrypt |
| Base de datos | MariaDB/MySQL; PostgreSQL disponible |
| Exportación | Apache POI y OpenPDF |
| Correo | Spring Mail |

## Arquitectura

El proyecto utiliza una arquitectura cliente-servidor:

```text
Frontend React/Vite (5173)
        |
        | HTTP/JSON + encabezado de autorización
        v
API REST Spring Boot (8080)
        |
        v
Servicios -> Repositorios Spring Data JPA
        |
        v
MariaDB/MySQL o PostgreSQL
```

### Capas del backend

| Paquete | Responsabilidad |
|---|---|
| `controller` | Endpoints REST de la aplicación |
| `dto` | Objetos de entrada y salida de la API |
| `model` | Entidades JPA |
| `repository` | Acceso a datos con Spring Data |
| `service` | Reglas de negocio y cálculos |
| `security` | Filtro JWT, tokens y carga de usuarios |
| `config` | Seguridad, datasource y datos iniciales |
| `exception` | Manejo centralizado de errores |

## Roles

| Rol | Acceso principal |
|---|---|
| `ADMIN` | Administración general de usuarios, catálogos y configuración |
| `DOCENTE` | Gestión académica, proyectos, criterios, evaluadores y evaluaciones |
| `JURADO` | Evaluación de proyectos y consulta de reportes permitidos |
| `ALUMNO` | Consulta de proyecto y criterios, autoevaluación y consulta de notas |

El backend trabaja de forma stateless. El frontend guarda el JWT en `localStorage`, Axios lo agrega a las solicitudes y `RutaPrivada` protege las vistas según el rol.

## Rutas principales del frontend

| Ruta | Descripción | Acceso |
|---|---|---|
| `/login` | Inicio de sesión | Público |
| `/dashboard` | Panel principal | Autenticados |
| `/gestion/:modulo` | Gestión de entidades y catálogos | `ADMIN`, `DOCENTE` |
| `/modalidades` | Esquemas de evaluación | `ADMIN`, `DOCENTE` |
| `/evaluacion-publica` | Configuración de evaluación pública | `ADMIN`, `DOCENTE` |
| `/notas` | Notas finales | `ADMIN`, `DOCENTE` |
| `/evaluacion/:modo` | Evaluaciones según modalidad | `ADMIN`, `DOCENTE`, `JURADO` |
| `/reportes` | Reportes y estadísticas | `ADMIN`, `DOCENTE`, `JURADO` |
| `/mi-proyecto` | Proyecto del alumno | `ALUMNO` |
| `/mis-criterios` | Criterios aplicables | `ALUMNO` |
| `/autoevaluacion` | Autoevaluación | `ALUMNO` |
| `/mi-nota` | Nota del alumno | `ALUMNO` |
| `/publico/:codigo` | Consulta pública por código | Público |

La API utiliza `/api` como prefijo. Entre sus áreas principales están autenticación, usuarios, alumnos, materias, secciones, grupos, empresas, proyectos, criterios, esquemas, invitaciones, evaluaciones, resultados, notas y reportes.

## Modelo de datos

Las tablas principales son:

- `roles` y `usuarios`.
- `materias`, `secciones`, `subsecciones`, `matriculas`, `asignaciones`, `grupos` y `grupo_alumnos`.
- `empresas` y `proyectos`.
- `criterios`, esquemas de evaluación y sus relaciones de criterios.
- `evaluadores_invitados`, `evaluaciones` y sus tablas de detalle.
- Entidades para autoevaluación, evaluación pública, recuperación y reportes.

El esquema principal está en [`database/schema.sql`](database/schema.sql). También existe [`database/schema-postgres.sql`](database/schema-postgres.sql) para PostgreSQL.

La calificación ponderada se obtiene a partir de las notas otorgadas y el peso de cada criterio. Las evaluaciones enviadas se consolidan para obtener la nota final del proyecto conforme a la modalidad configurada.

## Requisitos

- JDK 17.
- Maven o el wrapper incluido (`mvnw.cmd`).
- Node.js y npm.
- Docker Desktop, si se usará la base de datos incluida.

## Configuración y ejecución

### 1. Base de datos con Docker

Desde la raíz:

```powershell
docker compose up -d db
```

El servicio utiliza MariaDB 10.11 y publica la base de datos en `localhost:3307`. El script `database/schema.sql` se ejecuta al crear el volumen por primera vez.

Si se necesita reinicializar la base local:

```powershell
docker compose down -v
docker compose up -d db
```

> El segundo comando elimina el volumen de datos de Docker. Úsalo solo en entornos locales de desarrollo.

### 2. Backend

Revisa `src/main/resources/application.properties` y adapta la conexión a la base de datos y el origen CORS a tu entorno. No publiques claves JWT, contraseñas ni credenciales SMTP.

Ejecuta:

```powershell
.\mvnw.cmd spring-boot:run
```

El backend queda disponible normalmente en `http://localhost:8080`.

### 3. Frontend

```powershell
Set-Location .\evaluacion-front
npm install
npm run dev
```

El frontend queda disponible normalmente en `http://localhost:5173`.

## Datos iniciales

`DataInitializer` crea los roles y usuarios base, y carga datos de demostración cuando `app.seed.demo=true` (valor predeterminado). La carga incluye materias, secciones, grupos, empresas, proyectos, criterios, esquemas, invitaciones y evaluaciones.

Para iniciar sin datos de demostración:

```properties
app.seed.demo=false
```

Los usuarios y contraseñas de prueba se definen en el inicializador y en los scripts de desarrollo. No deben reutilizarse en producción.

## Validación

Backend:

```powershell
.\mvnw.cmd test
```

Frontend:

```powershell
Set-Location .\evaluacion-front
npm run lint
npm run build
```

## Estructura del repositorio

```text
.
├── src/main/java/com/example/Sistema_de_Evaluacion_de_Proyectos/
│   ├── config/
│   ├── controller/
│   ├── dto/
│   ├── exception/
│   ├── model/
│   ├── repository/
│   ├── security/
│   └── service/
├── src/main/resources/application.properties
├── src/test/
├── database/
│   ├── schema.sql
│   └── schema-postgres.sql
├── evaluacion-front/
│   └── src/
├── docker-compose.yml
├── pom.xml
├── CONTEXTO_PROYECTO.md
└── README.md
```

## Convenciones para contribuir

- Mantener la separación `controller -> service -> repository`.
- Usar DTOs en los límites de la API.
- Validar las entradas con Bean Validation y utilizar el manejador global de excepciones.
- Registrar en `SecurityConfig` los permisos de cualquier endpoint nuevo.
- Mantener consistentes los roles entre base de datos, JWT, backend y frontend.
- Actualizar los esquemas SQL cuando cambien las entidades.
- Proteger secretos mediante configuración local o variables de entorno.
- Proteger en el frontend las vistas que requieran autenticación con `RutaPrivada`.

## Estado y pendientes de producción

El sistema cuenta actualmente con autenticación, gestión académica, proyectos, criterios, modalidades, evaluaciones, autoevaluación, evaluación pública, notas y reportes.

Antes de desplegarlo en producción se debe revisar:

- Gestión de secretos y configuración por ambiente.
- Migraciones de base de datos en lugar de depender de `ddl-auto=update`.
- Pruebas de integración de flujos críticos y permisos por rol.
- Configuración segura del correo de recuperación.
- Desactivación o aislamiento de usuarios y datos de demostración.
- Revisión de CORS, límites de archivos y observabilidad.

Para una descripción más detallada del dominio y la organización interna, consulta [`CONTEXTO_PROYECTO.md`](CONTEXTO_PROYECTO.md).

## Equipo

| Carnet | Integrante |
|---|---|
| 024325 | Yesica Elizabeth Anaya Castaneda |
| 020225 | Fernando Daniel Castaneda Herrera |
| 026125 | Fatima Nahomy Miranda Urbano |
| 030225 | Dennis Alexander Serrano Loarca |
| 039725 | Sayra Hermione Valladares Peña |
| 041225 | Bryan Alexander Vanegas Rivas |
