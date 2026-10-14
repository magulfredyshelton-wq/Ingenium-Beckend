package co.mz.ingenium.ai.model;

/**
 * Necessidade: a base vetorial guarda textos de naturezas diferentes e precisamos de os
 * filtrar (ex.: recomendar só VAGAS).
 * Funções: enumerar os tipos de texto indexado.
 */
public enum TipoEntidade {
    // Perfil de um estudante (enviado pelo backend principal)
    PERFIL,
    // Vaga, estágio ou oportunidade
    VAGA,
    // Texto de conhecimento (guias de carreira, FAQ da plataforma, cursos)
    CONHECIMENTO
}
