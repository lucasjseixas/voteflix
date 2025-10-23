package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class CadastrarUsuarioRequest extends RequestBase {

    @SerializedName("usuario")
    public UsuarioData usuario;

    public CadastrarUsuarioRequest() {}

    public CadastrarUsuarioRequest(String nome, String senha) {
        super("CRIAR_USUARIO");
        this.usuario = new UsuarioData(nome, senha);
    }

    public static class UsuarioData {
        @SerializedName("nome")
        public String nome;

        @SerializedName("senha")
        public String senha;

        public UsuarioData() {}

        public UsuarioData(String nome, String senha) {
            this.nome = nome;
            this.senha = senha;
        }
    }
}