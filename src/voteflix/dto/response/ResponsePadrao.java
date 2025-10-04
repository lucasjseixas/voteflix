package voteflix.dto.response;

import com.google.gson.annotations.SerializedName;

public class ResponsePadrao {
    @SerializedName("status")
    public String status;

    public ResponsePadrao(){}
    public ResponsePadrao(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}