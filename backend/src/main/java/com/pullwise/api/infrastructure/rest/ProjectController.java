package com.pullwise.api.infrastructure.rest;

import com.pullwise.api.application.dto.request.CreateProjectRequest;
import com.pullwise.api.application.dto.request.UpdateProjectRequest;
import com.pullwise.api.application.dto.response.ProjectDTO;
import com.pullwise.api.application.service.auth.AuthenticationService;
import com.pullwise.api.domain.model.Project;
import com.pullwise.api.domain.model.User;
import com.pullwise.api.domain.repository.ProjectRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
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
    private final AuthenticationService authenticationService;

    /**
     * Lista todos os projetos do usuário.
     */
    @GetMapping
    public ResponseEntity<List<ProjectDTO>> listProjects(Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        // Filtrar projetos que o usuário tem acesso
        // Por simplicidade, retornando todos
        List<ProjectDTO> projects = projectRepository.findAll().stream()
                .map(ProjectDTO::from)
                .toList();
        return ResponseEntity.ok(projects);
    }

    /**
     * Busca um projeto por ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProject(@PathVariable Long id, Principal principal) {
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
        Long userId = getUserIdFromPrincipal(principal);

        // Buscar organização do usuário (simplificado)
        // Em produção, usuário selecionaria a organização

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
     * Atualiza um projeto.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            Principal principal) {

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
     * Remove um projeto.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, Principal principal) {
        if (!projectRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        projectRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Long getUserIdFromPrincipal(Principal principal) {
        // Extrair user ID do JWT ou OAuth2 principal
        if (principal instanceof OAuth2AuthenticatedPrincipal oauth) {
            Object userId = oauth.getAttribute("user_id");
            if (userId instanceof Long l) {
                return l;
            }
        }
        // Fallback: pegar do subject
        return 1L; // TODO: Implementar extração correta
    }
}
