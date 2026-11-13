import { useEffect, useState } from 'react'
import { Calculator, Send } from 'lucide-react'
import api from '../api/axios'
import Shell from '../components/Shell'
import { mensajeError } from '../utils/errores'

type Row = Record<string, any>
const lista = (d: any): Row[] => (Array.isArray(d) ? d : d?.content ?? [])
const fmt = (n: any) => (n === null || n === undefined ? '—' : Number(n).toFixed(2))
const usa = (peso: any) => Number(peso) > 0

export default function NotasFinales() {
    const [proyectos, setProyectos] = useState<Row[]>([])
    const [sel, setSel] = useState<Row | null>(null)
    const [datos, setDatos] = useState<Row | null>(null)
    const [error, setError] = useState('')
    const [aviso, setAviso] = useState('')

    useEffect(() => {
        api.get('/proyectos').then((r) => setProyectos(lista(r.data))).catch((e) => setError(mensajeError(e, 'No fue posible cargar los proyectos.')))
    }, [])

    const cargar = async (p: Row) => {
        setSel(p); setDatos(null); setError(''); setAviso('')
        try { setDatos((await api.get(`/notas/proyecto/${p.id}`)).data) }
        catch (e) { setError(mensajeError(e, 'No fue posible cargar las notas.')) }
    }

    const cambiarPublicacion = async (publicar: boolean) => {
        if (!sel) return
        setError(''); setAviso('')
        try {
            await api.post(`/proyectos/${sel.id}/${publicar ? 'publicar' : 'despublicar'}`)
            const actualizado = { ...sel, resultadosPublicados: publicar }
            setSel(actualizado)
            setProyectos(proyectos.map((p) => (p.id === sel.id ? actualizado : p)))
            setAviso(publicar ? 'Resultados publicados. Los alumnos ya pueden ver su nota.' : 'Resultados retirados.')
        } catch (e) { setError(mensajeError(e, 'No se pudo cambiar la publicación.')) }
    }

    return (
        <Shell eyebrow="RESULTADOS" title="Notas finales" description="Desglose por alumno según las modalidades que definió el docente para la materia.">
            {error && <p className="dashboard-alert">{error}</p>}
            {aviso && <p className="dashboard-success">{aviso}</p>}
            <div className="mod-two">
                <section className="panel compacto">
                    <div className="panel-heading"><div><span className="panel-kicker">PROYECTOS</span><h2>Selecciona uno</h2></div></div>
                    <div className="target-list">
                        {proyectos.map((p) => (
                            <button key={p.id} className={`target-item ${sel?.id === p.id ? 'chosen' : ''}`} onClick={() => void cargar(p)}>
                                <Calculator size={18} /><span><strong>{p.titulo}</strong><small>{p.grupoNombre}</small></span>
                            </button>
                        ))}
                    </div>
                </section>

                <section className="panel compacto">
                    {!datos && <div className="empty-state"><Calculator size={28} /><strong>Selecciona un proyecto</strong></div>}
                    {datos && sel && (
                        <>
                            <div className="panel-heading">
                                <div><span className="panel-kicker">FÓRMULA</span><h2>{datos.titulo}</h2></div>
                                <span className={`tag ${datos.definitiva ? 'tag-ok' : 'tag-warn'}`}>{datos.definitiva ? 'Definitiva' : 'Pendiente'}</span>
                            </div>
                            <p className="nota-info">
                                {usa(datos.pesoDocenteJurado) && <>Docente o jurado <strong>{datos.pesoDocenteJurado}%</strong> </>}
                                {usa(datos.pesoAutoevaluacion) && <>· Autoevaluación <strong>{datos.pesoAutoevaluacion}%</strong> </>}
                                {usa(datos.pesoPublico) && <>· Público <strong>{datos.pesoPublico}%</strong> ({datos.evaluacionesPublicas} evaluaciones)</>}
                            </p>

                            <div className="table-wrap">
                                <table>
                                    <thead>
                                    <tr>
                                        <th>Alumno</th>
                                        {usa(datos.pesoDocenteJurado) && <th>Docente / jurado</th>}
                                        {usa(datos.pesoAutoevaluacion) && <th>Autoevaluación</th>}
                                        {usa(datos.pesoPublico) && <th>Público</th>}
                                        <th>Nota final</th>
                                        <th>Estado</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {(datos.alumnos as Row[]).map((a) => (
                                        <tr key={a.alumnoId}>
                                            <td>{a.nombre}</td>
                                            {usa(datos.pesoDocenteJurado) && <td>{fmt(a.notaDocenteJurado)}</td>}
                                            {usa(datos.pesoAutoevaluacion) && <td>{fmt(a.notaAutoevaluacion)}</td>}
                                            {usa(datos.pesoPublico) && <td>{fmt(a.notaPublico)}</td>}
                                            <td><strong>{fmt(a.notaFinal)}</strong></td>
                                            <td><span className={`tag ${a.definitiva ? 'tag-ok' : 'tag-warn'}`}>{a.definitiva ? 'Definitiva' : 'Pendiente'}</span></td>
                                        </tr>
                                    ))}
                                    </tbody>
                                </table>
                            </div>

                            {!datos.definitiva && (
                                <p className="aviso-suma">
                                    Aún faltan evaluaciones del esquema.
                                    {usa(datos.pesoPublico) && datos.evaluacionesPublicas === 0 && ' Falta al menos una evaluación pública.'}
                                    {datos.publicoAbierto && ' Cierra la evaluación pública para dar la nota por definitiva.'}
                                </p>
                            )}

                            <div className="form-actions" style={{ marginTop: 18 }}>
                                {sel.resultadosPublicados ? (
                                    <>
                                        <span className="tag tag-ok">Resultados publicados</span>
                                        <button className="secondary-button" onClick={() => void cambiarPublicacion(false)}>Retirar publicación</button>
                                    </>
                                ) : (
                                    <button className="primary-button" disabled={!datos.definitiva} onClick={() => void cambiarPublicacion(true)}><Send size={16} /> Publicar resultados</button>
                                )}
                            </div>
                        </>
                    )}
                </section>
            </div>
        </Shell>
    )
}