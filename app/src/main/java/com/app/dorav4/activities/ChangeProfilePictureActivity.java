package com.app.dorav4.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.app.dorav4.R;
import com.github.drjacky.imagepicker.ImagePicker;
import com.github.drjacky.imagepicker.constant.ImageProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;

public class ChangeProfilePictureActivity extends AppCompatActivity {
    ImageView ivBack, ivProfilePicture;
    Button btnSaveProfile;

    FirebaseAuth mAuth;
    FirebaseUser mUser;
    DatabaseReference usersReference, reportsReference;
    StorageReference storageReference;

    ProgressDialog progressDialog;

    Uri photoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_profile_picture);

        ivBack = findViewById(R.id.ivBack);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        progressDialog = new ProgressDialog(ChangeProfilePictureActivity.this);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        usersReference = FirebaseDatabase.getInstance().getReference().child("Users");
        reportsReference = FirebaseDatabase.getInstance().getReference().child("Reports");
        storageReference = FirebaseStorage.getInstance().getReference().child("ProfilePictures");

        Intent intent = getIntent();
        Picasso.get().load(intent.getStringExtra("profilePicture")).into(ivProfilePicture);

        // ivBack OnClickListener
        ivBack.setOnClickListener(v -> finish());

        // ivProfilePicture OnClickListener
        ivProfilePicture.setOnClickListener(v -> chooseImage());

        // btnSaveProfile OnClickListener
        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    // Save changes
    private void saveProfile() {
        boolean isImageEmpty = false;

        // Validate profile picture
        if (photoUri != null && !photoUri.equals(Uri.EMPTY)) {
            isImageEmpty = true;
        } else {
            Toast.makeText(ChangeProfilePictureActivity.this, "Please upload a profile picture", Toast.LENGTH_SHORT).show();
        }

        if (isImageEmpty) {
            // ProgressDialog
            progressDialog.setTitle("Update Profile");
            progressDialog.setMessage("Saving your profile");
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();

            // Upload new image to cloud storage
            storageReference.child(mUser.getUid()).putFile(photoUri).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Get image uri
                    storageReference.child(mUser.getUid()).getDownloadUrl().addOnSuccessListener(uri -> {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("profilePicture", uri.toString());

                        // Save new image uri to "Users" database
                        usersReference.child(mUser.getUid()).updateChildren(hashMap).addOnSuccessListener(o -> {
                            // Save new image uri to "Reports" database
                            Query query = reportsReference.orderByChild("userId").equalTo(mUser.getUid());
                            query.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot ds: snapshot.getChildren()) {
                                        String child = ds.getKey();

                                        if (child != null) {
                                            if (snapshot.child(child).hasChild("profilePicture")) {
                                                snapshot.getRef().child(child).child("profilePicture").setValue(uri.toString());
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {

                                }
                            });

                            // Save new image to "Comments" database
                            reportsReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    for (DataSnapshot ds: snapshot.getChildren()) {
                                        String child = ds.getKey();

                                        if (child != null) {
                                            if (snapshot.child(child).hasChild("Comments")) {
                                                String child1 = String.valueOf(snapshot.child(child).getKey());
                                                Query query = reportsReference.child(child1).child("Comments").orderByChild("userId").equalTo(mUser.getUid());
                                                query.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        for (DataSnapshot ds: snapshot.getChildren()) {
                                                            String child = ds.getKey();

                                                            if (child != null) {
                                                                if (snapshot.child(child).hasChild("profilePicture")) {
                                                                    snapshot.getRef().child(child).child("profilePicture").setValue(uri.toString());
                                                                }
                                                            }
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {
                                                    }
                                                });
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError error) {
                                }
                            });

                            progressDialog.dismiss();
                            Toast.makeText(ChangeProfilePictureActivity.this, "Profile successfully updated", Toast.LENGTH_SHORT).show();
                            finish();
                        }).addOnFailureListener(e -> {
                            progressDialog.dismiss();
                            Toast.makeText(ChangeProfilePictureActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        });
                    });
                }
            });
        }
    }

    // Select image from gallery
    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            photoUri = result.getData().getData();
            ivProfilePicture.setImageURI(photoUri);
        } else if (result.getResultCode() == ImagePicker.RESULT_ERROR) {
            ImagePicker.Companion.getError(result.getData());
        }
    });

    // Choose image from gallery or camera
    private void chooseImage() {
        ImagePicker.Companion.with(this)
                .cropSquare()
                .provider(ImageProvider.BOTH)
                .createIntentFromDialog(new Function1(){
                    public Object invoke(Object var1) {
                        this.invoke((Intent) var1);
                        return Unit.INSTANCE;
                    }

                    public final void invoke(@NotNull Intent it) {
                        Intrinsics.checkNotNullParameter(it, "it");
                        launcher.launch(it);
                    }
                });
    }
}