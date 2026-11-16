package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.EsquemaDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.CriterioEsquema;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.CriterioEsquema.Modalidad;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.EsquemaEvaluacion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Materia;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.AsignacionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.CriterioEsquemaRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.EsquemaEvaluacionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.MateriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;


@Service
@RequiredArgsConstructor
public class EsquemaService {
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final EsquemaEvaluacionRepository esquemas;
    private final CriterioEsquemaRepository criteriosRepo;
    private final MateriaRepository materias;
    private final AccesoService acceso;
    private final AsignacionRepository asignaciones;

    @Transactional(readOnly = true)
    public EsquemaDto.Response obtener(Long materiaId, String correo) {
        Materia m = materia(materiaId);
        exigirGestion(acceso.actor(correo), m);
        return esquemas.findByMateriaId(materiaId).map(this::aDto)
                .orElse(new EsquemaDto.Response(materiaId, CIEN, BigDecimal.ZERO, BigDecimal.ZERO,
                        List.of(), List.of()));
    }

    @Transactional
    public EsquemaDto.Response guardar(Long materiaId, EsquemaDto.Guardar r, String correo) {
        Materia m = materia(materiaId);
        exigirGestion(acceso.actor(correo), m);

        BigDecimal total = r.pesoDocenteJurado().add(r.pesoAutoevaluacion()).add(r.pesoPublico());
        if (total.compareTo(CIEN) != 0)
            throw Http.invalido("Los porcentajes de las modalidades deben sumar exactamente 100% (suma enviada: "
                    + total + "%)");

        List<EsquemaDto.Item> auto = r.criteriosAutoevaluacion() == null ? List.of() : r.criteriosAutoevaluacion();
        List<EsquemaDto.Item> pub = r.criteriosPublico() == null ? List.of() : r.criteriosPublico();
        validarConjunto("autoevaluación", r.pesoAutoevaluacion(), auto);
        validarConjunto("público", r.pesoPublico(), pub);

        EsquemaEvaluacion e = esquemas.findByMateriaId(materiaId).orElseGet(() -> {
            EsquemaEvaluacion nuevo = new EsquemaEvaluacion();
            nuevo.setMateria(m);
            return nuevo;
        });
        e.setPesoDocenteJurado(r.pesoDocenteJurado());
        e.setPesoAutoevaluacion(r.pesoAutoevaluacion());
        e.setPesoPublico(r.pesoPublico());
        e = esquemas.save(e);

        reemplazar(e, Modalidad.AUTOEVALUACION, auto);
        reemplazar(e, Modalidad.PUBLICO, pub);
        return aDto(e);
    }

    // ---------- usados por otros servicios ----------
    @Transactional(readOnly = true)
    public EsquemaEvaluacion deMateria(Long materiaId) {
        return esquemas.findByMateriaId(materiaId).orElseGet(EsquemaEvaluacion::new);  // por defecto: 100% docente/jurado
    }

    @Transactional(readOnly = true)
    public List<CriterioEsquema> criterios(Long materiaId, Modalidad modalidad) {
        return esquemas.findByMateriaId(materiaId)
                .map(e -> criteriosRepo.findByEsquemaIdAndModalidad(e.getId(), modalidad))
                .orElse(List.of());
    }

    // ---------- auxiliares ----------
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
        List<CriterioEsquema> actuales = criteriosRepo.findByEsquemaIdAndModalidad(e.getId(), mod);
        if (iguales(actuales, nuevos)) return;
        criteriosRepo.deleteAll(actuales);
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

    private EsquemaDto.Response aDto(EsquemaEvaluacion e) {
        return new EsquemaDto.Response(e.getMateria().getId(), e.getPesoDocenteJurado(),
                e.getPesoAutoevaluacion(), e.getPesoPublico(),
                lista(e.getId(), Modalidad.AUTOEVALUACION), lista(e.getId(), Modalidad.PUBLICO));
    }

    private List<EsquemaDto.Criterio> lista(Long esquemaId, Modalidad mod) {
        return criteriosRepo.findByEsquemaIdAndModalidad(esquemaId, mod).stream()
                .map(c -> new EsquemaDto.Criterio(c.getId(), c.getNombre(), c.getDescripcion(), c.getPonderacion()))
                .toList();
    }
}