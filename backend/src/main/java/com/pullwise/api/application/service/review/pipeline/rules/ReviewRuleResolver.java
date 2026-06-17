package com.pullwise.api.application.service.review.pipeline.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Resolve, de forma <b>determinística</b>, qual checklist de review aplicar a
 * cada arquivo, casando o caminho contra padrões glob (primeiro match vence) e
 * retornando o conteúdo markdown da regra correspondente.
 *
 * <p>Inspirado no mecanismo {@code system_rules.json} + {@code {{system_rule}}}
 * do Open Code Review (Alibaba): em vez de deixar o LLM decidir o que checar, a
 * engenharia escolhe o checklist por tipo de arquivo — mais estável e debugável.
 *
 * <p>As regras vivem em {@code src/main/resources/review-rules/}: um índice
 * {@code system_rules.json} mapeando globs → arquivos markdown, mais o conteúdo
 * de cada regra. Tudo é carregado em memória no startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewRuleResolver {

    private static final String BASE = "review-rules/";

    private final ObjectMapper objectMapper;

    private final List<CompiledRule> rules = new ArrayList<>();
    private String defaultRule = "";

    /** Entrada do índice JSON. */
    private record RuleMapping(String pattern, String rule) {}
    private record RuleIndex(String defaultRule, List<RuleMapping> pathRuleMap) {}

    /** Regra já com o glob compilado e o conteúdo markdown carregado. */
    private record CompiledRule(Pattern pattern, String content) {}

    @PostConstruct
    void load() {
        try {
            RuleIndex index;
            try (InputStream in = new ClassPathResource(BASE + "system_rules.json").getInputStream()) {
                index = objectMapper.readValue(in, RuleIndex.class);
            }

            if (index.defaultRule() != null) {
                defaultRule = readRule(index.defaultRule());
            }
            if (index.pathRuleMap() != null) {
                for (RuleMapping m : index.pathRuleMap()) {
                    String content = readRule(m.rule());
                    if (!content.isBlank()) {
                        rules.add(new CompiledRule(GlobPattern.compile(m.pattern()), content));
                    }
                }
            }
            log.info("ReviewRuleResolver loaded {} file-type rule(s) + default", rules.size());
        } catch (Exception e) {
            log.warn("Could not load review rules, continuing without file-type checklists: {}", e.getMessage());
        }
    }

    /**
     * Retorna o checklist de review para o caminho informado (primeiro glob que
     * casar). Devolve o checklist default quando nenhum padrão casa, ou string
     * vazia se as regras não foram carregadas.
     */
    public String resolve(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return defaultRule;
        }
        String normalized = filePath.replace('\\', '/');
        for (CompiledRule rule : rules) {
            if (rule.pattern().matcher(normalized).matches()) {
                return rule.content();
            }
        }
        return defaultRule;
    }

    private String readRule(String fileName) {
        try (InputStream in = new ClassPathResource(BASE + fileName).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (Exception e) {
            log.warn("Could not read review rule file {}: {}", fileName, e.getMessage());
            return "";
        }
    }
}
