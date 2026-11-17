package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EsquemaDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.CriterioEsquema;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.CriterioEsquema.Modalidad;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EsquemaEvaluacion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Materia;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.AsignacionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.CriterioEsquemaRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EsquemaEvaluacionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Esquema de modalidades por materia y ciclo. ciclo null = esquema general.
 * Para un proyecto se usa el esquema de su ciclo; si no existe, el general; si tampoco, 100% docente/jurado.
 */
@Service
@RequiredArgsConstructor
public class EsquemaService {
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final Pattern CICLO = Pattern.compile("^\\d{2}-\\d{4}$");

    private final EsquemaEvaluacionRepository esquemas;
    private final CriterioEsquemaRepository criteriosRepo;
    private final MateriaRepository materias;
    private final AccesoService acceso;
    private final AsignacionRepository asignaciones;

    /** Para desactivar el bloqueo durante el desarrollo: app.criterios.bloquear-en-uso=false */
    @Value("${app.criterios.bloquear-en-uso:true}")
    private boolean bloquearEnUso;

    // ---------- consulta y edición (pantalla Modalidades) ----------
    @Transactional(readOnly = true)
    public EsquemaDto.Response obtener(Long materiaId, String correo) {
        return obtener(materiaId, null, correo);
    }

    @Transactional(readOnly = true)
    public EsquemaDto.Response obtener(Long materiaId, String cicloTxt, String correo) {
        Materia m = materia(materiaId);
        Usuario actor = acceso.actor(correo);
        exigirGestion(actor, m);
        String ciclo = ciclo(cicloTxt);
        boolean enUso = bloquearEnUso && afectaEvaluaciones(materiaId, ciclo);

        Optional<EsquemaEvaluacion> propio = exacto(materiaId, ciclo);
        if (propio.isPresent()) return aDto(propio.get(), ciclo, false, enUso);

        if (ciclo != null) {   // el ciclo no tiene esquema propio: se muestra el general como punto de partida
            Optional<EsquemaEvaluacion> general = exacto(materiaId, null);
            if (general.isPresent()) return aDto(general.get(), ciclo, true, enUso);
        }
        return new EsquemaDto.Response(materiaId, CIEN, BigDecimal.ZERO, BigDecimal.ZERO,
                List.of(), List.of(), ciclo, false, enUso);
    }

    /** Ciclos de la materia que tienen un esquema propio. */
    @Transactional(readOnly = true)
    public List<String> ciclosConEsquema(Long materiaId, String correo) {
        exigirGestion(acceso.actor(correo), materia(materiaId));
        return esquemas.findAllByMateriaId(materiaId).stream()
                .map(EsquemaEvaluacion::getCiclo).filter(Objects::nonNull).sorted().toList();
    }

    @Transactional
    public EsquemaDto.Response guardar(Long materiaId, EsquemaDto.Guardar r, String correo) {
        return guardar(materiaId, null, r, correo);
    }

    @Transactional
    public EsquemaDto.Response guardar(Long materiaId, String cicloTxt, EsquemaDto.Guardar r, String correo) {
        Materia m = materia(materiaId);
        Usuario actor = acceso.actor(correo);
        exigirGestion(actor, m);
        String ciclo = ciclo(cicloTxt);

        BigDecimal total = r.pesoDocenteJurado().add(r.pesoAutoevaluacion()).add(r.pesoPublico());
        if (total.compareTo(CIEN) != 0)
            throw Http.invalido("Los porcentajes de las modalidades deben sumar exactamente 100% (suma enviada: "
                    + total + "%)");

        List<EsquemaDto.Item> auto = r.criteriosAutoevaluacion() == null ? List.of() : r.criteriosAutoevaluacion();
        List<EsquemaDto.Item> pub = r.criteriosPublico() == null ? List.of() : r.criteriosPublico();
        validarConjunto("autoevaluación", r.pesoAutoevaluacion(), auto);
        validarConjunto("público", r.pesoPublico(), pub);

        EsquemaEvaluacion propio = exacto(materiaId, ciclo).orElse(null);

        // Si ya hay evaluaciones que dependen de este esquema, solo se acepta guardar sin cambios
        if (bloquearEnUso && !"admin".equals(acceso.rol(actor)) && afectaEvaluaciones(materiaId, ciclo)
                && (propio == null || !sinCambios(propio, r, auto, pub)))
            throw Http.conflicto("Ya hay evaluaciones con este esquema"
                    + (ciclo == null ? "" : " en el ciclo " + ciclo)
                    + " y no se puede cambiar. Elige otro ciclo (por ejemplo el siguiente) y guarda ahí el nuevo esquema.");

        EsquemaEvaluacion e = propio != null ? propio : new EsquemaEvaluacion();
        if (propio == null) {
            e.setMateria(m);
            e.setCiclo(ciclo);
        }
        e.setPesoDocenteJurado(r.pesoDocenteJurado());
        e.setPesoAutoevaluacion(r.pesoAutoevaluacion());
        e.setPesoPublico(r.pesoPublico());
        e = esquemas.save(e);

        reemplazar(e, Modalidad.AUTOEVALUACION, auto);
        reemplazar(e, Modalidad.PUBLICO, pub);
        return aDto(e, ciclo, false, false);
    }

    // ---------- usados por otros servicios ----------
    /** Esquema que se aplica a un proyecto: el de su ciclo, si no el general, si no el predeterminado. */
    @Transactional(readOnly = true)
    public EsquemaEvaluacion deProyecto(Proyecto p) {
        return resolver(materiaDe(p), cicloDe(p)).orElseGet(EsquemaEvaluacion::new);
    }

    /** Criterios de autoevaluación o de público del esquema que se aplica a un proyecto. */
    @Transactional(readOnly = true)
    public List<CriterioEsquema> criterios(Proyecto p, Modalidad modalidad) {
        return resolver(materiaDe(p), cicloDe(p))
                .map(e -> criteriosRepo.findByEsquemaIdAndModalidadAndActivoTrue(e.getId(), modalidad))
                .orElse(List.of());
    }

    /** Compatibilidad: esquema general de la materia (sin ciclo). */
    @Transactional(readOnly = true)
    public EsquemaEvaluacion deMateria(Long materiaId) {
        return exacto(materiaId, null).orElseGet(EsquemaEvaluacion::new);
    }

    /** Compatibilidad: criterios del esquema general de la materia (sin ciclo). */
    @Transactional(readOnly = true)
    public List<CriterioEsquema> criterios(Long materiaId, Modalidad modalidad) {
        return exacto(materiaId, null)
                .map(e -> criteriosRepo.findByEsquemaIdAndModalidadAndActivoTrue(e.getId(), modalidad))
                .orElse(List.of());
    }

    // ---------- resolución ----------
    private Optional<EsquemaEvaluacion> resolver(Long materiaId, String ciclo) {
        if (ciclo != null) {
            Optional<EsquemaEvaluacion> propio = esquemas.findByMateriaIdAndCiclo(materiaId, ciclo);
            if (propio.isPresent()) return propio;
        }
        return esquemas.findByMateriaIdAndCicloIsNull(materiaId);
    }

    private Optional<EsquemaEvaluacion> exacto(Long materiaId, String ciclo) {
        return ciclo == null ? esquemas.findByMateriaIdAndCicloIsNull(materiaId)
                : esquemas.findByMateriaIdAndCiclo(materiaId, ciclo);
    }

    private Long materiaDe(Proyecto p) {
        return p.getGrupo().getAsignacion().getMateria().getId();
    }

    private String cicloDe(Proyecto p) {
        return p.getGrupo().getAsignacion().getSubseccion().getSeccion().getCiclo();
    }

    // ---------- bloqueo cuando ya hay evaluaciones ----------
    /**
     * ¿Hay evaluaciones que dependen del esquema de ese ciclo? Para el esquema general (ciclo null) se
     * cuentan los ciclos con evaluaciones que no tienen esquema propio.
     */
    private boolean afectaEvaluaciones(Long materiaId, String ciclo) {
        Set<String> usados = new HashSet<>(esquemas.ciclosConEvaluacionDocente(materiaId));
        usados.addAll(esquemas.ciclosConEvaluacionAlumno(materiaId));
        usados.addAll(esquemas.ciclosConEvaluacionPublica(materiaId));
        if (ciclo != null) return usados.contains(ciclo);
        Set<String> conPropio = esquemas.findAllByMateriaId(materiaId).stream()
                .map(EsquemaEvaluacion::getCiclo).filter(Objects::nonNull).collect(Collectors.toSet());
        return usados.stream().anyMatch(c -> !conPropio.contains(c));
    }

    private boolean sinCambios(EsquemaEvaluacion e, EsquemaDto.Guardar r,
                               List<EsquemaDto.Item> auto, List<EsquemaDto.Item> pub) {
        return e.getPesoDocenteJurado().compareTo(r.pesoDocenteJurado()) == 0
                && e.getPesoAutoevaluacion().compareTo(r.pesoAutoevaluacion()) == 0
                && e.getPesoPublico().compareTo(r.pesoPublico()) == 0
                && iguales(criteriosRepo.findByEsquemaIdAndModalidadAndActivoTrue(e.getId(), Modalidad.AUTOEVALUACION), auto)
                && iguales(criteriosRepo.findByEsquemaIdAndModalidadAndActivoTrue(e.getId(), Modalidad.PUBLICO), pub);
    }

    // ---------- auxiliares ----------
    private String ciclo(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String c = texto.trim();
        if (!CICLO.matcher(c).matches())
            throw Http.invalido("El ciclo debe tener el formato NN-AAAA (por ejemplo 02-2026)");
        return c;
    }

    private void validarConjunto(String nombre, BigDecimal peso, List<EsquemaDto.Item> lista) {
        if (peso.signum() > 0 && lista.isEmpty())
            throw Http.invalido("Define los criterios de " + nombre + ", porque tiene un porcentaje asignado");
        if (!lista.isEmpty()) {
            BigDecimal suma = lista.stream().map(EsquemaDto.Item::ponderacion)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (suma.compareTo(CIEN) != 0)
                throw Http.invalido("Los criterios de " + nombre + " deben sumar exactamente 100% (suma enviada: "
                        + suma + "%)");
        }
    }

    private void guardarCriterios(EsquemaEvaluacion e, Modalidad mod, List<EsquemaDto.Item> lista) {
        for (EsquemaDto.Item i : lista) {
            CriterioEsquema c = new CriterioEsquema();
            c.setEsquema(e);
            c.setModalidad(mod);
            c.setNombre(i.nombre().trim());
            c.setDescripcion(i.descripcion());
            c.setPonderacion(i.ponderacion());
            criteriosRepo.save(c);
        }
    }

    /** Solo recrea los criterios si cambiaron, para no romper evaluaciones que ya los usan. */
    private void reemplazar(EsquemaEvaluacion e, Modalidad mod, List<EsquemaDto.Item> nuevos) {
        List<CriterioEsquema> actuales = criteriosRepo.findByEsquemaIdAndModalidadAndActivoTrue(e.getId(), mod);
        if (iguales(actuales, nuevos)) return;
        actuales.forEach(c -> c.setActivo(false));
        criteriosRepo.saveAll(actuales);
        guardarCriterios(e, mod, nuevos);
    }

    private boolean iguales(List<CriterioEsquema> actuales, List<EsquemaDto.Item> nuevos) {
        List<String> a = actuales.stream()
                .map(c -> clave(c.getNombre(), c.getDescripcion(), c.getPonderacion()))
                .sorted()
                .toList();
        List<String> b = nuevos.stream()
                .map(i -> clave(i.nombre(), i.descripcion(), i.ponderacion()))
                .sorted()
                .toList();
        return a.equals(b);
    }

    private String clave(String nombre, String descripcion, BigDecimal ponderacion) {
        return nombre.trim() + "|" + (descripcion == null ? "" : descripcion.trim()) + "|"
                + ponderacion.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private Materia materia(Long id) {
        return materias.findById(id).orElseThrow(() -> Http.noEncontrado("Materia no encontrada"));
    }

    private boolean puedeGestionar(Usuario u, Materia m) {
        String rol = acceso.rol(u);
        if ("admin".equals(rol)) return true;
        if (!"docente".equals(rol)) return false;
        boolean principal = m.getDocentePrincipal() != null
                && m.getDocentePrincipal().getId().equals(u.getId());
        return principal || asignaciones.existsByMateriaIdAndDocenteId(m.getId(), u.getId());
    }

    private void exigirGestion(Usuario u, Materia m) {
        if (!puedeGestionar(u, m))
            throw Http.prohibido("Solo el administrador o un docente que imparta la materia puede configurar el esquema");
    }

    @Transactional(readOnly = true)
    public List<EsquemaDto.MateriaOpcion> materiasGestionables(String correo) {
        Usuario u = acceso.actor(correo);
        return materias.findAll().stream()
                .filter(m -> puedeGestionar(u, m))
                .sorted(Comparator.comparing(Materia::getNombre))
                .map(m -> new EsquemaDto.MateriaOpcion(m.getId(), m.getNombre()))
                .toList();
    }

    private EsquemaDto.Response aDto(EsquemaEvaluacion e, String ciclo, boolean heredado, boolean enUso) {
        return new EsquemaDto.Response(e.getMateria().getId(), e.getPesoDocenteJurado(),
                e.getPesoAutoevaluacion(), e.getPesoPublico(),
                lista(e.getId(), Modalidad.AUTOEVALUACION), lista(e.getId(), Modalidad.PUBLICO),
                ciclo, heredado, enUso);
    }

    private List<EsquemaDto.Criterio> lista(Long esquemaId, Modalidad mod) {
        return criteriosRepo.findByEsquemaIdAndModalidadAndActivoTrue(esquemaId, mod).stream()
                .map(c -> new EsquemaDto.Criterio(c.getId(), c.getNombre(), c.getDescripcion(), c.getPonderacion()))
                .toList();
    }
}
