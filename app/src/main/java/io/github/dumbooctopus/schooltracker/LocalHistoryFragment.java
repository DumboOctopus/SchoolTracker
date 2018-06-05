package io.github.dumbooctopus.schooltracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.wdullaer.swipeactionadapter.SwipeActionAdapter;
import com.wdullaer.swipeactionadapter.SwipeDirection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import pub.devrel.easypermissions.EasyPermissions;

import static android.app.Activity.RESULT_OK;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LocalHistoryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LocalHistoryFragment extends Fragment implements EasyPermissions.PermissionCallbacks {

    private SharedPreferences prefs;
    private Button btnApprove;


    private GoogleAccountCredential mCredential;
    private final String[] SCOPES = {SheetsScopes.SPREADSHEETS };
    private String spreadsheetId = "1jIXLef6QYHqgH74ur-UnfPz3nrdncgOkcG7iqrI3fWg";
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String PREF_ACCOUNT_NAME = "accountName";


    public LocalHistoryFragment() {
        // Required empty public constructor
    }


    public static LocalHistoryFragment newInstance() {
        LocalHistoryFragment fragment = new LocalHistoryFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCredential = GoogleAccountCredential.usingOAuth2(
                getActivity().getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_local_history, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        prefs = getActivity().getSharedPreferences(getString(R.string.shared_preferences_name), Activity.MODE_PRIVATE);

        //load shift data ListView.
        try {
            final ArrayList<ShiftData> shiftDataThatAdapterUses = (ArrayList<ShiftData>) ObjectSerializer.deserialize(prefs.getString("SHIFT_DATA", ObjectSerializer.serialize(new ArrayList<ShiftData>())));
            ListView listview = (ListView) view.findViewById(R.id.shift_data_list_view);

            //filters all approved shifts.
            for(int i = shiftDataThatAdapterUses.size() -1; i >= 0; i--){
                if(shiftDataThatAdapterUses.get(i).getState() == ShiftData.ShiftDataState.APPROVED){
                    shiftDataThatAdapterUses.remove(i);
                }
            }
            Collections.reverse(shiftDataThatAdapterUses);
            ShiftDataArrayAdapter arrayAdapter = new ShiftDataArrayAdapter(getActivity(), shiftDataThatAdapterUses);
            //ArrayAdapter<ShiftData> arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, shiftData);
            final SwipeActionAdapter swipeActionAdapter = new SwipeActionAdapter(arrayAdapter);
            swipeActionAdapter.setListView(listview);
            listview.setAdapter(swipeActionAdapter);

            swipeActionAdapter.addBackground(SwipeDirection.DIRECTION_NORMAL_LEFT,R.layout.row_bg_left)
                    .addBackground(SwipeDirection.DIRECTION_NORMAL_RIGHT,R.layout.row_bg_right);

            swipeActionAdapter.setSwipeActionListener(new SwipeActionAdapter.SwipeActionListener(){
                @Override
                public boolean hasActions(int position, SwipeDirection direction){
                    if(direction.isLeft()) return true; // Change this to false to disable left swipes
                    if(direction.isRight()) return true;
                    return false;
                }

                @Override
                public boolean shouldDismiss(int position, SwipeDirection direction){
                    // Only dismiss an item when swiping normal left
                    return direction == SwipeDirection.DIRECTION_NORMAL_LEFT || direction == SwipeDirection.DIRECTION_NORMAL_RIGHT;
                }

                @Override
                public void onSwipe(int[] positionList, SwipeDirection[] directionList){
                    for(int i=0;i<positionList.length;i++) {
                        SwipeDirection direction = directionList[i];
                        int position = positionList[i];
                        String dir = "";

                        switch (direction) {
                            case DIRECTION_NORMAL_LEFT:
                                dir = "Left";
                                break;
                            case DIRECTION_NORMAL_RIGHT:
                                try {
                                    acceptShiftData(positionList.length - 1 - i);
                                    shiftDataThatAdapterUses.remove(i);
                                } catch(Exception e){
                                    Log.d("SHEETS",e.toString());
                                }
                                break;
                        }

                        swipeActionAdapter.notifyDataSetChanged();
                    }
                }

//                @Override
//                public void onSwipeStarted(ListView listView, int position, SwipeDirection direction) {
//                    // User is swiping
//                }
//
//                @Override
//                public void onSwipeEnded(ListView listView, int position, SwipeDirection direction) {
//                    // User stopped swiping (lifted finger from the screen)
//                }
            });

        } catch (Exception e){
            Log.d("ERROR", e.toString());
        }

        btnApprove = (Button) view.findViewById(R.id.btnApprove);
        btnApprove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Snackbar mySnackbar = Snackbar.make(view, "adsf", Snackbar.LENGTH_SHORT);
                mySnackbar.show();
            }
        });
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void acceptShiftData(int index) {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            //mOutputText.setText("No network connection available.");
        } else {
            new SingleRequest(mCredential, index, " ").execute();
        }
    }

    private void disputeShiftData(int index, String dispute) {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            //mOutputText.setText("No network connection available.");
        } else {
            new SingleRequest(mCredential, index, dispute).execute();
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                getContext(), Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getActivity().getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                //getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    public void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
//                    mOutputText.setText(
//                            "This app requires Google Play Services. Please install " +
//                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    //getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getActivity().getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        //getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    //getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(getActivity());
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(getActivity());
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                getActivity(),
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);

        dialog.show();
    }

    private class SingleRequest extends AsyncTask<Void, Void, List<String>> {
        private com.google.api.services.sheets.v4.Sheets mService = null;
        private Exception mLastError = null;
        private int shiftIndex;
        private String dispute;

        SingleRequest(GoogleAccountCredential credential, int shiftIndex, String dispute) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.sheets.v4.Sheets.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("SchoolTracker")
                    .build();
            this.shiftIndex  = shiftIndex;
            this.dispute = dispute;
        }

        /**
         * Background task to call Google Sheets API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                 sendData();
                return Arrays.asList(new String[]{"Nothing here"});
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of names and majors of students in a sample spreadsheet:
         * https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgvE2upms/edit
         * @return List of names and majors
         * @throws IOException
         */
        private void sendData() throws IOException, ClassNotFoundException {
            String range = "Sheet1!A1";
            List<List<Object>> values = new ArrayList<>();
            ArrayList<Object> row = new ArrayList<>();

            //get data from shared preferences.
            ArrayList<ShiftData> shiftData = (ArrayList<ShiftData>) ObjectSerializer.deserialize(prefs.getString("SHIFT_DATA", ObjectSerializer.serialize(new ArrayList<ShiftData>())));
            ShiftData curr = shiftData.get(shiftIndex);
            String name = prefs.getString("name","No Name Set");

            //Person	Location	Start Time	End Time	Duration	Notes
            row.add(name);
            row.add(curr.getLocation());
            row.add(curr.getStart().toString());
            //TODO: for debugging purposes. Do not allow nulls
            row.add(curr.getEnd()==null?" ":curr.getEnd().toString());
            row.add(curr.getDurationString());
            row.add(dispute);

            values.add(row);

            ValueRange body = new ValueRange()
                    .setValues(values);
            AppendValuesResponse result =
                    mService.spreadsheets().values().append(spreadsheetId, range, body)
                            .setValueInputOption("USER_ENTERED")
                            .execute();
            Log.d("SHEETS", result.toString());
            //System.out.printf("%d cells updated.", result.get());
        }



        @Override
        protected void onPreExecute() {
            Log.i("SHEETS", "onPreExecute");
           // mOutputText.setText("");
           // mProgress.show();
        }

        @Override
        protected void onPostExecute(List<String> output) {
            Log.i("SHEETS", "onPostExecute " + output);
           // mProgress.hide();
            if (output == null || output.size() == 0) {
             //   mOutputText.setText("No results returned.");
            } else {
                //output.add(0, "Data retrieved using the Google Sheets API:");
               // mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            Log.i("SHEETS", "onCancelled ");
            Log.d("SHEETS", "The following error occurred:\n"
                    + mLastError.toString());
            //mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());

                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            LocalHistoryFragment.REQUEST_AUTHORIZATION);
                } else {
                    Log.d("SHEETS", "The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
//                mOutputText.setText("Request cancelled.");
            }
        }
    }

}
