package tm.davletov.myshopping;

import android.app.AlertDialog;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

import android.graphics.Paint;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.ViewHolder> {
    private final List<ShoppingItem> items;
    private final Runnable onItemChanged;
    private final OnItemClickListener onItemClick;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public ShoppingListAdapter(List<ShoppingItem> items, Runnable onItemChanged, OnItemClickListener onItemClick) {
        this.items = items;
        this.onItemChanged = onItemChanged;
        this.onItemClick = onItemClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shopping, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShoppingItem item = items.get(position);

        // Добавляем лог для отладки
        Log.d("ShoppingListAdapter", "Item: " + item.getName() + ", Quantity: " + item.getQuantity() + ", Price: " + item.getPrice() + ", Total: " + item.getTotal());

        // Устанавливаем данные


        holder.tvName.setText(item.getName());
        holder.tvQuantity.setText(String.format(Locale.getDefault(), "%.2f", item.getQuantity()));
        holder.tvPrice.setText(String.format(Locale.getDefault(), "%.2f", item.getPrice()));
        holder.tvTotal.setText(String.format(Locale.getDefault(), "%.2f", item.getTotal()));

        // Устанавливаем цвет фона строки
        if (item.isPurchased()) {
            // Если товар куплен, цвет строки — светло-серый
            holder.itemView.setBackgroundColor(Color.LTGRAY); // Светло-серый цвет
            // Устанавливаем цвет текста (например, белый или черный в зависимости от читаемости)
            holder.tvName.setTextColor(Color.BLACK);
            holder.tvQuantity.setTextColor(Color.BLACK);
            holder.tvPrice.setTextColor(Color.BLACK);
            holder.tvTotal.setTextColor(Color.BLACK);
            // Зачеркиваем текст
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvQuantity.setPaintFlags(holder.tvQuantity.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvPrice.setPaintFlags(holder.tvPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTotal.setPaintFlags(holder.tvTotal.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            // Снимаем зачеркивание
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvQuantity.setPaintFlags(holder.tvQuantity.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvPrice.setPaintFlags(holder.tvPrice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTotal.setPaintFlags(holder.tvTotal.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            // Если товар не куплен, используем цвет категории (если есть)
            String color = item.getColor();
            if (color != null) {
                try {
                    holder.itemView.setBackgroundColor(Color.parseColor(color));
                    // Устанавливаем белый цвет текста
                    holder.tvName.setTextColor(Color.WHITE);
                    holder.tvQuantity.setTextColor(Color.WHITE);
                    holder.tvPrice.setTextColor(Color.WHITE);
                    holder.tvTotal.setTextColor(Color.WHITE);
                } catch (IllegalArgumentException e) {
                    holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                    holder.tvName.setTextColor(Color.WHITE);
                    holder.tvQuantity.setTextColor(Color.WHITE);
                    holder.tvPrice.setTextColor(Color.WHITE);
                    holder.tvTotal.setTextColor(Color.WHITE);
                }
            } else {
                holder.itemView.setBackgroundColor(Color.TRANSPARENT);
                holder.tvName.setTextColor(Color.WHITE);
                holder.tvQuantity.setTextColor(Color.WHITE);
                holder.tvPrice.setTextColor(Color.WHITE);
                holder.tvTotal.setTextColor(Color.WHITE);
                holder.tvTotal.setTextColor(Color.BLACK);
            }
        }

        // Обработчик клика по строке для переключения состояния покупки
        holder.itemView.setOnClickListener(v -> {
            item.setPurchased(!item.isPurchased()); // Переключаем состояние
            notifyItemChanged(position); // Обновляем строку
            onItemChanged.run(); // Обновляем итог
        });

        // Долгое нажатие — открытие формы редактирования
        holder.itemView.setOnLongClickListener(v -> {
            onItemClick.onItemClick(position); // Сразу открываем форму редактирования
            return true; // Событие обработано
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvQuantity;
        TextView tvPrice;
        TextView tvTotal;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvTotal = itemView.findViewById(R.id.total);
        }
    }
}