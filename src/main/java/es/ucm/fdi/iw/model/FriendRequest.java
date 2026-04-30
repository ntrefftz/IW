package es.ucm.fdi.iw.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(
    name = "IWFriendRequest",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "requester_id", "recipient_id" })
    }
)
public class FriendRequest implements Transferable<FriendRequest.Transfer> {

    public enum Status {
        PENDING,
        ACCEPTED,
        REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    @SequenceGenerator(name = "gen", sequenceName = "gen")
    private long id;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime respondedAt;

    @Getter
    @AllArgsConstructor
    public static class Transfer {
        private long id;
        private long requesterId;
        private String requesterName;
        private long recipientId;
        private String recipientName;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime respondedAt;
    }

    @Override
    public Transfer toTransfer() {
        return new Transfer(
            id,
            requester != null ? requester.getId() : -1,
            requester != null ? requester.getUsername() : "N/A",
            recipient != null ? recipient.getId() : -1,
            recipient != null ? recipient.getUsername() : "N/A",
            status != null ? status.name() : Status.PENDING.name(),
            createdAt,
            respondedAt
        );
    }

    @Override
    public String toString() {
        return "FriendRequest{" +
            "id=" + id +
            ", requester=" + (requester != null ? requester.getUsername() : "null") +
            ", recipient=" + (recipient != null ? recipient.getUsername() : "null") +
            ", status=" + status +
            ", createdAt=" + createdAt +
            ", respondedAt=" + respondedAt +
            '}';
    }
}
