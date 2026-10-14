package co.mz.ingenium.ai.model;

/**
 * Necessidade: cada mensagem do chat foi escrita ou pelo estudante ou pelo Ingenium.
 * Funções: enumerar os dois papéis possíveis (gravados como texto na coluna "papel").
 */
public enum PapelMensagem {
    // Mensagem escrita pelo estudante
    UTILIZADOR,
    // Mensagem escrita pelo Ingenium (IA)
    ASSISTENTE
}
