package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.SeccionDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Errores;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Materia;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Seccion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.MateriaRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.SeccionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SeccionService {

    private final SeccionRepository secciones;
    private final MateriaRepository materias;

    public SeccionService(SeccionRepository secciones, MateriaRepository materias) {
        this.secciones = secciones;
        this.materias = materias;
    }

    @Transactional(readOnly = true)
    public List<SeccionDto.Respuesta> listar(Long materiaId) {
        List<Seccion> lista = materiaId == null ? secciones.findAll() : secciones.findByMateriaId(materiaId);
        return lista.stream().map(SeccionDto.Respuesta::de).toList();
    }

    @Transactional(readOnly = true)
    public SeccionDto.Respuesta obtener(Long id) {
        return SeccionDto.Respuesta.de(buscar(id));
    }

    @Transactional
    public SeccionDto.Respuesta crear(SeccionDto.Guardar r) {
        String nombre = r.nombre().trim();
        String ciclo = r.ciclo().trim();
        if (secciones.existsByMateriaIdAndNombreIgnoreCaseAndCicloIgnoreCase(r.materiaId(), nombre, ciclo)) {
            throw Errores.conflicto("Ya existe la sección \"" + nombre + "\" en esa materia para el ciclo " + ciclo);
        }
        return SeccionDto.Respuesta.de(secciones.save(aplicar(new Seccion(), r, nombre, ciclo)));
    }

    @Transactional
    public SeccionDto.Respuesta actualizar(Long id, SeccionDto.Guardar r) {
        Seccion s = buscar(id);
        String nombre = r.nombre().trim();
        String ciclo = r.ciclo().trim();
        if (secciones.existsByMateriaIdAndNombreIgnoreCaseAndCicloIgnoreCaseAndIdNot(r.materiaId(), nombre, ciclo, id)) {
            throw Errores.conflicto("Ya existe otra sección \"" + nombre + "\" en esa materia para el ciclo " + ciclo);
        }
        return SeccionDto.Respuesta.de(secciones.save(aplicar(s, r, nombre, ciclo)));
    }

    @Transactional
    public void eliminar(Long id) {
        buscar(id);
        secciones.deleteById(id);
    }

    private Seccion aplicar(Seccion s, SeccionDto.Guardar r, String nombre, String ciclo) {
        Materia materia = materias.findById(r.materiaId())
                .orElseThrow(() -> Errores.invalido("La materia indicada no existe"));
        s.setNombre(nombre);
        s.setCiclo(ciclo);
        s.setMateria(materia);
        return s;
    }

    private Seccion buscar(Long id) {
        return secciones.findById(id)
                .orElseThrow(() -> Errores.noEncontrado("Sección no encontrada"));
    }
}