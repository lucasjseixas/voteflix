package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class UsuarioUpdate {
    @SerializedName("senha")
    public String senha;

    public UsuarioUpdate() {}

    public UsuarioUpdate(String senha) {
        this.senha = senha;
    }
}