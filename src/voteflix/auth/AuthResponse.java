package voteflix.auth;

import com.google.gson.annotations.SerializedName;

/**
 * Classe para desserializar a resposta do servidor após a operação LOGIN.
 * Contém o token e as claims essenciais.
 */

public class AuthResponse {
    @SerializedName("status")
    public String status;

    @SerializedName("token")
    public String token;

//    // Claims essenciais (devem estar no JWT)
//    @SerializedName("id_usuario")
//    public int idUsuario; // Requisito: id do usuario
//    @SerializedName("id")
//    public String id;

    @SerializedName("usuario")
    public String usuario; // Requisito: nome do usuario

    @SerializedName("funcao")
    public String funcao; // Requisito: funcao ("user", "admin")

    // Getters
    public String getStatus() {
        return status;
    }

    public String getToken() {
        return token;
    }
}

//    public int getIdUsuario() {
//        return idUsuario;
//    }
//
//    public String getUsuario() {
//        return usuario;
//    }
//
//    public String getFuncao() {
//        return funcao;
//    }
//}