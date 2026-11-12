import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import RutaPrivada from './components/RutaPrivada'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'
import Gestion from './pages/Gestion'

export default function App() {
  return (
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route
                path="/dashboard"
                element={
                  <RutaPrivada>
                    <Dashboard />
                  </RutaPrivada>
                }
            />
            <Route
                path="/gestion/:modulo"
                element={<RutaPrivada><Gestion /></RutaPrivada>}
            />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
  )
}