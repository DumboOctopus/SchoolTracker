package io.github.dumbooctopus.schooltracker;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import io.github.dumbooctopus.schooltracker.R;


public class GeofenceTransitionsIntentService extends IntentService {

    private static final String TAG = "GeofenceTransitionsIS";


    public GeofenceTransitionsIntentService() {
        super("GeofenceTransitionsIntentService");
    }


    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = ""+geofencingEvent.getErrorCode(); //GeofenceErrorMessages.getErrorString(this,geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger
            // multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(geofenceTransition, triggeringGeofences);

            // Send notification and log the transition details.
            sendNotification(geofenceTransition, triggeringGeofences);
            Log.i(TAG, geofenceTransitionDetails);
        } else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }
    }

    private void sendNotification(int geofenceTransition, List<Geofence> triggeringGeofences) {

        SharedPreferences prefs = getSharedPreferences(getString(R.string.shared_preferences_name), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if(triggeringGeofences.size() > 1)
            Log.d("Error", "Multiple triggeringGeofences" + getGeofenceTransitionDetails(geofenceTransition, triggeringGeofences));
        String location = triggeringGeofences.get(0).getRequestId();

        try {
            ArrayList<ShiftData> shiftData = (ArrayList<ShiftData>) ObjectSerializer.deserialize(prefs.getString("SHIFT_DATA", ObjectSerializer.serialize(new ArrayList<ShiftData>())));
            boolean changedShiftData = false;

            switch (geofenceTransition) {
                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    //figure out what was the most recent shiftdata of the same location and put the ending time
                    int lastEnterance = shiftData.size() - 1;
                    while (lastEnterance >= 0 && !shiftData.get(lastEnterance).getLocation().equals(location))
                        lastEnterance--;
                    if (lastEnterance < 0) {
                        Log.d("ERROR", "Exited Region we never entered" + getGeofenceTransitionDetails(geofenceTransition, triggeringGeofences));
                        return;
                    } else {
                        changedShiftData = true;
                        shiftData.get(lastEnterance).setEnd(Calendar.getInstance().getTime());
                    }
                    break;
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    //what if we accidentally get 2 entrance requests?
                    //we have to check if the last request was an entrance
                    int lastIndex = shiftData.size() - 1;
                    if (shiftData.size() > 0 && shiftData.get(lastIndex).getEnd() == null && shiftData.get(lastIndex).getLocation().equals(location)) {
                        //if the last shift hasn't ended and its at the same location as this shift, then... ignore this request.

                    } else {
                        changedShiftData = true;
                        shiftData.add(new ShiftData(location, Calendar.getInstance().getTime()));
                    }

                    //TODO: what if the last shift hasn't ended and we are at a DIFFERENT location, then ?
                    break;
            }

            if(changedShiftData) {
                editor.putString("SHIFT_DATA", ObjectSerializer.serialize(shiftData));
                editor.commit();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    private String getGeofenceTransitionDetails(
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    }

}