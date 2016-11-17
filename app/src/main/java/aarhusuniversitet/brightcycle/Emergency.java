package aarhusuniversitet.brightcycle;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

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

}
