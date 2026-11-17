import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import RutaPrivada from './components/RutaPrivada'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Gestion from './pages/Gestion'
import Evaluaciones from './pages/Evaluaciones'
import Esquema from './pages/Esquema'
import Autoevaluacion from './pages/Autoevaluacion'
import EvaluacionPublica from './pages/EvaluacionPublica'
import Publico from './pages/Publico'
import NotasFinales from './pages/NotasFinales'
import MiNota from './pages/MiNota'
import MiProyecto from './pages/MiProyecto'
import MisCriterios from './pages/MisCriterios'
import Reportes from './pages/Reportes'
import Matriculas from './pages/Matriculas'
import Ciclos from './pages/Ciclos'

export default function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    <Route path="/login" element={<Login />} />
                    <Route path="/publico" element={<Publico />} />
                    <Route path="/publico/:codigo" element={<Publico />} />

                    <Route path="/dashboard" element={<RutaPrivada><Dashboard /></RutaPrivada>} />

                    {/* Administración y docentes */}
                    <Route path="/gestion/:modulo" element={<RutaPrivada roles={['ADMIN', 'DOCENTE']}><Gestion /></RutaPrivada>} />
                    <Route path="/modalidades" element={<RutaPrivada roles={['ADMIN', 'DOCENTE']}><Esquema /></RutaPrivada>} />
                    <Route path="/evaluacion-publica" element={<RutaPrivada roles={['ADMIN', 'DOCENTE']}><EvaluacionPublica /></RutaPrivada>} />
                    <Route path="/notas" element={<RutaPrivada roles={['ADMIN', 'DOCENTE']}><NotasFinales /></RutaPrivada>} />
                    <Route path="/matriculas" element={<RutaPrivada roles={['ADMIN', 'DOCENTE']}><Matriculas /></RutaPrivada>} />
                    <Route path="/ciclos" element={<RutaPrivada roles={['ADMIN']}><Ciclos /></RutaPrivada>} />

                    {/* Evaluadores (cada modo valida su propio rol) */}
                    <Route path="/evaluacion/:modo" element={<RutaPrivada roles={['ADMIN', 'DOCENTE', 'JURADO']}><Evaluaciones /></RutaPrivada>} />
                    <Route path="/reportes" element={<RutaPrivada roles={['ADMIN', 'DOCENTE', 'JURADO']}><Reportes /></RutaPrivada>} />

                    {/* Alumnos */}
                    <Route path="/mi-proyecto" element={<RutaPrivada roles={['ALUMNO']}><MiProyecto /></RutaPrivada>} />
                    <Route path="/mis-criterios" element={<RutaPrivada roles={['ALUMNO']}><MisCriterios /></RutaPrivada>} />
                    <Route path="/autoevaluacion" element={<RutaPrivada roles={['ALUMNO']}><Autoevaluacion /></RutaPrivada>} />
                    <Route path="/mi-nota" element={<RutaPrivada roles={['ALUMNO']}><MiNota /></RutaPrivada>} />

                    <Route path="*" element={<Navigate to="/dashboard" replace />} />
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    )
}
