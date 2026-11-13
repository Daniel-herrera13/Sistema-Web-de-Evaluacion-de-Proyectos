import { useEffect, useState } from 'react'
import { UserPlus } from 'lucide-react'
// AJUSTAR: ruta de tu instancia de axios (la que ya agrega el token JWT).
import api from '../api/axios'
import type { AlumnoBasico } from '../types/matricula'
import { mensajeError } from '../utils/errores'

interface Props {
    grupoId: number
    /** Se llama después de agregar un alumno, para que la pantalla recargue los miembros del grupo. */
    onAgregado?: () => void
}

export default function AlumnosDisponibles({ grupoId, onAgregado }: Props) {
    const [disponibles, setDisponibles] = useState<AlumnoBasico[] | null>(null)
    const [error, setError] = useState('')
    const [agregando, setAgregando] = useState<number | null>(null)
    const [version, setVersion] = useState(0)

    useEffect(() => {
        let cancelado = false
        api.get<AlumnoBasico[]>(`/grupos/${grupoId}/alumnos-disponibles`)
            .then(r => { if (!cancelado) setDisponibles(r.data) })
            .catch(e => { if (!cancelado) { setDisponibles([]); setError(mensajeError(e, 'No se pudo cargar la lista de alumnos')) } })
        return () => { cancelado = true }
    }, [grupoId, version])

    async function agregar(alumnoId: number) {
        setError('')
        setAgregando(alumnoId)
        try {
            await api.post(`/grupos/${grupoId}/alumnos`, { alumnoId })
            onAgregado?.()
            setVersion(v => v + 1)
        } catch (e) {
            setError(mensajeError(e, 'No se pudo agregar el alumno al grupo'))
        } finally {
            setAgregando(null)
        }
    }

    return (
        <div className="space-y-3">
            <h3 className="font-semibold">Alumnos disponibles</h3>

            {error && (
                <div role="alert" className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-sm text-red-700">
                    {error}
                </div>
            )}

            {disponibles === null && <p className="text-sm text-gray-500">Cargando…</p>}

            {disponibles !== null && disponibles.length === 0 && (
                <div className="rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-800">
                    No hay alumnos disponibles. Verifique que estén matriculados en la subsección de este grupo.
                </div>
            )}

            {disponibles !== null && disponibles.length > 0 && (
                <ul className="divide-y rounded-md border">
                    {disponibles.map(a => (
                        <li key={a.id} className="flex items-center justify-between gap-3 px-3 py-2">
                            <span className="text-sm">
                                {a.nombre} {a.apellido}
                                <span className="ml-2 text-gray-500">{a.correo}</span>
                            </span>
                            <button
                                type="button"
                                onClick={() => agregar(a.id)}
                                disabled={agregando !== null}
                                className="inline-flex items-center gap-1 rounded-md bg-blue-600 px-3 py-1 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
                            >
                                <UserPlus size={16} /> Agregar
                            </button>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    )
}
