package es.ucm.fdi.iw.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@Table(
	name = "IWFriendship",
	uniqueConstraints = {
		@UniqueConstraint(columnNames = { "player1_id", "player2_id" }) //Creo que esta bien y no con unique en cada playerID.
	}
)
public class Friendship implements Transferable<Friendship.Transfer> {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
	@SequenceGenerator(name = "gen", sequenceName = "gen")
	private long id;

	@ManyToOne
	@JoinColumn(name = "player1_id", nullable = false)
	private User player1; // Clave foránea al Usuario (jugador 1)

	@ManyToOne
	@JoinColumn(name = "player2_id", nullable = false)
	private User player2; // Clave foránea al Usuario (jugador 2)

	private int gamesPlayed;
	private int timesBetrayed;
	private int affinityScore;

	@Getter
	@AllArgsConstructor
	public static class Transfer {
		private long id;
		private long player1Id;
		private String player1Name;
		private long player2Id;
		private String player2Name;
		private int gamesPlayed;
		private int timesBetrayed;
		private int affinityScore;
	}

	@Override
	public Transfer toTransfer() {
		return new Transfer(
			id,
			player1 != null ? player1.getId() : -1,
			player1 != null ? player1.getUsername() : "N/A",
			player2 != null ? player2.getId() : -1,
			player2 != null ? player2.getUsername() : "N/A",
			gamesPlayed,
			timesBetrayed,
			affinityScore
		);
	}

	@Override
	public String toString() { // Para logs
		return "Friendship{" +
				"id=" + id +
				", player1=" + (player1 != null ? player1.getUsername() : "null") +
				", player2=" + (player2 != null ? player2.getUsername() : "null") +
				", gamesPlayed=" + gamesPlayed +
				", timesBetrayed=" + timesBetrayed +
				", affinityScore=" + affinityScore +
				'}';
	}
}
