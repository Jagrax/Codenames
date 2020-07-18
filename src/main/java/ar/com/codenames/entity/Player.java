package ar.com.codenames.entity;

import com.corundumstudio.socketio.SocketIOClient;

public class Player {

    private String id;
    private String nickname;
    private Integer teamId;
    private Role role;

    public enum Role {
        GUESSER, SPYMASTER
    }

    public Player(SocketIOClient client, String nickname) {
        this.id = client.getSessionId().toString();
        this.nickname = nickname;
        this.role = Role.GUESSER;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "Player ["
                + ((id != null) ? "id=" + id + ", " : "")
                + ((nickname != null) ? "nickname=" + nickname + ", " : "")
                + ((teamId != null) ? "teamId=" + teamId + ", " : "")
                + ((role != null) ? "role=" + role : "")
                + "]";
    }
}