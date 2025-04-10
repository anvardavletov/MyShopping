package tm.davletov.myshopping;

import android.content.Context;
import android.net.Uri;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class FileUtils {
    public static String readFileFromUri(Context context, Uri uri) throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.getContentResolver().openInputStream(uri)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }

    public static ShoppingList parseMyShoppingFile(String jsonString) throws Exception {
        JSONObject json = new JSONObject(jsonString);
        String listName = json.getString("list_name");
        String dateTime = json.getString("date_time");
        String currency = json.getString("currency");
        JSONArray itemsArray = json.getJSONArray("items");
        List<ShoppingItem> items = new ArrayList<>();

        for (int i = 0; i < itemsArray.length(); i++) {
            JSONObject itemJson = itemsArray.getJSONObject(i);
            String name = itemJson.getString("name");
            int quantity = itemJson.getInt("quantity");
            double price = itemJson.getDouble("price");
            String category = itemJson.optString("category", null);
            String color = itemJson.optString("color", null);
            boolean isPurchased = itemJson.getBoolean("is_purchased");

            ShoppingItem item = new ShoppingItem(name, quantity, price, category, isPurchased);
            item.setColor(color);
            items.add(item);
        }

        return new ShoppingList(-1, listName, dateTime, currency, items); // ID не нужен для импорта
    }
}