package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class ListarReviewsUsuarioRequest extends RequestBase {

    @SerializedName("token")
    public String token;

    public ListarReviewsUsuarioRequest() {}

    public ListarReviewsUsuarioRequest(String token) {
        super("LISTAR_REVIEWS_USUARIO");
        this.token = token;
    }
}
