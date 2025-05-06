package tm.davletov.myshopping;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CurrencyManager {
    private final Context context;
    private final CurrencyLoadListener listener;
    private String currencySymbol;
    private SharedPreferences prefs;
    private static final String PREFS_NAME = "currency_prefs";
    private static final String KEY_CURRENCY = "selected_currency";
    private static final List<String> REQUIRED_CURRENCIES = Arrays.asList(
            "USD", "EUR", "RUB", "JPY", "GBP", "AUD", "CAD", "CHF",
            "CNY", "HKD", "NZD", "SEK", "KRW", "SGD", "NOK", "MXN",
            "INR", "ZAR", "TRY", "BRL", "TMT", "KZT", "AZN", "BYN"
    );
    private static final Map<String, String> CURRENCY_SYMBOLS = new HashMap<>();

    static {
        CURRENCY_SYMBOLS.put("USD", "$");
        CURRENCY_SYMBOLS.put("EUR", "€");
        CURRENCY_SYMBOLS.put("RUB", "₽");
        CURRENCY_SYMBOLS.put("JPY", "¥");
        CURRENCY_SYMBOLS.put("GBP", "£");
        CURRENCY_SYMBOLS.put("AUD", "$");
        CURRENCY_SYMBOLS.put("CAD", "$");
        CURRENCY_SYMBOLS.put("CHF", "₣");
        CURRENCY_SYMBOLS.put("CNY", "¥");
        CURRENCY_SYMBOLS.put("HKD", "$");
        CURRENCY_SYMBOLS.put("NZD", "$");
        CURRENCY_SYMBOLS.put("SEK", "kr");
        CURRENCY_SYMBOLS.put("KRW", "₩");
        CURRENCY_SYMBOLS.put("SGD", "$");
        CURRENCY_SYMBOLS.put("NOK", "kr");
        CURRENCY_SYMBOLS.put("MXN", "$");
        CURRENCY_SYMBOLS.put("INR", "₹");
        CURRENCY_SYMBOLS.put("ZAR", "R");
        CURRENCY_SYMBOLS.put("TRY", "₺");
        CURRENCY_SYMBOLS.put("BRL", "R$");
        CURRENCY_SYMBOLS.put("TMT", "m");
        CURRENCY_SYMBOLS.put("KZT", "₸");
        CURRENCY_SYMBOLS.put("AZN", "₼");
        CURRENCY_SYMBOLS.put("BYN", "Br");
    }

    public interface CurrencyLoadListener {
        void onCurrencyReady(String newSymbol);
        void onCurrencyLoadFailed();
    }

    public CurrencyManager(Context context, CurrencyLoadListener listener) {
        this.context = context;
        this.listener = listener;
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.currencySymbol = loadCurrency();
    }

    private String loadCurrency() {
        // Проверяем, есть ли сохраненная валюта
        String savedCurrency = prefs.getString(KEY_CURRENCY, null);
        if (savedCurrency != null) {
            return savedCurrency;
        }

        // Получаем регион устройства
        String countryCode = getDeviceRegion();
        Log.d("CurrencyManager", "Device region: " + countryCode);

        // Получаем валюту по региону
        String currencyCode = getCurrencyByCountry(countryCode);
        Log.d("CurrencyManager", "Currency code for region: " + currencyCode);

        // Если валюта региона не поддерживается, устанавливаем USD
        if (!REQUIRED_CURRENCIES.contains(currencyCode)) {
            currencyCode = "USD";
            Log.d("CurrencyManager", "Region currency not supported, defaulting to USD");
        }

        // Сохраняем валюту в SharedPreferences
        prefs.edit().putString(KEY_CURRENCY, currencyCode).apply();

        // Возвращаем символ валюты
        String symbol = CURRENCY_SYMBOLS.get(currencyCode);
        if (symbol != null) {
            listener.onCurrencyReady(symbol);
            return symbol;
        } else {
            listener.onCurrencyLoadFailed();
            return "$"; // По умолчанию
        }
    }

    private String getDeviceRegion() {
        // Получаем регион через Configuration
        Configuration config = context.getResources().getConfiguration();
        Locale locale;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = config.getLocales().get(0);
        } else {
            locale = config.locale;
        }
        return locale.getCountry();
    }

    private String getCurrencyByCountry(String countryCode) {
        try {
            Currency currency = Currency.getInstance(new Locale("", countryCode));
            return currency.getCurrencyCode();
        } catch (IllegalArgumentException e) {
            Log.e("CurrencyManager", "Invalid country code: " + countryCode);
            return "USD";
        }
    }

    public String getCurrency() {
        return currencySymbol;
    }
}