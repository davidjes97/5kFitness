package com.example.a5kfitness;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.time.LocalDate;
import java.util.Calendar;

public class FutureWorkoutsActivity extends AppCompatActivity {

    private View workouts;
    private static final String TAG = "FUTURE_TAG";
    private int[] dayRange = new int[10];
    private DatabaseReference goalOfTheDay;
    private TextView workoutsText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_future);

        getTodaysGoal();


    }

    private void getTodaysGoal() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("5kTrainingPlan");
        Calendar cal = Calendar.getInstance();
        LocalDate tempDate = null;
        LocalDate textUse = null;
        int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
        workouts = findViewById(R.id.futureWorkouts);
        workoutsText = findViewById(R.id.workouts);
        String text = "";

         determineRange(dayOfMonth);

         for(int i = 0; i < dayRange.length; i++){
            goalOfTheDay = myRef.child(String.valueOf(dayRange[i])).child("distance");
            textUse = tempDate.now().plusDays(i);

             final LocalDate finalTextUse = textUse;
             goalOfTheDay.addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String goal = "Goal Distance: " + dataSnapshot.getValue() + " miles";
//                    workouts.autofill(AutofillValue.forText(goal));
                    workoutsText.append("\n\nDate: " + finalTextUse.toString() +goal);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    public void determineRange(int today) {
        int tomorrow = today + 1;

        for(int i = 0; i < 10; i++){
            dayRange[i] = tomorrow + i;
        }
    }
}
