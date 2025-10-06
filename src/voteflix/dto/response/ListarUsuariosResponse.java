package voteflix.dto.response;

import com.google.gson.annotations.SerializedName;
import voteflix.dto.UsuarioDTO;
import java.util.List;

public class ListarUsuariosResponse {
    @SerializedName("status")
    public String status;

    @SerializedName("usuarios")
    public List<UsuarioDTO> usuarios;

    public ListarUsuariosResponse(String status, List<UsuarioDTO> usuarios) {
        this.status = status;
        this.usuarios = usuarios;
    }

    public ListarUsuariosResponse(String status) {
        this.status = status;
        this.usuarios = null;
    }

    public ListarUsuariosResponse() {}
}