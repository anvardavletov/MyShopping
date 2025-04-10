package tm.davletov.myshopping;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Класс, представляющий категорию товаров в списке покупок.
 */
public class Category implements Parcelable {
    private long id;
    private String name;
    private String color;
    private int icon;

    public Category() {
        this.id = 0;
        this.name = "";
        this.color = "#000000";
        this.icon = 0;
    }

    public Category(long id, String name, String color, int icon) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.icon = icon;
    }

    protected Category(Parcel in) {
        id = in.readLong();
        name = in.readString();
        color = in.readString();
        icon = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(color);
        dest.writeInt(icon);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Category> CREATOR = new Creator<Category>() {
        @Override
        public Category createFromParcel(Parcel in) {
            return new Category(in);
        }

        @Override
        public Category[] newArray(int size) {
            return new Category[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }
    
    @Override
    public String toString() {
        return name;
    }
} 