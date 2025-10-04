package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class ExcluirProprioUsuarioRequest extends RequestBase {

    @SerializedName("token")
    public String token;

    public ExcluirProprioUsuarioRequest() {}

    public ExcluirProprioUsuarioRequest(String token) {
        super("EXCLUIR_PROPRIO_USUARIO");
        this.token = token;
    }
}
