import java.time.LocalDate;

/** The single hard-coded Administrator account. Does not handle orders or payments. */
public class Administrator extends User {
    public Administrator() { super(2, "Administrator", "admin", "admin123"); }

    @Override public String getRole() { return "Administrator"; }

    public MenuItem manageMenu(String name, String category, double price, String imageFile) {
        return DataStore.get().addMenuItem(name, category, price, imageFile);
    }
    public void updatePrice(MenuItem m, double price) { m.updatePrice(price); }
    public void updateAvailability(MenuItem m, boolean available) { m.setAvailability(available); }

    public DailySalesSummary viewDailySales() {
        DailySalesSummary s = new DailySalesSummary(LocalDate.now());
        s.computeSummary(DataStore.get().getOrders());
        return s;
    }
}
