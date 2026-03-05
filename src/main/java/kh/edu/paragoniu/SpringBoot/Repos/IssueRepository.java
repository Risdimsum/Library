package kh.edu.paragoniu.SpringBoot.Repos;

import kh.edu.paragoniu.SpringBoot.Model.Book;
import kh.edu.paragoniu.SpringBoot.Model.Issue;
import kh.edu.paragoniu.SpringBoot.Model.IssueStatus;
import kh.edu.paragoniu.SpringBoot.Model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

@Repository
public class IssueRepository {
    private final JdbcTemplate jdbcTemplate;

    public IssueRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Issue> mapper = (rs, rowNum) -> {
        Issue issue = new Issue();
        issue.setId(rs.getLong("id"));

        User user = new User();
        user.setId(rs.getLong("user_id"));
        user.setName(rs.getString("user_name"));
        user.setEmail(rs.getString("user_email"));
        issue.setUser(user);

        Book book = new Book();
        book.setId(rs.getLong("book_id"));
        book.setTitle(rs.getString("book_title"));
        book.setIsbn(rs.getString("book_isbn"));
        issue.setBook(book);

        Long issuedById = rs.getObject("issued_by_user_id", Long.class);
        if (issuedById != null) {
            User issuedBy = new User();
            issuedBy.setId(issuedById);
            issuedBy.setName(rs.getString("issued_by_name"));
            issue.setIssuedByUser(issuedBy);
        }

        Date issueDate = rs.getDate("issue_date");
        Date dueDate = rs.getDate("due_date");
        Date returnDate = rs.getDate("return_date");
        issue.setIssueDate(issueDate == null ? null : issueDate.toLocalDate());
        issue.setDueDate(dueDate == null ? null : dueDate.toLocalDate());
        issue.setReturnDate(returnDate == null ? null : returnDate.toLocalDate());
        issue.setStatus(IssueStatus.valueOf(rs.getString("status")));
        return issue;
    };

    private static final String BASE_SELECT = """
            SELECT i.id, i.user_id, i.book_id, i.issued_by_user_id, i.issue_date, i.due_date, i.return_date, i.status,
                   u.name AS user_name, u.email AS user_email,
                   b.title AS book_title, b.isbn AS book_isbn,
                   iu.name AS issued_by_name
            FROM book_issues i
            JOIN users u ON u.id = i.user_id
            JOIN books b ON b.id = i.book_id
            LEFT JOIN users iu ON iu.id = i.issued_by_user_id
            """;

    public Issue save(Issue issue) {
        if (issue.getId() == null) {
            issue.prePersist();
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO book_issues (user_id, book_id, issued_by_user_id, issue_date, due_date, return_date, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setLong(1, issue.getUser().getId());
                ps.setLong(2, issue.getBook().getId());
                ps.setObject(3, issue.getIssuedByUser() == null ? null : issue.getIssuedByUser().getId());
                ps.setDate(4, Date.valueOf(issue.getIssueDate()));
                ps.setDate(5, Date.valueOf(issue.getDueDate()));
                ps.setObject(6, issue.getReturnDate() == null ? null : Date.valueOf(issue.getReturnDate()));
                ps.setString(7, issue.getStatus().name());
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key != null) {
                issue.setId(key.longValue());
            }
            return issue;
        }

        jdbcTemplate.update(
                "UPDATE book_issues SET user_id = ?, book_id = ?, issued_by_user_id = ?, issue_date = ?, due_date = ?, return_date = ?, status = ? WHERE id = ?",
                issue.getUser().getId(),
                issue.getBook().getId(),
                issue.getIssuedByUser() == null ? null : issue.getIssuedByUser().getId(),
                Date.valueOf(issue.getIssueDate()),
                Date.valueOf(issue.getDueDate()),
                issue.getReturnDate() == null ? null : Date.valueOf(issue.getReturnDate()),
                issue.getStatus().name(),
                issue.getId()
        );
        return issue;
    }

    public List<Issue> findByStatus(IssueStatus status) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE i.status = ? ORDER BY i.issue_date DESC, i.id DESC",
                mapper,
                status.name()
        );
    }

    public List<Issue> findAllByOrderByIssueDateDescIdDesc() {
        return jdbcTemplate.query(
                BASE_SELECT + " ORDER BY i.issue_date DESC, i.id DESC",
                mapper
        );
    }

    public List<Issue> findByUserIdOrderByIssueDateDescIdDesc(Long userId) {
        return jdbcTemplate.query(
                BASE_SELECT + " WHERE i.user_id = ? ORDER BY i.issue_date DESC, i.id DESC",
                mapper,
                userId
        );
    }
}
