package com.pullwise.api.domain.constants;

/**
 * Constantes centralizadas para chaves de configuracao do sistema.
 *
 * <p>Evita magic strings espalhadas pelo codebase. Todas as chaves usadas
 * com {@code ConfigurationResolver.getConfig()}, {@code saveProjectConfig()},
 * e {@code saveOrgConfig()} devem ser referenciadas por estas constantes.
 */
public final class ConfigKeys {

    private ConfigKeys() {
        // Utility class — prevent instantiation
    }

    // ===== SAST =====

    /** Habilita/desabilita analise SAST para o projeto. Tipo: BOOLEAN. Default: "true". */
    public static final String SAST_ENABLED = "sast.enabled";

    // ===== LLM =====

    /** Habilita/desabilita review via LLM para o projeto. Tipo: BOOLEAN. Default: "true". */
    public static final String LLM_ENABLED = "llm.enabled";

    /** Provider LLM configurado (ex: "openrouter", "ollama"). Tipo: STRING. Default: "openrouter". */
    public static final String LLM_PROVIDER = "llm.provider";

    /** Modelo LLM a ser usado (ex: "anthropic/claude-3-haiku"). Tipo: STRING. */
    public static final String LLM_MODEL = "llm.model";

    /** Chave de API do LLM / OpenRouter. Tipo: STRING. Sensitive. */
    public static final String LLM_API_KEY = "llm.api_key";

    // ===== RAG =====

    /** Habilita/desabilita contexto RAG para reviews. Tipo: BOOLEAN. Default: "false". */
    public static final String RAG_ENABLED = "rag.enabled";

    // ===== Review Settings =====

    /** Severidade minima para incluir issues no resultado. Tipo: STRING (enum Severity). */
    public static final String REVIEW_MIN_SEVERITY = "review.min_severity";

    /** Posta review automaticamente quando PR e criado/atualizado. Tipo: BOOLEAN. Default: "true". */
    public static final String REVIEW_AUTO_POST = "review.auto_post";

    /** Inclui secao de resumo no comentario do review. Tipo: BOOLEAN. Default: "true". */
    public static final String REVIEW_INCLUDE_SUMMARY = "review.include_summary";

    /** Habilita comentarios inline em linhas especificas do PR. Tipo: BOOLEAN. */
    public static final String REVIEW_INLINE_COMMENTS = "review.inline_comments";

    /** Idioma preferido para gerar reviews (ex: "pt", "en", "es"). Tipo: STRING. */
    public static final String REVIEW_LANGUAGE = "review.language";

    /** Habilita auto-approve de PRs de baixo risco. Tipo: BOOLEAN. Default: desabilitado. */
    public static final String REVIEW_AUTO_APPROVE_ENABLED = "review.auto_approve_enabled";

    /** Threshold de score de risco para auto-approve (0-100). Tipo: NUMBER. Default: 20. */
    public static final String REVIEW_AUTO_APPROVE_THRESHOLD = "review.auto_approve_threshold";

    // ===== Integration: SonarQube =====

    /** URL do servidor SonarQube. Tipo: STRING. */
    public static final String SONARQUBE_URL = "sonarqube.url";

    /** Token de autenticacao do SonarQube. Tipo: STRING. Sensitive. */
    public static final String SONARQUBE_TOKEN = "sonarqube.token";

    // ===== Integration: Slack =====

    /** URL do Incoming Webhook do Slack. Tipo: STRING. */
    public static final String SLACK_WEBHOOK_URL = "slack.webhook-url";

    /** Filtro de severidade para notificacoes Slack: "all", "critical", "high". Tipo: STRING. */
    public static final String SLACK_NOTIFY_ON = "slack.notify-on";

    // ===== Integration: Microsoft Teams =====

    /** URL do Incoming Webhook do Teams. Tipo: STRING. */
    public static final String TEAMS_WEBHOOK_URL = "teams.webhook-url";

    /** Filtro de severidade para notificacoes Teams: "all", "critical", "high". Tipo: STRING. */
    public static final String TEAMS_NOTIFY_ON = "teams.notify-on";

    // ===== Integration: GitHub =====

    /** Token de acesso do GitHub. Tipo: STRING. Sensitive. */
    public static final String GITHUB_TOKEN = "github.token";

    // ===== Integration: GitLab =====

    /** Token de acesso do GitLab. Tipo: STRING. Sensitive. */
    public static final String GITLAB_TOKEN = "gitlab.token";

    // ===== Integration: BitBucket =====

    /** Token de acesso do BitBucket. Tipo: STRING. Sensitive. */
    public static final String BITBUCKET_TOKEN = "bitbucket.token";

    // ===== Integration: OpenRouter =====

    /** Chave de API do OpenRouter. Tipo: STRING. Sensitive. */
    public static final String OPENROUTER_API_KEY = "openrouter.api_key";
}
