package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class CadastrarUsuarioRequest extends RequestBase {

    @SerializedName("usuario")
    public String usuario;

    @SerializedName("senha")
    public String senha;

    public CadastrarUsuarioRequest() {}

    public CadastrarUsuarioRequest(String usuario, String senha) {
        super("CRIAR_USUARIO");
        this.usuario = usuario;
        this.senha = senha;
    }
}