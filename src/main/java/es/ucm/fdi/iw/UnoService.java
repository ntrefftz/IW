package es.ucm.fdi.iw;

import es.ucm.fdi.iw.model.Card;
import es.ucm.fdi.iw.model.Friendship;
import es.ucm.fdi.iw.model.Game;
import es.ucm.fdi.iw.model.UnoActionRequest;
import es.ucm.fdi.iw.model.UnoState;
import es.ucm.fdi.iw.model.User;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UnoService {

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * Inicializa una partida de UNO estándar
     */
    @Transactional
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
        gameRepository.save(game);
    }

    public User resolvePlayerInGame(Game game, User sessionUser) {
        if (game == null || sessionUser == null) {
            return null;
        }

        return game.getPlayers().stream().filter(p -> sameUser(p, sessionUser)).findFirst().orElse(null);
    }

    @Transactional
    public void applyAction(Game game, User actor, UnoActionRequest req) {
        UnoState state = requireState(game);
        User realActor = resolvePlayerInGame(game, actor);

        if (realActor == null) {
            throw new LobbyException("El usuario no pertenece a esta partida");
        }

        if (game.getEstado() != Game.Estado.PARTIDA) {
            throw new LobbyException("La partida no esta activa");
        }

        if (state.getCurrentTurnId() != realActor.getId()) {
            throw new LobbyException("No es tu turno");
        }

        if (req == null || req.getActionType() == null) {
            throw new LobbyException("Accion no valida");
        }

        switch (req.getActionType()) {
            case DRAW_CARD -> applyDraw(game, state, realActor);
            case PLAY_CARD -> applyPlayCard(game, state, realActor, req);
            case PASS -> advanceTurn(game, state, realActor.getId(), 1);
            default -> throw new LobbyException("Accion no soportada");
        }

        gameRepository.save(game);
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


    public ObjectNode generatePlayerView(Game game, User viewer, ObjectMapper mapper) {
        UnoState state = requireState(game);
        User realViewer = resolvePlayerInGame(game, viewer);

        if (realViewer == null) {
            throw new LobbyException("El usuario no pertenece a esta partida");
        }

        ObjectNode root = mapper.createObjectNode();
        root.put("type", "GAME_STATE_UPDATE");
        root.put("code", game.getCode());
        root.put("gameStatus", game.getEstado().name());
        root.put("currentTurnId", state.getCurrentTurnId());
        root.put("clockwise", state.isClockwise());
        root.put("currentTurnUsername", resolveUsernameById(game.getPlayers(), state.getCurrentTurnId()));

        User winner = game.getWinner();
        root.put("winnerUsername", winner != null && winner.getUsername() != null ? winner.getUsername() : "");

        Card topCard = state.getDiscardPile().get(state.getDiscardPile().size() - 1);
        String topCode = cardCode(topCard);
        root.set("topCard", mapper.valueToTree(topCard));
        root.put("topCardCode", topCode);
        root.put("drawPileCount", state.getDeck().size());

        List<User> ordered = game.getPlayers().stream().filter(p -> !sameUser(p, realViewer)).collect(Collectors.toList());
        List<User> personalOrder = new ArrayList<>();
        personalOrder.add(realViewer);
        personalOrder.addAll(ordered);

        long currentTurnId = state.getCurrentTurnId();
        int turnIndex = 0;
        for (int i = 0; i < personalOrder.size(); i++) {
            if (personalOrder.get(i).getId() == currentTurnId) {
                turnIndex = i;
                break;
            }
        }

        root.put("turno", turnIndex);
        root.put("sentido", state.isClockwise() ? 1 : -1);

        ArrayNode yourHand = root.putArray("yourHand");
        List<Card> myCards = state.getHands().get(realViewer.getId());
        for (Card c : myCards) {
            ObjectNode cardNode = mapper.createObjectNode();
            cardNode.put("id", c.getId());
            cardNode.put("code", cardCode(c));
            yourHand.add(cardNode);
        }

        ObjectNode opponents = root.putObject("opponents");
        ArrayNode opponentCounts = root.putArray("opponentCounts");
        for (User p : ordered) {
            int count = state.getHands().get(p.getId()).size();
            opponents.put(p.getUsername(), count);
            opponentCounts.add(count);
        }

        ArrayNode mazos = root.putArray("mazos");
        ArrayNode myHandCodes = mapper.createArrayNode();
        for (Card c : myCards) {
            myHandCodes.add(cardCode(c));
        }
        mazos.add(myHandCodes);
        for (User p : ordered) {
            ArrayNode hidden = mapper.createArrayNode();
            int count = state.getHands().get(p.getId()).size();
            for (int i = 0; i < count; i++) {
                hidden.add("XX");
            }
            mazos.add(hidden);
        }

        ArrayNode jugadas = root.putArray("jugadas");
        jugadas.add(topCode);

        ArrayNode robo = root.putArray("robo");
        for (int i = 0; i < state.getDeck().size(); i++) {
            robo.add("D");
        }

        return root;
    }

    @Transactional
    public void resetToLobbyAfterGame(Game game) {
        if (game == null) {
            return;
        }

        game.setEstado(Game.Estado.LOBBY);
        game.setWinner(null);
        game.setUnoState(null);
        gameRepository.save(game);
    }

    private UnoState requireState(Game game) {
        if (game.getUnoState() == null) {
            throw new LobbyException("La partida UNO no esta inicializada");
        }
        return game.getUnoState();
    }

    private void applyDraw(Game game, UnoState state, User actor) {
        List<Card> hand = state.getHands().get(actor.getId());
        hand.add(drawFromDeck(state));
        advanceTurn(game, state, actor.getId(), 1);
    }

    private void applyPlayCard(Game game, UnoState state, User actor, UnoActionRequest req) {
        List<Card> hand = state.getHands().get(actor.getId());
        Card topCard = state.getDiscardPile().get(state.getDiscardPile().size() - 1);
        Card played = hand.stream().filter(c -> c.getId().equals(req.getCardId())).findFirst()
                .orElseThrow(() -> new LobbyException("No tienes esa carta"));

        if (!isPlayable(played, topCard)) {
            throw new LobbyException("La carta no es jugable");
        }

        hand.remove(played);

        if (played.getSymbol() == Card.Symbol.DRAW_TWO) {
            actor.setDrawTwoPlayed(actor.getDrawTwoPlayed() + 1);
        }
        if (played.getSymbol() == Card.Symbol.DRAW_FOUR) {
            actor.setDrawFourPlayed(actor.getDrawFourPlayed() + 1);
        }
        if (hand.size() == 1) {
            actor.setOneCardMoments(actor.getOneCardMoments() + 1);
        }

        Card toDiscard = played;
        if (played.getSymbol() == Card.Symbol.CHANGE || played.getSymbol() == Card.Symbol.DRAW_FOUR) {
            Card.Color chosen = parseChosenColor(req.getChosenColor());
            if (chosen == Card.Color.NO) {
                throw new LobbyException("Debes elegir un color para el comodin");
            }
            toDiscard = new Card(chosen, played.getSymbol(), played.getId());
        }

        state.getDiscardPile().add(toDiscard);

        if (hand.isEmpty()) {
            game.setWinner(actor);
            game.setEstado(Game.Estado.TERMINADA);
            return;
        }

        int steps = 1;
        if (played.getSymbol() == Card.Symbol.REVERSE) {
            state.setClockwise(!state.isClockwise());
        }
        if (played.getSymbol() == Card.Symbol.SKIP) {
            steps = 2;
        }
        if (played.getSymbol() == Card.Symbol.DRAW_TWO || played.getSymbol() == Card.Symbol.DRAW_FOUR) {
            int n = played.getSymbol() == Card.Symbol.DRAW_TWO ? 2 : 4;
            long targetId = nextPlayerId(game, state, actor.getId(), 1);
            List<Card> targetHand = state.getHands().get(targetId);
            for (int i = 0; i < n; i++) {
                targetHand.add(drawFromDeck(state));
            }
            User targetUser = entityManager.find(User.class, targetId);
            if (targetUser != null) {
                if (played.getSymbol() == Card.Symbol.DRAW_TWO) {
                    targetUser.setDrawTwoDrawn(targetUser.getDrawTwoDrawn() + 1);
                } else {
                    targetUser.setDrawFourDrawn(targetUser.getDrawFourDrawn() + 1);
                }
                incrementFriendshipBetrayal(actor, targetUser);
            }
            steps = 2;
        }

        advanceTurn(game, state, actor.getId(), steps);
    }

    private Card drawFromDeck(UnoState state) {
        if (state.getDeck().isEmpty()) {
            if (state.getDiscardPile().size() <= 1) {
                throw new LobbyException("No hay cartas para robar");
            }
            Card top = state.getDiscardPile().remove(state.getDiscardPile().size() - 1);
            List<Card> reshuffle = new ArrayList<>(state.getDiscardPile());
            Collections.shuffle(reshuffle);
            state.setDeck(reshuffle);
            state.getDiscardPile().clear();
            state.getDiscardPile().add(top);
        }
        return state.getDeck().remove(0);
    }

    private void advanceTurn(Game game, UnoState state, long fromPlayerId, int steps) {
        long nextId = nextPlayerId(game, state, fromPlayerId, steps);
        state.setCurrentTurnId(nextId);
    }

    private long nextPlayerId(Game game, UnoState state, long fromPlayerId, int steps) {
        List<User> players = game.getPlayers();
        if (players.isEmpty()) {
            throw new LobbyException("Partida sin jugadores");
        }

        int idx = 0;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getId() == fromPlayerId) {
                idx = i;
                break;
            }
        }

        int direction = state.isClockwise() ? 1 : -1;
        int n = players.size();
        int next = idx;
        for (int i = 0; i < steps; i++) {
            next = (next + direction + n) % n;
        }
        return players.get(next).getId();
    }

    private boolean isPlayable(Card card, Card topCard) {
        if (card.getColor() == Card.Color.NO) {
            return true;
        }
        return card.getColor() == topCard.getColor() || card.getSymbol() == topCard.getSymbol();
    }

    private Card.Color parseChosenColor(String chosenColor) {
        if (chosenColor == null) {
            return Card.Color.NO;
        }
        return switch (chosenColor.trim().toUpperCase()) {
            case "R", "RED" -> Card.Color.RED;
            case "V", "GREEN" -> Card.Color.GREEN;
            case "A", "BLUE" -> Card.Color.BLUE;
            case "Y", "YELLOW" -> Card.Color.YELLOW;
            default -> Card.Color.NO;
        };
    }

    private String cardCode(Card card) {
        String prefix = switch (card.getColor()) {
            case RED -> "R";
            case GREEN -> "V";
            case BLUE -> "A";
            case YELLOW -> "Y";
            case NO -> "W";
        };

        String value = switch (card.getSymbol()) {
            case ZERO -> "0";
            case ONE -> "1";
            case TWO -> "2";
            case THREE -> "3";
            case FOUR -> "4";
            case FIVE -> "5";
            case SIX -> "6";
            case SEVEN -> "7";
            case EIGHT -> "8";
            case NINE -> "9";
            case SKIP -> "SKIP";
            case REVERSE -> "REV";
            case DRAW_TWO -> "+2";
            case CHANGE -> "C";
            case DRAW_FOUR -> "+4";
        };

        return prefix + value;
    }

    private boolean sameUser(User a, User b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getId() > 0 && b.getId() > 0) {
            return a.getId() == b.getId();
        }
        return a.getUsername() != null && a.getUsername().equals(b.getUsername());
    }

    private void incrementFriendshipBetrayal(User a, User b) {
        Friendship friendship = entityManager.createQuery(
                "SELECT f FROM Friendship f WHERE (f.player1 = :a AND f.player2 = :b) "
                        + "OR (f.player1 = :b AND f.player2 = :a)",
                Friendship.class)
                .setParameter("a", a)
                .setParameter("b", b)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(null);

        if (friendship != null) {
            friendship.setTimesBetrayed(friendship.getTimesBetrayed() + 1);
        }
    }

    private String resolveUsernameById(List<User> players, long playerId) {
        for (User player : players) {
            if (player.getId() == playerId) {
                return player.getUsername() != null ? player.getUsername() : "";
            }
        }
        return "";
    }
}
