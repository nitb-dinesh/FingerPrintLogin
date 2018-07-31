package com.inducesmile.androidfingerprintlogin;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.google.gson.Gson;

public class UserProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        setTitle("Profile Page");

        String userBio = getIntent().getExtras().getString("USER_BIO");
        Gson gson = ((CustomApplication)getApplication()).getGsonObject();

        UserObject mUserObject = gson.fromJson(userBio, UserObject.class);
        String bio = mUserObject.getUsername() + "\n" +
                mUserObject.getAddress() + "\n" +
                mUserObject.getEmail() + "\n" +
                mUserObject.getPhone() + "\n" +
                "";
               // mUserObject.getPassword();

        TextView userTextValue = (TextView)findViewById(R.id.user_bio);
        userTextValue.setText(bio);
    }
}
