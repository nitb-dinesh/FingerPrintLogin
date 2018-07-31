package com.inducesmile.androidfingerprintlogin;

import android.Manifest;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = LoginActivity.class.getSimpleName();

    private static FingerprintManager fingerprintManager;
    private KeyguardManager keyguardManager;
    private KeyStore keyStore;
    private KeyGenerator keyGenerator;
    private Cipher cipher;
    private static FingerprintManager.CryptoObject cryptoObject;

    private  static FingerprintHandler fingerprintHandler;

    private static final String FINGERPRINT_KEY = "key_name";

    private static final int REQUEST_USE_FINGERPRINT = 300;


    protected static boolean isFirst = false;
    protected static Gson mGson;
    protected static CustomSharedPreference mPref;
    private static UserObject mUser;
    private static String userString;
    private static EditText verfyET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setTitle("Demo Fingerprint Verify");

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mGson = ((CustomApplication) getApplication()).getGsonObject();
        mPref = ((CustomApplication) getApplication()).getShared();

        fingerprintHandler = new FingerprintHandler(this);

        fingerprintManager = (FingerprintManager) getSystemService(FINGERPRINT_SERVICE);
        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

        // check support for android fingerprint on device
        checkDeviceFingerprintSupport();
        //generate fingerprint keystore
        generateFingerprintKeyStore();
        //instantiate Cipher class
        Cipher mCipher = instantiateCipher();
        if (mCipher != null) {
            cryptoObject = new FingerprintManager.CryptoObject(mCipher);
        }

        verfyET = findViewById(R.id.verfyET);
        if (isFirst) {
            verfyET.setVisibility(View.GONE);
        }

        ImageView fingerprintImage = (ImageView) findViewById(R.id.fingerprint_image);

        fingerprintHandler.completeFingerAuthentication(fingerprintManager, cryptoObject);
        fingerprintImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                fingerprintHandler.completeFingerAuthentication(fingerprintManager, cryptoObject);

            }
        });

        //check for fingerprint hardware
        // Check if we're running on Android 6.0 (M) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Fingerprint API only available on from Android 6.0 (M)
            FingerprintManager fingerprintManager = (FingerprintManager) getApplicationContext().getSystemService(Context.FINGERPRINT_SERVICE);
            if (!fingerprintManager.isHardwareDetected()) {
                // Device doesn't support fingerprint authentication
            } else if (!fingerprintManager.hasEnrolledFingerprints()) {
                // User hasn't enrolled any fingerprints to authenticate with
            } else {
                // Everything is ready for fingerprint authentication
            }
        }

    }

    private void checkDeviceFingerprintSupport() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.USE_FINGERPRINT}, REQUEST_USE_FINGERPRINT);
        } else {
            if (!fingerprintManager.isHardwareDetected()) {
                Toast.makeText(LoginActivity.this, "Fingerprint is not supported in this device", Toast.LENGTH_LONG).show();
            }
            if (!fingerprintManager.hasEnrolledFingerprints()) {
                Toast.makeText(LoginActivity.this, "Fingerprint not yet configured", Toast.LENGTH_LONG).show();
            }
            if (!keyguardManager.isKeyguardSecure()) {
                Toast.makeText(LoginActivity.this, "Screen lock is not secure and enable", Toast.LENGTH_LONG).show();
            }
            return;
        }
    }

    private void generateFingerprintKeyStore() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        try {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        try {
            keyGenerator.init(new KeyGenParameterSpec.Builder(FINGERPRINT_KEY, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
        keyGenerator.generateKey();
    }

    private Cipher instantiateCipher() {
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(FINGERPRINT_KEY, null);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | UnrecoverableKeyException |
                CertificateException | IOException | KeyStoreException | InvalidKeyException e) {
            throw new RuntimeException("Failed to instantiate Cipher class");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_USE_FINGERPRINT) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // check support for android fingerprint on device
                checkDeviceFingerprintSupport();
                //generate fingerprint keystore
                generateFingerprintKeyStore();
                //instantiate Cipher class
                Cipher mCipher = instantiateCipher();
                if (mCipher != null) {
                    cryptoObject = new FingerprintManager.CryptoObject(mCipher);
                }
            } else {
                Toast.makeText(this, R.string.permission_refused, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, getString(R.string.Unknown_permission_request), Toast.LENGTH_LONG).show();
        }
    }

    public static class FingerprintHandler extends FingerprintManager.AuthenticationCallback {

        private static final String TAG = FingerprintHandler.class.getSimpleName();

        private Context context;

        public FingerprintHandler(Context context) {
            this.context = context;

        }

        @Override
        public void onAuthenticationError(int errorCode, CharSequence errString) {
            super.onAuthenticationError(errorCode, errString);

            Log.d(TAG, "Error message " + errorCode + ": " + errString);
            Toast.makeText(context, context.getString(R.string.authenticate_fingerprint), Toast.LENGTH_LONG).show();
           // fingerprintHandler.completeFingerAuthentication(fingerprintManager, cryptoObject);
        }

        @Override
        public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
            super.onAuthenticationHelp(helpCode, helpString);

            Toast.makeText(context, R.string.auth_successful, Toast.LENGTH_LONG).show();
           // fingerprintHandler.completeFingerAuthentication(fingerprintManager, cryptoObject);
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            fingerprintHandler.completeFingerAuthentication(fingerprintManager, cryptoObject);

            userString = mPref.getUserData();
            mUser = mGson.fromJson(userString, UserObject.class);
            if (mUser != null) {
              //  Toast.makeText(context, context.getString(R.string.auth_successful), Toast.LENGTH_LONG).show();
                if (mUser.isLoginOption()) {
                    // login with fingerprint and password
                    showPasswordAuthentication(context);
                } else {
                    // login with only fingerprint
                    String name = verfyET.getText().toString();

                    if (isFirst) {
                        Toast.makeText(context, context.getString(R.string.auth_successful), Toast.LENGTH_LONG).show();
                        Intent userIntent = new Intent(context, UserProfileActivity.class);
                        userIntent.putExtra("USER_BIO", userString);
                        context.startActivity(userIntent);
                        isFirst = false;
                    } else if (name.equals(mUser.getUsername())) {


                            Toast.makeText(context, name+": You Verified", Toast.LENGTH_LONG).show();

                    }else  if (TextUtils.isEmpty(name)) {
                         Toast.makeText(context, "Please Enter User Name", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(context, "User Name not Verified", Toast.LENGTH_LONG).show();

                    }

                }
            } else {
                Toast.makeText(context, "You must register before login with fingerprint", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
            //fingerprintHandler.completeFingerAuthentication(fingerprintManager, cryptoObject);
        }

        public void completeFingerAuthentication(FingerprintManager fingerprintManager, FingerprintManager.CryptoObject cryptoObject) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            try {
                fingerprintManager.authenticate(cryptoObject, new CancellationSignal(), 0, this, null);
            } catch (SecurityException ex) {
                Log.d(TAG, "An error occurred:\n" + ex.getMessage());
            } catch (Exception ex) {
                Log.d(TAG, "An error occurred\n" + ex.getMessage());
            }
        }
    }

    private static void showPasswordAuthentication(Context context) {
        final Dialog openDialog = new Dialog(context);
        openDialog.setContentView(R.layout.password_layout);
        openDialog.setTitle("Enter Password");
        final EditText passwordDialog = (EditText) openDialog.findViewById(R.id.password);
        Button loginWithPasswordButton = (Button) openDialog.findViewById(R.id.login_button);
        loginWithPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String authPassword = passwordDialog.getText().toString();
                if (TextUtils.isEmpty(authPassword)) {
                    Toast.makeText(view.getContext(), "Password field must be filled", Toast.LENGTH_LONG).show();
                    return;
                }
                if (mUser.getPassword().equals(authPassword)) {
                    Intent userIntent = new Intent(view.getContext(), UserProfileActivity.class);
                    userIntent.putExtra("USER_BIO", userString);
                    view.getContext().startActivity(userIntent);
                } else {
                    Toast.makeText(view.getContext(), "Incorrect password! Try again", Toast.LENGTH_LONG).show();
                    return;
                }
                openDialog.dismiss();
            }
        });
        openDialog.show();
    }

}
