import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
    BarChart3,
    BookOpen,
    Building2,
    CheckCircle2,
    ClipboardCheck,
    FileText,
    GraduationCap,
    LayoutDashboard,
    LogOut,
    Menu,
    PanelLeftClose,
    PanelLeftOpen,
    Settings,
    ShieldCheck,
    Users,
    X,
} from 'lucide-react'
import api from '../api/axios'
import { useAuth } from '../context/AuthContext'
import './Dashboard.css'

type Rol = 'ADMIN' | 'DOCENTE' | 'JURADO' | 'ALUMNO'
type Icon = typeof LayoutDashboard

interface MenuItem {
    label: string
    icon: Icon
}

interface DashboardCounts {
    usuarios: number
    materias: number
    grupos: number
    proyectos: number
}

const menuPorRol: Record<Rol, MenuItem[]> = {
    ADMIN: [
        { label: 'Resumen general', icon: LayoutDashboard },
        { label: 'Docentes y jurado', icon: Users },
        { label: 'Materias y secciones', icon: BookOpen },
        { label: 'Alumnos y grupos', icon: GraduationCap },
        { label: 'Empresas', icon: Building2 },
        { label: 'Proyectos', icon: FileText },
        { label: 'Criterios', icon: ClipboardCheck },
        { label: 'Reportes', icon: BarChart3 },
    ],
    DOCENTE: [
        { label: 'Resumen general', icon: LayoutDashboard },
        { label: 'Materias y secciones', icon: BookOpen },
        { label: 'Alumnos y grupos', icon: GraduationCap },
        { label: 'Proyectos', icon: FileText },
        { label: 'Criterios', icon: ClipboardCheck },
        { label: 'Calificación', icon: CheckCircle2 },
        { label: 'Reportes', icon: BarChart3 },
    ],
    JURADO: [
        { label: 'Resumen general', icon: LayoutDashboard },
        { label: 'Proyectos asignados', icon: FileText },
        { label: 'Criterios', icon: ClipboardCheck },
        { label: 'Calificación', icon: CheckCircle2 },
    ],
    ALUMNO: [
        { label: 'Mi proyecto', icon: FileText },
        { label: 'Criterios', icon: ClipboardCheck },
        { label: 'Mi resultado', icon: BarChart3 },
    ],
}

const titulosPorRol: Record<Rol, { eyebrow: string; title: string; description: string }> = {
    ADMIN: {
        eyebrow: 'PANEL DE ADMINISTRACIÓN',
        title: 'Resumen general',
        description: 'Gestiona usuarios, procesos académicos y resultados del sistema.',
    },
    DOCENTE: {
        eyebrow: 'PANEL DOCENTE',
        title: 'Resumen general',
        description: 'Organiza tus materias, grupos y evaluaciones académicas.',
    },
    JURADO: {
        eyebrow: 'PANEL DE EVALUACIÓN',
        title: 'Proyectos asignados',
        description: 'Revisa los proyectos y registra tus calificaciones.',
    },
    ALUMNO: {
        eyebrow: 'VISTA ESTUDIANTE',
        title: 'Mi resultado',
        description: 'Consulta tu proyecto, criterios y resultados publicados.',
    },
}

function nombreRol(rol: Rol) {
    return { ADMIN: 'Administrador', DOCENTE: 'Docente', JURADO: 'Jurado', ALUMNO: 'Estudiante' }[rol]
}

export default function Dashboard() {
    const { usuario, logout } = useAuth()
    const navigate = useNavigate()
    const [menuAbierto, setMenuAbierto] = useState(false)
    const [barraColapsada, setBarraColapsada] = useState(false)
    const [seccionActiva, setSeccionActiva] = useState(0)
    const [counts, setCounts] = useState<DashboardCounts>({ usuarios: 0, materias: 0, grupos: 0, proyectos: 0 })
    const [resumenError, setResumenError] = useState('')

    const rol = (usuario?.rol ?? 'ALUMNO') as Rol
    const menu = menuPorRol[rol]
    const contenido = titulosPorRol[rol]
    const iniciales = useMemo(
        () => (usuario?.nombre ?? 'Usuario').split(' ').map((parte) => parte[0]).slice(0, 2).join('').toUpperCase(),
        [usuario?.nombre],
    )

    useEffect(() => {
        let activo = true
        const cargarResumen = async () => {
            const solicitudes: Promise<unknown>[] = [
                api.get('/materias').then(({ data }) => data),
                api.get('/grupos').then(({ data }) => data),
                api.get('/proyectos').then(({ data }) => data),
            ]
            if (rol === 'ADMIN') solicitudes.unshift(api.get('/usuarios').then(({ data }) => data))

            const resultados = await Promise.allSettled(solicitudes)
            if (!activo) return
            setResumenError(resultados.some((resultado) => resultado.status === 'rejected')
                ? 'Algunos datos del resumen no pudieron cargarse. Puedes continuar usando el menú.'
                : '')
            const valores = resultados.map((resultado) => resultado.status === 'fulfilled' && Array.isArray(resultado.value) ? resultado.value.length : 0)
            setCounts({
                usuarios: rol === 'ADMIN' ? valores[0] : 0,
                materias: rol === 'ADMIN' ? valores[1] : valores[0],
                grupos: rol === 'ADMIN' ? valores[2] : valores[1],
                proyectos: rol === 'ADMIN' ? valores[3] : valores[2],
            })
        }
        void cargarResumen()
        return () => { activo = false }
    }, [rol])

    const cerrarSesion = () => {
        logout()
        navigate('/login')
    }

    const tarjetas = rol === 'ADMIN'
        ? [
            ['Usuarios registrados', counts.usuarios, Users],
            ['Materias activas', counts.materias, BookOpen],
            ['Grupos creados', counts.grupos, GraduationCap],
            ['Proyectos registrados', counts.proyectos, FileText],
        ]
        : rol === 'ALUMNO'
            ? [
                ['Mi proyecto', counts.proyectos ? 'Activo' : 'Pendiente', FileText],
                ['Criterios de evaluación', 'Consultar', ClipboardCheck],
                ['Resultado final', 'Pendiente', BarChart3],
            ]
            : [
                ['Materias', counts.materias, BookOpen],
                ['Grupos', counts.grupos, GraduationCap],
                ['Proyectos', counts.proyectos, FileText],
            ]

    return (
        <div className={`app-shell ${barraColapsada ? 'sidebar-collapsed' : ''}`}>
            <button className="mobile-menu" onClick={() => setMenuAbierto(true)} aria-label="Abrir menú">
                <Menu size={22} />
            </button>
            <aside className={`sidebar ${menuAbierto ? 'sidebar-open' : ''}`}>
                <div className="sidebar-top">
                    <div className="brand">
                        <div className="brand-mark"><GraduationCap size={24} /></div>
                        <div className="brand-text"><strong>Sistema Web de<br />Evaluación</strong><span>Regional Santa Ana · Ciclo 02–2026</span></div>
                    </div>
                    <button className="close-menu" onClick={() => setMenuAbierto(false)} aria-label="Cerrar menú"><X size={20} /></button>
                    <nav>
                        <p className="nav-heading">NAVEGACIÓN</p>
                        {menu.map((item, index) => {
                            const Icon = item.icon
                            return (
                                <button
                                    className={`nav-item ${index === seccionActiva ? 'active' : ''}`}
                                    key={item.label}
                                    onClick={() => {
                                        setSeccionActiva(index); setMenuAbierto(false)
                                        const destinos: Record<string, string> = {
                                            'Docentes y jurado': 'usuarios',
                                            'Materias y secciones': 'materias',
                                            'Alumnos y grupos': 'grupos',
                                            Empresas: 'empresas',
                                            Proyectos: 'proyectos',
                                            Criterios: 'criterios',
                                        }
                                        if (destinos[item.label]) navigate(`/gestion/${destinos[item.label]}`)
                                    }}
                                >
                                    <Icon size={18} />
                                    <span>{item.label}</span>
                                </button>
                            )
                        })}
                    </nav>
                </div>
                <div className="sidebar-bottom">
                    <button className="logout-button" onClick={cerrarSesion}><LogOut size={18} /><span>Cerrar sesión</span></button>
                    <strong>ITCA-FEPADE</strong>
                </div>
            </aside>

            <main className="dashboard-main">
                <header className="topbar">
                    <button className="collapse-button" onClick={() => setBarraColapsada(!barraColapsada)} aria-label="Contraer menú">
                        {barraColapsada ? <PanelLeftOpen size={20} /> : <PanelLeftClose size={20} />}
                    </button>
                    <div className="topbar-title">{contenido.title}</div>
                    <div className="user-chip"><span className="avatar">{iniciales}</span><span>{nombreRol(rol)}</span></div>
                </header>

                <section className="dashboard-content">
                    <div className="page-heading">
                        <div><span className="page-eyebrow">{contenido.eyebrow}</span><h1>{contenido.title}</h1><p>{contenido.description}</p></div>
                        <div className="security-badge"><ShieldCheck size={17} /> Sesión segura</div>
                    </div>

                    <div className="summary-grid">
                        {tarjetas.map(([label, value, CardIcon]) => {
                            const Icon = CardIcon as Icon
                            return <article className="summary-card" key={String(label)}><div className="summary-icon"><Icon size={21} /></div><div><span>{String(label)}</span><strong>{String(value)}</strong></div></article>
                        })}
                    </div>
                    {resumenError && <p className="dashboard-alert" role="status">{resumenError}</p>}

                    {rol === 'ALUMNO' ? <StudentPanel /> : <ManagementPanel rol={rol} />}
                </section>
            </main>
        </div>
    )
}

function ManagementPanel({ rol }: { rol: Rol }) {
    return (
        <div className="content-grid">
            <section className="panel panel-large">
                <div className="panel-heading"><div><span className="panel-kicker">ACTIVIDAD RECIENTE</span><h2>{rol === 'ADMIN' ? 'Gestión del sistema' : 'Seguimiento académico'}</h2></div><Settings size={19} /></div>
                <div className="empty-state"><CheckCircle2 size={30} /><strong>Todo listo para trabajar</strong><p>Selecciona una opción del menú para comenzar a gestionar la información.</p></div>
            </section>
            <section className="panel">
                <div className="panel-heading"><div><span className="panel-kicker">ACCESOS RÁPIDOS</span><h2>Acciones frecuentes</h2></div></div>
                <div className="quick-actions">
                    <button><BookOpen size={18} /> Consultar materias</button>
                    <button><FileText size={18} /> Revisar proyectos</button>
                    <button><BarChart3 size={18} /> Ver reportes</button>
                </div>
            </section>
        </div>
    )
}

function StudentPanel() {
    return (
        <div className="content-grid student-grid">
            <section className="panel panel-large result-panel">
                <div className="panel-heading"><div><span className="panel-kicker">TUS RESULTADOS</span><h2>Mi proyecto</h2></div><FileText size={21} /></div>
                <div className="result-placeholder"><span>Resultado pendiente</span><strong>—</strong><p>Cuando el resultado sea publicado aparecerá aquí.</p></div>
            </section>
            <section className="panel">
                <div className="panel-heading"><div><span className="panel-kicker">EVALUACIÓN</span><h2>Estado</h2></div><ClipboardCheck size={20} /></div>
                <div className="progress-block"><div><span>Avance del proyecto</span><strong>En proceso</strong></div><div className="progress-track"><i /></div><small>Consulta los criterios asignados a tu grupo.</small></div>
            </section>
        </div>
    )
}
