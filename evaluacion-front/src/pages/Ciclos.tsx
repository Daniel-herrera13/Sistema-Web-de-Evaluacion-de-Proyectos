import { useEffect, useState, type FormEvent } from 'react'
import { CalendarDays, Power } from 'lucide-react'
import api from '../api/axios'
import Shell from '../components/Shell'
import { useLanguage } from '../context/LanguageContext'
import { mensajeError } from '../utils/errores'

interface Ciclo { id: number; codigo: string; activo: boolean }

export default function Ciclos() {
    const { language } = useLanguage()
    const en = language === 'en'
    const [ciclos, setCiclos] = useState<Ciclo[]>([])
    const [codigo, setCodigo] = useState('')
    const [error, setError] = useState('')
    const [notice, setNotice] = useState('')
    const [busy, setBusy] = useState(false)

    const cargar = async () => setCiclos((await api.get<Ciclo[]>('/ciclos')).data)
    useEffect(() => { void cargar().catch((e) => setError(mensajeError(e, 'No se pudieron cargar los ciclos.'))) }, [])

    const crear = async (e: FormEvent) => {
        e.preventDefault(); setError(''); setNotice('')
        if (!/^\d{2}-\d{4}$/.test(codigo.trim())) { setError(en ? 'Use the format NN-YYYY, for example 01-2027.' : 'Usa el formato NN-AAAA, por ejemplo 01-2027.'); return }
        setBusy(true)
        try { await api.post('/ciclos', { codigo: codigo.trim(), activo: true }); setCodigo(''); await cargar(); setNotice(en ? 'Term added.' : 'Ciclo agregado.') }
        catch (err) { setError(mensajeError(err, en ? 'Could not add the term.' : 'No se pudo agregar el ciclo.')) }
        finally { setBusy(false) }
    }

    const alternar = async (c: Ciclo) => {
        setError(''); setNotice('')
        try { await api.put(`/ciclos/${c.id}`, { codigo: c.codigo, activo: !c.activo }); await cargar() }
        catch (err) { setError(mensajeError(err, en ? 'Could not update the term.' : 'No se pudo actualizar el ciclo.')) }
    }

    return <Shell eyebrow={en ? 'ACADEMIC SETTINGS' : 'CONFIGURACIÓN ACADÉMICA'} title={en ? 'Academic terms' : 'Ciclos académicos'} description={en ? 'Add terms and choose which ones are available for new academic records.' : 'Agrega ciclos y determina cuáles estarán disponibles para nuevos registros.'}>
        <div className="grid gap-6 lg:grid-cols-[minmax(280px,0.75fr)_minmax(0,1.5fr)]">
            <section className="panel">
                <div className="panel-heading"><div><span className="panel-kicker">{en ? 'NEW TERM' : 'NUEVO CICLO'}</span><h2>{en ? 'Add a term' : 'Agregar ciclo'}</h2></div><CalendarDays size={20} /></div>
                <form onSubmit={(e) => void crear(e)} className="grid gap-3">
                    <label className="gestion-field"><span>{en ? 'Code (NN-YYYY)' : 'Código (NN-AAAA)'}</span><input required maxLength={7} placeholder="01-2027" value={codigo} onChange={(e) => setCodigo(e.target.value)} /></label>
                    <button className="primary-button" disabled={busy}>{en ? 'Add term' : 'Agregar ciclo'}</button>
                </form>
            </section>
            <section className="panel">
                <div className="panel-heading"><div><span className="panel-kicker">{en ? 'TERM LIST' : 'LISTADO DE CICLOS'}</span><h2>{ciclos.length} {en ? 'terms' : 'ciclos'}</h2></div></div>
                {error && <p role="alert" className="dashboard-alert">{error}</p>}{notice && <p role="status" className="dashboard-alert">{notice}</p>}
                <div className="table-wrap"><table><thead><tr><th>{en ? 'Term' : 'Ciclo'}</th><th>{en ? 'Status' : 'Estado'}</th><th>{en ? 'Action' : 'Acción'}</th></tr></thead><tbody>
                    {ciclos.map((c) => <tr key={c.id}><td><strong>{c.codigo}</strong></td><td><span className={`tag ${c.activo ? 'tag-ok' : 'tag-warn'}`}>{c.activo ? (en ? 'Active' : 'Activo') : (en ? 'Inactive' : 'Inactivo')}</span></td><td><button type="button" className="secondary-button" onClick={() => void alternar(c)}><Power size={15} />{c.activo ? (en ? 'Deactivate' : 'Desactivar') : (en ? 'Activate' : 'Activar')}</button></td></tr>)}
                </tbody></table></div>
            </section>
        </div>
    </Shell>
}
