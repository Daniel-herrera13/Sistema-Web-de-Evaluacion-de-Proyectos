import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, BarChart3, CheckCircle2, ClipboardCheck, Download, FileText, GraduationCap, Plus, Send, Users, X } from 'lucide-react'
import api from '../api/axios'
import { useAuth } from '../context/AuthContext'
import './Evaluaciones.css'

type Mode = 'invitar' | 'pendientes' | 'calificar' | 'resultados' | 'reportes'
type Row = Record<string, any>
const labels: Record<Mode, { title: string; description: string; icon: typeof Users }> = {
    invitar: { title: 'Invitar evaluadores', description: 'Asigna evaluadores a los proyectos y consulta sus invitaciones.', icon: Users },
    pendientes: { title: 'Evaluaciones pendientes', description: 'Revisa los proyectos que tienes pendientes de evaluar.', icon: ClipboardCheck },
    calificar: { title: 'Formulario de calificación', description: 'Registra la nota de cada criterio y tus comentarios.', icon: CheckCircle2 },
    resultados: { title: 'Resultados y nota final', description: 'Consulta el desglose de notas y publica los resultados.', icon: BarChart3 },
    reportes: { title: 'Reportes', description: 'Consulta resultados y descarga reportes del proceso.', icon: Download },
}
const list = (data: any): Row[] => Array.isArray(data) ? data : data?.content ?? []

export default function Evaluaciones() {
    const { modo = 'pendientes' } = useParams<{ modo: Mode }>()
    const mode = (Object.keys(labels).includes(modo) ? modo : 'pendientes') as Mode
    const navigate = useNavigate()
    const { usuario, logout } = useAuth()
    const [projects, setProjects] = useState<Row[]>([])
    const [users, setUsers] = useState<Row[]>([])
    const [selected, setSelected] = useState<Row | null>(null)
    const [evaluators, setEvaluators] = useState<Row[]>([])
    const [pending, setPending] = useState<Row[]>([])
    const [criteria, setCriteria] = useState<Row[]>([])
    const [result, setResult] = useState<Row | null>(null)
    const [notes, setNotes] = useState<Record<number, string>>({})
    const [comments, setComments] = useState('')
    const [message, setMessage] = useState('')
    const [error, setError] = useState('')
    const title = labels[mode]
    const Icon = title.icon
    const initials = useMemo(() => (usuario?.nombre ?? 'Usuario').split(' ').map((x) => x[0]).slice(0, 2).join('').toUpperCase(), [usuario?.nombre])

    const loadProjects = async () => {
        try {
            const data = (await api.get('/proyectos')).data
            setProjects(list(data))
        } catch { setError('No fue posible cargar los proyectos.') }
    }
    useEffect(() => {
        setSelected(null); setResult(null); setError(''); setMessage('')
        void loadProjects()
        if (mode === 'pendientes') {
            api.get('/invitaciones/mias')
                .then((r) => setPending(list(r.data).filter((row) => row.estadoEvaluacion !== 'ENVIADA')))
                .catch(() => setError('No fue posible cargar tus pendientes.'))
        }
    }, [mode])
    const chooseProject = async (project: Row) => {
        setSelected(project); setError(''); setMessage('')
        try {
            if (mode === 'invitar') {
                const [people, invited] = await Promise.all([api.get('/usuarios'), api.get(`/invitaciones?proyectoId=${project.id}`)])
                setUsers(list(people.data).filter((x) => ['DOCENTE', 'JURADO', 'docente', 'jurado'].includes(x.rol)))
                setEvaluators(list(invited.data))
            } else if (mode === 'calificar') {
                const [criteriaResponse, evaluationResponse] = await Promise.all([
                    api.get(`/criterios/efectivos?proyectoId=${project.id}`),
                    api.get(`/evaluaciones/proyecto/${project.id}/mia`),
                ])
                setCriteria(list(criteriaResponse.data?.criterios ?? criteriaResponse.data))
                const evaluation = evaluationResponse.data
                setNotes(Object.fromEntries((evaluation.detalles ?? []).map((item: Row) => [item.criterioId, item.nota ?? ''])))
                setComments(evaluation.comentarios ?? '')
            } else if (mode === 'resultados' || mode === 'reportes') {
                setResult((await api.get(`/reportes/proyectos/${project.id}`)).data)
            }
        } catch { setError('No fue posible cargar el detalle seleccionado.') }
    }
    const invite = async (evaluadorId: number) => {
        if (!selected) return
        try {
            await api.post('/invitaciones', { proyectoId: selected.id, evaluadorId })
            setMessage('Evaluador invitado correctamente.')
            await chooseProject(selected)
        } catch { setError('No se pudo enviar la invitación.') }
    }
    const submitEvaluation = async (event: FormEvent) => {
        event.preventDefault(); if (!selected) return
        try {
            await api.post(`/evaluaciones/proyecto/${selected.id}/enviar`, {
                notas: criteria.map((c) => ({ criterioId: c.id, nota: Number(notes[c.id] ?? 0) })),
                comentarios: comments,
            })
            setMessage('Calificación guardada correctamente.'); setSelected(null)
        } catch { setError('No se pudo guardar la calificación.') }
    }
    const publish = async () => {     if (!selected) return
    try {
        await api.post(`/proyectos/${selected.id}/publicar`)
        setMessage('Resultados publicados.')
        if (mode === 'resultados') await chooseProject(selected)
    } catch { setError('No se pudieron publicar los resultados.') } }
    const download = () => {
        if (!result) return
        const rows = [['Proyecto', 'Nota final', 'Evaluadores', 'Evaluaciones enviadas', 'Publicado'], [result.titulo, result.notaFinal ?? '', result.invitados, result.enviadas, result.publicado ? 'Sí' : 'No']]
        const blob = new Blob([rows.map((row) => row.join(',')).join('\n')], { type: 'text/csv;charset=utf-8' })
        const url = URL.createObjectURL(blob); const link = document.createElement('a'); link.href = url; link.download = `reporte-${result.proyectoId}.csv`; link.click(); URL.revokeObjectURL(url)
    }
    const nav = [['invitar', 'Invitar evaluadores'], ['pendientes', 'Pendientes'], ['calificar', 'Calificar'], ['resultados', 'Resultados'], ['reportes', 'Reportes']]
    return <div className="app-shell evaluation-shell"><aside className="sidebar"><div className="sidebar-top"><div className="brand"><div className="brand-mark"><GraduationCap size={24} /></div><div className="brand-text"><strong>Sistema Web de<br />Evaluación</strong><span>Regional Santa Ana · Ciclo 02–2026</span></div></div><nav><p className="nav-heading">EVALUACIÓN</p>{nav.map(([key, text]) => <button className={`nav-item ${key === mode ? 'active' : ''}`} key={key} onClick={() => navigate(`/evaluacion/${key}`)}><Icon size={18} /><span>{text}</span></button>)}</nav></div><div className="sidebar-bottom"><button className="logout-button" onClick={() => { logout(); navigate('/login') }}>Cerrar sesión</button><strong>ITCA-FEPADE</strong></div></aside>
        <main className="dashboard-main"><header className="topbar"><button className="back-button" onClick={() => navigate('/dashboard')}><ArrowLeft size={18} /> Dashboard</button><div className="topbar-title">{title.title}</div><div className="user-chip"><span className="avatar">{initials}</span><span>{usuario?.rol ?? 'Usuario'}</span></div></header><section className="dashboard-content"><div className="page-heading"><div><span className="page-eyebrow">MÓDULO DE EVALUACIÓN</span><h1>{title.title}</h1><p>{title.description}</p></div><div className="security-badge"><Icon size={16} /> Conectado a la API</div></div>{error && <p className="dashboard-alert">{error}</p>}{message && <p className="dashboard-success">{message}</p>}
            {mode === 'pendientes' && <section className="panel evaluation-panel"><div className="panel-heading"><div><span className="panel-kicker">LISTADO</span><h2>{pending.length} proyectos pendientes</h2></div></div><div className="evaluation-list">{pending.map((row) => <button className="evaluation-item" key={row.proyectoId} onClick={() => { setSelected(row); navigate('/evaluacion/calificar') }}><FileText size={20} /><span><strong>{row.proyecto}</strong><small>{row.grupo}</small></span><Plus size={16} /></button>)}</div>{pending.length === 0 && <div className="empty-state"><CheckCircle2 size={28} /><strong>No tienes evaluaciones pendientes</strong></div>}</section>}
            {['invitar', 'calificar', 'resultados', 'reportes'].includes(mode) && <div className="evaluation-layout"><section className="panel"><div className="panel-heading"><div><span className="panel-kicker">PROYECTOS</span><h2>Selecciona un proyecto</h2></div></div><div className="evaluation-list">{projects.map((row) => <button className={`evaluation-item ${selected?.id === row.id ? 'chosen' : ''}`} key={row.id} onClick={() => chooseProject(row)}><FileText size={19} /><span><strong>{row.titulo}</strong><small>{row.grupoNombre}</small></span></button>)}</div></section><section className="panel evaluation-detail">{!selected && !result && <div className="empty-state"><Icon size={30} /><strong>Selecciona un proyecto</strong><p>La información aparecerá aquí.</p></div>}
                {mode === 'invitar' && selected && <><div className="panel-heading"><h2>Evaluadores invitados</h2></div>{evaluators.map((row) => <div className="person-row" key={row.id}><span>{row.nombre} {row.apellido}<small>{row.correo}</small></span><button onClick={async () => { await api.delete(`/invitaciones/${row.id}`); await chooseProject(selected) }}><X size={15} /></button></div>)}<h3>Disponibles</h3>{users.filter((x) => !evaluators.some((e) => e.evaluadorId === x.id)).map((row) => <button className="available-row" key={row.id} onClick={() => invite(row.id)}><span>{row.nombre} {row.apellido}<small>{row.correo}</small></span><Send size={15} /></button>)}</>}
                {mode === 'calificar' && selected && <form onSubmit={submitEvaluation}><div className="panel-heading"><h2>{selected.titulo}</h2></div>{criteria.map((criterion) => <label className="note-field" key={criterion.id}><span>{criterion.nombre}<small>{criterion.ponderacion}% · {criterion.descripcion}</small></span><input type="number" min="0" max="10" step="0.01" required value={notes[criterion.id] ?? ''} onChange={(e) => setNotes({ ...notes, [criterion.id]: e.target.value })} /></label>)}<textarea placeholder="Comentarios de la evaluación" value={comments} onChange={(e) => setComments(e.target.value)} /><button className="primary-button" type="submit"><CheckCircle2 size={16} /> Enviar calificación</button></form>}
                {(mode === 'resultados' || mode === 'reportes') && result && <><div className="panel-heading"><div><span className="panel-kicker">DESGLOSE</span><h2>{result.titulo}</h2></div><button className="primary-button" onClick={download}><Download size={16} /> Descargar CSV</button></div><div className="result-metrics"><strong>{result.notaFinal ?? '—'}<small>Nota final</small></strong><strong>{result.invitados}<small>Invitados</small></strong><strong>{result.enviadas}<small>Enviadas</small></strong></div>{result.evaluadores?.map((evaluation: Row) => <div className="person-row" key={evaluation.evaluadorId}><span>{evaluation.nombre}<small>Nota: {evaluation.nota ?? '—'} · {evaluation.estado}</small></span><span>{evaluation.comentario ?? ''}</span></div>)}{mode === 'resultados' && !result.publicado && <button className="primary-button publish-button" onClick={publish}><Send size={16} /> Publicar resultados</button>}{result.publicado && <p className="published-label">Resultados publicados</p>}</>}</section></div>}
        </section></main></div>
}
