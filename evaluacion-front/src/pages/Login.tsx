import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/axios';
import { useTheme } from '../context/ThemeContext';
import {
    Eye,
    EyeOff,
    Lock,
    Mail,
    CheckCircle2,
    Sparkles,
    FileText,
    BarChart3,
    Moon,
    Sun,
    X,
    ArrowRight,
    ShieldCheck,
    GraduationCap,
    Loader2,
    ChevronLeft,
    ChevronRight,
    KeyRound,
    Globe,
    Bell,
    LogOut,
    Clock,
    Server,
    UserCheck,
    LayoutDashboard,
    Award,
    CheckCircle,
    Search,
    Settings,
    TrendingUp
} from 'lucide-react';

type TipoCuenta = 'student' | 'teacher' | 'jury' | 'admin';

const rolPorTipoCuenta: Record<TipoCuenta, 'ALUMNO' | 'DOCENTE' | 'JURADO' | 'ADMIN'> = {
    student: 'ALUMNO',
    teacher: 'DOCENTE',
    jury: 'JURADO',
    admin: 'ADMIN'
};

export default function App() {
    const { login, logout } = useAuth();
    const navigate = useNavigate();
    // Application State
    const [role, setRole] = useState<TipoCuenta>('student');
    const [email, setEmail] = useState('alumno@itca.edu.sv');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [rememberMe, setRememberMe] = useState(true);
    const [isLoading, setIsLoading] = useState(false);
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [error, setError] = useState('');
    const { theme, toggleTheme } = useTheme();
    const isDarkMode = theme === 'dark';
    const [lang, setLang] = useState('es'); // 'es' | 'en'

    // Hero Carousel Index
    const [carouselIndex, setCarouselIndex] = useState(0);

    // Recovery Password Modal State
    const [showForgotModal, setShowForgotModal] = useState(false);
    const [modalStep, setModalStep] = useState(1);
    const [resetEmail, setResetEmail] = useState('');
    const [resetCode, setResetCode] = useState(['', '', '', '', '', '']);
    const [isResetSuccess, setIsResetSuccess] = useState(false);
    const [resetPassword, setResetPassword] = useState('');
    const [resetPassword2, setResetPassword2] = useState('');
    const [resetError, setResetError] = useState('');
    const [resetLoading, setResetLoading] = useState(false);

    // Quick domain append
    const handleQuickDomain = () => {
        if (!email.includes('@')) {
            setEmail(email + '@itca.edu.sv');
        } else {
            const prefix = email.split('@')[0];
            setEmail(prefix + '@itca.edu.sv');
        }
    };

    // Password Strength Evaluator
    const getPasswordStrength = (pass: string) => {
        let score = 0;
        if (!pass) return { score: 0, label: '', color: 'bg-slate-200' };
        if (pass.length >= 8) score++;
        if (/[A-Z]/.test(pass)) score++;
        if (/\d/.test(pass)) score++;
        if (/[^A-Za-z0-9]/.test(pass)) score++;

        if (score <= 1) return { score: 25, label: lang === 'es' ? 'Básica' : 'Basic', color: 'bg-red-500', text: 'text-red-500' };
        if (score === 2 || score === 3) return { score: 65, label: lang === 'es' ? 'Segura' : 'Moderate', color: 'bg-amber-500', text: 'text-amber-500' };
        return { score: 100, label: lang === 'es' ? 'Muy Segura' : 'Strong', color: 'bg-emerald-500', text: 'text-emerald-500' };
    };

    const pwdStrength = getPasswordStrength(password);

    // Hero Carousel Slide Contents
    const carouselItems = [
        {
            icon: <FileText className="w-7 h-7 text-amber-300" />,
            title: lang === 'es' ? 'Evaluaciones Continuas & Adaptativas' : 'Adaptive & Continuous Exams',
            desc: lang === 'es' ? 'Cuestionarios interactivos con sincronización en tiempo real y detección automática de caídas de red.' : 'Interactive quizzes with real-time sync and network auto-recovery.'
        },
        {
            icon: <BarChart3 className="w-7 h-7 text-cyan-300" />,
            title: lang === 'es' ? 'Análisis de Rendimiento Inmediato' : 'Instant Performance Analytics',
            desc: lang === 'es' ? 'Gráficas de competencia, retroalimentación instantánea e informes detallados para estudiantes y docentes.' : 'Competency graphs, instant feedback, and detailed reports for students and faculty.'
        },
        {
            icon: <ShieldCheck className="w-7 h-7 text-emerald-300" />,
            title: lang === 'es' ? 'Seguridad Institucional Avanzada' : 'Advanced Institutional Security',
            desc: lang === 'es' ? 'Autenticación encriptada con credenciales centralizadas ITCA-FEPADE y monitoreo antifraude.' : 'Encrypted authentication with centralized ITCA-FEPADE credentials and anti-fraud checks.'
        }
    ];

    // Carousel timer
    useEffect(() => {
        const timer = setInterval(() => {
            setCarouselIndex((prev) => (prev + 1) % carouselItems.length);
        }, 6000);
        return () => clearInterval(timer);
    }, [carouselItems.length]);

    // Role Handler
    const handleRoleSelect = (selectedRole: TipoCuenta) => {
        setRole(selectedRole);
        if (selectedRole === 'student') setEmail('alumno@itca.edu.sv');
        else if (selectedRole === 'teacher') setEmail('docente@itca.edu.sv');
        else if (selectedRole === 'jury') setEmail('jurado@itca.edu.sv');
        else setEmail('admin@itca.edu.sv');
    };

    // Login Submit
    const handleSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        if (!email || !password) return;

        setError('');
        setIsLoading(true);
        try {
            const usuario = await login(email, password);
            if (usuario.rol !== rolPorTipoCuenta[role]) {
                logout();
                const nombres: Record<TipoCuenta, string> = { student: 'alumno', teacher: 'docente', jury: 'jurado', admin: 'administrador' };
                setError(`Esta cuenta no corresponde al tipo ${nombres[role]}. Selecciona el tipo de cuenta correcto.`);
                return;
            }
            navigate('/dashboard');
        } catch (err: unknown) {
            const response = (err as { response?: { data?: { mensaje?: string } } }).response;
            setError(response?.data?.mensaje ?? 'Correo o contraseña incorrectos');
        } finally {
            setIsLoading(false);
        }
    };

    // Code Input Handler
    const handleCodeChange = (index: number, value: string) => {
        if (value.length > 1 || (value && !/^\d$/.test(value))) return;
        const newCode = [...resetCode];
        newCode[index] = value;
        setResetCode(newCode);
        if (value && index < 5) {
            document.getElementById(`code-input-desktop-${index + 1}`)?.focus();
        }
    };

    const cerrarRecuperacion = () => {
        setShowForgotModal(false);
        setModalStep(1);
        setResetEmail('');
        setResetCode(['', '', '', '', '', '']);
        setResetPassword('');
        setResetPassword2('');
        setResetError('');
        setIsResetSuccess(false);
    };

    const mensajeDe = (err: unknown, porDefecto: string) =>
        (err as { response?: { data?: { mensaje?: string } } }).response?.data?.mensaje ?? porDefecto;

    const handleResetSubmit = async (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault();
        setResetError('');

        if (modalStep === 1) {
            if (!resetEmail) return;
            setResetLoading(true);
            try {
                await api.post('/auth/recuperar', { email: resetEmail.trim() });
                setModalStep(2);
            } catch (err) {
                setResetError(mensajeDe(err, 'No se pudo enviar el código. Intenta de nuevo.'));
            } finally {
                setResetLoading(false);
            }
            return;
        }

        const codigo = resetCode.join('');
        if (codigo.length !== 6) { setResetError('Ingresa los 6 dígitos del código.'); return; }
        if (resetPassword.length < 8) { setResetError('La contraseña debe tener al menos 8 caracteres.'); return; }
        if (resetPassword !== resetPassword2) { setResetError('Las contraseñas no coinciden.'); return; }

        setResetLoading(true);
        try {
            await api.post('/auth/restablecer', { email: resetEmail.trim(), codigo, password: resetPassword });
            setIsResetSuccess(true);
            setTimeout(cerrarRecuperacion, 2500);
        } catch (err) {
            setResetError(mensajeDe(err, 'No se pudo restablecer la contraseña.'));
        } finally {
            setResetLoading(false);
        }
    };

    const campo = `w-full px-4 py-3.5 rounded-2xl border text-sm outline-none transition-all ${
        isDarkMode
            ? 'bg-slate-800 border-slate-700 text-white focus:border-red-500'
            : 'bg-slate-50 border-slate-200 text-slate-900 focus:border-red-600'
    }`;

    const t = {
        title: lang === 'es' ? 'Sistema Web de Evaluación' : 'Evaluation Web System',
        subtitle: lang === 'es' ? 'Plataforma institucional para la gestión, ejecución y análisis de evaluaciones académicas en línea.' : 'Institutional platform for managing, conducting, and analyzing academic online assessments.',
        loginHeader: lang === 'es' ? 'Iniciar Sesión' : 'Sign In',
        loginDesc: lang === 'es' ? 'Accede con tu cuenta institucional de ITCA-FEPADE' : 'Access with your institutional ITCA-FEPADE account',
        student: lang === 'es' ? 'Alumno' : 'Student',
        teacher: lang === 'es' ? 'Docente' : 'Faculty',
        admin: lang === 'es' ? 'Administrador' : 'Admin',
        emailLabel: lang === 'es' ? 'Correo Institucional' : 'Institutional Email',
        passwordLabel: lang === 'es' ? 'Contraseña' : 'Password',
        rememberMe: lang === 'es' ? 'Recordar sesión' : 'Remember session',
        forgotPassword: lang === 'es' ? '¿Olvidaste tu contraseña?' : 'Forgot password?',
        submitBtn: lang === 'es' ? 'Ingresar al Portal' : 'Sign In to Portal',
        systemStatus: lang === 'es' ? 'Sistema Operativo (100%)' : 'System Operational (100%)',
        jury: lang === 'es' ? 'Jurado' : 'Jury'
    };

    return (
        <div className={`min-h-screen font-sans transition-colors duration-500 ${isDarkMode ? 'bg-slate-950 text-slate-100' : 'bg-slate-100 text-slate-900'} antialiased selection:bg-red-600 selection:text-white`}>

            {/* ================= DESKTOP TOP FLOATING TOOLBAR ================= */}
            <div className="fixed top-5 right-8 z-50 flex items-center gap-3">
                {/* Language Switcher */}
                <button
                    onClick={() => setLang(lang === 'es' ? 'en' : 'es')}
                    className="px-3 py-2 rounded-xl bg-white/70 dark:bg-slate-800/80 backdrop-blur-xl border border-white/50 dark:border-slate-700/60 shadow-lg hover:shadow-xl hover:scale-105 active:scale-95 transition-all text-xs font-bold flex items-center gap-2 cursor-pointer"
                >
                    <Globe className="w-4 h-4 text-red-600 dark:text-red-400" />
                    <span>{lang.toUpperCase()}</span>
                </button>

                {/* Theme Toggle */}
                <button
                    onClick={toggleTheme}
                    aria-label={isDarkMode ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro'}
                    className="p-2.5 rounded-xl bg-white/70 dark:bg-slate-800/80 backdrop-blur-xl border border-white/50 dark:border-slate-700/60 shadow-lg hover:shadow-xl hover:scale-105 active:scale-95 transition-all cursor-pointer"
                >
                    {isDarkMode ? <Sun className="w-4 h-4 text-amber-400" /> : <Moon className="w-4 h-4 text-slate-700" />}
                </button>
            </div>

            {/* ================= CONDITIONAL SCREEN: DASHBOARD OR LOGIN ================= */}
            {isLoggedIn ? (
                /* ================= DESKTOP DASHBOARD ================= */
                <div className="min-h-screen flex bg-slate-50 dark:bg-slate-950 animate-fade-in">

                    {/* Sidebar */}
                    <aside className={`w-64 border-r flex flex-col justify-between p-5 ${isDarkMode ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-200'}`}>
                        <div className="space-y-6">
                            <div className="flex items-center gap-3 px-2">
                                <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-red-600 to-red-800 flex items-center justify-center text-white font-bold shadow-md">
                                    <GraduationCap className="w-6 h-6" />
                                </div>
                                <div>
                                    <h1 className="font-extrabold text-sm leading-tight">ITCA-FEPADE</h1>
                                    <span className="text-[10px] text-red-600 font-bold tracking-wider uppercase">Portal Web</span>
                                </div>
                            </div>

                            <nav className="space-y-1">
                                <a href="#dashboard" className="flex items-center gap-3 px-3 py-2.5 rounded-xl bg-red-600 text-white font-semibold text-xs shadow-md shadow-red-600/20">
                                    <LayoutDashboard className="w-4 h-4" />
                                    <span>Panel Principal</span>
                                </a>
                                <a href="#evaluations" className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-medium transition-colors ${isDarkMode ? 'text-slate-400 hover:bg-slate-800' : 'text-slate-600 hover:bg-slate-100'}`}>
                                    <FileText className="w-4 h-4" />
                                    <span>Mis Evaluaciones</span>
                                </a>
                                <a href="#grades" className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-medium transition-colors ${isDarkMode ? 'text-slate-400 hover:bg-slate-800' : 'text-slate-600 hover:bg-slate-100'}`}>
                                    <BarChart3 className="w-4 h-4" />
                                    <span>Historial de Notas</span>
                                </a>
                                <a href="#settings" className={`flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs font-medium transition-colors ${isDarkMode ? 'text-slate-400 hover:bg-slate-800' : 'text-slate-600 hover:bg-slate-100'}`}>
                                    <Settings className="w-4 h-4" />
                                    <span>Ajustes</span>
                                </a>
                            </nav>
                        </div>

                        <div className={`p-3 rounded-2xl border flex items-center gap-3 ${isDarkMode ? 'bg-slate-800/50 border-slate-700/50' : 'bg-slate-100 border-slate-200'}`}>
                            <div className="w-9 h-9 rounded-xl bg-red-600 text-white flex items-center justify-center font-bold text-sm">
                                {role === 'student' ? 'ES' : role === 'teacher' ? 'DOC' : 'ADM'}
                            </div>
                            <div className="flex-1 min-w-0">
                                <p className="text-xs font-bold truncate">{email.split('@')[0]}</p>
                                <p className="text-[10px] text-slate-500 capitalize">{role}</p>
                            </div>
                            <button
                                onClick={() => setIsLoggedIn(false)}
                                className="text-slate-400 hover:text-red-500 p-1 transition-colors"
                                title="Cerrar Sesión"
                            >
                                <LogOut className="w-4 h-4" />
                            </button>
                        </div>
                    </aside>

                    {/* Main Dashboard Body */}
                    <main className="flex-1 flex flex-col min-w-0 overflow-y-auto">
                        {/* Header Toolbar */}
                        <header className={`px-8 py-4 border-b flex justify-between items-center ${isDarkMode ? 'bg-slate-900/50 border-slate-800' : 'bg-white border-slate-200'}`}>
                            <div className="relative w-80">
                                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                                <input
                                    type="text"
                                    placeholder="Buscar evaluaciones, materias..."
                                    className={`w-full pl-9 pr-4 py-1.5 rounded-xl text-xs border outline-none ${isDarkMode ? 'bg-slate-800 border-slate-700 text-white' : 'bg-slate-100 border-slate-200'}`}
                                />
                            </div>

                            <div className="flex items-center gap-4">
                                <div className="flex items-center gap-2 text-xs text-slate-500">
                                    <Clock className="w-4 h-4 text-emerald-500" />
                                    <span>Ciclo II - 2026</span>
                                </div>
                                <button className="relative p-2 rounded-xl border border-slate-200 dark:border-slate-800 text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800">
                                    <Bell className="w-4 h-4" />
                                    <span className="w-2 h-2 rounded-full bg-red-600 absolute top-1 right-1" />
                                </button>
                            </div>
                        </header>

                        {/* Dashboard Main Content */}
                        <div className="p-8 space-y-8 max-w-7xl">
                            {/* Banner */}
                            <div className="p-8 rounded-3xl bg-gradient-to-r from-red-700 via-red-800 to-red-950 text-white shadow-2xl relative overflow-hidden flex justify-between items-center">
                                <div className="relative z-10 space-y-2 max-w-xl">
                                    <span className="px-3 py-1 rounded-full bg-white/10 text-xs font-semibold border border-white/20">Portal del Alumno</span>
                                    <h2 className="text-3xl font-black">¡Bienvenido al Panel Académico!</h2>
                                    <p className="text-red-100/80 text-xs leading-relaxed">
                                        Tienes 2 evaluaciones pendientes para esta semana. Asegúrate de verificar los requerimientos técnicos de tu navegador antes de iniciar.
                                    </p>
                                </div>
                                <button className="relative z-10 px-6 py-3 rounded-2xl bg-white text-red-800 font-bold text-xs shadow-xl hover:bg-red-50 transition-all active:scale-95">
                                    Comenzar Evaluación
                                </button>
                                <div className="absolute right-0 bottom-0 w-80 h-80 bg-red-500/10 rounded-full blur-3xl pointer-events-none" />
                            </div>

                            {/* Stat Cards Grid */}
                            <div className="grid grid-cols-4 gap-5">
                                <div className={`p-5 rounded-2xl border ${isDarkMode ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-200'} shadow-sm space-y-2`}>
                                    <div className="flex justify-between text-amber-500">
                                        <Clock className="w-5 h-5" />
                                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-500/10">2 Pendientes</span>
                                    </div>
                                    <p className="text-2xl font-black">Evaluaciones</p>
                                    <p className="text-xs text-slate-400">Próximo vencimiento en 24h</p>
                                </div>

                                <div className={`p-5 rounded-2xl border ${isDarkMode ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-200'} shadow-sm space-y-2`}>
                                    <div className="flex justify-between text-emerald-500">
                                        <CheckCircle className="w-5 h-5" />
                                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-emerald-500/10">100% Entregado</span>
                                    </div>
                                    <p className="text-2xl font-black">12 Completadas</p>
                                    <p className="text-xs text-slate-400">Ciclo lectivo actual</p>
                                </div>

                                <div className={`p-5 rounded-2xl border ${isDarkMode ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-200'} shadow-sm space-y-2`}>
                                    <div className="flex justify-between text-red-500">
                                        <TrendingUp className="w-5 h-5" />
                                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-red-500/10">Sobresaliente</span>
                                    </div>
                                    <p className="text-2xl font-black">8.95 / 10</p>
                                    <p className="text-xs text-slate-400">Promedio generalacumulado</p>
                                </div>

                                <div className={`p-5 rounded-2xl border ${isDarkMode ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-200'} shadow-sm space-y-2`}>
                                    <div className="flex justify-between text-cyan-500">
                                        <Award className="w-5 h-5" />
                                        <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-cyan-500/10">Aprobado</span>
                                    </div>
                                    <p className="text-2xl font-black">4 Asignaturas</p>
                                    <p className="text-xs text-slate-400">Ingeniería de Software</p>
                                </div>
                            </div>

                            {/* Pending Evaluations Desktop Table */}
                            <div className={`rounded-3xl border overflow-hidden ${isDarkMode ? 'bg-slate-900 border-slate-800' : 'bg-white border-slate-200'} shadow-sm`}>
                                <div className="p-6 border-b border-slate-200 dark:border-slate-800 flex justify-between items-center">
                                    <div>
                                        <h3 className="font-bold text-base">Evaluaciones Activas</h3>
                                        <p className="text-xs text-slate-400">Selecciona una prueba para dar inicio al cuestionario en línea.</p>
                                    </div>
                                    <button className="text-xs font-bold text-red-600 hover:underline">Ver Historial Completo</button>
                                </div>

                                <div className="overflow-x-auto">
                                    <table className="w-full text-left text-xs">
                                        <thead className={`border-b ${isDarkMode ? 'bg-slate-800/40 border-slate-800 text-slate-400' : 'bg-slate-50 border-slate-200 text-slate-500'}`}>
                                        <tr>
                                            <th className="p-4 pl-6">Asignatura</th>
                                            <th className="p-4">Evaluación</th>
                                            <th className="p-4">Duración</th>
                                            <th className="p-4">Fecha Límite</th>
                                            <th className="p-4">Estado</th>
                                            <th className="p-4 text-right pr-6">Acción</th>
                                        </tr>
                                        </thead>
                                        <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                                        <tr>
                                            <td className="p-4 pl-6 font-bold">Programación Web Avanzada</td>
                                            <td className="p-4">Examen Parcial II - React & Tailwind</td>
                                            <td className="p-4">60 Minutos</td>
                                            <td className="p-4 text-red-500 font-semibold">Hoy, 11:59 PM</td>
                                            <td className="p-4">
                                                <span className="px-2.5 py-1 rounded-full bg-amber-500/10 text-amber-500 font-bold text-[10px]">Pendiente</span>
                                            </td>
                                            <td className="p-4 text-right pr-6">
                                                <button className="px-3 py-1.5 rounded-lg bg-red-600 text-white font-bold hover:bg-red-700 transition-colors">Iniciar</button>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td className="p-4 pl-6 font-bold">Base de Datos II</td>
                                            <td className="p-4">Laboratorio Práctico - Stored Procedures</td>
                                            <td className="p-4">90 Minutos</td>
                                            <td className="p-4">Mañana, 05:00 PM</td>
                                            <td className="p-4">
                                                <span className="px-2.5 py-1 rounded-full bg-amber-500/10 text-amber-500 font-bold text-[10px]">Pendiente</span>
                                            </td>
                                            <td className="p-4 text-right pr-6">
                                                <button className="px-3 py-1.5 rounded-lg bg-slate-200 dark:bg-slate-800 font-bold hover:bg-slate-300 transition-colors">Ver Detalles</button>
                                            </td>
                                        </tr>
                                        <tr>
                                            <td className="p-4 pl-6 font-bold">Redes de Computadoras</td>
                                            <td className="p-4">Prueba Corta - Modelo OSI & TCP/IP</td>
                                            <td className="p-4">30 Minutos</td>
                                            <td className="p-4 text-slate-400">12 Nov 2026</td>
                                            <td className="p-4">
                                                <span className="px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-500 font-bold text-[10px]">Completada (9.5)</span>
                                            </td>
                                            <td className="p-4 text-right pr-6">
                                                <button className="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 text-slate-400 cursor-not-allowed">Revisar</button>
                                            </td>
                                        </tr>
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                        </div>
                    </main>
                </div>
            ) : (
                /* ================= DESKTOP SPLIT SCREEN LOGIN ================= */
                <div className="grid grid-cols-12 min-h-screen w-full overflow-hidden">

                    {/* ---------------- LEFT PANEL: BRANDING & CAROUSEL (55% Width) ---------------- */}
                    <div className="col-span-12 lg:col-span-7 relative bg-gradient-to-br from-red-700 via-red-800 to-red-950 text-white p-12 lg:p-20 flex flex-col justify-between shadow-2xl overflow-hidden">

                        {/* Widescreen Dot-Grid Layer & Ambient Glowing Orbs */}
                        <div className="absolute inset-0 opacity-10 bg-[radial-gradient(#fff_1.5px,transparent_1.5px)] [background-size:24px_24px] pointer-events-none" />
                        <div className="absolute -top-40 -left-40 w-[500px] h-[500px] bg-red-500/20 rounded-full blur-[120px] pointer-events-none" />
                        <div className="absolute -bottom-40 -right-40 w-[500px] h-[500px] bg-amber-500/15 rounded-full blur-[120px] pointer-events-none" />

                        {/* Header / Institutional Branding */}
                        <div className="relative z-10 flex items-center justify-between">
                            <div className="flex items-center gap-4">
                                <div className="w-12 h-12 rounded-2xl bg-white/10 backdrop-blur-xl flex items-center justify-center border border-white/20 shadow-2xl">
                                    <GraduationCap className="w-7 h-7 text-white" />
                                </div>
                                <div>
                                    <h1 className="font-black text-xl tracking-wider text-white">ITCA - FEPADE</h1>
                                    <p className="text-[11px] text-red-200/80 tracking-widest uppercase font-semibold">Escuela Especializada en Ingeniería</p>
                                </div>
                            </div>

                            {/* Operational System Badge */}
                            <div className="hidden xl:flex items-center gap-2 px-4 py-2 rounded-full bg-emerald-500/20 border border-emerald-400/30 backdrop-blur-md text-emerald-200 text-xs font-semibold">
                                <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping" />
                                <Server className="w-4 h-4" />
                                <span>{t.systemStatus}</span>
                            </div>
                        </div>

                        {/* Middle Main Showcase */}
                        <div className="relative z-10 my-auto py-12 max-w-2xl space-y-8">

                            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white/10 backdrop-blur-md border border-white/20 text-xs text-red-100 shadow-sm">
                                <Sparkles className="w-4 h-4 text-amber-300" />
                                <span>Plataforma Oficial de Evaluación Virtual</span>
                            </div>

                            <div className="space-y-4">
                                <h2 className="text-5xl xl:text-6xl font-black tracking-tight leading-none text-white">
                                    {t.title}
                                </h2>
                                <p className="text-red-100/90 text-base xl:text-lg font-light leading-relaxed">
                                    {t.subtitle}
                                </p>
                            </div>

                            {/* Desktop Dynamic Interactive Feature Card */}
                            <div className="p-8 rounded-3xl bg-white/10 backdrop-blur-2xl border border-white/20 shadow-2xl transition-all duration-500 min-h-[190px] flex flex-col justify-between group hover:border-white/40">
                                <div className="flex items-start gap-5">
                                    <div className="p-4 rounded-2xl bg-white/10 border border-white/20 shadow-inner flex-shrink-0">
                                        {carouselItems[carouselIndex].icon}
                                    </div>
                                    <div className="space-y-1">
                                        <h3 className="font-extrabold text-xl text-white">
                                            {carouselItems[carouselIndex].title}
                                        </h3>
                                        <p className="text-xs text-red-100/80 leading-relaxed">
                                            {carouselItems[carouselIndex].desc}
                                        </p>
                                    </div>
                                </div>

                                {/* Carousel Controls */}
                                <div className="flex items-center justify-between pt-6 border-t border-white/10 mt-6">
                                    <div className="flex gap-2">
                                        {carouselItems.map((_, idx) => (
                                            <button
                                                key={idx}
                                                onClick={() => setCarouselIndex(idx)}
                                                className={`h-2 rounded-full transition-all duration-300 cursor-pointer ${carouselIndex === idx ? 'w-8 bg-amber-400' : 'w-2 bg-white/30 hover:bg-white/50'}`}
                                            />
                                        ))}
                                    </div>

                                    <div className="flex items-center gap-2">
                                        <button
                                            onClick={() => setCarouselIndex((prev) => (prev === 0 ? carouselItems.length - 1 : prev - 1))}
                                            className="p-2 rounded-xl bg-white/10 hover:bg-white/20 transition-colors text-white cursor-pointer"
                                        >
                                            <ChevronLeft className="w-4 h-4" />
                                        </button>
                                        <button
                                            onClick={() => setCarouselIndex((prev) => (prev + 1) % carouselItems.length)}
                                            className="p-2 rounded-xl bg-white/10 hover:bg-white/20 transition-colors text-white cursor-pointer"
                                        >
                                            <ChevronRight className="w-4 h-4" />
                                        </button>
                                    </div>
                                </div>
                            </div>

                            {/* Institutional Stats Strip */}
                            <div className="grid grid-cols-3 gap-6 pt-4 border-t border-white/10">
                                <div>
                                    <p className="text-2xl font-black text-white">+15,000</p>
                                    <p className="text-[11px] text-red-200/70">Evaluaciones Realizadas</p>
                                </div>
                                <div>
                                    <p className="text-2xl font-black text-white">99.9%</p>
                                    <p className="text-[11px] text-red-200/70">Disponibilidad del Sistema</p>
                                </div>
                                <div>
                                    <p className="text-2xl font-black text-white">4.9 / 5</p>
                                    <p className="text-[11px] text-red-200/70">Calificación de Usuarios</p>
                                </div>
                            </div>

                        </div>

                        {/* Desktop Footer */}
                        <div className="relative z-10 flex justify-between items-center text-xs text-red-200/60 font-mono border-t border-white/10 pt-6">
                            <span>© 2026 ITCA-FEPADE El Salvador</span>
                            <span>Soporte Técnico Especializado</span>
                        </div>

                    </div>

                    {/* ---------------- RIGHT PANEL: LOGIN FORM (45% Width) ---------------- */}
                    <div className={`col-span-12 lg:col-span-5 flex items-center justify-center p-8 lg:p-16 relative transition-colors ${isDarkMode ? 'bg-slate-900' : 'bg-slate-50'}`}>

                        <div className="w-full max-w-md space-y-8">

                            {/* Form Title & Subtitle */}
                            <div className="space-y-2">
                                <div className="inline-flex items-center gap-2 text-xs font-bold text-red-600 dark:text-red-400">
                                    <UserCheck className="w-4 h-4" />
                                    <span>Autenticación Centralizada</span>
                                </div>
                                <h2 className="text-4xl font-black tracking-tight">
                                    {t.loginHeader}
                                </h2>
                                <p className={`text-xs ${isDarkMode ? 'text-slate-400' : 'text-slate-500'}`}>
                                    {t.loginDesc}
                                </p>
                            </div>

                            {/* Interactive Role Switcher Pill Container */}
                            <div className="space-y-2">
                                <label className="block text-[11px] font-bold uppercase tracking-wider text-slate-400">
                                    Selecciona tu tipo de cuenta
                                </label>
                                <div className={`grid grid-cols-4 gap-2 p-1.5 rounded-2xl border ${isDarkMode ? 'bg-slate-800/70 border-slate-700/60' : 'bg-slate-200/70 border-slate-300/60'}`}>
                                    {([
                                        { id: 'student', label: t.student },
                                        { id: 'teacher', label: t.teacher },
                                        { id: 'jury', label: t.jury },
                                        { id: 'admin', label: t.admin }
                                    ] as const).map((r) => (
                                        <button
                                            key={r.id}
                                            type="button"
                                            onClick={() => handleRoleSelect(r.id)}
                                            className={`py-2.5 px-3 rounded-xl text-xs font-bold transition-all duration-200 cursor-pointer ${
                                                role === r.id
                                                    ? 'bg-red-600 text-white shadow-lg shadow-red-600/30'
                                                    : `${isDarkMode ? 'text-slate-300 hover:text-white' : 'text-slate-600 hover:text-slate-900'}`
                                            }`}
                                        >
                                            {r.label}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            {/* Main Login Form */}
                            <form onSubmit={handleSubmit} className="space-y-5">

                                {/* Email Field with Quick Domain Append */}
                                <div className="space-y-2">
                                    <div className="flex justify-between items-center">
                                        <label className="block text-xs font-bold uppercase tracking-wider text-slate-500">
                                            {t.emailLabel}
                                        </label>
                                        <button
                                            type="button"
                                            onClick={handleQuickDomain}
                                            className="text-[11px] font-semibold text-red-600 dark:text-red-400 hover:underline cursor-pointer"
                                        >
                                            + @itca.edu.sv
                                        </button>
                                    </div>

                                    <div className="relative group">
                                        <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-slate-400 group-focus-within:text-red-600 transition-colors">
                                            <Mail className="w-5 h-5" />
                                        </div>
                                        <input
                                            type="email"
                                            required
                                            value={email}
                                            onChange={(e) => setEmail(e.target.value)}
                                            placeholder="usuario@itca.edu.sv"
                                            className={`w-full pl-12 pr-4 py-4 rounded-2xl border text-sm transition-all outline-none ${
                                                isDarkMode
                                                    ? 'bg-slate-800 border-slate-700 text-white placeholder-slate-500 focus:border-red-500 focus:ring-4 focus:ring-red-500/15'
                                                    : 'bg-white border-slate-200 text-slate-900 placeholder-slate-400 focus:border-red-600 focus:ring-4 focus:ring-red-600/15 shadow-sm'
                                            }`}
                                        />
                                    </div>
                                    {error && (
                                        <p className="text-xs text-red-600" role="alert">{error}</p>
                                    )}
                                </div>

                                {/* Password Field */}
                                <div className="space-y-2">
                                    <label className="block text-xs font-bold uppercase tracking-wider text-slate-500">
                                        {t.passwordLabel}
                                    </label>

                                    <div className="relative group">
                                        <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none text-slate-400 group-focus-within:text-red-600 transition-colors">
                                            <Lock className="w-5 h-5" />
                                        </div>
                                        <input
                                            type={showPassword ? 'text' : 'password'}
                                            required
                                            value={password}
                                            onChange={(e) => setPassword(e.target.value)}
                                            placeholder="••••••••••••"
                                            className={`w-full pl-12 pr-12 py-4 rounded-2xl border text-sm transition-all outline-none ${
                                                isDarkMode
                                                    ? 'bg-slate-800 border-slate-700 text-white placeholder-slate-500 focus:border-red-500 focus:ring-4 focus:ring-red-500/15'
                                                    : 'bg-white border-slate-200 text-slate-900 placeholder-slate-400 focus:border-red-600 focus:ring-4 focus:ring-red-600/15 shadow-sm'
                                            }`}
                                        />
                                        <button
                                            type="button"
                                            onClick={() => setShowPassword(!showPassword)}
                                            className="absolute inset-y-0 right-0 pr-4 flex items-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition-colors cursor-pointer"
                                        >
                                            {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                                        </button>
                                    </div>

                                    {/* Password Strength Visual Meter */}
                                    {password && (
                                        <div className="space-y-1.5 pt-1 animate-fade-in">
                                            <div className="h-1.5 w-full bg-slate-200 dark:bg-slate-700 rounded-full overflow-hidden">
                                                <div
                                                    className={`h-full transition-all duration-300 ${pwdStrength.color}`}
                                                    style={{ width: `${pwdStrength.score}%` }}
                                                />
                                            </div>
                                            <div className="flex justify-between items-center text-[11px]">
                                                <span className="text-slate-400">Fortaleza clave:</span>
                                                <span className={`font-bold ${pwdStrength.text}`}>{pwdStrength.label}</span>
                                            </div>
                                        </div>
                                    )}
                                </div>

                                {/* Checkbox Remember Me & Forgot Password */}
                                <div className="flex items-center justify-between text-xs pt-1">
                                    <label className="flex items-center gap-2 cursor-pointer select-none">
                                        <input
                                            type="checkbox"
                                            checked={rememberMe}
                                            onChange={(e) => setRememberMe(e.target.checked)}
                                            className="w-4 h-4 rounded border-slate-300 text-red-600 focus:ring-red-500 accent-red-600"
                                        />
                                        <span className={isDarkMode ? 'text-slate-400' : 'text-slate-600'}>{t.rememberMe}</span>
                                    </label>

                                    <button
                                        type="button"
                                        onClick={() => setShowForgotModal(true)}
                                        className="font-semibold text-red-600 hover:text-red-700 dark:text-red-400 transition-colors cursor-pointer"
                                    >
                                        {t.forgotPassword}
                                    </button>
                                </div>

                                {/* Submit Button */}
                                <button
                                    type="submit"
                                    disabled={isLoading}
                                    className="w-full py-4 px-6 rounded-2xl bg-gradient-to-r from-red-600 via-red-700 to-red-800 hover:from-red-700 hover:to-red-900 text-white font-bold text-sm shadow-xl shadow-red-600/30 hover:shadow-red-600/50 active:scale-[0.98] transition-all duration-200 flex items-center justify-center gap-3 disabled:opacity-70 disabled:cursor-not-allowed group cursor-pointer"
                                >
                                    {isLoading ? (
                                        <>
                                            <Loader2 className="w-5 h-5 animate-spin text-white" />
                                            <span>Validando credenciales...</span>
                                        </>
                                    ) : (
                                        <>
                                            <span>{t.submitBtn}</span>
                                            <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                                        </>
                                    )}
                                </button>
                            </form>

                            {/* Help Desk Link */}
                            <div className={`text-center pt-6 text-xs border-t ${isDarkMode ? 'border-slate-800 text-slate-500' : 'border-slate-200 text-slate-400'}`}>
                                ¿Problemas para acceder a tu cuenta?{' '}
                                <a href="mailto:mesadeayuda@itca.edu.sv" className="text-red-600 dark:text-red-400 font-bold hover:underline">
                                    Mesa de Ayuda Técnica
                                </a>
                            </div>

                        </div>
                    </div>

                </div>
            )}

            {/* ================= RECOVERY CODE MODAL ================= */}
            {showForgotModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-md animate-fade-in">
                    <div className={`w-full max-w-md p-8 rounded-3xl shadow-2xl relative border ${isDarkMode ? 'bg-slate-900 border-slate-800 text-white' : 'bg-white border-slate-100 text-slate-900'} animate-scale-up`}>

                        <button
                            onClick={cerrarRecuperacion}
                            className="absolute top-5 right-5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-2 rounded-full transition-colors cursor-pointer"
                        >
                            <X className="w-5 h-5" />
                        </button>

                        <div className="w-12 h-12 rounded-2xl bg-red-500/10 text-red-600 dark:text-red-400 flex items-center justify-center mb-4">
                            <KeyRound className="w-6 h-6" />
                        </div>

                        <h3 className="text-2xl font-black mb-1">
                            {modalStep === 1 ? 'Recuperar Contraseña' : 'Código de Verificación'}
                        </h3>
                        <p className="text-xs text-slate-500 mb-6 leading-relaxed">
                            {modalStep === 1
                                ? 'Ingresa tu correo institucional registrado. Te enviaremos un código de seguridad de 6 dígitos.'
                                : `Ingresa el código enviado a ${resetEmail || 'tu correo registrado'}.`}
                        </p>

                        {isResetSuccess ? (
                            <div className="p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-600 dark:text-emerald-400 text-xs flex items-center gap-3">
                                <CheckCircle2 className="w-6 h-6 flex-shrink-0" />
                                <div>
                                    <p className="font-bold">¡Contraseña restablecida!</p>
                                    <p className="text-[11px] opacity-80">Ya puedes iniciar sesión con tu nueva contraseña.</p>
                                </div>
                            </div>
                        ) : (
                            <form onSubmit={handleResetSubmit} className="space-y-5">
                                {modalStep === 1 ? (
                                    <div>
                                        <label className="block text-xs font-bold uppercase tracking-wider text-slate-400 mb-2">
                                            Correo Institucional
                                        </label>
                                        <input
                                            type="email"
                                            required
                                            value={resetEmail}
                                            onChange={(e) => setResetEmail(e.target.value)}
                                            placeholder="usuario@itca.edu.sv"
                                            className={`w-full px-4 py-3.5 rounded-2xl border text-sm outline-none transition-all ${
                                                isDarkMode
                                                    ? 'bg-slate-800 border-slate-700 text-white focus:border-red-500'
                                                    : 'bg-slate-50 border-slate-200 text-slate-900 focus:border-red-600'
                                            }`}
                                        />
                                    </div>
                                ) : (
                                    <div className="space-y-4">
                                        <div>
                                            <label className="block text-xs font-bold uppercase tracking-wider text-slate-400 mb-3">
                                                Código de 6 dígitos
                                            </label>
                                            <div className="flex justify-between gap-2">
                                                {resetCode.map((digit, idx) => (
                                                    <input
                                                        key={idx}
                                                        id={`code-input-desktop-${idx}`}
                                                        type="text"
                                                        inputMode="numeric"
                                                        maxLength={1}
                                                        value={digit}
                                                        onChange={(e) => handleCodeChange(idx, e.target.value)}
                                                        className={`w-12 h-12 text-center text-xl font-bold rounded-2xl border outline-none ${
                                                            isDarkMode
                                                                ? 'bg-slate-800 border-slate-700 text-white focus:border-red-500'
                                                                : 'bg-slate-50 border-slate-200 text-slate-900 focus:border-red-600'
                                                        }`}
                                                    />
                                                ))}
                                            </div>
                                        </div>
                                        <input type="password" value={resetPassword} onChange={(e) => setResetPassword(e.target.value)}
                                            placeholder="Nueva contraseña (mínimo 8 caracteres)" className={campo} />
                                        <input type="password" value={resetPassword2} onChange={(e) => setResetPassword2(e.target.value)}
                                            placeholder="Repite la nueva contraseña" className={campo} />
                                    </div>
                                )}

                                {resetError && <p className="text-xs font-semibold text-red-600">{resetError}</p>}

                                <div className="flex gap-3 pt-2">
                                    <button
                                        type="button"
                                        onClick={cerrarRecuperacion}
                                        className="w-1/2 py-3.5 rounded-2xl border border-slate-300 dark:border-slate-700 text-xs font-bold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
                                    >
                                        Cancelar
                                    </button>
                                    <button
                                        type="submit"
                                        disabled={resetLoading}
                                        className="w-1/2 py-3.5 rounded-2xl bg-red-600 hover:bg-red-700 text-white text-xs font-bold shadow-lg shadow-red-600/30 transition-all cursor-pointer"
                                    >
                                        {resetLoading ? 'Procesando...' : modalStep === 1 ? 'Enviar Código' : 'Restablecer'}
                                    </button>
                                </div>
                            </form>
                        )}

                    </div>
                </div>
            )}

            {/* Tailwind Animations */}
            <style>{`
        @keyframes fadeIn {
          from { opacity: 0; }
          to { opacity: 1; }
        }
        @keyframes scaleUp {
          from { opacity: 0; transform: scale(0.96); }
          to { opacity: 1; transform: scale(1); }
        }
        .animate-fade-in { animation: fadeIn 0.3s ease-out forwards; }
        .animate-scale-up { animation: scaleUp 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards; }
      `}</style>
        </div>
    );
}