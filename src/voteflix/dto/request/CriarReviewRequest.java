package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class CriarReviewRequest extends RequestBase {

    @SerializedName("review")
    public ReviewData review;

    @SerializedName("token")
    public String token;

    public CriarReviewRequest() {}

    public CriarReviewRequest(ReviewData review, String token) {
        super("CRIAR_REVIEW");
        this.review = review;
        this.token = token;
    }

    public static class ReviewData {
        @SerializedName("id_filme")
        public String idFilme;

        @SerializedName("titulo")
        public String titulo;

        @SerializedName("descricao")
        public String descricao;

        @SerializedName("nota")
        public String nota;

        public ReviewData() {}

        public ReviewData(String idFilme, String titulo, String descricao, String nota) {
            this.idFilme = idFilme;
            this.titulo = titulo;
            this.descricao = descricao;
            this.nota = nota;
        }
    }
}
