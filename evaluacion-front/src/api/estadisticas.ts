import api from './axios';
import type { EstadisticasReporte, FiltrosEstadisticas } from '../types/estadisticas';

// Ruta relativa al baseURL de api/axios.ts (se asume que el baseURL ya incluye "/api").
const RUTA = '/reportes/estadisticas';

/** Quita los filtros vacíos para no enviar "?ciclo=&estado=" al backend. */
function limpiarFiltros(filtros: FiltrosEstadisticas): Record<string, string | number | boolean> {
  const params: Record<string, string | number | boolean> = {};
  Object.entries(filtros).forEach(([clave, valor]) => {
    if (valor === undefined || valor === null || valor === '') return;
    if (typeof valor === 'number' && Number.isNaN(valor)) return;
    params[clave] = valor;
  });
  return params;
}

export async function obtenerEstadisticas(
  filtros: FiltrosEstadisticas,
  signal?: AbortSignal,
): Promise<EstadisticasReporte> {
  const { data } = await api.get<EstadisticasReporte>(RUTA, {
    params: limpiarFiltros(filtros),
    signal,
  });
  return data;
}
