# Evaluación del proyecto según la rúbrica

**Proyecto:** Sistema Web de Evaluación de Proyectos  
**Fecha de revisión:** 17 de noviembre de 2026  
**Alcance:** revisión estática del código fuente, la configuración y la documentación. No se ejecutaron pruebas ni se comprobó conectividad en tiempo de ejecución.

## Resumen

El proyecto ya cuenta con una aplicación web de gestión académica, una API REST en Spring Boot, persistencia JPA, autenticación JWT y selección manual entre MySQL y PostgreSQL. La estructura cubre la mayor parte de las funciones de la rúbrica. Antes de presentarlo, conviene corregir la diferencia entre las credenciales de Docker y `application.properties`, actualizar el README y realizar una validación de ejecución de ambos motores.

| # | Criterio | Estado | Evaluación breve |
|---:|---|---|---|
| 1 | Instalación y configuración del proyecto | **Parcial** | Hay archivos de configuración y Docker Compose, pero las credenciales configuradas para las bases no coinciden entre sí. La guía de instalación también necesita actualizarse. |
| 2 | Uso correcto del framework | **Cumple en revisión estática** | Se usan React/TypeScript/Vite y Spring Boot con controladores, servicios, repositorios, DTO, JPA y configuración por beans. |
| 3 | Diseño de interfaz | **Cumple parcialmente** | Hay una interfaz navegable con páginas por rol, tema e idioma. La revisión visual no cubre todas las páginas, tamaños de pantalla ni accesibilidad. |
| 4 | Funcionalidad CRUD | **Cumple en código; falta validación en ejecución** | Hay operaciones de gestión para usuarios, materias, secciones, matrículas, grupos, empresas, proyectos y criterios. |
| 5 | Conexión a base de datos/API | **Parcial** | Existe API REST y selector manual MySQL/PostgreSQL. Las credenciales no concuerdan con Docker Compose y no se verificaron conexiones en ejecución. |
| 6 | Validaciones | **Cumple parcialmente** | Hay validación de DTO y reglas en servicios, pero debe verificarse el comportamiento de errores y reglas en los flujos completos. |
| 7 | Organización del código | **Cumple** | Backend separado en controladores, servicios, repositorios, DTO, modelos, seguridad y configuración; frontend separado por páginas, componentes, contexto y API. |
| 8 | Seguridad básica | **Cumple parcialmente** | JWT, BCrypt, CORS y autorización por roles están implementados. Hay secretos sensibles en configuración versionable y el token se guarda en `localStorage`. |
| 9 | Rendimiento y optimización | **Parcial / no verificado** | Se usa `open-in-view=false` y hay consultas específicas para estadísticas, pero no se hizo perfilado ni revisión integral de consultas/carga. |
| 10 | Presentación y documentación | **Parcial** | Existe README con descripción y arquitectura, pero algunas partes están desactualizadas y faltan instrucciones de instalación, configuración y demostración coherentes con el estado actual. |

## Hallazgos por criterio

### 1. Instalación y configuración del proyecto — Parcial

**Evidencia:** `pom.xml`, `evaluacion-front/package.json`, `docker-compose.yml` y `src/main/resources/application.properties`.

- El backend requiere Java 17 y Maven; el frontend usa React, TypeScript y Vite.
- Docker Compose define MariaDB en `localhost:3307` y PostgreSQL en `localhost:5432`.
- En `application.properties`, MySQL está configurado con usuario `root` y contraseña `123456`. En Docker Compose, MariaDB define `root123` como contraseña root y además crea el usuario `evaluacion` con contraseña `evaluacion123`.
- PostgreSQL también presenta diferencia: la aplicación indica `daniel` / `123456`, mientras Docker Compose define `evaluacion` / `evaluacion123`.
- El selector está en `app.db.active`: `primary` elige MySQL y `fallback` elige PostgreSQL. No cambia automáticamente al otro motor.
- El README no documenta todavía el selector ni refleja el esquema de dos motores.

**Acción recomendada:** elegir una sola pareja de usuario/contraseña por motor y usarla igual en Docker Compose y `application.properties`. Documentar cómo iniciar los contenedores y cómo cambiar `app.db.active`. No se recomienda guardar credenciales reales ni secretos JWT en el repositorio.

### 2. Uso correcto del framework — Cumple en revisión estática

**Evidencia:** `src/main/java/.../controller`, `service`, `repository`, `dto`; `evaluacion-front/src`.

- Spring Boot organiza las rutas REST en controladores y la lógica de negocio en servicios.
- Spring Data JPA se usa mediante repositorios y modelos persistentes.
- Los DTO desacoplan las solicitudes/respuestas de las entidades.
- El frontend es una SPA React con TypeScript y rutas con React Router; Axios centraliza llamadas a la API.

El cumplimiento funcional requiere ejecutar los flujos de punta a punta; esta revisión no hizo esa ejecución.

### 3. Diseño de interfaz — Cumple parcialmente

**Evidencia:** `evaluacion-front/src/pages`, `components`, `context/ThemeContext.tsx`, `context/LanguageContext.tsx`.

- Hay páginas separadas para gestión, evaluación, reportes, matrículas, ciclos y vistas de alumno.
- Hay contexto de tema e idioma y una estructura de navegación común.
- La captura de referencia muestra una interfaz consistente con navegación lateral y tarjetas.

No se verificaron visualmente todas las páginas, su adaptación a móvil, navegación por teclado, contraste ni etiquetas para lectores de pantalla. El contexto de traducción no equivale por sí solo a una auditoría completa de cada texto.

### 4. Funcionalidad CRUD — Cumple en código; falta validación en ejecución

**Evidencia:** controladores y servicios de `Usuario`, `Materia`, `Seccion`, `Subseccion`, `Matricula`, `Grupo`, `Empresa`, `Proyecto` y `Criterio`.

Se encuentran rutas de creación, consulta, actualización y eliminación en los módulos administrativos correspondientes. También existen acciones de dominio como asignar docentes, matricular alumnos, asociar miembros a grupos y gestionar proyectos.

Para confirmar el criterio en una demostración, se debe ejecutar cada operación y comprobar que los cambios se persisten y que los errores se comunican correctamente.

### 5. Conexión a base de datos/API — Parcial

**Evidencia:** `DataSourceConfig.java`, `application.properties`, `docker-compose.yml`, controladores REST.

- La API REST se implementa en Spring MVC y usa JSON.
- `DataSourceConfig` selecciona una sola conexión según `app.db.active`.
- Hibernate está configurado con `ddl-auto=update`; esto actualiza el esquema, no sincroniza ni copia datos entre MySQL y PostgreSQL.
- Las dos bases mantienen datos independientes. Cambiar el selector no migra usuarios ni registros.
- La revisión detecta credenciales distintas entre Docker Compose y el archivo de propiedades, por lo que una conexión puede fallar aunque el puerto esté abierto.

**Acción recomendada:** igualar las credenciales, comprobar que los puertos publicados estén activos y confirmar en el log tanto el motor seleccionado como el mensaje `Conectado a ...`. Para cambiar de motor conservando datos se necesita exportar/importar o migrar los datos.

### 6. Validaciones — Cumple parcialmente

**Evidencia:** `dto/*Dto.java`, controladores con `@Valid`, servicios de negocio y `GlobalExceptionHandler.java`.

- Se validan campos obligatorios, formatos de correo, longitudes y rangos numéricos en varios DTO.
- Las calificaciones y ponderaciones tienen límites declarados.
- Los servicios aplican reglas de dominio, por ejemplo pertenencia a grupos, asignaciones, ciclos y acceso a recursos.

No se hizo una ejecución sistemática con entradas válidas e inválidas. Deben verificarse mensajes y códigos HTTP, así como reglas límite y referencias inexistentes.

### 7. Organización del código — Cumple

**Evidencia:** estructura de `src/main/java/...` y `evaluacion-front/src`.

El backend separa configuración, controladores, DTO, excepciones, modelos, repositorios, seguridad y servicios. El frontend separa páginas, componentes, API, contextos, hooks, tipos y utilidades. Esta separación facilita mantener y extender módulos.

### 8. Seguridad básica — Cumple parcialmente

**Evidencia:** `SecurityConfig.java`, `JwtAuthFilter.java`, `JwtService.java`, `UsuarioDetailsService.java`, `application.properties`, `AuthContext.tsx`, `api/axios.ts`.

- Se utiliza BCrypt para almacenar contraseñas y JWT para autenticar solicitudes.
- Las rutas tienen reglas de acceso por rol y CORS se restringe a un origen configurado.
- CSRF está deshabilitado, una decisión habitual para una API stateless que autentica con bearer token; debe conservarse el control de origen y la protección del token.
- El JWT secret está escrito en `application.properties`; debe trasladarse a una variable de entorno o a un gestor de secretos antes de compartir o publicar el repositorio.
- El frontend guarda el token en `localStorage`, lo que lo expone ante una vulnerabilidad XSS. Para despliegues reales conviene evaluar cookies `HttpOnly`, `Secure` y `SameSite`.
- La autorización debe comprobar no solo el rol, sino también que el docente/evaluador/alumno tenga acceso al recurso específico solicitado.

### 9. Rendimiento y optimización — Parcial / no verificado

**Evidencia:** `spring.jpa.open-in-view=false`, `EstadisticasRepository.java`, servicios y repositorios.

- `open-in-view` está desactivado, lo que ayuda a evitar consultas perezosas durante la serialización de respuestas.
- El repositorio de estadísticas documenta consultas destinadas a evitar N+1 en ese módulo.
- No se encontró evidencia de pruebas de carga, métricas de latencia, índices revisados para consultas frecuentes ni perfilado de todas las pantallas.

No se puede afirmar rendimiento adecuado sin medirlo con un conjunto representativo de datos.

### 10. Presentación y documentación — Parcial

**Evidencia:** `README.md`, `evaluacion-front/README.md`, `docker-compose.yml`, `application.properties`.

- El README explica el problema, objetivos, arquitectura y modelo de datos.
- Hay discrepancias con el sistema actual: describe MySQL como única base, el diagrama contiene una relación materia-sección que ya no representa necesariamente el modelo vigente y deja el grupo académico sin completar.
- Faltan pasos completos y coherentes para configurar credenciales, elegir motor, iniciar backend/frontend, crear o cargar la base y preparar una demostración.
- Este documento añade la evaluación de la rúbrica, pero no sustituye una guía de instalación actualizada ni capturas de evidencia de pruebas.

## Preparación recomendada antes de entregar

1. Unificar credenciales de MySQL y PostgreSQL entre Docker Compose y `application.properties`.
2. Actualizar el README con requisitos, instalación, variables/propiedades, selección manual del motor y ejecución de frontend/backend.
3. Confirmar por separado inicio de sesión, operaciones CRUD, validaciones, permisos por rol, recuperación de contraseña y persistencia en ambos motores.
4. Revisar autorización por pertenencia del usuario a cada recurso y retirar secretos del repositorio.
5. Añadir evidencias de ejecución: capturas de pantallas, logs de conexión y resultados de las verificaciones funcionales.

## Límites de esta evaluación

Este documento describe lo que se observa en el código y la configuración del proyecto. No certifica conectividad, comportamiento en navegador, seguridad frente a ataques, rendimiento ni compatibilidad de todos los motores: esos puntos requieren validación de ejecución y pruebas controladas.
