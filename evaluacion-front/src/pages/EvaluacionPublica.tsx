import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Copy, Percent, QrCode } from 'lucide-react'
import api from '../api/axios'
import Shell from '../components/Shell'
import { mensajeError } from '../utils/errores'

type Row = Record<string, any>
const lista = (d: any): Row[] => (Array.isArray(d) ? d : d?.content ?? [])

export default function EvaluacionPublica() {
    const navigate = useNavigate()
    const [proyectos, setProyectos] = useState<Row[]>([])
    const [sel, setSel] = useState<Row | null>(null)
    const [estado, setEstado] = useState<Row | null>(null)
    const [codigoInput, setCodigoInput] = useState('')
    const [error, setError] = useState('')
    const [aviso, setAviso] = useState('')
    const [faltaEsquema, setFaltaEsquema] = useState(false)

    useEffect(() => {
        api.get('/esquemas/proyectos-publicos').then((r) => setProyectos(lista(r.data))).catch((e) => setError(mensajeError(e, 'No fue posible cargar los proyectos.')))
    }, [])

    const elegir = async (p: Row) => {
        setSel(p); setEstado(null); setError(''); setAviso(''); setFaltaEsquema(false)
        try {
            const data = (await api.get(`/esquemas/proyecto/${p.id}/publico`)).data
            setEstado(data)
            setCodigoInput(data.codigoValidacion ?? '')
        }
        catch (e) { setError(mensajeError(e, 'No fue posible consultar el estado.')) }
    }

    const accion = async (que: 'abrir' | 'cerrar') => {
        if (!sel) return
        setError(''); setAviso(''); setFaltaEsquema(false)
        try {
            setEstado((await api.post(`/esquemas/proyecto/${sel.id}/publico/${que}`)).data)
            setAviso(que === 'abrir' ? 'Evaluación pública abierta.' : 'Evaluación pública cerrada.')
        } catch (e) {
            const mensaje = mensajeError(e, 'No se pudo cambiar el estado.')
            setError(mensaje)
            if (que === 'abrir' && estado?.codigo && /esquema|criterios/i.test(mensaje)) setFaltaEsquema(true)
        }
    }

    const guardarCodigo = async () => {
        if (!sel) return
        setError(''); setAviso(''); setFaltaEsquema(false)
        try {
            const { data } = await api.put(`/esquemas/proyecto/${sel.id}/publico/codigo-validacion`, { codigo: codigoInput })
            setEstado(data)
            setCodigoInput(data.codigoValidacion ?? '')
            setAviso('Codigo de validacion guardado. El administrador y el docente responsable veran el mismo codigo.')
        } catch (e) { setError(mensajeError(e, 'No se pudo guardar el codigo.')) }
    }

    const enlace = estado?.codigo ? `${window.location.origin}/publico/${estado.codigo}` : ''
    const copiar = async () => {
        try { await navigator.clipboard.writeText(enlace); setAviso('Enlace copiado.') }
        catch { setAviso('Copia el enlace manualmente.') }
    }

    return (
        <Shell eyebrow="FERIAS Y DEFENSAS" title="Evaluación pública" description="Abre la evaluación de un proyecto para que el público la califique desde su celular, sin cuenta ni correo.">
            {error && <p className="dashboard-alert">{error}</p>}
            {aviso && <p className="dashboard-success">{aviso}</p>}
            {faltaEsquema && (
                <p className="nota-info">
                    Para abrirla, la materia del proyecto debe tener un porcentaje de <strong>público</strong> mayor a 0 en
                    Modalidades y porcentajes, con sus criterios sumando 100 %.{' '}
                    <button className="secondary-button" type="button" onClick={() => navigate('/modalidades')}>
                        <Percent size={15} /> Ir a Modalidades
                    </button>
                </p>
            )}
            <div className="mod-two">
                <section className="panel compacto">
                    <div className="panel-heading"><div><span className="panel-kicker">PROYECTOS</span><h2>Selecciona uno</h2></div></div>
                    <div className="target-list">
                        {proyectos.map((p) => (
                            <button key={p.id} className={`target-item ${sel?.id === p.id ? 'chosen' : ''}`} onClick={() => void elegir(p)}>
                                <QrCode size={18} /><span><strong>{p.titulo}</strong><small>{p.grupoNombre}</small></span>
                            </button>
                        ))}
                    </div>
                </section>

                <section className="panel compacto">
                    {!sel && <div className="empty-state"><QrCode size={28} /><strong>Selecciona un proyecto</strong></div>}
                    {sel && estado && (
                        <>
                            <div className="panel-heading">
                                <div><span className="panel-kicker">{sel.titulo}</span><h2>{estado.abierto ? 'Evaluación abierta' : 'Evaluación cerrada'}</h2></div>
                                <span className={`tag ${estado.abierto ? 'tag-ok' : 'tag-warn'}`}>{estado.abierto ? 'Abierta' : 'Cerrada'}</span>
                            </div>
                            <div className="code-box">
                                <span>CODIGO DEL PROYECTO (IDENTIFICADOR DEL ENLACE)</span>
                                {estado.codigo && <><strong>{estado.codigo}</strong><code>{enlace}</code></>}
                                <span>CODIGO DE VALIDACION (COMPARTIR POR SEPARADO)</span>
                                {estado.codigoValidacion && <strong>{estado.codigoValidacion}</strong>}
                                <label className="mod-field">
                                    <span>Define o cambia el codigo de validacion (4-20 letras, numeros o guiones)</span>
                                    <input className="mod-input" value={codigoInput} maxLength={20} placeholder="Ej. FERIA-2026" onChange={(e) => setCodigoInput(e.target.value)} />
                                </label>
                                <div className="export-actions">
                                    <button className="secondary-button" onClick={() => void guardarCodigo()} disabled={codigoInput.trim().length < 4}>Guardar codigo</button>
                                    {estado.codigo && <button className="secondary-button" onClick={() => void copiar()}><Copy size={15} /> Copiar enlace</button>}
                                </div>
                            </div>
                            <p className="nota-info"><strong>{estado.evaluaciones}</strong> evaluaciones recibidas.</p>
                            <div className="form-actions">
                                {estado.abierto
                                    ? <button className="secondary-button" onClick={() => void accion('cerrar')}>Cerrar evaluación</button>
                                    : <button className="primary-button" onClick={() => void accion('abrir')}>Abrir evaluación</button>}
                            </div>
                        </>
                    )}
                </section>
            </div>
        </Shell>
    )
}
