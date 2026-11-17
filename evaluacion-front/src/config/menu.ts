import {
    BookOpen, Building2, Calculator, CheckCircle2, ClipboardCheck, FileText, GraduationCap,
    LayoutDashboard, Layers, Percent, QrCode, UserPlus, Users, BarChart3, Award, CalendarDays,
} from 'lucide-react'
import type { Rol } from '../context/AuthContext'

export type Icono = typeof LayoutDashboard

export interface ItemMenu { to: string; label: string; icon: Icono }
export interface GrupoMenu { titulo: string; items: ItemMenu[] }

const inicio: GrupoMenu = {
    titulo: 'INICIO',
    items: [{ to: '/dashboard', label: 'Resumen general', icon: LayoutDashboard }],
}

const gestion = (conUsuarios: boolean): GrupoMenu => ({
    titulo: 'GESTIÓN ACADÉMICA',
    items: [
        ...(conUsuarios ? [{ to: '/gestion/usuarios', label: 'Usuarios', icon: Users }] : []),
        ...(conUsuarios ? [{ to: '/ciclos', label: 'Ciclos académicos', icon: CalendarDays }] : []),
        { to: '/gestion/materias', label: 'Materias', icon: BookOpen },
        { to: '/gestion/secciones', label: 'Secciones', icon: Layers },
        { to: '/matriculas', label: 'Matrículas', icon: UserPlus },
        { to: '/gestion/grupos', label: 'Alumnos y grupos', icon: GraduationCap },
        { to: '/gestion/empresas', label: 'Empresas', icon: Building2 },
        { to: '/gestion/proyectos', label: 'Proyectos', icon: FileText },
        { to: '/gestion/criterios', label: 'Criterios', icon: ClipboardCheck },
    ],
})

const configuracion: ItemMenu[] = [
    { to: '/modalidades', label: 'Modalidades y porcentajes', icon: Percent },
    { to: '/evaluacion/invitar', label: 'Invitar evaluadores', icon: UserPlus },
    { to: '/evaluacion-publica', label: 'Evaluación pública', icon: QrCode },
]

const comoEvaluador: ItemMenu[] = [
    { to: '/evaluacion/pendientes', label: 'Proyectos asignados', icon: FileText },
    { to: '/evaluacion/calificar', label: 'Calificar', icon: CheckCircle2 },
]

const resultadosGestion: GrupoMenu = {
    titulo: 'RESULTADOS',
    items: [
        { to: '/evaluacion/resultados', label: 'Resultados por evaluador', icon: BarChart3 },
        { to: '/notas', label: 'Notas finales', icon: Calculator },
        { to: '/reportes', label: 'Reportes', icon: BarChart3 },
    ],
}

export const menuPorRol: Record<Rol, GrupoMenu[]> = {
    ADMIN: [
        inicio,
        gestion(true),
        { titulo: 'EVALUACIÓN', items: configuracion },
        resultadosGestion,
    ],
    DOCENTE: [
        inicio,
        gestion(false),
        { titulo: 'EVALUACIÓN', items: [...configuracion, ...comoEvaluador] },
        resultadosGestion,
    ],
    JURADO: [
        inicio,
        { titulo: 'EVALUACIÓN', items: comoEvaluador },
        { titulo: 'RESULTADOS', items: [{ to: '/reportes', label: 'Reportes', icon: BarChart3 }] },
    ],
    ALUMNO: [
        { titulo: 'INICIO', items: [{ to: '/dashboard', label: 'Inicio', icon: LayoutDashboard }] },
        {
            titulo: 'MI PROYECTO',
            items: [
                { to: '/mi-proyecto', label: 'Mi proyecto', icon: FileText },
                { to: '/mis-criterios', label: 'Criterios del proyecto', icon: ClipboardCheck },
                { to: '/autoevaluacion', label: 'Autoevaluación', icon: CheckCircle2 },
                { to: '/mi-nota', label: 'Mi nota', icon: Award },
            ],
        },
    ],
}

export function nombreRol(rol: Rol) {
    return { ADMIN: 'Administrador', DOCENTE: 'Docente', JURADO: 'Jurado', ALUMNO: 'Estudiante' }[rol]
}
