package com.pullwise.api.application.service.config;

import com.pullwise.api.application.service.llm.client.OllamaClient;
import com.pullwise.api.domain.model.KnowledgeDocument;
import com.pullwise.api.domain.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço de RAG (Retrieval Augmented Generation).
 * Usa pgvector para busca vetorial de documentos de conhecimento.
 * Gera embeddings via Ollama (nomic-embed-text) e busca por similaridade coseno.
 * Fallback para busca por palavras-chave quando embeddings não estão disponíveis.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RAGService {

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final OllamaClient ollamaClient;

    @Value("${pullwise.rag.embedding-model:nomic-embed-text}")
    private String embeddingModel;

    @Value("${pullwise.rag.embedding-dimensions:1536}")
    private int embeddingDimensions;

    /**
     * Busca documentos relevantes baseado em uma query.
     * Usa busca vetorial (pgvector) quando embeddings estão disponíveis,
     * caso contrário faz fallback para busca por palavras-chave.
     */
    public List<KnowledgeDocument> searchRelevantDocuments(Long projectId, String query, int limit) {
        // Tentar busca vetorial primeiro
        try {
            if (ollamaClient.isAvailable()) {
                List<KnowledgeDocument> vectorResults = searchByVector(projectId, query, limit);
                if (!vectorResults.isEmpty()) {
                    log.debug("Vector search returned {} results for project {}", vectorResults.size(), projectId);
                    return vectorResults;
                }
            }
        } catch (Exception e) {
            log.debug("Vector search failed, falling back to keywords: {}", e.getMessage());
        }

        // Fallback: busca por palavras-chave
        return searchByKeywords(projectId, query, limit);
    }

    /**
     * Busca vetorial usando pgvector (similaridade coseno).
     * Gera embedding da query e busca os documentos mais próximos.
     */
    private List<KnowledgeDocument> searchByVector(Long projectId, String query, int limit) {
        List<Double> queryEmbedding = ollamaClient.generateEmbedding(embeddingModel, query);
        if (queryEmbedding == null || queryEmbedding.isEmpty()) {
            return List.of();
        }

        // Formatar vetor como string para pgvector: [0.1,0.2,0.3,...]
        String vectorStr = "[" + queryEmbedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "]";

        return knowledgeDocumentRepository.findNearestNeighbors(projectId, vectorStr, limit);
    }

    /**
     * Busca por palavras-chave (fallback quando embeddings não estão disponíveis).
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
     * Gera embeddings para todos os documentos ativos que ainda não possuem embedding.
     */
    @Async
    public void indexProjectDocuments(Long projectId) {
        log.info("Starting RAG indexing for project {}", projectId);

        List<KnowledgeDocument> docs = knowledgeDocumentRepository.findActiveByProjectId(projectId);
        int indexed = 0;

        for (KnowledgeDocument doc : docs) {
            if (doc.getEmbedding() != null && !doc.getEmbedding().isBlank()) {
                continue; // Já tem embedding
            }

            try {
                generateAndSaveEmbedding(doc);
                indexed++;
            } catch (Exception e) {
                log.warn("Failed to generate embedding for document {}: {}", doc.getId(), e.getMessage());
            }
        }

        log.info("RAG indexing completed for project {}: {}/{} documents indexed",
                projectId, indexed, docs.size());
    }

    /**
     * Gera embedding para um documento e salva no banco.
     */
    private void generateAndSaveEmbedding(KnowledgeDocument doc) {
        if (!ollamaClient.isAvailable()) {
            log.debug("Ollama not available, skipping embedding generation for document {}", doc.getId());
            return;
        }

        // Combinar título + conteúdo para embedding mais rico
        String textForEmbedding = doc.getTitle() + "\n\n" + truncateContent(doc.getContent(), 8000);

        List<Double> embedding = ollamaClient.generateEmbedding(embeddingModel, textForEmbedding);
        if (embedding == null || embedding.isEmpty()) {
            log.warn("Empty embedding generated for document {}", doc.getId());
            return;
        }

        // Formatar vetor como string para pgvector
        String vectorStr = "[" + embedding.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",")) + "]";

        doc.setEmbedding(vectorStr);
        knowledgeDocumentRepository.save(doc);

        log.debug("Generated embedding ({} dimensions) for document {}", embedding.size(), doc.getId());
    }

    /**
     * Adiciona um documento ao conhecimento.
     * Gera embedding automaticamente se Ollama estiver disponível.
     */
    public KnowledgeDocument addDocument(Long projectId, Long orgId, String title,
                                         String content, String sourceType, String sourcePath) {

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

        // Gerar embedding em background
        try {
            generateAndSaveEmbedding(saved);
        } catch (Exception e) {
            log.debug("Embedding generation skipped for document {}: {}", saved.getId(), e.getMessage());
        }

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
