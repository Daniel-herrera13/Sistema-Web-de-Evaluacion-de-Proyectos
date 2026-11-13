import { useEffect, useState } from 'react'
import api from '../api/axios'
import Shell from '../components/Shell'
import { mensajeError } from '../utils/errores'

type Row = Record<string, any>
const fmt = (n: any) => (n === null || n === undefined ? null : Number(n).toFixed(2))

export default function MiNota() {
    const [items, setItems] = useState<Row[]>([])
    const [error, setError] = useState('')
    const [cargado, setCargado] = useState(false)

    useEffect(() => {
        api.get('/resultados/mi-nota')
            .then((r) => setItems(Array.isArray(r.data) ? r.data : []))
            .catch((e) => setError(mensajeError(e, 'No fue posible cargar tu nota.')))
            .finally(() => setCargado(true))
    }, [])

    return (
        <Shell eyebrow="MI RESULTADO" title="Mi nota" description="Tu nota se muestra cuando el docente publique los resultados.">
            {error && <p className="dashboard-alert">{error}</p>}
            {cargado && items.length === 0 && !error && (
                <section className="panel"><div className="empty-state"><strong>No tienes proyectos asignados</strong></div></section>
            )}
            <div className="mod-stack">
                {items.map((it) => {
                    const nota = it.nota as Row | null
                    const filas: [string, string | null][] = nota
                        ? [['Docente o jurado', fmt(nota.notaDocenteJurado)], ['Autoevaluación', fmt(nota.notaAutoevaluacion)], ['Público', fmt(nota.notaPublico)]]
                        : []
                    return (
                        <section className="panel compacto nota-card" key={it.proyectoId}>
                            <span className="panel-kicker">{it.titulo}</span>
                            {!it.publicado || !nota ? (
                                <>
                                    <strong className="nota-grande pendiente">—</strong>
                                    <span className="tag tag-warn">Resultado pendiente</span>
                                    <p className="nota-info">Cuando el docente publique los resultados aparecerán aquí.</p>
                                </>
                            ) : (
                                <>
                                    <small>Nota final</small>
                                    <strong className="nota-grande">{fmt(nota.notaFinal) ?? '—'}</strong>
                                    {nota.definitiva && <span className="tag tag-ok">Nota definitiva</span>}
                                    <div className="desglose">
                                        {filas.filter(([, v]) => v !== null).map(([label, v]) => (
                                            <div key={label}><span>{label}</span><strong>{v}</strong></div>
                                        ))}
                                    </div>
                                </>
                            )}
                        </section>
                    )
                })}
            </div>
        </Shell>
    )
}