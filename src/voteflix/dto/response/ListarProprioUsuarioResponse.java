package voteflix.dto.response;

import com.google.gson.annotations.SerializedName;

public class ListarProprioUsuarioResponse {
    @SerializedName("status")
    public String status;

    @SerializedName("usuario")
    public String usuario;

    public ListarProprioUsuarioResponse(String status, String usuario) {
        this.status = status;
        this.usuario = usuario;
    }
    public ListarProprioUsuarioResponse() {}
}