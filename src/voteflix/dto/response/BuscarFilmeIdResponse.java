package voteflix.dto.response;

import com.google.gson.annotations.SerializedName;
import voteflix.dto.FilmeDTO;
import voteflix.dto.ReviewDTO;
import java.util.List;

public class BuscarFilmeIdResponse {
    @SerializedName("status")
    public String status;

    @SerializedName("mensagem")
    public String mensagem;

    @SerializedName("filme")
    public FilmeDTO filme;

    @SerializedName("reviews")
    public List<ReviewDTO> reviews;

    public BuscarFilmeIdResponse(String status, String mensagem, FilmeDTO filme, List<ReviewDTO> reviews) {
        this.status = status;
        this.mensagem = mensagem;
        this.filme = filme;
        this.reviews = reviews;
    }

    public BuscarFilmeIdResponse() {}
}
