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
        if (!call.getData().has("email") || !call.getData().has("password")) {
            plugin.handleFailure("Please set keys: email, password", null);
            return;
        }

        final String email = call.getString("email");
        final String password = call.getString("password");

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
