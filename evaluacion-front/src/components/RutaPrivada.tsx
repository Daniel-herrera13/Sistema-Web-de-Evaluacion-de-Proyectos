import { Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../context/AuthContext'

export default function RutaPrivada({ children }: { children: ReactNode }) {
    const { usuario } = useAuth()
    return usuario ? <>{children}</> : <Navigate to="/login" replace />
}