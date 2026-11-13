export type Agrupacion = 'proyecto' | 'grupo';

/** Mismos filtros que el reporte, más proyectoId opcional para el promedio por criterio. */
export interface FiltrosEstadisticas {
  materiaId?: number;
  seccionId?: number;
  grupoId?: number;
  empresaId?: number;
  estado?: string;
  ciclo?: string;
  publicado?: boolean;
  proyectoId?: number;
}

export interface ResumenVotos {
  totalProyectos: number;
  totalInvitados: number;
  pendientes: number;
  porcentajeAvance: number;
  proyectosDefinitivos: number;
  proyectosProvisionales: number;
  proyectosSinEvaluaciones: number;
}

export interface AvanceVotos {
  id: number;
  nombre: string;
  invitados: number;
  enviadas: number;
  pendientes: number;
  definitiva: boolean;
}

export interface RangoNota {
  etiqueta: string;
  desde: number;
  hasta: number;
  definitivos: number;
  provisionales: number;
}

export interface PromedioItem {
  id: number;
  nombre: string;
  promedio: number;
  provisional: boolean;
  evaluacionesEnviadas: number;
  evaluadoresInvitados: number;
}

export interface PromedioCriterio {
  criterioId: number;
  nombre: string;
  ponderacion: number;
  promedio: number;
  provisional: boolean;
}

export interface EstadisticasReporte {
  resumen: ResumenVotos;
  votosPorProyecto: AvanceVotos[];
  votosPorGrupo: AvanceVotos[];
  distribucion: RangoNota[];
  promediosPorProyecto: PromedioItem[];
  promediosPorGrupo: PromedioItem[];
  promediosPorCriterio: PromedioCriterio[];
  criteriosProyectoId: number | null;
  hayDatos: boolean;
  hayCalificaciones: boolean;
}
