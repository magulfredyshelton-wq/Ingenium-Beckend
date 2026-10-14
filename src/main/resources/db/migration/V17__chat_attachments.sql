-- Anexos no chat (imagem, áudio, ficheiro) e mensagens de chamada.

-- Nome original do ficheiro, para mostrar no chat (ex.: relatorio.pdf).
ALTER TABLE file ADD COLUMN original_name varchar(255);

-- Tipo da mensagem. As mensagens que já existem ficam como TEXT.
ALTER TABLE message ADD COLUMN type varchar(10) NOT NULL DEFAULT 'TEXT'
    CHECK (type IN ('TEXT', 'IMAGE', 'AUDIO', 'FILE', 'CALL'));

-- Ficheiro anexado (nulo nas mensagens de texto).
ALTER TABLE message ADD COLUMN file_id uuid REFERENCES file(id);

-- Uma mensagem com anexo pode não ter texto, mas tem de ter texto OU anexo.
ALTER TABLE message ALTER COLUMN message DROP NOT NULL;
ALTER TABLE message ADD CONSTRAINT message_content_check
    CHECK (message IS NOT NULL OR file_id IS NOT NULL);
