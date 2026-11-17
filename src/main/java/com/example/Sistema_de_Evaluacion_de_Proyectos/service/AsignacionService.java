package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.AsignacionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Errores;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Asignacion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Materia;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Subseccion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.AsignacionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.MateriaRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.SubseccionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AsignacionService {

    private final AsignacionRepository asignaciones;
    private final SubseccionRepository subsecciones;
    private final MateriaRepository materias;
    private final UsuarioRepository usuarios;

    @Transactional(readOnly = true)
    public List<AsignacionDto.Response> listar() {
        return asignaciones.findAll().stream()
                .map(a -> new AsignacionDto.Response(
                        a.getId(),
                        a.getMateria().getId(),
                        a.getMateria().getNombre(),
                        a.getSubseccion().getSeccion().getNombre(),
                        a.getSubseccion().getSeccion().getCiclo(),
                        a.getSubseccion().getLetra(),
                        a.getDocente().getId(),
                        nombreDocente(a.getDocente())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AsignacionDto.Detalle> detalle(Long seccionId) {
        return asignaciones.findBySubseccionSeccionId(seccionId).stream()
                .sorted(Comparator
                        .comparing((Asignacion a) -> a.getMateria().getNombre())
                        .thenComparing(a -> a.getSubseccion().getLetra()))
                .map(a -> new AsignacionDto.Detalle(
                        a.getId(),
                        a.getSubseccion().getId(),
                        a.getSubseccion().getSeccion().getNombre() + a.getSubseccion().getLetra(),
                        a.getMateria().getId(),
                        a.getMateria().getNombre(),
                        a.getDocente().getId(),
                        nombreDocente(a.getDocente())))
                .toList();
    }

    @Transactional
    public AsignacionDto.Resultado crear(AsignacionDto.Guardar r) {
        Materia materia = materias.findById(r.materiaId())
                .orElseThrow(() -> Errores.noEncontrado("La materia no existe"));
        Usuario docente = usuarios.findById(r.docenteId())
                .orElseThrow(() -> Errores.noEncontrado("El docente no existe"));
        if (docente.getRol() == null || !"DOCENTE".equalsIgnoreCase(docente.getRol().getNombre())) {
            throw Errores.invalido("El usuario seleccionado no es docente");
        }

        boolean unica = r.subseccionId() != null;
        List<Subseccion> destino;
        if (unica) {
            Subseccion s = subsecciones.findById(r.subseccionId())
                    .orElseThrow(() -> Errores.noEncontrado("La subsección no existe"));
            if (!s.getSeccion().getId().equals(r.seccionId())) {
                throw Errores.invalido("La subsección no pertenece a la sección indicada");
            }
            destino = List.of(s);
        } else {
            destino = subsecciones.findBySeccionIdOrderByLetraAsc(r.seccionId());
            if (destino.isEmpty()) {
                throw Errores.invalido("La sección no tiene subsecciones; crea al menos una primero");
            }
        }

        int creadas = 0, omitidas = 0;
        for (Subseccion s : destino) {
            if (asignaciones.existsBySubseccionIdAndMateriaId(s.getId(), materia.getId())) {
                omitidas++;
                continue;
            }
            Asignacion a = new Asignacion();
            a.setSubseccion(s);
            a.setMateria(materia);
            a.setDocente(docente);
            asignaciones.save(a);
            creadas++;
        }
        if (creadas == 0) {
            throw Errores.conflicto(unica
                    ? "Esa materia ya está asignada a la subsección"
                    : "Esa materia ya está asignada a todas las subsecciones");
        }
        return new AsignacionDto.Resultado(creadas, omitidas);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!asignaciones.existsById(id)) {
            throw Errores.noEncontrado("La asignación no existe");
        }
        try {
            asignaciones.deleteById(id);
            asignaciones.flush();
        } catch (DataIntegrityViolationException e) {
            throw Errores.conflicto("No se puede quitar: tiene grupos de proyecto asociados");
        }
    }

    private String nombreDocente(Usuario u) {
        return u.getNombre() + " " + u.getApellido();
    }
}
