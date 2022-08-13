package com.app.dorav4.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.dorav4.R;
import com.app.dorav4.adapters.UpvotesAdapter;
import com.app.dorav4.models.Upvotes;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UpvotesActivity extends AppCompatActivity {
    TextView tvToolbarHeader;
    RecyclerView recyclerView;
    ImageView ivBack;
    ConstraintLayout emptyView;

    List<Upvotes> upvotesList;
    UpvotesAdapter upvotesAdapter;

    DatabaseReference upvotesReference, usersReference;

    String reportId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upvotes);

        tvToolbarHeader = findViewById(R.id.tvToolbarHeader);
        ivBack = findViewById(R.id.ivBack);
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        // Set toolbar header
        tvToolbarHeader.setText("Upvotes");

        // Get reportId from previous activity
        Intent intent = getIntent();
        reportId = intent.getStringExtra("reportId");

        upvotesList = new ArrayList<>();

        upvotesReference = FirebaseDatabase.getInstance().getReference("Reports").child(reportId).child("Upvotes").child(reportId);
        usersReference = FirebaseDatabase.getInstance().getReference("Users");

        // ivBack OnClickListener
        ivBack.setOnClickListener(v -> finish());

        loadUpvotes();
    }

    // Get the list of all users who upvoted
    private void loadUpvotes() {
        upvotesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                upvotesList.clear();

                // Get the uid of the user who upvoted
                for (DataSnapshot ds: snapshot.getChildren()) {
                    String uid = ds.getRef().getKey();

                    // Get the user's details based on their uid
                    usersReference.orderByKey().equalTo(uid).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds: snapshot.getChildren()) {
                                // Add the user's info into the list
                                Upvotes upvotes = ds.getValue(Upvotes.class);
                                upvotesList.add(upvotes);
                                upvotesAdapter = new UpvotesAdapter(UpvotesActivity.this, upvotesList);
                                recyclerView.setAdapter(upvotesAdapter);
                            }

                            // Show emptyView if recyclerView is empty
                            if (upvotesList.size() == 0) {
                                recyclerView.setVisibility(View.GONE);
                            } else {
                                recyclerView.setVisibility(View.VISIBLE);
                                emptyView.setVisibility(View.GONE);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}