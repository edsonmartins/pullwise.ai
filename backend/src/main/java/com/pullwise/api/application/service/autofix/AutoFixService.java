package com.pullwise.api.application.service.autofix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullwise.api.application.service.autofix.dto.*;
import com.pullwise.api.application.service.llm.router.MultiModelLLMRouter;
import com.pullwise.api.domain.enums.FixConfidence;
import com.pullwise.api.domain.enums.FixStatus;
import com.pullwise.api.domain.enums.IssueType;
import com.pullwise.api.domain.enums.ReviewTaskType;
import com.pullwise.api.domain.model.*;
import com.pullwise.api.domain.repository.FixSuggestionRepository;
import com.pullwise.api.domain.repository.IssueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serviço de Auto-Fix para correção automática de código.
 *
 * <p>Workflow:
 * <ol>
 *   <li>Analisa a issue e o código original</li>
 *   <li>Gera correção usando LLM</li>
 *   <li>Valida a correção gerada</li>
 *   <li>Aplica a correção (via GitService)</li>
 *   <li>Registra o resultado</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoFixService {

    private final MultiModelLLMRouter llmRouter;
    private final GitService gitService;
    private final FixSuggestionRepository fixSuggestionRepository;
    private final IssueRepository issueRepository;
    private final ObjectMapper objectMapper;

    @Value("${autofix.enabled:true}")
    private boolean enabled;

    @Value("${autofix.max-file-size-kb:100}")
    private int maxFileSizeKb;

    @Value("${autofix.branch-prefix:pullwise/fix/}")
    private String branchPrefix;

    private static final int MAX_RETRIES = 2;
    private static final Pattern JSON_BLOCK_PATTERN = Pattern.compile("```json\\s*([\\s\\S]*?)```");
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:\\w+)?\\s*([\\s\\S]*?)```");

    /**
     * Gera uma sugestão de correção para uma issue.
     *
     * @param request Request de geração
     * @return Resultado da geração
     */
    @Transactional
    public FixGenerationResult generateFix(FixGenerationRequest request) {
        if (!enabled) {
            log.info("Auto-fix is disabled");
            return FixGenerationResult.failed("Auto-fix is disabled");
        }

        Issue issue = request.issue();

        // Verifica se já existe sugestão para esta issue
        Optional<FixSuggestion> existing = fixSuggestionRepository
                .findByIssueIdAndStatus(issue.getId(), FixStatus.PENDING);

        if (existing.isPresent()) {
            log.info("Fix suggestion already exists for issue {}", issue.getId());
            return fromExistingSuggestion(existing.get());
        }

        // Valida tamanho do arquivo
        if (request.fileContent() != null &&
            request.fileContent().length() > maxFileSizeKb * 1024) {
            return FixGenerationResult.failed(
                    "File too large for auto-fix (max " + maxFileSizeKb + "KB)"
            );
        }

        // Valida tipo de issue
        if (!canAutoFix(issue)) {
            return FixGenerationResult.failed(
                    "Issue type " + issue.getType() + " is not auto-fixable"
            );
        }

        // Gera correção usando LLM
        FixGenerationResult result = generateFixWithLLM(request);

        // Salva sugestão no banco
        if (result.confidence() != FixConfidence.LOW) {
            saveFixSuggestion(issue, result, request);
        }

        return result;
    }

    /**
     * Aplica uma sugestão de correção.
     *
     * @param request Request de aplicação
     * @return Resultado da aplicação
     */
    @Transactional
    public FixApplicationResult applyFix(FixApplicationRequest request) {
        FixSuggestion suggestion = request.suggestion();

        if (!suggestion.isReadyToApply()) {
            return FixApplicationResult.failed(
                    "Suggestion is not ready to apply (status=" + suggestion.getStatus() +
                    ", confidence=" + suggestion.getConfidence() + ")"
            );
        }

        Review review = suggestion.getReview();
        PullRequest pr = review.getPullRequest();
        Project project = pr.getProject();

        try {
            // Clone repositório
            GitService.GitCloneResult cloneResult = gitService.cloneRepository(project);
            Path repoPath = Path.of(cloneResult.localPath());

            // Cria branch para a correção
            String branchName = generateBranchName(suggestion);
            gitService.createBranch(repoPath, branchName, pr.getSourceBranch());

            // Aplica a correção
            applyCodeChange(repoPath, suggestion);

            // Commita a mudança
            String commitMessage = generateCommitMessage(suggestion);
            String commitHash = gitService.commit(repoPath, commitMessage);

            // Push se solicitado
            if (request.push()) {
                if (request.authToken() != null) {
                    String authUrl = injectAuth(project.getRepositoryUrl(), request.authToken());
                    gitService.pushWithAuth(repoPath, branchName, authUrl);
                } else {
                    gitService.push(repoPath, branchName, "origin");
                }
            }

            // Atualiza sugestão
            suggestion.setStatus(FixStatus.APPLIED);
            suggestion.setReviewedBy(request.appliedBy());
            suggestion.setReviewedAt(LocalDateTime.now());
            suggestion.setAppliedAt(LocalDateTime.now());
            suggestion.setAppliedCommitHash(commitHash);
            suggestion.setBranchName(branchName);
            fixSuggestionRepository.save(suggestion);

            // Cleanup
            gitService.cleanup(repoPath);

            List<String> modifiedFiles = suggestion.getFilePath() != null
                    ? List.of(suggestion.getFilePath())
                    : List.of();

            return FixApplicationResult.success(commitHash, branchName, modifiedFiles);

        } catch (Exception e) {
            log.error("Failed to apply fix for suggestion {}", suggestion.getId(), e);

            suggestion.setStatus(FixStatus.FAILED);
            suggestion.setErrorMessage(e.getMessage());
            fixSuggestionRepository.save(suggestion);

            return FixApplicationResult.failed(e.getMessage());
        }
    }

    /**
     * Gera e aplica correção em uma única operação (para auto-fix de alta confiança).
     *
     * @param issue      Issue a ser corrigida
     * @param fileContent Conteúdo do arquivo
     * @param authToken  Token de autenticação
     * @return Resultado da aplicação
     */
    @Transactional
    public FixApplicationResult generateAndApply(Issue issue, String fileContent, String authToken) {
        // Gera correção
        String branchName = generateBranchName(issue);
        FixGenerationRequest genRequest = FixGenerationRequest.of(
                issue, fileContent, branchName
        );

        FixGenerationResult genResult = generateFix(genRequest);

        if (genResult.confidence() != FixConfidence.HIGH) {
            return FixApplicationResult.failed(
                    "Cannot auto-apply fix with confidence " + genResult.confidence()
            );
        }

        // Busca a sugestão criada
        FixSuggestion suggestion = fixSuggestionRepository
                .findByIssueIdAndStatus(issue.getId(), FixStatus.PENDING)
                .orElseThrow();

        // Marca como aprovada
        suggestion.setStatus(FixStatus.APPROVED);
        fixSuggestionRepository.save(suggestion);

        // Aplica
        FixApplicationRequest applyRequest = FixApplicationRequest.autoApply(suggestion, authToken);
        return applyFix(applyRequest);
    }

    /**
     * Valida uma correção gerada.
     *
     * @param originalCode Código original
     * @param fixedCode    Código corrigido
     * @param language     Linguagem de programação
     * @return Resultado da validação
     */
    public CodeValidationResult validateFix(String originalCode, String fixedCode, String language) {
        List<String> issues = new ArrayList<>();

        // Validações básicas
        if (fixedCode == null || fixedCode.isBlank()) {
            issues.add("Fixed code is empty");
            return CodeValidationResult.ofInvalid(issues);
        }

        // Verifica se o código tem tamanho razoável
        if (fixedCode.length() < originalCode.length() * 0.1) {
            issues.add("Fixed code is significantly smaller than original");
        }

        if (fixedCode.length() > originalCode.length() * 10) {
            issues.add("Fixed code is significantly larger than original");
        }

        // Verifica se há blocos incompletos
        if (hasUnclosedBlocks(fixedCode)) {
            issues.add("Code has unclosed blocks");
        }

        // Verifica por padrões perigosos
        if (containsDangerousPatterns(fixedCode)) {
            issues.add("Code contains potentially dangerous patterns (CRITICAL)");
        }

        // Validações específicas por linguagem
        issues.addAll(languageSpecificValidation(fixedCode, language));

        return issues.isEmpty()
                ? CodeValidationResult.ofValid()
                : CodeValidationResult.ofInvalid(issues);
    }

    /**
     * Aprova uma sugestão de correção.
     *
     * @param suggestionId ID da sugestão
     * @param reviewedBy   Usuário que aprovou
     * @return true se aprovada com sucesso
     */
    @Transactional
    public boolean approveSuggestion(Long suggestionId, String reviewedBy) {
        Optional<FixSuggestion> opt = fixSuggestionRepository.findById(suggestionId);

        if (opt.isEmpty()) {
            return false;
        }

        FixSuggestion suggestion = opt.get();

        if (suggestion.getStatus() != FixStatus.PENDING) {
            log.warn("Cannot approve suggestion with status {}", suggestion.getStatus());
            return false;
        }

        suggestion.setStatus(FixStatus.APPROVED);
        suggestion.setReviewedBy(reviewedBy);
        suggestion.setReviewedAt(LocalDateTime.now());

        fixSuggestionRepository.save(suggestion);

        log.info("Approved fix suggestion {} by {}", suggestionId, reviewedBy);

        return true;
    }

    /**
     * Rejeita uma sugestão de correção.
     *
     * @param suggestionId ID da sugestão
     * @param reviewedBy   Usuário que rejeitou
     * @param reason       Motivo da rejeição
     * @return true se rejeitada com sucesso
     */
    @Transactional
    public boolean rejectSuggestion(Long suggestionId, String reviewedBy, String reason) {
        Optional<FixSuggestion> opt = fixSuggestionRepository.findById(suggestionId);

        if (opt.isEmpty()) {
            return false;
        }

        FixSuggestion suggestion = opt.get();

        if (suggestion.getStatus() != FixStatus.PENDING) {
            log.warn("Cannot reject suggestion with status {}", suggestion.getStatus());
            return false;
        }

        suggestion.setStatus(FixStatus.REJECTED);
        suggestion.setReviewedBy(reviewedBy);
        suggestion.setReviewedAt(LocalDateTime.now());
        suggestion.setErrorMessage(reason);

        fixSuggestionRepository.save(suggestion);

        log.info("Rejected fix suggestion {} by {}: {}", suggestionId, reviewedBy, reason);

        return true;
    }

    /**
     * Busca sugestões prontas para aplicar.
     *
     * @param reviewId ID do review
     * @return Lista de sugestões
     */
    public List<FixSuggestion> getReadyToApply(Long reviewId) {
        return fixSuggestionRepository.findReadyToApply(reviewId);
    }

    // ========== Private Methods ==========

    /**
     * Gera correção usando LLM.
     */
    private FixGenerationResult generateFixWithLLM(FixGenerationRequest request) {
        Issue issue = request.issue();
        String fileContent = request.fileContent();

        // Constrói prompt
        String systemPrompt = buildSystemPrompt(issue);
        String userPrompt = buildUserPrompt(issue, fileContent);

        try {
            // Executa LLM
            MultiModelLLMRouter.LLMResponse response = llmRouter.execute(
                    ReviewTaskType.REFACTORING,
                    systemPrompt,
                    userPrompt
            );

            // Parse da resposta
            FixGenerationResult result = parseLLMResponse(response, request);

            log.info("Generated fix for issue {} with confidence {} using model {}",
                    issue.getId(), result.confidence(), response.modelId());

            return result;

        } catch (Exception e) {
            log.error("Failed to generate fix for issue {}", issue.getId(), e);
            return FixGenerationResult.failed(e.getMessage());
        }
    }

    /**
     * Constrói o prompt do sistema.
     */
    private String buildSystemPrompt(Issue issue) {
        return String.format("""
                You are an expert code reviewer and developer. Your task is to generate a fix for a code issue.

                Issue Type: %s
                Severity: %s

                Rules:
                1. Fix ONLY the specific issue reported - do not make unrelated changes
                2. Preserve the original code style and formatting
                3. Maintain existing functionality while fixing the problem
                4. Add comments only if necessary to explain the fix
                5. Return a complete, working solution

                Response Format (JSON):
                {
                  "fixedCode": "the complete corrected code",
                  "originalCode": "the original problematic code snippet",
                  "explanation": "brief explanation of what was fixed and why",
                  "confidence": "HIGH|MEDIUM|LOW",
                  "blockingReasons": ["reason1", "reason2"]
                }
                """,
                issue.getType(),
                issue.getSeverity()
        );
    }

    /**
     * Constrói o prompt do usuário.
     */
    private String buildUserPrompt(Issue issue, String fileContent) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("## Issue\n\n");
        prompt.append("**Title:** ").append(issue.getTitle()).append("\n\n");
        prompt.append("**Description:** ").append(issue.getDescription() != null ?
                issue.getDescription() : "No description").append("\n\n");

        if (issue.getRuleId() != null) {
            prompt.append("**Rule:** ").append(issue.getRuleId()).append("\n\n");
        }

        if (issue.getSuggestion() != null) {
            prompt.append("**Suggestion:** ").append(issue.getSuggestion()).append("\n\n");
        }

        if (issue.getLineStart() != null) {
            prompt.append("**Location:** Line ").append(issue.getLineStart());
            if (issue.getLineEnd() != null && !issue.getLineEnd().equals(issue.getLineStart())) {
                prompt.append("-").append(issue.getLineEnd());
            }
            prompt.append("\n\n");
        }

        prompt.append("## Code to Fix\n\n");
        prompt.append("```");
        if (fileContent != null && !fileContent.isBlank()) {
            prompt.append(fileContent);
        } else if (issue.getCodeSnippet() != null) {
            prompt.append(issue.getCodeSnippet());
        } else {
            prompt.append("// No code provided");
        }
        prompt.append("```\n\n");

        prompt.append("Generate the fix following the system prompt format.");

        return prompt.toString();
    }

    /**
     * Parse da resposta do LLM.
     */
    private FixGenerationResult parseLLMResponse(
            MultiModelLLMRouter.LLMResponse response,
            FixGenerationRequest request) {

        try {
            String content = response.content().trim();

            // Tenta extrair JSON de bloco markdown
            Matcher jsonMatcher = JSON_BLOCK_PATTERN.matcher(content);
            String jsonContent;
            if (jsonMatcher.find()) {
                jsonContent = jsonMatcher.group(1);
            } else {
                // Tenta parsear diretamente
                jsonContent = content;
            }

            JsonNode root = objectMapper.readTree(jsonContent);

            String fixedCode = root.has("fixedCode") ?
                    root.get("fixedCode").asText() : null;
            String originalCode = root.has("originalCode") ?
                    root.get("originalCode").asText() :
                    request.issue().getCodeSnippet();
            String explanation = root.has("explanation") ?
                    root.get("explanation").asText() :
                    "No explanation provided";

            FixConfidence confidence = parseConfidence(
                    root.has("confidence") ? root.get("confidence").asText() : "MEDIUM"
            );

            // Valida a correção
            CodeValidationResult validation = validateFix(
                    originalCode,
                    fixedCode,
                    inferLanguage(request.issue().getFilePath())
            );

            List<String> blockingReasons = new ArrayList<>();
            if (root.has("blockingReasons")) {
                root.get("blockingReasons").forEach(node ->
                        blockingReasons.add(node.asText()));
            }

            if (!validation.valid()) {
                blockingReasons.addAll(validation.issues());
                if (confidence == FixConfidence.HIGH) {
                    confidence = FixConfidence.MEDIUM;
                }
            }

            boolean canAutoApply = confidence == FixConfidence.HIGH &&
                    blockingReasons.isEmpty();

            // Estima tokens
            int inputTokens = (int) (response.content().length() / 4.0);
            int outputTokens = (int) (content.length() / 4.0);

            return new FixGenerationResult(
                    fixedCode,
                    originalCode,
                    explanation,
                    confidence,
                    canAutoApply,
                    blockingReasons,
                    response.modelId(),
                    inputTokens,
                    outputTokens,
                    response.cost().doubleValue()
            );

        } catch (Exception e) {
            log.warn("Failed to parse LLM response as JSON, trying to extract code block", e);

            // Fallback: tenta extrair código de blocos markdown
            return extractCodeFromMarkdown(response.content(), response);
        }
    }

    /**
     * Extrai código de blocos markdown (fallback).
     */
    private FixGenerationResult extractCodeFromMarkdown(String content, MultiModelLLMRouter.LLMResponse response) {
        Matcher codeMatcher = CODE_BLOCK_PATTERN.matcher(content);

        if (codeMatcher.find()) {
            String fixedCode = codeMatcher.group(1).trim();

            return FixGenerationResult.mediumConfidence(
                    fixedCode,
                    null,
                    "Code extracted from response (structured JSON not available)",
                    response.modelId(),
                    (int) (content.length() / 4.0),
                    (int) (fixedCode.length() / 4.0),
                    response.cost().doubleValue()
            );
        }

        return FixGenerationResult.failed("Could not extract fix from LLM response");
    }

    /**
     * Parse do nível de confiança.
     */
    private FixConfidence parseConfidence(String value) {
        if (value == null) return FixConfidence.MEDIUM;

        String upper = value.toUpperCase();
        for (FixConfidence fc : FixConfidence.values()) {
            if (fc.name().equals(upper)) {
                return fc;
            }
        }
        return FixConfidence.MEDIUM;
    }

    /**
     * Verifica se uma issue pode ser auto-corrigida.
     */
    private boolean canAutoFix(Issue issue) {
        // Tipos que geralmente podem ser auto-corrigidos
        return switch (issue.getType()) {
            case CODE_SMELL,
                 BUG,
                 STYLE,
                 SUGGESTION,
                 TEST -> true;
            case VULNERABILITY,
                 SECURITY,
                 PERFORMANCE,
                 LOGIC,
                 DOCUMENTATION -> issue.getSeverity().ordinal() <= 2; // Até HIGH
        };
    }

    /**
     * Salva sugestão de correção.
     */
    private void saveFixSuggestion(Issue issue, FixGenerationResult result, FixGenerationRequest request) {
        FixSuggestion suggestion = FixSuggestion.builder()
                .review(issue.getReview())
                .issue(issue)
                .status(FixStatus.PENDING)
                .confidence(result.confidence())
                .fixedCode(result.fixedCode())
                .originalCode(result.originalCode())
                .explanation(result.explanation())
                .filePath(issue.getFilePath())
                .startLine(issue.getLineStart())
                .endLine(issue.getLineEnd())
                .branchName(request.branchName())
                .modelUsed(result.modelUsed())
                .inputTokens(result.inputTokens())
                .outputTokens(result.outputTokens())
                .estimatedCost(result.cost())
                .build();

        fixSuggestionRepository.save(suggestion);

        log.info("Saved fix suggestion {} for issue {}", suggestion.getId(), issue.getId());
    }

    /**
     * Converte sugestão existente em resultado.
     */
    private FixGenerationResult fromExistingSuggestion(FixSuggestion suggestion) {
        return new FixGenerationResult(
                suggestion.getFixedCode(),
                suggestion.getOriginalCode(),
                suggestion.getExplanation(),
                suggestion.getConfidence(),
                suggestion.isReadyToApply(),
                List.of(),
                suggestion.getModelUsed(),
                suggestion.getInputTokens(),
                suggestion.getOutputTokens(),
                suggestion.getEstimatedCost()
        );
    }

    /**
     * Aplica mudança de código no repositório.
     */
    private void applyCodeChange(Path repoPath, FixSuggestion suggestion) {
        if (suggestion.getFixedCode() == null) {
            throw new IllegalArgumentException("No fixed code to apply");
        }

        if (suggestion.getFilePath() == null) {
            throw new IllegalArgumentException("No file path specified");
        }

        // Escreve arquivo com código corrigido
        gitService.writeFile(
                repoPath,
                suggestion.getFilePath(),
                suggestion.getFixedCode()
        );
    }

    /**
     * Gera nome de branch para a correção.
     */
    private String generateBranchName(FixSuggestion suggestion) {
        Issue issue = suggestion.getIssue();

        String base = branchPrefix + issue.getId();

        // Adiciona tipo da issue
        String typeSlug = issue.getType().name().toLowerCase()
                .replace("_", "-");
        base += "-" + typeSlug;

        // Trunca se muito longo
        if (base.length() > 100) {
            base = base.substring(0, 100);
        }

        return base;
    }

    /**
     * Gera nome de branch para uma issue.
     */
    private String generateBranchName(Issue issue) {
        return branchPrefix + issue.getId();
    }

    /**
     * Gera mensagem de commit.
     */
    private String generateCommitMessage(FixSuggestion suggestion) {
        Issue issue = suggestion.getIssue();

        return String.format("""
                Fix issue #%d: %s

                %s

                Auto-generated by Pullwise.ai
                Model: %s
                Confidence: %s
                """,
                issue.getId(),
                issue.getTitle(),
                suggestion.getExplanation() != null ?
                        suggestion.getExplanation() : "",
                suggestion.getModelUsed(),
                suggestion.getConfidence()
        ).trim();
    }

    /**
     * Injeta credenciais em URL de repositório.
     */
    private String injectAuth(String repoUrl, String token) {
        // https://github.com/owner/repo.git
        // -> https://token@github.com/owner/repo.git

        return repoUrl.replace("https://", "https://" + token + "@");
    }

    /**
     * Infere linguagem do caminho do arquivo.
     */
    private String inferLanguage(String filePath) {
        if (filePath == null) return "unknown";

        int lastDot = filePath.lastIndexOf('.');
        if (lastDot < 0) return "unknown";

        String ext = filePath.substring(lastDot + 1).toLowerCase();

        return switch (ext) {
            case "java" -> "java";
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "py" -> "python";
            case "go" -> "go";
            case "rs" -> "rust";
            case "cpp", "cc", "cxx" -> "cpp";
            case "c" -> "c";
            case "cs" -> "csharp";
            case "php" -> "php";
            case "rb" -> "ruby";
            default -> "unknown";
        };
    }

    /**
     * Verifica se há blocos não fechados.
     */
    private boolean hasUnclosedBlocks(String code) {
        int braces = 0;
        int parens = 0;
        int brackets = 0;

        boolean inString = false;
        boolean inChar = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            char prev = i > 0 ? code.charAt(i - 1) : '\0';

            // Verifica comentários
            if (!inString && !inChar) {
                if (!inBlockComment && !inLineComment && c == '/' && i + 1 < code.length()) {
                    char next = code.charAt(i + 1);
                    if (next == '/') {
                        inLineComment = true;
                        i++;
                        continue;
                    } else if (next == '*') {
                        inBlockComment = true;
                        i++;
                        continue;
                    }
                }

                if (inLineComment && c == '\n') {
                    inLineComment = false;
                    continue;
                }

                if (inBlockComment && c == '*' && i + 1 < code.length() && code.charAt(i + 1) == '/') {
                    inBlockComment = false;
                    i++;
                    continue;
                }
            }

            if (inLineComment || inBlockComment) continue;

            // Verifica strings e chars
            if (c == '"' && prev != '\\' && !inChar) {
                inString = !inString;
                continue;
            }

            if (c == '\'' && prev != '\\' && !inString) {
                inChar = !inChar;
                continue;
            }

            if (inString || inChar) continue;

            // Conta brackets
            switch (c) {
                case '{' -> braces++;
                case '}' -> braces--;
                case '(' -> parens++;
                case ')' -> parens--;
                case '[' -> brackets++;
                case ']' -> brackets--;
            }
        }

        return braces != 0 || parens != 0 || brackets != 0;
    }

    /**
     * Verifica padrões perigosos no código.
     */
    private boolean containsDangerousPatterns(String code) {
        String lower = code.toLowerCase();

        // Padrões de SQL injection
        if (lower.contains("executequery(\"+ \"") ||
            lower.contains("executequery(\"$" + "\"")) {
            return true;
        }

        // Padrões de command injection
        if (lower.contains("runtime.getruntime().exec(\"+")) {
            return true;
        }

        // Hardcoded secrets (básico)
        if (lower.matches(".*password\\s*=\\s*['\"].*['\"].*")) {
            return true;
        }

        return false;
    }

    /**
     * Validações específicas por linguagem.
     */
    private List<String> languageSpecificValidation(String code, String language) {
        List<String> issues = new ArrayList<>();

        switch (language) {
            case "java" -> {
                // Verifica import statement solto
                if (code.trim().startsWith("import ") && !code.contains(";")) {
                    issues.add("Unterminated import statement");
                }

                // Verifica package statement
                if (code.contains("package ") && !code.contains(";")) {
                    issues.add("Unterminated package statement");
                }
            }

            case "python" -> {
                // Verifica indentação consistente
                if (code.contains("\t") && code.contains("    ")) {
                    issues.add("Mixed tabs and spaces");
                }
            }
        }

        return issues;
    }
}
