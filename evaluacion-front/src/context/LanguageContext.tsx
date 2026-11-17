import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'

export type Language = 'es' | 'en'
interface LanguageContextValue { language: Language; toggleLanguage: () => void; text: (spanish: string) => string }
const LanguageContext = createContext<LanguageContextValue | undefined>(undefined)

const translations: Record<string, string> = {
    'INICIO': 'HOME', 'GESTIÓN ACADÉMICA': 'ACADEMIC MANAGEMENT', 'EVALUACIÓN': 'ASSESSMENT', 'RESULTADOS': 'RESULTS', 'MI PROYECTO': 'MY PROJECT',
    'Resumen general': 'Overview', 'Usuarios': 'Users', 'Materias': 'Subjects', 'Secciones': 'Sections', 'Matrículas': 'Enrollments',
    'Alumnos y grupos': 'Students and groups', 'Empresas': 'Companies', 'Proyectos': 'Projects', 'Criterios': 'Criteria',
    'Modalidades y porcentajes': 'Assessment types and percentages', 'Invitar evaluadores': 'Invite evaluators', 'Evaluación pública': 'Public assessment',
    'Proyectos asignados': 'Assigned projects', 'Calificar': 'Grade', 'Resultados por evaluador': 'Results by evaluator', 'Notas finales': 'Final grades', 'Reportes': 'Reports',
    'Inicio': 'Home', 'Mi proyecto': 'My project', 'Criterios del proyecto': 'Project criteria', 'Autoevaluación': 'Self-assessment', 'Mi nota': 'My grade',
    'Administrador': 'Administrator', 'Docente': 'Teacher', 'Jurado': 'Jury', 'Estudiante': 'Student', 'Cerrar sesión': 'Sign out',
    'Cambiar a modo claro': 'Switch to light mode', 'Cambiar a modo oscuro': 'Switch to dark mode', 'Modo claro': 'Light mode', 'Modo oscuro': 'Dark mode',
    'Sistema Web de Evaluación': 'Assessment Web System', 'Sesión segura': 'Secure session',
    'MÓDULO DE EVALUACIÓN': 'ASSESSMENT MODULE',
    'Modalidades de evaluación': 'Assessment types', 'CONFIGURACIÓN': 'SETTINGS',
    'MI RESULTADO': 'MY RESULT', 'MI NOTA': 'MY GRADE',
    'FERIAS Y DEFENSAS': 'FAIRS AND PRESENTATIONS',
    'Califica tu aporte y el de tus compañeros. Nadie verá quién puso cada nota.': 'Rate your contribution and your teammates’ contributions. No one will see who submitted each grade.',
    'Elige quién evalúa a los estudiantes y cuánto vale cada nota. Se define por materia y por ciclo.': 'Choose who evaluates students and how much each grade is worth. Settings apply by subject and term.',
    'Asigna alumnos a las subsecciones de sus materias.': 'Assign students to subject subsections.',
    'Información de tu proyecto y de los integrantes de tu grupo.': 'Information about your project and group members.',
    'Con estos criterios se evalúa cada uno de tus proyectos activos.': 'These criteria are used to assess each of your active projects.',
    'Filtra los proyectos, consulta el detalle y descarga el reporte en PDF o Excel.': 'Filter projects, view details, and download reports in PDF or Excel.',
    'Desglose por alumno según las modalidades que definió el docente para la materia.': 'Student breakdown by the assessment types set by the teacher for the subject.',
    'Abre la evaluación de un proyecto para que el público la califique desde su celular, sin cuenta ni correo.': 'Open a project assessment for the public to rate from a phone, without an account or email.',
    'PANEL DE ADMINISTRACIÓN': 'ADMIN DASHBOARD', 'PANEL DOCENTE': 'TEACHER DASHBOARD', 'PANEL DE EVALUACIÓN': 'ASSESSMENT DASHBOARD', 'VISTA ESTUDIANTE': 'STUDENT VIEW',
    'Gestiona usuarios, procesos académicos y resultados del sistema.': 'Manage users, academic processes, and system results.',
    'Organiza tus materias, grupos y evaluaciones académicas.': 'Organize your subjects, groups, and assessments.',
    'Revisa los proyectos asignados y registra tus calificaciones.': 'Review assigned projects and record your grades.',
    'Consulta tu proyecto, tus criterios y tus resultados publicados.': 'View your project, criteria, and published results.',
    'Accesos rápidos': 'Quick links', 'Acciones frecuentes': 'Common actions', 'Tu nota se muestra cuando el docente publique los resultados.': 'Your grade will appear when the teacher publishes results.',
}

function getInitialLanguage(): Language {
    try { return localStorage.getItem('language') === 'en' ? 'en' : 'es' } catch { return 'es' }
}

export function LanguageProvider({ children }: { children: ReactNode }) {
    const [language, setLanguage] = useState<Language>(getInitialLanguage)
    useEffect(() => {
        document.documentElement.lang = language
        try { localStorage.setItem('language', language) } catch { /* ignore */ }
    }, [language])
    const toggleLanguage = () => setLanguage(current => current === 'es' ? 'en' : 'es')
    const text = (spanish: string) => language === 'es' ? spanish : translations[spanish] ?? spanish
    return <LanguageContext.Provider value={{ language, toggleLanguage, text }}>{children}</LanguageContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useLanguage() {
    const context = useContext(LanguageContext)
    if (!context) throw new Error('useLanguage debe usarse dentro de LanguageProvider')
    return context
}
