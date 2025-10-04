package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class ListarProprioUsuarioRequest extends RequestBase {

    @SerializedName("token")
    public String token;

    public ListarProprioUsuarioRequest() {}

    public ListarProprioUsuarioRequest(String token) {
        super("LISTAR_PROPRIO_USUARIO");
        this.token = token;
    }
}
