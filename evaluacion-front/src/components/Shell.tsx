import { useLayoutEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { GraduationCap, LogOut, Menu, PanelLeftClose, PanelLeftOpen, X } from 'lucide-react'
import { useAuth, type Rol } from '../context/AuthContext'
import { menuPorRol, nombreRol } from '../config/menu'
import ThemeToggle from './ThemeToggle'
import '../pages/Dashboard.css'
import '../pages/Gestion.css'
import '../pages/Modalidades.css'
import './Shell.css'

interface Props {
    eyebrow: string
    title: string
    description: string
    badge?: ReactNode
    children: ReactNode
}

const CLAVE_COLAPSADA = 'sidebarColapsada'

// Vive fuera del componente: sobrevive cuando el Shell se vuelve a montar al cambiar de pantalla
let scrollSidebar = 0

const leerColapsada = () => {
    try { return localStorage.getItem(CLAVE_COLAPSADA) === '1' } catch { return false }
}

export default function Shell({ eyebrow, title, description, badge, children }: Props) {
    const { usuario, logout } = useAuth()
    const navigate = useNavigate()
    const { pathname } = useLocation()
    const [abierto, setAbierto] = useState(false)
    const [colapsada, setColapsada] = useState(leerColapsada)
    const sidebarRef = useRef<HTMLElement>(null)

    // Restaura el scroll del sidebar antes de pintar, para que no se vea el salto
    useLayoutEffect(() => {
        if (sidebarRef.current) sidebarRef.current.scrollTop = scrollSidebar
    }, [])

    const alternarColapsada = () => {
        const nueva = !colapsada
        setColapsada(nueva)
        try { localStorage.setItem(CLAVE_COLAPSADA, nueva ? '1' : '0') } catch { /* ignorar */ }
    }

    const rol = (usuario?.rol ?? 'ALUMNO') as Rol
    const grupos = menuPorRol[rol]
    const iniciales = useMemo(
        () => (usuario?.nombre ?? 'Usuario').split(' ').map((x) => x[0]).slice(0, 2).join('').toUpperCase(),
        [usuario?.nombre],
    )

    const ir = (to: string) => {
        setAbierto(false)
        navigate(to)
    }

    return (
        <div className={`app-shell ${colapsada ? 'sidebar-collapsed' : ''}`}>
            <button className="mobile-menu" onClick={() => setAbierto(true)} aria-label="Abrir menú"><Menu size={22} /></button>

            <aside
                ref={sidebarRef}
                onScroll={(e) => { scrollSidebar = e.currentTarget.scrollTop }}
                className={`sidebar ${abierto ? 'sidebar-open' : ''}`}
            >
                <div className="sidebar-top">
                    <div className="brand">
                        <div className="brand-mark"><GraduationCap size={24} /></div>
                        <div className="brand-text"><strong>Sistema Web de<br />Evaluación</strong><span>Regional Santa Ana · Ciclo 02–2026</span></div>
                    </div>
                    <button className="close-menu" onClick={() => setAbierto(false)} aria-label="Cerrar menú"><X size={20} /></button>
                    <nav>
                        {grupos.map((grupo) => (
                            <div className="nav-group" key={grupo.titulo}>
                                <p className="nav-heading">{grupo.titulo}</p>
                                {grupo.items.map((item) => {
                                    const Icono = item.icon
                                    return (
                                        <button
                                            key={item.to}
                                            className={`nav-item ${pathname === item.to ? 'active' : ''}`}
                                            onClick={() => ir(item.to)}
                                            title={item.label}
                                        >
                                            <Icono size={18} /><span>{item.label}</span>
                                        </button>
                                    )
                                })}
                            </div>
                        ))}
                    </nav>
                </div>
                <div className="sidebar-bottom">
                    <button className="logout-button" onClick={() => { logout(); navigate('/login') }}>
                        <LogOut size={18} /><span>Cerrar sesión</span>
                    </button>
                    <strong>ITCA-FEPADE</strong>
                </div>
            </aside>

            <main className="dashboard-main">
                <header className="topbar">
                    <button className="collapse-button" onClick={alternarColapsada} aria-label="Contraer menú">
                        {colapsada ? <PanelLeftOpen size={20} /> : <PanelLeftClose size={20} />}
                    </button>
                    <div className="topbar-title">{title}</div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                        <ThemeToggle />
                        <div className="user-chip"><span className="avatar">{iniciales}</span><span>{nombreRol(rol)}</span></div>
                    </div>
                </header>
                <section className="dashboard-content">
                    <div className="page-heading">
                        <div><span className="page-eyebrow">{eyebrow}</span><h1>{title}</h1><p>{description}</p></div>
                        {badge && <div className="security-badge">{badge}</div>}
                    </div>
                    {children}
                </section>
            </main>
        </div>
    )
}