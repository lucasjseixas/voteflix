package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class ExcluirReviewRequest extends RequestBase {

    @SerializedName("id")
    public String id;

    @SerializedName("token")
    public String token;

    public ExcluirReviewRequest() {}

    public ExcluirReviewRequest(String id, String token) {
        super("EXCLUIR_REVIEW");
        this.id = id;
        this.token = token;
    }
}
