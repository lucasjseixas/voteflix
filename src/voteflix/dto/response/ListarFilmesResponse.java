package voteflix.dto.response;

import com.google.gson.annotations.SerializedName;
import voteflix.dto.FilmeDTO;
import java.util.List;

public class ListarFilmesResponse {
    @SerializedName("status")
    public String status;

    @SerializedName("filmes")
    public List<FilmeDTO> filmes;

    public ListarFilmesResponse(String status, List<FilmeDTO> filmes) {
        this.status = status;
        this.filmes = filmes;
    }

    public ListarFilmesResponse(String status) {
        this.status = status;
        this.filmes = null;
    }

    public ListarFilmesResponse() {}
}
