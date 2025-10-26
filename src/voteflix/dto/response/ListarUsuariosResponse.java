package voteflix.dto.response;

import com.google.gson.annotations.SerializedName;
import voteflix.dto.UsuarioDTO;
import java.util.List;

public class ListarUsuariosResponse {
    @SerializedName("status")
    public String status;

    @SerializedName("mensagem")
    public String mensagem;

    @SerializedName("usuarios")
    public List<UsuarioDTO> usuarios;

    public ListarUsuariosResponse(String status, String mensagem , List<UsuarioDTO> usuarios) {
        this.status = status;
        this.mensagem = mensagem;
        this.usuarios = usuarios;
    }

    public ListarUsuariosResponse(String status) {
        this.status = status;
        this.usuarios = null;
    }

    public ListarUsuariosResponse() {}
}