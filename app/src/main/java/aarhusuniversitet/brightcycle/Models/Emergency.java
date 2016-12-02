package aarhusuniversitet.brightcycle.Models;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.here.android.mpa.common.GeoCoordinate;

public class Emergency {

    private static final String EMERGENCY_NUMBER = "112";

    public static void makeEmergencyCall(Activity activity) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:"+ EMERGENCY_NUMBER));
            if (callIntent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(callIntent);
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity.getApplicationContext(), "Error in your phone call" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static void makeEmergencySMS(Activity activity, GeoCoordinate geoLocation) {
        String message = "SOS I have an emergency! Please call me ASAP!";
        if (geoLocation != null) {
            message = "SOS I have an emergency at: " + geoLocation.getLatitude() + ", " + geoLocation.getLongitude();
        }

        Uri smsUri = Uri.parse("tel:" + EMERGENCY_NUMBER);
        Intent intent = new Intent(Intent.ACTION_VIEW, smsUri);
        intent.putExtra("address", EMERGENCY_NUMBER);
        intent.putExtra("sms_body", message);
        intent.setType("vnd.android-dir/mms-sms");

        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        }
    }
}
