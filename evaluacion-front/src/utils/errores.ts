import axios from 'axios'

export function mensajeError(reason: unknown, porDefecto = 'No se pudo completar la acción.'): string {
    if (axios.isAxiosError(reason)) {
        if (!reason.response) return 'No hay conexión con el servidor. Verifica que la API esté encendida.'
        const data = reason.response.data as any
        if (typeof data?.mensaje === 'string' && data.mensaje) return data.mensaje
        const detalle = data?.errores ?? data?.campos
        if (Array.isArray(detalle) && detalle.length) {
            return detalle
                .map((d: any) => (typeof d === 'string' ? d : d?.mensaje ?? `${d?.campo ?? ''} ${d?.error ?? ''}`.trim()))
                .join(' · ')
        }
        if (reason.response.status === 403) return 'No tienes permiso para esta acción.'
        if (reason.response.status === 401) return 'Tu sesión expiró. Vuelve a iniciar sesión.'
        return porDefecto
    }
    if (reason instanceof Error && reason.message) return reason.message
    return porDefecto
}