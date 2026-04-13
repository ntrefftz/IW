package es.ucm.fdi.iw.model;

import lombok.Data;

@Data
public class UnoActionRequest {
    public enum ActionType {
        PLAY_CARD,
        DRAW_CARD,
        PASS
    }

    private ActionType actionType;
    private String cardId;
    private String chosenColor;
}
