export interface Seccion {
    id: number
    nombre: string
    materiaId: number
    materias: string[]
    ciclo: string
}

export interface Subseccion {
    id: number
    letra: string
    seccionId: number
    nombre: string
}

export interface AlumnoBasico {
    id: number
    nombre: string
    apellido: string
    correo: string
}

export interface Matricula {
    id: number
    alumnoId: number
    nombre: string
    apellido: string
    correo: string
    subseccionId: number
    subseccionNombre: string
}
