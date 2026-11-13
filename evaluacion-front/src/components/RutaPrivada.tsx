import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth, type Rol } from '../context/AuthContext'

interface Props {
    children: ReactNode
    roles?: Rol[]
}

export default function RutaPrivada({ children, roles }: Props) {
    const { usuario } = useAuth()
    if (!usuario) return <Navigate to="/login" replace />
    if (roles && !roles.includes(usuario.rol)) return <Navigate to="/dashboard" replace />
    return <>{children}</>
}