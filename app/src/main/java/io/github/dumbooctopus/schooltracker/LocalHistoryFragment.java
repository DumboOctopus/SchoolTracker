package io.github.dumbooctopus.schooltracker;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LocalHistoryFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LocalHistoryFragment extends Fragment {

    private SharedPreferences prefs;
    private Button btnApprove;
    private GoogleAccountCredential mCredential;
    private final String[] SCOPES = {SheetsScopes.SPREADSHEETS };
    private String spreadsheetId = "1jIXLef6QYHqgH74ur-UnfPz3nrdncgOkcG7iqrI3fWg";


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

        try {
            ArrayList<ShiftData> shiftData = (ArrayList<ShiftData>) ObjectSerializer.deserialize(prefs.getString("SHIFT_DATA", ObjectSerializer.serialize(new ArrayList<ShiftData>())));
            ListView listview = (ListView) view.findViewById(R.id.shift_data_list_view);
            ShiftDataArrayAdapter arrayAdapter = new ShiftDataArrayAdapter(getActivity(), shiftData);
            listview.setAdapter(arrayAdapter);
        } catch (Exception e){
            Log.d("ERROR", e.toString());
        }


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
            String range = "Sheet 1!A3:F3";
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
            row.add(curr.getEnd().toString());
            row.add(curr.getDurationString());
            row.add(dispute);

            values.add(row);

            ValueRange body = new ValueRange()
                    .setValues(values);
            UpdateValuesResponse result =
                    mService.spreadsheets().values().update(spreadsheetId, range, body)
                            .setValueInputOption("USER_ENTERED")
                            .execute();
            System.out.printf("%d cells updated.", result.getUpdatedCells());
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
                output.add(0, "Data retrieved using the Google Sheets API:");
               // mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            Log.i("SHEETS", "onCancelled ");
            //mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
//                    showGooglePlayServicesAvailabilityErrorDialog(
//                            ((GooglePlayServicesAvailabilityIOException) mLastError)
//                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
//                    startActivityForResult(
//                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
//                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
//                    mOutputText.setText("The following error occurred:\n"
//                            + mLastError.getMessage());
                }
            } else {
//                mOutputText.setText("Request cancelled.");
            }
        }
    }

}
