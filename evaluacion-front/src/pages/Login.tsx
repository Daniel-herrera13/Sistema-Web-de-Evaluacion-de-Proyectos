import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { isAxiosError } from 'axios'
import { useAuth } from '../context/AuthContext'

export default function Login() {
    const { login } = useAuth()
    const navigate = useNavigate()

    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [cargando, setCargando] = useState(false)

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault()
        setError('')
        setCargando(true)
        try {
            await login(email, password)
            navigate('/dashboard')
        } catch (err) {
            if (isAxiosError(err) && err.response?.data?.mensaje) {
                setError(err.response.data.mensaje)
            } else {
                setError('No se pudo conectar con el servidor')
            }
        } finally {
            setCargando(false)
        }
    }

    return (
        <div className="login-contenedor">
            <form className="login-card" onSubmit={handleSubmit}>
                <h1>Evaluación de Proyectos</h1>
                <p className="subtitulo">Inicia sesión para continuar</p>

                {error && <div className="alerta-error">{error}</div>}

                <label htmlFor="email">Correo institucional</label>
                <input
                    id="email"
                    type="email"
                    placeholder="usuario@itca.edu.sv"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    required
                />

                <label htmlFor="password">Contraseña</label>
                <input
                    id="password"
                    type="password"
                    placeholder="••••••••"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                />

                <button type="submit" disabled={cargando}>
                    {cargando ? 'Ingresando...' : 'Ingresar'}
                </button>
            </form>
        </div>
    )
}