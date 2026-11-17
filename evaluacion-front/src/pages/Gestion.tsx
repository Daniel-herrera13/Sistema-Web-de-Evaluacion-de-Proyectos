import { useEffect, useState, type FormEvent, type ReactNode } from 'react'
import { Navigate, useParams } from 'react-router-dom'
import axios from 'axios'
import { BookOpen, Building2, ClipboardCheck, Copy, Edit3, Eye, EyeOff, FileText, GraduationCap, Layers, Plus, Save, Trash2, Users, X } from 'lucide-react'
import api from '../api/axios'
import Shell from '../components/Shell'
import AlumnosDisponibles from '../components/AlumnosDisponibles'
import { useAuth, type Rol } from '../context/AuthContext'
import { useLanguage } from '../context/LanguageContext'
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
    pattern?: string
    placeholder?: string
    maxLength?: number
    options?: [string, string][]
}

const modules: Record<Module, { title: string; description: string; icon: Icono; roles: Rol[] }> = {
    usuarios: { title: 'Usuarios', description: 'Administra docentes, jurado y alumnos.', icon: Users, roles: ['ADMIN'] },
    materias: { title: 'Materias', description: 'Organiza las materias y su docente principal.', icon: BookOpen, roles: ['ADMIN', 'DOCENTE'] },
    secciones: { title: 'Secciones', description: 'Crea las secciones por ciclo, sus subsecciones (A, B, C) y asigna materias y docentes.', icon: Layers, roles: ['ADMIN', 'DOCENTE'] },
    grupos: { title: 'Alumnos y grupos', description: 'Crea grupos y asigna integrantes disponibles.', icon: GraduationCap, roles: ['ADMIN', 'DOCENTE'] },
    empresas: { title: 'Empresas', description: 'Registra las empresas vinculadas a los proyectos.', icon: Building2, roles: ['ADMIN', 'DOCENTE'] },
    proyectos: { title: 'Proyectos', description: 'Registra proyectos y consulta su información.', icon: FileText, roles: ['ADMIN', 'DOCENTE'] },
    criterios: { title: 'Criterios de evaluación', description: 'Define ponderaciones por materia, sección, grupo o proyecto, y por ciclo.', icon: ClipboardCheck, roles: ['ADMIN', 'DOCENTE'] },
}

const emptyForms: Record<Module, Row> = {
    usuarios: { nombre: '', apellido: '', correo: '', password: '', rolId: '', activo: true },
    materias: { nombre: '', descripcion: '', docentePrincipalId: '' },
    secciones: { nombre: '', ciclo: '' },
    grupos: { nombre: '', asignacionId: '' },
    empresas: { nombre: '', contacto: '', correo: '', telefono: '' },
    proyectos: { titulo: '', descripcion: '', grupoId: '', empresaId: '', estado: 'ACTIVO' },
    criterios: { nombre: '', descripcion: '', ponderacion: '', nivel: '', nivelId: '', ciclo: '' },
}

const etiquetaNivel: Record<string, string> = { MATERIA: 'Materia', ASIGNACION: 'Materia en subsección', GRUPO: 'Grupo', PROYECTO: 'Proyecto' }
const claveNivel: Record<string, string> = { MATERIA: 'materiaId', ASIGNACION: 'asignacionId', GRUPO: 'grupoId', PROYECTO: 'proyectoId' }
const ordenNivel: Record<string, number> = { MATERIA: 0, ASIGNACION: 1, GRUPO: 2, PROYECTO: 3 }
const claseNivel = (nivel: string) => `scope-${String(nivel ?? '').toLowerCase()}`
const idsNumericos = ['docentePrincipalId', 'asignacionId', 'seccionId', 'grupoId', 'empresaId', 'materiaId', 'proyectoId', 'rolId', 'ponderacion']

// Un conjunto de criterios = mismo nivel + mismo ciclo (ciclo vacío = conjunto general)
const claveConjunto = (r: Row) => `${r.nivel}:${r.nivelId}:${r.ciclo ?? ''}`

const asigVacia = { subseccionId: '', materiaId: '', docenteId: '' }

const listData = (data: any): Row[] => (Array.isArray(data) ? data : data?.content ?? [])
const get = async (url: string): Promise<Row[]> => listData((await api.get(url)).data)
const texto = (v: any): string =>
    v == null ? '' : typeof v === 'object' ? `${v.nombre ?? ''} ${v.apellido ?? ''}`.trim() : String(v)
const normalizarBusqueda = (v: string) => v.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
const etiquetaRol: Record<string, string> = { ADMIN: 'Administrador', DOCENTE: 'Docente', JURADO: 'Jurado', ALUMNO: 'Alumno' }

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
    const { usuario } = useAuth()
    const { language } = useLanguage()
    const meta = modules[active]
    const Icon = meta.icon
    const [rows, setRows] = useState<Row[]>([])
    const [lookups, setLookups] = useState<Record<string, Row[]>>({})
    const [ciclosActivos, setCiclosActivos] = useState<Row[]>([])
    const [conjuntoParaDuplicar, setConjuntoParaDuplicar] = useState<{ nivel: string; nivelId: number; ciclo: string } | null>(null)
    const [cicloDestino, setCicloDestino] = useState('')
    const [form, setForm] = useState<Row>(emptyForms[active])
    const [mostrarPasswordUsuario, setMostrarPasswordUsuario] = useState(false)
    const [editing, setEditing] = useState<number | null>(null)
    const [notice, setNotice] = useState('')
    const [error, setError] = useState('')
    const [nivelFiltro, setNivelFiltro] = useState<string | null>(null)
    const [busquedaUsuarios, setBusquedaUsuarios] = useState('')
    const [rolFiltroUsuarios, setRolFiltroUsuarios] = useState('TODOS')
    const [grupoSel, setGrupoSel] = useState<Row | null>(null)
    const [members, setMembers] = useState<Row[]>([])
    const [detail, setDetail] = useState<Row | null>(null)
    // criterios: conjuntos (nivel + ciclo) que ya se usaron en evaluaciones y no se pueden cambiar
    const [usos, setUsos] = useState<Record<string, boolean>>({})

    // subsecciones y asignaciones de una sección
    const [seccionSel, setSeccionSel] = useState<Row | null>(null)
    const [subsecciones, setSubsecciones] = useState<Row[]>([])
    const [letra, setLetra] = useState('')
    const [asignaciones, setAsignaciones] = useState<Row[]>([])
    const [listaMaterias, setListaMaterias] = useState<Row[]>([])
    const [listaDocentes, setListaDocentes] = useState<Row[]>([])
    const [asig, setAsig] = useState(asigVacia)

    // consulta, por cada nivel con criterios, qué conjuntos (ciclos) ya se usaron
    const cargarUsos = async (filas: Row[]) => {
        const niveles = new Map<string, Row>()
        for (const r of filas) niveles.set(`${r.nivel}:${r.nivelId}`, r)
        const resultados = await Promise.allSettled(
            [...niveles.values()].map((r) =>
                api.get('/criterios/conjuntos', { params: { [claveNivel[r.nivel]]: r.nivelId } })
                    .then((res) => ({ r, conjuntos: listData(res.data) }))),
        )
        const mapa: Record<string, boolean> = {}
        for (const x of resultados) {
            if (x.status !== 'fulfilled') continue
            for (const c of x.value.conjuntos) mapa[`${x.value.r.nivel}:${x.value.r.nivelId}:${c.ciclo ?? ''}`] = Boolean(c.enUso)
        }
        setUsos(mapa)
    }

    const load = async () => {
        setError('')
        let filas: Row[]
        try { filas = await get(`/${active}`); setRows(filas) }
        catch (e) { setError(mensajeError(e, 'No fue posible cargar la información.')); return }
        if (active === 'criterios') void cargarUsos(filas)

        const pedidos: Record<string, () => Promise<Row[]>> = {}
        if (active === 'usuarios') pedidos.roles = () => get('/roles')
        if (active === 'materias') pedidos.docentes = cargarDocentes
        if (active === 'grupos') pedidos.asignaciones = () => get('/asignaciones')
        if (active === 'proyectos') { pedidos.grupos = () => get('/grupos'); pedidos.empresas = () => get('/empresas') }
        if (active === 'criterios') {
            pedidos.materias = () => get('/materias'); pedidos.asignaciones = () => get('/asignaciones')
            pedidos.grupos = () => get('/grupos'); pedidos.proyectos = () => get('/proyectos')
        }
        if (active === 'secciones' || active === 'criterios') {
            try { setCiclosActivos(listData((await api.get('/ciclos', { params: { activos: true } })).data)) }
            catch { setCiclosActivos([]) }
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
    const etiquetaAsignacion = (x: Row) =>
        `${x.materiaNombre ?? x.materia ?? 'Materia'} · ${x.seccionNombre ?? x.seccion ?? 'Sección'}${x.subseccionLetra ?? ''}${x.ciclo ? ` · ${x.ciclo}` : ''} · ${x.docenteNombre ?? x.docente ?? 'Docente'}`
    const etiquetaGrupo = (x: Row) => `${x.nombre}${x.asignacionNombre ? ` · ${x.asignacionNombre}` : ''}`
    const nivelOpciones: Record<string, [string, string][]> = {
        MATERIA: opciones(lookups.materias, (x) => x.nombre),
        ASIGNACION: opciones(lookups.asignaciones, etiquetaAsignacion),
        GRUPO: opciones(lookups.grupos, etiquetaGrupo),
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
            { key: 'nombre', label: 'Sección (ej. SOFT41)', required: true },
            { key: 'ciclo', label: 'Ciclo', required: true, options: opciones(ciclosActivos, (x) => x.codigo) },
        ],
        grupos: [
            { key: 'nombre', label: 'Nombre', required: true },
            { key: 'asignacionId', label: 'Asignación', required: true, options: opciones(lookups.asignaciones, etiquetaAsignacion) },
        ],
        empresas: [
            { key: 'nombre', label: 'Nombre', required: true },
            { key: 'contacto', label: 'Contacto' },
            { key: 'correo', label: 'Correo', type: 'email' },
            { key: 'telefono', label: 'Teléfono (8 dígitos)', type: 'tel', pattern: '(?:\\d{8}|\\d{4}-\\d{4})', placeholder: '6620-5555 o 66205555', maxLength: 9 },
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
            { key: 'ciclo', label: 'Ciclo (opcional)', disabled: editing !== null, options: opciones(ciclosActivos, (x) => x.codigo) },
        ],
    }
    const campos = camposPor[active]

    const setField = (key: string, value: string) =>
        setForm((actual) => ({ ...actual, [key]: value, ...(key === 'nivel' ? { nivelId: '' } : {}) }))

    const cambiarCampo = (key: string, value: string) => {
        if (active === 'empresas' && key === 'telefono') {
            const digitos = value.replace(/\D/g, '').slice(0, 8)
            const telefono = digitos.length > 4 ? `${digitos.slice(0, 4)}-${digitos.slice(4)}` : digitos
            setField(key, telefono)
            return
        }
        setField(key, value)
    }

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
        if (active === 'criterios') { base.nivel = row.nivel ?? ''; base.nivelId = String(row.nivelId ?? ''); base.ciclo = row.ciclo ?? '' }
        setEditing(row.id); setForm(base)
        window.scrollTo({ top: 0, behavior: 'smooth' })
    }

    const remove = async (id: number) => {
        if (!window.confirm(language === 'en' ? 'Delete this record?' : '¿Deseas eliminar este registro?')) return
        setError(''); setNotice('')
        try { await api.delete(`/${active}/${id}`); setNotice('Registro eliminado.'); await load() }
        catch (e) { setError(mensajeError(e, 'No se pudo eliminar el registro.')) }
    }

    // ---------- criterios: copiar un conjunto a otro ciclo ----------
    const duplicarConjunto = async () => {
        const t = conjuntoParaDuplicar
        const destino = cicloDestino
        if (!t || !destino) return
        setError(''); setNotice('')
        try {
            await api.post('/criterios/duplicar', {
                [claveNivel[t.nivel]]: t.nivelId,
                cicloOrigen: t.ciclo || null,
                cicloDestino: destino,
            })
            setNotice(`Conjunto duplicado al ciclo ${destino}. Ya puedes editarlo ahí.`)
            setConjuntoParaDuplicar(null); setCicloDestino('')
            await load()
        } catch (e) { setError(mensajeError(e, 'No se pudo duplicar el conjunto.')) }
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

    // ---------- subsecciones y asignaciones de una sección ----------
    const cargarSubsecciones = async (s: Row) => {
        try { setSubsecciones(await get(`/subsecciones/detalle?seccionId=${s.id}`)) }
        catch (e) { setError(mensajeError(e, 'No fue posible cargar las subsecciones.')) }
    }
    const cargarAsignaciones = async (s: Row) => {
        try { setAsignaciones(await get(`/asignaciones/detalle?seccionId=${s.id}`)) }
        catch (e) { setError(mensajeError(e, 'No fue posible cargar las materias de la sección.')) }
    }
    const abrirSeccion = async (s: Row) => {
        setSeccionSel(s); setLetra(''); setAsig(asigVacia)
        setError(''); setNotice('')
        void cargarSubsecciones(s)
        void cargarAsignaciones(s)
        try {
            const [materias, docentes] = await Promise.all([get('/materias'), cargarDocentes()])
            setListaMaterias(materias)
            setListaDocentes(docentes)
        } catch (e) { setError(mensajeError(e, 'No fue posible cargar materias y docentes.')) }
    }
    const refrescarSeccion = async (s: Row) => {
        await Promise.all([cargarSubsecciones(s), cargarAsignaciones(s), load()])
    }
    const agregarSubseccion = async (valor: string) => {
        if (!seccionSel) return
        const l = valor.trim().toUpperCase()
        if (!l) return
        setError(''); setNotice('')
        try {
            await api.post('/subsecciones', { seccionId: seccionSel.id, letra: l })
            setLetra('')
            setNotice(`Subsección ${seccionSel.nombre}${l} creada.`)
            await refrescarSeccion(seccionSel)
        } catch (e) { setError(mensajeError(e, 'No se pudo crear la subsección.')) }
    }
    const quitarSubseccion = async (s: Row) => {
        if (!seccionSel || !window.confirm(language === 'en' ? `Delete subsection ${s.nombre}?` : `¿Eliminar la subsección ${s.nombre}?`)) return
        setError(''); setNotice('')
        try {
            await api.delete(`/subsecciones/${s.id}`)
            setNotice('Subsección eliminada.')
            await refrescarSeccion(seccionSel)
        } catch (e) { setError(mensajeError(e, 'No se pudo eliminar la subsección.')) }
    }
    const agregarAsignacion = async () => {
        if (!seccionSel) return
        if (!asig.materiaId || !asig.docenteId) { setError('Elige la materia y el docente.'); return }
        setError(''); setNotice('')
        try {
            const { data } = await api.post('/asignaciones', {
                seccionId: seccionSel.id,
                subseccionId: asig.subseccionId ? Number(asig.subseccionId) : null,
                materiaId: Number(asig.materiaId),
                docenteId: Number(asig.docenteId),
            })
            setNotice(`Materia asignada en ${data.creadas} subsección(es)` + (data.omitidas ? ` (${data.omitidas} ya la tenían).` : '.'))
            await refrescarSeccion(seccionSel)
        } catch (e) { setError(mensajeError(e, 'No se pudo asignar la materia.')) }
    }
    const quitarAsignacion = async (a: Row) => {
        if (!seccionSel || !window.confirm(language === 'en' ? `Remove ${a.materiaNombre} from ${a.subseccion}?` : `¿Quitar ${a.materiaNombre} de ${a.subseccion}?`)) return
        setError(''); setNotice('')
        try {
            await api.delete(`/asignaciones/${a.id}`)
            setNotice('Asignación eliminada.')
            await refrescarSeccion(seccionSel)
        } catch (e) { setError(mensajeError(e, 'No se pudo quitar la asignación.')) }
    }

    // ---------- tabla ----------
    const nombreDe = (arr: Row[] | undefined, id: any) => {
        const x = (arr ?? []).find((i) => i.id === id)
        return x ? (x.titulo ?? x.nombre) : ''
    }
    const nombreNivel = (r: Row) => {
        let nombre = ''
        if (r.nivel === 'MATERIA') nombre = nombreDe(lookups.materias, r.nivelId)
        else if (r.nivel === 'PROYECTO') nombre = nombreDe(lookups.proyectos, r.nivelId)
        else if (r.nivel === 'ASIGNACION') {
            const x = (lookups.asignaciones ?? []).find((i) => i.id === r.nivelId)
            nombre = x ? etiquetaAsignacion(x) : ''
        } else if (r.nivel === 'GRUPO') {
            const x = (lookups.grupos ?? []).find((i) => i.id === r.nivelId)
            nombre = x ? etiquetaGrupo(x) : ''
        }
        return `${etiquetaNivel[r.nivel] ?? r.nivel}: ${nombre || `#${r.nivelId}`}`
    }

    const columnas: Record<Module, { th: string; td: (r: Row) => ReactNode }[]> = {
        usuarios: [
            { th: 'Nombre', td: (r) => `${r.nombre ?? ''} ${r.apellido ?? ''}` },
            { th: 'Correo', td: (r) => r.correo },
            { th: 'Rol', td: (r) => {
                const rol = texto(r.rol).trim().toUpperCase()
                return <span className={`role-badge role-${rol.toLowerCase()}`}>{etiquetaRol[rol] ?? texto(r.rol)}</span>
            } },
        ],
        materias: [
            { th: 'Materia', td: (r) => <><strong>{r.nombre}</strong><small>{r.descripcion}</small></> },
            { th: 'Docente', td: (r) => texto(r.docentePrincipalNombre ?? r.docentePrincipal) },
        ],
        secciones: [
            { th: 'Sección', td: (r) => r.nombre },
            { th: 'Ciclo', td: (r) => r.ciclo },
            { th: 'Materias', td: (r) => (r.materias ?? []).join(', ') || 'Sin materias asignadas' },
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
            { th: 'Se aplica a', td: (r) => <span className={`scope-badge ${claseNivel(r.nivel)}`}>{nombreNivel(r)}</span> },
            { th: 'Ciclo', td: (r) => r.ciclo || 'General' },
            { th: 'Ponderación', td: (r) => `${r.ponderacion}%` },
        ],
    }

    // un total por conjunto (nivel + ciclo)
    const totales = (() => {
        if (active !== 'criterios') return []
        const mapa = new Map<string, { label: string; suma: number; nivel: string; nivelId: number; ciclo: string }>()
        for (const r of rows) {
            const key = claveConjunto(r)
            const actual = mapa.get(key) ?? {
                label: `${nombreNivel(r)} · ${r.ciclo ? `ciclo ${r.ciclo}` : 'general'}`,
                suma: 0, nivel: r.nivel, nivelId: r.nivelId, ciclo: r.ciclo ?? '',
            }
            actual.suma = Math.round((actual.suma + Number(r.ponderacion ?? 0)) * 100) / 100
            mapa.set(key, actual)
        }
        return [...mapa.entries()].map(([key, v]) => ({ key, ...v }))
    })()

    const filasTabla = active === 'criterios'
        ? rows.filter((row) => !nivelFiltro || row.nivel === nivelFiltro).sort((a, b) =>
            (ordenNivel[a.nivel] ?? 99) - (ordenNivel[b.nivel] ?? 99)
            || nombreNivel(a).localeCompare(nombreNivel(b), 'es')
            || String(a.ciclo ?? '').localeCompare(String(b.ciclo ?? ''), 'es')
            || String(a.nombre ?? '').localeCompare(String(b.nombre ?? ''), 'es'))
        : active === 'usuarios'
            ? rows.filter((row) => {
                const nombreCorreo = normalizarBusqueda(`${row.nombre ?? ''} ${row.apellido ?? ''} ${row.correo ?? ''}`)
                const rol = texto(row.rol).trim().toUpperCase()
                return nombreCorreo.includes(normalizarBusqueda(busquedaUsuarios.trim()))
                    && (rolFiltroUsuarios === 'TODOS' || rol === rolFiltroUsuarios)
            })
        : rows
    const totalesVisibles = nivelFiltro ? totales.filter((total) => total.nivel === nivelFiltro) : totales

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
                                        <option value="">{active === 'criterios' && c.key === 'ciclo' ? 'General (sin ciclo)' : 'Selecciona una opción'}</option>
                                        {c.options.map(([value, label]) => <option value={value} key={value}>{label}</option>)}
                                    </select>
                                ) : c.key === 'password' && active === 'usuarios' ? (
                                    <div className="user-password-field">
                                        <input
                                            required={c.required}
                                            type={mostrarPasswordUsuario ? 'text' : 'password'}
                                            autoComplete={editing ? 'new-password' : 'new-password'}
                                            value={form[c.key] ?? ''}
                                            placeholder={editing ? 'Deja vacío para conservarla' : undefined}
                                            onChange={(e) => cambiarCampo(c.key, e.target.value)}
                                        />
                                        <button
                                            type="button"
                                            className="user-password-toggle"
                                            onClick={() => setMostrarPasswordUsuario((mostrar) => !mostrar)}
                                            aria-label={mostrarPasswordUsuario ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                                            title={mostrarPasswordUsuario ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                                        >
                                            {mostrarPasswordUsuario ? <EyeOff size={18} /> : <Eye size={18} />}
                                        </button>
                                    </div>
                                ) : (
                                    <input
                                        required={c.required}
                                        disabled={c.disabled}
                                        type={c.type ?? 'text'}
                                        step={c.step}
                                        pattern={c.pattern}
                                        maxLength={c.maxLength}
                                        placeholder={c.placeholder}
                                        title={c.key === 'telefono' ? 'Ingresa 8 dígitos, con guion opcional: 6620-5555' : c.pattern ? 'Formato NN-AAAA, por ejemplo 02-2026' : undefined}
                                        value={form[c.key] ?? ''}
                                        onChange={(e) => cambiarCampo(c.key, e.target.value)}
                                    />
                                )}
                            </label>
                        ))}
                        {active === 'criterios' && (
                            <p className="nota-info">
                                Se usa el nivel más específico cuya suma sea 100 %: proyecto → grupo → materia en subsección → materia.
                                En cada nivel se prefiere el conjunto del ciclo del proyecto y, si no existe, el general.
                                Un conjunto que ya se usó en evaluaciones no se puede cambiar: duplícalo a otro ciclo.
                            </p>
                        )}
                        {active === 'grupos' && (lookups.asignaciones ?? []).length === 0 && (
                            <p className="nota-info">No hay asignaciones disponibles. Créalas en Secciones, con el icono de capas.</p>
                        )}
                        <div className="form-actions">
                            <button className="primary-button" type="submit"><Save size={16} /> {editing ? 'Actualizar' : 'Guardar'}</button>
                            {editing && <button className="secondary-button" type="button" onClick={() => { setEditing(null); setForm(emptyForms[active]) }}><X size={16} /> Cancelar</button>}
                        </div>
                    </form>
                </section>

                <section className="panel table-panel">
                    <div className="panel-heading"><div><span className="panel-kicker">LISTADO</span><h2>{filasTabla.length} registros</h2></div></div>

                    {active === 'usuarios' && (
                        <div className="user-filters">
                            <label className="gestion-field">
                                <span>Buscar por nombre o correo</span>
                                <input type="search" value={busquedaUsuarios} onChange={(e) => setBusquedaUsuarios(e.target.value)} placeholder="Nombre, apellido o correo" />
                            </label>
                            <label className="gestion-field">
                                <span>Filtrar por rol</span>
                                <select value={rolFiltroUsuarios} onChange={(e) => setRolFiltroUsuarios(e.target.value)}>
                                    <option value="TODOS">Todos los roles</option>
                                    <option value="ADMIN">Administrador</option>
                                    <option value="DOCENTE">Docente</option>
                                    <option value="JURADO">Jurado</option>
                                    <option value="ALUMNO">Alumno</option>
                                </select>
                            </label>
                            <button type="button" className="secondary-button" onClick={() => { setBusquedaUsuarios(''); setRolFiltroUsuarios('TODOS') }} disabled={!busquedaUsuarios && rolFiltroUsuarios === 'TODOS'}>Limpiar filtros</button>
                        </div>
                    )}

                    {active === 'criterios' && totalesVisibles.length > 0 && (
                        <div className="level-totals">
                            {totalesVisibles.map((t) => (
                                <div key={t.key} className={`criteria-total ${claseNivel(t.nivel)} ${t.suma > 100 ? 'over' : t.suma === 100 ? '' : 'pend'}`}>
                                    {t.label}: <strong>{t.suma.toFixed(2)}%</strong>{' '}
                                    {t.suma === 100 ? '— completo' : t.suma > 100 ? '— excede el 100%' : `— faltan ${(100 - t.suma).toFixed(2)}%`}
                                    {usos[t.key] && <span className="tag tag-warn" style={{ marginLeft: 8 }}>En uso</span>}
                                    {t.suma === 100 && (
                                        <button type="button" className="preset-btn" style={{ marginLeft: 8 }} title="Copiar este conjunto a otro ciclo" onClick={() => { setConjuntoParaDuplicar(t); setCicloDestino('') }}>
                                            <Copy size={13} /> Duplicar a otro ciclo
                                        </button>
                                    )}
                                </div>
                            ))}
                        </div>
                    )}

                    {active === 'criterios' && (
                        <div className="criteria-legend" aria-label="Colores por alcance del criterio">
                            <button type="button" className={`scope-badge scope-materia ${nivelFiltro === 'MATERIA' ? 'is-selected' : ''}`} aria-pressed={nivelFiltro === 'MATERIA'} onClick={() => setNivelFiltro(nivelFiltro === 'MATERIA' ? null : 'MATERIA')}>Materia · estándar</button>
                            <button type="button" className={`scope-badge scope-asignacion ${nivelFiltro === 'ASIGNACION' ? 'is-selected' : ''}`} aria-pressed={nivelFiltro === 'ASIGNACION'} onClick={() => setNivelFiltro(nivelFiltro === 'ASIGNACION' ? null : 'ASIGNACION')}>Sección / subsección</button>
                            <button type="button" className={`scope-badge scope-grupo ${nivelFiltro === 'GRUPO' ? 'is-selected' : ''}`} aria-pressed={nivelFiltro === 'GRUPO'} onClick={() => setNivelFiltro(nivelFiltro === 'GRUPO' ? null : 'GRUPO')}>Grupo</button>
                            <button type="button" className={`scope-badge scope-proyecto ${nivelFiltro === 'PROYECTO' ? 'is-selected' : ''}`} aria-pressed={nivelFiltro === 'PROYECTO'} onClick={() => setNivelFiltro(nivelFiltro === 'PROYECTO' ? null : 'PROYECTO')}>Proyecto</button>
                            <button type="button" className={`scope-all ${nivelFiltro === null ? 'is-selected' : ''}`} aria-pressed={nivelFiltro === null} onClick={() => setNivelFiltro(null)}>Todos</button>
                            <small>Selecciona un color para filtrar por alcance. La lista va de lo general a lo específico.</small>
                        </div>
                    )}

                    {active === 'criterios' && conjuntoParaDuplicar && (
                        <div className="panel" role="group" aria-label="Duplicar criterios">
                            <h3>Duplicar criterios a otro ciclo</h3>
                            <label className="gestion-field"><span>Ciclo destino</span>
                                <select value={cicloDestino} onChange={(e) => setCicloDestino(e.target.value)}>
                                    <option value="">Selecciona un ciclo activo</option>
                                    {ciclosActivos.filter((c) => c.codigo !== conjuntoParaDuplicar.ciclo).map((c) => <option key={c.id} value={c.codigo}>{c.codigo}</option>)}
                                </select>
                            </label>
                            <div className="form-actions"><button type="button" className="primary-button" disabled={!cicloDestino} onClick={() => void duplicarConjunto()}><Copy size={15} /> Duplicar</button><button type="button" className="secondary-button" onClick={() => setConjuntoParaDuplicar(null)}>Cancelar</button></div>
                        </div>
                    )}

                    <div className="table-wrap">
                        <table>
                            <thead><tr>{columnas[active].map((c) => <th key={c.th}>{c.th}</th>)}<th>Acciones</th></tr></thead>
                            <tbody>
                            {filasTabla.map((row) => {
                                const bloqueado = active === 'criterios' && Boolean(usos[claveConjunto(row)]) && usuario?.rol !== 'ADMIN'
                                const aviso = 'Este conjunto ya se usó en evaluaciones: duplícalo a otro ciclo para cambiarlo'
                                return (
                                    <tr key={row.id} className={active === 'criterios' ? `criteria-row ${claseNivel(row.nivel)}` : undefined}>
                                        {columnas[active].map((c) => <td key={c.th}>{c.td(row)}</td>)}
                                        <td>
                                            <div className="row-actions">
                                                <button onClick={() => edit(row)} title={bloqueado ? aviso : 'Editar'} disabled={bloqueado}><Edit3 size={15} /></button>
                                                <button onClick={() => void remove(row.id)} title={bloqueado ? aviso : 'Eliminar'} disabled={bloqueado}><Trash2 size={15} /></button>
                                                {active === 'grupos' && <button onClick={() => abrirGrupo(row)} title="Integrantes"><Users size={15} /></button>}
                                                {active === 'secciones' && <button onClick={() => void abrirSeccion(row)} title="Subsecciones y materias"><Layers size={15} /></button>}
                                            </div>
                                        </td>
                                    </tr>
                                )
                            })}
                            </tbody>
                        </table>
                        {filasTabla.length === 0 && <div className="empty-state"><Icon size={28} /><strong>{active === 'usuarios' && (busquedaUsuarios || rolFiltroUsuarios !== 'TODOS') ? 'No hay usuarios que coincidan con estos filtros' : nivelFiltro ? 'No hay criterios para este alcance' : 'No hay registros todavía'}</strong>{active !== 'usuarios' && !nivelFiltro && <p>Agrega el primero usando el formulario.</p>}</div>}
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
                        <AlumnosDisponibles grupoId={grupoSel.id} onAgregado={() => { void cargarIntegrantes(grupoSel); void load() }} />
                    </div>
                </section>
            )}

            {active === 'secciones' && seccionSel && (
                <section className="panel members-panel">
                    <div className="panel-heading">
                        <div><span className="panel-kicker">SUBSECCIONES Y MATERIAS</span><h2>{seccionSel.nombre} · {seccionSel.ciclo}</h2></div>
                        <button className="icon-button" onClick={() => setSeccionSel(null)}><X size={18} /></button>
                    </div>
                    <div className="members-grid">
                        <div>
                            <h3>Subsecciones creadas</h3>
                            {subsecciones.length === 0 && <p className="nota-info">Esta sección aún no tiene subsecciones.</p>}
                            {subsecciones.map((s) => (
                                <div className="member-row" key={s.id}>
                                    <span>{s.nombre}</span>
                                    <button onClick={() => void quitarSubseccion(s)} title="Eliminar"><Trash2 size={15} /></button>
                                </div>
                            ))}
                        </div>
                        <div>
                            <h3>Agregar subsección</h3>
                            <div className="preset-row">
                                {['A', 'B', 'C'].filter((l) => !subsecciones.some((s) => s.letra === l)).map((l) => (
                                    <button type="button" className="preset-btn" key={l} onClick={() => void agregarSubseccion(l)}>
                                        {seccionSel.nombre}{l}
                                    </button>
                                ))}
                            </div>
                            <form onSubmit={(e) => { e.preventDefault(); void agregarSubseccion(letra) }} style={{ display: 'grid', gap: 10, marginTop: 14 }}>
                                <label className="gestion-field">
                                    <span>Otra letra (por ejemplo D)</span>
                                    <input maxLength={5} value={letra} onChange={(e) => setLetra(e.target.value)} />
                                </label>
                                <div className="form-actions">
                                    <button className="primary-button" type="submit"><Plus size={16} /> Agregar</button>
                                </div>
                            </form>
                        </div>
                    </div>

                    <div style={{ marginTop: 22 }}>
                        <h3>Materias y docentes</h3>
                        {asignaciones.length === 0 && <p className="nota-info">Esta sección aún no tiene materias asignadas.</p>}
                        {asignaciones.map((a) => (
                            <div className="member-row" key={a.id}>
                                <span>{a.materiaNombre} · {a.subseccion} · {a.docenteNombre}</span>
                                <button onClick={() => void quitarAsignacion(a)} title="Quitar"><Trash2 size={15} /></button>
                            </div>
                        ))}

                        <div style={{ display: 'grid', gap: 10, marginTop: 14 }}>
                            <label className="gestion-field">
                                <span>Materia</span>
                                <select value={asig.materiaId} onChange={(e) => setAsig({ ...asig, materiaId: e.target.value })}>
                                    <option value="">Selecciona…</option>
                                    {listaMaterias.map((m) => <option key={m.id} value={m.id}>{m.nombre}</option>)}
                                </select>
                            </label>
                            <label className="gestion-field">
                                <span>Docente</span>
                                <select value={asig.docenteId} onChange={(e) => setAsig({ ...asig, docenteId: e.target.value })}>
                                    <option value="">Selecciona…</option>
                                    {listaDocentes.map((d) => <option key={d.id} value={d.id}>{texto(d)}</option>)}
                                </select>
                            </label>
                            <label className="gestion-field">
                                <span>Subsección</span>
                                <select value={asig.subseccionId} onChange={(e) => setAsig({ ...asig, subseccionId: e.target.value })}>
                                    <option value="">Todas las subsecciones de la sección</option>
                                    {subsecciones.map((s) => <option key={s.id} value={s.id}>{s.nombre}</option>)}
                                </select>
                            </label>
                            <div className="form-actions">
                                <button className="primary-button" type="button" onClick={() => void agregarAsignacion()}>
                                    <Plus size={16} /> Asignar materia
                                </button>
                            </div>
                        </div>
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
