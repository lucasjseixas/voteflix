package voteflix.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO para serialização/desserialização de usuários em JSON.
 * Contém id, usuario e senha.
 */
public class UsuarioDTO {
    @SerializedName("id")
    public int id;

    @SerializedName("usuario")
    public String usuario;

    @SerializedName("senha")
    public String senha;

    public UsuarioDTO() {}

    public UsuarioDTO(int id, String usuario, String senha) {
        this.id = id;
        this.usuario = usuario;
        this.senha = senha;
    }
}
