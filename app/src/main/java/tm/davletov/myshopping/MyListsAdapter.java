package tm.davletov.myshopping;

import android.content.Context;
import android.content.Intent;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyListsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_LIST = 0;
    private static final int TYPE_CATEGORY = 1;

    private final List<Object> items = new ArrayList<>();
    private final Map<String, List<ShoppingItem>> categoryItems = new HashMap<>();
    private final Context context;
    private final OnListClickListener listClickListener;
    private final OnCategoryClickListener categoryClickListener;

    public interface OnListClickListener {
        void onListClick(ShoppingList list);
    }

    public interface OnCategoryClickListener {
        void onCategoryClick(String category, List<ShoppingItem> items);
    }

    public MyListsAdapter(Context context, OnListClickListener listClickListener, OnCategoryClickListener categoryClickListener) {
        this.context = context;
        this.listClickListener = listClickListener;
        this.categoryClickListener = categoryClickListener;
    }

    public void setData(List<ShoppingList> lists) {
        items.clear();
        categoryItems.clear();

        for (ShoppingList list : lists) {
            items.add(list);
            Map<String, List<ShoppingItem>> categories = new HashMap<>();
            Map<String, String> categoryColors = new HashMap<>();
            
            // Группируем элементы по категориям и сохраняем цвета
            for (ShoppingItem item : list.getItems()) {
                String category = item.getCategory();
                if (category != null && !category.isEmpty()) {
                    categories.computeIfAbsent(category, k -> new ArrayList<>()).add(item);
                    categoryColors.put(category, item.getColor());
                }
            }

            // Добавляем категории после списка
            for (Map.Entry<String, List<ShoppingItem>> entry : categories.entrySet()) {
                String category = entry.getKey();
                CategoryItem categoryItem = new CategoryItem(category, list.getName(), categoryColors.get(category));
                items.add(categoryItem);
                categoryItems.put(getCategoryKey(list.getName(), category), entry.getValue());
            }
        }
        notifyDataSetChanged();
    }

    private String getCategoryKey(String listName, String category) {
        return listName + "_" + category;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof ShoppingList ? TYPE_LIST : TYPE_CATEGORY;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_LIST) {
            View view = inflater.inflate(R.layout.item_shopping_list, parent, false);
            return new ListViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_shopping_list_category, parent, false);
            return new CategoryViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_LIST) {
            ShoppingList list = (ShoppingList) items.get(position);
            ListViewHolder listHolder = (ListViewHolder) holder;
            listHolder.bind(list);
        } else {
            CategoryItem category = (CategoryItem) items.get(position);
            CategoryViewHolder categoryHolder = (CategoryViewHolder) holder;
            categoryHolder.bind(category);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public ShoppingList getItem(int position) {
        if (position >= 0 && position < items.size() && items.get(position) instanceof ShoppingList) {
            return (ShoppingList) items.get(position);
        }
        return null;
    }

    private class ListViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {
        private final TextView tvName;
        private final TextView tvDateTime;

        ListViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_list_name);
            tvDateTime = itemView.findViewById(R.id.tv_date_time);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    ShoppingList list = (ShoppingList) items.get(position);
                    listClickListener.onListClick(list);
                }
            });
            
            // Устанавливаем обработчик контекстного меню
            itemView.setOnCreateContextMenuListener(this);
        }

        void bind(ShoppingList list) {
            tvName.setText(list.getName());
            tvDateTime.setText(list.getDateTime());
        }
        
        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION && items.get(position) instanceof ShoppingList) {
                ShoppingList list = (ShoppingList) items.get(position);
                // Устанавливаем заголовок меню
                menu.setHeaderTitle(list.getName());
                
                // Сохраняем позицию в активности
                ((MyListsActivity) context).setContextMenuPosition(position);
            }
        }
    }

    private class CategoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCategory;
        private final View itemView;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            tvCategory = itemView.findViewById(R.id.tv_category);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    CategoryItem category = (CategoryItem) items.get(position);
                    String key = getCategoryKey(category.getListName(), category.getName());
                    List<ShoppingItem> items = categoryItems.get(key);
                    if (items != null) {
                        categoryClickListener.onCategoryClick(category.getName(), items);
                    }
                }
            });
        }

        void bind(CategoryItem category) {
            tvCategory.setText("    • " + category.getName());
            try {
                itemView.setBackgroundColor(android.graphics.Color.parseColor(category.getColor()));
            } catch (IllegalArgumentException e) {
                itemView.setBackgroundColor(android.graphics.Color.GRAY);
            }
        }
    }

    private static class CategoryItem {
        private final String name;
        private final String listName;
        private final String color;

        CategoryItem(String name, String listName, String color) {
            this.name = name;
            this.listName = listName;
            this.color = color;
        }

        String getName() {
            return name;
        }

        String getListName() {
            return listName;
        }

        String getColor() {
            return color;
        }
    }
} 