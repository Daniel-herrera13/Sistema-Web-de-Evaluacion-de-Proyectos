import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { obtenerEstadisticas } from '../api/estadisticas';
import type { EstadisticasReporte, FiltrosEstadisticas } from '../types/estadisticas';

function mensajeDeError(error: unknown): string {
  if (axios.isAxiosError(error)) {
    if (error.response?.status === 403) {
      return 'No tienes permiso para ver las gráficas de este reporte.';
    }
    if (!error.response) {
      return 'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.';
    }
    const body = error.response.data as { message?: string; mensaje?: string } | undefined;
    const mensaje = body?.message ?? body?.mensaje;
    if (mensaje) return mensaje;
  }
  return 'No se pudieron cargar las gráficas. Inténtalo de nuevo.';
}

export function useEstadisticas(filtros: FiltrosEstadisticas, habilitado: boolean) {
  const [datos, setDatos] = useState<EstadisticasReporte | null>(null);
  const [cargando, setCargando] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [intento, setIntento] = useState(0);

  // Clave estable: evita recargar cuando el padre crea un objeto nuevo con los mismos valores.
  const clave = JSON.stringify(filtros);

  useEffect(() => {
    if (!habilitado) return;

    const controlador = new AbortController();
    setCargando(true);
    setError(null);

    obtenerEstadisticas(JSON.parse(clave) as FiltrosEstadisticas, controlador.signal)
      .then((respuesta) => setDatos(respuesta))
      .catch((e: unknown) => {
        if (axios.isCancel(e)) return;
        setDatos(null);
        setError(mensajeDeError(e));
      })
      .finally(() => {
        if (!controlador.signal.aborted) setCargando(false);
      });

    return () => controlador.abort();
  }, [clave, habilitado, intento]);

  const reintentar = useCallback(() => setIntento((n) => n + 1), []);

  return { datos, cargando, error, reintentar };
}
