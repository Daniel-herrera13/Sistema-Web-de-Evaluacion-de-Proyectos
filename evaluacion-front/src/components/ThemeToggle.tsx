import { Moon, Sun } from 'lucide-react'
import { useTheme } from '../context/ThemeContext'

export default function ThemeToggle({ className = 'theme-toggle' }: { className?: string }) {
    const { theme, toggleTheme } = useTheme()
    const oscuro = theme === 'dark'

    return (
        <button
            type="button"
            className={className}
            onClick={toggleTheme}
            aria-label={oscuro ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
            title={oscuro ? 'Modo claro' : 'Modo oscuro'}
        >
            {oscuro ? <Sun size={18} /> : <Moon size={18} />}
        </button>
    )
}