package aarhusuniversitet.brightcycle.Utility;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import aarhusuniversitet.brightcycle.MapsActivity;
import aarhusuniversitet.brightcycle.R;

/**
 * Created by janek on 17/11/2016.
 */


