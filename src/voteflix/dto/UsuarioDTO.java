package voteflix.dto;

import com.google.gson.annotations.SerializedName;

/**
 * DTO para serialização/desserialização de usuários em JSON.
 * Schema: id (String), nome (String), senha (String)
 */
public class UsuarioDTO {
    @SerializedName("id")
    public String id;

    @SerializedName("nome")
    public String nome;

    @SerializedName("senha")
    public String senha;

    public UsuarioDTO() {}

    public UsuarioDTO(String id, String nome, String senha) {
        this.id = id;
        this.nome = nome;
        this.senha = senha;
    }
}