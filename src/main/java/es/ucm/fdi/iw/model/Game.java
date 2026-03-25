package es.ucm.fdi.iw.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "IWGame")
public class Game implements Transferable<Game.Transfer> {

    public enum Estado {
        LOBBY,
        PARTIDA,
        TERMINADA,
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    @SequenceGenerator(name = "gen", sequenceName = "gen")
    private long id;

    @Column(nullable = false, unique = true)
    private String code;
    private String modalidad = "UNO";
    private boolean privado;
    private String password;
    private Estado estado;

    @ManyToOne
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @ManyToMany
    @JoinTable(
        name = "lobby_players",
        joinColumns = @JoinColumn(name = "lobby_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    
    private List<User> players = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String estadoFinal; 

    private int mensaje;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Transfer {
        private long id;
        private String estadoFinal;
        private String code;
        private String winnerName;
    }

    @Override
    public Transfer toTransfer() {
        return new Transfer(
            id, 
            estadoFinal, 
            code, 
            winner != null ? winner.getUsername() : "N/A"
        );
    }

    @Override
    public String toString() {
        return "Game{" +
                "id=" + id +
                ", code=" + code +
                ", winner=" + (winner != null ? winner.getUsername() : "none") +
                ", estadoResumido='" + (estadoFinal != null && estadoFinal.length() > 50 ? 
                                        estadoFinal.substring(0, 47) + "..." : estadoFinal) + '\'' +
                '}';
    }

    public int getPlayerCount() {
        return this.players != null ? this.players.size() : 0;
    }

    public String getGameType() {
        return modalidad != null ? modalidad : "UNO";
    }
}