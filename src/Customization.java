import java.util.*;

public class Customization {
    public static final Map<String, Double> SIZES = new LinkedHashMap<>();
    public static final Map<String, Double> MILKS = new LinkedHashMap<>();
    public static final Map<String, Double> ADD_ONS = new LinkedHashMap<>();
    public static final String[] TEMPERATURES = {"Hot", "Iced"};
    public static final String[] SWEETNESS = {"0%", "25%", "50%", "75%", "100%"};

    static {
        SIZES.put("Small", 0.0);
        SIZES.put("Medium", 15.0);
        SIZES.put("Large", 30.0);
        MILKS.put("Regular", 0.0);
        MILKS.put("Oat", 20.0);
        MILKS.put("Almond", 20.0);
        MILKS.put("Soy", 15.0);
        ADD_ONS.put("Extra Shot", 25.0);
        ADD_ONS.put("Whipped Cream", 15.0);
        ADD_ONS.put("Caramel Drizzle", 10.0);
        ADD_ONS.put("Chocolate Syrup", 10.0);
    }

    private String size = "Small";
    private String temperature = "Hot";
    private String milkType = "Regular";
    private String sweetness = "100%";
    private final List<String> addOns = new ArrayList<>();

    public static Customization defaultFor(MenuItem item) {
        if (!item.isDrink()) return null;
        Customization c = new Customization();
        if (item.isFrappe()) c.temperature = "Iced";
        return c;
    }

    public double getExtraCost() {
        double extra = SIZES.getOrDefault(size, 0.0) + MILKS.getOrDefault(milkType, 0.0);
        for (String a : addOns) extra += ADD_ONS.getOrDefault(a, 0.0);
        return extra;
    }

    public String summary() {
        StringBuilder sb = new StringBuilder(size + " \u2022 " + temperature + " \u2022 " + milkType + " milk \u2022 " + sweetness + " sweet");
        if (!addOns.isEmpty()) sb.append(" \u2022 + ").append(String.join(", ", addOns));
        return sb.toString();
    }

    public Customization copy() {
        Customization c = new Customization();
        c.size = size; c.temperature = temperature; c.milkType = milkType; c.sweetness = sweetness;
        c.addOns.addAll(addOns);
        return c;
    }

    public String getSize() { return size; }
    public String getTemperature() { return temperature; }
    public String getMilkType() { return milkType; }
    public String getSweetness() { return sweetness; }
    public List<String> getAddOns() { return addOns; }
    public void setSize(String v) { size = v; }
    public void setTemperature(String v) { temperature = v; }
    public void setMilkType(String v) { milkType = v; }
    public void setSweetness(String v) { sweetness = v; }
}
