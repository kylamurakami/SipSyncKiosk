public class MenuItem {
    private final int itemId;
    private String name;
    private String category;
    private double price;
    private boolean isAvailable = true;
    private String imageFile;

    public MenuItem(int itemId, String name, String category, double price, String imageFile) {
        this.itemId = itemId;
        this.name = name;
        this.category = category;
        this.price = price;
        this.imageFile = imageFile;
    }

    public void updatePrice(double newPrice) { this.price = newPrice; }
    public void setAvailability(boolean available) { this.isAvailable = available; }

    public int getItemId() { return itemId; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public boolean isAvailable() { return isAvailable; }
    public String getImageFile() { return imageFile; }

    /** Everything except Snacks can be customized (size, temperature, milk, sweetness, add-ons). */
    public boolean isDrink() { return !category.equals("Snacks"); }
    public boolean isFrappe() { return category.equals("Frappes"); }
}
