package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class ListarFilmesRequest extends RequestBase {

    @SerializedName("token")
    public String token;

    public ListarFilmesRequest() {}

    public ListarFilmesRequest(String token) {
        super("LISTAR_FILMES");
        this.token = token;
    }
}
