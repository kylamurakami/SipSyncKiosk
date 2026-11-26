
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final Staff staffAccount = new Staff();
    private final Administrator adminAccount = new Administrator();

    private final JTextField username = Theme.field(18);
    private final JPasswordField password = Theme.passwordField(18);

    private final JLabel error = Theme.label(
            " ", Font.BOLD, 14, Theme.PRIMARY
    );

    public LoginFrame() {
        Theme.prep(this, "SipSync Cafe - Employee Login");

        setLayout(new BorderLayout());

        add(
                Theme.header("Staff / Administrator Login"),
                BorderLayout.NORTH
        );

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(Theme.CREAM);

        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Theme.CREAM);

        card.setBorder(new CompoundBorder(
                new LineBorder(Theme.LATTE, 3, true),
                new EmptyBorder(28, 44, 28, 44)
        ));

        GridBagConstraints c = new GridBagConstraints();

        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.insets = new Insets(4, 0, 4, 0);

        JLabel logo = new JLabel(
                Theme.icon("logo_main.png", 110),
                SwingConstants.CENTER
        );

        JLabel title = Theme.label(
                "Staff / Administrator Login",
                Font.BOLD, 28, Theme.PRIMARY
        );

        title.setHorizontalAlignment(SwingConstants.CENTER);
        error.setHorizontalAlignment(SwingConstants.CENTER);

        Theme.Btn login = new Theme.Btn("Log In");
        login.setFont(Theme.font(Font.BOLD, 18));
        login.addActionListener(e -> doLogin());

        Theme.Btn back = Theme.Btn.outline("Back");

        back.addActionListener(e -> {
            dispose();
            StartFrame.goRoles();
        });

        password.addActionListener(e -> doLogin());

        username.addActionListener(
                e -> password.requestFocusInWindow()
        );

        card.add(logo, c);
        card.add(title, c);

        c.insets = new Insets(14, 0, 2, 0);

        card.add(Theme.label(
                "Username", Font.BOLD, 15, Theme.ESPRESSO
        ), c);

        c.insets = new Insets(2, 0, 4, 0);
        card.add(username, c);

        c.insets = new Insets(10, 0, 2, 0);

        card.add(Theme.label(
                "Password", Font.BOLD, 15, Theme.ESPRESSO
        ), c);

        c.insets = new Insets(2, 0, 4, 0);
        card.add(password, c);

        c.insets = new Insets(6, 0, 6, 0);

        card.add(error, c);
        card.add(login, c);
        card.add(back, c);

        center.add(card);
        add(center, BorderLayout.CENTER);
    }

    public LoginFrame(String role) {
        this();
    }

    private void doLogin() {
        String enteredUsername = username.getText().trim();
        String enteredPassword = new String(password.getPassword());

        if (staffAccount.login(enteredUsername, enteredPassword)) {

            dispose();
            new StaffFrame(staffAccount).setVisible(true);

        } else if (adminAccount.login(enteredUsername, enteredPassword)) {

            dispose();
            new AdminFrame(adminAccount).setVisible(true);

        } else {

            error.setText("Incorrect username or password.");
            password.setText("");
            password.requestFocusInWindow();
        }
    }
}
