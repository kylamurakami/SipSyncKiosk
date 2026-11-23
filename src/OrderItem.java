public class OrderItem {
    private final MenuItem item;
    private final Customization customization; // null for snacks
    private final double unitPrice;             // price at the time it was added
    private int quantity;

    public OrderItem(MenuItem item, Customization customization, int quantity) {
        this.item = item;
        this.customization = customization;
        this.quantity = quantity;
        this.unitPrice = item.getPrice() + (customization == null ? 0 : customization.getExtraCost());
    }

    public boolean sameAs(MenuItem other, Customization c) {
        if (item.getItemId() != other.getItemId()) return false;
        String a = customization == null ? "" : customization.summary();
        String b = c == null ? "" : c.summary();
        return a.equals(b);
    }

    public double calculateSubtotal() { return unitPrice * quantity; }

    public MenuItem getItem() { return item; }
    public Customization getCustomization() { return customization; }
    public double getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int q) { quantity = q; }
    public String getOptionsText() { return customization == null ? "" : customization.summary(); }
}
