package voteflix.dto.request;

import com.google.gson.annotations.SerializedName;

public class LogoutRequest extends RequestBase {

    @SerializedName("token")
    public String token;

    public LogoutRequest() {}

    public LogoutRequest(String token) {
        super("LOGOUT");
        this.token = token;
    }
}