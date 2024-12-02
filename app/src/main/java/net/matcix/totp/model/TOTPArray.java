package net.matcix.totp.model;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TOTPArray {
    private static TOTPArray instance;
    private static Context applicationContext;
    private static final String PREFS_NAME = "totp_prefs";
    private static final String KEY_ENTRIES = "entries";
    private final Gson gson = new Gson();
    
    public static void init(Context context) {
        applicationContext = context.getApplicationContext();
    }
    
    public static TOTPArray getInstance() {
        if (instance == null) {
            instance = new TOTPArray();
            instance.loadEntries();
        }
        return instance;
    }
    
    private TOTPArray() {}
    
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private List<TOTPEntry> entries = new ArrayList<>();

    public static class TOTPEntry {
        private String name;
        private String secret;
        private transient String currentCode;

        public TOTPEntry(String name, String secret) {
            this.name = name;
            this.secret = secret;
            updateCode();
        }

        public String getName() { return name; }
        public String getCurrentCode() { return currentCode; }
        public String getSecret() { return secret; }
        
        public void updateCode() {
            this.currentCode = generateTOTP(secret);
        }
    }

    public boolean addEntry(String name, String secret) {
        try {
            if (!isValidBase32(secret)) {
                return false;
            }
            entries.add(new TOTPEntry(name, secret));
            saveEntries();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void removeEntry(int position) {
        if (position >= 0 && position < entries.size()) {
            entries.remove(position);
            saveEntries();
        }
    }

    private void loadEntries() {
        SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_ENTRIES, "[]");
        Type type = new TypeToken<ArrayList<TOTPEntry>>(){}.getType();
        entries = gson.fromJson(json, type);
        
        for (TOTPEntry entry : entries) {
            entry.updateCode();
        }
    }

    private void saveEntries() {
        SharedPreferences prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = gson.toJson(entries);
        prefs.edit().putString(KEY_ENTRIES, json).apply();
    }

    private boolean isValidBase32(String secret) {
        String base32Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        secret = secret.toUpperCase().replaceAll("\\s", "");
        
        if (secret.length() % 8 != 0) return false;
        
        for (char c : secret.toCharArray()) {
            if (base32Chars.indexOf(c) == -1) return false;
        }
        return true;
    }

    private static String generateTOTP(String secret) {
        try {
            long time = System.currentTimeMillis() / 30000;
            byte[] msg = longToBytes(time);
            byte[] k = Base64.getDecoder().decode(secret);
            
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(k, HMAC_ALGORITHM);
            mac.init(key);
            
            byte[] hash = mac.doFinal(msg);
            int offset = hash[hash.length - 1] & 0xf;
            
            int binary = ((hash[offset] & 0x7f) << 24) |
                        ((hash[offset + 1] & 0xff) << 16) |
                        ((hash[offset + 2] & 0xff) << 8) |
                        (hash[offset + 3] & 0xff);
            
            int otp = binary % 1000000;
            return String.format("%06d", otp);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return "000000";
        }
    }

    private static byte[] longToBytes(long l) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= 8;
        }
        return result;
    }

    public List<TOTPEntry> getEntries() {
        return entries;
    }
} 