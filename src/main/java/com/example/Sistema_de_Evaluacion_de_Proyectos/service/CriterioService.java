package com.example.Sistema_de_Evaluacion_de_Proyectos.service;

import com.example.Sistema_de_Evaluacion_de_Proyectos.dto.CriterioDto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.exception.Http;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Asignacion;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Criterio;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Grupo;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Materia;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Proyecto;
import com.example.Sistema_de_Evaluacion_de_Proyectos.model.Usuario;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.AsignacionRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.CriterioRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.GrupoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.MateriaRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.ProyectoRepository;
import com.example.Sistema_de_Evaluacion_de_Proyectos.repository.CicloAcademicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Un CONJUNTO de criterios = todos los criterios con el mismo nivel (materia, asignación, grupo o proyecto)
 * y el mismo ciclo. ciclo null = conjunto general (vale para cualquier ciclo que no tenga uno propio).
 * Un conjunto que ya se usó en evaluaciones no se modifica: se duplica para otro ciclo.
 */
@Service
@RequiredArgsConstructor
public class CriterioService {
    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final Pattern CICLO = Pattern.compile("^\\d{2}-\\d{4}$");

    private enum Nivel { MATERIA, ASIGNACION, GRUPO, PROYECTO }
    private record Ambito(Nivel nivel, Long id) {}

    private final CriterioRepository criterios;
    private final MateriaRepository materias;
    private final AsignacionRepository asignaciones;
    private final GrupoRepository grupos;
    private final ProyectoRepository proyectos;
    private final AccesoService acceso;
    private final CicloAcademicoRepository ciclos;

    /** Para desactivar el bloqueo durante el desarrollo: app.criterios.bloquear-en-uso=false */
    @Value("${app.criterios.bloquear-en-uso:true}")
    private boolean bloquearEnUso;

    // ---------- consulta ----------
    /** Sin ciclo devuelve el conjunto general; con ciclo, el de ese ciclo. Sin ningún nivel, todos los criterios. */
    @Transactional(readOnly = true)
    public List<CriterioDto.Response> listar(Long m, Long a, Long g, Long p, String ciclo) {
        if (m == null && a == null && g == null && p == null)
            return criterios.findAllByActivoTrue().stream().map(this::aDto).toList();
        return delConjunto(ambito(m, a, g, p), ciclo(ciclo)).stream().map(this::aDto).toList();
    }

    /** Conjuntos que existen en un nivel (uno por ciclo), con su suma y si ya se usaron. */
    @Transactional(readOnly = true)
    public List<CriterioDto.ConjuntoResumen> conjuntos(Long m, Long a, Long g, Long p) {
        Ambito ambito = ambito(m, a, g, p);
        Map<String, List<Criterio>> porCiclo = new TreeMap<>();   // "" = general (queda primero)
        for (Criterio c : delAmbito(ambito))
            porCiclo.computeIfAbsent(c.getCiclo() == null ? "" : c.getCiclo(), k -> new ArrayList<>()).add(c);

        List<CriterioDto.ConjuntoResumen> salida = new ArrayList<>();
        porCiclo.forEach((ciclo, lista) -> {
            BigDecimal suma = suma(lista);
            List<Long> ids = lista.stream().map(Criterio::getId).toList();
            salida.add(new CriterioDto.ConjuntoResumen(ciclo.isEmpty() ? null : ciclo, lista.size(), suma,
                    suma.compareTo(CIEN) == 0, criterios.contarUsos(ids) > 0));
        });
        return salida;
    }

    // ---------- CRUD ----------
    @Transactional
    public CriterioDto.Response crear(CriterioDto.Request r, String correo) {
        Usuario u = acceso.actor(correo);
        Ambito a = ambito(r.materiaId(), r.asignacionId(), r.grupoId(), r.proyectoId());
        exigirGestion(u, a);
        String ciclo = ciclo(r.ciclo());
        validarCicloActivo(ciclo);

        List<Criterio> conjunto = delConjunto(a, ciclo);
        exigirEditable(conjunto, u);
        BigDecimal actual = suma(conjunto);
        BigDecimal disponible = CIEN.subtract(actual);
        if (r.ponderacion().compareTo(disponible) > 0)
            throw Http.conflicto("Solo quedan " + disponible + "% disponibles en este conjunto (ya suman "
                    + actual + "%)");

        Criterio c = new Criterio();
        c.setNombre(r.nombre().trim());
        c.setDescripcion(r.descripcion());
        c.setPonderacion(r.ponderacion());
        c.setCiclo(ciclo);
        asignar(c, a);
        return aDto(criterios.save(c));
    }

    @Transactional
    public CriterioDto.Response actualizar(Long id, CriterioDto.Request r, String correo) {
        Usuario u = acceso.actor(correo);
        Criterio c = buscar(id);
        Ambito a = ambitoDe(c);   // el nivel y el ciclo no se cambian; se ignoran los del body
        exigirGestion(u, a);

        List<Criterio> conjunto = delConjunto(a, c.getCiclo());
        exigirEditable(conjunto, u);
        BigDecimal otros = suma(conjunto.stream().filter(x -> !x.getId().equals(id)).toList());
        BigDecimal maximo = CIEN.subtract(otros);
        if (r.ponderacion().compareTo(maximo) > 0)
            throw Http.conflicto("Este criterio puede valer como máximo " + maximo
                    + "% porque los demás del conjunto suman " + otros + "%. Baja primero otro criterio.");

        boolean usado = enUso(conjunto);
        if (usado) {
            c.setActivo(false);
            criterios.save(c);
            Criterio nuevo = new Criterio();
            nuevo.setNombre(r.nombre().trim());
            nuevo.setDescripcion(r.descripcion());
            nuevo.setPonderacion(r.ponderacion());
            nuevo.setCiclo(c.getCiclo());
            asignar(nuevo, a);
            return aDto(criterios.save(nuevo));
        }
        c.setNombre(r.nombre().trim());
        c.setDescripcion(r.descripcion());
        c.setPonderacion(r.ponderacion());
        return aDto(criterios.save(c));
    }

    @Transactional
    public void eliminar(Long id, String correo) {
        Usuario u = acceso.actor(correo);
        Criterio c = buscar(id);
        Ambito a = ambitoDe(c);
        exigirGestion(u, a);
        List<Criterio> conjunto = delConjunto(a, c.getCiclo());
        exigirEditable(conjunto, u);
        if (enUso(List.of(c))) {
            c.setActivo(false);
            criterios.save(c);
        } else {
            criterios.delete(c);
        }
    }

    // ---------- conjunto completo (exige 100%) ----------
    @Transactional
    public List<CriterioDto.Response> reemplazarConjunto(CriterioDto.Conjunto r, String correo) {
        Usuario u = acceso.actor(correo);
        Ambito a = ambito(r.materiaId(), r.asignacionId(), r.grupoId(), r.proyectoId());
        exigirGestion(u, a);
        String ciclo = ciclo(r.ciclo());
        validarCicloActivo(ciclo);

        BigDecimal total = r.criterios().stream().map(CriterioDto.Item::ponderacion)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(CIEN) != 0)
            throw Http.invalido("Las ponderaciones deben sumar exactamente 100% (suma enviada: " + total + "%)");

        List<Criterio> existentes = delConjunto(a, ciclo);
        exigirEditable(existentes, u);
        if (enUso(existentes)) {
            existentes.forEach(c -> c.setActivo(false));
            criterios.saveAll(existentes);
        } else {
            criterios.deleteAll(existentes);
        }
        return criterios.saveAll(copiar(r.criterios(), a, ciclo)).stream().map(this::aDto).toList();
    }

    /** Copia el conjunto de un ciclo a otro para que se pueda cambiar sin tocar el que ya se usó. */
    @Transactional
    public List<CriterioDto.Response> duplicar(CriterioDto.Duplicar r, String correo) {
        Usuario u = acceso.actor(correo);
        Ambito a = ambito(r.materiaId(), r.asignacionId(), r.grupoId(), r.proyectoId());
        exigirGestion(u, a);
        String origen = ciclo(r.cicloOrigen());
        String destino = ciclo(r.cicloDestino());
        validarCicloActivo(destino);
        if (destino == null) throw Http.invalido("Indica el ciclo de destino (formato NN-AAAA)");
        if (Objects.equals(origen, destino))
            throw Http.invalido("El ciclo de destino debe ser distinto al de origen");

        List<Criterio> fuente = delConjunto(a, origen);
        if (fuente.isEmpty()) throw Http.noEncontrado("No hay criterios en el conjunto de origen");
        if (!delConjunto(a, destino).isEmpty())
            throw Http.conflicto("El ciclo " + destino + " ya tiene criterios en este nivel. "
                    + "Modifícalos o elimínalos primero.");

        List<CriterioDto.Item> items = fuente.stream()
                .map(c -> new CriterioDto.Item(c.getNombre(), c.getDescripcion(), c.getPonderacion()))
                .toList();
        return criterios.saveAll(copiar(items, a, destino)).stream().map(this::aDto).toList();
    }

    // ---------- herencia: proyecto > grupo > asignación > materia ----------
    @Transactional(readOnly = true)
    public CriterioDto.Efectivos efectivos(Long proyectoId, Long grupoId, Long asignacionId, Long materiaId) {
        return efectivos(proyectoId, grupoId, asignacionId, materiaId, null);
    }

    /**
     * En cada nivel se prefiere el conjunto del ciclo del proyecto (el de su sección); si no existe o no
     * suma 100%, se usa el conjunto general de ese nivel. Solo con materiaId hace falta indicar el ciclo.
     */
    @Transactional(readOnly = true)
    public CriterioDto.Efectivos efectivos(Long proyectoId, Long grupoId, Long asignacionId, Long materiaId,
                                           String cicloParam) {
        Long p = null, g = null, a = null, m = null;
        String ciclo = ciclo(cicloParam);
        if (proyectoId != null) {
            Proyecto x = proyectos.findById(proyectoId)
                    .orElseThrow(() -> Http.noEncontrado("Proyecto no encontrado"));
            p = x.getId();
            g = x.getGrupo().getId();
            a = x.getGrupo().getAsignacion().getId();
            m = x.getGrupo().getAsignacion().getMateria().getId();
            ciclo = cicloDe(x.getGrupo().getAsignacion());
        } else if (grupoId != null) {
            Grupo x = grupos.findById(grupoId)
                    .orElseThrow(() -> Http.noEncontrado("Grupo no encontrado"));
            g = x.getId();
            a = x.getAsignacion().getId();
            m = x.getAsignacion().getMateria().getId();
            ciclo = cicloDe(x.getAsignacion());
        } else if (asignacionId != null) {
            Asignacion x = asignaciones.findById(asignacionId)
                    .orElseThrow(() -> Http.noEncontrado("Asignación no encontrada"));
            a = x.getId();
            m = x.getMateria().getId();
            ciclo = cicloDe(x);
        } else if (materiaId != null) {
            if (!materias.existsById(materiaId)) throw Http.noEncontrado("Materia no encontrada");
            m = materiaId;
        } else {
            throw Http.invalido("Indique proyectoId, grupoId, asignacionId o materiaId");
        }

        Optional<CriterioDto.Efectivos> r;
        if (p != null && (r = elegir("PROYECTO", criterios.findByProyectoId(p), ciclo)).isPresent()) return r.get();
        if (g != null && (r = elegir("GRUPO", criterios.findByGrupoId(g), ciclo)).isPresent()) return r.get();
        if (a != null && (r = elegir("ASIGNACION", criterios.findByAsignacionId(a), ciclo)).isPresent()) return r.get();
        if (m != null && (r = elegir("MATERIA", criterios.findByMateriaId(m), ciclo)).isPresent()) return r.get();
        return new CriterioDto.Efectivos("NINGUNO", BigDecimal.ZERO, List.of(), ciclo);
    }

    private Optional<CriterioDto.Efectivos> elegir(String origen, List<Criterio> todos, String ciclo) {
        if (ciclo != null) {
            Optional<CriterioDto.Efectivos> propio = completo(origen, ciclo,
                    todos.stream().filter(c -> ciclo.equals(c.getCiclo())).toList());
            if (propio.isPresent()) return propio;
        }
        return completo(origen, null, todos.stream().filter(c -> c.getCiclo() == null).toList());
    }

    private Optional<CriterioDto.Efectivos> completo(String origen, String ciclo, List<Criterio> lista) {
        BigDecimal total = suma(lista);
        if (lista.isEmpty() || total.compareTo(CIEN) != 0) return Optional.empty();
        return Optional.of(new CriterioDto.Efectivos(origen, total, lista.stream().map(this::aDto).toList(), ciclo));
    }

    // ---------- permisos ----------
    private void exigirGestion(Usuario u, Ambito a) {
        if (!puedeGestionar(u, a))
            throw Http.prohibido("Solo el administrador o el docente de la materia puede modificar estos criterios");
    }

    private boolean puedeGestionar(Usuario u, Ambito a) {
        String rol = acceso.rol(u);
        if ("admin".equals(rol)) return true;
        if (!"docente".equals(rol)) return false;
        return switch (a.nivel()) {
            case MATERIA -> docenteDeMateria(u, materias.findById(a.id())
                    .orElseThrow(() -> Http.noEncontrado("Materia no encontrada")));
            case ASIGNACION -> docenteDeAsignacion(u, asignaciones.findById(a.id())
                    .orElseThrow(() -> Http.noEncontrado("Asignación no encontrada")));
            case GRUPO -> docenteDeAsignacion(u, grupos.findById(a.id())
                    .orElseThrow(() -> Http.noEncontrado("Grupo no encontrado")).getAsignacion());
            case PROYECTO -> acceso.puedeGestionar(u, proyectos.findById(a.id())
                    .orElseThrow(() -> Http.noEncontrado("Proyecto no encontrado")));
        };
    }

    private boolean docenteDeMateria(Usuario u, Materia m) {
        boolean principal = m.getDocentePrincipal() != null && m.getDocentePrincipal().getId().equals(u.getId());
        return principal || asignaciones.existsByMateriaIdAndDocenteId(m.getId(), u.getId());
    }

    private boolean docenteDeAsignacion(Usuario u, Asignacion x) {
        boolean titular = x.getDocente() != null && x.getDocente().getId().equals(u.getId());
        Materia m = x.getMateria();
        boolean principal = m.getDocentePrincipal() != null && m.getDocentePrincipal().getId().equals(u.getId());
        return titular || principal;
    }

    // ---------- auxiliares ----------
    /** Un conjunto que ya se usó en evaluaciones no se puede modificar ni borrar. */
    private void exigirEditable(List<Criterio> conjunto, Usuario actor) {
        if (!bloquearEnUso || conjunto.isEmpty()) return;
        if ("admin".equals(acceso.rol(actor))) return;
        if (enUso(conjunto))
            throw Http.conflicto("Este conjunto de criterios ya se usó en evaluaciones y no se puede modificar. "
                    + "Usa «Duplicar» para crear uno nuevo para otro ciclo.");
    }

    private boolean enUso(List<Criterio> conjunto) {
        if (conjunto.isEmpty()) return false;
        List<Long> ids = conjunto.stream().map(Criterio::getId).toList();
        return criterios.contarUsos(ids) > 0;
    }

    private String ciclo(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String c = texto.trim();
        if (!CICLO.matcher(c).matches())
            throw Http.invalido("El ciclo debe tener el formato NN-AAAA (por ejemplo 02-2026)");
        return c;
    }

    private void validarCicloActivo(String codigo) {
        if (codigo != null && ciclos.findByCodigoIgnoreCase(codigo).filter(c -> c.isActivo()).isEmpty())
            throw Http.invalido("Selecciona un ciclo académico activo");
    }

    private String cicloDe(Asignacion a) {
        return a.getSubseccion().getSeccion().getCiclo();
    }

    private List<Criterio> copiar(List<CriterioDto.Item> items, Ambito a, String ciclo) {
        List<Criterio> nuevos = new ArrayList<>();
        for (CriterioDto.Item i : items) {
            Criterio c = new Criterio();
            c.setNombre(i.nombre().trim());
            c.setDescripcion(i.descripcion());
            c.setPonderacion(i.ponderacion());
            c.setCiclo(ciclo);
            asignar(c, a);
            nuevos.add(c);
        }
        return nuevos;
    }

    private Ambito ambito(Long m, Long a, Long g, Long p) {
        long n = Stream.of(m, a, g, p).filter(Objects::nonNull).count();
        if (n != 1) throw Http.invalido("Indique exactamente uno: materiaId, asignacionId, grupoId o proyectoId");
        if (m != null) {
            if (!materias.existsById(m)) throw Http.noEncontrado("Materia no encontrada");
            return new Ambito(Nivel.MATERIA, m);
        }
        if (a != null) {
            if (!asignaciones.existsById(a)) throw Http.noEncontrado("Asignación no encontrada");
            return new Ambito(Nivel.ASIGNACION, a);
        }
        if (g != null) {
            if (!grupos.existsById(g)) throw Http.noEncontrado("Grupo no encontrado");
            return new Ambito(Nivel.GRUPO, g);
        }
        if (!proyectos.existsById(p)) throw Http.noEncontrado("Proyecto no encontrado");
        return new Ambito(Nivel.PROYECTO, p);
    }

    private Ambito ambitoDe(Criterio c) {
        if (c.getProyecto() != null)   return new Ambito(Nivel.PROYECTO, c.getProyecto().getId());
        if (c.getGrupo() != null)      return new Ambito(Nivel.GRUPO, c.getGrupo().getId());
        if (c.getAsignacion() != null) return new Ambito(Nivel.ASIGNACION, c.getAsignacion().getId());
        return new Ambito(Nivel.MATERIA, c.getMateria().getId());
    }

    private List<Criterio> delAmbito(Ambito a) {
        return switch (a.nivel()) {
            case MATERIA    -> criterios.findByMateriaIdAndActivoTrue(a.id());
            case ASIGNACION -> criterios.findByAsignacionIdAndActivoTrue(a.id());
            case GRUPO      -> criterios.findByGrupoIdAndActivoTrue(a.id());
            case PROYECTO   -> criterios.findByProyectoIdAndActivoTrue(a.id());
        };
    }

    private List<Criterio> delConjunto(Ambito a, String ciclo) {
        return delAmbito(a).stream().filter(c -> Objects.equals(c.getCiclo(), ciclo)).toList();
    }

    private void asignar(Criterio c, Ambito a) {
        switch (a.nivel()) {
            case MATERIA    -> c.setMateria(materias.getReferenceById(a.id()));
            case ASIGNACION -> c.setAsignacion(asignaciones.getReferenceById(a.id()));
            case GRUPO      -> c.setGrupo(grupos.getReferenceById(a.id()));
            case PROYECTO   -> c.setProyecto(proyectos.getReferenceById(a.id()));
        }
    }

    private BigDecimal suma(List<Criterio> lista) {
        return lista.stream().map(Criterio::getPonderacion).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Criterio buscar(Long id) {
        Criterio c = criterios.findById(id).orElseThrow(() -> Http.noEncontrado("Criterio no encontrado"));
        if (!c.isActivo()) throw Http.noEncontrado("Criterio no encontrado");
        return c;
    }

    private CriterioDto.Response aDto(Criterio c) {
        Ambito a = ambitoDe(c);
        return new CriterioDto.Response(c.getId(), c.getNombre(), c.getDescripcion(),
                c.getPonderacion(), a.nivel().name(), a.id(), c.getCiclo());
    }
}
