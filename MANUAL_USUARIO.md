# Manual de usuario

## Sistema Web de Evaluación de Proyectos

**Institución:** ITCA-FEPADE, Centro Regional Santa Ana  
**Versión del manual:** 1.0 · **Fecha:** 17 de noviembre de 2026

Este manual explica cómo ingresar al sistema y completar las tareas habituales de administración, gestión académica y evaluación. Las opciones visibles dependen del rol asignado a la cuenta.

## 1. Ingreso al sistema

1. Abre la dirección web que te proporcionó el administrador.
2. Escribe tu correo y contraseña y selecciona **Iniciar sesión**.
3. El sistema abrirá el panel inicial y mostrará las opciones permitidas para tu rol.
4. Para salir, selecciona **Cerrar sesión** en la parte inferior del menú.

Si olvidaste la contraseña, usa **¿Olvidaste tu contraseña?**, ingresa el correo de tu cuenta y solicita el código. Escribe el código recibido, ingresa y confirma la contraseña nueva y selecciona **Restablecer**. El envío depende de que el correo esté registrado, habilitado para recuperación y de que SMTP esté configurado. En modo de demostración, el código puede quedar únicamente en la consola del backend.

## 2. Elementos generales de la interfaz

- **Menú lateral:** abre los módulos habilitados para tu rol. En pantallas pequeñas puede abrirse con el botón de menú.
- **Idioma:** el botón con el globo alterna entre español e inglés. La preferencia se guarda en el navegador.
- **Tema:** el botón de apariencia alterna el tema disponible.
- **Formularios:** los campos marcados como obligatorios deben completarse. Los mensajes de error aparecen en la página.
- **Listados:** usa los botones de editar, eliminar o acciones adicionales de cada fila. Algunas eliminaciones piden confirmación.

## 3. Roles y opciones

| Rol | Tareas principales |
|---|---|
| Administrador | Administra usuarios, ciclos, materias, secciones, matrículas, grupos, empresas, proyectos, criterios, modalidades, evaluadores, notas y reportes. |
| Docente | Gestiona materias, secciones, matrículas, grupos, empresas, proyectos y criterios; configura modalidades, invita evaluadores, califica y consulta resultados/reportes. |
| Jurado | Consulta proyectos que tiene asignados, registra evaluaciones y consulta reportes permitidos. |
| Alumno | Consulta su proyecto y criterios, evalúa a sus compañeros y revisa su nota cuando se publiquen resultados. |

## 4. Guía para administración y docentes

### 4.1 Preparar un ciclo académico

El administrador abre **Ciclos académicos**.

1. Para registrar uno, escribe el código en formato `NN-AAAA`, por ejemplo `01-2027`, y selecciona **Agregar ciclo**.
2. Revisa el listado de ciclos y usa la acción de estado para activar o desactivar uno.
3. Los ciclos ya presentes en las secciones pueden aparecer automáticamente en el listado.

Los ciclos inactivos no deben usarse para crear nuevas secciones o configurar nuevos periodos. Revisa el estado antes de continuar.

### 4.2 Registrar usuarios

Abre **Usuarios** y completa nombre, apellido, correo, contraseña y rol. La contraseña puede mostrarse u ocultarse con el icono del campo. Guarda el registro y confirma que aparezca en el listado.

Para localizar cuentas existentes, filtra por nombre/correo o rol. Al editar, deja la contraseña vacía si deseas conservarla. Para deshabilitar o retirar un usuario, utiliza la acción disponible y confirma la operación.

### 4.3 Registrar materias y secciones

1. En **Materias**, crea cada materia con su nombre y datos solicitados.
2. En **Secciones**, registra el nombre de la sección y selecciona un ciclo disponible.
3. En el listado de secciones, abre **Subsecciones y materias**.
4. Agrega las subsecciones que correspondan (por ejemplo, A y B).
5. En **Materias y docentes**, selecciona la materia, el docente y, si corresponde, una subsección. Si no eliges una subsección específica, la asignación se aplica a todas las subsecciones de esa sección.
6. Selecciona **Asignar materia** y confirma que aparezca en el listado.

Una materia debe estar asignada al docente y a la sección/subsección correctos para que pueda gestionar los proyectos correspondientes.

### 4.4 Matricular alumnos

1. Abre **Matrículas**.
2. Selecciona una **Sección** y luego una **Subsección**.
3. En **Alumnos sin matrícula**, busca por nombre o correo y marca a los alumnos que deseas agregar.
4. Selecciona **Matricular seleccionados**.
5. Verifica que aparezcan en **Matriculados**.

Para quitar una matrícula, selecciona el icono de eliminar junto al alumno y confirma. Si el alumno pertenece a un grupo de esa subsección, primero quítalo del grupo desde **Alumnos y grupos** y luego vuelve a Matrículas.

### 4.5 Crear grupos y administrar integrantes

1. Abre **Alumnos y grupos** y registra el nombre del grupo.
2. Selecciona la asignación académica disponible (materia, docente y subsección).
3. Guarda el grupo.
4. En el listado, abre la acción de integrantes.
5. Agrega alumnos disponibles y verifica que aparezcan como integrantes.

Usa la asignación correcta para que el grupo quede vinculado a la materia esperada. Un alumno debe estar matriculado en la subsección que corresponde a esa asignación.

### 4.6 Registrar empresas y proyectos

1. Si el proyecto está asociado con una organización, registra primero la empresa en **Empresas**. La empresa puede ser opcional.
2. Abre **Proyectos** y completa título, descripción, grupo, empresa (si aplica) y estado.
3. Guarda y revisa el detalle del proyecto, especialmente grupo, materia/asignación y estado.

El proyecto pertenece al grupo y a la asignación académica con la que fue creado. Verifica esa relación antes de iniciar las evaluaciones.

### 4.7 Configurar criterios

1. Abre **Criterios**.
2. Elige el alcance del criterio: materia, sección/subsección, grupo o proyecto.
3. Selecciona el registro correspondiente, el ciclo si aplica, el nombre y la ponderación.
4. Repite para cada criterio y confirma que el total del conjunto sume exactamente **100 %**.
5. Guarda el conjunto.

Los colores identifican el alcance y pueden usarse para filtrar el listado. Si un conjunto ya se utilizó en evaluaciones, un docente puede tener que duplicarlo a otro ciclo para modificarlo. El administrador cuenta con permisos adicionales indicados por la aplicación.

### 4.8 Configurar modalidades y porcentajes

1. Abre **Modalidades y porcentajes**.
2. Selecciona una materia y el ciclo. Usa **General** para definir el esquema que se hereda en los ciclos sin uno propio.
3. Distribuye el porcentaje entre **Docente o jurado**, **Autoevaluación** y **Público**. La suma debe ser exactamente **100 %**.
4. Para cada modalidad distinta de cero, revisa o completa sus criterios.
5. Guarda el esquema y confirma el mensaje de éxito.

Si ya existen evaluaciones, la pantalla puede bloquear cambios para docentes. Considera que cambiar ponderaciones puede afectar el cálculo de notas finales según el aviso mostrado.

### 4.9 Invitar evaluadores y calificar proyectos

En **Invitar evaluadores**, selecciona un proyecto y agrega docentes o jurados disponibles. Revisa la lista de invitados y retira una invitación si se agregó por error.

Para calificar:

1. Abre **Proyectos asignados** o **Calificar**.
2. Selecciona un proyecto pendiente.
3. Ingresa una nota por criterio dentro del rango indicado y agrega comentarios si hace falta.
4. Guarda como borrador si aún no has terminado.
5. Selecciona **Enviar evaluación** cuando esté lista. Una evaluación enviada puede requerir que un usuario con permisos la reabra antes de corregirla.

Revisa el nombre del proyecto, la materia y el grupo antes de enviar. La pantalla muestra el estado de la evaluación.

### 4.10 Habilitar evaluación pública

1. Abre **Evaluación pública** y selecciona el proyecto.
2. Revisa el estado y habilita la evaluación cuando el proyecto esté preparado.
3. Comparte el enlace o código público del proyecto con quienes participarán.
4. Al terminar, cierra la evaluación pública.

Las respuestas públicas son anónimas. No compartas códigos de proyectos que todavía no deban recibir respuestas.

### 4.11 Revisar notas y publicar resultados

1. Abre **Notas finales** y selecciona un proyecto.
2. Revisa la fórmula, los porcentajes por modalidad, el estado y el desglose por alumno.
3. Completa las evaluaciones pendientes. La nota solo se considera definitiva cuando cumple el esquema configurado.
4. Cuando esté habilitado el botón **Publicar resultados**, selecciónalo.
5. Confirma que el estado indique **Resultados publicados**.

Si se publica antes de tiempo, la pantalla permite retirar la publicación según los permisos de la cuenta.

### 4.12 Consultar y descargar reportes

En **Reportes**, usa los filtros de materia, estado, ciclo y publicación. Selecciona una fila para abrir el detalle, que muestra evaluadores, estados y promedios por criterio. Usa **PDF** o **Excel** para descargar el listado filtrado o el detalle individual.

## 5. Guía para alumnos

### 5.1 Consultar el proyecto y sus integrantes

Abre **Mi proyecto** para revisar el proyecto asociado a tu grupo, la materia, la empresa si existe, el estado, los resultados y los integrantes. Si no aparece un proyecto, consulta con el docente para revisar tu matrícula, grupo y asignación.

### 5.2 Consultar criterios

Abre **Criterios del proyecto** para revisar los criterios aplicables a tu proyecto, sus ponderaciones y el alcance/ciclo que corresponden.

### 5.3 Evaluar a tus compañeros

1. Abre **Autoevaluación** y selecciona el proyecto si tienes más de uno.
2. En **A quién evaluar**, selecciona a un compañero.
3. Ingresa una nota para cada criterio dentro del rango de 0 a 10.
4. Selecciona **Guardar calificación** y repite con los demás integrantes.
5. Consulta el indicador de avance para verificar cuántos compañeros faltan.

No puedes seleccionarte ni calificarte a ti mismo. Las calificaciones son anónimas para los demás; tú puedes consultar las que registraste.

### 5.4 Consultar la nota

Abre **Mi nota**. Los resultados aparecerán cuando el docente los publique. Si el resultado está pendiente, aún falta completar alguna modalidad o publicar la nota.

## 6. Evaluación pública

La persona invitada abre el enlace público compartido por el docente o ingresa el código del proyecto en la página pública. Revisa el proyecto y completa las calificaciones requeridas. La respuesta se registra de forma anónima. Si la evaluación está cerrada o el código no es válido, solicita al docente que confirme el enlace y el estado de apertura.

## 7. Problemas frecuentes

| Situación | Qué revisar |
|---|---|
| No puedo iniciar sesión | Verifica correo, contraseña, estado de la cuenta y que estés usando la base de datos donde está registrado el usuario. |
| No aparece una materia o sección | Pide al administrador que revise la asignación de docente, ciclo, sección y subsección. |
| No puedo matricular o quitar a un alumno | Comprueba que no pertenezca a un grupo de esa subsección; quítalo del grupo primero. |
| No puedo guardar criterios o modalidades | Revisa que las ponderaciones sumen 100 %, que el ciclo esté disponible y si ya hay evaluaciones asociadas. |
| La nota sigue pendiente | Revisa las modalidades activas, evaluaciones faltantes y si la evaluación pública debe cerrarse. |
| No aparece mi nota | El docente debe publicar los resultados. |
| No llega el código de recuperación | Verifica que el correo pertenezca a una cuenta activa y autorizada; el administrador debe revisar SMTP. En demostración, consulta con el responsable si el código se muestra en la consola. |
| Los datos parecen haber desaparecido al cambiar de base | MySQL y PostgreSQL mantienen datos separados. Cambiar el motor activo no copia registros de una base a otra. |

## 8. Buenas prácticas

- Confirma ciclo, materia, sección y grupo antes de guardar registros o calificaciones.
- No compartas tu contraseña ni códigos de recuperación.
- Guarda borradores durante una evaluación y envíala solo cuando esté completa.
- No elimines registros relacionados con evaluaciones ya realizadas sin consultar al administrador.
- Cierra sesión al terminar en un equipo compartido.

## 9. Nota para el administrador técnico

Este documento es para usuarios finales. La instalación del backend/frontend, los contenedores, las credenciales, el cambio de motor de base de datos y SMTP deben seguir la guía técnica del proyecto y ser atendidos por una persona administradora. Los nombres exactos de botones pueden variar según el idioma seleccionado y el rol.
