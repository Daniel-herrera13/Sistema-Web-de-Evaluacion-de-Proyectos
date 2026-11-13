import type { ReactNode } from 'react';
import { COLORES } from './utilidades';

interface TarjetaProps {
  titulo: string;
  descripcion?: string;
  children: ReactNode;
}

export function Tarjeta({ titulo, descripcion, children }: TarjetaProps) {
  return (
    <article className="est-tarjeta">
      <h4 className="est-tarjeta__titulo">{titulo}</h4>
      {descripcion && <p className="est-tarjeta__desc">{descripcion}</p>}
      {children}
    </article>
  );
}

export interface ItemLeyenda {
  etiqueta: string;
  color?: string;
  /** Rayado dorado: así se marcan las notas provisionales (no depende solo del color). */
  rayado?: boolean;
}

export function Leyenda({ items }: { items: ItemLeyenda[] }) {
  return (
    <ul className="est-leyenda">
      {items.map((item) => (
        <li key={item.etiqueta} className="est-leyenda__item">
          <span
            className={`est-leyenda__marca${item.rayado ? ' est-leyenda__marca--rayada' : ''}`}
            style={item.rayado ? undefined : { background: item.color }}
            aria-hidden="true"
          />
          {item.etiqueta}
        </li>
      ))}
    </ul>
  );
}

interface AvisoProps {
  tipo?: 'vacio' | 'error';
  titulo: string;
  children?: ReactNode;
  accion?: ReactNode;
}

export function Aviso({ tipo = 'vacio', titulo, children, accion }: AvisoProps) {
  return (
    <div
      className={`est-aviso${tipo === 'error' ? ' est-aviso--error' : ''}`}
      role={tipo === 'error' ? 'alert' : 'status'}
    >
      <p className="est-aviso__titulo">{titulo}</p>
      {children && <p className="est-aviso__texto">{children}</p>}
      {accion}
    </div>
  );
}

export function CajaTooltip({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <div className="est-tooltip">
      <p className="est-tooltip__titulo">{titulo}</p>
      {children}
    </div>
  );
}

/**
 * Define el patrón de rayas para las barras provisionales.
 * Se llama como función (no como componente) para que <defs> sea hijo directo del gráfico de Recharts.
 */
export function definirPatronProvisional(id: string) {
  return (
    <defs>
      <pattern
        id={id}
        width="8"
        height="8"
        patternUnits="userSpaceOnUse"
        patternTransform="rotate(45)"
      >
        <rect width="8" height="8" fill={COLORES.dorado} />
        <rect width="3" height="8" fill={COLORES.crema} />
      </pattern>
    </defs>
  );
}
