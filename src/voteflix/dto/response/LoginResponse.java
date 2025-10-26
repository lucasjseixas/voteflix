package voteflix.dto.response;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("status")
    public String status;

    @SerializedName("mensagem")
    public String mensagem;

    @SerializedName("token")
    public String token;

    // Construtor para sucesso
    public LoginResponse(String status, String mensagem, String token) {
        this.status = status;
        this.mensagem = mensagem;
        this.token = token;
    }

    // Construtor para falha (token será null)
    public LoginResponse(String status, String mensagem) {
        this.status = status;
        this.mensagem = mensagem;
        this.token = null;
    }

    // Construtor padrão para GSON
    public LoginResponse() {}
}
