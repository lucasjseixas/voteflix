package voteflix.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonValidator {

    /**
     * Valida se todas as chaves do JSON seguem o padrão snake_case
     * (letras minúsculas, números e underscore apenas, sem acentuação)
     *
     * @param jsonString String JSON a ser validada
     * @return true se válido, false caso contrário
     */
    public static boolean validateKeys(String jsonString) {
        try {
            JsonElement element = JsonParser.parseString(jsonString);

            if (element.isJsonObject()) {
                return validateJsonObjectKeys(element.getAsJsonObject());
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateJsonObjectKeys(JsonObject jsonObject) {
        for (String key : jsonObject.keySet()) {
            // Regex: apenas letras minúsculas, números e underscore
            if (!key.matches("^[a-z0-9_]+$")) {
                System.err.println("Chave JSON inválida detectada: '" + key + "' (deve ser snake_case sem acentuação)");
                return false;
            }

            // Validação dos valores String
            JsonElement value = jsonObject.get(key);

            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                String strValue = value.getAsString();
                // Regex: apenas letras (maiúsculas/minúsculas), números, underscore e alguns caracteres especiais comuns
                // Permite: a-z, A-Z, 0-9, underscore, ponto, hífen
                if (!strValue.matches("^[a-zA-Z0-9_.\\-]*$")) {
                    System.err.println("Valor inválido no campo '" + key + "': '" + strValue + "' (contém acentuação ou caracteres especiais não permitidos)");
                    return false;
                }
            }

            // Validação recursiva para objetos aninhados
            if (value.isJsonObject() && !validateJsonObjectKeys(value.getAsJsonObject())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Valida se o valor de "operacao" está em formato válido:
     * - Letras maiúsculas
     * - Pode conter underscore
     * - Sem acentuação
     *
     * @param operacao Valor da operação a ser validada
     * @return true se válido, false caso contrário
     */
    public static boolean validateOperacao(String operacao) {
        if (operacao == null || operacao.isEmpty()) {
            return false;
        }

        // Regex: apenas letras MAIÚSCULAS e underscore
        if (!operacao.matches("^[A-Z_]+$")) {
            System.err.println("Operação inválida: '" + operacao + "' (deve ser MAIÚSCULAS e underscore, sem acentuação)");
            return false;
        }

        return true;
    }

    /**
     * Valida completamente o JSON: chaves e operação
     *
     * @param jsonString String JSON completa
     * @return true se válido, false caso contrário
     */
    public static boolean validateComplete(String jsonString) {
        // 1. Valida as chaves
        if (!validateKeys(jsonString)) {
            return false;
        }

        // 2. Extrai e valida a operação
        try {
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();

            if (json.has("operacao")) {
                String operacao = json.get("operacao").getAsString();
                return validateOperacao(operacao);
            }

            // Se não tem campo "operacao", é inválido
            System.err.println("JSON sem campo 'operacao'");
            return false;

        } catch (Exception e) {
            System.err.println("Erro ao validar JSON: " + e.getMessage());
            return false;
        }
    }
}