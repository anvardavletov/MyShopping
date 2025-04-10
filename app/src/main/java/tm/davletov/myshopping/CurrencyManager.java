package tm.davletov.myshopping;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import androidx.appcompat.app.AlertDialog;
import org.json.JSONObject;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CurrencyManager {
    private Context context;
    private SharedPreferences prefs;
    private Map<String, String> currencyMap;
    private String currencySymbol;

    // Список нужных валют
    private static final List<String> REQUIRED_CURRENCIES = Arrays.asList(
            "USD", "EUR", "RUB", "GBP", "TRY", "TMT", "AZN", "KZT", "UZS",
            "CNY", "JPY", "KRW", "THB", "CHF", "INR", "KWD", "AED", "UAH",
            "MXN", "ILS", "SAR"
    );

    // Базовый набор валют на случай ошибки API
    private static final Map<String, String> FALLBACK_CURRENCY_SYMBOLS = new HashMap<>();
    static {
        FALLBACK_CURRENCY_SYMBOLS.put("USD", "$");
        FALLBACK_CURRENCY_SYMBOLS.put("EUR", "€");
        FALLBACK_CURRENCY_SYMBOLS.put("RUB", "₽");
        FALLBACK_CURRENCY_SYMBOLS.put("GBP", "£");
    }

    // Маппинг для исправления символов
    private static final Map<String, String> SYMBOL_CORRECTIONS = new HashMap<>();
    static {
        SYMBOL_CORRECTIONS.put("RUB", "₽");     // Вместо "руб."
        SYMBOL_CORRECTIONS.put("INR", "₹");     // Вместо "টকা"
        SYMBOL_CORRECTIONS.put("TRY", "₺");     // Вместо "TL"
        SYMBOL_CORRECTIONS.put("TMT", "m");     // Вместо "T‏"
        SYMBOL_CORRECTIONS.put("UZS", "сўм");   // Вместо "UZS"
        SYMBOL_CORRECTIONS.put("CHF", "Fr.");   // Вместо "CHF"
        SYMBOL_CORRECTIONS.put("AZN", "₼");     // Вместо "ман."
        SYMBOL_CORRECTIONS.put("KZT", "₸");     // Вместо "тңг."
        SYMBOL_CORRECTIONS.put("CNY", "¥");     // Вместо "CN¥"
    }

    public CurrencyManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        this.currencySymbol = prefs.getString("currency", "$");
        this.currencyMap = new HashMap<>(FALLBACK_CURRENCY_SYMBOLS);
        loadCurrencyFromLocation();
    }

    private void loadCurrencyFromLocation() {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(Locale.getDefault().getCountry(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                String countryCode = addresses.get(0).getCountryCode();
                String defaultCurrencyCode = mapCountryToCurrency(countryCode);
                currencySymbol = currencyMap.getOrDefault(defaultCurrencyCode, "$");
                prefs.edit().putString("currency", currencySymbol).apply();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        new FetchCurrenciesTask().execute();
    }

    private String mapCountryToCurrency(String countryCode) {
        switch (countryCode) {
            case "US": return "USD";
            case "EU": return "EUR";
            case "RU": return "RUB";
            case "GB": return "GBP";
            case "TR": return "TRY";
            case "TM": return "TMT";
            case "AZ": return "AZN";
            case "KZ": return "KZT";
            case "UZ": return "UZS";
            case "CN": return "CNY";
            case "JP": return "JPY";
            case "KR": return "KRW";
            case "TH": return "THB";
            case "CH": return "CHF";
            case "IN": return "INR";
            case "KW": return "KWD";
            case "AE": return "AED";
            case "UA": return "UAH";
            case "MX": return "MXN";
            case "IL": return "ILS";
            case "SA": return "SAR";
            default: return "USD";
        }
    }

    public String getCurrency() {
        return currencySymbol;
    }

    public void showCurrencyDialog(Runnable onCurrencyChanged) {
        String[] currencies = currencyMap.values().toArray(new String[0]);
        new AlertDialog.Builder(context)
                .setTitle("Select Currency")
                .setItems(currencies, (dialog, which) -> {
                    currencySymbol = currencies[which];
                    prefs.edit().putString("currency", currencySymbol).apply();
                    onCurrencyChanged.run();
                })
                .show();
    }

    private class FetchCurrenciesTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            OkHttpClient client = new OkHttpClient();
            String cachedSymbols = prefs.getString("symbols_cache", null);
            if (cachedSymbols != null) {
                parseSymbolsJson(cachedSymbols);
            }

            Request request = new Request.Builder()
                    .url("https://api.currencyapi.com/v3/currencies?apikey=cur_live_szz2WibroRPL5NLm7xSjj3AUfi76IJLz5iuz1msU")
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else {
                    return "Error: " + response.code();
                }
            } catch (IOException e) {
                return "Error: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                if (result.startsWith("Error")) {
                    String cached = prefs.getString("symbols_cache", null);
                    if (cached != null) {
                        parseSymbolsJson(cached);
                    }
                } else {
                    prefs.edit().putString("symbols_cache", result).apply();
                    parseSymbolsJson(result);
                }
            }
        }

        private void parseSymbolsJson(String jsonString) {
            try {
                JSONObject json = new JSONObject(jsonString);
                JSONObject data = json.getJSONObject("data");
                Iterator<String> keys = data.keys();
                currencyMap.clear();
                while (keys.hasNext()) {
                    String code = keys.next();
                    if (REQUIRED_CURRENCIES.contains(code)) {
                        JSONObject currencyData = data.getJSONObject(code);
                        String symbol = currencyData.optString("symbol_native", code);
                        // Применяем исправления, если есть
                        symbol = SYMBOL_CORRECTIONS.getOrDefault(code, symbol);
                        currencyMap.put(code, symbol);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                currencyMap.putAll(FALLBACK_CURRENCY_SYMBOLS);
            }
        }
    }
}