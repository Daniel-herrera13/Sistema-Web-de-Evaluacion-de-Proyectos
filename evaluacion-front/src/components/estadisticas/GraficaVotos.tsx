import { useMemo, useState } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { useEsMovil } from '../../hooks/useEsMovil';
import type { Agrupacion, AvanceVotos, EstadisticasReporte } from '../../types/estadisticas';
import { Aviso, CajaTooltip, Leyenda, Tarjeta } from './Comunes';
import {
  COLORES,
  ESTILO_MARCA,
  ESTILO_TITULO_EJE,
  LIMITE_BARRAS,
  altoBarras,
  formatoPorcentaje,
  pluralizar,
  recortar,
} from './utilidades';

interface Props {
  datos: EstadisticasReporte;
  agrupar: Agrupacion;
}

interface TooltipVotosProps {
  active?: boolean;
  payload?: ReadonlyArray<{ payload?: AvanceVotos }>;
}

function TooltipVotos({ active, payload }: TooltipVotosProps) {
  const fila = payload?.[0]?.payload;
  if (!active || !fila) return null;

  let estado = 'Sin evaluaciones enviadas';
  if (fila.enviadas > 0) estado = fila.definitiva ? 'Nota definitiva' : 'Nota provisional';

  return (
    <CajaTooltip titulo={fila.nombre}>
      <p>
        Enviadas: <strong>{fila.enviadas}</strong> de {fila.invitados}
      </p>
      <p>
        Pendientes: <strong>{fila.pendientes}</strong>
      </p>
      <p>{estado}</p>
    </CajaTooltip>
  );
}

interface TooltipDonaProps {
  active?: boolean;
  payload?: ReadonlyArray<{ name?: string | number; value?: number | string }>;
}

function TooltipDona({ active, payload }: TooltipDonaProps) {
  const item = payload?.[0];
  if (!active || !item) return null;
  return (
    <CajaTooltip titulo={String(item.name ?? '')}>
      <p>{pluralizar(Number(item.value ?? 0), 'evaluación', 'evaluaciones')}</p>
    </CajaTooltip>
  );
}

export default function GraficaVotos({ datos, agrupar }: Props) {
  const movil = useEsMovil();
  const [verTodos, setVerTodos] = useState(false);

  const { resumen } = datos;
  const lista = agrupar === 'proyecto' ? datos.votosPorProyecto : datos.votosPorGrupo;
  const nombreEje = agrupar === 'proyecto' ? 'Proyecto' : 'Grupo';

  // Primero los que más evaluaciones tienen pendientes: es lo que el docente necesita ver.
  const ordenada = useMemo(
    () =>
      [...lista].sort(
        (a, b) => b.pendientes - a.pendientes || a.nombre.localeCompare(b.nombre, 'es'),
      ),
    [lista],
  );
  const visibles = verTodos ? ordenada : ordenada.slice(0, LIMITE_BARRAS);
  const datosBarras = useMemo(
    () => visibles.map((fila) => ({ ...fila, etiqueta: recortar(fila.nombre, movil ? 12 : 18) })),
    [visibles, movil],
  );

  const enviadas = resumen.totalInvitados - resumen.pendientes;
  const dona = [
    { name: 'Enviadas', value: enviadas, color: COLORES.exito },
    { name: 'Pendientes', value: resumen.pendientes, color: COLORES.dorado },
  ];
  const leyenda = [
    { etiqueta: 'Enviadas', color: COLORES.exito },
    { etiqueta: 'Pendientes', color: COLORES.dorado },
  ];

  const sinInvitados = resumen.totalInvitados === 0;

  return (
    <div className="est__rejilla est__rejilla--2">
      <Tarjeta
        titulo="Avance general"
        descripcion="Evaluaciones enviadas frente a las pendientes de todos los proyectos del reporte."
      >
        {sinInvitados ? (
          <Aviso titulo="Aún no hay evaluadores invitados">
            Los proyectos de este reporte todavía no tienen evaluadores invitados.
          </Aviso>
        ) : (
          <>
            <div
              className="est-dona"
              role="img"
              aria-label={`Avance general: ${enviadas} de ${resumen.totalInvitados} evaluaciones enviadas, ${formatoPorcentaje(resumen.porcentajeAvance)}.`}
            >
              <ResponsiveContainer width="100%" height={240}>
                <PieChart>
                  <Pie
                    data={dona}
                    dataKey="value"
                    nameKey="name"
                    innerRadius="62%"
                    outerRadius="90%"
                    paddingAngle={resumen.pendientes > 0 && enviadas > 0 ? 2 : 0}
                    startAngle={90}
                    endAngle={-270}
                    stroke="none"
                  >
                    {dona.map((tramo) => (
                      <Cell key={tramo.name} fill={tramo.color} />
                    ))}
                  </Pie>
                  <Tooltip content={<TooltipDona />} />
                </PieChart>
              </ResponsiveContainer>
              <div className="est-dona__centro">
                <span className="est-dona__valor">{formatoPorcentaje(resumen.porcentajeAvance)}</span>
                <span className="est-dona__texto">
                  {enviadas} de {resumen.totalInvitados} enviadas
                </span>
              </div>
            </div>
            <Leyenda items={leyenda} />
            <dl className="est-cifras">
              <div>
                <dt>Con nota definitiva</dt>
                <dd>{resumen.proyectosDefinitivos}</dd>
              </div>
              <div>
                <dt>Con nota provisional</dt>
                <dd>{resumen.proyectosProvisionales}</dd>
              </div>
              <div>
                <dt>Sin evaluaciones enviadas</dt>
                <dd>{resumen.proyectosSinEvaluaciones}</dd>
              </div>
              <div>
                <dt>Proyectos en el reporte</dt>
                <dd>{resumen.totalProyectos}</dd>
              </div>
            </dl>
          </>
        )}
      </Tarjeta>

      <Tarjeta
        titulo={`Votos por ${agrupar}`}
        descripcion="Evaluadores invitados que ya enviaron su evaluación y los que faltan. Primero aparecen los que tienen más pendientes."
      >
        {sinInvitados || datosBarras.length === 0 ? (
          <Aviso titulo={agrupar === 'grupo' ? 'No hay grupos para mostrar' : 'No hay votos para mostrar'}>
            {agrupar === 'grupo'
              ? 'Los proyectos de este reporte no tienen un grupo asignado o no tienen evaluadores invitados.'
              : 'Cuando haya evaluadores invitados, verás aquí su avance.'}
          </Aviso>
        ) : (
          <>
            <div
              role="img"
              aria-label={`Barras apiladas con evaluaciones enviadas y pendientes por ${agrupar}.`}
            >
              <ResponsiveContainer width="100%" height={altoBarras(datosBarras.length)}>
                <BarChart
                  data={datosBarras}
                  layout="vertical"
                  margin={{ top: 4, right: 16, bottom: 28, left: 4 }}
                  barCategoryGap={8}
                >
                  <CartesianGrid horizontal={false} stroke={COLORES.borde} />
                  <XAxis
                    type="number"
                    allowDecimals={false}
                    tick={ESTILO_MARCA}
                    stroke={COLORES.borde}
                    label={{
                      value: 'Evaluadores invitados',
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
                  <Tooltip content={<TooltipVotos />} cursor={{ fill: COLORES.crema }} />
                  <Bar dataKey="enviadas" name="Enviadas" stackId="votos" fill={COLORES.exito} />
                  <Bar
                    dataKey="pendientes"
                    name="Pendientes"
                    stackId="votos"
                    fill={COLORES.dorado}
                    radius={[0, 4, 4, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <Leyenda items={leyenda} />
            {ordenada.length > LIMITE_BARRAS && (
              <button type="button" className="est__enlace" onClick={() => setVerTodos((v) => !v)}>
                {verTodos ? `Mostrar solo los primeros ${LIMITE_BARRAS}` : `Ver todos (${ordenada.length})`}
              </button>
            )}
          </>
        )}
      </Tarjeta>
    </div>
  );
}
