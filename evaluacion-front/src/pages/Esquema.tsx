import { useEffect, useState } from 'react'
import { Plus, Save, Trash2 } from 'lucide-react'
import api from '../api/axios'
import Shell from '../components/Shell'
import { mensajeError } from '../utils/errores'

type Row = Record<string, any>
interface Item { nombre: string; descripcion: string; ponderacion: string }

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

function Criterios({ titulo, items, onChange }: { titulo: string; items: Item[]; onChange: (items: Item[]) => void }) {
    const total = sumar(items)
    const estado = total === 100 ? 'ok' : total > 100 ? 'over' : ''
    const cambiar = (i: number, campo: keyof Item, valor: string) =>
        onChange(items.map((item, k) => (k === i ? { ...item, [campo]: valor } : item)))

    return (
        <section className="panel compacto">
            <div className="panel-heading"><div><span className="panel-kicker">CRITERIOS</span><h2>{titulo}</h2></div></div>
            {items.map((item, i) => (
                <div className="crit-row" key={i}>
                    <input className="mod-input" placeholder="Criterio" value={item.nombre} onChange={(e) => cambiar(i, 'nombre', e.target.value)} />
                    <input className="mod-input" placeholder="Descripción (opcional)" value={item.descripcion} onChange={(e) => cambiar(i, 'descripcion', e.target.value)} />
                    <input className="mod-input" type="number" min="0" max="100" step="0.01" placeholder="%" value={item.ponderacion} onChange={(e) => cambiar(i, 'ponderacion', e.target.value)} />
                    <button type="button" className="mini-btn" title="Quitar" onClick={() => onChange(items.filter((_, k) => k !== i))}><Trash2 size={15} /></button>
                </div>
            ))}
            <button type="button" className="secondary-button add-crit" onClick={() => onChange([...items, vacio()])}><Plus size={15} /> Agregar criterio</button>
            <div className={`sum-bar ${estado}`}><i style={{ width: `${Math.min(total, 100)}%` }} /></div>
            <p className={`sum-text ${estado}`}>Suma: <strong>{total}%</strong> {total === 100 ? '— correcto' : '— debe sumar exactamente 100%'}</p>
        </section>
    )
}

export default function Esquema() {
    const [materias, setMaterias] = useState<Row[]>([])
    const [materiaId, setMateriaId] = useState('')
    const [doc, setDoc] = useState('100')
    const [auto, setAuto] = useState('0')
    const [pub, setPub] = useState('0')
    const [critAuto, setCritAuto] = useState<Item[]>([])
    const [critPub, setCritPub] = useState<Item[]>([])
    const [error, setError] = useState('')
    const [aviso, setAviso] = useState('')

    useEffect(() => {
        api.get('/materias').then((r) => setMaterias(lista(r.data))).catch((e) => setError(mensajeError(e, 'No se pudieron cargar las materias.')))
    }, [])

    const cargar = async (id: string) => {
        setMateriaId(id); setError(''); setAviso('')
        if (!id) return
        try {
            const { data } = await api.get(`/esquemas/materia/${id}`)
            setDoc(String(data.pesoDocenteJurado)); setAuto(String(data.pesoAutoevaluacion)); setPub(String(data.pesoPublico))
            setCritAuto(aItems(data.criteriosAutoevaluacion)); setCritPub(aItems(data.criteriosPublico))
        } catch (e) { setError(mensajeError(e, 'No se pudo cargar el esquema de la materia.')) }
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
    const puedeGuardar = materiaId !== '' && totalOk && autoOk && pubOk

    const guardar = async () => {
        setError(''); setAviso('')
        try {
            const { data } = await api.put(`/esquemas/materia/${materiaId}`, {
                pesoDocenteJurado: num(doc),
                pesoAutoevaluacion: num(auto),
                pesoPublico: num(pub),
                criteriosAutoevaluacion: num(auto) > 0 ? aPayload(critAuto) : [],
                criteriosPublico: num(pub) > 0 ? aPayload(critPub) : [],
            })
            setCritAuto(aItems(data.criteriosAutoevaluacion)); setCritPub(aItems(data.criteriosPublico))
            setAviso('Esquema guardado. Se aplica a todas las secciones de la materia.')
        } catch (e) { setError(mensajeError(e, 'No se pudo guardar el esquema.')) }
    }

    const tarjetas: { id: string; label: string; valor: string; set: (v: string) => void; nota: string }[] = [
        { id: 'doc', label: 'DOCENTE O JURADO', valor: doc, set: setDoc, nota: 'Evalúan con los criterios de la materia' },
        { id: 'auto', label: 'AUTOEVALUACIÓN', valor: auto, set: setAuto, nota: 'Cada alumno a sí mismo y a su grupo, anónimo' },
        { id: 'pub', label: 'PÚBLICO', valor: pub, set: setPub, nota: 'Visitantes de ferias o defensas, anónimo' },
    ]

    return (
        <Shell eyebrow="CONFIGURACIÓN" title="Modalidades de evaluación" description="Elige quién evalúa a los estudiantes y cuánto vale cada nota. Aplica a todas las secciones de la materia.">
            {error && <p className="dashboard-alert">{error}</p>}
            {aviso && <p className="dashboard-success">{aviso}</p>}

            <section className="panel compacto">
                <label className="mod-field">
                    <span>MATERIA</span>
                    <select className="mod-select" value={materiaId} onChange={(e) => void cargar(e.target.value)}>
                        <option value="">Selecciona una materia</option>
                        {materias.map((m) => <option key={m.id} value={m.id}>{m.nombre}</option>)}
                    </select>
                </label>
            </section>

            {materiaId && (
                <div className="mod-stack" style={{ marginTop: 24 }}>
                    <section className="panel compacto">
                        <div className="panel-heading"><div><span className="panel-kicker">FORMA DE EVALUAR</span><h2>¿Cómo se calculará la nota?</h2></div></div>
                        <div className="preset-row">
                            {atajos.map((a) => <button key={a.label} type="button" className="preset-btn" onClick={() => aplicarAtajo(a)}>{a.label}</button>)}
                        </div>
                        <div className="pct-grid">
                            {tarjetas.map((t) => (
                                <div key={t.id} className={`pct-card ${num(t.valor) > 0 ? 'activa' : ''}`}>
                                    <label htmlFor={`pct-${t.id}`}>{t.label}</label>
                                    <div className="pct-input"><input id={`pct-${t.id}`} type="number" min="0" max="100" step="0.01" value={t.valor} onChange={(e) => t.set(e.target.value)} /><span>%</span></div>
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
                        <p className="nota-info">Los criterios de docente o jurado se definen en <strong>Criterios</strong> y se heredan de materia, sección, grupo o proyecto.</p>
                    )}
                    {num(auto) > 0 && <Criterios titulo="Criterios de autoevaluación" items={critAuto} onChange={setCritAuto} />}
                    {num(pub) > 0 && <Criterios titulo="Criterios de evaluación pública" items={critPub} onChange={setCritPub} />}

                    <div className="form-actions">
                        <button className="primary-button" type="button" disabled={!puedeGuardar} onClick={() => void guardar()}><Save size={16} /> Guardar esquema</button>
                    </div>
                </div>
            )}
        </Shell>
    )
}