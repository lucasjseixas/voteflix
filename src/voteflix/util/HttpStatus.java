package voteflix.util;

public enum HttpStatus {
    // 2xx - Sucesso
    OK("200", "Sucesso", "Operação realizada com sucesso."),
    CREATED("201", "Criado", "Recurso criado com sucesso."),

    // 4xx - Erros do cliente
    BAD_REQUEST("400", "Requisição Inválida", "A requisição contém dados inválidos ou malformados."),
    UNAUTHORIZED("401", "Não Autorizado", "Credenciais inválidas ou token expirado."),
    FORBIDDEN("403", "Proibido", "Você não tem permissão para acessar este recurso."),
    NOT_FOUND("404", "Não Encontrado", "O recurso solicitado não foi encontrado."),
    CONFLICT("409", "Conflito", "O recurso já existe."),
    GONE("410", "Removido Permanentemente", "O recurso foi removido e não voltará."),
    LENGTH_REQUIRED("411", "Tamanho Obrigatório", "A requisição precisa especificar o tamanho do conteúdo."),
    PAYLOAD_TOO_LARGE("413", "Requisição Muito Grande", "A requisição excede o tamanho permitido."),
    IM_A_TEAPOT("418", "Sou uma Chaleira", "O servidor se recusa a preparar café."),
    UNPROCESSABLE_ENTITY("422", "Entidade Não Processável", "Os dados estão fora do padrão esperado."),

    // 5xx - Erros do servidor
    INTERNAL_SERVER_ERROR("500", "Erro Interno do Servidor", "Ocorreu um erro inesperado no servidor."),

    // Status desconhecido
    UNKNOWN("000", "Desconhecido", "Status de resposta desconhecido.");

    private final String code;
    private final String title;
    private final String message;

    HttpStatus(String code, String title, String message) {
        this.code = code;
        this.title = title;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Obtém o HttpStatus pelo código
     * @param code Código do status (ex: "200", "404")
     * @return HttpStatus correspondente ou UNKNOWN se não encontrado
     */
    public static HttpStatus fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }

        for (HttpStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return UNKNOWN;
    }

    /**
     * Verifica se é um status de sucesso (2xx)
     * @return true se for sucesso, false caso contrário
     */
    public boolean isSuccess() {
        return code.startsWith("2");
    }

    /**
     * Verifica se é um erro do cliente (4xx)
     * @return true se for erro do cliente, false caso contrário
     */
    public boolean isClientError() {
        return code.startsWith("4");
    }

    /**
     * Verifica se é um erro do servidor (5xx)
     * @return true se for erro do servidor, false caso contrário
     */
    public boolean isServerError() {
        return code.startsWith("5");
    }

    /**
     * Retorna mensagem formatada completa
     * @return String formatada com código, título e mensagem
     */
    public String getFormattedMessage() {
        return String.format("[%s] %s: %s", code, title, message);
    }

    @Override
    public String toString() {
        return code + " - " + title;
    }
}