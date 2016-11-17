package aarhusuniversitet.brightcycle;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

public class Emergency {

    public static void makeEmergencyCall(Activity activity) {
        try {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:112"));
            if (callIntent.resolveActivity(activity.getPackageManager()) != null) {
                activity.startActivity(callIntent);
            }
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity.getApplicationContext(), "Error in your phone call" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static void makeEmergencySMS(Activity activity, LatLng latLng) {
        String message = "SOS I have an emergency! Please call me ASAP!";
        if (latLng != null) {
            message = "SOS I have an emergency at: " + latLng.latitude + ", " + latLng.longitude;
        }

        Uri smsUri = Uri.parse("tel:112");
        Intent intent = new Intent(Intent.ACTION_VIEW, smsUri);
        intent.putExtra("address", "112");
        intent.putExtra("sms_body", message);
        intent.setType("vnd.android-dir/mms-sms");

        if (intent.resolveActivity(activity.getPackageManager()) != null) {
            activity.startActivity(intent);
        }
    }
}
