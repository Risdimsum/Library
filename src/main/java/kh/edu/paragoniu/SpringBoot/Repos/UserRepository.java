package kh.edu.paragoniu.SpringBoot.Repos;

import kh.edu.paragoniu.SpringBoot.Model.Role;
import kh.edu.paragoniu.SpringBoot.Model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> mapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        String role = rs.getString("role");
        user.setRole(role == null ? null : Role.valueOf(role));
        user.setActive(rs.getObject("active", Boolean.class));
        Timestamp createdAt = rs.getTimestamp("created_at");
        user.setCreatedAt(createdAt == null ? null : createdAt.toLocalDateTime());
        return user;
    };

    public Optional<User> findByEmail(String email) {
        List<User> users = jdbcTemplate.query(
                "SELECT * FROM users WHERE email = ? LIMIT 1",
                mapper,
                email
        );
        return users.stream().findFirst();
    }

    public List<User> findByRoleOrderByCreatedAtDesc(Role role) {
        return jdbcTemplate.query(
                "SELECT * FROM users WHERE role = ? ORDER BY created_at DESC",
                mapper,
                role.name()
        );
    }

    public Optional<User> findById(Long id) {
        List<User> users = jdbcTemplate.query(
                "SELECT * FROM users WHERE id = ? LIMIT 1",
                mapper,
                id
        );
        return users.stream().findFirst();
    }

    public List<User> findAll() {
        return jdbcTemplate.query("SELECT * FROM users ORDER BY id", mapper);
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return count == null ? 0L : count;
    }

    public User save(User user) {
        if (user.getId() == null) {
            user.prePersist();
            if (user.getCreatedAt() == null) {
                user.setCreatedAt(LocalDateTime.now());
            }
            Long id = jdbcTemplate.queryForObject(
                    "INSERT INTO users (id, name, email, password, role, active, created_at) " +
                            "VALUES ((SELECT COALESCE(MAX(id), 0) + 1 FROM users), ?, ?, ?, ?, ?, ?) RETURNING id",
                    Long.class,
                    user.getName(),
                    user.getEmail(),
                    user.getPassword(),
                    user.getRole() == null ? null : user.getRole().name(),
                    user.getActive(),
                    Timestamp.valueOf(user.getCreatedAt())
            );
            user.setId(id);
            return user;
        }

        jdbcTemplate.update(
                "UPDATE users SET name = ?, email = ?, password = ?, role = ?, active = ? WHERE id = ?",
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole() == null ? null : user.getRole().name(),
                user.getActive(),
                user.getId()
        );
        return user;
    }

    public void delete(User user) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", user.getId());
    }
}
