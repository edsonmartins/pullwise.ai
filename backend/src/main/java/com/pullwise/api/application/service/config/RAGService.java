package com.pullwise.api.application.service.config;

import com.pullwise.api.domain.model.KnowledgeDocument;
import com.pullwise.api.domain.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de RAG (Retrieval Augmented Generation).
 * Usa pgvector para busca vetorial de documentos de conhecimento.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    /**
     * Busca documentos relevantes baseado em uma query.
     */
    public List<KnowledgeDocument> searchRelevantDocuments(Long projectId, String query, int limit) {
        // Por enquanto, busca por palavras-chave
        // Embeddings podem ser adicionados posteriormente
        return searchByKeywords(projectId, query, limit);
    }

    /**
     * Busca por palavras-chave.
     */
    private List<KnowledgeDocument> searchByKeywords(Long projectId, String query, int limit) {
        List<KnowledgeDocument> allDocs = knowledgeDocumentRepository.findActiveByProjectId(projectId);

        String[] keywords = query.toLowerCase().split("\\s+");

        return allDocs.stream()
                .filter(doc -> {
                    String content = (doc.getTitle() + " " + doc.getContent()).toLowerCase();
                    for (String keyword : keywords) {
                        if (content.contains(keyword)) {
                            return true;
                        }
                    }
                    return false;
                })
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Indexa documentos de um projeto para RAG.
     */
    public void indexProjectDocuments(Long projectId) {
        log.info("RAG indexing for project {} - embeddings not yet implemented", projectId);
    }

    /**
     * Adiciona um documento ao conhecimento.
     */
    public KnowledgeDocument addDocument(Long projectId, Long orgId, String title,
                                         String content, String sourceType, String sourcePath) {

        // Criar documentos usando setters manuais (sem builder para evitar problemas com entidades JPA)
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(title);
        doc.setContent(content);
        doc.setSourceType(sourceType);
        doc.setSourcePath(sourcePath);
        doc.setIsActive(true);

        if (projectId != null) {
            com.pullwise.api.domain.model.Project p = new com.pullwise.api.domain.model.Project();
            p.setId(projectId);
            doc.setProject(p);
        }
        if (orgId != null) {
            com.pullwise.api.domain.model.Organization o = new com.pullwise.api.domain.model.Organization();
            o.setId(orgId);
            doc.setOrganization(o);
        }

        KnowledgeDocument saved = knowledgeDocumentRepository.save(doc);
        log.info("Document {} saved", saved.getId());
        return saved;
    }

    /**
     * Remove um documento do conhecimento (soft delete).
     */
    public void removeDocument(Long documentId) {
        KnowledgeDocument doc = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        doc.setIsActive(false);
        knowledgeDocumentRepository.save(doc);
    }

    /**
     * Gera contexto para o LLM baseado nos documentos relevantes.
     */
    public String generateContext(Long projectId, String query, int maxDocs) {
        List<KnowledgeDocument> docs = searchRelevantDocuments(projectId, query, maxDocs);

        if (docs.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("Relevant project context:\n\n");

        for (KnowledgeDocument doc : docs) {
            context.append(String.format("From: %s\n", doc.getTitle()));
            context.append(String.format("%s\n\n", truncateContent(doc.getContent(), 500)));
        }

        return context.toString();
    }

    /**
     * Trunca conteúdo para não exceder limite.
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content != null ? content : "";
        }
        return content.substring(0, maxLength) + "...";
    }
}
