package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class ListarUsuariosRequest extends RequestBase {

    @SerializedName("token")
    public String token;

    public ListarUsuariosRequest() {}

    public ListarUsuariosRequest(String token) {
        super("LISTAR_USUARIOS");
        this.token = token;
    }
}
