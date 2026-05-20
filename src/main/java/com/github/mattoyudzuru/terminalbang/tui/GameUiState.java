package com.github.mattoyudzuru.terminalbang.tui;

final class GameUiState {
    private int selectedCard;
    private int selectedChoice;
    private int selectedTarget;
    private GameFocus focus = GameFocus.HAND;
    private String message = "";

    int selectedCard() {
        return selectedCard;
    }

    void setSelectedCard(int selectedCard) {
        this.selectedCard = Math.max(0, selectedCard);
    }

    int selectedChoice() {
        return selectedChoice;
    }

    void setSelectedChoice(int selectedChoice) {
        this.selectedChoice = Math.max(0, selectedChoice);
    }

    int selectedTarget() {
        return selectedTarget;
    }

    void setSelectedTarget(int selectedTarget) {
        this.selectedTarget = Math.max(0, selectedTarget);
    }

    GameFocus focus() {
        return focus;
    }

    void setFocus(GameFocus focus) {
        this.focus = focus;
    }

    String message() {
        return message;
    }

    void setMessage(String message) {
        this.message = message == null ? "" : message;
    }
}
