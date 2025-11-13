package voteflix.dto.response;

import com.google.gson.annotations.SerializedName;
import voteflix.dto.ReviewDTO;
import java.util.List;

public class ListarReviewsUsuarioResponse {
    @SerializedName("status")
    public String status;

    @SerializedName("mensagem")
    public String mensagem;

    @SerializedName("reviews")
    public List<ReviewDTO> reviews;

    public ListarReviewsUsuarioResponse(String status, String mensagem, List<ReviewDTO> reviews) {
        this.status = status;
        this.mensagem = mensagem;
        this.reviews = reviews;
    }

    public ListarReviewsUsuarioResponse() {}
}
