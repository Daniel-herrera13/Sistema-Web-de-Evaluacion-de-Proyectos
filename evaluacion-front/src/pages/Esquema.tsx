import { useEffect, useState } from 'react'
import { Plus, Save, Trash2 } from 'lucide-react'
import api from '../api/axios'
import Shell from '../components/Shell'
import { useAuth } from '../context/AuthContext'
import { mensajeError } from '../utils/errores'

type Row = Record<string, any>
interface Item { nombre: string; descripcion: string; ponderacion: string }

const FORMATO_CICLO = /^\d{2}-\d{4}$/

const vacio = (): Item => ({ nombre: '', descripcion: '', ponderacion: '' })
const num = (valor: string) => (valor === '' || Number.isNaN(Number(valor)) ? 0 : Number(valor))
const redondear = (n: number) => Math.round(n * 100) / 100
const sumar = (items: Item[]) => redondear(items.reduce((s, i) => s + num(i.ponderacion), 0))
const lista = (d: any): Row[] => (Array.isArray(d) ? d : d?.content ?? [])
const validoItem = (i: Item) => i.nombre.trim() !== '' && num(i.ponderacion) > 0
const aItems = (arr: Row[] = []): Item[] =>
    arr.map((c) => ({ nombre: c.nombre, descripcion: c.descripcion ?? '', ponderacion: String(c.ponderacion) }))
const aPayload = (items: Item[]) =>
    items.map((i) => ({ nombre: i.nombre.trim(), descripcion: i.descripcion.trim() || null, ponderacion: num(i.ponderacion) }))

const atajos = [
    { label: 'Solo docente o jurado', doc: '100', auto: '0', pub: '0' },
    { label: 'Docente + autoevaluación', doc: '70', auto: '30', pub: '0' },
    { label: 'Docente + público', doc: '60', auto: '0', pub: '40' },
    { label: 'Las tres modalidades', doc: '50', auto: '25', pub: '25' },
]

function Criterios({ titulo, items, onChange, bloqueado }: { titulo: string; items: Item[]; onChange: (items: Item[]) => void; bloqueado: boolean }) {
    const total = sumar(items)
    const estado = total === 100 ? 'ok' : total > 100 ? 'over' : ''
    const cambiar = (i: number, campo: keyof Item, valor: string) =>
        onChange(items.map((item, k) => (k === i ? { ...item, [campo]: valor } : item)))

    return (
        <section className="panel compacto">
            <div className="panel-heading"><div><span className="panel-kicker">CRITERIOS</span><h2>{titulo}</h2></div></div>
            {items.map((item, i) => (
                <div className="crit-row" key={i}>
                    <input className="mod-input" placeholder="Criterio" disabled={bloqueado} value={item.nombre} onChange={(e) => cambiar(i, 'nombre', e.target.value)} />
                    <input className="mod-input" placeholder="Descripción (opcional)" disabled={bloqueado} value={item.descripcion} onChange={(e) => cambiar(i, 'descripcion', e.target.value)} />
                    <input className="mod-input" type="number" min="0" max="100" step="0.01" placeholder="%" disabled={bloqueado} value={item.ponderacion} onChange={(e) => cambiar(i, 'ponderacion', e.target.value)} />
                    {!bloqueado && <button type="button" className="mini-btn" title="Quitar" onClick={() => onChange(items.filter((_, k) => k !== i))}><Trash2 size={15} /></button>}
                </div>
            ))}
            {!bloqueado && <button type="button" className="secondary-button add-crit" onClick={() => onChange([...items, vacio()])}><Plus size={15} /> Agregar criterio</button>}
            <div className={`sum-bar ${estado}`}><i style={{ width: `${Math.min(total, 100)}%` }} /></div>
            <p className={`sum-text ${estado}`}>Suma: <strong>{total}%</strong> {total === 100 ? '— correcto' : '— debe sumar exactamente 100%'}</p>
        </section>
    )
}

export default function Esquema() {
    const { usuario } = useAuth()
    const esAdmin = usuario?.rol === 'ADMIN'
    const [materias, setMaterias] = useState<Row[]>([])
    const [materiaId, setMateriaId] = useState('')
    const [ciclo, setCiclo] = useState('')                       // '' = esquema general
    const [ciclosSecciones, setCiclosSecciones] = useState<string[]>([])
    const [ciclosPropios, setCiclosPropios] = useState<string[]>([])
    const [ciclosNuevos, setCiclosNuevos] = useState<string[]>([])
    const [nuevoCiclo, setNuevoCiclo] = useState('')
    const [heredado, setHeredado] = useState(false)              // el ciclo no tiene esquema propio: se ve el general
    const [enUso, setEnUso] = useState(false)                    // ya hay evaluaciones: solo lectura
    const [doc, setDoc] = useState('100')
    const [auto, setAuto] = useState('0')
    const [pub, setPub] = useState('0')
    const [critAuto, setCritAuto] = useState<Item[]>([])
    const [critPub, setCritPub] = useState<Item[]>([])
    const [error, setError] = useState('')
    const [aviso, setAviso] = useState('')
    const bloqueado = enUso && !esAdmin

    useEffect(() => {
        api.get('/esquemas/materias').then((r) => setMaterias(lista(r.data))).catch((e) => setError(mensajeError(e, 'No se pudieron cargar las materias.')))
        // ciclos que ya existen en las secciones, como sugerencias
        api.get('/secciones')
            .then((r) => setCiclosSecciones([...new Set(lista(r.data).map((s) => String(s.ciclo ?? '')).filter(Boolean))]))
            .catch(() => { /* sin sugerencias de ciclo */ })
    }, [])

    const ciclos = [...new Set([...ciclosSecciones, ...ciclosPropios, ...ciclosNuevos])].sort()

    const cargar = async (id: string, cicloElegido = '') => {
        setMateriaId(id); setCiclo(cicloElegido); setError(''); setAviso('')
        // limpia el estado de la materia o el ciclo anterior
        setDoc('100'); setAuto('0'); setPub('0'); setCritAuto([]); setCritPub([]); setHeredado(false); setEnUso(false)
        if (!id) { setCiclosPropios([]); return }
        try {
            const [esquema, propios] = await Promise.all([
                api.get(`/esquemas/materia/${id}`, { params: cicloElegido ? { ciclo: cicloElegido } : {} }),
                api.get(`/esquemas/materia/${id}/ciclos`),
            ])
            const data = esquema.data
            setDoc(String(data.pesoDocenteJurado)); setAuto(String(data.pesoAutoevaluacion)); setPub(String(data.pesoPublico))
            setCritAuto(aItems(data.criteriosAutoevaluacion)); setCritPub(aItems(data.criteriosPublico))
            setHeredado(Boolean(data.heredado)); setEnUso(Boolean(data.enUso))
            setCiclosPropios(lista(propios.data).map((c) => String(c)))
        } catch (e) {
            setMateriaId(''); setCiclo('')   // si no se pudo cargar, no se muestra un formulario con datos de otra materia
            setError(mensajeError(e, 'No se pudo cargar el esquema de la materia.'))
        }
    }

    const agregarCiclo = () => {
        const c = nuevoCiclo.trim()
        if (!FORMATO_CICLO.test(c)) { setError('El ciclo debe tener el formato NN-AAAA, por ejemplo 01-2027.'); return }
        if (!ciclosNuevos.includes(c)) setCiclosNuevos([...ciclosNuevos, c])
        setNuevoCiclo('')
        void cargar(materiaId, c)
    }

    const aplicarAtajo = (a: typeof atajos[number]) => {
        setDoc(a.doc); setAuto(a.auto); setPub(a.pub)
        if (num(a.auto) > 0 && critAuto.length === 0) setCritAuto([vacio()])
        if (num(a.pub) > 0 && critPub.length === 0) setCritPub([vacio()])
    }

    const total = redondear(num(doc) + num(auto) + num(pub))
    const totalOk = total === 100
    const autoOk = num(auto) === 0 || (critAuto.length > 0 && sumar(critAuto) === 100 && critAuto.every(validoItem))
    const pubOk = num(pub) === 0 || (critPub.length > 0 && sumar(critPub) === 100 && critPub.every(validoItem))
    const puedeGuardar = materiaId !== '' && totalOk && autoOk && pubOk && !bloqueado

    const guardar = async () => {
        setError(''); setAviso('')
        try {
            await api.put(`/esquemas/materia/${materiaId}`, {
                pesoDocenteJurado: num(doc),
                pesoAutoevaluacion: num(auto),
                pesoPublico: num(pub),
                criteriosAutoevaluacion: num(auto) > 0 ? aPayload(critAuto) : [],
                criteriosPublico: num(pub) > 0 ? aPayload(critPub) : [],
            }, { params: ciclo ? { ciclo } : {} })
            await cargar(materiaId, ciclo)
            setAviso(ciclo
                ? `Esquema guardado para el ciclo ${ciclo}.`
                : 'Esquema general guardado. Se aplica a los ciclos que no tengan uno propio.')
        } catch (e) { setError(mensajeError(e, 'No se pudo guardar el esquema.')) }
    }

    const tarjetas: { id: string; label: string; valor: string; set: (v: string) => void; nota: string }[] = [
        { id: 'doc', label: 'DOCENTE O JURADO', valor: doc, set: setDoc, nota: 'Evalúan con los criterios de la materia' },
        { id: 'auto', label: 'AUTOEVALUACIÓN', valor: auto, set: setAuto, nota: 'Cada alumno a sí mismo y a su grupo, anónimo' },
        { id: 'pub', label: 'PÚBLICO', valor: pub, set: setPub, nota: 'Visitantes de ferias o defensas, anónimo' },
    ]

    return (
        <Shell eyebrow="CONFIGURACIÓN" title="Modalidades de evaluación" description="Elige quién evalúa a los estudiantes y cuánto vale cada nota. Se define por materia y por ciclo.">
            {error && <p className="dashboard-alert">{error}</p>}
            {aviso && <p className="dashboard-success">{aviso}</p>}

            <section className="panel compacto">
                <div style={{ display: 'grid', gap: 16 }}>
                    <label className="mod-field">
                        <span>MATERIA</span>
                        <select className="mod-select" value={materiaId} onChange={(e) => void cargar(e.target.value)}>
                            <option value="">Selecciona una materia</option>
                            {materias.map((m) => <option key={m.id} value={m.id}>{m.nombre}</option>)}
                        </select>
                    </label>

                    {materiaId && (
                        <>
                            <label className="mod-field">
                                <span>CICLO</span>
                                <select className="mod-select" value={ciclo} onChange={(e) => void cargar(materiaId, e.target.value)}>
                                    <option value="">General (ciclos sin esquema propio)</option>
                                    {ciclos.map((c) => (
                                        <option key={c} value={c}>{c}{ciclosPropios.includes(c) ? ' · con esquema propio' : ''}</option>
                                    ))}
                                </select>
                            </label>
                            <div className="preset-row">
                                <input className="mod-input" style={{ maxWidth: 200 }} placeholder="Nuevo ciclo, ej. 01-2027" value={nuevoCiclo} onChange={(e) => setNuevoCiclo(e.target.value)} />
                                <button type="button" className="secondary-button" onClick={agregarCiclo}><Plus size={15} /> Agregar ciclo</button>
                            </div>
                        </>
                    )}
                </div>
            </section>

            {materiaId && heredado && !enUso && (
                <p className="aviso-suma">Este ciclo todavía no tiene esquema propio: se muestra el esquema general. Al guardar se creará uno propio para {ciclo}.</p>
            )}
            {materiaId && enUso && (
                <p className="aviso-suma">
                    {esAdmin
                        ? (ciclo ? "Este esquema del ciclo " + ciclo + " ya tiene evaluaciones. Como administrador puedes modificarlo. Las respuestas anteriores conservan sus criterios; las nuevas evaluaciones usar\u00e1n los vigentes. Si cambias porcentajes, las notas finales se recalcular\u00e1n con esos porcentajes." : "El esquema general ya tiene evaluaciones. Como administrador puedes modificarlo. Las respuestas anteriores conservan sus criterios; las nuevas evaluaciones usar\u00e1n los vigentes. Si cambias porcentajes, las notas finales se recalcular\u00e1n con esos porcentajes.")
                        : "Ya hay evaluaciones con este esquema. " + (ciclo ? "Ciclo " + ciclo + ". " : "") + "No se puede cambiar; elige otro ciclo para guardar porcentajes o criterios diferentes."}
                </p>
            )}

            {materiaId && (
                <div className="mod-stack" style={{ marginTop: 24 }}>
                    <section className="panel compacto">
                        <div className="panel-heading"><div><span className="panel-kicker">FORMA DE EVALUAR</span><h2>¿Cómo se calculará la nota?</h2></div></div>
                        <div className="preset-row">
                            {atajos.map((a) => <button key={a.label} type="button" className="preset-btn" disabled={bloqueado} onClick={() => aplicarAtajo(a)}>{a.label}</button>)}
                        </div>
                        <div className="pct-grid">
                            {tarjetas.map((t) => (
                                <div key={t.id} className={`pct-card ${num(t.valor) > 0 ? 'activa' : ''}`}>
                                    <label htmlFor={`pct-${t.id}`}>{t.label}</label>
                                    <div className="pct-input"><input id={`pct-${t.id}`} type="number" min="0" max="100" step="0.01" disabled={bloqueado} value={t.valor} onChange={(e) => t.set(e.target.value)} /><span>%</span></div>
                                    <small>{t.nota}</small>
                                </div>
                            ))}
                        </div>
                        <div className={`sum-bar ${totalOk ? 'ok' : total > 100 ? 'over' : ''}`}><i style={{ width: `${Math.min(total, 100)}%` }} /></div>
                        {totalOk
                            ? <p className="sum-text ok">Los porcentajes suman <strong>100%</strong>.</p>
                            : <p className="aviso-suma">Los porcentajes deben sumar exactamente 100%. Llevas {total}%.</p>}
                    </section>

                    {num(doc) > 0 && (
                        <p className="nota-info">Los criterios de docente o jurado se definen en <strong>Criterios</strong>, también por ciclo, y se heredan de materia, sección, grupo o proyecto.</p>
                    )}
                    {num(auto) === 0 && num(pub) === 0 && (
                        <p className="nota-info">Para definir criterios de autoevaluación o de evaluación pública, asigna un porcentaje mayor a 0 a esa modalidad.</p>
                    )}
                    {num(auto) > 0 && <Criterios titulo="Criterios de autoevaluación" items={critAuto} onChange={setCritAuto} bloqueado={bloqueado} />}
                    {num(pub) > 0 && <Criterios titulo="Criterios de evaluación pública" items={critPub} onChange={setCritPub} bloqueado={bloqueado} />}

                    <div className="form-actions">
                        <button className="primary-button" type="button" disabled={!puedeGuardar} onClick={() => void guardar()}>
                            <Save size={16} /> {ciclo ? `Guardar esquema del ciclo ${ciclo}` : 'Guardar esquema general'}
                        </button>
                    </div>
                </div>
            )}
        </Shell>
    )
}
