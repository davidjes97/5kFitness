package com.example.a5kfitness;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.SessionReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.fitness.result.SessionReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getTimeInstance;


/**
 * This sample demonstrates how to use the Sessions API of the Google Fit platform to insert
 * sessions into the History API, query against existing data, and remove sessions. It also
 * demonstrates how to authenticate a user with Google Play Services and how to properly
 * represent data in a Session, as well as how to use ActivitySegments.
 */
public class MainActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {
    private static final String TAG = "FIT_TAG";
    private static final int REQUEST_OAUTH_REQUEST_CODE = 1;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private SessionReadRequest sessionsRequest;
    public static TextView fitData;
    public static TextView goalDist;
    public Double goalDistance;
    public String data;
    public static double distance = 0;
    public static double steps = 0;
    DecimalFormat decimalFormat = new DecimalFormat("0.00");
    public DatabaseReference todaysGoalDistance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        fitData = findViewById(R.id.fit_Data);

        ImageButton refreshButton = findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recreate();
            }
        });

        if (!hasOAuthPermission()) {
            requestOAuthPermission();
        }
        sessionsRequest = readFitnessSession();
        data = sessionsRequest.toString();
        fitData.setText("Data: ");
//            readHistoryData();
        try {
            verifySession();
        } catch (Exception e) {
            Log.i(TAG, "Exception Found: " + e);
        }

        getTodaysGoal();
    }

    public void showMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.popup_menu);
        popupMenu.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.futureWorkouts) {
            Intent intent = new Intent(this, FutureWorkoutsActivity.class);
            startActivity(intent);
            return true;
        }
        return false;
    }

    private void getTodaysGoal() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("5kTrainingPlan");
        Calendar cal = Calendar.getInstance();
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        goalDist = findViewById(R.id.goal_dist);

        String dayOfMonthStr = String.valueOf(dayOfMonth);

        todaysGoalDistance = myRef.child(dayOfMonthStr).child("distance");

        todaysGoalDistance.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                goalDistance = (Double) dataSnapshot.getValue();
                Log.i(TAG, "Got: " + goalDistance);
                String goal = "Goal Distance " + goalDistance + " miles";
                goalDist.setText(goal);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Checks if user's account has OAuth permission to Fitness API.
     * Keep
     */
    private boolean hasOAuthPermission() {
        FitnessOptions fitnessOptions = getFitnessSignInOptions();
        return GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions);
    }

    /**
     * Launches the Google SignIn activity to request OAuth permission for the user.
     * Keep
     */
    private void requestOAuthPermission() {
        FitnessOptions fitnessOptions = getFitnessSignInOptions();
        GoogleSignIn.requestPermissions(
                this,
                REQUEST_OAUTH_REQUEST_CODE,
                GoogleSignIn.getLastSignedInAccount(this),
                fitnessOptions);
    }

    /**
     * Gets {@link FitnessOptions} in order to check or request OAuth permission for the user.
     * Keep
     */
    private FitnessOptions getFitnessSignInOptions() {
        return FitnessOptions.builder()
                .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_DISTANCE_CUMULATIVE, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.AGGREGATE_DISTANCE_DELTA, FitnessOptions.ACCESS_READ)
                .build();
    }

    /**
     * Returns a {@link SessionReadRequest} for all speed data in the past week.
     */
    private SessionReadRequest readFitnessSession() {
        // [START build_read_session_request]
        // Set a start and end time for our query, using a start time of 1 week before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        long startTime = cal.getTimeInMillis();

        // Build a session read request
        SessionReadRequest readRequest = new SessionReadRequest.Builder()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .read(DataType.TYPE_DISTANCE_CUMULATIVE)
                .read(DataType.AGGREGATE_DISTANCE_DELTA)
                .read(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .readSessionsFromAllApps()
                .enableServerQueries()
                .build();
        // [END build_read_session_request]

        return readRequest;
    }

    /**
     * Creates and executes a {@link SessionReadRequest} using {@link
     */
    private Task<SessionReadResponse> verifySession() {
        // Begin by creating the query.
        SessionReadRequest readRequest = readFitnessSession();

        // [START read_session]
        // Invoke the Sessions API to fetch the session with the query and wait for the result
        // of the read request. Note: Fitness.SessionsApi.readSession() requires the
        // ACCESS_FINE_LOCATION permission.
        return Fitness.getSessionsClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readSession(readRequest)
                .addOnSuccessListener(new OnSuccessListener<SessionReadResponse>() {
                    @Override
                    public void onSuccess(SessionReadResponse sessionReadResponse) {
                        // Get a list of the sessions that match the criteria to check the result.
                        List<Session> sessions = sessionReadResponse.getSessions();
                        Log.i(TAG, "Session read was successful. Number of returned sessions is: "
                                + sessions.size());

                        for (Session session : sessions) {
                            // Process the session
                            if (session.getActivity().contains("running")) {
                                logSession(session);
                                // Process the data sets for this session
                                List<DataSet> dataSets = sessionReadResponse.getDataSet(session);
                                for (DataSet dataSet : dataSets) {
                                    logDataSet(dataSet);
                                }
                                fitData.setText("Total Distance: " + decimalFormat.format(metersToMiles(distance)) + " miles\nTotal Steps: " + steps);
                            }
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        fitData.append("Failed to read session" + e);
                        Log.i(TAG, "Failed to read session");
                    }
                });
        // [END read_session]
    }

    private Task<DataReadResponse> readHistoryData(long startTime, long endTime) {
        DataReadRequest readRequest = queryFitnessData(startTime, endTime);

        return Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(readRequest)
                .addOnSuccessListener(
                        new OnSuccessListener<DataReadResponse>() {
                            @Override
                            public void onSuccess(DataReadResponse dataReadResponse) {
                                logDataResponse(dataReadResponse);
                            }
                        }
                );
    }

    public static double metersToMiles(double meters) {
        return meters / 1609.355;
    }

    public static DataReadRequest queryFitnessData(long endTime, long startTime) {
        DateFormat dateFormat = getDateInstance();

        Log.i(TAG, "Start of data Search: " + dateFormat.format(startTime));
        Log.i(TAG, "End of data search: " + dateFormat.format(endTime));

        DataReadRequest readRequest =
                new DataReadRequest.Builder()
                        .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                        .bucketByTime(1, TimeUnit.DAYS)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build();

        return readRequest;
    }

    public static void logDataResponse(DataReadResponse dataReadResult) {
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of buckets of DataSets is: " + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    logDataSet(dataSet);
                }
            }
            fitData.append("Total Distance" + distance);
        }
    }

    private static void logDataSet(DataSet dataSet) {
        Log.i(TAG, "Data returned from Data type: " + dataSet.getDataType());
        for (DataPoint dp : dataSet.getDataPoints()) {
            DateFormat dateFormat = getTimeInstance();
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for (Field field : dp.getDataType().getFields()) {
                if (field.getName().contains("distance"))
                    distance += dp.getValue(field).asFloat();
                if (field.getName().contains("steps")) {
                    Log.d(TAG, "\t\t\nHit steps");
                    steps = Double.parseDouble(dp.getValue(field).toString());
                }

                Log.i(TAG, "\tField: " + field.getName() +
                        " Value: " + dp.getValue(field));
            }
            Log.i(TAG, "Total Distance: " + metersToMiles(distance) + " miles\n Total Steps: " + steps);
        }
    }

    private void logSession(Session session) {
        DateFormat dateFormat = getTimeInstance();

        Log.i(TAG, "Data returned for Session: " + session.getName()
                + "\n\tDescription: " + session.getDescription()
                + "\n\tActivity: " + session.getActivity()
                + "\n\tStart: " + dateFormat.format(session.getStartTime(TimeUnit.MILLISECONDS))
                + "\n\tEnd: " + dateFormat.format(session.getEndTime(TimeUnit.MILLISECONDS)));
    }

//    private void readAllRequests () {
//        Calendar cal = Calendar.getInstance();
//        Date now = new Date();
//        cal.setTime(now);
//        long endTime = cal.getTimeInMillis();
//        cal.add(Calendar.WEEK_OF_YEAR, -1);
//        long startTime = cal.getTimeInMillis();
//
//        SessionReadRequest readRequest = new SessionReadRequest.Builder()
//                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
//                .read(DataType.TYPE_DISTANCE_CUMULATIVE)
//                .setSessionName(SAMPLE_SESSION_NAME)
//                .build();
//
//        PendingResult<SessionReadResult> sessionReadResult =
//                Fitness.getSessionsClient().readSession();
//    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        System.out.println("onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                Log.i(TAG, "Must grant permissions");

            } else {
                // Permission denied.

                // In this Activity we've chosen to notify the user that they
                // have rejected a core permission for the app since it makes the Activity useless.
                // We're communicating this message in a Snackbar since this is a sample app, but
                // core permissions would typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                fitData.setText("Permission Denied");
            }
        }
    }
}