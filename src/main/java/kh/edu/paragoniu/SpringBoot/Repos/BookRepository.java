package kh.edu.paragoniu.SpringBoot.Repos;

import kh.edu.paragoniu.SpringBoot.Model.Book;
import kh.edu.paragoniu.SpringBoot.Model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class BookRepository {
    private final JdbcTemplate jdbcTemplate;

    public BookRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Book> mapper = (rs, rowNum) -> {
        Book book = new Book();
        book.setId(rs.getLong("id"));
        book.setIsbn(rs.getString("isbn"));
        book.setTitle(rs.getString("title"));
        book.setAuthor(rs.getString("author"));
        book.setCategory(rs.getString("category"));
        book.setPublication(rs.getString("publication"));
        book.setDetail(rs.getString("detail"));
        book.setBranch(rs.getString("branch"));
        book.setPrice(rs.getBigDecimal("price"));
        book.setTotalCopies(rs.getObject("total_copies", Integer.class));
        book.setAvailableCopies(rs.getObject("available_copies", Integer.class));
        Long createdById = rs.getObject("created_by_user_id", Long.class);
        if (createdById != null) {
            User user = new User();
            user.setId(createdById);
            book.setCreatedByUser(user);
        }
        Timestamp createdAt = rs.getTimestamp("created_at");
        book.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return book;
    };

    public Optional<Book> findByIsbn(String isbn) {
        List<Book> books = jdbcTemplate.query(
                "SELECT * FROM books WHERE isbn = ? LIMIT 1",
                mapper,
                isbn
        );
        return books.stream().findFirst();
    }

    public Optional<Book> findById(Long id) {
        List<Book> books = jdbcTemplate.query(
                "SELECT * FROM books WHERE id = ? LIMIT 1",
                mapper,
                id
        );
        return books.stream().findFirst();
    }

    public List<Book> findAll() {
        return jdbcTemplate.query("SELECT * FROM books ORDER BY id DESC", mapper);
    }

    public List<Book> searchByTitleOrAuthor(String keyword) {
        String key = "%" + keyword.toLowerCase() + "%";
        return jdbcTemplate.query(
                "SELECT * FROM books WHERE LOWER(title) LIKE ? OR LOWER(author) LIKE ? ORDER BY id DESC",
                mapper,
                key, key
        );
    }

    public List<Book> findByAvailableCopiesGreaterThanOrderByTitleAsc(Integer availableCopies) {
        return jdbcTemplate.query(
                "SELECT * FROM books WHERE available_copies > ? ORDER BY title ASC",
                mapper,
                availableCopies
        );
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM books", Long.class);
        return count == null ? 0L : count;
    }

    public Book save(Book book) {
        if (book.getId() == null) {
            book.prePersist();
            if (book.getCreatedAt() == null) {
                book.setCreatedAt(LocalDateTime.now());
            }
            Long id = jdbcTemplate.queryForObject(
                    "INSERT INTO books (id, isbn, title, author, category, publication, detail, branch, price, total_copies, available_copies, created_by_user_id, created_at) " +
                            "VALUES ((SELECT COALESCE(MAX(id), 0) + 1 FROM books), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                    Long.class,
                    book.getIsbn(),
                    book.getTitle(),
                    book.getAuthor(),
                    book.getCategory(),
                    book.getPublication(),
                    book.getDetail(),
                    book.getBranch(),
                    book.getPrice() == null ? BigDecimal.ZERO : book.getPrice(),
                    book.getTotalCopies(),
                    book.getAvailableCopies(),
                    book.getCreatedByUser() == null ? null : book.getCreatedByUser().getId(),
                    Timestamp.valueOf(book.getCreatedAt())
            );
            book.setId(id);
            return book;
        }

        jdbcTemplate.update(
                "UPDATE books SET isbn = ?, title = ?, author = ?, category = ?, publication = ?, detail = ?, branch = ?, price = ?, total_copies = ?, available_copies = ?, created_by_user_id = ? WHERE id = ?",
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.getCategory(),
                book.getPublication(),
                book.getDetail(),
                book.getBranch(),
                book.getPrice(),
                book.getTotalCopies(),
                book.getAvailableCopies(),
                book.getCreatedByUser() == null ? null : book.getCreatedByUser().getId(),
                book.getId()
        );
        return book;
    }
}
