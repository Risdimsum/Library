package kh.edu.paragoniu.SpringBoot.Controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import kh.edu.paragoniu.SpringBoot.Model.Book;
import kh.edu.paragoniu.SpringBoot.Model.Issue;
import kh.edu.paragoniu.SpringBoot.Model.IssueStatus;
import kh.edu.paragoniu.SpringBoot.Model.Role;
import kh.edu.paragoniu.SpringBoot.Model.User;
import kh.edu.paragoniu.SpringBoot.Repos.BookRepository;
import kh.edu.paragoniu.SpringBoot.Repos.IssueRepository;
import kh.edu.paragoniu.SpringBoot.Repos.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class    LibraryFlowController {
    private static final String SESSION_USER_ID = "currentUserId";
    private static final String SESSION_USER_NAME = "currentUserName";
    private static final String SESSION_USER_ROLE = "currentUserRole";

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final IssueRepository issueRepository;

    public LibraryFlowController(UserRepository userRepository,
                                 BookRepository bookRepository,
                                 IssueRepository issueRepository) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.issueRepository = issueRepository;
    }

    @ModelAttribute("currentUserName")
    public String currentUserName(HttpSession session) {
        Object value = session.getAttribute(SESSION_USER_NAME);
        return value == null ? "Guest" : value.toString();
    }

    @ModelAttribute("currentUserRoleLabel")
    public String currentUserRoleLabel(HttpSession session) {
        User currentUser = resolveSessionUser(session);
        Role effectiveRole = resolveEffectiveRole(session, currentUser);
        if (effectiveRole == null) {
            return "Guest";
        }
        return Role.ADMIN.equals(effectiveRole) ? "Admin" : "Student";
    }

    @GetMapping("/students/add")
    public String showAddStudent(Model model, HttpSession session) {
        if (resolveSessionUser(session) == null) {
            return "redirect:/login";
        }
        model.addAttribute("studentForm", new StudentForm());
        model.addAttribute("activePage", "add-student");
        return "AddStudent";
    }

    @PostMapping("/students/add")
    public String addStudent(@Valid @ModelAttribute("studentForm") StudentForm studentForm,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        if (resolveSessionUser(session) == null) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            model.addAttribute("activePage", "add-student");
            return "AddStudent";
        }

        String name = studentForm.getName() == null ? "" : studentForm.getName().trim();
        String email = studentForm.getEmail() == null ? "" : studentForm.getEmail().trim();

        User student = new User();
        student.setName(name);
        student.setEmail(email);
        String rawPassword = studentForm.getPassword() == null ? "" : studentForm.getPassword().trim();
        if (rawPassword.isBlank()) {
            rawPassword = "changeme123";
        }
        student.setPassword(rawPassword);
        student.setRole(Role.USER);
        student.setActive(true);
        try {
            if (userRepository.findByEmail(email).isPresent()) {
                model.addAttribute("activePage", "add-student");
                model.addAttribute("errorMessage", "A student with this email already exists.");
                return "AddStudent";
            }
            userRepository.save(student);
        } catch (DataIntegrityViolationException ex) {
            model.addAttribute("activePage", "add-student");
            model.addAttribute("errorMessage", "This email already exists. Please use another email.");
            return "AddStudent";
        } catch (Exception ex) {
            model.addAttribute("activePage", "add-student");
            model.addAttribute("errorMessage", "Could not save student. Please try again.");
            return "AddStudent";
        }

        session.setAttribute(SESSION_USER_ID, student.getId());
        session.setAttribute(SESSION_USER_NAME, student.getName());
        session.setAttribute(SESSION_USER_ROLE, Role.USER.name());
        redirectAttributes.addFlashAttribute("successMessage", "Student added successfully.");
        return "redirect:/issues/new";
    }

    @GetMapping("/students")
    public String listStudents(Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        if (resolveSessionUser(session) == null) {
            return "redirect:/login";
        }
        List<User> students = userRepository.findByRoleOrderByCreatedAtDesc(Role.USER);
        if (students.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Add at least one student first.");
            return "redirect:/students/add";
        }
        model.addAttribute("students", students);
        model.addAttribute("activePage", "student-report");
        return "StudentReport";
    }

    @GetMapping("/issues/new")
    public String showIssueBookForm(Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        User currentUser = resolveSessionUser(session);
        Role effectiveRole = resolveEffectiveRole(session, currentUser);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (userRepository.findByRoleOrderByCreatedAtDesc(Role.USER).isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Add students before issuing books.");
            return "redirect:/students/add";
        }
        if (bookRepository.findByAvailableCopiesGreaterThanOrderByTitleAsc(0).isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "No available books in inventory.");
            return "redirect:/books";
        }
        addIssuePageData(model, currentUser, effectiveRole);
        IssueForm issueForm = new IssueForm();
        if (Role.USER.equals(effectiveRole)) {
            issueForm.setUserId(currentUser.getId());
        }
        model.addAttribute("issueForm", issueForm);
        model.addAttribute("studentLocked", Role.USER.equals(effectiveRole));
        model.addAttribute("activePage", "issue-book");
        return "IssueBook";
    }

    @PostMapping("/issues/new")
    public String issueBook(@ModelAttribute("issueForm") IssueForm issueForm,
                            Model model,
                            RedirectAttributes redirectAttributes,
                            HttpSession session) {
        User currentUser = resolveSessionUser(session);
        Role effectiveRole = resolveEffectiveRole(session, currentUser);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (Role.USER.equals(effectiveRole)) {
            issueForm.setUserId(currentUser.getId());
        }
        if (issueForm.getUserId() == null || issueForm.getBookId() == null) {
            addIssuePageData(model, currentUser, effectiveRole);
            model.addAttribute("activePage", "issue-book");
            model.addAttribute("studentLocked", Role.USER.equals(effectiveRole));
            model.addAttribute("errorMessage", "Please select both student and book.");
            return "IssueBook";
        }

        User user = userRepository.findById(issueForm.getUserId())
                .orElse(null);
        Book book = bookRepository.findById(issueForm.getBookId())
                .orElse(null);

        if (user == null || book == null) {
            addIssuePageData(model, currentUser, effectiveRole);
            model.addAttribute("activePage", "issue-book");
            model.addAttribute("studentLocked", Role.USER.equals(effectiveRole));
            model.addAttribute("errorMessage", "Selected student or book was not found.");
            return "IssueBook";
        }

        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            addIssuePageData(model, currentUser, effectiveRole);
            model.addAttribute("activePage", "issue-book");
            model.addAttribute("studentLocked", Role.USER.equals(effectiveRole));
            model.addAttribute("errorMessage", "This book is currently unavailable.");
            return "IssueBook";
        }

        Issue issue = new Issue();
        issue.setUser(user);
        issue.setBook(book);
        issue.setIssuedByUser(currentUser);
        issue.setStatus(IssueStatus.ISSUED);
        issueRepository.save(issue);

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        redirectAttributes.addFlashAttribute("successMessage", "Book issued successfully.");
        return "redirect:/issues";
    }

    @GetMapping("/issues")
    public String issueReport(Model model, HttpSession session) {
        User currentUser = resolveSessionUser(session);
        Role effectiveRole = resolveEffectiveRole(session, currentUser);
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (Role.USER.equals(effectiveRole)) {
            model.addAttribute("issues", issueRepository.findByUserIdOrderByIssueDateDescIdDesc(currentUser.getId()));
        } else {
            model.addAttribute("issues", issueRepository.findAllByOrderByIssueDateDescIdDesc());
        }
        model.addAttribute("activePage", "issue-report");
        return "IssueReport";
    }

    private void addIssuePageData(Model model, User currentUser, Role effectiveRole) {
        if (currentUser != null && Role.USER.equals(effectiveRole)) {
            model.addAttribute("students", List.of(currentUser));
        } else {
            model.addAttribute("students", userRepository.findByRoleOrderByCreatedAtDesc(Role.USER));
        }
        model.addAttribute("books", bookRepository.findByAvailableCopiesGreaterThanOrderByTitleAsc(0));
    }

    public static class StudentForm {
        @NotBlank(message = "Student name is mandatory")
        private String name;

        @NotBlank(message = "Email is mandatory")
        @Email(message = "Invalid email")
        private String email;

        private String password;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class IssueForm {
        private Long userId;
        private Long bookId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getBookId() {
            return bookId;
        }

        public void setBookId(Long bookId) {
            this.bookId = bookId;
        }
    }

    private User resolveSessionUser(HttpSession session) {
        Object id = session.getAttribute(SESSION_USER_ID);
        if (!(id instanceof Long userId)) {
            return null;
        }
        return userRepository.findById(userId).orElse(null);
    }

    private Role resolveEffectiveRole(HttpSession session, User currentUser) {
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
