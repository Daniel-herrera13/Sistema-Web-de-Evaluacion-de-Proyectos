import axios from 'axios'
import api from '../api/axios'

export type Formato = 'pdf' | 'xlsx'

export const limpiar = (obj: Record<string, unknown>) =>
    Object.fromEntries(Object.entries(obj).filter(([, v]) => v !== '' && v !== null && v !== undefined))

export async function descargar(url: string, params: Record<string, unknown>, nombre: string, formato: Formato) {
    try {
        const { data } = await api.get<Blob>(url, { params: { ...params, formato }, responseType: 'blob' })
        const href = URL.createObjectURL(data)
        const enlace = document.createElement('a')
        enlace.href = href
        enlace.download = `${nombre}.${formato}`
        document.body.appendChild(enlace)
        enlace.click()
        enlace.remove()
        URL.revokeObjectURL(href)
    } catch (e) {
        // Con responseType blob, el mensaje de error del backend llega como Blob
        if (axios.isAxiosError(e) && e.response?.data instanceof Blob) {
            let mensaje = 'No se pudo generar el archivo.'
            try {
                mensaje = JSON.parse(await e.response.data.text()).mensaje ?? mensaje
            } catch { /* sin detalle */ }
            throw new Error(mensaje)
        }
        throw e
    }
}