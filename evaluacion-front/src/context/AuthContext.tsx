import { createContext, useContext, useState, type ReactNode } from 'react'
import api from '../api/axios'

export interface Usuario {
    id: number
    nombre: string
    email: string
    rol: 'ADMIN' | 'DOCENTE' | 'JURADO'|'ALUMNO'
}

interface AuthContextType {
    usuario: Usuario | null
    login: (email: string, password: string) => Promise<Usuario>
    logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
    const [usuario, setUsuario] = useState<Usuario | null>(() => {
        const guardado = localStorage.getItem('usuario')
        return guardado ? JSON.parse(guardado) : null
    })

    const login = async (email: string, password: string): Promise<Usuario> => {
        const { data } = await api.post('/auth/login', { email, password })
        const { token, ...datosUsuario } = data
        localStorage.setItem('token', token)
        localStorage.setItem('usuario', JSON.stringify(datosUsuario))
        setUsuario(datosUsuario)
        return datosUsuario
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