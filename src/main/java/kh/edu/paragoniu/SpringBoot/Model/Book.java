package kh.edu.paragoniu.SpringBoot.Model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String isbn;

    @NotBlank(message = "Title is mandatory")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "Author is mandatory")
    @Column(nullable = false)
    private String author;

    @Column(nullable = false)
    private String category;

    private String publication;

    @Column(columnDefinition = "TEXT")
    private String detail;

    private String branch;

    @PositiveOrZero(message = "Price must be 0 or greater")
    private BigDecimal price;

    @Min(value = 1, message = "Total copies must be at least 1")
    @Column(nullable = false)
    private Integer totalCopies;

    @Min(value = 0, message = "Available copies cannot be negative")
    @Column(nullable = false)
    private Integer availableCopies;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (isbn == null || isbn.isBlank()) {
            isbn = "AUTO-" + System.currentTimeMillis();
        }
        if (category == null || category.isBlank()) {
            category = "General";
        }
        if (availableCopies == null) {
            availableCopies = totalCopies;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
    public boolean isAvailable() {
        return availableCopies != null && availableCopies > 0;
    }

    public int getRentedCount() {
        if (totalCopies == null || availableCopies == null) return 0;
        return totalCopies - availableCopies;
    }
}
