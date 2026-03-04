package kh.edu.paragoniu.SpringBoot.Repos;

import kh.edu.paragoniu.SpringBoot.Model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);

    List<Book> findByTitleContainingIgnoreCase(String title);

    List<Book> findByAuthorContainingIgnoreCase(String author);

    List<Book> findByCategoryContainingIgnoreCase(String category);

    @Query("""
            SELECT b FROM Book b
            WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    List<Book> searchByTitleOrAuthor(@Param("keyword") String keyword);

    List<Book> findByAvailableCopiesGreaterThanOrderByTitleAsc(Integer availableCopies);
}
