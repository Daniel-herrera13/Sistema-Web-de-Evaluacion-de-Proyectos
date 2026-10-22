import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Dashboard() {
    const { usuario, logout } = useAuth()
    const navigate = useNavigate()

    const cerrarSesion = () => {
        logout()
        navigate('/login')
    }

    return (
        <div className="dashboard">
            <h1>Bienvenido, {usuario?.nombre}</h1>
            <p>Rol: <strong>{usuario?.rol}</strong></p>
            <p>Correo: {usuario?.email}</p>
            <button onClick={cerrarSesion}>Cerrar sesión</button>
        </div>
    )
}