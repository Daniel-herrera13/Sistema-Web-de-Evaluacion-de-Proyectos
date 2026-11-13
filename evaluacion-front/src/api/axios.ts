import axios from 'axios'

const api = axios.create({
    baseURL: 'http://localhost:8080/api',
})

api.interceptors.request.use((config) => {
    const token = localStorage.getItem('token')
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})

// Sesión vencida: limpia y manda al login (el login fallido no cuenta)
api.interceptors.response.use(
    (respuesta) => respuesta,
    (error) => {
        const url: string = error.config?.url ?? ''
        if (error.response?.status === 401 && !url.includes('/auth/login')) {
            localStorage.removeItem('token')
            localStorage.removeItem('usuario')
            if (window.location.pathname !== '/login') window.location.assign('/login')
        }
        return Promise.reject(error)
    },
)

export default api