import { useState } from 'react';
// Se asume que AuthContext expone useAuth() con user.rol ("ADMIN", "DOCENTE", "ROLE_ADMIN"...). Ajusta la ruta si difiere.
import { useAuth } from '../../context/AuthContext';
import { useEstadisticas } from '../../hooks/useEstadisticas';
import type { Agrupacion, FiltrosEstadisticas } from '../../types/estadisticas';
import { Aviso } from './Comunes';
import GraficaCalificaciones from './GraficaCalificaciones';
import GraficaVotos from './GraficaVotos';
import './estadisticas.css';

interface Props {
  /** Filtros actuales del reporte (los mismos que se envían a /api/reportes/proyectos). */
  filtros: FiltrosEstadisticas;
}

const OPCIONES: { valor: Agrupacion; etiqueta: string }[] = [
  { valor: 'proyecto', etiqueta: 'Proyecto' },
  { valor: 'grupo', etiqueta: 'Grupo' },
];

export default function EstadisticasReporte({ filtros }: Props) {
  const { usuario } = useAuth();
  const rol = String(usuario?.rol ?? '').replace(/^ROLE_/, '').toUpperCase();
  // JURADO y ALUMNO no ven las gráficas (el backend además responde 403).
  const permitido = rol === 'ADMIN' || rol === 'DOCENTE';

  const { datos, cargando, error, reintentar } = useEstadisticas(filtros, permitido);
  const [agrupar, setAgrupar] = useState<Agrupacion>('proyecto');

  if (!permitido) return null;

  return (
    <section className="est" aria-labelledby="est-titulo" aria-busy={cargando}>
      <header className="est__cabecera">
        <div>
          <h2 id="est-titulo" className="est__titulo">
            Estadísticas del reporte
          </h2>
          <p className="est__subtitulo">
            Resumen visual de los proyectos con los filtros que aplicaste.
          </p>
        </div>

        <div className="est__agrupar">
          <span id="est-agrupar-etiqueta" className="est__agrupar-etiqueta">
            Agrupar por
          </span>
          <div className="est__segmentado" role="radiogroup" aria-labelledby="est-agrupar-etiqueta">
            {OPCIONES.map((opcion) => (
              <button
                key={opcion.valor}
                type="button"
                role="radio"
                aria-checked={agrupar === opcion.valor}
                className="est__segmento"
                onClick={() => setAgrupar(opcion.valor)}
              >
                {opcion.etiqueta}
              </button>
            ))}
          </div>
        </div>
      </header>

      {cargando && (
        <div className="est__rejilla est__rejilla--2" role="status" aria-label="Cargando gráficas">
          <div className="est-esqueleto" />
          <div className="est-esqueleto" />
        </div>
      )}

      {!cargando && error && (
        <Aviso
          tipo="error"
          titulo="No se pudieron cargar las gráficas"
          accion={
            <button type="button" className="est__boton" onClick={reintentar}>
              Reintentar
            </button>
          }
        >
          {error}
        </Aviso>
      )}

      {!cargando && !error && datos && !datos.hayDatos && (
        <Aviso titulo="No hay datos para graficar">
          Ningún proyecto cumple los filtros aplicados. Cambia los filtros del reporte para ver las gráficas.
        </Aviso>
      )}

      {!cargando && !error && datos?.hayDatos && (
        <>
          <h3 className="est__seccion">Votos (evaluaciones enviadas)</h3>
          <GraficaVotos datos={datos} agrupar={agrupar} />

          <h3 className="est__seccion">Calificaciones</h3>
          <GraficaCalificaciones datos={datos} agrupar={agrupar} />
        </>
      )}
    </section>
  );
}
