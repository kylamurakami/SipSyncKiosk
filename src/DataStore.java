import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory data shared by the customer, staff and administrator screens.
 * Orders are also saved to "sipsync_orders.txt" (in the folder the app runs from) so the sales
 * history is still there the next time the kiosk is opened. Delete that file to start fresh.
 */
public final class DataStore {
    public static final String[] CATEGORIES = {"Coffee", "Non-Coffee", "Frappes", "Snacks", "Signature Drinks"};
    private static final DataStore INSTANCE = new DataStore();

    private final Path file = Paths.get("sipsync_orders.txt");
    private String lastSaved = "";

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

        load();

        // Save changes (payment confirmed, status changed...) every couple of seconds and when the app closes.
        Thread saver = new Thread(() -> {
            while (true) {
                try { Thread.sleep(2000); } catch (InterruptedException e) { return; }
                save();
            }
        }, "sipsync-autosave");
        saver.setDaemon(true);
        saver.start();
        Runtime.getRuntime().addShutdownHook(new Thread(this::save));
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

    public Order placeOrder(List<OrderItem> items, String paymentMethod) {
        Order o;
        synchronized (this) {
            o = new Order(nextOrderNo++, items, paymentMethod);
            orders.add(o);
        }
        save();
        return o;
    }

    public synchronized List<Order> getOrders() { return new ArrayList<>(orders); }

    // ------------------------------------------------------------ saving and loading
    // One "O" line per order, followed by one "I" line per item. Fields are separated by tabs.

    private static String clean(String s) { return s == null ? "" : s.replace('\t', ' ').replace('\n', ' '); }

    private synchronized String snapshot() {
        StringBuilder sb = new StringBuilder();
        for (Order o : orders) {
            sb.append("O\t").append(o.getOrderNumber())
                    .append('\t').append(o.getOrderDate())
                    .append('\t').append(clean(o.getPayment().getMethod()))
                    .append('\t').append(o.getPayment().isConfirmed() ? "1" : "0")
                    .append('\t').append(o.getStatus().name()).append('\n');
            for (OrderItem i : o.getItems()) {
                MenuItem m = i.getItem();
                Customization c = i.getCustomization();
                sb.append("I\t").append(clean(m.getName()))
                        .append('\t').append(clean(m.getCategory()))
                        .append('\t').append(i.getQuantity())
                        .append('\t').append(i.getUnitPrice())
                        .append('\t').append(c == null ? "0" : "1");
                if (c == null) {
                    sb.append("\t\t\t\t\t");
                } else {
                    sb.append('\t').append(clean(c.getSize()))
                            .append('\t').append(clean(c.getTemperature()))
                            .append('\t').append(clean(c.getMilkType()))
                            .append('\t').append(clean(c.getSweetness()))
                            .append('\t').append(clean(String.join(";", c.getAddOns())));
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    /** Writes the orders to the file (only when something changed). */
    public void save() {
        try {
            String s;
            synchronized (this) {
                s = snapshot();
                if (s.equals(lastSaved)) return;
            }
            Path tmp = Paths.get(file.toString() + ".tmp");
            Files.write(tmp, s.getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            synchronized (this) { lastSaved = s; }
        } catch (Exception e) {
            System.err.println("Could not save orders: " + e.getMessage());
        }
    }

    private void load() {
        if (!Files.exists(file)) return;
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            int no = 0;
            LocalDateTime date = null;
            String method = null, status = "PENDING";
            boolean confirmed = false;
            List<OrderItem> items = new ArrayList<>();

            for (String line : lines) {
                if (line.isEmpty()) continue;
                String[] f = line.split("\t", -1);
                if (f[0].equals("O")) {
                    addLoaded(no, date, method, confirmed, status, items);
                    no = Integer.parseInt(f[1]);
                    date = LocalDateTime.parse(f[2]);
                    method = f[3].isEmpty() ? null : f[3];
                    confirmed = f[4].equals("1");
                    status = f[5];
                    items = new ArrayList<>();
                } else if (f[0].equals("I")) {
                    double unit = Double.parseDouble(f[4]);
                    Customization c = null;
                    if (f[5].equals("1")) {
                        c = new Customization();
                        c.setSize(f[6]);
                        c.setTemperature(f[7]);
                        c.setMilkType(f[8]);
                        c.setSweetness(f[9]);
                        for (String a : f[10].split(";")) if (!a.isEmpty()) c.getAddOns().add(a);
                    }
                    // a copy of the item as it was when ordered, so later price changes do not rewrite history
                    double base = unit - (c == null ? 0 : c.getExtraCost());
                    MenuItem m = new MenuItem(-1, f[1], f[2], base, null);
                    items.add(new OrderItem(m, c, Integer.parseInt(f[3])));
                }
            }
            addLoaded(no, date, method, confirmed, status, items);
            lastSaved = snapshot();
        } catch (Exception e) {
            System.err.println("Could not fully read saved orders: " + e.getMessage());
        }
    }

    private void addLoaded(int no, LocalDateTime date, String method, boolean confirmed, String status, List<OrderItem> items) {
        if (date == null || items.isEmpty()) return;
        Order o = new Order(no, items, method, date);
        if (confirmed) o.getPayment().confirmPayment();
        Order.Status target = Order.Status.valueOf(status);
        for (int i = 0; i < 5 && o.getStatus() != target; i++) if (!o.updateStatus()) break;
        orders.add(o);
        nextOrderNo = Math.max(nextOrderNo, no + 1);
    }
}