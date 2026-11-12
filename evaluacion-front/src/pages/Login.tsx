import {useState, type FormEvent} from 'react';
import {useNavigate} from 'react-router-dom';
import {useAuth} from '../context/AuthContext';
import './Login.css';

export default function Login() {
    const {login} = useAuth();
    const navigate = useNavigate();

    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [cargando, setCargando] = useState(false);

    const enviar = async (e: FormEvent) => {
        e.preventDefault();
        setError('');
        setCargando(true);
        try {
            await login(email, password);
            navigate('/dashboard');
        } catch (err: any) {
            setError(err?.response?.data?.mensaje ?? 'Correo o contraseña incorrectos');
        } finally {
            setCargando(false);
        }
    };

    return (
        <div className="login">
            <aside className="login-panel">
                <div className="contenido">
                    <h1>Sistema Web de Evaluación</h1>
                    <p>Crea, responde y revisa evaluaciones desde cualquier navegador.</p>
                </div>
                <span className="marca">ITCA–FEPADE</span>
            </aside>

            <main className="login-form-lado">
                <form className="login-form" onSubmit={enviar} noValidate>
                    <h2>Iniciar sesión</h2>
                    <p className="sub">Ingresa con tu cuenta institucional</p>

                    <div className={`campo ${error ? 'con-error' : ''}`}>
                        <label htmlFor="email">Correo institucional</label>
                        <input
                            id="email"
                            type="email"
                            autoComplete="username"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                        />
                    </div>

                    <div className={`campo ${error ? 'con-error' : ''}`}>
                        <label htmlFor="password">Contraseña</label>
                        <input
                            id="password"
                            type="password"
                            autoComplete="current-password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                        {error && <p className="mensaje-error" role="alert">{error}</p>}
                    </div>

                    <a href="#" className="olvido" onClick={(e) => e.preventDefault()}>
                        ¿Olvidaste tu contraseña?
                    </a>

                    <button type="submit" className="btn-ingresar" disabled={cargando || !email || !password}>
                        {cargando ? 'Ingresando...' : 'Ingresar'}
                    </button>
                </form>
            </main>
        </div>
    );
}