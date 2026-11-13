import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import axios from 'axios'
import { CheckCircle2, GraduationCap, Lock } from 'lucide-react'
import api from '../api/axios'
import { mensajeError } from '../utils/errores'
import './Modalidades.css'

const apiPublica = axios.create({ baseURL: api.defaults.baseURL })

export default function Publico() {
    const { codigo = '' } = useParams<{ codigo: string }>()
    const clave = `publico-evaluado-${codigo.toUpperCase()}`
    const [info, setInfo] = useState<any>(null)
    const [notas, setNotas] = useState<Record<number, number>>({})
    const [estado, setEstado] = useState<'cargando' | 'listo' | 'enviado' | 'cerrado'>('cargando')
    const [mensaje, setMensaje] = useState('')
    const [enviando, setEnviando] = useState(false)

    useEffect(() => {
        let ya = false
        try { ya = localStorage.getItem(clave) === '1' } catch { /* sin almacenamiento */ }
        if (ya) { setEstado('enviado'); return }
        apiPublica.get(`/publico/proyectos/${codigo}`)
            .then((r) => { setInfo(r.data); setEstado('listo') })
            .catch((e) => { setMensaje(mensajeError(e, 'No se pudo abrir esta evaluación.')); setEstado('cerrado') })
    }, [codigo, clave])

    const criterios: any[] = info?.criterios ?? []
    const completo = criterios.length > 0 && criterios.every((c) => notas[c.id] !== undefined)

    const enviar = async () => {
        setEnviando(true); setMensaje('')
        try {
            await apiPublica.post(`/publico/proyectos/${codigo}/evaluar`, {
                notas: criterios.map((c) => ({ criterioId: c.id, nota: notas[c.id] })),
            })
            try { localStorage.setItem(clave, '1') } catch { /* sin almacenamiento */ }
            setEstado('enviado')
        } catch (e) { setMensaje(mensajeError(e, 'No se pudo enviar tu evaluación.')) }
        finally { setEnviando(false) }
    }

    return (
        <div className="pub">
            <header className="pub-head">
                <span><GraduationCap size={18} /> ITCA-FEPADE · Evaluación pública</span>
                <h1>{info?.proyecto ?? 'Evaluar proyecto'}</h1>
                {info && <p>{info.grupo}{info.empresa ? ` · ${info.empresa}` : ''}</p>}
            </header>

            <main className="pub-body">
                {estado === 'cargando' && <div className="pub-card"><p>Cargando…</p></div>}

                {estado === 'cerrado' && (
                    <div className="pub-card pub-centro"><Lock size={30} /><h2>Evaluación no disponible</h2><p>{mensaje}</p></div>
                )}

                {estado === 'enviado' && (
                    <div className="pub-card pub-centro ok"><CheckCircle2 size={34} /><h2>¡Gracias por tu evaluación!</h2><p>Tu respuesta es anónima y ya quedó registrada.</p></div>
                )}

                {estado === 'listo' && (
                    <>
                        <div className="pub-card"><p className="pub-ayuda">Califica cada criterio del <strong>1</strong> (muy bajo) al <strong>10</strong> (excelente). No necesitas cuenta ni correo.</p></div>
                        {criterios.map((c) => (
                            <div className="pub-card" key={c.id}>
                                <strong>{c.nombre}</strong>
                                {c.descripcion && <small>{c.descripcion}</small>}
                                <div className="rate-grid">
                                    {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((n) => (
                                        <button key={n} type="button" className={`rate-btn ${notas[c.id] === n ? 'on' : ''}`} onClick={() => setNotas({ ...notas, [c.id]: n })}>{n}</button>
                                    ))}
                                </div>
                            </div>
                        ))}
                        {mensaje && <p className="dashboard-alert">{mensaje}</p>}
                        <button className="pub-send" disabled={!completo || enviando} onClick={() => void enviar()}>
                            {enviando ? 'Enviando…' : completo ? 'Enviar evaluación' : 'Califica todos los criterios'}
                        </button>
                    </>
                )}
            </main>
        </div>
    )
}