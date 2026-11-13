import { useEffect, useState, type FormEvent } from 'react'
import { Navigate, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { CheckCircle2, Download, FileText, Plus, Save, Send, X } from 'lucide-react'
import api from '../api/axios'
import Shell from '../components/Shell'
import { useAuth, type Rol } from '../context/AuthContext'
import { descargar, type Formato } from '../utils/descargas'
import { mensajeError } from '../utils/errores'
import './Evaluaciones.css'

type Mode = 'invitar' | 'pendientes' | 'calificar' | 'resultados'
type Row = Record<string, any>

const modos: Record<Mode, { title: string; description: string; roles: Rol[] }> = {
    invitar: { title: 'Invitar evaluadores', description: 'Asigna jurado o docentes a un proyecto y consulta sus invitaciones.', roles: ['ADMIN', 'DOCENTE'] },
    pendientes: { title: 'Proyectos asignados', description: 'Proyectos que tienes pendientes de evaluar.', roles: ['DOCENTE', 'JURADO'] },
    calificar: { title: 'Formulario de calificación', description: 'Registra la nota de cada criterio. Puedes guardar un borrador antes de enviar.', roles: ['DOCENTE', 'JURADO'] },
    resultados: { title: 'Resultados por evaluador', description: 'Consulta la nota de cada evaluador y publica los resultados.', roles: ['ADMIN', 'DOCENTE'] },
}
const estadoTexto: Record<string, string> = { SIN_INICIAR: 'Sin iniciar', BORRADOR: 'Borrador', ENVIADA: 'Enviada' }
const list = (data: any): Row[] => (Array.isArray(data) ? data : data?.content ?? [])
const fmt = (n: any) => (n === null || n === undefined ? '—' : Number(n).toFixed(2))

export default function Evaluaciones() {
    const { modo = 'pendientes' } = useParams()
    const { usuario } = useAuth()
    if (modo === 'reportes') return <Navigate to="/reportes" replace />
    if (!(modo in modos)) return <Navigate to="/dashboard" replace />
    const mode = modo as Mode
    if (!usuario || !modos[mode].roles.includes(usuario.rol)) return <Navigate to="/dashboard" replace />
    return <Contenido key={mode} mode={mode} />
}

function Contenido({ mode }: { mode: Mode }) {
    const navigate = useNavigate()
    const [params] = useSearchParams()
    const info = modos[mode]

    const [projects, setProjects] = useState<Row[]>([])
    const [selected, setSelected] = useState<Row | null>(null)
    const [message, setMessage] = useState('')
    const [error, setError] = useState('')
    // invitar
    const [users, setUsers] = useState<Row[]>([])
    const [evaluators, setEvaluators] = useState<Row[]>([])
    // calificar
    const [form, setForm] = useState<Row | null>(null)
    const [descripciones, setDescripciones] = useState<Record<number, string>>({})
    const [notes, setNotes] = useState<Record<number, string>>({})
    const [comments, setComments] = useState('')
    // resultados
    const [result, setResult] = useState<Row | null>(null)

    const cargarProyectos = async () => {
        try {
            if (mode === 'pendientes' || mode === 'calificar') {
                const { data } = await api.get('/invitaciones/mias')
                setProjects(list(data).map((r) => ({ id: r.proyectoId, titulo: r.titulo, grupoNombre: r.grupo, estadoEvaluacion: r.estadoEvaluacion })))
            } else {
                setProjects(list((await api.get('/proyectos')).data))
            }
        } catch (e) { setError(mensajeError(e, 'No fue posible cargar los proyectos.')) }
    }

    const aplicarEvaluacion = (data: Row) => {
        setForm(data)
        setNotes(Object.fromEntries((data.detalles ?? []).map((d: Row) => [d.criterioId, d.nota ?? ''])))
        setComments(data.comentarios ?? '')
    }

    const elegir = async (project: Row) => {
        setSelected(project); setError(''); setMessage(''); setResult(null); setForm(null)
        try {
            if (mode === 'invitar') {
                const [candidatos, invitados] = await Promise.all([
                    api.get('/invitaciones/candidatos').catch(() => api.get('/usuarios')),
                    api.get('/invitaciones', { params: { proyectoId: project.id } }),
                ])
                setUsers(list(candidatos.data).filter((x) => ['DOCENTE', 'JURADO'].includes(String(x.rol ?? '').toUpperCase())))
                setEvaluators(list(invitados.data))
            } else if (mode === 'calificar') {
                const [mia, efectivos] = await Promise.all([
                    api.get(`/evaluaciones/proyecto/${project.id}/mia`),
                    api.get('/criterios/efectivos', { params: { proyectoId: project.id } }).catch(() => null),
                ])
                setDescripciones(Object.fromEntries(list(efectivos?.data?.criterios).map((c) => [c.id, c.descripcion ?? ''])))
                aplicarEvaluacion(mia.data)
            } else if (mode === 'resultados') {
                setResult((await api.get(`/reportes/proyectos/${project.id}`)).data)
            }
        } catch (e) { setError(mensajeError(e, 'No fue posible cargar el detalle seleccionado.')) }
    }

    useEffect(() => { void cargarProyectos() }, [])

    // Si se llegó desde "pendientes" (?proyecto=ID), se abre ese proyecto
    useEffect(() => {
        const pre = Number(params.get('proyecto'))
        if (mode !== 'calificar' || !pre || selected || projects.length === 0) return
        const encontrado = projects.find((p) => p.id === pre)
        if (encontrado) void elegir(encontrado)
    }, [projects])

    // ---------- invitar ----------
    const invitar = async (evaluadorId: number) => {
        if (!selected) return
        setError(''); setMessage('')
        try {
            await api.post('/invitaciones', { proyectoId: selected.id, evaluadorId })
            setMessage('Evaluador invitado correctamente.')
            await elegir(selected)
        } catch (e) { setError(mensajeError(e, 'No se pudo enviar la invitación.')) }
    }
    const quitar = async (row: Row) => {
        if (!selected) return
        setError(''); setMessage('')
        try {
            await api.delete(`/invitaciones/${row.id}`)
            setMessage('Invitación eliminada.')
            await elegir(selected)
        } catch (e) { setError(mensajeError(e, 'No se pudo quitar la invitación.')) }
    }

    // ---------- calificar ----------
    const detalles: Row[] = form?.detalles ?? []
    const enviada = form?.estado === 'ENVIADA'
    const vacia = (id: number) => notes[id] === undefined || notes[id] === ''
    const itemsNotas = (soloLlenas: boolean) =>
        detalles.filter((d) => !soloLlenas || !vacia(d.criterioId)).map((d) => ({ criterioId: d.criterioId, nota: Number(notes[d.criterioId]) }))
    const enRango = (items: { nota: number }[]) => items.every((i) => !Number.isNaN(i.nota) && i.nota >= 0 && i.nota <= 10)

    const guardarBorrador = async () => {
        if (!selected) return
        const items = itemsNotas(true)
        if (!enRango(items)) { setError('Las notas deben estar entre 0 y 10.'); return }
        setError(''); setMessage('')
        try {
            const { data } = await api.put(`/evaluaciones/proyecto/${selected.id}/borrador`, { notas: items, comentarios: comments })
            aplicarEvaluacion(data)
            setMessage('Borrador guardado.')
            await cargarProyectos()
        } catch (e) { setError(mensajeError(e, 'No se pudo guardar el borrador.')) }
    }

    const enviar = async (event: FormEvent) => {
        event.preventDefault()
        if (!selected) return
        if (detalles.some((d) => vacia(d.criterioId))) { setError('Debes calificar todos los criterios.'); return }
        const items = itemsNotas(false)
        if (!enRango(items)) { setError('Las notas deben estar entre 0 y 10.'); return }
        setError(''); setMessage('')
        try {
            const { data } = await api.post(`/evaluaciones/proyecto/${selected.id}/enviar`, { notas: items, comentarios: comments })
            aplicarEvaluacion(data)
            setMessage('Calificación enviada correctamente.')
            await cargarProyectos()
        } catch (e) { setError(mensajeError(e, 'No se pudo enviar la calificación.')) }
    }

    const reabrir = async () => {
        if (!form?.id) return
        setError(''); setMessage('')
        try {
            const { data } = await api.post(`/evaluaciones/${form.id}/reabrir`)
            aplicarEvaluacion(data)
            setMessage('Evaluación reabierta. Puedes editarla y volver a enviarla.')
            await cargarProyectos()
        } catch (e) { setError(mensajeError(e, 'No se pudo reabrir la evaluación.')) }
    }

    // ---------- resultados ----------
    const publicar = async (publicar: boolean) => {
        if (!selected) return
        setError(''); setMessage('')
        try {
            await api.post(`/proyectos/${selected.id}/${publicar ? 'publicar' : 'despublicar'}`)
            setMessage(publicar ? 'Resultados publicados.' : 'Resultados retirados.')
            await elegir(selected)
        } catch (e) { setError(mensajeError(e, 'No se pudo cambiar la publicación.')) }
    }
    const exportar = async (formato: Formato) => {
        if (!selected) return
        try { await descargar(`/reportes/proyectos/${selected.id}/exportar`, {}, `proyecto-${selected.id}`, formato) }
        catch (e) { setError(mensajeError(e, 'No se pudo descargar el archivo.')) }
    }

    const pendientes = projects.filter((p) => p.estadoEvaluacion !== 'ENVIADA')
    const disponibles = users.filter((x) => !evaluators.some((e) => e.evaluadorId === x.id))

    return (
        <Shell eyebrow="MÓDULO DE EVALUACIÓN" title={info.title} description={info.description}>
            {error && <p className="dashboard-alert">{error}</p>}
            {message && <p className="dashboard-success">{message}</p>}

            {mode === 'pendientes' && (
                <section className="panel evaluation-panel">
                    <div className="panel-heading"><div><span className="panel-kicker">LISTADO</span><h2>{pendientes.length} proyectos pendientes</h2></div></div>
                    <div className="evaluation-list">
                        {pendientes.map((row) => (
                            <button className="evaluation-item" key={row.id} onClick={() => navigate(`/evaluacion/calificar?proyecto=${row.id}`)}>
                                <FileText size={20} />
                                <span><strong>{row.titulo}</strong><small>{row.grupoNombre} · {estadoTexto[row.estadoEvaluacion] ?? row.estadoEvaluacion}</small></span>
                                <Plus size={16} />
                            </button>
                        ))}
                    </div>
                    {pendientes.length === 0 && <div className="empty-state"><CheckCircle2 size={28} /><strong>No tienes evaluaciones pendientes</strong></div>}
                </section>
            )}

            {mode !== 'pendientes' && (
                <div className="evaluation-layout">
                    <section className="panel">
                        <div className="panel-heading"><div><span className="panel-kicker">PROYECTOS</span><h2>Selecciona un proyecto</h2></div></div>
                        <div className="evaluation-list">
                            {projects.map((row) => (
                                <button className={`evaluation-item ${selected?.id === row.id ? 'chosen' : ''}`} key={row.id} onClick={() => void elegir(row)}>
                                    <FileText size={19} />
                                    <span>
                                        <strong>{row.titulo}</strong>
                                        <small>{row.grupoNombre}{mode === 'calificar' ? ` · ${estadoTexto[row.estadoEvaluacion] ?? row.estadoEvaluacion}` : ''}</small>
                                    </span>
                                </button>
                            ))}
                        </div>
                        {projects.length === 0 && <div className="empty-state"><strong>No hay proyectos para mostrar</strong></div>}
                    </section>

                    <section className="panel evaluation-detail">
                        {!selected && (
                            <div className="empty-state"><FileText size={30} /><strong>Selecciona un proyecto</strong><p>La información aparecerá aquí.</p></div>
                        )}

                        {mode === 'invitar' && selected && (
                            <>
                                <div className="panel-heading"><h2>Evaluadores invitados</h2></div>
                                {evaluators.length === 0 && <p className="nota-info">Todavía no hay evaluadores invitados.</p>}
                                {evaluators.map((row) => (
                                    <div className="person-row" key={row.id}>
                                        <span>{row.nombre} {row.apellido}<small>{row.correo} · {row.rol}</small></span>
                                        <button title="Quitar invitación" onClick={() => void quitar(row)}><X size={15} /></button>
                                    </div>
                                ))}
                                <h3>Disponibles</h3>
                                {disponibles.length === 0 && <p className="nota-info">No hay más docentes o jurado para invitar.</p>}
                                {disponibles.map((row) => (
                                    <button className="available-row" key={row.id} onClick={() => void invitar(row.id)}>
                                        <span>{row.nombre} {row.apellido}<small>{row.correo} · {row.rol}</small></span><Send size={15} />
                                    </button>
                                ))}
                            </>
                        )}

                        {mode === 'calificar' && selected && form && (
                            <form onSubmit={enviar}>
                                <div className="panel-heading">
                                    <div><span className="panel-kicker">{estadoTexto[form.estado] ?? form.estado}</span><h2>{selected.titulo}</h2></div>
                                    {enviada && <span className="tag tag-ok">Nota: {fmt(form.calificacionFinal)}</span>}
                                </div>
                                {detalles.map((d) => (
                                    <label className="note-field" key={d.criterioId}>
                                        <span>{d.criterio}<small>{d.ponderacion}%{descripciones[d.criterioId] ? ` · ${descripciones[d.criterioId]}` : ''}</small></span>
                                        <input
                                            type="number" min="0" max="10" step="0.01" required disabled={enviada}
                                            value={notes[d.criterioId] ?? ''}
                                            onChange={(e) => setNotes({ ...notes, [d.criterioId]: e.target.value })}
                                        />
                                    </label>
                                ))}
                                <textarea placeholder="Comentarios de la evaluación" disabled={enviada} value={comments} onChange={(e) => setComments(e.target.value)} />
                                <div className="form-actions">
                                    {enviada ? (
                                        <button type="button" className="secondary-button" onClick={() => void reabrir()}>Reabrir evaluación</button>
                                    ) : (
                                        <>
                                            <button type="button" className="secondary-button" onClick={() => void guardarBorrador()}><Save size={16} /> Guardar borrador</button>
                                            <button type="submit" className="primary-button"><CheckCircle2 size={16} /> Enviar calificación</button>
                                        </>
                                    )}
                                </div>
                            </form>
                        )}

                        {mode === 'resultados' && selected && result && (
                            <>
                                <div className="panel-heading">
                                    <div><span className="panel-kicker">DESGLOSE</span><h2>{result.titulo}</h2></div>
                                    <div className="export-actions">
                                        <button className="secondary-button" onClick={() => void exportar('pdf')}><Download size={15} /> PDF</button>
                                        <button className="secondary-button" onClick={() => void exportar('xlsx')}><Download size={15} /> Excel</button>
                                    </div>
                                </div>
                                <div className="result-metrics">
                                    <strong>{fmt(result.notaFinal)}<small>Nota de docente / jurado</small></strong>
                                    <strong>{result.invitados}<small>Invitados</small></strong>
                                    <strong>{result.enviadas}<small>Enviadas</small></strong>
                                </div>
                                {(result.evaluadores ?? []).map((e: Row) => (
                                    <div className="person-row" key={e.evaluadorId}>
                                        <span>{e.nombre}<small>Nota: {fmt(e.nota)} · {estadoTexto[e.estado] ?? e.estado}</small></span>
                                        <span>{e.comentario ?? ''}</span>
                                    </div>
                                ))}
                                <p className="nota-info">La nota final con autoevaluación y público está en <strong>Notas finales</strong>.</p>
                                {result.publicado ? (
                                    <>
                                        <p className="published-label">Resultados publicados</p>
                                        <button className="secondary-button" onClick={() => void publicar(false)}>Retirar publicación</button>
                                    </>
                                ) : (
                                    <button className="primary-button publish-button" onClick={() => void publicar(true)}><Send size={16} /> Publicar resultados</button>
                                )}
                            </>
                        )}
                    </section>
                </div>
            )}
        </Shell>
    )
}