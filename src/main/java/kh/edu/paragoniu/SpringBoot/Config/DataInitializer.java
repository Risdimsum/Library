package kh.edu.paragoniu.SpringBoot.Config;

import kh.edu.paragoniu.SpringBoot.Model.Book;
import kh.edu.paragoniu.SpringBoot.Model.Role;
import kh.edu.paragoniu.SpringBoot.Model.User;
import kh.edu.paragoniu.SpringBoot.Repos.BookRepository;
import kh.edu.paragoniu.SpringBoot.Repos.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedData(UserRepository userRepository, BookRepository bookRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                User admin = new User();
                admin.setName("Admin");
                admin.setEmail("admin@library.local");
                admin.setPassword("admin123");
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);

                User user = new User();
                user.setName("Demo User");
                user.setEmail("user@library.local");
                user.setPassword("user123");
                user.setRole(Role.USER);
                userRepository.save(user);
            }

            if (bookRepository.count() == 0) {
                Book book1 = new Book();
                book1.setIsbn("9780134685991");
                book1.setTitle("Effective Java");
                book1.setAuthor("Joshua Bloch");
                book1.setCategory("Programming");
                book1.setPublication("Addison-Wesley");
                book1.setDetail("A practical guide to writing robust Java applications.");
                book1.setBranch("BSIT");
                book1.setPrice(new BigDecimal("49.99"));
                book1.setTotalCopies(5);
                book1.setAvailableCopies(5);

                Book book2 = new Book();
                book2.setIsbn("9781617294945");
                book2.setTitle("Spring in Action");
                book2.setAuthor("Craig Walls");
                book2.setCategory("Framework");
                book2.setPublication("Manning");
                book2.setDetail("Hands-on Spring development patterns and features.");
                book2.setBranch("BSCS");
                book2.setPrice(new BigDecimal("54.90"));
                book2.setTotalCopies(4);
                book2.setAvailableCopies(4);

                bookRepository.save(book1);
                bookRepository.save(book2);
            }
        };
    }
}
