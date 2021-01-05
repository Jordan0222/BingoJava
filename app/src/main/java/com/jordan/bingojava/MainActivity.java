package com.jordan.bingojava;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.Group;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements FirebaseAuth.AuthStateListener, View.OnClickListener {

    private static final int RC_SIGN_IN = 100;
    private FirebaseAuth auth;
    private TextView nickText;
    private ImageView avatar;
    private Group groupAvatars;
    int[] avatarIds = {R.drawable.avatar_0, R.drawable.avatar_1, R.drawable.avatar_2, R.drawable.avatar_3,
            R.drawable.avatar_4,R.drawable.avatar_5,R.drawable.avatar_6};
    private FloatingActionButton fab;
    private Member member;
    private RecyclerView recycler;
    private FirebaseRecyclerAdapter<GameRoom, RoomHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViews();

        auth = FirebaseAuth.getInstance();
    }

    private void findViews() {
        nickText = findViewById(R.id.nickname);
        nickText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNickDialog(nickText.getText().toString());
            }
        });

        avatar = findViewById(R.id.avatar);
        avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                groupAvatars.setVisibility(
                        groupAvatars.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            }
        });

        groupAvatars = findViewById(R.id.group_avatars);
        groupAvatars.setVisibility(View.GONE);

        findViewById(R.id.avatar_0).setOnClickListener(this);
        findViewById(R.id.avatar_1).setOnClickListener(this);
        findViewById(R.id.avatar_2).setOnClickListener(this);
        findViewById(R.id.avatar_3).setOnClickListener(this);
        findViewById(R.id.avatar_4).setOnClickListener(this);
        findViewById(R.id.avatar_5).setOnClickListener(this);
        findViewById(R.id.avatar_6).setOnClickListener(this);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText roomEdit = new EditText(MainActivity.this);
                roomEdit.setText("Welcome");
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Game Room")
                        .setMessage("Please enter your roomTitle")
                        .setView(roomEdit)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String roomTitle = roomEdit.getText().toString();
                                GameRoom room = new GameRoom(roomTitle, member);

                                FirebaseDatabase.getInstance()
                                        .getReference("rooms")
                                        .push().setValue(room);
                            }
                        }).show();
            }
        });
        // RecyclerView
        recycler = findViewById(R.id.recycler);
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        Query query = FirebaseDatabase.getInstance()
                .getReference("rooms")
                .limitToLast(30);

        FirebaseRecyclerOptions<GameRoom> options =
                new FirebaseRecyclerOptions.Builder<GameRoom>()
                        .setQuery(query, GameRoom.class)
                        .build();

        adapter = new FirebaseRecyclerAdapter<GameRoom, RoomHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RoomHolder holder, int position, @NonNull GameRoom model) {
                holder.image.setImageResource(avatarIds[model.init.avatarId]);
                holder.titleText.setText(model.title);
            }

            @NonNull
            @Override
            public RoomHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(MainActivity.this)
                        .inflate(R.layout.room_row, parent, false);
                return new RoomHolder(view);
            }
        };
        recycler.setAdapter(adapter);
    }

    public class RoomHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView titleText;
        public RoomHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.room_image);
            titleText = itemView.findViewById(R.id.room_title);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        auth.addAuthStateListener(this);
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        auth.removeAuthStateListener(this);
        adapter.stopListening();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_menu_signout:
                auth.signOut();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            startActivityForResult(
                    AuthUI.getInstance().createSignInIntentBuilder()
                            .setAvailableProviders(Arrays.asList(
                                    new AuthUI.IdpConfig.EmailBuilder().build(),
                                    new AuthUI.IdpConfig.GoogleBuilder().build()
                            ))
                            .setIsSmartLockEnabled(false)
                            .build()
                    , RC_SIGN_IN);
        } else {
            final String displayName = user.getDisplayName();
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("displayName")
                    .setValue(displayName);
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("uid")
                    .setValue(user.getUid());

            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            member = snapshot.getValue(Member.class);
                            if (member != null) {
                                if (member.nickname != null) {
                                    nickText.setText(member.nickname);
                                } else {
                                    showNickDialog(displayName);
                                }
                                avatar.setImageResource(avatarIds[member.avatarId]);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

            /*FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(user.getUid())
                    .child("nickname")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.getValue() != null) {
                                String nickname = (String) snapshot.getValue();
                            } else {
                                showNickDialog(displayName);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });*/
        }
    }

    private void showNickDialog(String displayName) {
        final EditText nickEdit = new EditText(this);
        nickEdit.setText(displayName);
        new AlertDialog.Builder(this)
                .setTitle("Your nickname")
                .setMessage("Please enter your nickname")
                .setView(nickEdit)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String nickname = nickEdit.getText().toString();
                        FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(auth.getUid())
                                .child("nickname")
                                .setValue(nickname);
                    }
                }).show();
    }

    @Override
    public void onClick(View view) {
        if (view instanceof ImageView) {
            int selectedID = 0;
            switch (view.getId()) {
                case R.id.avatar_0:
                    selectedID = 0;
                    break;
                case R.id.avatar_1:
                    selectedID = 1;
                    break;
                case R.id.avatar_2:
                    selectedID = 2;
                    break;
                case R.id.avatar_3:
                    selectedID = 3;
                    break;
                case R.id.avatar_4:
                    selectedID = 4;
                    break;
                case R.id.avatar_5:
                    selectedID = 5;
                    break;
                case R.id.avatar_6:
                    selectedID = 6;
                    break;
            }
            avatar.setImageResource(avatarIds[selectedID]);
            groupAvatars.setVisibility(View.GONE);
            FirebaseDatabase.getInstance().getReference("users")
                    .child(auth.getUid())
                    .child("avatarId")
                    .setValue(selectedID);
        }
    }
}