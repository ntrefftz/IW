package es.ucm.fdi.iw.model.converters;

import es.ucm.fdi.iw.model.Card;
import es.ucm.fdi.iw.model.UnoState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UnoStateConverterTest {

    @Test
    void roundTripShouldPreserveUnoState() {
        UnoState state = new UnoState();
        state.setDeck(new ArrayList<>(List.of(
                new Card(Card.Color.RED, Card.Symbol.ONE, "d1"),
                new Card(Card.Color.GREEN, Card.Symbol.TWO, "d2")
        )));
        state.setDiscardPile(new ArrayList<>(List.of(
                new Card(Card.Color.BLUE, Card.Symbol.REVERSE, "x1")
        )));

        Map<Long, List<Card>> hands = new HashMap<>();
        hands.put(1L, new ArrayList<>(List.of(
                new Card(Card.Color.YELLOW, Card.Symbol.SKIP, "h1")
        )));
        hands.put(2L, new ArrayList<>(List.of(
                new Card(Card.Color.NO, Card.Symbol.CHANGE, "h2")
        )));
        state.setHands(hands);
        state.setCurrentTurnId(2L);
        state.setClockwise(false);

        UnoStateConverter converter = new UnoStateConverter();

        String json = converter.convertToDatabaseColumn(state);
        UnoState restored = converter.convertToEntityAttribute(json);

        assertNotNull(json);
        assertNotNull(restored);
        assertEquals(state.getDeck(), restored.getDeck());
        assertEquals(state.getDiscardPile(), restored.getDiscardPile());
        assertEquals(state.getHands(), restored.getHands());
        assertEquals(state.getCurrentTurnId(), restored.getCurrentTurnId());
        assertEquals(state.isClockwise(), restored.isClockwise());
    }

    @Test
    void shouldReturnNullForNullOrBlankDatabaseValue() {
        UnoStateConverter converter = new UnoStateConverter();

        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute("   "));
    }
}
