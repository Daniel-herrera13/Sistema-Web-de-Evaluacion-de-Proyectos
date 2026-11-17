import { useEffect, useState } from 'react'
import { FileText } from 'lucide-react'
import api from '../api/axios'
import Shell from '../components/Shell'
import { mensajeError } from '../utils/errores'

type Row = Record<string, any>
interface Item { proyecto: Row; integrantes: Row[] }

export default function MiProyecto() {
    const [items, setItems] = useState<Item[]>([])
    const [error, setError] = useState('')
    const [cargado, setCargado] = useState(false)

    useEffect(() => {
        const cargar = async () => {
            try {
                const { data } = await api.get('/proyectos')
                const proyectos: Row[] = Array.isArray(data) ? data : data?.content ?? []
                const completos = await Promise.all(proyectos.map(async (proyecto) => {
                    try {
                        const r = await api.get(`/grupos/${proyecto.grupoId}/alumnos`)
                        return { proyecto, integrantes: r.data as Row[] }
                    } catch {
                        return { proyecto, integrantes: [] as Row[] }
                    }
                }))
                setItems(completos)
            } catch (e) {
                setError(mensajeError(e, 'No fue posible cargar tu proyecto.'))
            } finally {
                setCargado(true)
            }
        }
        void cargar()
    }, [])

    return (
        <Shell eyebrow="MI PROYECTO" title="Mi proyecto" description="Información de tu proyecto y de los integrantes de tu grupo.">
            {error && <p className="dashboard-alert">{error}</p>}
            {cargado && items.length === 0 && !error && (
                <section className="panel">
                    <div className="empty-state"><FileText size={28} /><strong>No tienes un proyecto asignado</strong><p>Cuando tu docente te agregue a un grupo con proyecto, aparecerá aquí.</p></div>
                </section>
            )}
            <div className="stack-gap">
                {items.map(({ proyecto, integrantes }) => (
                    <section className="panel compacto" key={proyecto.id}>
                        <div className="panel-heading">
                            <div><span className="panel-kicker">GRUPO {proyecto.grupoNombre}</span><h2>{proyecto.titulo}</h2></div>
                            <span className="status-tag">{proyecto.estado}</span>
                        </div>
                        <div className="detail-grid">
                            <div><span>Materia</span><strong>{proyecto.materiaNombre || 'Materia no especificada'}</strong></div>
                            <div><span>Descripción</span><strong>{proyecto.descripcion || 'Sin descripción'}</strong></div>
                            <div><span>Empresa</span><strong>{proyecto.empresaNombre || 'Sin empresa'}</strong></div>
                            <div><span>Resultados</span><strong>{proyecto.resultadosPublicados ? 'Publicados' : 'Aún sin publicar'}</strong></div>
                        </div>
                        <h3 style={{ margin: '22px 0 8px', fontSize: 13 }}>Integrantes del grupo</h3>
                        {integrantes.map((m) => <div className="member-row" key={m.id}>{m.nombre} {m.apellido}</div>)}
                    </section>
                ))}
            </div>
        </Shell>
    )
}
