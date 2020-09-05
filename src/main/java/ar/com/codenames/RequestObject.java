package ar.com.codenames;

import ar.com.codenames.entity.Player;

import java.util.ArrayList;

public class RequestObject {

    private String excelname;
    private String nickname;
    private String message;
    private int boardSize;
    private int wordsByTeam;
    private int turnDuration;
    private int x;
    private int y;
    private int teamId;
    private Player.Role role;
    private ArrayList<String> teamColors;
    private ArrayList<String> wordsPacksSelected;
    private String sticker;

    public RequestObject() {
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getBoardSize() {
        return boardSize;
    }

    public void setBoardSize(int boardSize) {
        this.boardSize = boardSize;
    }

    public int getWordsByTeam() {
        return wordsByTeam;
    }

    public void setWordsByTeam(int wordsByTeam) {
        this.wordsByTeam = wordsByTeam;
    }

    public int getTurnDuration() {
        return turnDuration;
    }

    public void setTurnDuration(int turnDuration) {
        this.turnDuration = turnDuration;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getTeamId() {
        return teamId;
    }

    public void setTeamId(int teamId) {
        this.teamId = teamId;
    }

    public Player.Role getRole() {
        return role;
    }

    public void setRole(Player.Role role) {
        this.role = role;
    }

    public ArrayList<String> getTeamColors() {
        return teamColors;
    }

    public void setTeamColors(ArrayList<String> teamColors) {
        this.teamColors = teamColors;
    }

    public ArrayList<String> getWordsPacksSelected() {
        return wordsPacksSelected;
    }

    public void setWordsPacksSelected(ArrayList<String> wordsPacksSelected) {
        this.wordsPacksSelected = wordsPacksSelected;
    }

    public String getSticker() {
        return sticker;
    }

    public void setSticker(String sticker) {
        this.sticker = sticker;
    }
}
