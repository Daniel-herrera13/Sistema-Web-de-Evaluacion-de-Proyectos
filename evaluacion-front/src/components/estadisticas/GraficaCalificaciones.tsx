import { useMemo, useState } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  LabelList,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { useEsMovil } from '../../hooks/useEsMovil';
import type {
  Agrupacion,
  EstadisticasReporte,
  PromedioCriterio,
  PromedioItem,
  RangoNota,
} from '../../types/estadisticas';
import { Aviso, CajaTooltip, Leyenda, Tarjeta, definirPatronProvisional } from './Comunes';
import {
  COLORES,
  ESTILO_MARCA,
  ESTILO_TITULO_EJE,
  LIMITE_BARRAS,
  altoBarras,
  formatoNota,
  pluralizar,
  recortar,
} from './utilidades';

interface Props {
  datos: EstadisticasReporte;
  agrupar: Agrupacion;
}

const MARCAS_NOTA = [0, 2, 4, 6, 8, 10];

const LEYENDA_NOTAS = [
  { etiqueta: 'Definitiva (todos enviaron)', color: COLORES.rojo },
  { etiqueta: 'Provisional (faltan evaluaciones)', rayado: true },
];

// ------------------------------------------------------------------ tooltips

interface TooltipRangoProps {
  active?: boolean;
  payload?: ReadonlyArray<{ payload?: RangoNota }>;
}

function TooltipRango({ active, payload }: TooltipRangoProps) {
  const fila = payload?.[0]?.payload;
  if (!active || !fila) return null;
  const total = fila.definitivos + fila.provisionales;
  return (
    <CajaTooltip titulo={`Notas de ${fila.etiqueta}`}>
      <p>
        <strong>{pluralizar(total, 'proyecto', 'proyectos')}</strong>
      </p>
      <p>Definitivos: {fila.definitivos}</p>
      <p>Provisionales: {fila.provisionales}</p>
    </CajaTooltip>
  );
}

interface TooltipPromedioProps {
  active?: boolean;
  payload?: ReadonlyArray<{ payload?: PromedioItem }>;
}

function TooltipPromedio({ active, payload }: TooltipPromedioProps) {
  const fila = payload?.[0]?.payload;
  if (!active || !fila) return null;
  return (
    <CajaTooltip titulo={fila.nombre}>
      <p>
        Promedio: <strong>{formatoNota(fila.promedio)}</strong> de 10
      </p>
      <p>
        Evaluaciones enviadas: {fila.evaluacionesEnviadas} de {fila.evaluadoresInvitados}
      </p>
      <p>{fila.provisional ? 'Nota provisional' : 'Nota definitiva'}</p>
    </CajaTooltip>
  );
}

interface TooltipCriterioProps {
  active?: boolean;
  payload?: ReadonlyArray<{ payload?: PromedioCriterio }>;
}

function TooltipCriterio({ active, payload }: TooltipCriterioProps) {
  const fila = payload?.[0]?.payload;
  if (!active || !fila) return null;
  return (
    <CajaTooltip titulo={fila.nombre}>
      <p>
        Promedio: <strong>{formatoNota(fila.promedio)}</strong> de 10
      </p>
      <p>Ponderación: {fila.ponderacion.toLocaleString('es-SV')} %</p>
      <p>{fila.provisional ? 'Nota provisional' : 'Nota definitiva'}</p>
    </CajaTooltip>
  );
}

// --------------------------------------------------------------- componente

export default function GraficaCalificaciones({ datos, agrupar }: Props) {
  const movil = useEsMovil();
  const [verTodos, setVerTodos] = useState(false);

  const promedios = agrupar === 'proyecto' ? datos.promediosPorProyecto : datos.promediosPorGrupo;
  const nombreEje = agrupar === 'proyecto' ? 'Proyecto' : 'Grupo';
  const visibles = verTodos ? promedios : promedios.slice(0, LIMITE_BARRAS);

  const datosPromedios = useMemo(
    () => visibles.map((fila) => ({ ...fila, etiqueta: recortar(fila.nombre, movil ? 12 : 18) })),
    [visibles, movil],
  );

  const datosCriterios = useMemo(
    () =>
      datos.promediosPorCriterio.map((fila) => ({
        ...fila,
        etiqueta: `${recortar(fila.nombre, movil ? 16 : 26)} (${fila.ponderacion.toLocaleString('es-SV')} %)`,
      })),
    [datos.promediosPorCriterio, movil],
  );

  const nombreProyecto =
    datos.votosPorProyecto.find((p) => p.id === datos.criteriosProyectoId)?.nombre ?? 'el proyecto';

  if (!datos.hayCalificaciones) {
    return (
      <Aviso titulo="Aún no hay evaluaciones enviadas">
        Las calificaciones aparecerán aquí cuando al menos un evaluador envíe su evaluación.
      </Aviso>
    );
  }

  const idPatron = 'patron-provisional-notas';

  return (
    <>
      <div className="est__rejilla est__rejilla--2">
        <Tarjeta
          titulo="Distribución de notas finales"
          descripcion="Cuántos proyectos hay en cada rango de nota. Solo cuentan proyectos con al menos una evaluación enviada."
        >
          <div
            role="img"
            aria-label="Barras con la cantidad de proyectos por rango de nota final, de 0 a 10."
          >
            <ResponsiveContainer width="100%" height={300}>
              <BarChart
                data={datos.distribucion}
                margin={{ top: 8, right: 8, bottom: 28, left: 4 }}
              >
                {definirPatronProvisional(idPatron)}
                <CartesianGrid vertical={false} stroke={COLORES.borde} />
                <XAxis
                  dataKey="etiqueta"
                  tick={ESTILO_MARCA}
                  stroke={COLORES.borde}
                  label={{
                    value: 'Rango de nota final',
                    position: 'insideBottom',
                    offset: -14,
                    style: ESTILO_TITULO_EJE,
                  }}
                />
                <YAxis
                  allowDecimals={false}
                  tick={ESTILO_MARCA}
                  stroke={COLORES.borde}
                  width={44}
                  label={{
                    value: 'Proyectos',
                    angle: -90,
                    position: 'insideLeft',
                    style: ESTILO_TITULO_EJE,
                  }}
                />
                <Tooltip content={<TooltipRango />} cursor={{ fill: COLORES.crema }} />
                <Bar dataKey="definitivos" name="Definitivos" stackId="rango" fill={COLORES.rojo} />
                <Bar
                  dataKey="provisionales"
                  name="Provisionales"
                  stackId="rango"
                  fill={`url(#${idPatron})`}
                  radius={[6, 6, 0, 0]}
                />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <Leyenda items={LEYENDA_NOTAS} />
        </Tarjeta>

        <Tarjeta
          titulo={`Promedio por ${agrupar}`}
          descripcion="Promedio de las evaluaciones enviadas, de 0 a 10. Primero aparecen las notas más altas."
        >
          {datosPromedios.length === 0 ? (
            <Aviso titulo="No hay promedios para mostrar">
              Los proyectos de este reporte no tienen un grupo asignado o aún no tienen evaluaciones enviadas.
            </Aviso>
          ) : (
            <>
              <div
                role="img"
                aria-label={`Barras horizontales con el promedio de nota por ${agrupar}, de 0 a 10.`}
              >
                <ResponsiveContainer width="100%" height={altoBarras(datosPromedios.length)}>
                  <BarChart
                    data={datosPromedios}
                    layout="vertical"
                    margin={{ top: 4, right: 40, bottom: 28, left: 4 }}
                    barCategoryGap={8}
                  >
                    {definirPatronProvisional(`${idPatron}-prom`)}
                    <CartesianGrid horizontal={false} stroke={COLORES.borde} />
                    <XAxis
                      type="number"
                      domain={[0, 10]}
                      ticks={MARCAS_NOTA}
                      tick={ESTILO_MARCA}
                      stroke={COLORES.borde}
                      label={{
                        value: 'Nota promedio (0 a 10)',
                        position: 'insideBottom',
                        offset: -14,
                        style: ESTILO_TITULO_EJE,
                      }}
                    />
                    <YAxis
                      type="category"
                      dataKey="etiqueta"
                      width={movil ? 104 : 160}
                      tick={ESTILO_MARCA}
                      stroke={COLORES.borde}
                      label={{
                        value: nombreEje,
                        angle: -90,
                        position: 'insideLeft',
                        style: ESTILO_TITULO_EJE,
                      }}
                    />
                    <Tooltip content={<TooltipPromedio />} cursor={{ fill: COLORES.crema }} />
                    <Bar dataKey="promedio" name="Promedio" radius={[0, 4, 4, 0]}>
                      {datosPromedios.map((fila) => (
                        <Cell
                          key={fila.id}
                          fill={fila.provisional ? `url(#${idPatron}-prom)` : COLORES.rojo}
                        />
                      ))}
                      <LabelList
                        dataKey="promedio"
                        position="right"
                        formatter={(valor: unknown) => formatoNota(Number(valor))}
                        style={{ ...ESTILO_MARCA, fill: COLORES.tinta, fontWeight: 600 }}
                      />
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              </div>
              <Leyenda items={LEYENDA_NOTAS} />
              {promedios.length > LIMITE_BARRAS && (
                <button type="button" className="est__enlace" onClick={() => setVerTodos((v) => !v)}>
                  {verTodos
                    ? `Mostrar solo los primeros ${LIMITE_BARRAS}`
                    : `Ver todos (${promedios.length})`}
                </button>
              )}
            </>
          )}
        </Tarjeta>
      </div>

      {datosCriterios.length > 0 && (
        <div className="est__rejilla">
          <Tarjeta
            titulo="Promedio por criterio"
            descripcion={`Promedio de cada criterio en ${nombreProyecto}, de 0 a 10. Entre paréntesis, la ponderación del criterio.`}
          >
            <div
              role="img"
              aria-label="Barras horizontales con el promedio de nota por criterio, de 0 a 10."
            >
              <ResponsiveContainer width="100%" height={altoBarras(datosCriterios.length)}>
                <BarChart
                  data={datosCriterios}
                  layout="vertical"
                  margin={{ top: 4, right: 40, bottom: 28, left: 4 }}
                  barCategoryGap={8}
                >
                  {definirPatronProvisional(`${idPatron}-crit`)}
                  <CartesianGrid horizontal={false} stroke={COLORES.borde} />
                  <XAxis
                    type="number"
                    domain={[0, 10]}
                    ticks={MARCAS_NOTA}
                    tick={ESTILO_MARCA}
                    stroke={COLORES.borde}
                    label={{
                      value: 'Nota promedio (0 a 10)',
                      position: 'insideBottom',
                      offset: -14,
                      style: ESTILO_TITULO_EJE,
                    }}
                  />
                  <YAxis
                    type="category"
                    dataKey="etiqueta"
                    width={movil ? 128 : 220}
                    tick={ESTILO_MARCA}
                    stroke={COLORES.borde}
                    label={{
                      value: 'Criterio',
                      angle: -90,
                      position: 'insideLeft',
                      style: ESTILO_TITULO_EJE,
                    }}
                  />
                  <Tooltip content={<TooltipCriterio />} cursor={{ fill: COLORES.crema }} />
                  <Bar dataKey="promedio" name="Promedio" radius={[0, 4, 4, 0]}>
                    {datosCriterios.map((fila) => (
                      <Cell
                        key={fila.criterioId}
                        fill={fila.provisional ? `url(#${idPatron}-crit)` : COLORES.rojo}
                      />
                    ))}
                    <LabelList
                      dataKey="promedio"
                      position="right"
                      formatter={(valor: unknown) => formatoNota(Number(valor))}
                      style={{ ...ESTILO_MARCA, fill: COLORES.tinta, fontWeight: 600 }}
                    />
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
            <Leyenda items={LEYENDA_NOTAS} />
          </Tarjeta>
        </div>
      )}
    </>
  );
}
