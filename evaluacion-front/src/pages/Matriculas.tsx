import { useEffect, useMemo, useState } from 'react'
import { Trash2, UserPlus } from 'lucide-react'
// AJUSTAR: ruta de tu instancia de axios (la que ya agrega el token JWT).
// Se asume que su baseURL termina en /api (las rutas de abajo no llevan /api).
import api from '../api/axios'
import type { AlumnoBasico, Matricula, Seccion, Subseccion } from '../types/matricula'
import { mensajeError } from '../utils/errores'
import Shell from '../components/Shell'
import { useLanguage } from '../context/LanguageContext'

export default function Matriculas() {
    const { language } = useLanguage()
    const [secciones, setSecciones] = useState<Seccion[]>([])
    const [subsecciones, setSubsecciones] = useState<Subseccion[]>([])
    const [seccionId, setSeccionId] = useState('')
    const [subseccionId, setSubseccionId] = useState('')
    const [matriculados, setMatriculados] = useState<Matricula[]>([])
    const [sinMatricula, setSinMatricula] = useState<AlumnoBasico[]>([])
    const [seleccion, setSeleccion] = useState<Set<number>>(new Set())
    const [filtro, setFiltro] = useState('')
    const [error, setError] = useState('')
    const [ok, setOk] = useState('')
    const [guardando, setGuardando] = useState(false)

    useEffect(() => {
        api.get<Seccion[]>('/secciones')
            .then(r => setSecciones(r.data))
            .catch(e => setError(mensajeError(e, 'No se pudieron cargar las secciones')))
    }, [])

    async function cargarDatos(subId: string) {
        const [m, s] = await Promise.all([
            api.get<Matricula[]>('/matriculas', { params: { subseccionId: subId } }),
            api.get<AlumnoBasico[]>('/matriculas/sin-matricula'),
        ])
        setMatriculados(m.data)
        setSinMatricula(s.data)
        setSeleccion(new Set())
    }

    async function elegirSeccion(id: string) {
        setSeccionId(id)
        setSubseccionId('')
        setSubsecciones([])
        setMatriculados([])
        setSinMatricula([])
        setError('')
        setOk('')
        if (!id) return
        try {
            const r = await api.get<Subseccion[]>('/subsecciones', { params: { seccionId: id } })
            setSubsecciones(r.data)
        } catch (e) {
            setError(mensajeError(e, 'No se pudieron cargar las subsecciones'))
        }
    }

    async function elegirSubseccion(id: string) {
        setSubseccionId(id)
        setError('')
        setOk('')
        setMatriculados([])
        setSinMatricula([])
        if (!id) return
        try {
            await cargarDatos(id)
        } catch (e) {
            setError(mensajeError(e, 'No se pudieron cargar las matrículas'))
        }
    }

    async function matricular() {
        if (!subseccionId || seleccion.size === 0) return
        setError('')
        setOk('')
        setGuardando(true)
        try {
            const r = await api.post<Matricula[]>('/matriculas', {
                subseccionId: Number(subseccionId),
                alumnoIds: Array.from(seleccion),
            })
            await cargarDatos(subseccionId)
            setOk(`${r.data.length} alumno(s) matriculado(s) correctamente`)
        } catch (e) {
            setError(mensajeError(e, 'No se pudo matricular a los alumnos'))
        } finally {
            setGuardando(false)
        }
    }

    async function quitar(m: Matricula) {
        if (!window.confirm(language === 'en' ? `Remove enrollment for ${m.nombre} ${m.apellido}?` : `¿Quitar la matrícula de ${m.nombre} ${m.apellido}?`)) return
        setError('')
        setOk('')
        try {
            await api.delete(`/matriculas/${m.id}`)
            await cargarDatos(subseccionId)
            setOk('Matrícula eliminada')
        } catch (e) {
            setError(mensajeError(e, 'No se pudo quitar la matrícula'))
        }
    }

    function alternar(id: number) {
        setSeleccion(prev => {
            const nuevo = new Set(prev)
            if (nuevo.has(id)) nuevo.delete(id)
            else nuevo.add(id)
            return nuevo
        })
    }

    const visibles = useMemo(() => {
        const t = filtro.trim().toLowerCase()
        if (!t) return sinMatricula
        return sinMatricula.filter(a => `${a.nombre} ${a.apellido} ${a.correo}`.toLowerCase().includes(t))
    }, [sinMatricula, filtro])

    return (
        <Shell eyebrow="GESTIÓN ACADÉMICA" title="Matrículas" description="Asigna alumnos a las subsecciones de sus materias.">
        <div className="space-y-6">
            <h2 className="text-xl font-semibold">Matrícula de alumnos</h2>

            {error && (
                <div role="alert" className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>
            )}
            {ok && (
                <div role="status" className="rounded-md border border-green-300 bg-green-50 px-3 py-2 text-sm text-green-700">{ok}</div>
            )}

            <div className="grid gap-4 sm:grid-cols-2">
                <label className="block text-sm">
                    Sección
                    <select
                        value={seccionId}
                        onChange={e => void elegirSeccion(e.target.value)}
                        className="mt-1 w-full rounded-md border px-3 py-2"
                    >
                        <option value="">Seleccione…</option>
                        {secciones.map(s => (
                            <option key={s.id} value={s.id}>
                                {s.nombre} · {s.ciclo}{s.materias?.length ? ` · ${s.materias.join(', ')}` : ''}
                            </option>
                        ))}
                    </select>
                </label>
                <label className="block text-sm">
                    Subsección
                    <select
                        value={subseccionId}
                        onChange={e => void elegirSubseccion(e.target.value)}
                        disabled={!seccionId}
                        className="mt-1 w-full rounded-md border px-3 py-2 disabled:bg-gray-100"
                    >
                        <option value="">Seleccione…</option>
                        {subsecciones.map(s => (
                            <option key={s.id} value={s.id}>{s.nombre}</option>
                        ))}
                    </select>
                </label>
            </div>

            {seccionId && subsecciones.length === 0 && (
                <p className="text-sm text-amber-700">Esta sección no tiene subsecciones creadas.</p>
            )}

            {subseccionId && (
                <div className="grid gap-6 lg:grid-cols-2">
                    <section className="space-y-2">
                        <h3 className="font-semibold">Matriculados ({matriculados.length})</h3>
                        {matriculados.length === 0 ? (
                            <p className="text-sm text-gray-500">Aún no hay alumnos matriculados en esta subsección.</p>
                        ) : (
                            <ul className="divide-y rounded-md border">
                                {matriculados.map(m => (
                                    <li key={m.id} className="flex items-center justify-between gap-3 px-3 py-2">
                                        <span className="text-sm">
                                            {m.nombre} {m.apellido}
                                            <span className="ml-2 text-gray-500">{m.correo}</span>
                                        </span>
                                        <button
                                            type="button"
                                            onClick={() => void quitar(m)}
                                            title="Quitar matrícula"
                                            className="rounded-md p-1 text-red-600 hover:bg-red-50"
                                        >
                                            <Trash2 size={16} />
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </section>

                    <section className="space-y-2">
                        <h3 className="font-semibold">Alumnos sin matrícula ({sinMatricula.length})</h3>
                        <input
                            value={filtro}
                            onChange={e => setFiltro(e.target.value)}
                            placeholder="Buscar por nombre o correo"
                            className="w-full rounded-md border px-3 py-2 text-sm"
                        />
                        {visibles.length === 0 ? (
                            <p className="text-sm text-gray-500">No hay alumnos sin matrícula que coincidan.</p>
                        ) : (
                            <ul className="max-h-80 divide-y overflow-y-auto rounded-md border">
                                {visibles.map(a => (
                                    <li key={a.id} className="px-3 py-2">
                                        <label className="flex cursor-pointer items-center gap-3 text-sm">
                                            <input
                                                type="checkbox"
                                                checked={seleccion.has(a.id)}
                                                onChange={() => alternar(a.id)}
                                            />
                                            <span>
                                                {a.nombre} {a.apellido}
                                                <span className="ml-2 text-gray-500">{a.correo}</span>
                                            </span>
                                        </label>
                                    </li>
                                ))}
                            </ul>
                        )}
                        <button
                            type="button"
                            onClick={() => void matricular()}
                            disabled={guardando || seleccion.size === 0}
                            className="inline-flex items-center gap-2 rounded-md bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
                        >
                            <UserPlus size={16} />
                            Matricular seleccionados ({seleccion.size})
                        </button>
                    </section>
                </div>
            )}
        </div>
        </Shell>
    )
}
