package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.SubseccionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Seccion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Subseccion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.SeccionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.SubseccionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SubseccionService {

    private final SubseccionRepository repo;
    private final SeccionRepository seccionRepo;

    public SubseccionService(SubseccionRepository repo, SeccionRepository seccionRepo) {
        this.repo = repo;
        this.seccionRepo = seccionRepo;
    }

    @Transactional(readOnly = true)
    public List<SubseccionDto.Respuesta> listar(Long seccionId) {
        List<Subseccion> lista = seccionId == null
                ? repo.findAll(Sort.by("letra"))
                : repo.findBySeccionIdOrderByLetraAsc(seccionId);
        return lista.stream().map(this::aRespuesta).toList();
    }

    @Transactional
    public SubseccionDto.Respuesta crear(SubseccionDto.Guardar r) {
        Seccion seccion = seccionRepo.findById(r.seccionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La sección no existe"));

        String letra = r.letra().trim().toUpperCase();
        if (repo.existsBySeccionIdAndLetraIgnoreCase(seccion.getId(), letra)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La subsección " + seccion.getNombre() + letra + " ya existe");
        }

        Subseccion s = new Subseccion();
        s.setSeccion(seccion);
        s.setLetra(letra);
        return aRespuesta(repo.save(s));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La subsección no existe");
        }
        try {
            repo.deleteById(id);
            repo.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar: la subsección tiene alumnos matriculados o grupos asociados");
        }
    }

    private SubseccionDto.Respuesta aRespuesta(Subseccion s) {
        return new SubseccionDto.Respuesta(
                s.getId(),
                s.getSeccion().getId(),
                s.getLetra(),
                s.getSeccion().getNombre() + s.getLetra());
    }
}