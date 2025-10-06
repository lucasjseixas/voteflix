package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class AdminExcluirUsuarioRequest extends RequestBase {

    @SerializedName("id")
    public String id;

    @SerializedName("token")
    public String token;

    public AdminExcluirUsuarioRequest() {}

    public AdminExcluirUsuarioRequest(String id, String token) {
        super("ADMIN_EXCLUIR_USUARIO");
        this.id = id;
        this.token = token;
    }
}