import { createContext, useContext, useState, type ReactNode } from 'react'
import api from '../api/axios'

export type Rol = 'ADMIN' | 'DOCENTE' | 'JURADO' | 'ALUMNO'

export interface Usuario {
    id: number
    nombre: string
    email: string
    rol: Rol
}

interface AuthContextType {
    usuario: Usuario | null
    login: (email: string, password: string) => Promise<Usuario>
    logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

// El backend puede mandar el rol en minúscula; siempre se guarda en mayúscula
function normalizar(datos: any): Usuario | null {
    if (!datos || typeof datos !== 'object') return null
    return { ...datos, rol: String(datos.rol ?? '').toUpperCase() as Rol }
}

function leerGuardado(): Usuario | null {
    try {
        const guardado = localStorage.getItem('usuario')
        return guardado ? normalizar(JSON.parse(guardado)) : null
    } catch {
        return null
    }
}

export function AuthProvider({ children }: { children: ReactNode }) {
    const [usuario, setUsuario] = useState<Usuario | null>(leerGuardado)

    const login = async (email: string, password: string): Promise<Usuario> => {
        const { data } = await api.post('/auth/login', { email, password })
        const { token, ...datos } = data
        const nuevo = normalizar(datos) as Usuario
        localStorage.setItem('token', token)
        localStorage.setItem('usuario', JSON.stringify(nuevo))
        setUsuario(nuevo)
        return nuevo
    }

    const logout = () => {
        localStorage.removeItem('token')
        localStorage.removeItem('usuario')
        setUsuario(null)
    }

    return (
        <AuthContext.Provider value={{ usuario, login, logout }}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    const ctx = useContext(AuthContext)
    if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
    return ctx
}