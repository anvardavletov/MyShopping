package tm.davletov.myshopping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ShoppingList implements Serializable {
    private int id;
    private String name;
    private String dateTime;
    private String currency;
    private List<ShoppingItem> items;

    public ShoppingList(int id, String name, String dateTime, String currency, List<ShoppingItem> items) {
        this.id = id;
        this.name = name;
        this.dateTime = dateTime;
        this.currency = currency;
        this.items = items != null ? items : new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<ShoppingItem> getItems() {
        return items;
    }

    public void setItems(List<ShoppingItem> items) {
        this.items = items;
    }

    public void addItem(ShoppingItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
    }

    public double getTotalPrice() {
        double total = 0;
        for (ShoppingItem item : items) {
            total += item.getPrice() * item.getQuantity();
        }
        return total;
    }

    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        for (ShoppingItem item : items) {
            String category = item.getCategory();
            if (!categories.contains(category)) {
                categories.add(category);
            }
        }
        return categories;
    }

    public List<ShoppingItem> getItemsByCategory(String category) {
        List<ShoppingItem> categoryItems = new ArrayList<>();
        for (ShoppingItem item : items) {
            if (item.getCategory().equals(category)) {
                categoryItems.add(item);
            }
        }
        return categoryItems;
    }
}