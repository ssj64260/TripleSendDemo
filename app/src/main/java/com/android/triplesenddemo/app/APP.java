package com.android.triplesenddemo.app;

import android.app.Application;

import com.android.triplesenddemo.R;
import com.orhanobut.logger.Logger;

/**
 * application
 */

public class APP extends Application {

    private static APP INStANCE;

    public APP() {
        INStANCE = this;
    }

    public static APP getInstance() {
        if (INStANCE == null) {
            synchronized (APP.class) {
                if (INStANCE == null) {
                    INStANCE = new APP();
                }
            }
        }
        return INStANCE;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.init(getString(R.string.app_name));
    }
}
