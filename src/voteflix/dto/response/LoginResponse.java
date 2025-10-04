package voteflix.dto.response;

import com.google.gson.annotations.SerializedName;

public class LoginResponse {
    @SerializedName("status")
    public String status;

    @SerializedName("token")
    public String token;

    // Construtor para sucesso
    public LoginResponse(String status, String token) {
        this.status = status;
        this.token = token;
    }

    // Construtor para falha (token será null)
    public LoginResponse(String status) {
        this.status = status;
        this.token = null;
    }

    // Construtor padrão para GSON
    public LoginResponse() {}
}
