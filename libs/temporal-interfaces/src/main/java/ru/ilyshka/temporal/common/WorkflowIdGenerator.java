package ru.ilyshka.temporal.common;

import io.temporal.workflow.Workflow;

/**
 * Интерфейс для генерации уникальных Workflow Id.
 * Гарантирует детерминированность и уникальность идентификаторов.
 */
public interface WorkflowIdGenerator {

    /**
     * Генерирует уникальный Workflow Id на основе контекста.
     *
     * @param workflowType тип воркфлоу
     * @param context контекстные данные для генерации
     * @return уникальный Workflow Id
     */
    String generate(String workflowType, String context);

    /**
     * Генерирует уникальный Workflow Id с префиксом.
     *
     * @param prefix префикс для идентификатора
     * @param context контекстные данные для генерации
     * @return уникальный Workflow Id с префиксом
     */
    default String generateWithPrefix(String prefix, String context) {
        return prefix + "-" + generate(prefix, context);
    }

    /**
     * Генерирует детерминированный Workflow Id на основе текущего workflow.
     *
     * @param workflowType тип воркфлоу
     * @param context контекстные данные для генерации
     * @return уникальный Workflow Id
     */
    static String deterministicGenerate(String workflowType, String context) {
        String workflowId = Workflow.getInfo().getWorkflowId();
        return workflowId + "-" + workflowType + "-" + context;
    }
}