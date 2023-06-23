package com.mohistmc.exoticgarden;

import lombok.Getter;
import lombok.SneakyThrows;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
public class TranslateHelper {

    public static Map<String, String> CURRENT_CACHE = new ConcurrentHashMap<>();
    private static PropertyResourceBundle rb;
    private static String properties = "message";
    @Getter
    private static Locale locale;
    private static InputStream inputStream;

    @SneakyThrows
    public TranslateHelper(Class<?> classLoader, Locale locale) {
        this.locale = locale;
        String lang = "_" + locale.getLanguage() + "_" + locale.getCountry();
        InputStream deFinputStream = classLoader.getResourceAsStream("/exotic_garden_lang/" + properties + ".properties");
        inputStream = classLoader.getResourceAsStream("/exotic_garden_lang/" + properties + lang + ".properties");
        if (inputStream == null) {
            inputStream = deFinputStream;
        } else if (inputStream == null) {
            System.out.println("invalid language file");
            System.exit(0);
        }
        rb = new PropertyResourceBundle(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    public String get(String key) {
        String string = rb.getString(key);
        if (!CURRENT_CACHE.containsKey(key)) {
            CURRENT_CACHE.put(key, string);
        } else {
            return CURRENT_CACHE.get(key);
        }
        return string;
    }

    public String get(String key, Object... f) {
        return new MessageFormat(get(key)).format(f);
    }

    public boolean isCN() {
        TimeZone timeZone = TimeZone.getDefault();
        return "Asia/Shanghai".equals(timeZone.getID()) || "CN".equals(locale.getCountry());
    }
}