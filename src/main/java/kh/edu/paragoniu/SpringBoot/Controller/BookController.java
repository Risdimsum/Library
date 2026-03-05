package kh.edu.paragoniu.SpringBoot.Controller;

import jakarta.validation.Valid;
import kh.edu.paragoniu.SpringBoot.Model.Book;
import kh.edu.paragoniu.SpringBoot.Model.BookRequest;
import kh.edu.paragoniu.SpringBoot.Model.Role;
import kh.edu.paragoniu.SpringBoot.Model.User;
import kh.edu.paragoniu.SpringBoot.Repos.BookRepository;
import kh.edu.paragoniu.SpringBoot.Repos.BookRequestRepository;
import kh.edu.paragoniu.SpringBoot.Repos.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

@Controller
public class BookController {
    private static final String SESSION_USER_ID = "currentUserId";
    private static final String SESSION_USER_NAME = "currentUserName";

    private final BookRepository bookRepository;
    private final BookRequestRepository bookRequestRepository;
    private final UserRepository userRepository;

    public BookController(BookRepository bookRepository,
                          BookRequestRepository bookRequestRepository,
                          UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.bookRequestRepository = bookRequestRepository;
        this.userRepository = userRepository;
    }

    @ModelAttribute("book")
    public Book bookModel() {
        return new Book();
    }

    @GetMapping({"/", "/login"})
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            HttpSession session,
                            Model model) {
        if (resolveSessionUser(session) != null) {
            return "redirect:/students/add";
        }
        model.addAttribute("error", error != null);
        return "Login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam(value = "name", required = false) String name,
                              @RequestParam("username") String username,
                              @RequestParam("password") String password,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        // Non-strict login with auto student provisioning from input.
        String inputName = name == null ? "" : name.trim();
        String email = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
        if (email.isBlank()) {
            email = "guest@library.local";
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            String generatedName = inputName;
            if (generatedName.isBlank()) {
                generatedName = email.contains("@")
                        ? email.substring(0, email.indexOf('@')).replace('.', ' ').replace('_', ' ')
                        : "Student";
            }
            if (generatedName.isBlank()) {
                generatedName = "Student";
            }
            user = new User();
            user.setName(generatedName);
            user.setEmail(email);
            user.setPassword((password == null || password.isBlank()) ? "changeme123" : password);
            user.setRole(Role.USER);
            user.setActive(true);
            user = userRepository.save(user);
        } else if (!inputName.isBlank() && !inputName.equals(user.getName())) {
            user.setName(inputName);
            user = userRepository.save(user);
        }
        session.setAttribute(SESSION_USER_ID, user.getId());
        session.setAttribute(SESSION_USER_NAME, user.getName());
        return "redirect:/issues/new";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @ModelAttribute("currentUserName")
    public String currentUserName(HttpSession session) {
        Object value = session.getAttribute(SESSION_USER_NAME);
        return value == null ? "Guest" : value.toString();
    }

    @GetMapping("/books")
    public String listBooks(@RequestParam(value = "keyword", required = false) String keyword,
                            Model model,
                            HttpSession session) {
        if (resolveSessionUser(session) == null) {
            return "redirect:/login";
        }
        List<Book> books;
        if (keyword == null || keyword.isBlank()) {
            books = bookRepository.findAll();
        } else {
            books = bookRepository.searchByTitleOrAuthor(keyword.trim());
        }
        model.addAttribute("books", books);
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("activePage", "book-record");
        return "List";
    }

    @GetMapping("/books/add")
    public String showAddForm(Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        if (resolveSessionUser(session) == null) {
            return "redirect:/login";
        }
        if (userRepository.findByRoleOrderByCreatedAtDesc(Role.USER).isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Step 1: add a student before adding books.");
            return "redirect:/students/add";
        }
        if (!model.containsAttribute("book")) {
            model.addAttribute("book", new Book());
        }
        model.addAttribute("activePage", "add-book");
        return "Form";
    }

    @PostMapping("/books/add")
    public String addBook(@Valid Book book,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirectAttributes,
                          HttpSession session) {
        User currentUser = resolveSessionUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            model.addAttribute("book", book);
            model.addAttribute("activePage", "add-book");
            return "Form";
        }
        applySimpleBookDefaults(book, currentUser);
        bookRepository.save(book);
        redirectAttributes.addFlashAttribute("successMessage", "Book added successfully.");
        return "redirect:/books";
    }

    @GetMapping("/books/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model, HttpSession session) {
        if (resolveSessionUser(session) == null) {
            return "redirect:/login";
        }
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid book ID: " + id));
        model.addAttribute("book", book);
        model.addAttribute("activePage", "book-record");
        return "Form";
    }

    @PostMapping("/books/edit/{id}")
    public String updateBook(@PathVariable("id") Long id, @Valid Book book, BindingResult result, Model model,
                             RedirectAttributes redirectAttributes, HttpSession session) {
        User currentUser = resolveSessionUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            book.setId(id);
            model.addAttribute("activePage", "book-record");
            return "Form";
        }
        Book existingBook = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid book ID: " + id));
        existingBook.setTitle(book.getTitle());
        existingBook.setAuthor(book.getAuthor());
        existingBook.setTotalCopies(book.getTotalCopies());
        if (existingBook.getCreatedByUser() == null) {
            existingBook.setCreatedByUser(currentUser);
        }
        if (existingBook.getAvailableCopies() == null) {
            existingBook.setAvailableCopies(book.getTotalCopies());
        } else if (existingBook.getAvailableCopies() > book.getTotalCopies()) {
            existingBook.setAvailableCopies(book.getTotalCopies());
        }
        bookRepository.save(existingBook);
        redirectAttributes.addFlashAttribute("successMessage", "Book updated successfully.");
        return "redirect:/books";
    }

    private void applySimpleBookDefaults(Book book, User currentUser) {
        book.setCreatedByUser(currentUser);
        if (book.getIsbn() == null || book.getIsbn().isBlank()) {
            book.setIsbn("AUTO-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(100, 999));
        }
        if (book.getCategory() == null || book.getCategory().isBlank()) {
            book.setCategory("General");
        }
        if (book.getTotalCopies() != null && (book.getAvailableCopies() == null || book.getAvailableCopies() < 0)) {
            book.setAvailableCopies(book.getTotalCopies());
        }
    }

    @GetMapping("/books/view/{id}")
    public String viewBook(@PathVariable("id") Long id, HttpSession session) {
        if (resolveSessionUser(session) == null) {
            return "redirect:/login";
        }
        return "redirect:/books/edit/" + id;
    }

    @GetMapping("/books/requests")
    public String showBookRequests(Model model, HttpSession session) {
        if (resolveSessionUser(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("bookRequest", new BookRequest());
        model.addAttribute("requests", bookRequestRepository.findAllByOrderByCreatedAtDesc());
        model.addAttribute("activePage", "book-requests");
        return "BookRequests";
    }

    @PostMapping("/books/requests")
    public String submitBookRequest(@Valid @ModelAttribute("bookRequest") BookRequest bookRequest,
                                    BindingResult result,
                                    Model model,
                                    RedirectAttributes redirectAttributes,
                                    HttpSession session) {
        if (resolveSessionUser(session) == null) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            model.addAttribute("requests", bookRequestRepository.findAllByOrderByCreatedAtDesc());
            model.addAttribute("activePage", "book-requests");
            return "BookRequests";
        }
        bookRequestRepository.save(bookRequest);
        redirectAttributes.addFlashAttribute("successMessage", "Book request submitted successfully.");
        return "redirect:/books/requests";
    }

    private User resolveSessionUser(HttpSession session) {
        Object id = session.getAttribute(SESSION_USER_ID);
        if (!(id instanceof Long userId)) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }
}
