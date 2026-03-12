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
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Controller
public class BookController {
    // Session keys used across controller methods.
    private static final String SESSION_USER_ID = "currentUserId";
    private static final String SESSION_USER_NAME = "currentUserName";
    private static final String SESSION_USER_ROLE = "currentUserRole";

    private final BookRepository bookRepository;
    private final BookRequestRepository bookRequestRepository;
    private final UserRepository userRepository;

    // Saves uploaded cover to a writable temp folder (/tmp/covers) and returns web path.
    private String storeCover(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String name = UUID.randomUUID() + "." + (ext == null ? "png" : ext);
        // Use system temp (writable in most deployments, including Railway)
        Path base = Paths.get(System.getProperty("java.io.tmpdir"), "covers");
        Files.createDirectories(base);
        Path dest = base.resolve(name);
        file.transferTo(dest.toFile());
        return "/covers/" + name; // served via file:${java.io.tmpdir}/ in static locations
    }

    public BookController(BookRepository bookRepository,
                          BookRequestRepository bookRequestRepository,
                          UserRepository userRepository) {
        this.bookRepository = bookRepository;
        this.bookRequestRepository = bookRequestRepository;
        this.userRepository = userRepository;
    }

    @ModelAttribute("book")
    public Book bookModel() {
        // Default model object for add/edit form binding.
        return new Book();
    }

    @GetMapping({"/", "/login"})
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            HttpSession session,
                            Model model) {
        // If already logged in, skip login page.
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
        // Non-strict login: creates a USER account automatically if email does not exist.
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
        session.setAttribute(SESSION_USER_ROLE, user.getRole() == null ? Role.USER.name() : user.getRole().name());
        return "redirect:/issues/new";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // Remove all session values and return to login.
        session.invalidate();
        return "redirect:/login";
    }

    @ModelAttribute("currentUserName")
    public String currentUserName(HttpSession session) {
        // Shared template value for navbar/user label.
        Object value = session.getAttribute(SESSION_USER_NAME);
        return value == null ? "Guest" : value.toString();
    }

    @ModelAttribute("currentUserRoleLabel")
    public String currentUserRoleLabel(HttpSession session) {
        // Shared template value for showing current role.
        User currentUser = resolveSessionUser(session);
        Role effectiveRole = resolveEffectiveRole(session, currentUser);
        if (effectiveRole == null) {
            return "Guest";
        }
        return Role.ADMIN.equals(effectiveRole) ? "Admin" : "Student";
    }

    @GetMapping("/books")
    public String listBooks(@RequestParam(value = "keyword", required = false) String keyword,
                            Model model,
                            HttpSession session) {
        // Shows all books or filtered search results.
        User currentUser = resolveSessionUser(session);
        Role effectiveRole = resolveEffectiveRole(session, currentUser);
        if (currentUser == null) {
            return "redirect:/login";
        }
        List<Book> books;
        if (keyword == null || keyword.isBlank()) {
            books = bookRepository.findAll();
        } else {
            books = bookRepository.searchByTitleOrAuthor(keyword.trim());
        }
        model.addAttribute("books", books);
        model.addAttribute("isAdmin", Role.ADMIN.equals(effectiveRole));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("activePage", "book-record");
        return "List";
    }

    @GetMapping("/books/add")
    public String showAddForm(Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        // Only ADMIN can access add-book page.
        User currentUser = resolveSessionUser(session);
        Role effectiveRole = resolveEffectiveRole(session, currentUser);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (!Role.ADMIN.equals(effectiveRole)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only admin can add books.");
            return "redirect:/books";
        }
        if (userRepository.findByRoleOrderByCreatedAtDesc(Role.USER).isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Step 1: add a student before adding books.");
            return "redirect:/students/add";
        }
        if (!model.containsAttribute("book")) {
            model.addAttribute("book", new Book());
        }
        model.addAttribute("activePage", "add-book");
        return "Form"; // add book
    }

    @PostMapping(value = "/books/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String addBook(@Valid Book book,
                          BindingResult result,
                          @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
                          Model model,
                          RedirectAttributes redirectAttributes,
                          HttpSession session) {
        // Validates input, applies default values, then saves.
        User currentUser = resolveSessionUser(session);
        Role effectiveRole = resolveEffectiveRole(session, currentUser);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (!Role.ADMIN.equals(effectiveRole)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only admin can add books.");
            return "redirect:/books";
        }
        if (result.hasErrors()) {
            model.addAttribute("book", book);
            model.addAttribute("activePage", "add-book");
            return "Form"; // add book
        }
        applySimpleBookDefaults(book, currentUser);
        try {
            String coverPath = storeCover(coverFile); // store cover and attach path
            if (coverPath != null) {
                book.setCoverPath(coverPath);
            }
        } catch (IOException ioEx) {
            model.addAttribute("book", book);
            model.addAttribute("activePage", "add-book");
            model.addAttribute("errorMessage", "Cannot save cover image: " + ioEx.getMessage());
            return "Form";
        }
        try {
            bookRepository.save(book);
        } catch (DataAccessException ex) {
            model.addAttribute("book", book);
            model.addAttribute("activePage", "add-book");
            model.addAttribute("errorMessage", "Cannot save book. Please fill all required fields and try again.");
            return "Form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Book added successfully.");
        return "redirect:/books";
    }

    @GetMapping("/books/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model, HttpSession session) {
        // Reuses the same form template for editing an existing book.
        if (resolveSessionUser(session) == null) {
            return "redirect:/login";
        }
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid book ID: " + id));
        model.addAttribute("book", book);
        model.addAttribute("activePage", "book-record");
        return "Form";
    }

    @PostMapping(value = "/books/edit/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String updateBook(@PathVariable("id") Long id, @Valid Book book, BindingResult result, Model model,
                             RedirectAttributes redirectAttributes, HttpSession session,
                             @RequestParam(value = "coverFile", required = false) MultipartFile coverFile) {
        // Updates editable fields and keeps availableCopies consistent with totalCopies.
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
        try {
            String coverPath = storeCover(coverFile); // replace cover when new file uploaded
            if (coverPath != null) {
                existingBook.setCoverPath(coverPath);
            }
        } catch (IOException ioEx) {
            model.addAttribute("book", book);
            model.addAttribute("activePage", "book-record");
            model.addAttribute("errorMessage", "Cannot save cover image: " + ioEx.getMessage());
            return "Form";
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
        // Fill optional fields so book records remain usable even with minimal input.
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
        // "View" route currently redirects to edit page.
        if (resolveSessionUser(session) == null) {
            return "redirect:/login";
        }
        return "redirect:/books/edit/" + id; ///
    }

    @GetMapping("/books/requests")
    public String showBookRequests(Model model, HttpSession session) {
        // Shows request form and request history.
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
        // Saves a new request when validation passes.
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

    @GetMapping("/switch-role")
    public String switchRole(HttpSession session,
                             RedirectAttributes redirectAttributes,
                             @RequestHeader(value = "Referer", required = false) String referer) {
        // Debug/helper action to switch between ADMIN and USER role in session.
        User currentUser = resolveSessionUser(session);
        if (currentUser == null) {
            return "redirect:/login";
        }
        Role currentRole = resolveEffectiveRole(session, currentUser);
        Role nextRole = Role.ADMIN.equals(currentRole) ? Role.USER : Role.ADMIN;
        session.setAttribute(SESSION_USER_ROLE, nextRole.name());
        redirectAttributes.addFlashAttribute("successMessage",
                Role.ADMIN.equals(nextRole) ? "Switched to Admin." : "Switched to Student.");

        if (referer != null && !referer.isBlank()) {
            return "redirect:" + referer;  // go BACK to where you came from
        }
        return "redirect:/books";
    }

    private User resolveSessionUser(HttpSession session) {
        // Reads logged-in user id from session and loads full user entity.
        Object id = session.getAttribute(SESSION_USER_ID);
        if (!(id instanceof Long userId)) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private Role resolveEffectiveRole(HttpSession session, User currentUser) {
        // Session role overrides persisted role; fallback protects against bad session values.
        Object roleAttr = session.getAttribute(SESSION_USER_ROLE);
        if (roleAttr instanceof String roleName) {
            try {
                return Role.valueOf(roleName);
            } catch (IllegalArgumentException ignored) {
                // Fallback to persisted role when session role is invalid.
            }
        }
        return currentUser == null ? null : currentUser.getRole();
    }
}
