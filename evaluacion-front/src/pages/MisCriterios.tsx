import { useEffect, useState } from 'react'
import { ClipboardCheck } from 'lucide-react'
import api from '../api/axios'
import Shell from '../components/Shell'
import { mensajeError } from '../utils/errores'

type Row = Record<string, any>
interface Bloque { proyecto: Row; origen: string; suma: number; criterios: Row[] }

const origenes: Record<string, string> = {
    PROYECTO: 'Definidos para tu proyecto',
    GRUPO: 'Definidos para tu grupo',
    SECCION: 'Definidos para tu sección',
    MATERIA: 'Definidos para la materia',
}

export default function MisCriterios() {
    const [bloques, setBloques] = useState<Bloque[]>([])
    const [error, setError] = useState('')
    const [cargado, setCargado] = useState(false)

    useEffect(() => {
        const cargar = async () => {
            try {
                const { data } = await api.get('/proyectos')
                const lista: Row[] = Array.isArray(data) ? data : data?.content ?? []
                const activos = lista.filter((p) => p.estado === 'ACTIVO')
                const resultado = await Promise.all(activos.map(async (proyecto) => {
                    try {
                        const r = await api.get('/criterios/efectivos', { params: { proyectoId: proyecto.id } })
                        return { proyecto, origen: r.data.origen, suma: Number(r.data.suma ?? 0), criterios: (r.data.criterios ?? []) as Row[] }
                    } catch {
                        return { proyecto, origen: 'NINGUNO', suma: 0, criterios: [] as Row[] }
                    }
                }))
                setBloques(resultado)
            } catch (e) {
                setError(mensajeError(e, 'No fue posible cargar los criterios.'))
            } finally {
                setCargado(true)
            }
        }
        void cargar()
    }, [])

    return (
        <Shell eyebrow="MI PROYECTO" title="Criterios del proyecto" description="Con estos criterios se evalúa cada uno de tus proyectos activos.">
            {error && <p className="dashboard-alert">{error}</p>}
            {cargado && bloques.length === 0 && !error && (
                <section className="panel">
                    <div className="empty-state"><ClipboardCheck size={28} /><strong>No tienes proyectos activos</strong><p>Cuando tengas un proyecto activo, verás aquí sus criterios.</p></div>
                </section>
            )}
            <div className="stack-gap">
                {bloques.map((b) => (
                    <section className="panel compacto" key={b.proyecto.id}>
                        <div className="panel-heading">
                            <div>
                                <span className="panel-kicker">GRUPO {b.proyecto.grupoNombre}</span>
                                <h2>{b.proyecto.titulo}</h2>
                                <p className="nota-info">Materia: {b.proyecto.materiaNombre || 'Materia no especificada'}</p>
                            </div>
                            {b.origen !== 'NINGUNO' && <span className="tag tag-ok">{origenes[b.origen] ?? b.origen}</span>}
                        </div>
                        {b.origen === 'NINGUNO' ? (
                            <p className="aviso-suma">Tu docente aún no definió los criterios de este proyecto (deben sumar 100%).</p>
                        ) : (
                            <div className="table-wrap">
                                <table>
                                    <thead><tr><th>Criterio</th><th>Descripción</th><th>Ponderación</th></tr></thead>
                                    <tbody>
                                    {b.criterios.map((c) => (
                                        <tr key={c.id}>
                                            <td><strong>{c.nombre}</strong></td>
                                            <td className="wrap">{c.descripcion || '—'}</td>
                                            <td>{c.ponderacion}%</td>
                                        </tr>
                                    ))}
                                    <tr><td><strong>Total</strong></td><td /><td><strong>{b.suma}%</strong></td></tr>
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </section>
                ))}
            </div>
        </Shell>
    )
}
