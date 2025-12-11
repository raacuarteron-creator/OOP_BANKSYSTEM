package conwiAssignment;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private String name;
    
    public User(String username, String password, String name) {
        this.username = username;
        this.password = password;
        this.name = name;
    }
    
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    
    public void setPassword(String password) { this.password = password; }
    
    public abstract String getRole();
}

class Admin extends User {
    private static final long serialVersionUID = 1L;
    public Admin(String username, String password, String name) {
        super(username, password, name);
    }
    
    @Override
    public String getRole() {
        return "ADMIN";
    }
}

class Student extends User {
    private static final long serialVersionUID = 1L;
    private String studentId;
    private String section;
    private String email;
    
    public Student(String username, String password, String name, String studentId, String section, String email) {
        super(username, password, name);
        this.studentId = studentId;
        this.section = section;
        this.email = email;
    }
    
    public String getStudentId() { return studentId; }
    public String getSection() { return section; }
    public String getEmail() { return email; }
    
    @Override
    public String getRole() {
        return "STUDENT";
    }
}

class Question implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String questionText;
    private String type;
    private List<String> options;
    private String correctAnswer;
    private int points;
    private String subject;
    private String difficulty;
    
    public Question(int id, String questionText, String type, List<String> options, 
                   String correctAnswer, int points, String subject, String difficulty) {
        this.id = id;
        this.questionText = questionText;
        this.type = type;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.points = points;
        this.subject = subject;
        this.difficulty = difficulty;
    }
    
    public int getId() { return id; }
    public String getQuestionText() { return questionText; }
    public String getType() { return type; }
    public List<String> getOptions() { return options; }
    public String getCorrectAnswer() { return correctAnswer; }
    public int getPoints() { return points; }
    public String getSubject() { return subject; }
    public String getDifficulty() { return difficulty; }
    
    public void setQuestionText(String text) { this.questionText = text; }
    public void setType(String type) { this.type = type; }
    public void setOptions(List<String> options) { this.options = options; }
    public void setCorrectAnswer(String answer) { this.correctAnswer = answer; }
    public void setPoints(int points) { this.points = points; }
    public void setSubject(String subject) { this.subject = subject; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
}

class QuizResult implements Serializable {
    private static final long serialVersionUID = 1L;
    private String studentUsername;
    private String subject;
    private int score;
    private int total;
    private double percentage;
    private String date;
    
    public QuizResult(String studentUsername, String subject, int score, int total, String date) {
        this.studentUsername = studentUsername;
        this.subject = subject;
        this.score = score;
        this.total = total;
        this.percentage = total > 0 ? (score * 100.0) / total : 0;
        this.date = date;
    }
    
    public String getStudentUsername() { return studentUsername; }
    public String getSubject() { return subject; }
    public int getScore() { return score; }
    public int getTotal() { return total; }
    public double getPercentage() { return percentage; }
    public String getDate() { return date; }
    
    public String getStatus() {
        return percentage >= 60 ? "PASSED" : "FAILED";
    }
}

public class QuizBankSystem {
    
    private static List<User> users = new ArrayList<>();
    private static List<Question> questions = new ArrayList<>();
    private static List<QuizResult> results = new ArrayList<>();
    private static User currentUser = null;
    private static int nextQuestionId = 1;
    
    private static JFrame loginFrame;
    private static JFrame registerFrame;
    private static JFrame adminFrame;
    private static JFrame studentFrame;
    private static JFrame quizFrame;
    private static JFrame resultFrame;
    
    private static java.util.Timer quizTimer;
    private static int timeRemaining = 0;
    private static JLabel timerLabel;
    private static JPanel timerPanel;
    
    private static List<Question> currentQuizQuestions;
    private static Map<Integer, String> studentAnswers = new HashMap<>();
    private static int currentQuestionIndex = 0;
    private static String currentQuizSubject;
    
    private static final String USERS_FILE = "users.dat";
    private static final String QUESTIONS_FILE = "questions.dat";
    private static final String RESULTS_FILE = "results.dat";
    
    public static void main(String[] args) {
        loadData();
        createWelcomeScreen();
    }
    
    @SuppressWarnings("unchecked")
    private static void loadData() {
        try {
            File usersFile = new File(USERS_FILE);
            if (usersFile.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(usersFile));
                users = (List<User>) ois.readObject();
                ois.close();
                
                boolean adminExists = false;
                for (User user : users) {
                    if (user.getUsername().equals("admin") && user.getRole().equals("ADMIN")) {
                        adminExists = true;
                        break;
                    }
                }
                
                if (!adminExists) {
                    users.add(new Admin("admin", "admin123", "System Administrator"));
                    saveData();
                }
            } else {
                users.add(new Admin("admin", "admin123", "System Administrator"));
                users.add(new Student("student1", "pass123", "John Doe", "S1001", "CS-101", "john@example.com"));
                users.add(new Student("student2", "pass123", "Jane Smith", "S1002", "CS-101", "jane@example.com"));
                saveData();
            }
            
            File questionsFile = new File(QUESTIONS_FILE);
            if (questionsFile.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(questionsFile));
                questions = (List<Question>) ois.readObject();
                ois.close();
                nextQuestionId = questions.stream().mapToInt(Question::getId).max().orElse(0) + 1;
            } else {
                createSampleQuestions();
            }
            
            File resultsFile = new File(RESULTS_FILE);
            if (resultsFile.exists()) {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(resultsFile));
                results = (List<QuizResult>) ois.readObject();
                ois.close();
            }
            
        } catch (Exception e) {
            users.add(new Admin("admin", "admin123", "System Administrator"));
            saveData();
        }
    }
    
    private static void saveData() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE));
            oos.writeObject(users);
            oos.close();
            
            oos = new ObjectOutputStream(new FileOutputStream(QUESTIONS_FILE));
            oos.writeObject(questions);
            oos.close();
            
            oos = new ObjectOutputStream(new FileOutputStream(RESULTS_FILE));
            oos.writeObject(results);
            oos.close();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error saving data: " + e.getMessage());
        }
    }
    
    private static void createSampleQuestions() {
        questions.add(new Question(nextQuestionId++, 
            "What is the capital of France?",
            "MCQ",
            Arrays.asList("London", "Berlin", "Paris", "Madrid"),
            "Paris",
            5,
            "Geography",
            "Easy"
        ));
        
        questions.add(new Question(nextQuestionId++,
            "Which planet is known as the Red Planet?",
            "MCQ",
            Arrays.asList("Venus", "Mars", "Jupiter", "Saturn"),
            "Mars",
            5,
            "Science",
            "Easy"
        ));
        
        questions.add(new Question(nextQuestionId++,
            "The process by which plants make their own food is called?",
            "IDENTIFICATION",
            new ArrayList<>(),
            "Photosynthesis",
            10,
            "Science",
            "Medium"
        ));
        
        questions.add(new Question(nextQuestionId++,
            "Who wrote 'Romeo and Juliet'?",
            "IDENTIFICATION",
            new ArrayList<>(),
            "William Shakespeare",
            10,
            "Literature",
            "Medium"
        ));
        
        saveData();
    }
    
    private static void createWelcomeScreen() {
        JFrame welcomeFrame = new JFrame("Quiz Bank System");
        welcomeFrame.setSize(500, 400);
        welcomeFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        welcomeFrame.setLayout(new BorderLayout());
        welcomeFrame.getContentPane().setBackground(new Color(240, 240, 240));
        
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(70, 130, 180));
        titlePanel.setBorder(BorderFactory.createEmptyBorder(30, 20, 30, 20));
        
        JLabel titleLabel = new JLabel("QUIZ BANK SYSTEM", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        
        JLabel subtitleLabel = new JLabel("Test Your Knowledge", JLabel.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 16));
        subtitleLabel.setForeground(new Color(220, 220, 220));
        titlePanel.add(subtitleLabel);
        
        JPanel buttonPanel = new JPanel(new GridLayout(3, 1, 20, 20));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(40, 80, 40, 80));
        buttonPanel.setBackground(new Color(240, 240, 240));
        
        JButton loginButton = new JButton("Login");
        loginButton.setFont(new Font("Arial", Font.BOLD, 18));
        loginButton.setBackground(new Color(70, 130, 180));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JButton registerButton = new JButton("Register as Student");
        registerButton.setFont(new Font("Arial", Font.BOLD, 18));
        registerButton.setBackground(new Color(60, 179, 113));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFocusPainted(false);
        registerButton.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JButton exitButton = new JButton("Exit");
        exitButton.setFont(new Font("Arial", Font.BOLD, 18));
        exitButton.setBackground(new Color(220, 20, 60));
        exitButton.setForeground(Color.WHITE);
        exitButton.setFocusPainted(false);
        exitButton.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        loginButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                loginButton.setBackground(new Color(100, 149, 237));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                loginButton.setBackground(new Color(70, 130, 180));
            }
        });
        
        registerButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                registerButton.setBackground(new Color(85, 205, 142));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                registerButton.setBackground(new Color(60, 179, 113));
            }
        });
        
        exitButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exitButton.setBackground(new Color(255, 69, 0));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exitButton.setBackground(new Color(220, 20, 60));
            }
        });
        
        loginButton.addActionListener(e -> {
            welcomeFrame.dispose();
            createLoginScreen();
        });
        
        registerButton.addActionListener(e -> {
            welcomeFrame.dispose();
            createRegistrationScreen();
        });
        
        exitButton.addActionListener(e -> System.exit(0));
        
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        buttonPanel.add(exitButton);
        
        JPanel infoPanel = new JPanel();
        infoPanel.setBackground(new Color(240, 240, 240));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel infoLabel = new JLabel("<html><center>"
            + "<b>Features:</b><br>"
            + "• Admin: Create & manage quizzes<br>"
            + "• Student: Take timed quizzes<br>"
            + "• Automatic grading<br>"
            + "• Score tracking<br>"
            + "• Multiple question types"
            + "</center></html>");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        infoLabel.setForeground(new Color(70, 70, 70));
        infoPanel.add(infoLabel);
        
        welcomeFrame.add(titlePanel, BorderLayout.NORTH);
        welcomeFrame.add(buttonPanel, BorderLayout.CENTER);
        welcomeFrame.add(infoPanel, BorderLayout.SOUTH);
        
        welcomeFrame.setLocationRelativeTo(null);
        welcomeFrame.setVisible(true);
    }
    
    private static void createRegistrationScreen() {
        registerFrame = new JFrame("Student Registration");
        registerFrame.setSize(500, 500);
        registerFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        registerFrame.setLayout(new BorderLayout());
        registerFrame.getContentPane().setBackground(new Color(240, 240, 240));
        
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(60, 179, 113));
        JLabel titleLabel = new JLabel("STUDENT REGISTRATION", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(240, 240, 240));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Full Name:"), gbc);
        gbc.gridx = 1;
        JTextField nameField = new JTextField(20);
        formPanel.add(nameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        JTextField usernameField = new JTextField(20);
        formPanel.add(usernameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        JTextField emailField = new JTextField(20);
        formPanel.add(emailField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Student ID:"), gbc);
        gbc.gridx = 1;
        JTextField studentIdField = new JTextField(20);
        formPanel.add(studentIdField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Section:"), gbc);
        gbc.gridx = 1;
        JTextField sectionField = new JTextField(20);
        formPanel.add(sectionField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 5;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        JPasswordField confirmPasswordField = new JPasswordField(20);
        formPanel.add(confirmPasswordField, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBackground(new Color(240, 240, 240));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        
        JButton registerButton = new JButton("Register");
        registerButton.setBackground(new Color(60, 179, 113));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFont(new Font("Arial", Font.BOLD, 14));
        registerButton.setPreferredSize(new Dimension(120, 35));
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.setBackground(new Color(220, 220, 220));
        cancelButton.setPreferredSize(new Dimension(120, 35));
        
        JButton backToLoginButton = new JButton("Back to Login");
        backToLoginButton.setBackground(new Color(70, 130, 180));
        backToLoginButton.setForeground(Color.WHITE);
        backToLoginButton.setPreferredSize(new Dimension(120, 35));
        
        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(backToLoginButton);
        
        JPanel termsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        termsPanel.setBackground(new Color(240, 240, 240));
        JCheckBox termsCheckBox = new JCheckBox("I agree to the Terms and Conditions");
        termsPanel.add(termsCheckBox);
        
        gbc.gridx = 0; gbc.gridy = 7;
        gbc.gridwidth = 2;
        formPanel.add(termsPanel, gbc);
        
        gbc.gridy = 8;
        formPanel.add(buttonPanel, gbc);
        
        registerButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String username = usernameField.getText().trim();
            String email = emailField.getText().trim();
            String studentId = studentIdField.getText().trim();
            String section = sectionField.getText().trim();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            
            if (name.isEmpty() || username.isEmpty() || email.isEmpty() || 
                studentId.isEmpty() || section.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(registerFrame, "Please fill in all fields!");
                return;
            }
            
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(registerFrame, "Passwords do not match!");
                return;
            }
            
            if (password.length() < 6) {
                JOptionPane.showMessageDialog(registerFrame, "Password must be at least 6 characters!");
                return;
            }
            
            if (!termsCheckBox.isSelected()) {
                JOptionPane.showMessageDialog(registerFrame, "Please agree to the Terms and Conditions!");
                return;
            }
            
            for (User user : users) {
                if (user.getUsername().equals(username)) {
                    JOptionPane.showMessageDialog(registerFrame, "Username already exists! Please choose another.");
                    return;
                }
            }
            
            for (User user : users) {
                if (user instanceof Student) {
                    Student student = (Student) user;
                    if (student.getStudentId().equals(studentId)) {
                        JOptionPane.showMessageDialog(registerFrame, "Student ID already registered!");
                        return;
                    }
                }
            }
            
            Student newStudent = new Student(username, password, name, studentId, section, email);
            users.add(newStudent);
            saveData();
            
            JOptionPane.showMessageDialog(registerFrame, 
                "Registration successful!\nYou can now login with your credentials.");
            
            registerFrame.dispose();
            createLoginScreen();
        });
        
        cancelButton.addActionListener(e -> {
            nameField.setText("");
            usernameField.setText("");
            emailField.setText("");
            studentIdField.setText("");
            sectionField.setText("");
            passwordField.setText("");
            confirmPasswordField.setText("");
            termsCheckBox.setSelected(false);
        });
        
        backToLoginButton.addActionListener(e -> {
            registerFrame.dispose();
            createLoginScreen();
        });
        
        registerFrame.add(titlePanel, BorderLayout.NORTH);
        registerFrame.add(formPanel, BorderLayout.CENTER);
        
        registerFrame.setLocationRelativeTo(null);
        registerFrame.setVisible(true);
    }
    
    private static void createLoginScreen() {
        loginFrame = new JFrame("Quiz Bank System - Login");
        loginFrame.setSize(450, 400);
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setLayout(new BorderLayout());
        loginFrame.getContentPane().setBackground(new Color(240, 240, 240));
        
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(70, 130, 180));
        JLabel titleLabel = new JLabel("LOGIN TO QUIZ BANK SYSTEM", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 22));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel);
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(240, 240, 240));
        formPanel.setBorder(BorderFactory.createEmptyBorder(30, 50, 20, 50));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        JTextField usernameField = new JTextField(15);
        formPanel.add(usernameField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(15);
        formPanel.add(passwordField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Login as:"), gbc);
        gbc.gridx = 1;
        String[] roles = {"Student", "Admin"};
        JComboBox<String> roleCombo = new JComboBox<>(roles);
        formPanel.add(roleCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        JCheckBox rememberCheckBox = new JCheckBox("Remember me");
        formPanel.add(rememberCheckBox, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(new Color(240, 240, 240));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
        
        JButton loginButton = new JButton("Login");
        loginButton.setBackground(new Color(70, 130, 180));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.setPreferredSize(new Dimension(100, 35));
        
        JButton clearButton = new JButton("Clear");
        clearButton.setBackground(new Color(220, 220, 220));
        clearButton.setPreferredSize(new Dimension(100, 35));
        
        JButton registerButton = new JButton("Register");
        registerButton.setBackground(new Color(60, 179, 113));
        registerButton.setForeground(Color.WHITE);
        registerButton.setPreferredSize(new Dimension(100, 35));
        
        buttonPanel.add(loginButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(registerButton);
        
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        formPanel.add(buttonPanel, gbc);
        
        gbc.gridy = 5;
        JPanel linkPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        linkPanel.setBackground(new Color(240, 240, 240));
        JLabel forgotPasswordLabel = new JLabel("<html><u>Forgot Password?</u></html>");
        forgotPasswordLabel.setForeground(new Color(70, 130, 180));
        forgotPasswordLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgotPasswordLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(loginFrame, 
                    "Please contact the system administrator to reset your password.");
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                forgotPasswordLabel.setForeground(Color.RED);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                forgotPasswordLabel.setForeground(new Color(70, 130, 180));
            }
        });
        linkPanel.add(forgotPasswordLabel);
        formPanel.add(linkPanel, gbc);
        
        loginButton.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String role = (String) roleCombo.getSelectedItem();
            
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(loginFrame, "Please enter both username and password!");
                return;
            }
            
            authenticateUser(username, password, role);
        });
        
        clearButton.addActionListener(e -> {
            usernameField.setText("");
            passwordField.setText("");
            roleCombo.setSelectedIndex(0);
            rememberCheckBox.setSelected(false);
        });
        
        registerButton.addActionListener(e -> {
            loginFrame.dispose();
            createRegistrationScreen();
        });
        
        passwordField.addActionListener(e -> loginButton.doClick());
        
        loginFrame.add(titlePanel, BorderLayout.NORTH);
        loginFrame.add(formPanel, BorderLayout.CENTER);
        
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setVisible(true);
    }
    
    private static void authenticateUser(String username, String password, String role) {
        for (User user : users) {
            if (user.getUsername().equals(username) && 
                user.getPassword().equals(password) && 
                user.getRole().equals(role.toUpperCase())) {
                
                currentUser = user;
                loginFrame.dispose();
                
                if (role.equals("Admin")) {
                    createAdminDashboard();
                } else {
                    createStudentDashboard();
                }
                return;
            }
        }
        JOptionPane.showMessageDialog(loginFrame, 
            "Invalid credentials or user role!\nPlease check your username, password, and selected role.");
    }
    
    private static void createAdminDashboard() {
        adminFrame = new JFrame("Quiz Bank System - Admin Dashboard");
        adminFrame.setSize(900, 600);
        adminFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        adminFrame.setLayout(new BorderLayout());
        
        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.setBackground(new Color(70, 130, 180));
        welcomePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getName() + " (Admin)");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 18));
        welcomeLabel.setForeground(Color.WHITE);
        
        JButton logoutButton = new JButton("Logout");
        logoutButton.setBackground(Color.RED);
        logoutButton.setForeground(Color.WHITE);
        logoutButton.addActionListener(e -> {
            adminFrame.dispose();
            currentUser = null;
            createWelcomeScreen();
        });
        
        welcomePanel.add(welcomeLabel, BorderLayout.WEST);
        welcomePanel.add(logoutButton, BorderLayout.EAST);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        JPanel questionPanel = createQuestionManagementPanel();
        tabbedPane.addTab("Question Bank", questionPanel);
        
        JPanel settingsPanel = createQuizSettingsPanel();
        tabbedPane.addTab("Quiz Settings", settingsPanel);
        
        JPanel resultsPanel = createResultsViewPanel();
        tabbedPane.addTab("View Results", resultsPanel);
        
        JPanel userPanel = createUserManagementPanel();
        tabbedPane.addTab("User Management", userPanel);
        
        adminFrame.add(welcomePanel, BorderLayout.NORTH);
        adminFrame.add(tabbedPane, BorderLayout.CENTER);
        
        adminFrame.setLocationRelativeTo(null);
        adminFrame.setVisible(true);
    }
    
    private static JPanel createQuestionManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Add/Edit Question"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Question:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        JTextArea questionTextArea = new JTextArea(3, 40);
        JScrollPane questionScroll = new JScrollPane(questionTextArea);
        formPanel.add(questionScroll, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        formPanel.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        String[] types = {"MCQ", "IDENTIFICATION"};
        JComboBox<String> typeCombo = new JComboBox<>(types);
        formPanel.add(typeCombo, gbc);
        
        gbc.gridx = 2; gbc.gridy = 1;
        formPanel.add(new JLabel("Points:"), gbc);
        gbc.gridx = 3;
        JTextField pointsField = new JTextField("5");
        formPanel.add(pointsField, gbc);
        
        JTextField[] optionFields = new JTextField[4];
        for (int i = 0; i < 4; i++) {
            gbc.gridx = 0; gbc.gridy = 2 + i;
            formPanel.add(new JLabel("Option " + (char)('A' + i) + ":"), gbc);
            gbc.gridx = 1; gbc.gridwidth = 3;
            optionFields[i] = new JTextField();
            formPanel.add(optionFields[i], gbc);
            gbc.gridwidth = 1;
        }
        
        typeCombo.addActionListener(e -> {
            boolean isMCQ = typeCombo.getSelectedItem().equals("MCQ");
            for (JTextField field : optionFields) {
                field.setEnabled(isMCQ);
            }
        });
        
        gbc.gridx = 0; gbc.gridy = 6;
        formPanel.add(new JLabel("Correct Answer:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3;
        JTextField answerField = new JTextField();
        formPanel.add(answerField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 1;
        formPanel.add(new JLabel("Subject:"), gbc);
        gbc.gridx = 1;
        JTextField subjectField = new JTextField();
        formPanel.add(subjectField, gbc);
        
        gbc.gridx = 2;
        formPanel.add(new JLabel("Difficulty:"), gbc);
        gbc.gridx = 3;
        String[] difficulties = {"Easy", "Medium", "Hard"};
        JComboBox<String> difficultyCombo = new JComboBox<>(difficulties);
        formPanel.add(difficultyCombo, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton addButton = new JButton("Add Question");
        JButton clearButton = new JButton("Clear Form");
        JButton editButton = new JButton("Edit Selected");
        JButton deleteButton = new JButton("Delete Selected");
        
        addButton.setBackground(new Color(70, 130, 180));
        addButton.setForeground(Color.WHITE);
        editButton.setBackground(new Color(255, 165, 0));
        editButton.setForeground(Color.WHITE);
        deleteButton.setBackground(Color.RED);
        deleteButton.setForeground(Color.WHITE);
        
        buttonPanel.add(addButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        
        gbc.gridx = 0; gbc.gridy = 8;
        gbc.gridwidth = 4;
        formPanel.add(buttonPanel, gbc);
        
        String[] columns = {"ID", "Question", "Type", "Subject", "Difficulty", "Points"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable questionsTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(questionsTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Question Bank"));
        
        refreshQuestionsTable(tableModel);
        
        addButton.addActionListener(e -> {
            String questionText = questionTextArea.getText().trim();
            String type = (String) typeCombo.getSelectedItem();
            String subject = subjectField.getText().trim();
            String difficulty = (String) difficultyCombo.getSelectedItem();
            String correctAnswer = answerField.getText().trim();
            int points;
            
            try {
                points = Integer.parseInt(pointsField.getText());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(adminFrame, "Points must be a number!");
                return;
            }
            
            if (questionText.isEmpty() || subject.isEmpty() || correctAnswer.isEmpty()) {
                JOptionPane.showMessageDialog(adminFrame, "Please fill all required fields!");
                return;
            }
            
            List<String> options = new ArrayList<>();
            if (type.equals("MCQ")) {
                for (int i = 0; i < 4; i++) {
                    String option = optionFields[i].getText().trim();
                    if (option.isEmpty()) {
                        JOptionPane.showMessageDialog(adminFrame, "Please fill all MCQ options!");
                        return;
                    }
                    options.add(option);
                }
            }
            
            Question newQuestion = new Question(
                nextQuestionId++,
                questionText,
                type,
                options,
                correctAnswer,
                points,
                subject,
                difficulty
            );
            
            questions.add(newQuestion);
            saveData();
            refreshQuestionsTable(tableModel);
            clearQuestionForm(questionTextArea, optionFields, answerField, subjectField);
            JOptionPane.showMessageDialog(adminFrame, "Question added successfully!");
        });
        
        clearButton.addActionListener(e -> {
            clearQuestionForm(questionTextArea, optionFields, answerField, subjectField);
            typeCombo.setSelectedIndex(0);
            difficultyCombo.setSelectedIndex(0);
            pointsField.setText("5");
        });
        
        editButton.addActionListener(e -> {
            int selectedRow = questionsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(adminFrame, "Please select a question to edit!");
                return;
            }
            
            int questionId = (int) tableModel.getValueAt(selectedRow, 0);
            Question question = questions.stream()
                .filter(q -> q.getId() == questionId)
                .findFirst()
                .orElse(null);
            
            if (question != null) {
                questionTextArea.setText(question.getQuestionText());
                typeCombo.setSelectedItem(question.getType());
                pointsField.setText(String.valueOf(question.getPoints()));
                answerField.setText(question.getCorrectAnswer());
                subjectField.setText(question.getSubject());
                difficultyCombo.setSelectedItem(question.getDifficulty());
                
                List<String> options = question.getOptions();
                for (int i = 0; i < 4; i++) {
                    if (i < options.size()) {
                        optionFields[i].setText(options.get(i));
                    } else {
                        optionFields[i].setText("");
                    }
                }
                
                questions.remove(question);
                refreshQuestionsTable(tableModel);
                JOptionPane.showMessageDialog(adminFrame, "Question loaded for editing. Click 'Add Question' to save changes.");
            }
        });
        
        deleteButton.addActionListener(e -> {
            int selectedRow = questionsTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(adminFrame, "Please select a question to delete!");
                return;
            }
            
            int confirm = JOptionPane.showConfirmDialog(adminFrame, 
                "Are you sure you want to delete this question?", 
                "Confirm Delete", 
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                int questionId = (int) tableModel.getValueAt(selectedRow, 0);
                questions.removeIf(q -> q.getId() == questionId);
                saveData();
                refreshQuestionsTable(tableModel);
                JOptionPane.showMessageDialog(adminFrame, "Question deleted successfully!");
            }
        });
        
        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);
        
        return panel;
    }
    
    private static void refreshQuestionsTable(DefaultTableModel tableModel) {
        tableModel.setRowCount(0);
        for (Question q : questions) {
            Object[] row = {
                q.getId(),
                q.getQuestionText().length() > 50 ? q.getQuestionText().substring(0, 50) + "..." : q.getQuestionText(),
                q.getType(),
                q.getSubject(),
                q.getDifficulty(),
                q.getPoints()
            };
            tableModel.addRow(row);
        }
    }
    
    private static void clearQuestionForm(JTextArea questionTextArea, JTextField[] optionFields, 
                                         JTextField answerField, JTextField subjectField) {
        questionTextArea.setText("");
        for (JTextField field : optionFields) {
            field.setText("");
        }
        answerField.setText("");
        subjectField.setText("");
    }
    
    private static JPanel createQuizSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Quiz Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        Set<String> subjects = new TreeSet<>();
        for (Question q : questions) {
            subjects.add(q.getSubject());
        }
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Subject:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> subjectCombo = new JComboBox<>(subjects.toArray(new String[0]));
        formPanel.add(subjectCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Time Limit (minutes):"), gbc);
        gbc.gridx = 1;
        JTextField timeField = new JTextField("30");
        formPanel.add(timeField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Number of Questions:"), gbc);
        gbc.gridx = 1;
        JTextField countField = new JTextField("10");
        formPanel.add(countField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Difficulty Filter:"), gbc);
        gbc.gridx = 1;
        String[] filters = {"All", "Easy Only", "Medium Only", "Hard Only"};
        JComboBox<String> filterCombo = new JComboBox<>(filters);
        formPanel.add(filterCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        JButton previewButton = new JButton("Preview Available Questions");
        previewButton.setBackground(new Color(70, 130, 180));
        previewButton.setForeground(Color.WHITE);
        formPanel.add(previewButton, gbc);
        
        JTextArea previewArea = new JTextArea(15, 60);
        previewArea.setEditable(false);
        JScrollPane previewScroll = new JScrollPane(previewArea);
        previewScroll.setBorder(BorderFactory.createTitledBorder("Available Questions"));
        
        previewButton.addActionListener(e -> {
            String selectedSubject = (String) subjectCombo.getSelectedItem();
            String filter = (String) filterCombo.getSelectedItem();
            
            List<Question> filteredQuestions = new ArrayList<>();
            for (Question q : questions) {
                if (q.getSubject().equals(selectedSubject)) {
                    if (filter.equals("All") || 
                        filter.equals("Easy Only") && q.getDifficulty().equals("Easy") ||
                        filter.equals("Medium Only") && q.getDifficulty().equals("Medium") ||
                        filter.equals("Hard Only") && q.getDifficulty().equals("Hard")) {
                        filteredQuestions.add(q);
                    }
                }
            }
            
            previewArea.setText("");
            previewArea.append("Total available questions: " + filteredQuestions.size() + "\n");
            previewArea.append("========================================\n");
            
            for (Question q : filteredQuestions) {
                previewArea.append("ID: " + q.getId() + " | " + q.getDifficulty() + " | " + q.getPoints() + " pts\n");
                previewArea.append("Q: " + q.getQuestionText() + "\n");
                previewArea.append("Type: " + q.getType() + "\n\n");
            }
        });
        
        panel.add(formPanel, BorderLayout.NORTH);
        panel.add(previewScroll, BorderLayout.CENTER);
        
        return panel;
    }
    
    private static JPanel createResultsViewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columns = {"Student", "Subject", "Score", "Total", "Percentage", "Status", "Date"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable resultsTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(resultsTable);
        
        refreshResultsTable(tableModel);
        
        JPanel controlPanel = new JPanel(new FlowLayout());
        JButton refreshButton = new JButton("Refresh");
        JButton exportButton = new JButton("Export to CSV");
        
        refreshButton.addActionListener(e -> refreshResultsTable(tableModel));
        exportButton.addActionListener(e -> exportResultsToCSV());
        
        controlPanel.add(refreshButton);
        controlPanel.add(exportButton);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);
        
        return panel;
    }
    
    private static void refreshResultsTable(DefaultTableModel tableModel) {
        tableModel.setRowCount(0);
        for (QuizResult result : results) {
            Object[] row = {
                result.getStudentUsername(),
                result.getSubject(),
                result.getScore(),
                result.getTotal(),
                String.format("%.2f%%", result.getPercentage()),
                result.getStatus(),
                result.getDate()
            };
            tableModel.addRow(row);
        }
    }
    
    private static void exportResultsToCSV() {
        try {
            FileWriter writer = new FileWriter("quiz_results.csv");
            writer.write("Student,Subject,Score,Total,Percentage,Status,Date\n");
            
            for (QuizResult result : results) {
                writer.write(String.format("%s,%s,%d,%d,%.2f,%s,%s\n",
                    result.getStudentUsername(),
                    result.getSubject(),
                    result.getScore(),
                    result.getTotal(),
                    result.getPercentage(),
                    result.getStatus(),
                    result.getDate()
                ));
            }
            
            writer.close();
            JOptionPane.showMessageDialog(adminFrame, "Results exported to quiz_results.csv");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(adminFrame, "Error exporting results: " + e.getMessage());
        }
    }
    
    private static JPanel createUserManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columns = {"Username", "Name", "Role", "Student ID", "Section", "Email"};
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable usersTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(usersTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Registered Users"));
        
        refreshUsersTable(tableModel);
        
        JPanel controlPanel = new JPanel(new FlowLayout());
        JButton refreshButton = new JButton("Refresh");
        JButton deleteButton = new JButton("Delete User");
        JButton resetPasswordButton = new JButton("Reset Password");
        
        refreshButton.addActionListener(e -> refreshUsersTable(tableModel));
        
        deleteButton.addActionListener(e -> {
            int selectedRow = usersTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(adminFrame, "Please select a user to delete!");
                return;
            }
            
            String username = (String) tableModel.getValueAt(selectedRow, 0);
            
            if (username.equals(currentUser.getUsername())) {
                JOptionPane.showMessageDialog(adminFrame, "You cannot delete your own account!");
                return;
            }
            
            if (username.equals("admin")) {
                JOptionPane.showMessageDialog(adminFrame, "Cannot delete the main admin account!");
                return;
            }
            
            int confirm = JOptionPane.showConfirmDialog(adminFrame, 
                "Are you sure you want to delete user: " + username + "?", 
                "Confirm Delete", 
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                users.removeIf(user -> user.getUsername().equals(username));
                saveData();
                refreshUsersTable(tableModel);
                JOptionPane.showMessageDialog(adminFrame, "User deleted successfully!");
            }
        });
        
        resetPasswordButton.addActionListener(e -> {
            int selectedRow = usersTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(adminFrame, "Please select a user!");
                return;
            }
            
            String username = (String) tableModel.getValueAt(selectedRow, 0);
            
            String newPassword = JOptionPane.showInputDialog(adminFrame, 
                "Enter new password for " + username + ":");
            
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                for (User user : users) {
                    if (user.getUsername().equals(username)) {
                        user.setPassword(newPassword.trim());
                        saveData();
                        JOptionPane.showMessageDialog(adminFrame, "Password reset successfully!");
                        break;
                    }
                }
            }
        });
        
        controlPanel.add(refreshButton);
        controlPanel.add(deleteButton);
        controlPanel.add(resetPasswordButton);
        
        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);
        
        return panel;
    }
    
    private static void refreshUsersTable(DefaultTableModel tableModel) {
        tableModel.setRowCount(0);
        for (User user : users) {
            if (user instanceof Student) {
                Student student = (Student) user;
                Object[] row = {
                    student.getUsername(),
                    student.getName(),
                    "Student",
                    student.getStudentId(),
                    student.getSection(),
                    student.getEmail()
                };
                tableModel.addRow(row);
            } else if (user instanceof Admin) {
                Object[] row = {
                    user.getUsername(),
                    user.getName(),
                    "Admin",
                    "N/A",
                    "N/A",
                    "N/A"
                };
                tableModel.addRow(row);
            }
        }
    }
    
    private static void createStudentDashboard() {
        Student student = (Student) currentUser;
        
        studentFrame = new JFrame("Quiz Bank System - Student Dashboard");
        studentFrame.setSize(900, 600);
        studentFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        studentFrame.setLayout(new BorderLayout());
        
        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.setBackground(new Color(70, 130, 180));
        welcomePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        JLabel welcomeLabel = new JLabel("<html>Welcome, <b>" + student.getName() + "</b><br>"
            + "ID: " + student.getStudentId() + " | Section: " + student.getSection() + " | Email: " + student.getEmail() + "</html>");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        welcomeLabel.setForeground(Color.WHITE);
        
        JButton logoutButton = new JButton("Logout");
        logoutButton.setBackground(Color.RED);
        logoutButton.setForeground(Color.WHITE);
        logoutButton.addActionListener(e -> {
            studentFrame.dispose();
            currentUser = null;
            createWelcomeScreen();
        });
        
        JButton profileButton = new JButton("My Profile");
        profileButton.setBackground(new Color(60, 179, 113));
        profileButton.setForeground(Color.WHITE);
        profileButton.addActionListener(e -> showStudentProfile(student));
        
        welcomePanel.add(welcomeLabel, BorderLayout.WEST);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        buttonPanel.add(profileButton);
        buttonPanel.add(logoutButton);
        welcomePanel.add(buttonPanel, BorderLayout.EAST);
        
        JTabbedPane mainTabbedPane = new JTabbedPane();
        
        JPanel takeQuizPanel = createTakeQuizPanel();
        mainTabbedPane.addTab("Take Quiz", takeQuizPanel);
        
        JPanel historyPanel = createScoreHistoryPanel();
        mainTabbedPane.addTab("Score History", historyPanel);
        
        JPanel instructionsPanel = createInstructionsPanel();
        mainTabbedPane.addTab("Instructions", instructionsPanel);
        
        studentFrame.add(welcomePanel, BorderLayout.NORTH);
        studentFrame.add(mainTabbedPane, BorderLayout.CENTER);
        
        studentFrame.setLocationRelativeTo(null);
        studentFrame.setVisible(true);
    }
    
    private static JPanel createTakeQuizPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.setBackground(new Color(240, 240, 240));
        
        Set<String> subjects = new TreeSet<>();
        for (Question q : questions) {
            subjects.add(q.getSubject());
        }
        
        if (subjects.isEmpty()) {
            JLabel noQuizLabel = new JLabel("<html><center><h2>No quizzes available</h2>"
                + "<p>Please contact your administrator to add quizzes.</p></center></html>", 
                JLabel.CENTER);
            panel.add(noQuizLabel, BorderLayout.CENTER);
            return panel;
        }
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createTitledBorder("Quiz Configuration"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 15, 15, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Select Subject:"), gbc);
        gbc.gridx = 1;
        JComboBox<String> subjectCombo = new JComboBox<>(subjects.toArray(new String[0]));
        subjectCombo.setPreferredSize(new Dimension(200, 25));
        formPanel.add(subjectCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Number of Questions:"), gbc);
        gbc.gridx = 1;
        JTextField questionCountField = new JTextField("5");
        questionCountField.setPreferredSize(new Dimension(200, 25));
        formPanel.add(questionCountField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Time Limit (minutes):"), gbc);
        gbc.gridx = 1;
        JTextField timeLimitField = new JTextField("10");
        timeLimitField.setPreferredSize(new Dimension(200, 25));
        formPanel.add(timeLimitField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Difficulty:"), gbc);
        gbc.gridx = 1;
        String[] difficulties = {"All Difficulties", "Easy", "Medium", "Hard"};
        JComboBox<String> difficultyCombo = new JComboBox<>(difficulties);
        difficultyCombo.setPreferredSize(new Dimension(200, 25));
        formPanel.add(difficultyCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton startQuizButton = new JButton("Start Quiz");
        startQuizButton.setBackground(new Color(70, 130, 180));
        startQuizButton.setForeground(Color.WHITE);
        startQuizButton.setFont(new Font("Arial", Font.BOLD, 16));
        startQuizButton.setPreferredSize(new Dimension(200, 40));
        
        startQuizButton.addActionListener(e -> {
            String subject = (String) subjectCombo.getSelectedItem();
            String difficultyFilter = (String) difficultyCombo.getSelectedItem();
            int questionCount;
            int timeLimit;
            
            try {
                questionCount = Integer.parseInt(questionCountField.getText());
                timeLimit = Integer.parseInt(timeLimitField.getText());
                
                if (questionCount <= 0 || timeLimit <= 0) {
                    JOptionPane.showMessageDialog(studentFrame, "Please enter positive numbers!");
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(studentFrame, "Please enter valid numbers!");
                return;
            }
            
            startQuiz(subject, difficultyFilter, questionCount, timeLimit);
        });
        
        formPanel.add(startQuizButton, gbc);
        
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Quiz Statistics"));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setPreferredSize(new Dimension(400, 100));
        
        String selectedSubject = (String) subjectCombo.getSelectedItem();
        long totalQuestions = questions.stream()
            .filter(q -> q.getSubject().equals(selectedSubject))
            .count();
        
        long easyQuestions = questions.stream()
            .filter(q -> q.getSubject().equals(selectedSubject) && q.getDifficulty().equals("Easy"))
            .count();
        
        long mediumQuestions = questions.stream()
            .filter(q -> q.getSubject().equals(selectedSubject) && q.getDifficulty().equals("Medium"))
            .count();
        
        long hardQuestions = questions.stream()
            .filter(q -> q.getSubject().equals(selectedSubject) && q.getDifficulty().equals("Hard"))
            .count();
        
        statsPanel.add(new JLabel("Total Questions: " + totalQuestions));
        statsPanel.add(new JLabel("Easy: " + easyQuestions));
        statsPanel.add(new JLabel("Medium: " + mediumQuestions));
        statsPanel.add(new JLabel("Hard: " + hardQuestions));
        
        subjectCombo.addActionListener(e -> {
            String subject = (String) subjectCombo.getSelectedItem();
            long total = questions.stream()
                .filter(q -> q.getSubject().equals(subject))
                .count();
            
            long easy = questions.stream()
                .filter(q -> q.getSubject().equals(subject) && q.getDifficulty().equals("Easy"))
                .count();
            
            long medium = questions.stream()
                .filter(q -> q.getSubject().equals(subject) && q.getDifficulty().equals("Medium"))
                .count();
            
            long hard = questions.stream()
                .filter(q -> q.getSubject().equals(subject) && q.getDifficulty().equals("Hard"))
                .count();
            
            ((JLabel)statsPanel.getComponent(0)).setText("Total Questions: " + total);
            ((JLabel)statsPanel.getComponent(1)).setText("Easy: " + easy);
            ((JLabel)statsPanel.getComponent(2)).setText("Medium: " + medium);
            ((JLabel)statsPanel.getComponent(3)).setText("Hard: " + hard);
        });
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(formPanel, BorderLayout.NORTH);
        centerPanel.add(statsPanel, BorderLayout.SOUTH);
        centerPanel.setBackground(new Color(240, 240, 240));
        
        panel.add(centerPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private static JPanel createScoreHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        String[] columns = {"Subject", "Score", "Total", "Percentage", "Status", "Date"};
        DefaultTableModel historyModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        JTable historyTable = new JTable(historyModel);
        JScrollPane historyScroll = new JScrollPane(historyTable);
        historyScroll.setBorder(BorderFactory.createTitledBorder("Your Quiz History"));
        
        refreshScoreHistory(historyModel);
        
        JPanel statsPanel = new JPanel(new GridLayout(2, 3, 10, 10));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Performance Statistics"));
        statsPanel.setPreferredSize(new Dimension(0, 100));
        
        JLabel totalQuizzesLabel = new JLabel("Total Quizzes: 0");
        JLabel averageScoreLabel = new JLabel("Average Score: 0%");
        JLabel highestScoreLabel = new JLabel("Highest Score: 0%");
        JLabel passedQuizzesLabel = new JLabel("Passed: 0");
        JLabel failedQuizzesLabel = new JLabel("Failed: 0");
        JLabel passRateLabel = new JLabel("Pass Rate: 0%");
        
        statsPanel.add(totalQuizzesLabel);
        statsPanel.add(averageScoreLabel);
        statsPanel.add(highestScoreLabel);
        statsPanel.add(passedQuizzesLabel);
        statsPanel.add(failedQuizzesLabel);
        statsPanel.add(passRateLabel);
        
        List<QuizResult> studentResults = new ArrayList<>();
        for (QuizResult result : results) {
            if (result.getStudentUsername().equals(currentUser.getUsername())) {
                studentResults.add(result);
            }
        }
        
        if (!studentResults.isEmpty()) {
            int total = studentResults.size();
            double totalPercentage = 0;
            double highestPercentage = 0;
            int passed = 0;
            
            for (QuizResult result : studentResults) {
                totalPercentage += result.getPercentage();
                if (result.getPercentage() > highestPercentage) {
                    highestPercentage = result.getPercentage();
                }
                if (result.getStatus().equals("PASSED")) {
                    passed++;
                }
            }
            
            double averagePercentage = totalPercentage / total;
            int failed = total - passed;
            double passRate = (passed * 100.0) / total;
            
            totalQuizzesLabel.setText("Total Quizzes: " + total);
            averageScoreLabel.setText(String.format("Average Score: %.1f%%", averagePercentage));
            highestScoreLabel.setText(String.format("Highest Score: %.1f%%", highestPercentage));
            passedQuizzesLabel.setText("Passed: " + passed);
            failedQuizzesLabel.setText("Failed: " + failed);
            passRateLabel.setText(String.format("Pass Rate: %.1f%%", passRate));
        }
        
        JPanel controlPanel = new JPanel(new FlowLayout());
        JButton refreshButton = new JButton("Refresh");
        JButton exportButton = new JButton("Export My Results");
        
        refreshButton.addActionListener(e -> {
            refreshScoreHistory(historyModel);
            
            List<QuizResult> updatedResults = new ArrayList<>();
            for (QuizResult result : results) {
                if (result.getStudentUsername().equals(currentUser.getUsername())) {
                    updatedResults.add(result);
                }
            }
            
            if (!updatedResults.isEmpty()) {
                int total = updatedResults.size();
                double totalPercentage = 0;
                double highestPercentage = 0;
                int passed = 0;
                
                for (QuizResult result : updatedResults) {
                    totalPercentage += result.getPercentage();
                    if (result.getPercentage() > highestPercentage) {
                        highestPercentage = result.getPercentage();
                    }
                    if (result.getStatus().equals("PASSED")) {
                        passed++;
                    }
                }
                
                double averagePercentage = totalPercentage / total;
                int failed = total - passed;
                double passRate = (passed * 100.0) / total;
                
                totalQuizzesLabel.setText("Total Quizzes: " + total);
                averageScoreLabel.setText(String.format("Average Score: %.1f%%", averagePercentage));
                highestScoreLabel.setText(String.format("Highest Score: %.1f%%", highestPercentage));
                passedQuizzesLabel.setText("Passed: " + passed);
                failedQuizzesLabel.setText("Failed: " + failed);
                passRateLabel.setText(String.format("Pass Rate: %.1f%%", passRate));
            }
        });
        
        exportButton.addActionListener(e -> exportStudentResults());
        
        controlPanel.add(refreshButton);
        controlPanel.add(exportButton);
        
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.add(controlPanel, BorderLayout.NORTH);
        northPanel.add(statsPanel, BorderLayout.SOUTH);
        
        panel.add(northPanel, BorderLayout.NORTH);
        panel.add(historyScroll, BorderLayout.CENTER);
        
        return panel;
    }
    
    private static void refreshScoreHistory(DefaultTableModel historyModel) {
        historyModel.setRowCount(0);
        for (QuizResult result : results) {
            if (result.getStudentUsername().equals(currentUser.getUsername())) {
                Object[] row = {
                    result.getSubject(),
                    result.getScore(),
                    result.getTotal(),
                    String.format("%.2f%%", result.getPercentage()),
                    result.getStatus(),
                    result.getDate()
                };
                historyModel.addRow(row);
            }
        }
    }
    
    private static void exportStudentResults() {
        try {
            String filename = currentUser.getUsername() + "_quiz_results.csv";
            FileWriter writer = new FileWriter(filename);
            writer.write("Subject,Score,Total,Percentage,Status,Date\n");
            
            for (QuizResult result : results) {
                if (result.getStudentUsername().equals(currentUser.getUsername())) {
                    writer.write(String.format("%s,%d,%d,%.2f,%s,%s\n",
                        result.getSubject(),
                        result.getScore(),
                        result.getTotal(),
                        result.getPercentage(),
                        result.getStatus(),
                        result.getDate()
                    ));
                }
            }
            
            writer.close();
            JOptionPane.showMessageDialog(studentFrame, "Your results exported to " + filename);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(studentFrame, "Error exporting results: " + e.getMessage());
        }
    }
    
    private static JPanel createInstructionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JTextArea instructionsArea = new JTextArea();
        instructionsArea.setEditable(false);
        instructionsArea.setFont(new Font("Arial", Font.PLAIN, 14));
        instructionsArea.setLineWrap(true);
        instructionsArea.setWrapStyleWord(true);
        
        String instructions = """
            ========================================
                    QUIZ INSTRUCTIONS
            ========================================
            
            1. SELECTING A QUIZ:
               - Choose a subject from the dropdown
               - Select number of questions (5-20 recommended)
               - Set time limit (10-60 minutes recommended)
               - Choose difficulty level if desired
            
            2. TAKING THE QUIZ:
               - Read each question carefully
               - For Multiple Choice: Select one answer
               - For Identification: Type your answer
               - Use Previous/Next buttons to navigate
               - Timer shows remaining time
            
            3. SUBMITTING:
               - Click "Submit Quiz" when finished
               - Quiz auto-submits when time expires
               - Unanswered questions count as wrong
            
            4. GRADING:
               - Immediate results after submission
               - Passing score: 60% or higher
               - Review correct/wrong answers
            
            5. TIPS:
               - Read all options before answering
               - Manage your time wisely
               - Review answers before submitting
               - Contact admin for technical issues
            
            Good luck with your quizzes!
            ========================================""";
        
        instructionsArea.setText(instructions);
        
        JScrollPane scrollPane = new JScrollPane(instructionsArea);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private static void showStudentProfile(Student student) {
        JDialog profileDialog = new JDialog(studentFrame, "My Profile", true);
        profileDialog.setSize(400, 400);
        profileDialog.setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel profileIcon = new JLabel("👨‍🎓", JLabel.CENTER);
        profileIcon.setFont(new Font("Arial", Font.PLAIN, 48));
        mainPanel.add(profileIcon, gbc);
        
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        
        String[][] info = {
            {"Full Name:", student.getName()},
            {"Username:", student.getUsername()},
            {"Student ID:", student.getStudentId()},
            {"Section:", student.getSection()},
            {"Email:", student.getEmail()},
            {"Role:", "Student"}
        };
        
        for (int i = 0; i < info.length; i++) {
            gbc.gridx = 0; gbc.gridy = 2 + i;
            mainPanel.add(new JLabel("<html><b>" + info[i][0] + "</b></html>"), gbc);
            
            gbc.gridx = 1;
            mainPanel.add(new JLabel(info[i][1]), gbc);
        }
        
        gbc.gridx = 0; gbc.gridy = 8;
        gbc.gridwidth = 2;
        JButton changePasswordButton = new JButton("Change Password");
        changePasswordButton.addActionListener(e -> changePassword());
        mainPanel.add(changePasswordButton, gbc);
        
        gbc.gridy = 9;
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> profileDialog.dispose());
        mainPanel.add(closeButton, gbc);
        
        profileDialog.add(mainPanel, BorderLayout.CENTER);
        profileDialog.setLocationRelativeTo(studentFrame);
        profileDialog.setVisible(true);
    }
    
    private static void changePassword() {
        JDialog passwordDialog = new JDialog(studentFrame, "Change Password", true);
        passwordDialog.setSize(300, 250);
        passwordDialog.setLayout(new BorderLayout());
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Current Password:"), gbc);
        gbc.gridx = 1;
        JPasswordField currentPasswordField = new JPasswordField(15);
        formPanel.add(currentPasswordField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("New Password:"), gbc);
        gbc.gridx = 1;
        JPasswordField newPasswordField = new JPasswordField(15);
        formPanel.add(newPasswordField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Confirm Password:"), gbc);
        gbc.gridx = 1;
        JPasswordField confirmPasswordField = new JPasswordField(15);
        formPanel.add(confirmPasswordField, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        
        saveButton.addActionListener(e -> {
            String currentPassword = new String(currentPasswordField.getPassword());
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());
            
            if (!currentPassword.equals(currentUser.getPassword())) {
                JOptionPane.showMessageDialog(passwordDialog, "Current password is incorrect!");
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(passwordDialog, "New passwords do not match!");
                return;
            }
            
            if (newPassword.length() < 6) {
                JOptionPane.showMessageDialog(passwordDialog, "Password must be at least 6 characters!");
                return;
            }
            
            currentUser.setPassword(newPassword);
            saveData();
            JOptionPane.showMessageDialog(passwordDialog, "Password changed successfully!");
            passwordDialog.dispose();
        });
        
        cancelButton.addActionListener(e -> passwordDialog.dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        formPanel.add(buttonPanel, gbc);
        
        passwordDialog.add(formPanel, BorderLayout.CENTER);
        passwordDialog.setLocationRelativeTo(studentFrame);
        passwordDialog.setVisible(true);
    }
    
    private static void startQuiz(String subject, String difficultyFilter, int questionCount, int timeLimit) {
        List<Question> subjectQuestions = new ArrayList<>();
        for (Question q : questions) {
            if (q.getSubject().equals(subject)) {
                if (difficultyFilter.equals("All Difficulties") || 
                    q.getDifficulty().equals(difficultyFilter)) {
                    subjectQuestions.add(q);
                }
            }
        }
        
        if (subjectQuestions.size() < questionCount) {
            JOptionPane.showMessageDialog(studentFrame, 
                "Only " + subjectQuestions.size() + " questions available for " + subject + 
                " (" + difficultyFilter + "). Please select fewer questions or different filter.");
            return;
        }
        
        Collections.shuffle(subjectQuestions, ThreadLocalRandom.current());
        currentQuizQuestions = subjectQuestions.subList(0, questionCount);
        currentQuizSubject = subject;
        currentQuestionIndex = 0;
        studentAnswers.clear();
        timeRemaining = timeLimit * 60;
        
        studentFrame.setVisible(false);
        createQuizInterface();
    }
    
    private static void createQuizInterface() {
        quizFrame = new JFrame("Quiz - " + currentQuizSubject);
        quizFrame.setSize(900, 650);
        quizFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        quizFrame.setLayout(new BorderLayout());
        
        timerPanel = new JPanel(new BorderLayout());
        timerPanel.setBackground(Color.RED);
        timerPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        timerLabel = new JLabel("Time Remaining: " + formatTime(timeRemaining), JLabel.CENTER);
        timerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        timerLabel.setForeground(Color.WHITE);
        
        JLabel warningLabel = new JLabel("Time will auto-submit when expired!", JLabel.CENTER);
        warningLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        warningLabel.setForeground(Color.YELLOW);
        
        timerPanel.add(timerLabel, BorderLayout.CENTER);
        timerPanel.add(warningLabel, BorderLayout.SOUTH);
        
        JPanel questionPanel = new JPanel(new BorderLayout());
        questionPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel questionNumberLabel = new JLabel();
        questionNumberLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        JTextArea questionTextArea = new JTextArea();
        questionTextArea.setFont(new Font("Arial", Font.PLAIN, 16));
        questionTextArea.setLineWrap(true);
        questionTextArea.setWrapStyleWord(true);
        questionTextArea.setEditable(false);
        questionTextArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane questionScroll = new JScrollPane(questionTextArea);
        
        JPanel answerPanel = new JPanel();
        
        JPanel navPanel = new JPanel(new BorderLayout());
        navPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        
        JButton prevButton = new JButton("← Previous");
        JButton nextButton = new JButton("Next →");
        JButton submitButton = new JButton("Submit Quiz");
        
        prevButton.setEnabled(false);
        submitButton.setBackground(new Color(0, 128, 0));
        submitButton.setForeground(Color.WHITE);
        
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);
        buttonPanel.add(submitButton);
        
        JLabel progressLabel = new JLabel();
        progressLabel.setHorizontalAlignment(SwingConstants.CENTER);
        progressLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        navPanel.add(progressLabel, BorderLayout.NORTH);
        navPanel.add(buttonPanel, BorderLayout.CENTER);
        
        Runnable updateQuestionDisplay = new Runnable() {
            @Override
            public void run() {
                if (currentQuestionIndex >= currentQuizQuestions.size()) {
                    return;
                }
                
                Question currentQuestion = currentQuizQuestions.get(currentQuestionIndex);
                
                questionNumberLabel.setText("Question " + (currentQuestionIndex + 1) + " of " + 
                                          currentQuizQuestions.size() + " | " + 
                                          currentQuestion.getDifficulty() + " | " + 
                                          currentQuestion.getPoints() + " points");
                
                questionTextArea.setText(currentQuestion.getQuestionText());
                
                answerPanel.removeAll();
                answerPanel.setLayout(new BorderLayout());
                
                if (currentQuestion.getType().equals("MCQ")) {
                    JPanel mcqPanel = new JPanel(new GridLayout(4, 1, 5, 5));
                    mcqPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
                    ButtonGroup group = new ButtonGroup();
                    List<String> options = currentQuestion.getOptions();
                    
                    for (int i = 0; i < options.size(); i++) {
                        JRadioButton radioButton = new JRadioButton((char)('A' + i) + ". " + options.get(i));
                        radioButton.setFont(new Font("Arial", Font.PLAIN, 14));
                        group.add(radioButton);
                        mcqPanel.add(radioButton);
                        
                        String savedAnswer = studentAnswers.get(currentQuestion.getId());
                        if (savedAnswer != null && savedAnswer.equals(options.get(i))) {
                            radioButton.setSelected(true);
                        }
                        
                        final int optionIndex = i;
                        radioButton.addActionListener(e -> {
                            studentAnswers.put(currentQuestion.getId(), options.get(optionIndex));
                            updateProgress(progressLabel);
                        });
                    }
                    
                    answerPanel.add(mcqPanel, BorderLayout.NORTH);
                } else {
                    JPanel idPanel = new JPanel(new BorderLayout());
                    idPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
                    
                    JTextField textField = new JTextField(40);
                    textField.setFont(new Font("Arial", Font.PLAIN, 14));
                    
                    String savedAnswer = studentAnswers.get(currentQuestion.getId());
                    if (savedAnswer != null) {
                        textField.setText(savedAnswer);
                    }
                    
                    textField.getDocument().addDocumentListener(new DocumentListener() {
                        public void changedUpdate(DocumentEvent e) { updateAnswer(); }
                        public void removeUpdate(DocumentEvent e) { updateAnswer(); }
                        public void insertUpdate(DocumentEvent e) { updateAnswer(); }
                        
                        private void updateAnswer() {
                            studentAnswers.put(currentQuestion.getId(), textField.getText().trim());
                            updateProgress(progressLabel);
                        }
                    });
                    
                    idPanel.add(new JLabel("Your Answer: "), BorderLayout.WEST);
                    idPanel.add(textField, BorderLayout.CENTER);
                    answerPanel.add(idPanel, BorderLayout.NORTH);
                }
                
                answerPanel.revalidate();
                answerPanel.repaint();
                
                prevButton.setEnabled(currentQuestionIndex > 0);
                nextButton.setText(currentQuestionIndex < currentQuizQuestions.size() - 1 ? "Next →" : "Finish");
                
                updateProgress(progressLabel);
            }
        };
        
        Consumer<JLabel> updateProgress = new Consumer<JLabel>() {
            @Override
            public void accept(JLabel label) {
                int answered = studentAnswers.size();
                int total = currentQuizQuestions.size();
                label.setText("Progress: " + answered + " / " + total + " questions answered");
            }
        };
        
        prevButton.addActionListener(e -> {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--;
                updateQuestionDisplay.run();
            }
        });
        
        nextButton.addActionListener(e -> {
            if (currentQuestionIndex < currentQuizQuestions.size() - 1) {
                currentQuestionIndex++;
                updateQuestionDisplay.run();
            } else {
                submitQuiz();
            }
        });
        
        submitButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(quizFrame,
                "Are you sure you want to submit your quiz?\nUnanswered questions will be marked wrong.",
                "Submit Quiz",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
            
            if (confirm == JOptionPane.YES_OPTION) {
                submitQuiz();
            }
        });
        
        JPanel questionHeaderPanel = new JPanel(new BorderLayout());
        questionHeaderPanel.add(questionNumberLabel, BorderLayout.NORTH);
        questionHeaderPanel.add(questionScroll, BorderLayout.CENTER);
        
        questionPanel.add(questionHeaderPanel, BorderLayout.NORTH);
        questionPanel.add(answerPanel, BorderLayout.CENTER);
        
        quizFrame.add(timerPanel, BorderLayout.NORTH);
        quizFrame.add(questionPanel, BorderLayout.CENTER);
        quizFrame.add(navPanel, BorderLayout.SOUTH);
        
        startQuizTimer();
        
        updateQuestionDisplay.run();
        
        quizFrame.setLocationRelativeTo(null);
        quizFrame.setVisible(true);
    }
    
    private static void startQuizTimer() {
        quizTimer = new java.util.Timer();
        quizTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                timeRemaining--;
                SwingUtilities.invokeLater(() -> {
                    timerLabel.setText("Time Remaining: " + formatTime(timeRemaining));
                    
                    if (timeRemaining <= 60) {
                        timerPanel.setBackground(Color.ORANGE);
                    }
                    if (timeRemaining <= 30) {
                        timerPanel.setBackground(new Color(255, 69, 0));
                    }
                    
                    if (timeRemaining <= 10) {
                        timerPanel.setBackground(Color.RED);
                        if (timeRemaining <= 5) {
                            timerPanel.setVisible(timeRemaining % 2 == 0);
                        }
                    }
                });
                
                if (timeRemaining <= 0) {
                    quizTimer.cancel();
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(quizFrame, 
                            "Time's up! Auto-submitting your quiz.", 
                            "Time Expired",
                            JOptionPane.INFORMATION_MESSAGE);
                        submitQuiz();
                    });
                }
            }
        }, 1000, 1000);
    }
    
    private static String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
    
    private static void updateProgress(JLabel progressLabel) {
        int answered = studentAnswers.size();
        int total = currentQuizQuestions.size();
        progressLabel.setText("Progress: " + answered + " / " + total + " questions answered");
    }
    
    private static void submitQuiz() {
        if (quizTimer != null) {
            quizTimer.cancel();
        }
        
        int totalScore = 0;
        int totalPoints = 0;
        
        for (Question q : currentQuizQuestions) {
            totalPoints += q.getPoints();
            String studentAnswer = studentAnswers.get(q.getId());
            
            if (studentAnswer != null && !studentAnswer.isEmpty() &&
                studentAnswer.equalsIgnoreCase(q.getCorrectAnswer())) {
                totalScore += q.getPoints();
            }
        }
        
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        QuizResult result = new QuizResult(
            currentUser.getUsername(),
            currentQuizSubject,
            totalScore,
            totalPoints,
            date
        );
        results.add(result);
        saveData();
        
        quizFrame.dispose();
        showQuizResult(totalScore, totalPoints);
    }
    
    private static void showQuizResult(int score, int total) {
        double percentage = (score * 100.0) / total;
        String status = percentage >= 60 ? "PASSED" : "FAILED";
        
        resultFrame = new JFrame("Quiz Results");
        resultFrame.setSize(500, 400);
        resultFrame.setLayout(new BorderLayout());
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        JLabel resultLabel = new JLabel("QUIZ RESULTS", JLabel.CENTER);
        resultLabel.setFont(new Font("Arial", Font.BOLD, 24));
        resultLabel.setForeground(new Color(70, 130, 180));
        
        JPanel scorePanel = new JPanel(new GridLayout(5, 1, 10, 10));
        scorePanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        
        JLabel subjectLabel = new JLabel("Subject: " + currentQuizSubject, JLabel.CENTER);
        subjectLabel.setFont(new Font("Arial", Font.BOLD, 16));
        
        JLabel scoreLabel = new JLabel("Score: " + score + " / " + total, JLabel.CENTER);
        scoreLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        
        JLabel percentageLabel = new JLabel(String.format("Percentage: %.2f%%", percentage), JLabel.CENTER);
        percentageLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        
        JLabel statusLabel = new JLabel("Status: " + status, JLabel.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 20));
        statusLabel.setForeground(status.equals("PASSED") ? Color.GREEN : Color.RED);
        
        JButton reviewButton = new JButton("View Question Review");
        reviewButton.addActionListener(e -> showQuestionReview());
        
        scorePanel.add(subjectLabel);
        scorePanel.add(scoreLabel);
        scorePanel.add(percentageLabel);
        scorePanel.add(statusLabel);
        scorePanel.add(reviewButton);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton retryButton = new JButton("Take Another Quiz");
        JButton dashboardButton = new JButton("Back to Dashboard");
        
        retryButton.addActionListener(e -> {
            resultFrame.dispose();
            studentFrame.setVisible(true);
        });
        
        dashboardButton.addActionListener(e -> {
            resultFrame.dispose();
            studentFrame.setVisible(true);
        });
        
        buttonPanel.add(retryButton);
        buttonPanel.add(dashboardButton);
        
        mainPanel.add(resultLabel, BorderLayout.NORTH);
        mainPanel.add(scorePanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        resultFrame.add(mainPanel, BorderLayout.CENTER);
        resultFrame.setLocationRelativeTo(null);
        resultFrame.setVisible(true);
    }
    
    private static void showQuestionReview() {
        JDialog reviewDialog = new JDialog(resultFrame, "Question Review", true);
        reviewDialog.setSize(600, 500);
        reviewDialog.setLayout(new BorderLayout());
        
        JTextArea reviewArea = new JTextArea();
        reviewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        reviewArea.setEditable(false);
        
        StringBuilder reviewText = new StringBuilder();
        reviewText.append("QUESTION REVIEW - ").append(currentQuizSubject).append("\n");
        reviewText.append("=".repeat(60)).append("\n\n");
        
        for (int i = 0; i < currentQuizQuestions.size(); i++) {
            Question q = currentQuizQuestions.get(i);
            String studentAnswer = studentAnswers.get(q.getId());
            boolean correct = studentAnswer != null && 
                            studentAnswer.equalsIgnoreCase(q.getCorrectAnswer());
            
            reviewText.append("Q").append(i + 1).append(" (").append(q.getDifficulty())
                     .append(", ").append(q.getPoints()).append(" pts):\n");
            reviewText.append(q.getQuestionText()).append("\n");
            
            if (q.getType().equals("MCQ")) {
                List<String> options = q.getOptions();
                for (int j = 0; j < options.size(); j++) {
                    char letter = (char)('A' + j);
                    reviewText.append("  ").append(letter).append(". ").append(options.get(j));
                    if (options.get(j).equals(q.getCorrectAnswer())) {
                        reviewText.append(" ✓ (Correct)");
                    }
                    if (studentAnswer != null && studentAnswer.equals(options.get(j))) {
                        reviewText.append(" ← Your Choice");
                    }
                    reviewText.append("\n");
                }
            } else {
                reviewText.append("Your Answer: ").append(studentAnswer != null ? studentAnswer : "(Not answered)").append("\n");
                reviewText.append("Correct Answer: ").append(q.getCorrectAnswer()).append("\n");
            }
            
            reviewText.append("Status: ").append(correct ? "✓ CORRECT" : "✗ WRONG").append("\n");
            reviewText.append("Points Earned: ").append(correct ? q.getPoints() : 0).append("/").append(q.getPoints()).append("\n");
            reviewText.append("-".repeat(60)).append("\n\n");
        }
        
        reviewArea.setText(reviewText.toString());
        
        JScrollPane scrollPane = new JScrollPane(reviewArea);
        reviewDialog.add(scrollPane, BorderLayout.CENTER);
        
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> reviewDialog.dispose());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(closeButton);
        reviewDialog.add(buttonPanel, BorderLayout.SOUTH);
        
        reviewDialog.setLocationRelativeTo(resultFrame);
        reviewDialog.setVisible(true);
    }
}