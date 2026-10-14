package co.mz.ingenium.ai.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Necessidade: o documento "Visão da IA" decidiu que existe UM ÚNICO ChatClient partilhado
 * por todos os serviços. Esta classe cria-o (e o TransactionTemplate usado no streaming).
 * Funções: expor os beans ChatClient e TransactionTemplate ao resto da aplicação.
 */
// Diz ao Spring que esta classe contém métodos @Bean (fábricas de objetos)
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Configuration
public class AiConfiguration {

    /**
     * O Spring AI já criou um ChatClient.Builder ligado ao Gemini (graças ao starter
     * google-genai e às propriedades spring.ai.google.genai.*). Aqui só o construímos.
     */
    // O objeto devolvido fica registado no contexto e pode ser injetado em qualquer classe
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * TransactionTemplate: permite abrir uma transação "à mão" dentro de código reativo
     * (streaming), onde a anotação @Transactional não funciona.
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager gestor) {
        // Cria o template usando o gestor de transações do JPA
        return new TransactionTemplate(gestor);
    }
}
