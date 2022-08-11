package com.app.dorav4.adapters;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.app.dorav4.R;
import com.app.dorav4.holders.CommentsViewHolder;
import com.app.dorav4.models.Comments;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class CommentsAdapters extends RecyclerView.Adapter<CommentsViewHolder>{
    Context context;
    List<Comments> commentsList;

    String currentUserId, reportId;

    public CommentsAdapters(Context context, List<Comments> commentsList, String currentUserId, String reportId) {
        this.context = context;
        this.commentsList = commentsList;
        this.currentUserId = currentUserId;
        this.reportId = reportId;
    }

    @NonNull
    @Override
    public CommentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.single_view_comments, parent, false);
        return new CommentsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentsViewHolder holder, int position) {
        // Fetch value from the list
        String commentId = commentsList.get(position).getCommentId();
        String comment = commentsList.get(position).getComment();
        String name = commentsList.get(position).getName();
        String date = commentsList.get(position).getDate();
        String profilePicture = commentsList.get(position).getProfilePicture();
        String userId = commentsList.get(position).getUserId();

        // Set corresponding values to the layout
        holder.tvCommentComment.setText(comment);
        holder.tvCommentName.setText(name);
        holder.tvCommentDate.setText(calculateTime(date));
        Picasso.get().load(profilePicture).into(holder.ivCommentProfile);

        // Hide ivMore if current user's id doesn't match the report's user id
        if (!userId.equals(currentUserId)) {
            holder.ivMore.setVisibility(View.INVISIBLE);
        }

        // ivMore OnClickListener
        holder.ivMore.setOnClickListener(v -> showCommentOption(holder.ivMore, commentId));
    }

    @Override
    public int getItemCount() {
        return commentsList.size();
    }

    // Show comment options
    private void showCommentOption(ImageView ivMore, String commentId) {
        PopupMenu popupMenu = new PopupMenu(context, ivMore, Gravity.END);
        popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");

        // Menu OnClickListener
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            // Delete is clicked
            if (id == 0) {
                // Confirmation dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setCancelable(true);
                builder.setTitle("Delete Comment");
                builder.setMessage("Are you sure you want to delete your comment?");
                builder.setPositiveButton("Delete", (dialog, which) -> deleteComment(commentId));
                builder.setNegativeButton("Cancel", (dialog, which) -> { });
                builder.create().show();
            }
            return false;
        });
        popupMenu.show();
    }

    // Delete comment
    private void deleteComment(String commentId) {
        // Delete comment from the database
        DatabaseReference commentsReference = FirebaseDatabase.getInstance().getReference("Reports").child(reportId);
        commentsReference.child("Comments").child(commentId).removeValue();

        // Update comment count
        commentsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String comments = String.valueOf(snapshot.child("comments").getValue());
                int commentAdded = Integer.parseInt(comments) - 1;
                commentsReference.child("comments").setValue(String.valueOf(commentAdded));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    // Convert time into "time ago"
    private String calculateTime (String strDate) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy hh:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("GMT-4"));
        try {
            Date date = format.parse(strDate);
            assert date != null;
            return (String) DateUtils.getRelativeTimeSpanString(date.getTime(), Calendar.getInstance().getTimeInMillis(), DateUtils.MINUTE_IN_MILLIS);
        } catch (ParseException e){
            e.printStackTrace();
        }
        return null;
    }
}