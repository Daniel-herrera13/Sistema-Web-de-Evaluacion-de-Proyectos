import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, BookOpen, Building2, ClipboardCheck, Edit3, FileText, GraduationCap, Plus, Save, Trash2, Users, X } from 'lucide-react'
import api from '../api/axios'
import { useAuth } from '../context/AuthContext'
import './Gestion.css'

type Module = 'usuarios' | 'materias' | 'grupos' | 'empresas' | 'proyectos' | 'criterios'
type Row = Record<string, any>
const modules: Record<Module, { title: string; description: string; icon: typeof Users }> = {
    usuarios: { title: 'Docentes y jurado', description: 'Administra los usuarios que participan en la evaluación.', icon: Users },
    materias: { title: 'Materias y secciones', description: 'Organiza las materias, ciclos y secciones académicas.', icon: BookOpen },
    grupos: { title: 'Alumnos y grupos', description: 'Crea grupos y asigna integrantes disponibles.', icon: GraduationCap },
    empresas: { title: 'Empresas', description: 'Registra las empresas vinculadas a los proyectos.', icon: Building2 },
    proyectos: { title: 'Proyectos', description: 'Registra proyectos y consulta su información detallada.', icon: FileText },
    criterios: { title: 'Criterios de evaluación', description: 'Define ponderaciones y revisa el porcentaje acumulado.', icon: ClipboardCheck },
}

const emptyForms: Record<Module, Row> = {
    usuarios: { nombre: '', apellido: '', correo: '', password: '', rol: 'DOCENTE' },
    materias: { nombre: '', descripcion: '', docentePrincipalId: '' },
    grupos: { nombre: '', seccionId: '' },
    empresas: { nombre: '', contacto: '', correo: '', telefono: '' },
    proyectos: { titulo: '', descripcion: '', grupoId: '', empresaId: '', estado: 'ACTIVO' },
    criterios: { nombre: '', descripcion: '', ponderacion: '', materiaId: '', seccionId: '', grupoId: '', proyectoId: '' },
}

function listData(data: any) { return Array.isArray(data) ? data : data?.content ?? [] }

export default function Gestion() {
    const { modulo = 'materias' } = useParams<{ modulo: Module }>()
    const active = (Object.keys(modules).includes(modulo) ? modulo : 'materias') as Module
    const navigate = useNavigate()
    const { usuario, logout } = useAuth()
    const [rows, setRows] = useState<Row[]>([])
    const [lookups, setLookups] = useState<Record<string, Row[]>>({})
    const [form, setForm] = useState<Row>(emptyForms[active])
    const [editing, setEditing] = useState<number | null>(null)
    const [notice, setNotice] = useState('')
    const [error, setError] = useState('')
    const [selectedGroup, setSelectedGroup] = useState<number | null>(null)
    const [members, setMembers] = useState<Row[]>([])
    const [available, setAvailable] = useState<Row[]>([])
    const [detailProject, setDetailProject] = useState<Row | null>(null)

    const load = async () => {
        setError('')
        try {
            const response = await api.get(`/${active}`)
            setRows(listData(response.data))
            const requests: Record<string, string> = {}
            if (active === 'materias') requests.usuarios = '/usuarios'
            if (active === 'grupos') requests.secciones = '/secciones'
            if (active === 'proyectos') { requests.grupos = '/grupos'; requests.empresas = '/empresas' }
            if (active === 'criterios') { requests.materias = '/materias'; requests.secciones = '/secciones'; requests.grupos = '/grupos'; requests.proyectos = '/proyectos' }
            const entries = await Promise.all(Object.entries(requests).map(async ([key, url]) => [key, listData((await api.get(url)).data)] as const))
            setLookups(Object.fromEntries(entries))
        } catch { setError('No fue posible cargar la información. Verifica que la API esté disponible.') }
    }

    useEffect(() => { setForm(emptyForms[active]); setEditing(null); void load() }, [active])
    useEffect(() => {
        if (active !== 'grupos' || selectedGroup === null) return
        Promise.all([api.get(`/grupos/${selectedGroup}/alumnos`), api.get('/alumnos')])
            .then(([group, all]) => { setMembers(listData(group.data)); setAvailable(listData(all.data)) })
            .catch(() => setError('No fue posible cargar los integrantes.'))
    }, [active, selectedGroup])

    const title = modules[active]
    const Icon = title.icon
    const initials = useMemo(() => (usuario?.nombre ?? 'Usuario').split(' ').map((x) => x[0]).slice(0, 2).join('').toUpperCase(), [usuario?.nombre])
    const setField = (key: string, value: string) => setForm((current) => ({ ...current, [key]: value }))
    const submit = async (event: FormEvent) => {
        event.preventDefault(); setError(''); setNotice('')
        const payload = Object.fromEntries(Object.entries(form).filter(([, value]) => value !== ''))
        for (const key of ['docentePrincipalId', 'seccionId', 'grupoId', 'empresaId', 'materiaId', 'proyectoId']) if (payload[key]) payload[key] = Number(payload[key])
        if (payload.ponderacion) payload.ponderacion = Number(payload.ponderacion)
        try {
            if (active === 'usuarios') {
                await (editing ? api.put(`/usuarios/${editing}`, payload) : api.post('/usuarios', payload))
            } else await (editing ? api.put(`/${active}/${editing}`, payload) : api.post(`/${active}`, payload))
            setForm(emptyForms[active]); setEditing(null); setNotice('Cambios guardados correctamente.'); await load()
        } catch { setError('No se pudo guardar. Revisa los campos e inténtalo nuevamente.') }
    }
    const edit = (row: Row) => {
        setEditing(row.id); setForm(Object.keys(emptyForms[active]).reduce((acc, key) => ({ ...acc, [key]: row[key] ?? '' }), {}))
        window.scrollTo({ top: 0, behavior: 'smooth' })
    }
    const remove = async (id: number) => {
        if (!window.confirm('¿Deseas eliminar este registro?')) return
        try { await api.delete(`/${active}/${id}`); setNotice('Registro eliminado.'); await load() } catch { setError('No se pudo eliminar el registro.') }
    }
    const addMember = async (alumnoId: number) => {
        if (selectedGroup === null) return
        try { await api.post(`/grupos/${selectedGroup}/alumnos`, { alumnoId }); setSelectedGroup(selectedGroup) } catch { setError('No se pudo asignar el alumno.') }
    }
    const removeMember = async (alumnoId: number) => {
        if (selectedGroup === null) return
        try { await api.delete(`/grupos/${selectedGroup}/alumnos/${alumnoId}`); setSelectedGroup(selectedGroup) } catch { setError('No se pudo quitar el alumno.') }
    }
    const fields: Record<Module, { key: string; label: string; type?: string; options?: [string, string][] }[]> = {
        usuarios: [{ key: 'nombre', label: 'Nombre' }, { key: 'apellido', label: 'Apellido' }, { key: 'correo', label: 'Correo', type: 'email' }, { key: 'password', label: 'Contraseña', type: 'password' }, { key: 'rol', label: 'Rol', options: [['DOCENTE', 'Docente'], ['JURADO', 'Jurado'], ['ALUMNO', 'Alumno']] }],
        materias: [{ key: 'nombre', label: 'Nombre' }, { key: 'descripcion', label: 'Descripción' }, { key: 'docentePrincipalId', label: 'Docente', options: (lookups.usuarios ?? []).filter((x) => x.rol?.toUpperCase() === 'DOCENTE').map((x) => [String(x.id), `${x.nombre} ${x.apellido ?? ''}`]) }],
        grupos: [{ key: 'nombre', label: 'Nombre' }, { key: 'seccionId', label: 'Sección', options: (lookups.secciones ?? []).map((x) => [String(x.id), `${x.nombre} · ${x.ciclo}`]) }],
        empresas: [{ key: 'nombre', label: 'Nombre' }, { key: 'contacto', label: 'Contacto' }, { key: 'correo', label: 'Correo', type: 'email' }, { key: 'telefono', label: 'Teléfono' }],
        proyectos: [{ key: 'titulo', label: 'Título' }, { key: 'descripcion', label: 'Descripción' }, { key: 'grupoId', label: 'Grupo', options: (lookups.grupos ?? []).map((x) => [String(x.id), x.nombre]) }, { key: 'empresaId', label: 'Empresa', options: (lookups.empresas ?? []).map((x) => [String(x.id), x.nombre]) }, { key: 'estado', label: 'Estado', options: [['ACTIVO', 'Activo'], ['FINALIZADO', 'Finalizado'], ['CANCELADO', 'Cancelado']] }],
        criterios: [{ key: 'nombre', label: 'Nombre' }, { key: 'descripcion', label: 'Descripción' }, { key: 'ponderacion', label: 'Ponderación (%)', type: 'number' }, { key: 'materiaId', label: 'Materia', options: (lookups.materias ?? []).map((x) => [String(x.id), x.nombre]) }, { key: 'seccionId', label: 'Sección', options: (lookups.secciones ?? []).map((x) => [String(x.id), x.nombre]) }, { key: 'grupoId', label: 'Grupo', options: (lookups.grupos ?? []).map((x) => [String(x.id), x.nombre]) }, { key: 'proyectoId', label: 'Proyecto', options: (lookups.proyectos ?? []).map((x) => [String(x.id), x.titulo]) }],
    }
    const criterionTotal = active === 'criterios' ? rows.reduce((sum, row) => sum + Number(row.ponderacion ?? 0), 0) : 0
    return <div className="app-shell gestion-shell">
        <aside className="sidebar">
            <div className="sidebar-top"><div className="brand"><div className="brand-mark"><GraduationCap size={24} /></div><div className="brand-text"><strong>Sistema Web de<br />Evaluación</strong><span>Regional Santa Ana · Ciclo 02–2026</span></div></div>
                <nav><p className="nav-heading">NAVEGACIÓN</p>{Object.entries(modules).map(([key, item]) => { const NavIcon = item.icon; return <button className={`nav-item ${key === active ? 'active' : ''}`} key={key} onClick={() => navigate(`/gestion/${key}`)}><NavIcon size={18} /><span>{item.title}</span></button> })}</nav>
            </div><div className="sidebar-bottom"><button className="logout-button" onClick={() => { logout(); navigate('/login') }}>Cerrar sesión</button><strong>ITCA-FEPADE</strong></div>
        </aside>
        <main className="dashboard-main"><header className="topbar"><button className="back-button" onClick={() => navigate('/dashboard')}><ArrowLeft size={18} /> Dashboard</button><div className="topbar-title">{title.title}</div><div className="user-chip"><span className="avatar">{initials}</span><span>{usuario?.rol ?? 'Usuario'}</span></div></header>
            <section className="dashboard-content"><div className="page-heading"><div><span className="page-eyebrow">GESTIÓN ACADÉMICA</span><h1>{title.title}</h1><p>{title.description}</p></div><div className="security-badge"><Icon size={16} /> Conectado a la API</div></div>
                {error && <p className="dashboard-alert">{error}</p>}{notice && <p className="dashboard-success">{notice}</p>}
                <div className="gestion-layout"><section className="panel form-panel"><div className="panel-heading"><div><span className="panel-kicker">{editing ? 'EDITAR REGISTRO' : 'NUEVO REGISTRO'}</span><h2>{editing ? 'Actualizar información' : 'Agregar registro'}</h2></div><Plus size={19} /></div><form onSubmit={submit}>{fields[active].map((field) => <label className="gestion-field" key={field.key}><span>{field.label}</span>{field.options ? <select required={['nombre', 'rol', 'docentePrincipalId', 'seccionId', 'grupoId', 'ponderacion'].includes(field.key)} value={form[field.key]} onChange={(e) => setField(field.key, e.target.value)}><option value="">Selecciona una opción</option>{field.options.map(([value, label]) => <option value={value} key={value}>{label}</option>)}</select> : <input required={['nombre', 'apellido', 'correo', 'password', 'titulo', 'ponderacion'].includes(field.key) && !(editing && field.key === 'password')} type={field.type ?? 'text'} value={form[field.key]} onChange={(e) => setField(field.key, e.target.value)} />}</label>)}<div className="form-actions"><button className="primary-button" type="submit"><Save size={16} /> {editing ? 'Actualizar' : 'Guardar'}</button>{editing && <button className="secondary-button" type="button" onClick={() => { setEditing(null); setForm(emptyForms[active]) }}><X size={16} /> Cancelar</button>}</div></form></section>
                    <section className="panel table-panel"><div className="panel-heading"><div><span className="panel-kicker">LISTADO</span><h2>{rows.length} registros</h2></div></div>{active === 'criterios' && <div className={`criteria-total ${criterionTotal > 100 ? 'over' : ''}`}>Porcentaje acumulado: <strong>{criterionTotal.toFixed(2)}%</strong> {criterionTotal > 100 && '(excede el 100%)'}</div>}<div className="table-wrap"><table><thead><tr>{active === 'usuarios' ? <><th>Nombre</th><th>Correo</th><th>Rol</th></> : active === 'materias' ? <><th>Materia</th><th>Docente</th></> : active === 'grupos' ? <><th>Grupo</th><th>Sección</th><th>Alumnos</th></> : active === 'empresas' ? <><th>Empresa</th><th>Contacto</th><th>Correo</th></> : active === 'proyectos' ? <><th>Proyecto</th><th>Grupo</th><th>Estado</th></> : <><th>Criterio</th><th>Nivel</th><th>Ponderación</th></>}{active !== 'criterios' || true ? <th>Acciones</th> : null}</tr></thead><tbody>{rows.map((row) => <tr key={row.id}>{active === 'usuarios' && <><td>{row.nombre} {row.apellido}</td><td>{row.correo}</td><td>{row.rol}</td></>}{active === 'materias' && <><td><strong>{row.nombre}</strong><small>{row.descripcion}</small></td><td>{row.docentePrincipal}</td></>}{active === 'grupos' && <><td>{row.nombre}</td><td>{row.seccionNombre}</td><td>{row.totalAlumnos}</td></>}{active === 'empresas' && <><td>{row.nombre}</td><td>{row.contacto || '—'}</td><td>{row.correo || '—'}</td></>                    }{active === 'proyectos' && <><td><button className="project-link" onClick={() => setDetailProject(row)}><strong>{row.titulo}</strong></button><small>{row.empresaNombre || 'Sin empresa'}</small></td><td>{row.grupoNombre}</td><td><span className="status-tag">{row.estado}</span></td></>}{active === 'criterios' && <><td>{row.nombre}<small>{row.descripcion}</small></td><td>{row.nivel || 'General'}</td><td>{row.ponderacion}%</td></>}<td><div className="row-actions"><button onClick={() => edit(row)} title="Editar"><Edit3 size={15} /></button><button onClick={() => remove(row.id)} title="Eliminar"><Trash2 size={15} /></button>{active === 'grupos' && <button onClick={() => setSelectedGroup(row.id)} title="Integrantes"><Users size={15} /></button>}</div></td></tr>)}</tbody></table>{rows.length === 0 && <div className="empty-state"><Icon size={28} /><strong>No hay registros todavía</strong><p>Agrega el primero usando el formulario.</p></div>}</div></section></div>
                {active === 'grupos' && selectedGroup !== null && <section className="panel members-panel"><div className="panel-heading"><div><span className="panel-kicker">INTEGRANTES</span><h2>Asignación de alumnos</h2></div><button className="icon-button" onClick={() => setSelectedGroup(null)}><X size={18} /></button></div><div className="members-grid"><div><h3>Alumnos asignados</h3>{members.map((member) => <div className="member-row" key={member.id}>{member.nombre} {member.apellido}<button onClick={() => removeMember(member.id)}><X size={15} /></button></div>)}</div><div><h3>Disponibles</h3>{available.filter((item) => !members.some((member) => member.id === item.id)).map((member) => <button className="available-row" key={member.id} onClick={() => addMember(member.id)}>{member.nombre} {member.apellido}<Plus size={15} /></button>)}</div></div></section>}
                {active === 'proyectos' && detailProject && <section className="panel members-panel project-detail"><div className="panel-heading"><div><span className="panel-kicker">DETALLE DEL PROYECTO</span><h2>{detailProject.titulo}</h2></div><button className="icon-button" onClick={() => setDetailProject(null)}><X size={18} /></button></div><div className="detail-grid"><div><span>Descripción</span><strong>{detailProject.descripcion || 'Sin descripción'}</strong></div><div><span>Grupo</span><strong>{detailProject.grupoNombre || 'Sin grupo'}</strong></div><div><span>Empresa</span><strong>{detailProject.empresaNombre || 'Sin empresa'}</strong></div><div><span>Estado</span><strong>{detailProject.estado || 'Sin estado'}</strong></div></div></section>}
            </section>
        </main>
    </div>
}
