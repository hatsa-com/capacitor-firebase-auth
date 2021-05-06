package com.baumblatt.capacitor.firebase.auth.handlers;

import android.content.Intent;
import com.baumblatt.capacitor.firebase.auth.CapacitorFirebaseAuth;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;


public class EmailPasswordHandler implements ProviderHandler {
    private static final String HANDLER_TAG = "EmailPasswordHandler";

    private CapacitorFirebaseAuth plugin;
    private final FirebaseAuth mAuth;

    public EmailPasswordHandler(FirebaseAuth mAuth){
        this.mAuth = mAuth;
    }

    @Override
    public void init(CapacitorFirebaseAuth plugin) {
        this.plugin = plugin;
    }

    @Override
    public void signIn(PluginCall call) {
        if (!call.getData().has("data")) {
            call.reject("The auth data is required");
            return;
        }

        JSObject data = call.getObject("data", new JSObject());

        String email = data.getString("email", null);
        String password = data.getString("password", null);

        if (email == null || email.equalsIgnoreCase("null") 
            || password == null || password.equalsIgnoreCase("null")) {
            call.reject("Email and password are required");
            return;
        }

        AuthCredential authCredential = EmailAuthProvider.getCredential(email, password);
        plugin.handleAuthCredentials(authCredential);
    }

    @Override
    public int getRequestCode() {
        return 0;
    }

    @Override
    public void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
    }

    @Override
    public boolean isAuthenticated() {
        return mAuth.getCurrentUser() != null;
    }

    @Override
    public void fillResult(JSObject jsResult) {
        // Nothing to add here️
    }

    @Override
    public void signOut() {
        mAuth.signOut();
    }
}
