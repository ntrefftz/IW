package es.ucm.fdi.iw.model;

import lombok.Data;
import java.util.*;

@Data
/*
* Estado interno de la partida
*/
public class UnoState {
    private List<Card> deck;         
    private List<Card> discardPile;       
    private Map<Long, List<Card>> hands;
    private long currentTurnId; 
    private boolean clockwise;            
    
    public UnoState(List<User> players) {
        this.deck = new ArrayList<>();
        this.discardPile = new ArrayList<>();
        this.hands = new HashMap<>();
        this.clockwise = true;
        
        for (User u : players) {
            hands.put(u.getId(), new ArrayList<>());
        }
    }
}