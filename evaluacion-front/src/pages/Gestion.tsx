import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Navigate, useParams } from 'react-router-dom'
import axios from 'axios'
import { BookOpen, Building2, ClipboardCheck, Edit3, FileText, GraduationCap, Layers, Plus, Save, Trash2, Users, X } from 'lucide-react'
import api from '../api/axios'
import Shell from '../components/Shell'
import AlumnosDisponibles from '../components/AlumnosDisponibles'
import { useAuth, type Rol } from '../context/AuthContext'
import type { Icono } from '../config/menu'
import { mensajeError } from '../utils/errores'
import './Gestion.css'

type Module = 'usuarios' | 'materias' | 'secciones' | 'grupos' | 'empresas' | 'proyectos' | 'criterios'
type Row = Record<string, any>
interface Campo {
    key: string
    label: string
    type?: string
    step?: string
    required?: boolean
    disabled?: boolean
    options?: [string, string][]
}

const modules: Record<Module, { title: string; description: string; icon: Icono; roles: Rol[] }> = {
    usuarios: { title: 'Usuarios', description: 'Administra docentes, jurado y alumnos.', icon: Users, roles: ['ADMIN'] },
    materias: { title: 'Materias', description: 'Organiza las materias y su docente principal.', icon: BookOpen, roles: ['ADMIN', 'DOCENTE'] },
    secciones: { title: 'Secciones', description: 'Crea las secciones de cada materia por ciclo.', icon: Layers, roles: ['ADMIN', 'DOCENTE'] },
    grupos: { title: 'Alumnos y grupos', description: 'Crea grupos y asigna integrantes disponibles.', icon: GraduationCap, roles: ['ADMIN', 'DOCENTE'] },
    empresas: { title: 'Empresas', description: 'Registra las empresas vinculadas a los proyectos.', icon: Building2, roles: ['ADMIN', 'DOCENTE'] },
    proyectos: { title: 'Proyectos', description: 'Registra proyectos y consulta su información.', icon: FileText, roles: ['ADMIN', 'DOCENTE'] },
    criterios: { title: 'Criterios de evaluación', description: 'Define ponderaciones por materia, sección, grupo o proyecto.', icon: ClipboardCheck, roles: ['ADMIN', 'DOCENTE'] },
}

const emptyForms: Record<Module, Row> = {
    usuarios: { nombre: '', apellido: '', correo: '', password: '', rolId: '', activo: true },
    materias: { nombre: '', descripcion: '', docentePrincipalId: '' },
    secciones: { nombre: '', ciclo: '', materiaId: '' },
    grupos: { nombre: '', asignacionId: '' },
    empresas: { nombre: '', contacto: '', correo: '', telefono: '' },
    proyectos: { titulo: '', descripcion: '', grupoId: '', empresaId: '', estado: 'ACTIVO' },
    criterios: { nombre: '', descripcion: '', ponderacion: '', nivel: '', nivelId: '' },
}

const etiquetaNivel: Record<string, string> = { MATERIA: 'Materia', SECCION: 'Sección', GRUPO: 'Grupo', PROYECTO: 'Proyecto' }
const claveNivel: Record<string, string> = { MATERIA: 'materiaId', SECCION: 'seccionId', GRUPO: 'grupoId', PROYECTO: 'proyectoId' }
const idsNumericos = ['docentePrincipalId', 'asignacionId', 'seccionId', 'grupoId', 'empresaId', 'materiaId', 'proyectoId', 'rolId', 'ponderacion']

const listData = (data: any): Row[] => (Array.isArray(data) ? data : data?.content ?? [])
const get = async (url: string): Promise<Row[]> => listData((await api.get(url)).data)
const texto = (v: any): string =>
    v == null ? '' : typeof v === 'object' ? `${v.nombre ?? ''} ${v.apellido ?? ''}`.trim() : String(v)

async function cargarDocentes(): Promise<Row[]> {
    try { return await get('/usuarios/docentes') } catch { /* se intenta con el listado general */ }
    const todos = await get('/usuarios')
    return todos.filter((u) => texto(u.rol).toUpperCase() === 'DOCENTE')
}

export default function Gestion() {
    const { modulo = 'materias' } = useParams()
    const { usuario } = useAuth()
    if (!(modulo in modules)) return <Navigate to="/dashboard" replace />
    const active = modulo as Module
    if (!usuario || !modules[active].roles.includes(usuario.rol)) return <Navigate to="/dashboard" replace />
    return <Modulo key={active} active={active} />
}

function Modulo({ active }: { active: Module }) {
    const meta = modules[active]
    const Icon = meta.icon
    const [rows, setRows] = useState<Row[]>([])
    const [lookups, setLookups] = useState<Record<string, Row[]>>({})
    const [form, setForm] = useState<Row>(emptyForms[active])
    const [editing, setEditing] = useState<number | null>(null)
    const [notice, setNotice] = useState('')
    const [error, setError] = useState('')
    const [grupoSel, setGrupoSel] = useState<Row | null>(null)
    const [members, setMembers] = useState<Row[]>([])
    const [detail, setDetail] = useState<Row | null>(null)

    const load = async () => {
        setError('')
        try { setRows(await get(`/${active}`)) }
        catch (e) { setError(mensajeError(e, 'No fue posible cargar la información.')); return }

        const pedidos: Record<string, () => Promise<Row[]>> = {}
        if (active === 'usuarios') pedidos.roles = () => get('/roles')
        if (active === 'materias') pedidos.docentes = cargarDocentes
        if (active === 'secciones') pedidos.materias = () => get('/materias')
        if (active === 'grupos') pedidos.asignaciones = () => get('/asignaciones')
        if (active === 'proyectos') { pedidos.grupos = () => get('/grupos'); pedidos.empresas = () => get('/empresas') }
        if (active === 'criterios') {
            pedidos.materias = () => get('/materias'); pedidos.secciones = () => get('/secciones')
            pedidos.grupos = () => get('/grupos'); pedidos.proyectos = () => get('/proyectos')
        }
        const claves = Object.keys(pedidos)
        if (claves.length === 0) return
        const resultados = await Promise.allSettled(claves.map((k) => pedidos[k]()))
        const nuevos: Record<string, Row[]> = {}
        const fallos: string[] = []
        resultados.forEach((r, i) => {
            if (r.status === 'fulfilled') nuevos[claves[i]] = r.value
            else {
                const status = axios.isAxiosError(r.reason) ? r.reason.response?.status : undefined
                fallos.push(`${claves[i]}${status ? ` (${status})` : ''}`)
            }
        })
        setLookups(nuevos)
        if (fallos.length) setError(`No fue posible cargar: ${fallos.join(', ')}.`)
    }

    useEffect(() => { void load() }, [])

    // ---------- opciones de los selects ----------
    const opciones = (arr: Row[] | undefined, etiqueta: (x: Row) => string): [string, string][] =>
        (arr ?? []).map((x) => [String(x.id), etiqueta(x)])
    const etiquetaSeccion = (x: Row) => `${x.nombre} · ${x.ciclo}${x.materiaNombre ? ` · ${x.materiaNombre}` : ''}`
    const etiquetaAsignacion = (x: Row) =>
        `${x.materiaNombre ?? x.materia ?? 'Materia'} · ${x.seccionNombre ?? x.seccion ?? 'Sección'}${x.ciclo ? ` · ${x.ciclo}` : ''}${x.subseccionLetra ? ` · ${x.subseccionLetra}` : ''} · ${x.docenteNombre ?? x.docente ?? 'Docente'}`
    const nivelOpciones: Record<string, [string, string][]> = {
        MATERIA: opciones(lookups.materias, (x) => x.nombre),
        SECCION: opciones(lookups.secciones, etiquetaSeccion),
        GRUPO: opciones(lookups.grupos, (x) => x.nombre),
        PROYECTO: opciones(lookups.proyectos, (x) => x.titulo),
    }

    const camposPor: Record<Module, Campo[]> = {
        usuarios: [
            { key: 'nombre', label: 'Nombre', required: true },
            { key: 'apellido', label: 'Apellido', required: true },
            { key: 'correo', label: 'Correo', type: 'email', required: true },
            { key: 'password', label: editing ? 'Contraseña (déjala vacía para no cambiarla)' : 'Contraseña', type: 'password', required: !editing },
            { key: 'rolId', label: 'Rol', required: true, options: opciones((lookups.roles ?? []).filter((r) => String(r.nombre).toUpperCase() !== 'ADMIN'), (x) => x.nombre) },
        ],
        materias: [
            { key: 'nombre', label: 'Nombre', required: true },
            { key: 'descripcion', label: 'Descripción' },
            { key: 'docentePrincipalId', label: 'Docente principal', required: true, options: opciones(lookups.docentes, (x) => texto(x)) },
        ],
        secciones: [
            { key: 'nombre', label: 'Sección (ej. A)', required: true },
            { key: 'ciclo', label: 'Ciclo (ej. 02-2026)', required: true },
            { key: 'materiaId', label: 'Materia', required: true, options: opciones(lookups.materias, (x) => x.nombre) },
        ],
        grupos: [
            { key: 'nombre', label: 'Nombre', required: true },
            { key: 'asignacionId', label: 'Asignación', required: true, options: opciones(lookups.asignaciones, etiquetaAsignacion) },
        ],
        empresas: [
            { key: 'nombre', label: 'Nombre', required: true },
            { key: 'contacto', label: 'Contacto' },
            { key: 'correo', label: 'Correo', type: 'email' },
            { key: 'telefono', label: 'Teléfono' },
        ],
        proyectos: [
            { key: 'titulo', label: 'Título', required: true },
            { key: 'descripcion', label: 'Descripción' },
            { key: 'grupoId', label: 'Grupo', required: true, options: opciones(lookups.grupos, (x) => `${x.nombre}${x.seccionNombre ? ` · ${x.seccionNombre}` : ''}`) },
            { key: 'empresaId', label: 'Empresa', options: opciones(lookups.empresas, (x) => x.nombre) },
            { key: 'estado', label: 'Estado', options: [['ACTIVO', 'Activo'], ['FINALIZADO', 'Finalizado'], ['CANCELADO', 'Cancelado']] },
        ],
        criterios: [
            { key: 'nombre', label: 'Nombre', required: true },
            { key: 'descripcion', label: 'Descripción' },
            { key: 'ponderacion', label: 'Ponderación (%)', type: 'number', step: '0.01', required: true },
            { key: 'nivel', label: 'Se aplica a', required: true, disabled: editing !== null, options: Object.entries(etiquetaNivel) },
            { key: 'nivelId', label: 'Cuál', required: true, disabled: editing !== null || !form.nivel, options: nivelOpciones[String(form.nivel)] ?? [] },
        ],
    }
    const campos = camposPor[active]

    const setField = (key: string, value: string) =>
        setForm((actual) => ({ ...actual, [key]: value, ...(key === 'nivel' ? { nivelId: '' } : {}) }))

    // ---------- acciones ----------
    const submit = async (event: FormEvent) => {
        event.preventDefault()
        setError(''); setNotice('')
        if (active === 'grupos' && (!String(form.nombre ?? '').trim() || !form.asignacionId)) {
            setError('El nombre y la asignación son obligatorios.')
            return
        }
        const payload: Row = {}
        for (const [k, v] of Object.entries(form)) if (v !== '' && v !== null && v !== undefined) payload[k] = v
        for (const k of idsNumericos) if (payload[k] !== undefined) payload[k] = Number(payload[k])
        if (active === 'criterios') {
            const clave = claveNivel[String(form.nivel)]
            delete payload.nivel; delete payload.nivelId
            if (clave) payload[clave] = Number(form.nivelId)
        }
        if (active === 'usuarios' && !editing) delete payload.activo
        try {
            await (editing ? api.put(`/${active}/${editing}`, payload) : api.post(`/${active}`, payload))
            setForm(emptyForms[active]); setEditing(null)
            setNotice('Cambios guardados correctamente.')
            await load()
        } catch (e) { setError(mensajeError(e, 'No se pudo guardar. Revisa los campos e inténtalo nuevamente.')) }
    }

    const edit = (row: Row) => {
        const base: Row = {}
        for (const key of Object.keys(emptyForms[active])) base[key] = row[key] ?? ''
        if (active === 'usuarios') {
            const rol = (lookups.roles ?? []).find((r) => String(r.nombre).toUpperCase() === texto(row.rol).toUpperCase())
            base.rolId = rol ? String(rol.id) : ''
            base.activo = row.activo ?? true
            base.password = ''
        }
        if (active === 'grupos') base.asignacionId = String(row.asignacionId ?? '')
        if (active === 'criterios') { base.nivel = row.nivel ?? ''; base.nivelId = String(row.nivelId ?? '') }
        setEditing(row.id); setForm(base)
        window.scrollTo({ top: 0, behavior: 'smooth' })
    }

    const remove = async (id: number) => {
        if (!window.confirm('¿Deseas eliminar este registro?')) return
        setError(''); setNotice('')
        try { await api.delete(`/${active}/${id}`); setNotice('Registro eliminado.'); await load() }
        catch (e) { setError(mensajeError(e, 'No se pudo eliminar el registro.')) }
    }

    // ---------- integrantes de un grupo ----------
    const cargarIntegrantes = async (g: Row) => {
        try {
            const m = await api.get(`/grupos/${g.id}/alumnos`)
            setMembers(listData(m.data))
        } catch (e) { setError(mensajeError(e, 'No fue posible cargar los integrantes.')) }
    }
    const abrirGrupo = (g: Row) => { setGrupoSel(g); void cargarIntegrantes(g) }
    const removeMember = async (alumnoId: number) => {
        if (!grupoSel) return
        try { await api.delete(`/grupos/${grupoSel.id}/alumnos/${alumnoId}`); await cargarIntegrantes(grupoSel); await load() }
        catch (e) { setError(mensajeError(e, 'No se pudo quitar el alumno.')) }
    }

    // ---------- tabla ----------
    const nombreDe = (arr: Row[] | undefined, id: any) => {
        const x = (arr ?? []).find((i) => i.id === id)
        return x ? (x.titulo ?? x.nombre) : ''
    }
    const nombreNivel = (r: Row) => {
        const arr = r.nivel === 'MATERIA' ? lookups.materias : r.nivel === 'SECCION' ? lookups.secciones
            : r.nivel === 'GRUPO' ? lookups.grupos : lookups.proyectos
        return `${etiquetaNivel[r.nivel] ?? r.nivel}: ${nombreDe(arr, r.nivelId) || `#${r.nivelId}`}`
    }

    const columnas: Record<Module, { th: string; td: (r: Row) => ReactNode }[]> = {
        usuarios: [
            { th: 'Nombre', td: (r) => `${r.nombre ?? ''} ${r.apellido ?? ''}` },
            { th: 'Correo', td: (r) => r.correo },
            { th: 'Rol', td: (r) => texto(r.rol) },
        ],
        materias: [
            { th: 'Materia', td: (r) => <><strong>{r.nombre}</strong><small>{r.descripcion}</small></> },
            { th: 'Docente', td: (r) => texto(r.docentePrincipalNombre ?? r.docentePrincipal) },
        ],
        secciones: [
            { th: 'Sección', td: (r) => r.nombre },
            { th: 'Ciclo', td: (r) => r.ciclo },
            { th: 'Materia', td: (r) => r.materiaNombre ?? nombreDe(lookups.materias, r.materiaId) },
        ],
        grupos: [
            { th: 'Grupo', td: (r) => r.nombre },
            { th: 'Asignación', td: (r) => r.asignacionNombre || `#${r.asignacionId}` },
            { th: 'Alumnos', td: (r) => r.totalAlumnos },
        ],
        empresas: [
            { th: 'Empresa', td: (r) => r.nombre },
            { th: 'Contacto', td: (r) => r.contacto || '—' },
            { th: 'Correo', td: (r) => r.correo || '—' },
        ],
        proyectos: [
            { th: 'Proyecto', td: (r) => <><button className="project-link" onClick={() => setDetail(r)}><strong>{r.titulo}</strong></button><small>{r.empresaNombre || 'Sin empresa'}</small></> },
            { th: 'Grupo', td: (r) => r.grupoNombre },
            { th: 'Estado', td: (r) => <span className="status-tag">{r.estado}</span> },
        ],
        criterios: [
            { th: 'Criterio', td: (r) => <>{r.nombre}<small>{r.descripcion}</small></> },
            { th: 'Se aplica a', td: (r) => nombreNivel(r) },
            { th: 'Ponderación', td: (r) => `${r.ponderacion}%` },
        ],
    }

    const totales = (() => {
        if (active !== 'criterios') return []
        const mapa = new Map<string, { label: string; suma: number }>()
        for (const r of rows) {
            const key = `${r.nivel}:${r.nivelId}`
            const actual = mapa.get(key) ?? { label: nombreNivel(r), suma: 0 }
            actual.suma = Math.round((actual.suma + Number(r.ponderacion ?? 0)) * 100) / 100
            mapa.set(key, actual)
        }
        return [...mapa.entries()].map(([key, v]) => ({ key, ...v }))
    })()

    return (
        <Shell eyebrow="GESTIÓN ACADÉMICA" title={meta.title} description={meta.description}>
            {error && <p className="dashboard-alert">{error}</p>}
            {notice && <p className="dashboard-success">{notice}</p>}

            <div className="gestion-layout">
                <section className="panel form-panel">
                    <div className="panel-heading">
                        <div><span className="panel-kicker">{editing ? 'EDITAR REGISTRO' : 'NUEVO REGISTRO'}</span><h2>{editing ? 'Actualizar información' : 'Agregar registro'}</h2></div>
                        <Plus size={19} />
                    </div>
                    <form onSubmit={submit}>
                        {campos.map((c) => (
                            <label className="gestion-field" key={c.key}>
                                <span>{c.label}</span>
                                {c.options ? (
                                    <select required={c.required} disabled={c.disabled} value={form[c.key] ?? ''} onChange={(e) => setField(c.key, e.target.value)}>
                                        <option value="">Selecciona una opción</option>
                                        {c.options.map(([value, label]) => <option value={value} key={value}>{label}</option>)}
                                    </select>
                                ) : (
                                    <input required={c.required} type={c.type ?? 'text'} step={c.step} value={form[c.key] ?? ''} onChange={(e) => setField(c.key, e.target.value)} />
                                )}
                            </label>
                        ))}
                        {active === 'criterios' && <p className="nota-info">Se usa el nivel más específico cuya suma sea 100 %: proyecto → grupo → sección → materia.</p>}
                        {active === 'grupos' && (lookups.asignaciones ?? []).length === 0 && (
                            <p className="nota-info">No hay asignaciones disponibles. Primero debe crearse una asignación.</p>
                        )}
                        <div className="form-actions">
                            <button className="primary-button" type="submit"><Save size={16} /> {editing ? 'Actualizar' : 'Guardar'}</button>
                            {editing && <button className="secondary-button" type="button" onClick={() => { setEditing(null); setForm(emptyForms[active]) }}><X size={16} /> Cancelar</button>}
                        </div>
                    </form>
                </section>

                <section className="panel table-panel">
                    <div className="panel-heading"><div><span className="panel-kicker">LISTADO</span><h2>{rows.length} registros</h2></div></div>

                    {active === 'criterios' && totales.length > 0 && (
                        <div className="level-totals">
                            {totales.map((t) => (
                                <div key={t.key} className={`criteria-total ${t.suma > 100 ? 'over' : t.suma === 100 ? '' : 'pend'}`}>
                                    {t.label}: <strong>{t.suma.toFixed(2)}%</strong>{' '}
                                    {t.suma === 100 ? '— completo' : t.suma > 100 ? '— excede el 100%' : `— faltan ${(100 - t.suma).toFixed(2)}%`}
                                </div>
                            ))}
                        </div>
                    )}

                    <div className="table-wrap">
                        <table>
                            <thead><tr>{columnas[active].map((c) => <th key={c.th}>{c.th}</th>)}<th>Acciones</th></tr></thead>
                            <tbody>
                            {rows.map((row) => (
                                <tr key={row.id}>
                                    {columnas[active].map((c) => <td key={c.th}>{c.td(row)}</td>)}
                                    <td>
                                        <div className="row-actions">
                                            <button onClick={() => edit(row)} title="Editar"><Edit3 size={15} /></button>
                                            <button onClick={() => void remove(row.id)} title="Eliminar"><Trash2 size={15} /></button>
                                            {active === 'grupos' && <button onClick={() => abrirGrupo(row)} title="Integrantes"><Users size={15} /></button>}
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                        {rows.length === 0 && <div className="empty-state"><Icon size={28} /><strong>No hay registros todavía</strong><p>Agrega el primero usando el formulario.</p></div>}
                    </div>
                </section>
            </div>

            {active === 'grupos' && grupoSel && (
                <section className="panel members-panel">
                    <div className="panel-heading">
                        <div><span className="panel-kicker">INTEGRANTES</span><h2>{grupoSel.nombre}</h2></div>
                        <button className="icon-button" onClick={() => setGrupoSel(null)}><X size={18} /></button>
                    </div>
                    <div className="members-grid">
                        <div>
                            <h3>Alumnos asignados</h3>
                            {members.length === 0 && <p className="nota-info">Aún no hay alumnos en este grupo.</p>}
                            {members.map((m) => <div className="member-row" key={m.id}>{m.nombre} {m.apellido}<button onClick={() => void removeMember(m.id)}><X size={15} /></button></div>)}
                        </div>
                        <AlumnosDisponibles grupoId={grupoSel.id} onAgregado={() => void cargarIntegrantes(grupoSel)} />
                    </div>
                </section>
            )}

            {active === 'proyectos' && detail && (
                <section className="panel members-panel project-detail">
                    <div className="panel-heading">
                        <div><span className="panel-kicker">DETALLE DEL PROYECTO</span><h2>{detail.titulo}</h2></div>
                        <button className="icon-button" onClick={() => setDetail(null)}><X size={18} /></button>
                    </div>
                    <div className="detail-grid">
                        <div><span>Descripción</span><strong>{detail.descripcion || 'Sin descripción'}</strong></div>
                        <div><span>Grupo</span><strong>{detail.grupoNombre || 'Sin grupo'}</strong></div>
                        <div><span>Empresa</span><strong>{detail.empresaNombre || 'Sin empresa'}</strong></div>
                        <div><span>Estado</span><strong>{detail.estado || 'Sin estado'}</strong></div>
                        <div><span>Resultados</span><strong>{detail.resultadosPublicados ? 'Publicados' : 'Sin publicar'}</strong></div>
                    </div>
                </section>
            )}
        </Shell>
    )
}