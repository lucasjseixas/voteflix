package voteflix.util;

public enum HttpStatus {
    // 2xx - Sucesso
    OK("200", "Sucesso: Operacao realizada com sucesso."),
    CREATED("201", "Sucesso: Recurso cadastrado."),

    // 4xx - Erros do cliente
    BAD_REQUEST("400", "Erro: Operacao nao encontrada ou invalida"),
    UNAUTHORIZED("401", "Erro: Token invalido"),
    FORBIDDEN("403", "Erro: Sem permissao"),
    NOT_FOUND("404", "Erro: Recurso inexistente"),
    INVALID_FIELDS("405", "Erro: Campos invalidos, verifique o tipo e quantidade de caracteres"),
    CONFLICT("409", "Erro: Recurso ja existe"),
//    GONE("410", "Removido Permanentemente", "O recurso foi removido e não voltará."),
//    LENGTH_REQUIRED("411", "Tamanho Obrigatório", "A requisição precisa especificar o tamanho do conteúdo."),
//    PAYLOAD_TOO_LARGE("413", "Requisição Muito Grande", "A requisição excede o tamanho permitido."),
//    IM_A_TEAPOT("418", "Sou uma Chaleira", "O servidor se recusa a preparar café."),
    UNPROCESSABLE_ENTITY("422", "Erro: Chaves faltantes ou invalidas"),
    // 5xx - Erros do servidor
    INTERNAL_SERVER_ERROR("500", "Erro: Falha interna do servidor");

    // Status desconhecido
    //UNKNOWN("000", "Desconhecido", "Status de resposta desconhecido.");

    private final String code;
    private final String message;

    HttpStatus(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
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
            return INTERNAL_SERVER_ERROR;
        }

        for (HttpStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return INTERNAL_SERVER_ERROR;
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
//    public String getFormattedMessage() {
//        return String.format("[%s] %s: %s", code, message);
//    }

    @Override
    public String toString() {
        return code + " - " + message;
    }
}