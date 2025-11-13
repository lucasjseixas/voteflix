package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class EditarReviewRequest extends RequestBase {

    @SerializedName("review")
    public ReviewUpdate review;

    @SerializedName("token")
    public String token;

    public EditarReviewRequest() {}

    public EditarReviewRequest(ReviewUpdate review, String token) {
        super("EDITAR_REVIEW");
        this.review = review;
        this.token = token;
    }

    public static class ReviewUpdate {
        @SerializedName("id")
        public String id;

        @SerializedName("titulo")
        public String titulo;

        @SerializedName("descricao")
        public String descricao;

        @SerializedName("nota")
        public String nota;

        public ReviewUpdate() {}

        public ReviewUpdate(String id, String titulo, String descricao, String nota) {
            this.id = id;
            this.titulo = titulo;
            this.descricao = descricao;
            this.nota = nota;
        }
    }
}
