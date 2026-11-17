import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
    Award, BarChart3, BookOpen, Calculator, CheckCircle2, ClipboardCheck, FileText,
    GraduationCap, Percent, ShieldCheck, UserPlus, Users,
} from 'lucide-react'
import api from '../api/axios'
import Shell from '../components/Shell'
import { useAuth, type Rol } from '../context/AuthContext'
import type { Icono } from '../config/menu'

type Row = Record<string, any>
interface Tarjeta { label: string; valor: string | number; icon: Icono }
interface Acceso { to: string; label: string; icon: Icono }

const lista = (d: any): Row[] => (Array.isArray(d) ? d : d?.content ?? [])

const textos: Record<Rol, { eyebrow: string; title: string; description: string }> = {
    ADMIN: { eyebrow: 'PANEL DE ADMINISTRACIÓN', title: 'Resumen general', description: 'Gestiona usuarios, procesos académicos y resultados del sistema.' },
    DOCENTE: { eyebrow: 'PANEL DOCENTE', title: 'Resumen general', description: 'Organiza tus materias, grupos y evaluaciones académicas.' },
    JURADO: { eyebrow: 'PANEL DE EVALUACIÓN', title: 'Resumen general', description: 'Revisa los proyectos asignados y registra tus calificaciones.' },
    ALUMNO: { eyebrow: 'VISTA ESTUDIANTE', title: 'Inicio', description: 'Consulta tu proyecto, tus criterios y tus resultados publicados.' },
}

const accesos: Record<Rol, Acceso[]> = {
    ADMIN: [
        { to: '/gestion/proyectos', label: 'Gestionar proyectos', icon: FileText },
        { to: '/modalidades', label: 'Modalidades y porcentajes', icon: Percent },
        { to: '/notas', label: 'Notas finales', icon: Calculator },
        { to: '/reportes', label: 'Reportes', icon: BarChart3 },
    ],
    DOCENTE: [
        { to: '/gestion/proyectos', label: 'Gestionar proyectos', icon: FileText },
        { to: '/evaluacion/invitar', label: 'Invitar evaluadores', icon: UserPlus },
        { to: '/notas', label: 'Notas finales', icon: Calculator },
        { to: '/reportes', label: 'Reportes', icon: BarChart3 },
    ],
    JURADO: [
        { to: '/evaluacion/pendientes', label: 'Proyectos asignados', icon: FileText },
        { to: '/evaluacion/calificar', label: 'Calificar', icon: CheckCircle2 },
        { to: '/reportes', label: 'Reportes', icon: BarChart3 },
    ],
    ALUMNO: [
        { to: '/autoevaluacion', label: 'Autoevaluación', icon: ClipboardCheck },
        { to: '/mis-criterios', label: 'Criterios del proyecto', icon: FileText },
        { to: '/mi-nota', label: 'Mi nota', icon: Award },
    ],
}

export default function Dashboard() {
    const { usuario } = useAuth()
    const navigate = useNavigate()
    const rol = (usuario?.rol ?? 'ALUMNO') as Rol
    const [tarjetas, setTarjetas] = useState<Tarjeta[]>([])
    const [pendientes, setPendientes] = useState<Row[]>([])
    const [notas, setNotas] = useState<Row[]>([])
    const [aviso, setAviso] = useState('')

    useEffect(() => {
        let activo = true
        const cargar = async () => {
            const fallos: string[] = []
            const pedir = async (url: string): Promise<Row[]> => {
                try { return lista((await api.get(url)).data) } catch { fallos.push(url); return [] }
            }
            let cards: Tarjeta[] = []
            let pend: Row[] = []
            let nts: Row[] = []

            if (rol === 'ADMIN') {
                const [u, m, g, p] = await Promise.all([pedir('/usuarios'), pedir('/materias'), pedir('/grupos'), pedir('/proyectos')])
                cards = [
                    { label: 'Usuarios registrados', valor: u.length, icon: Users },
                    { label: 'Materias activas', valor: m.length, icon: BookOpen },
                    { label: 'Grupos creados', valor: g.length, icon: GraduationCap },
                    { label: 'Proyectos registrados', valor: p.length, icon: FileText },
                ]
            } else if (rol === 'DOCENTE') {
                const [materias, grupos, proyectos, asignaciones, mias] = await Promise.all([
                    pedir('/materias'), pedir('/grupos'), pedir('/proyectos'), pedir('/asignaciones'), pedir('/invitaciones/mias'),
                ])
                const docenteId = Number(usuario?.id)
                const asignacionesPropias = asignaciones.filter((a) => Number(a.docenteId) === docenteId)
                const asignacionIds = new Set(asignacionesPropias.map((a) => Number(a.id)))
                const gruposPropios = grupos.filter((g) => asignacionIds.has(Number(g.asignacionId)))
                const grupoIds = new Set(gruposPropios.map((g) => Number(g.id)))
                const materiasIds = new Set<number>([
                    ...materias.filter((m) => Number(m.docentePrincipalId) === docenteId).map((m) => Number(m.id)),
                    ...asignacionesPropias.map((a) => Number(a.materiaId)),
                ])
                const proyectosPropios = proyectos.filter((p) => grupoIds.has(Number(p.grupoId)))
                pend = mias.filter((x) => x.estadoEvaluacion !== 'ENVIADA')
                cards = [
                    { label: 'Materias', valor: materiasIds.size, icon: BookOpen },
                    { label: 'Grupos', valor: gruposPropios.length, icon: GraduationCap },
                    { label: 'Proyectos', valor: proyectosPropios.length, icon: FileText },
                    { label: 'Por calificar', valor: pend.length, icon: CheckCircle2 },
                ]
            } else if (rol === 'JURADO') {
                const mias = await pedir('/invitaciones/mias')
                pend = mias.filter((x) => x.estadoEvaluacion !== 'ENVIADA')
                cards = [
                    { label: 'Proyectos asignados', valor: mias.length, icon: FileText },
                    { label: 'Por calificar', valor: pend.length, icon: ClipboardCheck },
                    { label: 'Enviadas', valor: mias.length - pend.length, icon: CheckCircle2 },
                ]
            } else {
                const [p, n] = await Promise.all([pedir('/proyectos'), pedir('/resultados/mi-nota')])
                nts = n
                const publicada = n.find((x) => x.publicado && x.nota)
                cards = [
                    { label: 'Mi proyecto', valor: p.length ? 'Asignado' : 'Sin proyecto', icon: FileText },
                    { label: 'Resultados', valor: n.some((x) => x.publicado) ? 'Publicados' : 'Pendientes', icon: BarChart3 },
                    { label: 'Nota final', valor: publicada?.nota?.notaFinal != null ? Number(publicada.nota.notaFinal).toFixed(2) : '—', icon: Award },
                ]
            }

            if (!activo) return
            setTarjetas(cards)
            setPendientes(pend)
            setNotas(nts)
            setAviso(fallos.length ? 'Algunos datos del resumen no pudieron cargarse. Puedes continuar usando el menú.' : '')
        }
        void cargar()
        return () => { activo = false }
    }, [rol, usuario?.id])

    const texto = textos[rol]
    const publicada = notas.find((x) => x.publicado && x.nota)

    return (
        <Shell
            eyebrow={texto.eyebrow}
            title={texto.title}
            description={texto.description}
            badge={<><ShieldCheck size={17} /> Sesión segura</>}
        >
            <div className="summary-grid">
                {tarjetas.map((t) => {
                    const Icono = t.icon
                    return (
                        <article className="summary-card" key={t.label}>
                            <div className="summary-icon"><Icono size={21} /></div>
                            <div><span>{t.label}</span><strong>{t.valor}</strong></div>
                        </article>
                    )
                })}
            </div>
            {aviso && <p className="dashboard-alert" role="status">{aviso}</p>}

            <div className="content-grid">
                {rol === 'ALUMNO' ? (
                    <section className="panel panel-large result-panel">
                        <div className="panel-heading"><div><span className="panel-kicker">TUS RESULTADOS</span><h2>Mi nota</h2></div><Award size={21} /></div>
                        <div className="result-placeholder">
                            <span>{publicada ? 'Nota final' : 'Resultado pendiente'}</span>
                            <strong>{publicada?.nota?.notaFinal != null ? Number(publicada.nota.notaFinal).toFixed(2) : '—'}</strong>
                            <p>{publicada ? publicada.titulo : 'Cuando el docente publique el resultado aparecerá aquí.'}</p>
                        </div>
                    </section>
                ) : rol === 'ADMIN' ? (
                    <section className="panel panel-large">
                        <div className="panel-heading"><div><span className="panel-kicker">ESTADO</span><h2>Gestión del sistema</h2></div></div>
                        <div className="empty-state"><CheckCircle2 size={30} /><strong>Todo listo para trabajar</strong><p>Usa el menú para gestionar la información.</p></div>
                    </section>
                ) : (
                    <section className="panel panel-large">
                        <div className="panel-heading"><div><span className="panel-kicker">POR CALIFICAR</span><h2>{pendientes.length} proyectos pendientes</h2></div></div>
                        {pendientes.length === 0 ? (
                            <div className="empty-state"><CheckCircle2 size={30} /><strong>No tienes evaluaciones pendientes</strong></div>
                        ) : (
                            <div className="target-list">
                                {pendientes.slice(0, 6).map((p) => (
                                    <button key={p.proyectoId} className="target-item" onClick={() => navigate(`/evaluacion/calificar?proyecto=${p.proyectoId}`)}>
                                        <FileText size={18} /><span><strong>{p.titulo}</strong><small>{p.grupo}</small></span>
                                    </button>
                                ))}
                            </div>
                        )}
                    </section>
                )}

                <section className="panel">
                    <div className="panel-heading"><div><span className="panel-kicker">ACCESOS RÁPIDOS</span><h2>Acciones frecuentes</h2></div></div>
                    <div className="quick-actions">
                        {accesos[rol].map((a) => {
                            const Icono = a.icon
                            return <button key={a.to} onClick={() => navigate(a.to)}><Icono size={18} /> {a.label}</button>
                        })}
                    </div>
                </section>
            </div>
        </Shell>
    )
}
