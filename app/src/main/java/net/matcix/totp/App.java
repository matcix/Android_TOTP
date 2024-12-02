package net.matcix.totp;

import android.app.Application;
import net.matcix.totp.model.TOTPArray;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        TOTPArray.init(this);
    }
} 