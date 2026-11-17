package com.example.Sistema_de_Evaluacion_de_Proyectos.config;

import com.example.Sistema_de_Evaluacion_de_Proyectos.model.*;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.CriterioEsquema.Modalidad;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Inicializador único:
 *  1) Siempre crea los roles y los 4 usuarios básicos (admin@, docente@, jurado@, alumno@).
 *  2) Si app.seed.demo es true (por defecto), carga además el conjunto de datos de prueba
 *     para la defensa. Es idempotente: si ya existe fernando.castaneda25@itca.edu.sv, no repite nada.
 *     Para desactivar la demo: app.seed.demo=false en application.properties.
 *
 * Criterios y esquemas por ciclo:
 *  - Los datos del ciclo vigente (02-2026) se guardan como conjunto GENERAL (ciclo null): se aplican a
 *    cualquier ciclo que no tenga uno propio.
 *  - Desarrollo Web trae además un conjunto y un esquema propios del ciclo siguiente (01-2027), todavía
 *    sin evaluaciones, para probar la edición y la duplicación por ciclo.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final BigDecimal CIEN = new BigDecimal("100");

    /** Ciclo de las secciones de prueba (formato NN-AAAA). */
    private static final String CICLO = "02-2026";
    /** Ciclo siguiente: solo tiene criterios y esquema, sin secciones ni evaluaciones. */
    private static final String CICLO_SIGUIENTE = "01-2027";

    private final RolRepository roles;
    private final UsuarioRepository usuarios;
    private final MateriaRepository materias;
    private final SeccionRepository secciones;
    private final SubseccionRepository subsecciones;
    private final MatriculaRepository matriculas;
    private final AsignacionRepository asignaciones;
    private final GrupoRepository grupos;
    private final GrupoAlumnoRepository miembros;
    private final EmpresaRepository empresas;
    private final ProyectoRepository proyectos;
    private final CriterioRepository criterios;
    private final EsquemaEvaluacionRepository esquemas;
    private final CriterioEsquemaRepository criteriosEsquema;
    private final EvaluadorInvitadoRepository invitaciones;
    private final EvaluacionRepository evaluaciones;
    private final EvaluacionDetalleRepository detalles;
    private final EvaluacionAlumnoRepository evaluacionesAlumno;
    private final EvaluacionAlumnoDetalleRepository detallesAlumno;
    private final EvaluacionPublicaRepository evaluacionesPublicas;
    private final EvaluacionPublicaDetalleRepository detallesPublica;
    private final PasswordEncoder encoder;

    @Value("${app.seed.demo:true}")
    private boolean cargarDemo;

    @Override
    @Transactional
    public void run(String... args) {
        crearBase();
        if (cargarDemo) {
            cargarDemo();
        } else {
            log.info("app.seed.demo=false: se omiten los datos de demostración.");
        }
        corregirApellidoFernando();
    }

    // ================= DATOS BASE (los de siempre) =================
    private void crearBase() {
        for (String n : new String[]{"alumno", "jurado", "docente", "admin"}) {
            if (roles.findByNombre(n).isEmpty()) {
                Rol r = new Rol();
                r.setNombre(n);
                roles.save(r);
            }
        }
        crearUsuarioBase("Admin", "admin@itca.edu.sv", "Admin123", "admin");
        crearUsuarioBase("Docente", "docente@itca.edu.sv", "Docente123", "docente");
        crearUsuarioBase("Jurado", "jurado@itca.edu.sv", "Jurado123", "jurado");
        crearUsuarioBase("Alumno", "alumno@itca.edu.sv", "Alumno123", "alumno");
    }

    private void crearUsuarioBase(String nombre, String correo, String pass, String rol) {
        if (usuarios.findByCorreo(correo).isPresent()) return;
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setApellido("Prueba");
        u.setCorreo(correo);
        u.setPassword(encoder.encode(pass));
        u.setRol(roles.findByNombre(rol).orElseThrow());
        usuarios.save(u);
    }

    private void corregirApellidoFernando() {
        usuarios.findByCorreo("fernando.castaneda25@itca.edu.sv").ifPresent(f -> {
            if ("Casta\u00f1eda".equals(f.getApellido())) {
                f.setApellido("Castaneda");
                usuarios.save(f);
            }
        });
    }

    // ================= DATOS DE DEMOSTRACIÓN =================
    private void cargarDemo() {
        if (usuarios.findByCorreo("fernando.castaneda25@itca.edu.sv").isPresent()) {
            log.info("Datos de prueba ya existentes: no se vuelven a crear.");
            return;
        }

        // ================= USUARIOS =================
        Map<String, Usuario> u = new HashMap<>();
        u.put("ricardo", usuario("Ricardo", "Alas", "ricardo.alas@itca.edu.sv", "Admin1234", "admin", true));
        u.put("carlos", usuario("Carlos", "Mejía", "carlos.mejia@itca.edu.sv", "Docente123", "docente", true));
        u.put("ana", usuario("Ana", "Ramírez", "ana.ramirez@itca.edu.sv", "Docente123", "docente", true));
        u.put("mauricio", usuario("Mauricio", "Alfaro", "mauricio.alfaro@itca.edu.sv", "Jurado123", "jurado", true));
        u.put("silvia", usuario("Silvia", "Cañas", "silvia.canas@itca.edu.sv", "Jurado123", "jurado", true));
        u.put("jorge", usuario("Jorge", "Portillo", "jorge.portillo@itca.edu.sv", "Jurado123", "jurado", true));
        u.put("kevin", usuario("Kevin", "Hernández", "kevin.hernandez@itca.edu.sv", "Alumno123", "alumno", true));
        u.put("daniela", usuario("Daniela", "Martínez", "daniela.martinez@itca.edu.sv", "Alumno123", "alumno", true));
        u.put("jose", usuario("José", "Rivas", "jose.rivas@itca.edu.sv", "Alumno123", "alumno", true));
        u.put("maria", usuario("María", "Flores", "maria.flores@itca.edu.sv", "Alumno123", "alumno", true));
        u.put("luis", usuario("Luis", "Guzmán", "luis.guzman@itca.edu.sv", "Alumno123", "alumno", true));
        u.put("fatima", usuario("Fátima", "Cruz", "fatima.cruz@itca.edu.sv", "Alumno123", "alumno", true));
        u.put("eduardo", usuario("Eduardo", "Pineda", "eduardo.pineda@itca.edu.sv", "Alumno123", "alumno", true));
        u.put("gabriela", usuario("Gabriela", "Ortiz", "gabriela.ortiz@itca.edu.sv", "Alumno123", "alumno", true));
        u.put("walter", usuario("Walter", "Calderón", "walter.calderon@itca.edu.sv", "Alumno123", "alumno", true));
        u.put("sofia", usuario("Sofía", "Menjívar", "sofia.menjivar@itca.edu.sv", "Alumno123", "alumno", true));
        u.put("josue", usuario("Josué", "Barrera", "josue.barrera@itca.edu.sv", "Alumno123", "alumno", true));
        u.put("andrea", usuario("Andrea", "Rosales", "andrea.rosales@itca.edu.sv", "Alumno123", "alumno", true));
        u.put("karla", usuario("Karla", "Navarro", "karla.navarro@itca.edu.sv", "Alumno123", "alumno", true));   // sin grupo
        u.put("oscar", usuario("Oscar", "Villalta", "oscar.villalta@itca.edu.sv", "Alumno123", "alumno", false)); // inactivo
        u.put("fernando", usuario("Fernando", "Castaneda", "fernando.castaneda25@itca.edu.sv", "Admin123", "admin", true));
        u.put("dennis", usuario("Dennis", "Serrano", "dennis.serrano25@itca.edu.sv", "Alumno123", "alumno", true));
        u.put("yesica", usuario("Yesica", "Anaya", "yesica.anaya25@itca.edu.sv", "Docente123", "docente", true));
        u.put("fatimaM", usuario("Fátima", "Miranda", "fatima.miranda25@itca.edu.sv", "Jurado123", "jurado", true));

        // ================= MATERIAS, SECCIONES Y GRUPOS =================
        Materia web = materia("Desarrollo Web",
                "Diseño y construcción de aplicaciones web con HTML, CSS, JavaScript, React y APIs REST.", u.get("carlos"));
        Materia bd = materia("Bases de Datos",
                "Modelado relacional, normalización, SQL e integridad de datos.", u.get("ana"));

        // Sección SOFT41 con subsecciones A y B. Cada alumno se matricula en UNA sola subsección.
        Seccion soft41 = seccion("SOFT41", CICLO);
        Subseccion soft41a = subseccion("A", soft41);
        Subseccion soft41b = subseccion("B", soft41);

        for (String k : new String[]{"kevin", "daniela", "jose", "maria", "luis", "walter", "sofia", "karla"}) {
            matricular(soft41a, u.get(k));   // karla: matriculada pero SIN grupo
        }
        for (String k : new String[]{"fatima", "eduardo", "gabriela", "josue", "andrea",
                "fernando", "dennis", "yesica", "fatimaM"}) {
            matricular(soft41b, u.get(k));
        }
        // Oscar (inactivo) no se matricula.

        // Asignaciones: materia de una subsección con SU docente
        Asignacion aWeb = asignacion(soft41a, web, u.get("carlos"));
        Asignacion aBd = asignacion(soft41a, bd, u.get("ana"));
        Asignacion bWeb = asignacion(soft41b, web, u.get("carlos"));
        Asignacion bBd = asignacion(soft41b, bd, u.get("ana"));

        // Grupos por asignación (un alumno solo puede estar en un grupo por asignación)
        Grupo alfa = equipo("Equipo Alfa", aWeb, u.get("kevin"), u.get("daniela"), u.get("jose"));
        Grupo beta = equipo("Equipo Beta", aWeb, u.get("maria"), u.get("luis"));
        Grupo alfaBd = equipo("Equipo Alfa", aBd, u.get("kevin"), u.get("daniela"), u.get("jose"));
        Grupo delta = equipo("Equipo Delta", aBd, u.get("walter"), u.get("sofia"));
        Grupo gamma = equipo("Equipo Gamma", bWeb, u.get("fatima"), u.get("eduardo"), u.get("gabriela"));
        Grupo omega = equipo("Equipo Omega", bWeb, u.get("fernando"), u.get("dennis"), u.get("yesica"), u.get("fatimaM"));
        Grupo epsilon = equipo("Equipo Épsilon", bBd, u.get("josue"), u.get("andrea"));

        // ================= EMPRESAS =================
        Empresa ferreteria = empresa("Ferretería El Tornillo Feliz", "Mario Alvarenga", "contacto@tornillofeliz.example", "2222-3344");
        Empresa clinica = empresa("Clínica Dental Sonrisa Plena", "Patricia Lemus", "citas@sonrisaplena.example", "2271-5500");
        Empresa distribuidora = empresa("Distribuidora La Cosecha", "Héctor Amaya", "pedidos@lacosecha.example", "2250-1188");
        Empresa boutique = empresa("Boutique Marisol", "Marisol Quintanilla", "ventas@boutiquemarisol.example", "2235-9076");

        // ================= PROYECTOS =================
        // (título, descripción, grupo, empresa, estado, código público, evaluación pública abierta, resultados publicados)
        Proyecto p1 = proyecto("Sistema web de inventario para ferretería",
                "Control de existencias, entradas, salidas y alertas de stock mínimo.",
                alfa, ferreteria, "FINALIZADO", "H3R8T2WP", false, true);
        Proyecto p2 = proyecto("Diseño de base de datos para gestión de citas de clínica dental",
                "Modelo relacional de pacientes, doctores, citas y tratamientos, con consultas de reporte.",
                delta, clinica, "ACTIVO", null, false, false);
        Proyecto p3 = proyecto("Base de datos para control de pedidos de distribuidora",
                "Clientes, productos, pedidos y facturación con integridad referencial.",
                epsilon, distribuidora, "ACTIVO", null, false, false);
        Proyecto p4 = proyecto("Tienda en línea de ropa para boutique",
                "Catálogo, carrito de compras y registro de pedidos.",
                beta, boutique, "ACTIVO", "S2D8U5EJ", false, false);
        Proyecto p5 = proyecto("Portal web de citas en línea para clínica dental",
                "Reserva de citas por parte de pacientes y agenda para la clínica.",
                omega, clinica, "ACTIVO", "K7M4Q9XB", true, false);
        Proyecto p6 = proyecto("Aplicación web para control de gastos del hogar",
                "Registro de ingresos y gastos con resumen mensual por categoría.",
                gamma, null, "ACTIVO", "T6B3W8HC", false, false);
        // Mismo grupo de alumnos que P1, pero en OTRA materia con OTRO docente (un proyecto por materia)
        Proyecto p7 = proyecto("Diseño de base de datos para control de biblioteca escolar",
                "Modelo relacional de libros, préstamos y usuarios, con consultas de reporte.",
                alfaBd, null, "ACTIVO", null, false, false);

        // ================= CRITERIOS DOCENTE / JURADO (suman 100 por conjunto) =================
        // Conjuntos GENERALES (ciclo null): valen para cualquier ciclo que no tenga conjunto propio.
        List<Criterio> critWeb = crearCriterios(web, null, null, new String[][]{
                {"Funcionalidad", "30"}, {"Diseño de interfaz y usabilidad", "20"}, {"Calidad del código", "20"},
                {"Documentación", "15"}, {"Presentación y defensa", "15"}});
        List<Criterio> critBd = crearCriterios(bd, null, null, new String[][]{
                {"Modelo entidad-relación", "30"}, {"Normalización", "20"}, {"Consultas SQL", "25"},
                {"Integridad y seguridad", "15"}, {"Presentación", "10"}});
        // Nivel grupo: sobrescribe por completo los de la materia para el proyecto P6
        List<Criterio> critGamma = crearCriterios(null, gamma, null, new String[][]{
                {"Funcionalidad", "40"}, {"Diseño y usabilidad", "20"}, {"Calidad del código", "20"},
                {"Presentación y defensa", "20"}});
        // Ejemplo de conjunto PROPIO de un ciclo: Desarrollo Web, ciclo siguiente (sin evaluaciones: se puede editar)
        crearCriterios(web, null, CICLO_SIGUIENTE, new String[][]{
                {"Funcionalidad", "25"}, {"Diseño de interfaz y usabilidad", "20"}, {"Calidad del código", "20"},
                {"Pruebas", "15"}, {"Documentación", "10"}, {"Presentación y defensa", "10"}});

        // ================= ESQUEMAS (modalidades) =================
        // Esquemas GENERALES (ciclo null)
        EsquemaEvaluacion esqWeb = esquema(web, null, "70", "20", "10");
        EsquemaEvaluacion esqBd = esquema(bd, null, "80", "20", "0");
        String[][] criteriosAuto = {{"Responsabilidad", "25"}, {"Aporte técnico", "30"},
                {"Trabajo en equipo", "25"}, {"Cumplimiento de plazos", "20"}};
        String[][] criteriosPublico = {{"Utilidad", "40"}, {"Facilidad de uso", "35"}, {"Presentación visual", "25"}};
        List<CriterioEsquema> autoWeb = crearCriteriosEsquema(esqWeb, Modalidad.AUTOEVALUACION, criteriosAuto);
        List<CriterioEsquema> pubWeb = crearCriteriosEsquema(esqWeb, Modalidad.PUBLICO, criteriosPublico);
        List<CriterioEsquema> autoBd = crearCriteriosEsquema(esqBd, Modalidad.AUTOEVALUACION, criteriosAuto);
        // Ejemplo de esquema PROPIO de un ciclo: Desarrollo Web, ciclo siguiente (60 / 25 / 15)
        EsquemaEvaluacion esqWebSiguiente = esquema(web, CICLO_SIGUIENTE, "60", "25", "15");
        crearCriteriosEsquema(esqWebSiguiente, Modalidad.AUTOEVALUACION, criteriosAuto);
        crearCriteriosEsquema(esqWebSiguiente, Modalidad.PUBLICO, criteriosPublico);

        // ================= INVITACIONES =================
        // El docente de la asignación siempre es evaluador de los proyectos de sus grupos.
        invitar(p1, u.get("mauricio"), u.get("carlos"));
        invitar(p1, u.get("silvia"), u.get("carlos"));
        invitar(p1, u.get("carlos"), u.get("ricardo"));
        invitar(p2, u.get("ana"), u.get("ricardo"));
        invitar(p2, u.get("carlos"), u.get("ana"));     // docente NO principal de la materia
        invitar(p2, u.get("jorge"), u.get("ana"));
        invitar(p3, u.get("ana"), u.get("ricardo"));
        invitar(p3, u.get("silvia"), u.get("ana"));
        invitar(p4, u.get("carlos"), u.get("ricardo"));
        invitar(p4, u.get("mauricio"), u.get("carlos"));
        invitar(p5, u.get("mauricio"), u.get("carlos"));
        invitar(p5, u.get("carlos"), u.get("ricardo"));
        invitar(p6, u.get("carlos"), u.get("ricardo"));
        invitar(p6, u.get("silvia"), u.get("ricardo"));
        invitar(p6, u.get("jorge"), u.get("carlos"));
        invitar(p7, u.get("ana"), u.get("ricardo"));

        // ================= EVALUACIONES DE DOCENTE / JURADO =================
        // Notas por criterio, en el mismo orden de los criterios de cada lista
        // P1: las tres enviadas (8.78, 8.60, 9.23)
        evaluar(p1, u.get("mauricio"), critWeb, "ENVIADA", "Buena arquitectura y presentación segura.", "9", "8.5", "8", "9", "9.5");
        evaluar(p1, u.get("silvia"), critWeb, "ENVIADA", "Interfaz clara; faltó más documentación.", "8.5", "9", "8.5", "8", "9");
        evaluar(p1, u.get("carlos"), critWeb, "ENVIADA", "Excelente cumplimiento de los requerimientos.", "9.5", "9", "9", "8.5", "10");
        // P2: las tres enviadas (8.65, 8.43, 8.73) -> definitiva, sin publicar
        evaluar(p2, u.get("ana"), critBd, "ENVIADA", "Modelo sólido y bien normalizado.", "9", "8", "9", "8", "9");
        evaluar(p2, u.get("carlos"), critBd, "ENVIADA", "Buenas consultas; mejorar la seguridad.", "8.5", "8", "8.5", "9", "8");
        evaluar(p2, u.get("jorge"), critBd, "ENVIADA", "Presentación muy clara.", "9", "9", "8", "8.5", "9.5");
        // P3: una enviada (8.08) y Silvia sin evaluación
        evaluar(p3, u.get("ana"), critBd, "ENVIADA", "Falta optimizar algunas consultas.", "8", "8.5", "7.5", "8", "9");
        // P4: Carlos enviada (8.13) y Mauricio en BORRADOR (7.73, no cuenta)
        evaluar(p4, u.get("carlos"), critWeb, "ENVIADA", "Catálogo completo; revisar el carrito.", "8", "8", "7.5", "8.5", "9");
        evaluar(p4, u.get("mauricio"), critWeb, "BORRADOR", "Borrador: falta revisar el código.", "7.5", "8", "7", "8", "8.5");
        // P6: Silvia enviada (8.70) con los criterios del grupo; Carlos y Jorge sin evaluación
        evaluar(p6, u.get("silvia"), critGamma, "ENVIADA", "Cumple lo pedido con buen diseño.", "9", "8.5", "8", "9");

        // ================= AUTOEVALUACIONES (P1 y P2 completas) =================
        // Criterios: responsabilidad, aporte técnico, trabajo en equipo, cumplimiento de plazos
        autoevaluar(p1, u.get("kevin"), u.get("kevin"), autoWeb, "9", "9", "9", "9");
        autoevaluar(p1, u.get("kevin"), u.get("daniela"), autoWeb, "9", "8", "9", "9");
        autoevaluar(p1, u.get("kevin"), u.get("jose"), autoWeb, "8", "8", "9", "8");
        autoevaluar(p1, u.get("daniela"), u.get("kevin"), autoWeb, "9", "9", "10", "9");
        autoevaluar(p1, u.get("daniela"), u.get("daniela"), autoWeb, "10", "9", "10", "10");
        autoevaluar(p1, u.get("daniela"), u.get("jose"), autoWeb, "8", "8", "9", "8");
        autoevaluar(p1, u.get("jose"), u.get("kevin"), autoWeb, "9", "10", "9", "9");
        autoevaluar(p1, u.get("jose"), u.get("daniela"), autoWeb, "9", "9", "9", "9");
        autoevaluar(p1, u.get("jose"), u.get("jose"), autoWeb, "9", "9", "9", "8");

        autoevaluar(p2, u.get("walter"), u.get("walter"), autoBd, "9", "9", "9", "9");
        autoevaluar(p2, u.get("walter"), u.get("sofia"), autoBd, "9", "9", "10", "9");
        autoevaluar(p2, u.get("sofia"), u.get("walter"), autoBd, "8", "9", "9", "8");
        autoevaluar(p2, u.get("sofia"), u.get("sofia"), autoBd, "9", "10", "9", "9");

        // ================= EVALUACIÓN PÚBLICA (P1 cerrada con 3 respuestas; P5 abierta sin respuestas) =================
        publica(p1, pubWeb, "9", "8", "9");
        publica(p1, pubWeb, "10", "9", "9");
        publica(p1, pubWeb, "8", "9", "10");

        log.info("Datos de prueba creados. Admin: ricardo.alas@itca.edu.sv / Admin1234 | "
                + "Docentes: carlos.mejia@ y ana.ramirez@ (Docente123) | "
                + "Jurados: mauricio.alfaro@, silvia.canas@, jorge.portillo@ (Jurado123) | "
                + "Alumnos: kevin.hernandez@, fatima.cruz@, walter.calderon@, fernando.castaneda25@... (Alumno123)");
    }

    // ================= AUXILIARES =================
    private Usuario usuario(String nombre, String apellido, String correo, String pass, String rol, boolean activo) {
        return usuarios.findByCorreo(correo).orElseGet(() -> {
            Usuario x = new Usuario();
            x.setNombre(nombre);
            x.setApellido(apellido);
            x.setCorreo(correo);
            x.setPassword(encoder.encode(pass));
            x.setRol(roles.findByNombre(rol).orElseThrow());
            x.setActivo(activo);
            return usuarios.save(x);
        });
    }

    private Materia materia(String nombre, String descripcion, Usuario docente) {
        Materia m = new Materia();
        m.setNombre(nombre);
        m.setDescripcion(descripcion);
        m.setDocentePrincipal(docente);
        return materias.save(m);
    }

    private Seccion seccion(String nombre, String ciclo) {
        Seccion s = new Seccion();
        s.setNombre(nombre);
        s.setCiclo(ciclo);
        return secciones.save(s);
    }

    private Subseccion subseccion(String letra, Seccion seccion) {
        Subseccion s = new Subseccion();
        s.setLetra(letra);
        s.setSeccion(seccion);
        return subsecciones.save(s);
    }

    private void matricular(Subseccion subseccion, Usuario alumno) {
        Matricula m = new Matricula();
        m.setAlumno(alumno);
        m.setSubseccion(subseccion);
        matriculas.save(m);
    }

    private Asignacion asignacion(Subseccion subseccion, Materia materia, Usuario docente) {
        Asignacion a = new Asignacion();
        a.setSubseccion(subseccion);
        a.setMateria(materia);
        a.setDocente(docente);
        return asignaciones.save(a);
    }

    private Grupo equipo(String nombre, Asignacion asignacion, Usuario... integrantes) {
        Grupo g = new Grupo();
        g.setNombre(nombre);
        g.setAsignacion(asignacion);
        g = grupos.save(g);
        for (Usuario a : integrantes) {
            GrupoAlumno m = new GrupoAlumno();
            m.setGrupo(g);
            m.setAsignacion(asignacion);
            m.setAlumno(a);
            miembros.save(m);
        }
        return g;
    }

    private Empresa empresa(String nombre, String contacto, String correo, String telefono) {
        Empresa e = new Empresa();
        e.setNombre(nombre);
        e.setContacto(contacto);
        e.setCorreo(correo);
        e.setTelefono(telefono);
        return empresas.save(e);
    }

    private Proyecto proyecto(String titulo, String descripcion, Grupo grupo, Empresa empresa,
                              String estado, String codigo, boolean publicoAbierto, boolean publicado) {
        Proyecto p = new Proyecto();
        p.setTitulo(titulo);
        p.setDescripcion(descripcion);
        p.setGrupo(grupo);
        p.setEmpresa(empresa);
        p.setEstado(estado);
        p.setCodigoPublico(codigo);
        p.setPublicoAbierto(publicoAbierto);
        p.setResultadosPublicados(publicado);
        return proyectos.save(p);
    }

    /**
     * Crea el conjunto de criterios de un nivel (materia O grupo; el otro va null) para un ciclo
     * (null = conjunto general). Devuelve la lista en orden.
     */
    private List<Criterio> crearCriterios(Materia materia, Grupo grupo, String ciclo, String[][] items) {
        List<Criterio> salida = new ArrayList<>();
        for (String[] it : items) {
            Criterio c = new Criterio();
            c.setNombre(it[0]);
            c.setPonderacion(new BigDecimal(it[1]));
            c.setMateria(materia);
            c.setGrupo(grupo);
            c.setCiclo(ciclo);
            salida.add(criterios.save(c));
        }
        return salida;
    }

    /** Esquema de una materia para un ciclo (null = esquema general). */
    private EsquemaEvaluacion esquema(Materia materia, String ciclo, String docenteJurado, String auto, String publico) {
        EsquemaEvaluacion e = new EsquemaEvaluacion();
        e.setMateria(materia);
        e.setCiclo(ciclo);
        e.setPesoDocenteJurado(new BigDecimal(docenteJurado));
        e.setPesoAutoevaluacion(new BigDecimal(auto));
        e.setPesoPublico(new BigDecimal(publico));
        return esquemas.save(e);
    }

    private List<CriterioEsquema> crearCriteriosEsquema(EsquemaEvaluacion esq, Modalidad modalidad, String[][] items) {
        List<CriterioEsquema> salida = new ArrayList<>();
        for (String[] it : items) {
            CriterioEsquema c = new CriterioEsquema();
            c.setEsquema(esq);
            c.setModalidad(modalidad);
            c.setNombre(it[0]);
            c.setPonderacion(new BigDecimal(it[1]));
            salida.add(criteriosEsquema.save(c));
        }
        return salida;
    }

    private void invitar(Proyecto p, Usuario evaluador, Usuario invitadoPor) {
        EvaluadorInvitado i = new EvaluadorInvitado();
        i.setProyecto(p);
        i.setEvaluador(evaluador);
        i.setInvitadoPor(invitadoPor);
        invitaciones.save(i);
    }

    private void evaluar(Proyecto p, Usuario evaluador, List<Criterio> crit, String estado,
                         String comentario, String... notas) {
        Evaluacion e = new Evaluacion();
        e.setProyecto(p);
        e.setEvaluador(evaluador);
        e.setEstado(estado);
        e.setComentarios(comentario);
        e = evaluaciones.save(e);

        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < crit.size(); i++) {
            BigDecimal nota = new BigDecimal(notas[i]);
            EvaluacionDetalle d = new EvaluacionDetalle();
            d.setEvaluacion(e);
            d.setCriterio(crit.get(i));
            d.setNotaOtorgada(nota);
            detalles.save(d);
            total = total.add(nota.multiply(crit.get(i).getPonderacion()).divide(CIEN, 6, RoundingMode.HALF_UP));
        }
        if ("ENVIADA".equals(estado)) {
            e.setCalificacionFinal(total.setScale(2, RoundingMode.HALF_UP));
            evaluaciones.save(e);
        }
    }

    private void autoevaluar(Proyecto p, Usuario evaluador, Usuario evaluado,
                             List<CriterioEsquema> crit, String... notas) {
        EvaluacionAlumno e = new EvaluacionAlumno();
        e.setProyecto(p);
        e.setEvaluador(evaluador);
        e.setEvaluado(evaluado);
        e = evaluacionesAlumno.save(e);
        for (int i = 0; i < crit.size(); i++) {
            EvaluacionAlumnoDetalle d = new EvaluacionAlumnoDetalle();
            d.setEvaluacion(e);
            d.setCriterio(crit.get(i));
            d.setNota(new BigDecimal(notas[i]));
            detallesAlumno.save(d);
        }
    }

    private void publica(Proyecto p, List<CriterioEsquema> crit, String... notas) {
        EvaluacionPublica e = new EvaluacionPublica();
        e.setProyecto(p);
        e = evaluacionesPublicas.save(e);
        for (int i = 0; i < crit.size(); i++) {
            EvaluacionPublicaDetalle d = new EvaluacionPublicaDetalle();
            d.setEvaluacion(e);
            d.setCriterio(crit.get(i));
            d.setNota(new BigDecimal(notas[i]));
            detallesPublica.save(d);
        }
    }
}
