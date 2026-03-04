package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.dto.request.CreateProjectRequest;
import com.pullwise.api.application.dto.request.UpdateProjectRequest;
import com.pullwise.api.application.dto.response.ProjectDTO;
import com.pullwise.api.application.service.auth.AuthorizationService;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.repository.ProjectRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Controller REST para gerenciamento de Projetos.
 */
@Slf4j
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final AuthorizationService authorizationService;

    /**
     * Lista projetos das organizações do usuário autenticado (paginado).
     */
    @GetMapping
    public ResponseEntity<Page<ProjectDTO>> listProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {
        Long userId = authorizationService.getUserId(principal);
        List<Long> orgIds = authorizationService.getUserOrganizationIds(userId);

        if (orgIds.isEmpty()) {
            return ResponseEntity.ok(Page.empty());
        }

        Page<ProjectDTO> projects = projectRepository
                .findActiveByOrganizationIds(orgIds, PageRequest.of(page, size, Sort.by("name")))
                .map(ProjectDTO::from);
        return ResponseEntity.ok(projects);
    }

    /**
     * Busca um projeto por ID (verifica acesso via organização).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProject(@PathVariable Long id, Principal principal) {
        Long userId = authorizationService.getUserId(principal);
        authorizationService.requireProjectAccess(userId, id);

        return projectRepository.findById(id)
                .map(ProjectDTO::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cria um novo projeto.
     */
    @PostMapping
    public ResponseEntity<ProjectDTO> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            Principal principal) {
        Long userId = authorizationService.getUserId(principal);

        Project project = Project.builder()
                .name(request.name())
                .description(request.description())
                .platform(request.platform())
                .repositoryUrl(request.repositoryUrl())
                .repositoryId(request.repositoryId())
                .githubInstallationId(request.githubInstallationId())
                .autoReviewEnabled(request.autoReviewEnabled())
                .isActive(true)
                .build();

        Project saved = projectRepository.save(project);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProjectDTO.from(saved));
    }

    /**
     * Atualiza um projeto (verifica acesso).
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            Principal principal) {
        Long userId = authorizationService.getUserId(principal);
        authorizationService.requireProjectAccess(userId, id);

        return projectRepository.findById(id)
                .map(project -> {
                    if (request.name() != null) {
                        project.setName(request.name());
                    }
                    if (request.description() != null) {
                        project.setDescription(request.description());
                    }
                    if (request.isActive() != null) {
                        project.setIsActive(request.isActive());
                    }
                    if (request.autoReviewEnabled() != null) {
                        project.setAutoReviewEnabled(request.autoReviewEnabled());
                    }
                    Project saved = projectRepository.save(project);
                    return ResponseEntity.ok(ProjectDTO.from(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Remove um projeto (verifica acesso).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, Principal principal) {
        Long userId = authorizationService.getUserId(principal);
        authorizationService.requireProjectAccess(userId, id);

        if (!projectRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        projectRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
