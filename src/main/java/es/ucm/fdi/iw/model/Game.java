package es.ucm.fdi.iw.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import es.ucm.fdi.iw.model.Transferable;
import es.ucm.fdi.iw.model.User;
import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(name = "IWGame")
public class Game implements Transferable<Game.Transfer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    @SequenceGenerator(name = "gen", sequenceName = "gen")
    private long id;

    @Column(columnDefinition = "TEXT") // Esto lo va ha ser JSON
    private String estadoFinal; 

    @ManyToOne
    @JoinColumn(name = "lobby_id", nullable = false)
    private Lobby lobby; // Clave foránea al Lobby (obligatoria)

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner; // Clave foránea al Usuario ganador (opcional si es empate)

    @Getter
    @AllArgsConstructor
    public static class Transfer {
        private long id;
        private String estadoFinal;
        private long lobbyId;
        private String winnerName;
    }

    @Override
    public Transfer toTransfer() {
        return new Transfer(
            id, 
            estadoFinal, 
            lobby != null ? lobby.getId() : -1, 
            winner != null ? winner.getUsername() : "N/A"
        );
    }

    @Override
    public String toString() { // Para logs
        return "Game{" +
                "id=" + id +
                ", lobbyId=" + (lobby != null ? lobby.getId() : "null") +
                ", winner=" + (winner != null ? winner.getUsername() : "none") +
                ", estadoResumido='" + (estadoFinal != null && estadoFinal.length() > 50 ? 
                                        estadoFinal.substring(0, 47) + "..." : estadoFinal) + '\'' +
                '}';
    }
}