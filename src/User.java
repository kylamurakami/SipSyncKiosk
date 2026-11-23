public abstract class User {
    private final int userId;
    private final String name;
    private final String username;
    private final String password;
    private boolean loggedIn;

    protected User(int userId, String name, String username, String password) {
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.password = password;
    }

    public boolean login(String user, String pass) {
        loggedIn = username.equals(user) && password.equals(pass);
        return loggedIn;
    }

    public void logout() { loggedIn = false; }
    public abstract String getRole();

    public int getUserId() { return userId; }
    public String getName() { return name; }
    public boolean isLoggedIn() { return loggedIn; }
}
