import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'

export type Language = 'es' | 'en'
interface LanguageContextValue { language: Language; toggleLanguage: () => void; text: (spanish: string) => string }
const LanguageContext = createContext<LanguageContextValue | undefined>(undefined)
interface TraduccionGuardada { original: string; traducido: string }
const textosTraducidos = new WeakMap<Node, TraduccionGuardada>()
const atributosTraducidos = new WeakMap<Element, Map<string, TraduccionGuardada>>()

const translations: Record<string, string> = Object.fromEntries([
    ['INICIO','HOME'], ['GESTIÓN ACADÉMICA','ACADEMIC MANAGEMENT'], ['EVALUACIÓN','ASSESSMENT'], ['RESULTADOS','RESULTS'], ['MI PROYECTO','MY PROJECT'],
    ['Resumen general','Overview'], ['Usuarios','Users'], ['Materias','Subjects'], ['Secciones','Sections'], ['Matrículas','Enrollments'],
    ['Alumnos y grupos','Students and groups'], ['Empresas','Companies'], ['Proyectos','Projects'], ['Criterios','Criteria'],
    ['Modalidades y porcentajes','Assessment types and percentages'], ['Invitar evaluadores','Invite evaluators'], ['Evaluación pública','Public assessment'],
    ['Proyectos asignados','Assigned projects'], ['Calificar','Grade'], ['Resultados por evaluador','Results by evaluator'], ['Notas finales','Final grades'], ['Reportes','Reports'],
    ['Ciclos académicos','Academic terms'],
    ['Inicio','Home'], ['Mi proyecto','My project'], ['Criterios del proyecto','Project criteria'], ['Autoevaluación','Self-assessment'], ['Mi nota','My grade'],
    ['Administrador','Administrator'], ['Docente','Teacher'], ['Jurado','Jury'], ['Estudiante','Student'], ['Cerrar sesión','Sign out'],
    ['Cambiar a modo claro','Switch to light mode'], ['Cambiar a modo oscuro','Switch to dark mode'], ['Modo claro','Light mode'], ['Modo oscuro','Dark mode'],
    ['Sistema Web de Evaluación','Assessment Web System'], ['Sesión segura','Secure session'],
    ['MÓDULO DE EVALUACIÓN','ASSESSMENT MODULE'],
    ['Modalidades de evaluación','Assessment types'], ['CONFIGURACIÓN','SETTINGS'],
    ['MI RESULTADO','MY RESULT'], ['MI NOTA','MY GRADE'],
    ['FERIAS Y DEFENSAS','FAIRS AND PRESENTATIONS'],
    ['Califica tu aporte y el de tus compañeros. Nadie verá quién puso cada nota.','Rate your contribution and your teammates’ contributions. No one will see who submitted each grade.'],
    ['Elige quién evalúa a los estudiantes y cuánto vale cada nota. Se define por materia y por ciclo.','Choose who evaluates students and how much each grade is worth. Settings apply by subject and term.'],
    ['Asigna alumnos a las subsecciones de sus materias.','Assign students to subject subsections.'],
    ['Información de tu proyecto y de los integrantes de tu grupo.','Information about your project and group members.'],
    ['Con estos criterios se evalúa cada uno de tus proyectos activos.','These criteria are used to assess each of your active projects.'],
    ['Filtra los proyectos, consulta el detalle y descarga el reporte en PDF o Excel.','Filter projects, view details, and download reports in PDF or Excel.'],
    ['Desglose por alumno según las modalidades que definió el docente para la materia.','Student breakdown by the assessment types set by the teacher for the subject.'],
    ['Abre la evaluación de un proyecto para que el público la califique desde su celular, sin cuenta ni correo.','Open a project assessment for the public to rate from a phone, without an account or email.'],
    ['PANEL DE ADMINISTRACIÓN','ADMIN DASHBOARD'], ['PANEL DOCENTE','TEACHER DASHBOARD'], ['PANEL DE EVALUACIÓN','ASSESSMENT DASHBOARD'], ['VISTA ESTUDIANTE','STUDENT VIEW'],
    ['Gestiona usuarios, procesos académicos y resultados del sistema.','Manage users, academic processes, and system results.'],
    ['Organiza tus materias, grupos y evaluaciones académicas.','Organize your subjects, groups, and assessments.'],
    ['Revisa los proyectos asignados y registra tus calificaciones.','Review assigned projects and record your grades.'],
    ['Consulta tu proyecto, tus criterios y tus resultados publicados.','View your project, criteria, and published results.'],
    ['Accesos rápidos','Quick links'], ['Acciones frecuentes','Common actions'], ['Tu nota se muestra cuando el docente publique los resultados.','Your grade will appear when the teacher publishes results.'],
    ['Matricula de alumnos','Student enrollment'], ['Matriculados','Enrolled students'], ['Alumnos sin matricula','Unenrolled students'],
    ['Buscar por nombre o correo','Search by name or email'], ['Nombre, apellido o correo','Name, last name, or email'],
    ['Selecciona una opcion','Select an option'], ['Selecciona un proyecto','Select a project'], ['Selecciona una materia','Select a subject'],
    ['Matricular seleccionados','Enroll selected students'], ['Quitar matricula','Remove enrollment'],
    ['Alumnos asignados','Assigned students'], ['Alumnos disponibles','Available students'], ['Subsecciones creadas','Created subsections'],
    ['Agregar subseccion','Add subsection'], ['Materias y docentes','Subjects and teachers'],
    ['Guardar','Save'], ['Cancelar','Cancel'], ['Actualizar','Update'], ['Eliminar','Delete'], ['Editar','Edit'], ['Acciones','Actions'],
    ['Nombre','Name'], ['Apellido','Last name'], ['Correo','Email'], ['Descripcion','Description'], ['Ciclo','Term'],
    ['Seccion','Section'], ['Subseccion','Subsection'], ['Materia','Subject'], ['Grupo','Group'], ['Alumno','Student'],
    ['Docente','Teacher'], ['Empresa','Company'], ['Contacto','Contact'], ['Rol','Role'], ['Todos','All'], ['Todas','All'],
    ['Estado','Status'], ['Activo','Active'], ['Inactivo','Inactive'], ['Pendiente','Pending'], ['Definitiva','Final'],
    ['FILTROS','FILTERS'], ['LISTADO','LIST'], ['DETALLE','DETAIL'], ['PROYECTOS','PROJECTS'], ['INTEGRANTES','MEMBERS'],
    ['No hay proyectos para mostrar','There are no projects to display'], ['No hay proyectos con esos filtros','No projects match these filters'],
    ['No tienes evaluaciones pendientes','You have no pending assessments'], ['No tienes proyectos asignados','You have no assigned projects'],
    ['No tienes un proyecto asignado','You do not have an assigned project'], ['No tienes proyectos activos','You have no active projects'],
    ['Selecciona a un companero','Select a teammate'], ['No hay companeros que evaluar','There are no teammates to assess'],
    ['Responsabilidad','Responsibility'], ['Aporte tecnico','Technical contribution'], ['Trabajo en equipo','Teamwork'],
    ['Cumplimiento de plazos','Meeting deadlines'], ['Guardar calificacion','Save assessment'], ['Guardar borrador','Save draft'],
    ['Enviar calificacion','Submit assessment'], ['Reabrir evaluacion','Reopen assessment'], ['Comentarios de la evaluacion','Assessment comments'],
    ['Evaluadores invitados','Invited evaluators'], ['Disponibles','Available'], ['Resultado pendiente','Result pending'],
    ['Nota final','Final grade'], ['Promedio por criterio','Average by criterion'], ['Publicar resultados','Publish results'],
    ['Retirar publicacion','Unpublish results'], ['Estadisticas del reporte','Report statistics'], ['Agrupar por','Group by'],
    ['Calificaciones','Grades'], ['No hay datos para graficar','There is no data to chart'], ['Reintentar','Retry'],
    ['Codigo del proyecto','Project code'], ['Codigo de validacion','Validation code'], ['Evaluar proyecto','Assess project'],
    ['Evaluacion no disponible','Assessment unavailable'], ['Continuar','Continue'], ['Validar y acceder','Verify and continue'],
    ['Enviar evaluacion','Submit assessment'], ['Califica todos los criterios','Rate all criteria'], ['Enviando...','Submitting...'],
    ['Guardar codigo','Save code'], ['Copiar enlace','Copy link'], ['Enlace copiado.','Link copied.'], ['Abrir evaluacion','Open assessment'],
    ['Cerrar evaluacion','Close assessment'], ['Sin empresa','No company'], ['Sin descripcion','No description'],
    ['Publicados','Published'], ['Sin publicar','Unpublished'], ['Materia no especificada','Subject not specified'],
    ['Ciclo destino','Destination term'], ['Duplicar a otro ciclo','Duplicate to another term'],
    ['Ciclos academicos','Academic terms'], ['Agregar ciclo','Add term'], ['Desactivar','Deactivate'], ['Activar','Activate'],
    ['Administra docentes, jurado y alumnos.','Manage teachers, jury members, and students.'],
    ['Organiza las materias y su docente principal.','Manage subjects and their main teachers.'],
    ['Crea las secciones por ciclo, sus subsecciones (A, B, C) y asigna materias y docentes.','Create sections by term, their subsections (A, B, C), and assign subjects and teachers.'],
    ['Crea grupos y asigna integrantes disponibles.','Create groups and assign available members.'],
    ['Registra las empresas vinculadas a los proyectos.','Register companies linked to projects.'],
    ['Registra proyectos y consulta su informacion.','Register projects and view their information.'],
    ['Define ponderaciones por materia, seccion, grupo o proyecto, y por ciclo.','Set weights by subject, section, group, project, and term.'],
    ['Seccion (ej. SOFT41)','Section (e.g., SOFT41)'], ['Ciclo (ej. 02-2026)','Term (e.g., 02-2026)'],
    ['Ciclo (opcional; vacio = conjunto general)','Term (optional; blank = general set)'], ['Selecciona una opcion','Select an option'],
    ['Materia en subseccion','Subject in subsection'], ['Se aplica a','Applies to'], ['Cual','Which one'],
    ['Telefono (8 digitos)','Phone (8 digits)'], ['6620-5555 o 66205555','6620-5555 or 66205555'],
    ['Estado','Status'], ['Activo','Active'], ['Finalizado','Finished'], ['Cancelado','Canceled'],
    ['Contraseña (déjala vacía para no cambiarla)','Password (leave blank to keep the current one)'],
    ['Docente principal','Main teacher'], ['Ponderacion (%)','Weight (%)'], ['Descripcion','Description'],
    ['Ciclo destino disponible:','Available destination terms:'], ['Conjunto duplicado al ciclo','Set duplicated to term'],
    ['Ya puedes editarlo ahi.','You can edit it there now.'], ['Duplicar criterios a otro ciclo','Duplicate criteria to another term'],
    ['Selecciona un ciclo activo','Select an active term'], ['Duplicar','Duplicate'], ['Cancelar','Cancel'],
    ['En cada nivel se prefiere el conjunto del ciclo del proyecto y, si no existe, el general.','At each level, the project term set is preferred; if none exists, the general set is used.'],
    ['Un conjunto que ya se uso en evaluaciones no se puede cambiar: duplicalo a otro ciclo.','A set already used in assessments cannot be changed. Duplicate it to another term.'],
    ['Se usa el nivel mas especifico cuya suma sea 100 %: proyecto -> grupo -> materia en subseccion -> materia.','The most specific level totaling 100% is used: project -> group -> subject in subsection -> subject.'],
    ['Se usa el nivel más específico cuya suma sea 100 %: proyecto → grupo → materia en subsección → materia.','The most specific level totaling 100% is used: project → group → subject in subsection → subject.'],
    ['No hay asignaciones disponibles. Crealas en Secciones, con el icono de capas.','No assignments are available. Create them in Sections using the layers icon.'],
    ['Agrega el primero usando el formulario.','Add the first one using the form.'], ['No hay criterios para este alcance','There are no criteria for this scope.'],
    ['No hay usuarios que coincidan con estos filtros','No users match these filters.'], ['Filtrar por rol','Filter by role'],
    ['Todos los roles','All roles'], ['Nombre, apellido o correo','Name, last name, or email'],
    ['Materia · estándar','Subject · standard'], ['Sección / subsección','Section / subsection'],
    ['Selecciona un color para filtrar por alcance. La lista va de lo general a lo específico.','Select a color to filter by scope. The list goes from general to specific.'],
    ['La lista va de lo general a lo específico.','The list goes from general to specific.'],
    ['Gestionar proyectos','Manage projects'], ['Acciones frecuentes','Common actions'], ['Gestiona usuarios, procesos académicos y resultados del sistema.','Manage users, academic processes, and system results.'],
    ['Materias activas','Active subjects'], ['Grupos creados','Groups created'], ['Usuarios registrados','Registered users'], ['Proyectos registrados','Registered projects'],
    ['Enviadas','Submitted'], ['Sin iniciar','Not started'], ['Borrador','Draft'], ['Enviada','Submitted'],
    ['Evaluacion enviada correctamente.','Assessment submitted successfully.'], ['Evaluacion guardada.','Assessment saved.'],
    ['Calificacion enviada correctamente.','Assessment submitted successfully.'], ['Evaluacion reabierta. Puedes editarla y volver a enviarla.','Assessment reopened. You can edit and resubmit it.'],
    ['No se pudo guardar la evaluacion.','Could not save the assessment.'], ['No se pudo enviar la calificacion.','Could not submit the assessment.'],
    ['No se pudo guardar el borrador.','Could not save the draft.'], ['No se pudo reabrir la evaluacion.','Could not reopen the assessment.'],
    ['La informacion aparecera aqui.','Information will appear here.'], ['La información aparecerá aquí.','Information will appear here.'],
    ['No fue posible cargar los proyectos.','Could not load the projects.'], ['No fue posible cargar el detalle seleccionado.','Could not load the selected details.'],
    ['No fue posible cargar el reporte.','Could not load the report.'], ['No se pudo descargar el archivo.','Could not download the file.'],
    ['Selecciona un proyecto para ver el detalle.','Select a project to view details.'], ['Selecciona un proyecto','Select a project'],
    ['Selecciona uno','Select one'], ['Invitar evaluadores','Invite evaluators'], ['Resultados por evaluador','Results by evaluator'],
    ['Publicados','Published'], ['No','No'], ['Sí','Yes'], ['Si','Yes'], ['Todas','All'], ['Todos','All'],
    ['PDF','PDF'], ['Excel','Excel'], ['Promedio por criterio','Average by criterion'], ['Evaluaciones','Assessments'],
    ['Cargando...','Loading...'], ['Cargando…','Loading…'], ['Aun sin publicar','Not published yet'],
    ['Resultados publicados. Los alumnos ya pueden ver su nota.','Results published. Students can now view their grades.'],
    ['Resultados retirados.','Results unpublished.'], ['Publicar resultados','Publish results'], ['Retirar publicación','Unpublish results'],
    ['Formula','Formula'], ['Definitiva','Final'], ['Pendiente','Pending'], ['Invitados','Invited'],
    ['Estadisticas del reporte','Report statistics'], ['Resumen visual de los proyectos con los filtros que aplicaste.','Visual summary of projects matching your filters.'],
    ['Agrupar por','Group by'], ['Proyecto','Project'], ['Grupo','Group'], ['Votos (evaluaciones enviadas)','Votes (submitted assessments)'],
    ['Ningun proyecto cumple los filtros aplicados. Cambia los filtros del reporte para ver las graficas.','No projects match the selected filters. Change the report filters to view charts.'],
    ['No se pudieron cargar las graficas','Could not load charts'], ['Reintentar','Retry'],
    ['Codigo de validacion guardado. El administrador y el docente responsable veran el mismo codigo.','Validation code saved. The administrator and responsible teacher will see the same code.'],
    ['Codigo del proyecto','Project code'], ['Codigo de validacion','Validation code'],
    ['Define o cambia el codigo de validacion (4-20 letras, numeros o guiones)','Set or change the validation code (4–20 letters, numbers, or hyphens)'],
    ['Ingresa el codigo del proyecto para continuar.','Enter the project code to continue.'],
    ['Ingresa el codigo de validacion que te proporciono el administrador o el docente responsable.','Enter the validation code provided by the administrator or responsible teacher.'],
    ['Gracias por tu evaluacion','Thank you for your assessment.'], ['Tu respuesta es anonima y ya quedo registrada.','Your response is anonymous and has been recorded.'],
    ['No necesitas cuenta ni correo.','No account or email is required.'], ['Califica cada criterio del','Rate each criterion from'],
    ['Muy bajo','Very low'], ['Excelente','Excellent'], ['Evaluacion no disponible','Assessment unavailable'],
    ['Resultados','Results'], ['Grupo','Group'], ['Criterio','Criterion'], ['Criterios','Criteria'],
    ['Completo','Complete'], ['En uso','In use'], ['General (ciclos sin esquema propio)','General (terms without a custom setup)'],
    ['Este ciclo todavía no tiene esquema propio: se muestra el esquema general. Al guardar se creará uno propio para','This term does not have its own setup yet; the general setup is shown. Saving will create one for'],
    ['Selecciona una materia','Select a subject'], ['MATERIA','SUBJECT'], ['CICLO','TERM'],
    ['Nuevo ciclo, ej. 01-2027','New term, e.g. 01-2027'], ['Agregar ciclo','Add term'],
    ['Criterio','Criterion'], ['Descripción (opcional)','Description (optional)'], ['Suma:','Total:'],
    ['— correcto','— correct'], ['— debe sumar exactamente 100%','— must add up to exactly 100%'],
    ['Solo docente o jurado','Teacher or jury only'], ['Docente + autoevaluación','Teacher + self-assessment'],
    ['Docente + público','Teacher + public'], ['Las tres modalidades','All three methods'],
    ['Cada alumno a sí mismo y a su grupo, anónimo','Each student rates their teammates anonymously'],
    ['Visitantes de ferias o defensas, anónimo','Fair or presentation visitors, anonymously'],
    ['Evalúan con los criterios de la materia','They use the subject criteria'],
    ['Este conjunto ya se usó en evaluaciones: duplícalo a otro ciclo para cambiarlo','This set has been used in assessments. Duplicate it to another term to make changes.'],
    ['Elige quién evalúa a los estudiantes y cuánto vale cada nota. Se define por materia y por ciclo.','Choose who assesses students and the weight of each grade. Settings apply by subject and term.'],
    ['Definidos para tu proyecto','Defined for your project'], ['Definidos para tu grupo','Defined for your group'],
    ['Definidos para tu sección','Defined for your section'], ['Definidos para la materia','Defined for the subject'],
    ['Tu docente aún no definió los criterios de este proyecto (deben sumar 100%).','Your teacher has not defined this project’s criteria yet (they must add up to 100%).'],
    ['No tienes proyectos activos','You have no active projects'], ['No tienes proyectos asignados','You have no assigned projects'],
    ['No tienes un proyecto asignado','You do not have an assigned project'], ['Aún sin publicar','Not published yet'],
    ['REGISTRO','RECORD'], ['Nuevo registro','New record'], ['Guardar cambios','Save changes'], ['Buscar','Search'], ['Filtrar','Filter'],
    [' proyectos pendientes',' pending projects'], [' registros',' records'], [' evaluaciones recibidas.',' assessments received.'],
    ['Matriculados (','Enrolled students ('], ['Alumnos sin matrícula (','Unenrolled students ('],
    ['alumno(s) matriculado(s) correctamente','student(s) enrolled successfully'],
    ['ADMIN','Administrator'], ['DOCENTE','Teacher'], ['JURADO','Jury'], ['ALUMNO','Student'],
    ['ACTIVO','ACTIVE'], ['FINALIZADO','FINISHED'], ['CANCELADO','CANCELED'], ['SIN_INICIAR','NOT STARTED'], ['BORRADOR','DRAFT'], ['ENVIADA','SUBMITTED'],
    ['Ver todos (','View all ('], ['Mostrar solo los primeros ','Show only the first '],
    ['Notas de','Grades for'], ['proyecto','project'], ['proyectos','projects'], ['Definitivos:','Final:'], ['Provisionales:','Provisional:'],
    ['Promedio:','Average:'], ['Evaluaciones enviadas:','Assessments submitted:'], ['Nota provisional','Provisional grade'], ['Nota definitiva','Final grade'],
    ['Definitiva (todos enviaron)','Final (everyone submitted)'], ['Provisional (faltan evaluaciones)','Provisional (assessments missing)'],
    ['Distribución de notas finales','Final grade distribution'],
    ['Cuántos proyectos hay en cada rango de nota. Solo cuentan proyectos con al menos una evaluación enviada.','Number of projects in each grade range. Only projects with at least one submitted assessment are included.'],
    ['Barras con la cantidad de proyectos por rango de nota final, de 0 a 10.','Bars showing project counts in each final grade range, from 0 to 10.'],
    ['Rango de nota final','Final grade range'], ['Proyectos','Projects'], ['Definitivos','Final'], ['Provisionales','Provisional'],
    ['Promedio de las evaluaciones enviadas, de 0 a 10. Primero aparecen las notas más altas.','Average of submitted assessments, from 0 to 10. Highest grades appear first.'],
    ['Promedio de nota (0 a 10)','Average grade (0 to 10)'], ['Evaluadores invitados','Invited evaluators'],
    ['Aún no hay evaluaciones enviadas','No assessments have been submitted yet'],
    ['Las calificaciones aparecerán aquí cuando al menos un evaluador envíe su evaluación.','Grades will appear here when at least one evaluator submits an assessment.'],
    ['No hay promedios para mostrar','There are no averages to display'],
    ['Los proyectos de este reporte no tienen un grupo asignado o aún no tienen evaluaciones enviadas.','Projects in this report have no assigned group or no submitted assessments yet.'],
    ['Mostrar todos','Show all'], ['Mostrar menos','Show less'], ['Porcentaje','Percentage'], ['Cantidad','Count'],
    ['CRITERIOS','CRITERIA'], ['FORMA DE EVALUAR','ASSESSMENT METHOD'], ['¿Cómo se calculará la nota?','How will the grade be calculated?'],
    ['Los porcentajes suman','The percentages add up to'], ['Los porcentajes deben sumar exactamente 100%. Llevas','The percentages must add up to exactly 100%. Current total:'],
    ['Los criterios de docente o jurado se definen en','Teacher or jury criteria are defined in'], ['también por ciclo, y se heredan de materia, sección, grupo o proyecto.','by term as well, and are inherited from the subject, section, group, or project.'],
    ['Para definir criterios de autoevaluación o de evaluación pública, asigna un porcentaje mayor a 0 a esa modalidad.','To define self-assessment or public assessment criteria, assign that method a percentage greater than 0.'],
    ['Este esquema del ciclo','This term setup'], ['ya tiene evaluaciones. Como administrador puedes modificarlo.','already has assessments. As an administrator, you can change it.'],
    ['El esquema general ya tiene evaluaciones. Como administrador puedes modificarlo.','The general setup already has assessments. As an administrator, you can change it.'],
    ['Las respuestas anteriores conservan sus criterios; las nuevas evaluaciones usarán los vigentes.','Previous responses keep their criteria; new assessments will use the current criteria.'],
    ['Si cambias porcentajes, las notas finales se recalcularán con esos porcentajes.','If you change the percentages, final grades will be recalculated using them.'],
    ['Ya hay evaluaciones con este esquema.','There are already assessments using this setup.'],
    ['No se puede cambiar; elige otro ciclo para guardar porcentajes o criterios diferentes.','It cannot be changed; choose another term to save different percentages or criteria.'],
    ['Esquema guardado para el ciclo','Setup saved for term'], ['Esquema general guardado. Se aplica a los ciclos que no tengan uno propio.','General setup saved. It applies to terms without their own setup.'],
    ['Guardar esquema general','Save general setup'], ['Guardar esquema del ciclo','Save term setup'],
    ['Criterios de autoevaluación','Self-assessment criteria'], ['Criterios de evaluación pública','Public assessment criteria'],
    ['Otra letra (por ejemplo D)','Another letter (for example D)'], ['Agregar','Add'],
    ['No se pudo cargar la información.','Could not load the information.'], ['No fue posible cargar las subsecciones.','Could not load the subsections.'],
    ['No fue posible cargar las materias de la sección.','Could not load subjects for the section.'],
    ['No se pudo crear la subsección.','Could not create the subsection.'], ['No se pudo eliminar la subsección.','Could not delete the subsection.'],
    ['No se pudo asignar la materia.','Could not assign the subject.'], ['No se pudo quitar la asignación.','Could not remove the assignment.'],
    ['No se pudieron cargar las secciones','Could not load sections'], ['No se pudieron cargar las subsecciones','Could not load subsections'],
    ['Matrícula eliminada','Enrollment removed'],
    ['Este ciclo todavía no tiene esquema propio: se muestra el esquema general. Al guardar se creará uno propio para ','This term does not have its own setup yet; the general setup is shown. Saving will create one for '],
    ['Este esquema del ciclo ','This term setup '], [' ya tiene evaluaciones. Como administrador puedes modificarlo. ',' already has assessments. As an administrator, you can change it. '],
    ['El esquema general ya tiene evaluaciones. Como administrador puedes modificarlo. ','The general setup already has assessments. As an administrator, you can change it. '],
    ['Las respuestas anteriores conservan sus criterios; las nuevas evaluaciones usarán los vigentes. Si cambias porcentajes, las notas finales se recalcularán con esos porcentajes.','Previous responses keep their criteria; new assessments use the current criteria. If you change the percentages, final grades are recalculated.'],
    ['Ya hay evaluaciones con este esquema. ','There are already assessments using this setup. '],
    ['Ciclo ','Term '], ['Guardar esquema del ciclo ','Save setup for term '],
    ['Barras horizontales con el promedio de nota por grupo, de 0 a 10.','Horizontal bars showing the average grade by group, from 0 to 10.'],
    ['Barras horizontales con el promedio de nota por proyecto, de 0 a 10.','Horizontal bars showing the average grade by project, from 0 to 10.'],
    ['Promedio por proyecto','Average by project'], ['Promedio por grupo','Average by group'],
    ['Votos (evaluaciones enviadas)','Votes (submitted assessments)'], ['Evaluadores invitados','Invited evaluators'],
    ['Proyectos pendientes','Pending projects'], ['Evaluaciones pendientes','Pending assessments'],
    ['Borrador guardado.','Draft saved.'], ['Calificación enviada correctamente.','Assessment submitted successfully.'],
    ['Evaluación reabierta. Puedes editarla y volver a enviarla.','Assessment reopened. You can edit and resubmit it.'],
    ['Evaluador invitado correctamente.','Evaluator invited successfully.'], ['Invitación eliminada.','Invitation removed.'],
    ['No se pudo enviar la invitación.','Could not send the invitation.'], ['No se pudo quitar la invitación.','Could not remove the invitation.'],
    ['No hay más docentes o jurado para invitar.','There are no more teachers or jury members to invite.'],
    ['Selecciona un proyecto','Select a project'], ['Selecciona uno','Select one'], ['La información aparecerá aquí.','Information will appear here.'],
    ['Evaluación pública abierta.','Public assessment opened.'], ['Evaluación pública cerrada.','Public assessment closed.'],
    ['Para abrirla, la materia del proyecto debe tener un porcentaje de ','To open it, the project subject must have a public assessment weight above '],
    [' mayor a 0 en Modalidades y porcentajes, con sus criterios sumando 100 %.',' in Assessment types and percentages, with criteria totaling 100%.'],
    ['El código está habilitado.','The code is enabled.'], ['Evaluación abierta','Assessment open'], ['Evaluación cerrada','Assessment closed'],
    ['CODIGO DEL PROYECTO (IDENTIFICADOR DEL ENLACE)','PROJECT CODE (LINK IDENTIFIER)'],
    ['CODIGO DE VALIDACION (COMPARTIR POR SEPARADO)','VALIDATION CODE (SHARE SEPARATELY)'],
    ['CODIGO DEL PROYECTO (IDENTIFICADOR DEL ENLACE)','PROJECT CODE (LINK IDENTIFIER)'],
    ['Define o cambia el codigo de validacion (4-20 letras, numeros o guiones)','Set or change the validation code (4–20 letters, numbers, or hyphens)'],
    ['Copia el enlace manualmente.','Copy the link manually.'], ['Cambiar codigo de proyecto','Change project code'],
    ['Ingresar otro codigo de proyecto','Enter another project code'], ['Evaluacion no disponible','Assessment unavailable'],
    ['Entra al menu para gestionar la informacion.','Use the menu to manage information.'],
    ['No fue posible cargar la autoevaluación.','Could not load the self-assessment.'], ['No fue posible cargar tu proyecto.','Could not load your project.'],
    ['No fue posible cargar los criterios.','Could not load the criteria.'], ['No fue posible cargar tu nota.','Could not load your grade.'],
    ['No fue posible cargar las notas.','Could not load the grades.'], ['No se pudo cambiar la publicación.','Could not update publication status.'],
    ['No se pudo guardar el esquema.','Could not save the setup.'], ['No se pudieron cargar las materias.','Could not load subjects.'],
    ['No se pudo cargar el esquema de la materia.','Could not load the subject setup.'], ['No se pudo cargar el estado.','Could not load the status.'],
    ['No se pudo cambiar el estado.','Could not update the status.'], ['No se pudo guardar el código.','Could not save the code.'],
    ['No se pudo validar el código.','Could not validate the code.'], ['No se pudo enviar la evaluación.','Could not submit the assessment.'],
    ['No fue posible consultar el estado.','Could not retrieve the status.'], ['No fue posible cargar el reporte.','Could not load the report.'],
    ['No fue posible cargar los proyectos.','Could not load the projects.'], ['No se pudo descargar el archivo.','Could not download the file.'],
    ['No se pudo guardar el registro.','Could not save the record.'], ['No se pudo eliminar el registro.','Could not delete the record.'],
    ['No fue posible cargar la información.','Could not load the information.'], ['No fue posible cargar las subsecciones.','Could not load subsections.'],
    ['No se pudo crear la subsección.','Could not create the subsection.'], ['No se pudo eliminar la subsección.','Could not delete the subsection.'],
    ['No se pudo asignar la materia.','Could not assign the subject.'], ['No se pudo quitar la asignación.','Could not remove the assignment.'],
    ['Matrícula eliminada','Enrollment removed'], ['No se pudieron cargar las secciones','Could not load sections'],
    ['No hay alumnos sin matrícula que coincidan.','No unenrolled students match your search.'],
    ['Aún no hay alumnos matriculados en esta subsección.','There are no students enrolled in this subsection yet.'],
    ['Esta sección no tiene subsecciones creadas.','This section has no subsections yet.'],
    ['No hay asignaciones disponibles. Créelas en Secciones, con el icono de capas.','There are no assignments available. Create them in Sections using the layers icon.'],
    ['X evaluar','X to assess'], ['enviadas','submitted'], ['No hay calificaciones','No grades'],
    ['Con nota definitiva','With final grade'], ['Con nota provisional','With provisional grade'],
    ['Sin evaluaciones enviadas','No assessments submitted'], ['Proyectos en el reporte','Projects in report'],
    ['elegir un proyecto','select a project'], ['materia seleccionada','selected subject'],
    ['cuando menos un evaluador envíe su evaluación.','when at least one evaluator submits an assessment.'],
    ['No se ha enviado ninguna evaluación.','No assessments have been submitted.'], ['Sin resultados','No results'],
    ['Detalle del proyecto','Project details'], ['Notas finales','Final grades'], ['Integrantes del grupo','Group members'],
    ['Resultados','Results'], ['Descripción','Description'], ['Ponderación','Weight'], ['Criterio','Criterion'],
    ['Alumno','Student'], ['Evaluación','Assessment'], ['Estado','Status'], ['Nota final','Final grade'],
    ['Publicación','Publication'], ['Todos','All'], ['Todas','All'], ['Activo','Active'], ['Finalizado','Finished'], ['Cancelado','Canceled'],
    ['General (ciclos sin esquema propio)','General (terms without their own setup)'],
    ['Criterios de autoevaluación','Self-assessment criteria'], ['Criterios de evaluación pública','Public assessment criteria'],
    ['Criterios de evaluación','Assessment criteria'], ['¿Cómo se calculará la nota?','How will the grade be calculated?'],
    ['Solo docente o jurado','Teacher or jury only'], ['Docente + autoevaluación','Teacher + self-assessment'],
    ['Docente + público','Teacher + public'], ['Las tres modalidades','All three methods'],
    ['Evalúan con los criterios de la materia','They use the subject criteria'],
    ['Cada alumno a sí mismo y a su grupo, anónimo','Students assess their teammates anonymously'],
    ['Visitantes de ferias o defensas, anónimo','Fair and presentation visitors, anonymously'],
    ['Los porcentajes suman','The percentages add up to'], ['Los porcentajes deben sumar exactamente 100%. Llevas','Percentages must add up to exactly 100%. Current total:'],
    ['No tienes compañeros para evaluar en este grupo. No puedes evaluarte a ti mismo.','There are no teammates to assess in this group. You cannot assess yourself.'],
    ['Con estos criterios se evalúa cada uno de tus proyectos activos.','These criteria are used to assess each of your active projects.'],
    ['Tu nota se muestra cuando el docente publique los resultados.','Your grade will appear when your teacher publishes the results.'],
    ['Regional Santa Ana · Ciclo 02–2026','Santa Ana Regional · Term 02–2026'],
    ['Ciclo II - 2026','Term II - 2026'], ['Ciclo lectivo actual','Current academic term'],
    ['ESTADO','STATUS'], ['Estado','Status'], ['Gestión del sistema','System management'],
    ['Todo listo para trabajar','Everything is ready'], ['Usa el menú para gestionar la información.','Use the menu to manage information.'],
    ['Seleccione…','Select…'], ['Seleccione...','Select...'], ['Selecciona…','Select…'], ['Selecciona...','Select...'],
    ['Selecciona una opción','Select an option'], ['Selecciona un ciclo activo','Select an active term'],
])

function getInitialLanguage(): Language {
    try { return localStorage.getItem('language') === 'en' ? 'en' : 'es' } catch { return 'es' }
}

export function LanguageProvider({ children }: { children: ReactNode }) {
    const [language, setLanguage] = useState<Language>(getInitialLanguage)
    useEffect(() => {
        document.documentElement.lang = language
        try { localStorage.setItem('language', language) } catch { /* ignore */ }
    }, [language])
    useEffect(() => {
        const key = (value: string) => value.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase()
        const forward = new Map(Object.entries(translations).map(([es, en]) => [key(es), en]))
        const reverse = new Map(Object.entries(translations).map(([es, en]) => [key(en), es]))
        const localize = (value: string, previous?: TraduccionGuardada) => {
            const original = previous && value === previous.traducido ? previous.original : value
            const trimmed = original.trim()
            const translated = language === 'en' ? forward.get(key(trimmed)) : reverse.get(key(trimmed))
            if (!translated) return { original, traducido: original }
            const leading = original.slice(0, original.length - original.trimStart().length)
            const trailing = original.slice(original.trimEnd().length)
            return { original, traducido: `${leading}${translated}${trailing}` }
        }
        const walk = (root: ParentNode) => {
            const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT)
            let node: Node | null
            while ((node = walker.nextNode())) {
                const value = node.nodeValue ?? ''
                const state = localize(value, textosTraducidos.get(node))
                textosTraducidos.set(node, state)
                if (value !== state.traducido) node.nodeValue = state.traducido
            }
            if (root instanceof Element) {
                for (const element of [root, ...root.querySelectorAll('*')]) {
                    for (const attr of ['placeholder', 'title', 'aria-label', 'alt']) {
                        const value = element.getAttribute(attr)
                        if (value !== null) {
                            let saved = atributosTraducidos.get(element)
                            if (!saved) { saved = new Map(); atributosTraducidos.set(element, saved) }
                            const state = localize(value, saved.get(attr))
                            saved.set(attr, state)
                            if (value !== state.traducido) element.setAttribute(attr, state.traducido)
                        }
                    }
                }
            }
        }
        const root = document.getElementById('root')
        if (!root) return
        walk(root)
        const observer = new MutationObserver((records) => {
            observer.disconnect()
            for (const record of records) {
                if (record.type === 'characterData' && record.target.parentNode) walk(record.target.parentNode)
                else if (record.type === 'attributes' && record.target instanceof Element) walk(record.target)
                else record.addedNodes.forEach((node) => { if (node instanceof Element || node instanceof DocumentFragment) walk(node) })
            }
            observer.observe(root, { subtree: true, childList: true, characterData: true, attributes: true, attributeFilter: ['placeholder', 'title', 'aria-label', 'alt'] })
        })
        observer.observe(root, { subtree: true, childList: true, characterData: true, attributes: true, attributeFilter: ['placeholder', 'title', 'aria-label', 'alt'] })
        return () => observer.disconnect()
    }, [language])
    const toggleLanguage = () => setLanguage(current => current === 'es' ? 'en' : 'es')
    const text = (spanish: string) => language === 'es' ? spanish : translations[spanish] ?? [...Object.entries(translations)].find(([key]) => key.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase() === spanish.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase())?.[1] ?? spanish
    return <LanguageContext.Provider value={{ language, toggleLanguage, text }}>{children}</LanguageContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useLanguage() {
    const context = useContext(LanguageContext)
    if (!context) throw new Error('useLanguage debe usarse dentro de LanguageProvider')
    return context
}
