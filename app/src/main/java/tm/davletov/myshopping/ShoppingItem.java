package tm.davletov.myshopping;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.Serializable;

public class ShoppingItem implements Serializable, Parcelable {
    private long id;
    private String name;
    private double quantity;
    private double price;
    private boolean checked;
    private String color;
    private long categoryId; // ID связанной категории

    public ShoppingItem() {
        this.id = 0;
        this.name = "";
        this.quantity = 1.0;
        this.price = 0.0;
        this.checked = false;
        this.color = null;
        this.categoryId = -1; // -1 означает отсутствие категории
    }

    public ShoppingItem(long id, String name, double quantity, double price, boolean checked, String color, long categoryId) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.checked = checked;
        this.color = color;
        this.categoryId = categoryId;
    }
    
    // Конструктор для совместимости со старым кодом
    public ShoppingItem(String name, double quantity, double price, String category, boolean isPurchased) {
        this.id = 0;
        this.name = name;
        this.quantity = quantity;
        this.price = price;
        this.checked = isPurchased;
        this.color = null;
        this.categoryId = -1;
    }

    // Конструктор для Parcelable
    protected ShoppingItem(Parcel in) {
        id = in.readLong();
        name = in.readString();
        quantity = in.readDouble();
        price = in.readDouble();
        checked = in.readByte() != 0;
        color = in.readString();
        categoryId = in.readLong();
    }

    // Геттеры и сеттеры остаются без изменений
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Nullable
    public String getColor() {
        return color;
    }

    public void setColor(@Nullable String color) {
        this.color = color;
    }

    public double getTotal() {
        return quantity * price;
    }
    
    public long getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }
    
    public boolean isPurchased() {
        return checked;
    }
    
    public void setPurchased(boolean purchased) {
        this.checked = purchased;
    }
    
    // Методы для обратной совместимости
    public String getCategory() {
        return String.valueOf(categoryId);
    }
    
    public void setCategory(String category) {
        try {
            this.categoryId = Long.parseLong(category);
        } catch (NumberFormatException e) {
            this.categoryId = -1;
        }
    }

    // Реализация Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeDouble(quantity);
        dest.writeDouble(price);
        dest.writeByte((byte) (checked ? 1 : 0));
        dest.writeString(color);
        dest.writeLong(categoryId);
    }

    public static final Creator<ShoppingItem> CREATOR = new Creator<ShoppingItem>() {
        @Override
        public ShoppingItem createFromParcel(Parcel in) {
            return new ShoppingItem(in);
        }

        @Override
        public ShoppingItem[] newArray(int size) {
            return new ShoppingItem[size];
        }
    };
}