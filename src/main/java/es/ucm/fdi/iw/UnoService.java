package es.ucm.fdi.iw;

import es.ucm.fdi.iw.model.Card;
import es.ucm.fdi.iw.model.Game;
import es.ucm.fdi.iw.model.UnoState;
import es.ucm.fdi.iw.model.User;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class UnoService {

    /**
     * Inicializa una partida de UNO estándar
     */
    public void prepareGame(Game game) {
        game.initUno(); 
        UnoState state = game.getUnoState();

        List<Card> deck = generateStandardDeck();
        Collections.shuffle(deck);
        state.setDeck(deck);

        for (User player : game.getPlayers()) {
            List<Card> hand = state.getHands().get(player.getId());
            for (int i = 0; i < 7; i++) {
                if (!state.getDeck().isEmpty()) {
                    hand.add(state.getDeck().remove(0));
                }
            }
        }

        state.getDiscardPile().add(state.getDeck().remove(0));

        state.setCurrentTurnId(game.getHost().getId());
    }

    private List<Card> generateStandardDeck() {
        List<Card> deck = new ArrayList<>();
        
        for (Card.Color c : Card.Color.values()) {
            if (c == Card.Color.NO) continue;

            for (Card.Symbol s : Card.Symbol.values()) {
                if (s == Card.Symbol.CHANGE || s == Card.Symbol.DRAW_FOUR) continue;
                
                // El 0 es único, del 1 al 9 y especiales (Skip, Rev, +2) hay dos de cada
                deck.add(new Card(c, s, UUID.randomUUID().toString()));
                if (s != Card.Symbol.ZERO) {
                    deck.add(new Card(c, s, UUID.randomUUID().toString()));
                }
            }
        }
        
        // Añadimos 4 Comodines y 4 Comodines +4
        for (int i = 0; i < 4; i++) {
            deck.add(new Card(Card.Color.NO, Card.Symbol.CHANGE, UUID.randomUUID().toString()));
            deck.add(new Card(Card.Color.NO, Card.Symbol.DRAW_FOUR, UUID.randomUUID().toString()));
        }
        
        return deck;
    }


public ObjectNode generatePlayerView(Game game, User viewer, ObjectMapper mapper) { // !!!!
    UnoState state = game.getUnoState();
    ObjectNode root = mapper.createObjectNode();

    // 1. Información general
    root.put("type", "GAME_STATE_UPDATE");
    root.put("currentTurnId", state.getCurrentTurnId());
    root.put("clockwise", state.isClockwise());

    // 2. La carta en la mesa (solo la última del montón de descartes)
    Card topCard = state.getDiscardPile().get(state.getDiscardPile().size() - 1);
    root.set("topCard", mapper.valueToTree(topCard));

    // 3. TUS cartas (detalladas)
    ArrayNode yourHand = root.putArray("yourHand");
    List<Card> myCards = state.getHands().get(viewer.getId());
    for (Card c : myCards) {
        yourHand.add(mapper.valueToTree(c));
    }

    // 4. Los RIVALES (solo cuántas cartas tienen)
    ObjectNode opponents = root.putObject("opponents");
    state.getHands().forEach((userId, cards) -> {
        if (userId != viewer.getId()) {
            // Buscamos el nombre del usuario para que sea más fácil de leer en el JS
            String username = game.getPlayers().stream()
                .filter(u -> u.getId() == userId)
                .findFirst().map(User::getUsername).orElse("Desconocido");
            opponents.put(username, cards.size());
        }
    });

    return root;
}
}
