/** Paleta de la guía visual ITCA-FEPADE. */
export const COLORES = {
  rojo: '#B5260A',
  dorado: '#F5B800',
  crema: '#F7F3EE',
  tinta: '#241B17',
  gris: '#7A6F68',
  borde: '#E8E1D9',
  exito: '#2E7D32',
  error: '#B3261E',
} as const;

export const FUENTE = "'Plus Jakarta Sans', system-ui, sans-serif";

/** Estilo común de las marcas de los ejes de Recharts. */
export const ESTILO_MARCA = { fill: COLORES.gris, fontSize: 12, fontFamily: FUENTE };

/** Estilo común de los títulos de los ejes. */
export const ESTILO_TITULO_EJE = { fill: COLORES.gris, fontSize: 12, fontWeight: 600, fontFamily: FUENTE };

/** Cantidad de barras que se muestran antes de pedir "Ver todos". */
export const LIMITE_BARRAS = 12;

export function recortar(texto: string, maximo: number): string {
  return texto.length > maximo ? `${texto.slice(0, maximo - 1)}…` : texto;
}

export function formatoNota(valor: number): string {
  return valor.toLocaleString('es-SV', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function formatoPorcentaje(valor: number): string {
  return `${valor.toLocaleString('es-SV', { maximumFractionDigits: 1 })} %`;
}

export function pluralizar(cantidad: number, singular: string, plural: string): string {
  return `${cantidad} ${cantidad === 1 ? singular : plural}`;
}

/** Alto de un gráfico de barras horizontales según la cantidad de filas. */
export function altoBarras(filas: number): number {
  return Math.max(220, filas * 34 + 72);
}
