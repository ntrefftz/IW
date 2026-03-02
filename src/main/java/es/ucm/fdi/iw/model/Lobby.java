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
@Table(name = "IWLobby")
public class Lobby implements Transferable<Lobby.Transfer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    @SequenceGenerator(name = "gen", sequenceName = "gen")
    private long id;

    @Column(nullable = false, unique = true)
    private String code;
    private boolean privado;
    private String password;

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

    @OneToMany(mappedBy = "lobby")
    private List<Game> games = new ArrayList<>();

    @Getter
    @AllArgsConstructor
    public static class Transfer {
        private long id;
        private String code;
        private String hostName;
        private boolean privado;
        private int playerCount;
    }

    @Override
    public Transfer toTransfer() {
        return new Transfer(
            id, 
            code, 
            host != null ? host.getUsername() : "N/A", 
            privado, 
            players.size()
        );
    }

    @Override
    public String toString() {
        return "Lobby{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", privado=" + privado +
                ", host=" + (host != null ? host.getUsername() : "null") +
                ", numJugadores=" + (players != null ? players.size() : 0) +
                ", numPartidas=" + (games != null ? games.size() : 0) +
                '}';
    }
}