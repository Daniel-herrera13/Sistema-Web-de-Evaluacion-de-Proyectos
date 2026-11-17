package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.SeccionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Errores;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Seccion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.SeccionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.SubseccionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.CicloAcademicoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SeccionService {

    private final SeccionRepository secciones;
    private final SubseccionRepository subsecciones;
    private final CicloAcademicoRepository ciclos;

    public SeccionService(SeccionRepository secciones, SubseccionRepository subsecciones, CicloAcademicoRepository ciclos) {
        this.secciones = secciones;
        this.subsecciones = subsecciones;
        this.ciclos = ciclos;
    }

    @Transactional(readOnly = true)
    public List<SeccionDto.Respuesta> listar(Long materiaId) {
        List<Seccion> lista = materiaId == null
                ? secciones.findAll(Sort.by("nombre"))
                : secciones.findByMateriaAsignada(materiaId);
        return lista.stream().map(this::aDto).toList();
    }

    @Transactional(readOnly = true)
    public SeccionDto.Respuesta obtener(Long id) {
        return aDto(buscar(id));
    }

    @Transactional
    public SeccionDto.Respuesta crear(SeccionDto.Guardar r) {
        String nombre = r.nombre().trim();
        String ciclo = r.ciclo().trim();
        validarCicloActivo(ciclo);
        if (secciones.existsByNombreIgnoreCaseAndCicloIgnoreCase(nombre, ciclo)) {
            throw Errores.conflicto("Ya existe la sección \"" + nombre + "\" en el ciclo " + ciclo);
        }
        Seccion s = new Seccion();
        s.setNombre(nombre);
        s.setCiclo(ciclo);
        return aDto(secciones.save(s));
    }

    @Transactional
    public SeccionDto.Respuesta actualizar(Long id, SeccionDto.Guardar r) {
        Seccion s = buscar(id);
        String nombre = r.nombre().trim();
        String ciclo = r.ciclo().trim();
        validarCicloActivo(ciclo);
        if (secciones.existsByNombreIgnoreCaseAndCicloIgnoreCaseAndIdNot(nombre, ciclo, id)) {
            throw Errores.conflicto("Ya existe otra sección \"" + nombre + "\" en el ciclo " + ciclo);
        }
        s.setNombre(nombre);
        s.setCiclo(ciclo);
        return aDto(secciones.save(s));
    }

    @Transactional
    public void eliminar(Long id) {
        buscar(id);
        if (!subsecciones.findBySeccionId(id).isEmpty()) {
            throw Errores.conflicto("La sección tiene subsecciones; elimínalas primero");
        }
        secciones.deleteById(id);
    }

    private SeccionDto.Respuesta aDto(Seccion s) {
        return new SeccionDto.Respuesta(s.getId(), s.getNombre(), s.getCiclo(),
                secciones.nombresDeMaterias(s.getId()));
    }

    private Seccion buscar(Long id) {
        return secciones.findById(id)
                .orElseThrow(() -> Errores.noEncontrado("Sección no encontrada"));
    }

    private void validarCicloActivo(String codigo) {
        if (ciclos.findByCodigoIgnoreCase(codigo).filter(c -> c.isActivo()).isEmpty()) {
            throw Errores.invalido("Selecciona un ciclo académico activo");
        }
    }
}
