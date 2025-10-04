package voteflix.session;

import java.time.LocalDateTime;

public class SessionInfo {
    private final String username;
    private final String ipAddress;
    private final LocalDateTime loginTime;

    public SessionInfo(String username, String ipAddress) {
        this.username = username;
        this.ipAddress = ipAddress;
        this.loginTime = LocalDateTime.now();
    }

    public String getUsername() {
        return username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }
}
