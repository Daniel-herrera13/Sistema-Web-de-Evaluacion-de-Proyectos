import { useEffect, useState, type FormEvent } from 'react'
import { CheckCircle2, Circle, ShieldCheck } from 'lucide-react'
import api from '../api/axios'
import Shell from '../components/Shell'
import { mensajeError } from '../utils/errores'

type Row = Record<string, any>
const lista = (d: any): Row[] => (Array.isArray(d) ? d : d?.content ?? [])

export default function Autoevaluacion() {
    const [proyectos, setProyectos] = useState<Row[]>([])
    const [proyectoId, setProyectoId] = useState<number | null>(null)
    const [form, setForm] = useState<Row | null>(null)
    const [objetivoId, setObjetivoId] = useState<number | null>(null)
    const [notas, setNotas] = useState<Record<number, string>>({})
    const [error, setError] = useState('')
    const [aviso, setAviso] = useState('')

    const elegir = (o: Row) => {
        setObjetivoId(o.alumnoId)
        setNotas(Object.fromEntries((o.notas ?? []).map((n: Row) => [n.criterioId, String(n.nota)])))
    }

    const abrir = async (id: number) => {
        setProyectoId(id); setError(''); setAviso(''); setForm(null); setObjetivoId(null)
        try {
            const { data } = await api.get(`/autoevaluacion/proyecto/${id}`)
            setForm(data)
            const primero = (data.objetivos ?? [])[0]
            if (primero) elegir(primero)
        } catch (e) { setError(mensajeError(e, 'No fue posible cargar la autoevaluación.')) }
    }

    useEffect(() => {
        api.get('/proyectos')
            .then((r) => {
                const l = lista(r.data)
                setProyectos(l)
                if (l.length === 1) void abrir(l[0].id)
            })
            .catch((e) => setError(mensajeError(e, 'No fue posible cargar tu proyecto.')))
    }, [])

    const guardar = async (event: FormEvent) => {
        event.preventDefault()
        if (!form || objetivoId === null) return
        const criterios: Row[] = form.criterios ?? []
        if (criterios.some((c) => notas[c.id] === undefined || notas[c.id] === '')) {
            setError('Debes calificar todos los criterios.'); return
        }
        if (criterios.some((c) => { const n = Number(notas[c.id]); return Number.isNaN(n) || n < 0 || n > 10 })) {
            setError('Las notas deben estar entre 0 y 10.'); return
        }
        setError(''); setAviso('')
        try {
            const { data } = await api.put(`/autoevaluacion/proyecto/${proyectoId}`, {
                evaluadoId: objetivoId,
                notas: criterios.map((c) => ({ criterioId: c.id, nota: Number(notas[c.id]) })),
            })
            setForm(data)
            setAviso('Calificación guardada.')
            const siguiente = ((data.objetivos ?? []) as Row[]).find((o) => !o.evaluado)
            if (siguiente) elegir(siguiente)
        } catch (e) { setError(mensajeError(e, 'No se pudo guardar la calificación.')) }
    }

    const objetivos: Row[] = form?.objetivos ?? []
    const hechos = objetivos.filter((o) => o.evaluado).length
    const porcentaje = objetivos.length ? Math.round((hechos / objetivos.length) * 100) : 0
    const actual = objetivos.find((o) => o.alumnoId === objetivoId)

    return (
        <Shell eyebrow="MI GRUPO" title="Autoevaluación" description="Califica tu aporte y el de tus compañeros. Nadie verá quién puso cada nota.">
            {error && <p className="dashboard-alert">{error}</p>}
            {aviso && <p className="dashboard-success">{aviso}</p>}

            {proyectos.length > 1 && (
                <div className="chip-row">
                    {proyectos.map((p) => (
                        <button key={p.id} className={`preset-btn ${proyectoId === p.id ? 'sel' : ''}`} onClick={() => void abrir(p.id)}>{p.titulo}</button>
                    ))}
                </div>
            )}

            {proyectos.length === 0 && !error && (
                <section className="panel"><div className="empty-state"><strong>No tienes un proyecto asignado</strong><p>Cuando tu docente te agregue a un grupo con proyecto, aparecerá aquí.</p></div></section>
            )}

            {form && (
                <>
                    <section className="panel compacto" style={{ marginBottom: 24 }}>
                        <div className="progress-line"><span>{form.proyecto}</span><strong>{hechos} de {objetivos.length} evaluados</strong></div>
                        <div className="mod-progress"><i style={{ width: `${porcentaje}%` }} /></div>
                        <p className="anon-note"><ShieldCheck size={15} /> Tus calificaciones son anónimas. Solo ves lo que tú mismo registraste.</p>
                        {form.completo && <p className="sum-text ok" style={{ marginTop: 10 }}>Completaste la evaluación de todo tu grupo.</p>}
                    </section>

                    <div className="mod-two">
                        <section className="panel compacto">
                            <div className="panel-heading"><div><span className="panel-kicker">INTEGRANTES</span><h2>A quién evaluar</h2></div></div>
                            <div className="target-list">
                                {objetivos.map((o) => (
                                    <button key={o.alumnoId} className={`target-item ${o.alumnoId === objetivoId ? 'chosen' : ''}`} onClick={() => elegir(o)}>
                                        {o.evaluado ? <CheckCircle2 size={18} className="ok-icon" /> : <Circle size={18} />}
                                        <span><strong>{o.nombre}</strong>{o.propio && <small>Tú (autoevaluación)</small>}</span>
                                    </button>
                                ))}
                            </div>
                        </section>

                        <section className="panel compacto">
                            {actual ? (
                                <form onSubmit={guardar}>
                                    <div className="panel-heading"><div><span className="panel-kicker">{actual.propio ? 'AUTOEVALUACIÓN' : 'COMPAÑERO'}</span><h2>{actual.nombre}</h2></div></div>
                                    {(form.criterios as Row[]).map((c) => (
                                        <label className="note-row" key={c.id}>
                                            <span>{c.nombre}<small>{c.ponderacion}%{c.descripcion ? ` · ${c.descripcion}` : ''}</small></span>
                                            <input type="number" min="0" max="10" step="0.01" required value={notas[c.id] ?? ''} onChange={(e) => setNotas({ ...notas, [c.id]: e.target.value })} />
                                        </label>
                                    ))}
                                    <div className="form-actions" style={{ marginTop: 18 }}>
                                        <button className="primary-button" type="submit"><CheckCircle2 size={16} /> Guardar calificación</button>
                                    </div>
                                </form>
                            ) : (
                                <div className="empty-state"><strong>Selecciona a un compañero</strong></div>
                            )}
                        </section>
                    </div>
                </>
            )}
        </Shell>
    )
}