import java.util.ArrayList;
import java.util.List;

/** In-memory data shared by the customer, staff and administrator screens. */
public final class DataStore {
    public static final String[] CATEGORIES = {"Coffee", "Non-Coffee", "Frappes", "Snacks", "Signature Drinks"};
    private static final DataStore INSTANCE = new DataStore();

    private final List<MenuItem> menu = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();
    private int nextItemId = 1;
    private int nextOrderNo = 1;

    public static DataStore get() { return INSTANCE; }

    private DataStore() {
        add("Americano", "Coffee", 89, "americano");
        add("Spanish Latte", "Coffee", 120, "spanish-latte");
        add("Caramel Macchiato", "Coffee", 125, "caramel-macchiato");
        add("Mocha", "Coffee", 115, "mocha");

        add("Matcha Latte", "Non-Coffee", 120, "matcha-latte");
        add("Taro Milk", "Non-Coffee", 100, "taro-milk");
        add("Chocolate Milk", "Non-Coffee", 95, "chocolate-milk");
        add("Strawberry Milk", "Non-Coffee", 100, "strawberry-milk");

        add("Java Chip", "Frappes", 140, "java-chip");
        add("Cookies & Cream", "Frappes", 135, "cookies-and-cream");
        add("Caramel Frappe", "Frappes", 130, "caramel-frappe");

        add("Croissant", "Snacks", 65, "croissant");
        add("Cookies", "Snacks", 45, "cookies");
        add("Brownies", "Snacks", 55, "brownies");
        add("French Fries", "Snacks", 75, "french-fries");

        add("Sync-Up Latte", "Signature Drinks", 135, "sync-up-latte");
        add("Pixel Mocha", "Signature Drinks", 130, "pixel-mocha");
        add("Digital Matcha", "Signature Drinks", 140, "digital-matcha");
    }

    private void add(String name, String cat, double price, String slug) {
        menu.add(new MenuItem(nextItemId++, name, cat, price, "menu/" + slug + ".png"));
    }

    public synchronized List<MenuItem> getMenu() { return new ArrayList<>(menu); }

    public synchronized MenuItem addMenuItem(String name, String category, double price, String imageFile) {
        MenuItem m = new MenuItem(nextItemId++, name, category, price, imageFile);
        menu.add(m);
        return m;
    }

    public synchronized MenuItem findByName(String name) {
        for (MenuItem m : menu) if (m.getName().equalsIgnoreCase(name)) return m;
        return null;
    }

    public synchronized Order placeOrder(List<OrderItem> items, String paymentMethod) {
        Order o = new Order(nextOrderNo++, items, paymentMethod);
        orders.add(o);
        return o;
    }

    public synchronized List<Order> getOrders() { return new ArrayList<>(orders); }
}
