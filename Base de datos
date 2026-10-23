-- Crear la base de datos
CREATE DATABASE IF NOT EXISTS evaluacion_proyectos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE evaluacion_proyectos;

-- 1. Tabla de Roles (Protegida con RESTRICT para evitar borrar roles con usuarios activos)
CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE -- 'alumno', 'jurado', 'docente'
);

-- 2. Tabla de Usuarios (Alumnos, Jurados, Docentes con BIGINT)
CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    correo VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    rol_id BIGINT NOT NULL,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES roles(id) ON DELETE RESTRICT
);

-- 3. Tabla de Materias (Asociada a su Docente Principal - HU-04)
CREATE TABLE materias (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    docente_principal_id BIGINT NOT NULL,
    CONSTRAINT fk_materia_docente FOREIGN KEY (docente_principal_id) REFERENCES usuarios(id) ON DELETE RESTRICT
);

-- 4. Tabla de Secciones (HU-04)
CREATE TABLE secciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL, -- Ej: "Sección 01"
    materia_id BIGINT NOT NULL,
    ciclo VARCHAR(20) NOT NULL,    -- Ej: "Ciclo II-2026"
    CONSTRAINT fk_seccion_materia FOREIGN KEY (materia_id) REFERENCES materias(id) ON DELETE CASCADE
);

-- 5. Tabla de Grupos (HU-05: Los alumnos se organizan en grupos dentro de una sección)
CREATE TABLE grupos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL, -- Ej: "Grupo Alfa", "Equipo 1"
    seccion_id BIGINT NOT NULL,
    CONSTRAINT fk_grupo_seccion FOREIGN KEY (seccion_id) REFERENCES secciones(id) ON DELETE CASCADE
);

-- 6. Tabla Intermedia: Alumnos por Grupo (HU-05: Controla los integrantes de cada grupo)
CREATE TABLE grupo_alumnos (
    grupo_id BIGINT,
    alumno_id BIGINT,
    PRIMARY KEY (grupo_id, alumno_id),
    CONSTRAINT fk_grupokalum_grupo FOREIGN KEY (grupo_id) REFERENCES grupos(id) ON DELETE CASCADE,
    CONSTRAINT fk_grupokalum_alumno FOREIGN KEY (alumno_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- 7. Tabla de Empresas (HU-06)
CREATE TABLE empresas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    contacto VARCHAR(100),
    correo VARCHAR(150),
    telefono VARCHAR(20)
);

-- 8. Tabla de Proyectos (HU-07: Vinculado a grupo, materia, sección, empresa y control de publicación HU-13)
CREATE TABLE proyectos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    descripcion TEXT,
    empresa_id BIGINT,
    materia_id BIGINT NOT NULL,
    seccion_id BIGINT NOT NULL,
    grupo_id BIGINT NOT NULL UNIQUE, -- Un grupo desarrolla un proyecto y un proyecto pertenece a un grupo
    resultados_publicados BOOLEAN DEFAULT FALSE, -- HU-13: Controla si el estudiante ya puede ver su nota
    CONSTRAINT fk_proyecto_empresa FOREIGN KEY (empresa_id) REFERENCES empresas(id) ON DELETE SET NULL,
    CONSTRAINT fk_proyecto_materia FOREIGN KEY (materia_id) REFERENCES materias(id) ON DELETE CASCADE,
    CONSTRAINT fk_proyecto_seccion FOREIGN KEY (seccion_id) REFERENCES secciones(id) ON DELETE CASCADE,
    CONSTRAINT fk_proyecto_grupo FOREIGN KEY (grupo_id) REFERENCES grupos(id) ON DELETE CASCADE
);

-- 9. Criterios de Evaluación Configurables (HU-08)
CREATE TABLE criterios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    materia_id BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL, -- Ej: "Innovación", "Defensa", "Código"
    descripcion TEXT,
    ponderacion DECIMAL(5,2) NOT NULL, -- Porcentaje (ej: 30.00 para 30%)
    CONSTRAINT fk_criterio_materia FOREIGN KEY (materia_id) REFERENCES materias(id) ON DELETE CASCADE
);

-- 10. Evaluadores Invitados (HU-09)
CREATE TABLE evaluadores_invitados (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proyecto_id BIGINT NOT NULL,
    evaluador_id BIGINT NOT NULL,
    CONSTRAINT fk_invitado_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyectos(id) ON DELETE CASCADE,
    CONSTRAINT fk_invitado_usuario FOREIGN KEY (evaluador_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT uq_proyecto_evaluador UNIQUE (proyecto_id, evaluador_id)
);

-- 11. Cabecera de Evaluaciones (HU-10: Soporta estado BORRADOR o ENVIADA y evita duplicados)
CREATE TABLE evaluaciones (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    proyecto_id BIGINT NOT NULL,
    evaluador_id BIGINT NOT NULL,
    calificacion_final DECIMAL(5,2) DEFAULT 0.00,
    estado VARCHAR(20) DEFAULT 'BORRADOR', -- HU-10: 'BORRADOR' o 'ENVIADA'
    comentarios TEXT,
    fecha TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_evaluacion_proyecto FOREIGN KEY (proyecto_id) REFERENCES proyectos(id) ON DELETE CASCADE,
    CONSTRAINT fk_evaluacion_evaluador FOREIGN KEY (evaluador_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    CONSTRAINT uq_evaluador_proyecto UNIQUE (proyecto_id, evaluador_id)
);

-- 12. Detalle de Notas por Criterio (HU-10 y HU-11)
CREATE TABLE evaluacion_detalles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    evaluacion_id BIGINT NOT NULL,
    criterio_id BIGINT NOT NULL,
    nota_otorgada DECIMAL(5,2) NOT NULL,
    CONSTRAINT fk_detalle_evaluacion FOREIGN KEY (evaluacion_id) REFERENCES evaluaciones(id) ON DELETE CASCADE,
    CONSTRAINT fk_detalle_criterio FOREIGN KEY (criterio_id) REFERENCES criterios(id) ON DELETE CASCADE,
    CONSTRAINT uq_evaluacion_criterio UNIQUE (evaluacion_id, criterio_id)
);

-- Inserción inicial de los 3 roles base
INSERT INTO roles (nombre) VALUES ('alumno'), ('jurado'), ('docente');SSSSs
