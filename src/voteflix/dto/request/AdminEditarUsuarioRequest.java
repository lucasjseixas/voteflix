package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class AdminEditarUsuarioRequest extends RequestBase {

    @SerializedName("usuario")
    public UsuarioUpdate usuario;

    @SerializedName("token")
    public String token;

    @SerializedName("id")
    public String id;

    public AdminEditarUsuarioRequest() {}

    public AdminEditarUsuarioRequest(String senha, String token, String id) {
        super("ADMIN_EDITAR_USUARIO");
        this.usuario = new UsuarioUpdate(senha);
        this.token = token;
        this.id = id;
    }
}
