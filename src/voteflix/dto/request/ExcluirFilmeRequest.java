package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class ExcluirFilmeRequest extends RequestBase {

    @SerializedName("id")
    public String id;

    @SerializedName("token")
    public String token;

    public ExcluirFilmeRequest() {}

    public ExcluirFilmeRequest(String id, String token) {
        super("EXCLUIR_FILME");
        this.id = id;
        this.token = token;
    }
}
