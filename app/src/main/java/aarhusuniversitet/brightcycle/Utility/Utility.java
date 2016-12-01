package aarhusuniversitet.brightcycle.Utility;

import android.content.Context;
import android.content.pm.PackageManager;

/**
 * Created by janek on 17/11/2016.
 */

public class Utility {
    public static boolean checkLocationPermission(Context view) {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = view.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }
}
