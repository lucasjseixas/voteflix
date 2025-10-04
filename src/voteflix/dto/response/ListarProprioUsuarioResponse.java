package voteflix.dto.response;

import com.google.gson.annotations.SerializedName;

public class ListarProprioUsuarioResponse {
    @SerializedName("status")
    public String status;

    @SerializedName("usuario")
    public String usuario;

    // Construtor para sucesso
    public ListarProprioUsuarioResponse(String status, String usuario) {
        this.status = status;
        this.usuario = usuario;
    }

    // Construtor para falha (usuario será null)
    public ListarProprioUsuarioResponse(String status) {
        this.status = status;
    }

    // Construtor padrão para GSON
    public ListarProprioUsuarioResponse() {}
}

