import { useEffect, useState, type FormEvent } from 'react'
import { useParams } from 'react-router-dom'
import axios from 'axios'
import { CheckCircle2, Lock, Globe } from 'lucide-react'
import api from '../api/axios'
import { mensajeError } from '../utils/errores'
import { useLanguage } from '../context/LanguageContext'
import './Modalidades.css'

const apiPublica = axios.create({ baseURL: api.defaults.baseURL })

type Estado = 'ingresar-proyecto' | 'validar' | 'cargando' | 'listo' | 'enviado' | 'cerrado'

export default function Publico() {
    const { language, toggleLanguage } = useLanguage()
    const { codigo: codigoRuta = '' } = useParams<{ codigo: string }>()
    const [codigo, setCodigo] = useState(codigoRuta.trim().toUpperCase())
    const [codigoProyectoEntrada, setCodigoProyectoEntrada] = useState('')
    const [codigoValidacionEntrada, setCodigoValidacionEntrada] = useState('')
    const [codigoValidacion, setCodigoValidacion] = useState('')
    const [info, setInfo] = useState<any>(null)
    const [notas, setNotas] = useState<Record<number, number>>({})
    const [estado, setEstado] = useState<Estado>(codigo ? 'validar' : 'ingresar-proyecto')
    const [mensaje, setMensaje] = useState('')
    const [enviando, setEnviando] = useState(false)
    const clave = `publico-evaluado-${codigo}`

    useEffect(() => {
        const ruta = codigoRuta.trim().toUpperCase()
        setCodigo(ruta)
        setEstado(ruta ? 'validar' : 'ingresar-proyecto')
        setInfo(null)
        setCodigoValidacion('')
        setMensaje('')
    }, [codigoRuta])

    const accederConCodigoProyecto = (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        const valor = codigoProyectoEntrada.trim().toUpperCase()
        if (valor.length < 4) { setMensaje('Escribe el codigo del proyecto.'); return }
        setCodigo(valor)
        setCodigoValidacionEntrada('')
        setMensaje('')
        setEstado('validar')
    }

    const validarCodigo = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault()
        const valor = codigoValidacionEntrada.trim().toUpperCase()
        if (valor.length < 4) { setMensaje('Escribe el codigo de validacion.'); return }
        setEstado('cargando')
        setMensaje('')
        try {
            if (localStorage.getItem(clave) === '1') { setEstado('enviado'); return }
        } catch { /* sin almacenamiento local */ }
        try {
            const { data } = await apiPublica.post(`/publico/proyectos/${codigo}/validar`, { codigoValidacion: valor })
            setInfo(data)
            setCodigoValidacion(valor)
            setNotas({})
            setEstado('listo')
        } catch (e) {
            const error = mensajeError(e, 'No se pudo validar el codigo.')
            setMensaje(error)
            setEstado(/cerrad/i.test(error) ? 'cerrado' : 'validar')
        }
    }

    const criterios: any[] = info?.criterios ?? []
    const completo = criterios.length > 0 && criterios.every((c) => notas[c.id] !== undefined)

    const enviar = async () => {
        setEnviando(true); setMensaje('')
        try {
            await apiPublica.post(`/publico/proyectos/${codigo}/evaluar`, {
                codigoValidacion,
                notas: criterios.map((c) => ({ criterioId: c.id, nota: notas[c.id] })),
            })
            try { localStorage.setItem(clave, '1') } catch { /* sin almacenamiento */ }
            setEstado('enviado')
        } catch (e) { setMensaje(mensajeError(e, 'No se pudo enviar la evaluacion.')) }
        finally { setEnviando(false) }
    }

    const cambiarProyecto = () => {
        setCodigo('')
        setCodigoProyectoEntrada('')
        setCodigoValidacionEntrada('')
        setCodigoValidacion('')
        setInfo(null)
        setMensaje('')
        setEstado('ingresar-proyecto')
    }

    return (
        <div className="pub">
            <header className="pub-head">
                <button type="button" onClick={toggleLanguage} aria-label={language === 'es' ? 'Switch to English' : 'Cambiar a español'} style={{ position: 'absolute', top: 16, right: 16, display: 'inline-flex', alignItems: 'center', gap: 6, border: '1px solid currentColor', borderRadius: 999, padding: '6px 10px', background: 'transparent', color: 'inherit', cursor: 'pointer' }}><Globe size={15} />{language.toUpperCase()}</button>
                <img className="public-institution-logo" src="/itca-fepade-logo.png" alt="ITCA-FEPADE" />
                <span>Evaluación pública de proyectos</span>
                <h1>{info?.proyecto ?? 'Evaluar proyecto'}</h1>
                {info && <p>{info.grupo}{info.empresa ? ` - ${info.empresa}` : ''}</p>}
            </header>

            <main className="pub-body">
                {estado === 'cargando' && <div className="pub-card"><p>Cargando...</p></div>}

                {estado === 'ingresar-proyecto' && (
                    <form className="pub-card" onSubmit={accederConCodigoProyecto}>
                        <p className="pub-ayuda">Ingresa el codigo del proyecto para continuar.</p>
                        <input className="mod-input" value={codigoProyectoEntrada} maxLength={20} autoComplete="off" placeholder="Codigo del proyecto" onChange={(e) => setCodigoProyectoEntrada(e.target.value)} />
                        {mensaje && <p className="dashboard-alert">{mensaje}</p>}
                        <button className="pub-send" type="submit" disabled={codigoProyectoEntrada.trim().length < 4}>Continuar</button>
                    </form>
                )}

                {estado === 'validar' && (
                    <form className="pub-card" onSubmit={(e) => void validarCodigo(e)}>
                        <Lock size={26} />
                        <p className="pub-ayuda">Ingresa el codigo de validacion que te proporciono el administrador o el docente responsable.</p>
                        <input className="mod-input" value={codigoValidacionEntrada} maxLength={20} autoComplete="off" placeholder="Codigo de validacion" onChange={(e) => setCodigoValidacionEntrada(e.target.value)} />
                        {mensaje && <p className="dashboard-alert">{mensaje}</p>}
                        <button className="pub-send" type="submit" disabled={codigoValidacionEntrada.trim().length < 4}>Validar y acceder</button>
                        {!codigoRuta && <button className="secondary-button" type="button" onClick={cambiarProyecto}>Cambiar codigo de proyecto</button>}
                    </form>
                )}

                {estado === 'cerrado' && (
                    <div className="pub-card pub-centro"><Lock size={30} /><h2>Evaluacion no disponible</h2><p>{mensaje}</p><button className="secondary-button" onClick={cambiarProyecto}>Ingresar otro codigo de proyecto</button></div>
                )}

                {estado === 'enviado' && (
                    <div className="pub-card pub-centro ok"><CheckCircle2 size={34} /><h2>Gracias por tu evaluacion</h2><p>Tu respuesta es anonima y ya quedo registrada.</p></div>
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
                            {enviando ? 'Enviando...' : completo ? 'Enviar evaluacion' : 'Califica todos los criterios'}
                        </button>
                    </>
                )}
            </main>
        </div>
    )
}
