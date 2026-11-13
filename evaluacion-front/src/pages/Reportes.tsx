import { useEffect, useState } from 'react'
import { Download } from 'lucide-react'
import api from '../api/axios'
import Shell from '../components/Shell'
import EstadisticasReporte from '../components/estadisticas/EstadisticasReporte'
import { descargar, limpiar, type Formato } from '../utils/descargas'
import { mensajeError } from '../utils/errores'
import type { FiltrosEstadisticas } from '../types/estadisticas'

type Row = Record<string, any>
const lista = (d: any): Row[] => (Array.isArray(d) ? d : d?.content ?? [])
const fmt = (n: any) => (n === null || n === undefined ? '—' : Number(n).toFixed(2))
const estados: Record<string, string> = { SIN_INICIAR: 'Sin iniciar', BORRADOR: 'Borrador', ENVIADA: 'Enviada' }

export default function Reportes() {
    const [materias, setMaterias] = useState<Row[]>([])
    const [filtros, setFiltros] = useState({ materiaId: '', estado: '', ciclo: '', publicado: '' })
    const [filas, setFilas] = useState<Row[]>([])
    const [detalle, setDetalle] = useState<Row | null>(null)
    const [error, setError] = useState('')
    const [cargando, setCargando] = useState(true)

    useEffect(() => {
        api.get('/materias').then((r) => setMaterias(lista(r.data))).catch(() => undefined)
    }, [])

    useEffect(() => {
        let activo = true
        setCargando(true)
        api.get('/reportes/proyectos', { params: limpiar(filtros) })
            .then((r) => { if (activo) { setFilas(lista(r.data)); setError('') } })
            .catch((e) => { if (activo) setError(mensajeError(e, 'No fue posible cargar el reporte.')) })
            .finally(() => { if (activo) setCargando(false) })
        return () => { activo = false }
    }, [filtros])

    const cambiar = (clave: keyof typeof filtros, valor: string) => setFiltros({ ...filtros, [clave]: valor })
    const filtrosEstadisticas: FiltrosEstadisticas = {
        materiaId: filtros.materiaId ? Number(filtros.materiaId) : undefined,
        estado: filtros.estado || undefined,
        ciclo: filtros.ciclo || undefined,
        publicado: filtros.publicado === '' ? undefined : filtros.publicado === 'true',
    }

    const abrir = async (id: number) => {
        setError('')
        try { setDetalle((await api.get(`/reportes/proyectos/${id}`)).data) }
        catch (e) { setError(mensajeError(e, 'No fue posible cargar el detalle.')) }
    }

    const exportar = async (url: string, params: Record<string, unknown>, nombre: string, formato: Formato) => {
        setError('')
        try { await descargar(url, params, nombre, formato) }
        catch (e) { setError(mensajeError(e, 'No se pudo descargar el archivo.')) }
    }

    return (
        <Shell eyebrow="RESULTADOS" title="Reportes" description="Filtra los proyectos, consulta el detalle y descarga el reporte en PDF o Excel.">
            {error && <p className="dashboard-alert">{error}</p>}

            <section className="panel compacto">
                <div className="panel-heading">
                    <div><span className="panel-kicker">FILTROS</span><h2>{filas.length} proyectos</h2></div>
                    <div className="export-actions">
                        <button className="secondary-button" onClick={() => void exportar('/reportes/proyectos/exportar', limpiar(filtros), 'reporte-proyectos', 'pdf')}><Download size={15} /> PDF</button>
                        <button className="secondary-button" onClick={() => void exportar('/reportes/proyectos/exportar', limpiar(filtros), 'reporte-proyectos', 'xlsx')}><Download size={15} /> Excel</button>
                    </div>
                </div>
                <div className="filtros">
                    <label className="mod-field"><span>MATERIA</span>
                        <select className="mod-select" value={filtros.materiaId} onChange={(e) => cambiar('materiaId', e.target.value)}>
                            <option value="">Todas</option>
                            {materias.map((m) => <option key={m.id} value={m.id}>{m.nombre}</option>)}
                        </select>
                    </label>
                    <label className="mod-field"><span>ESTADO</span>
                        <select className="mod-select" value={filtros.estado} onChange={(e) => cambiar('estado', e.target.value)}>
                            <option value="">Todos</option><option value="ACTIVO">Activo</option>
                            <option value="FINALIZADO">Finalizado</option><option value="CANCELADO">Cancelado</option>
                        </select>
                    </label>
                    <label className="mod-field"><span>CICLO</span>
                        <input className="mod-input" placeholder="02-2026" value={filtros.ciclo} onChange={(e) => cambiar('ciclo', e.target.value)} />
                    </label>
                    <label className="mod-field"><span>PUBLICACIÓN</span>
                        <select className="mod-select" value={filtros.publicado} onChange={(e) => cambiar('publicado', e.target.value)}>
                            <option value="">Todos</option><option value="true">Publicados</option><option value="false">Sin publicar</option>
                        </select>
                    </label>
                </div>

                <div className="table-wrap">
                    <table>
                        <thead><tr><th>Proyecto</th><th>Grupo</th><th>Sección</th><th>Materia</th><th>Evaluaciones</th><th>Nota</th><th>Publicado</th></tr></thead>
                        <tbody>
                        {filas.map((f) => (
                            <tr key={f.proyectoId}>
                                <td><button className="project-link" onClick={() => void abrir(f.proyectoId)}><strong>{f.titulo}</strong></button><small>{f.empresa || 'Sin empresa'}</small></td>
                                <td>{f.grupo}</td>
                                <td>{f.seccion} · {f.ciclo}</td>
                                <td>{f.materia}</td>
                                <td>{f.enviadas} de {f.invitados}</td>
                                <td><strong>{fmt(f.notaFinal)}</strong></td>
                                <td><span className={`tag ${f.publicado ? 'tag-ok' : 'tag-warn'}`}>{f.publicado ? 'Sí' : 'No'}</span></td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                    {!cargando && filas.length === 0 && <div className="empty-state"><strong>No hay proyectos con esos filtros</strong></div>}
                </div>
            </section>

            <EstadisticasReporte filtros={filtrosEstadisticas} />

            {detalle && (
                <section className="panel compacto" style={{ marginTop: 24 }}>
                    <div className="panel-heading">
                        <div><span className="panel-kicker">DETALLE</span><h2>{detalle.titulo}</h2></div>
                        <div className="export-actions">
                            <button className="secondary-button" onClick={() => void exportar(`/reportes/proyectos/${detalle.proyectoId}/exportar`, {}, `proyecto-${detalle.proyectoId}`, 'pdf')}><Download size={15} /> PDF</button>
                            <button className="secondary-button" onClick={() => void exportar(`/reportes/proyectos/${detalle.proyectoId}/exportar`, {}, `proyecto-${detalle.proyectoId}`, 'xlsx')}><Download size={15} /> Excel</button>
                        </div>
                    </div>
                    <div className="result-metrics">
                        <strong>{fmt(detalle.notaFinal)}<small>Nota de docente / jurado</small></strong>
                        <strong>{detalle.invitados}<small>Invitados</small></strong>
                        <strong>{detalle.enviadas}<small>Enviadas</small></strong>
                    </div>
                    {(detalle.evaluadores ?? []).map((e: Row) => (
                        <div className="person-row" key={e.evaluadorId}>
                            <span>{e.nombre}<small>{e.rol} · {estados[e.estado] ?? e.estado} · Nota: {fmt(e.nota)}</small></span>
                            <span>{e.comentario ?? ''}</span>
                        </div>
                    ))}
                    {(detalle.criterios ?? []).length > 0 && <h3 style={{ margin: '22px 0 8px', fontSize: 13 }}>Promedio por criterio</h3>}
                    {(detalle.criterios ?? []).map((c: Row) => (
                        <div className="person-row" key={c.criterioId}><span>{c.nombre}<small>{c.ponderacion}%</small></span><strong>{fmt(c.promedio)}</strong></div>
                    ))}
                </section>
            )}
        </Shell>
    )
}