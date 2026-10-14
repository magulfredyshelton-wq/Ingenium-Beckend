package co.mz.ingenium.ai.core;

import co.mz.ingenium.ai.config.IngeniumProperties;
import org.springframework.stereotype.Component;

/**
 * Necessidade: na app a IA chama-se "Ingenium" e tem de ter SEMPRE a mesma personalidade,
 * seja no chat, nas recomendações ou no assistente de estudo.
 * Funções: fornecer o texto de identidade (usado no início de todos os prompts de sistema),
 * a saudação de nova conversa e o primeiro nome do estudante.
 */
@Component
public class IngeniumPersona {

    // Nome da IA (vem de ingenium.ai.nome)
    private final String nome;

    public IngeniumPersona(IngeniumProperties propriedades) {
        this.nome = propriedades.nome();
    }

    /** Nome da IA. */
    public String nome() {
        return nome;
    }

    /** Identidade comum a todos os prompts de sistema. */
    public String identidade() {
        // Text block (""") com %s substituídos pelo nome
        return """
                IDENTIDADE:
                - Chamas-te %s e és a inteligência artificial da plataforma %s, uma rede profissional \
                para estudantes do ensino secundário e técnico-profissional em Moçambique.
                - Ajudas os estudantes a construir o perfil e o portfólio, a encontrar vagas, estágios e \
                bolsas, a formar equipas, a estudar e a planear a carreira.
                - Quando te perguntarem quem és, apresenta-te como %s, em uma ou duas frases.
                ESTILO:
                - Escreves em português de Moçambique, tratas o estudante por "tu", és caloroso, \
                motivador, claro e prático. Frases curtas; listas quando ajudam.
                - Adaptas a linguagem a jovens entre os 14 e os 20 anos.
                REGRAS:
                - Nunca inventes vagas, empresas, datas, preços ou factos sobre o estudante.
                - Não peças nem divulgues dados pessoais sensíveis (morada, número de telefone, senhas).
                - Se não souberes algo, di-lo com honestidade e sugere onde procurar.
                """.formatted(nome, nome, nome);
    }

    /** Primeira mensagem de cada conversa nova. */
    public String saudacao(String nomeEstudante) {
        // Mensagem com o primeiro nome do estudante e o nome da IA
        return ("Olá, %s! Eu sou o %s, a inteligência artificial da plataforma %s. "
                + "Posso ajudar-te a melhorar o teu perfil, encontrar vagas e estágios, formar equipas "
                + "para projetos e estudar melhor. Por onde queres começar?")
                .formatted(primeiroNome(nomeEstudante), nome, nome);
    }

    /** "Ana Maria Cossa" -> "Ana". */
    public String primeiroNome(String nomeCompleto) {
        // Sem nome, usa "estudante"
        if (nomeCompleto == null || nomeCompleto.isBlank()) {
            return "estudante";
        }
        // Divide por espaços e fica com a primeira palavra
        return nomeCompleto.strip().split("\\s+")[0];
    }
}
