
import java.util.HashMap;
import java.util.Map;

public class PersonalityQuiz {

    public static class Recommendation {
        public final String drink;
        public final String menuItem;
        public final String temperature;

        Recommendation(String drink, String menuItem, String temperature) {
            this.drink = drink;
            this.menuItem = menuItem;
            this.temperature = temperature;
        }

        public boolean isExactMatch() {
            return drink.equalsIgnoreCase(menuItem);
        }
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

    private static void r(String combo, String drink, String temp) {
        TABLE.put(combo, new Recommendation(drink, drink, temp));
    }

    private static void r(String combo, String drink) {
        r(combo, drink, null);
    }

    static {
        r("AAA", "Caramel Macchiato");
        r("AAB", "Spanish Latte");
        r("AAC", "Mocha");

        r("ABA", "Spanish Latte", "Hot");
        r("ABB", "Strawberry Milk");
        r("ABC", "Spanish Latte", "Iced");

        r("ACA", "Cookies & Cream", "Iced");
        r("ACB", "Matcha Latte", "Iced");
        r("ACC", "Caramel Frappe");

        r("BAA", "Spanish Latte", "Hot");
        r("BAB", "Strawberry Milk");
        r("BAC", "Mocha", "Hot");

        r("BBA", "Matcha Latte");
        r("BBB", "Matcha Latte", "Iced");
        r("BBC", "Digital Matcha", "Iced");

        r("BCA", "Taro Milk");
        r("BCB", "Matcha Latte", "Iced");
        r("BCC", "Mocha", "Iced");

        r("CAA", "Spanish Latte");
        r("CAB", "Caramel Macchiato", "Iced");
        r("CAC", "Caramel Macchiato");

        r("CBA", "Spanish Latte", "Hot");
        r("CBB", "Americano", "Iced");
        r("CBC", "Americano", "Iced");

        r("CCA", "Mocha");
        r("CCB", "Mocha", "Iced");
        r("CCC", "Americano");
    }

    public void startQuiz() {
        java.util.Arrays.fill(answers, ' ');
    }

    public String getQuestion(int i) {
        return questions[i];
    }

    public String[] getOptions(int i) {
        return options[i];
    }

    public int getQuestionCount() {
        return questions.length;
    }

    public void answer(int q, int choice) {
        answers[q] = (char) ('A' + choice);
    }

    public Recommendation getRecommendations() {
        return TABLE.get(new String(answers));
    }
}
