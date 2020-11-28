package ar.com.codenames.entity;

import com.corundumstudio.socketio.SocketIOClient;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

public class Player implements Serializable, Comparable<Player> {

    private String id;
    private String excelname;
    private String nickname;
    private Integer teamId;
    private Role role;
    private String roomName;

    @Override
    public int compareTo(Player o) {
        return this.excelname.compareTo(o.excelname);
    }

    public enum Role {
        GUESSER, SPYMASTER
    }

    public Player(SocketIOClient client, String excelname, String nickname) {
        this(client, excelname, nickname, null, null);
    }

    public Player(SocketIOClient client, String excelname, String nickname, String roomName, Map<String, Player> PLAYER_LIST) {
        this.id = client.getSessionId().toString();
        this.excelname = excelname;
        this.nickname = nickname;
        this.roomName = roomName;
        this.role = Role.GUESSER;

        // Add player to player list and add their socket to the socket list
        if (PLAYER_LIST != null) PLAYER_LIST.put(this.id, this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getExcelname() {
        return excelname;
    }

    public void setExcelname(String excelname) {
        this.excelname = excelname;
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

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    @Override
    public String toString() {
        return "Player ["
                + ((id != null) ? "id=" + id + ", " : "")
                + ((excelname != null) ? "excelname=" + excelname + ", " : "")
                + ((nickname != null) ? "nickname=" + nickname + ", " : "")
                + ((teamId != null) ? "teamId=" + teamId + ", " : "")
                + ((role != null) ? "role=" + role + ", " : "")
                + ((roomName != null) ? "room=" + roomName : "")
                + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(excelname, player.excelname);
    }

    @Override
    public int hashCode() {
        return Objects.hash(excelname);
    }
}