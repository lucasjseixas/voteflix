package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class EditarProprioUsuarioRequest extends RequestBase {

    @SerializedName("usuario")
    public UsuarioUpdate usuario;

    @SerializedName("token")
    public String token;

    public EditarProprioUsuarioRequest() {}

    public EditarProprioUsuarioRequest(String senha, String token) {
        super("EDITAR_PROPRIO_USUARIO");
        this.usuario = new UsuarioUpdate(senha);
        this.token = token;
    }
}