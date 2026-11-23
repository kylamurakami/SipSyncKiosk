import java.util.HashMap;
import java.util.Map;

public class PersonalityQuiz {
    public static class Recommendation {
        public final String drink;      // the quiz result
        public final String menuItem;   // the item on the kiosk menu that matches (or is closest)
        public final String temperature; // "Hot", "Iced" or null (no preset)
        Recommendation(String drink, String menuItem, String temperature) {
            this.drink = drink; this.menuItem = menuItem; this.temperature = temperature;
        }
        public boolean isExactMatch() { return drink.equalsIgnoreCase(menuItem); }
    }

    private final String[] questions = {
        "How would you describe your personality?",
        "What's your current mood?",
        "What flavor do you prefer?"
    };
    private final String[][] options = {
        {"Sweet and friendly", "Calm and relaxed", "Bold and energetic"},
        {"Happy and cheerful", "Peaceful and cozy", "Adventurous and confident"},
        {"Sweet and creamy", "Light and refreshing", "Strong and rich"}
    };
    private final char[] answers = new char[3];
    private static final Map<String, Recommendation> TABLE = new HashMap<>();

    // Quiz result -> closest drink on the current menu. Edit the 2nd column if you add the missing drinks to the menu.
    private static void r(String combo, String drink, String menuItem, String temp) {
        TABLE.put(combo, new Recommendation(drink, menuItem, temp));
    }
    private static void r(String combo, String drink) { r(combo, drink, drink, null); }

    static {
        r("AAA", "Caramel Macchiato");
        r("AAB", "Spanish Latte");
        r("AAC", "Mocha");
        r("ABA", "Vanilla Latte", "Spanish Latte", "Hot");
        r("ABB", "Strawberry Milk");
        r("ABC", "Iced Spanish Latte", "Spanish Latte", "Iced");
        r("ACA", "Cookies & Cream Frappe", "Cookies & Cream", "Iced");
        r("ACB", "Strawberry Matcha", "Matcha Latte", "Iced");
        r("ACC", "Caramel Frappe");
        r("BAA", "Vanilla Latte", "Spanish Latte", "Hot");
        r("BAB", "Strawberry Milk");
        r("BAC", "White Chocolate Mocha", "Mocha", "Hot");
        r("BBA", "Matcha Latte");
        r("BBB", "Iced Matcha Latte", "Matcha Latte", "Iced");
        r("BBC", "Matcha Frappe", "Digital Matcha", "Iced");
        r("BCA", "Taro Milk");
        r("BCB", "Strawberry Matcha", "Matcha Latte", "Iced");
        r("BCC", "Iced Mocha", "Mocha", "Iced");
        r("CAA", "Spanish Latte");
        r("CAB", "Iced Caramel Latte", "Caramel Macchiato", "Iced");
        r("CAC", "Caramel Macchiato");
        r("CBA", "Vanilla Latte", "Spanish Latte", "Hot");
        r("CBB", "Cold Brew", "Americano", "Iced");
        r("CBC", "Iced Americano", "Americano", "Iced");
        r("CCA", "Mocha");
        r("CCB", "Iced Mocha", "Mocha", "Iced");
        r("CCC", "Americano");
    }

    public void startQuiz() { java.util.Arrays.fill(answers, ' '); }
    public String getQuestion(int i) { return questions[i]; }
    public String[] getOptions(int i) { return options[i]; }
    public int getQuestionCount() { return questions.length; }
    public void answer(int q, int choice) { answers[q] = (char) ('A' + choice); }

    public Recommendation getRecommendations() { return TABLE.get(new String(answers)); }
}
