package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class LoginRequest extends RequestBase {

    @SerializedName("usuario")
    public String usuario;

    @SerializedName("senha")
    public String senha;

    public LoginRequest() {}

    public LoginRequest(String usuario, String senha) {
        super("LOGIN");
        this.usuario = usuario;
        this.senha = senha;
    }
}