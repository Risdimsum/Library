package kh.edu.paragoniu.SpringBoot.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "book_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BookRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Requester name is mandatory")
    @Column(nullable = false)
    private String requesterName; //

    @NotBlank(message = "Requested title is mandatory")
    @Column(nullable = false)
    private String requestedTitle; //

    private String requestedAuthor;

    @Column(columnDefinition = "TEXT")
    private String note; //

    @Column(nullable = false)
    private String status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (status == null || status.isBlank()) {
            status = "PENDING";
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now(); //
        }
    }
}
