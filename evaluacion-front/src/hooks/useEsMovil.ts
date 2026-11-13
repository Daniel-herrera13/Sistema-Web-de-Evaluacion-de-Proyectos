import { useEffect, useState } from 'react';

const CONSULTA = '(max-width: 640px)';

export function useEsMovil(): boolean {
  const [movil, setMovil] = useState<boolean>(
    () => typeof window !== 'undefined' && window.matchMedia(CONSULTA).matches,
  );

  useEffect(() => {
    const lista = window.matchMedia(CONSULTA);
    const alCambiar = (evento: MediaQueryListEvent) => setMovil(evento.matches);
    lista.addEventListener('change', alCambiar);
    return () => lista.removeEventListener('change', alCambiar);
  }, []);

  return movil;
}
