package kh.edu.paragoniu.SpringBoot.Repos;

import kh.edu.paragoniu.SpringBoot.Model.BookRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class BookRequestRepository {
    private final JdbcTemplate jdbcTemplate;

    public BookRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<BookRequest> mapper = (rs, rowNum) -> {
        BookRequest request = new BookRequest();
        request.setId(rs.getLong("id"));
        request.setRequesterName(rs.getString("requester_name"));
        request.setRequestedTitle(rs.getString("requested_title"));
        request.setRequestedAuthor(rs.getString("requested_author"));
        request.setNote(rs.getString("note"));
        request.setStatus(rs.getString("status"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        request.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return request;
    };

    public List<BookRequest> findAllByOrderByCreatedAtDesc() {
        return jdbcTemplate.query("SELECT * FROM book_requests ORDER BY created_at DESC", mapper);
    }

    public BookRequest save(BookRequest request) {
        if (request.getId() == null) {
            request.prePersist();
            if (request.getCreatedAt() == null) {
                request.setCreatedAt(LocalDateTime.now());
            }
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO book_requests (requester_name, requested_title, requested_author, note, status, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, request.getRequesterName());
                ps.setString(2, request.getRequestedTitle());
                ps.setString(3, request.getRequestedAuthor());
                ps.setString(4, request.getNote());
                ps.setString(5, request.getStatus());
                ps.setTimestamp(6, Timestamp.valueOf(request.getCreatedAt()));
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key != null) {
                request.setId(key.longValue());
            }
            return request;
        }

        jdbcTemplate.update(
                "UPDATE book_requests SET requester_name = ?, requested_title = ?, requested_author = ?, note = ?, status = ? WHERE id = ?",
                request.getRequesterName(),
                request.getRequestedTitle(),
                request.getRequestedAuthor(),
                request.getNote(),
                request.getStatus(),
                request.getId()
        );
        return request;
    }
}
