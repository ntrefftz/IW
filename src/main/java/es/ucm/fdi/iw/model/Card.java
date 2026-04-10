package es.ucm.fdi.iw.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Carta de la baraja del UNO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Card {

    public enum Color {
        RED, GREEN, BLUE, YELLOW, NO
    }

    public enum Symbol {
        ZERO, ONE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, 
        SKIP, REVERSE, DRAW_TWO, CHANGE, DRAW_FOUR
    }

    private Color color;
    private Symbol symbol;
    
    private String id; 

    @Override
    public String toString() {
        return color + "_" + symbol;
    }
}