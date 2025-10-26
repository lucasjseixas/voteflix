package voteflix.dto.response;

import com.google.gson.annotations.SerializedName;

public class ListarProprioUsuarioResponse {
    @SerializedName("status")
    public String status;

    @SerializedName("mensagem")
    public String mensagem;

    @SerializedName("usuario")
    public String usuario;

    public ListarProprioUsuarioResponse(String status, String mensagem, String usuario) {
        this.status = status;
        this.mensagem = mensagem;
        this.usuario = usuario;
    }
    public ListarProprioUsuarioResponse() {}
}