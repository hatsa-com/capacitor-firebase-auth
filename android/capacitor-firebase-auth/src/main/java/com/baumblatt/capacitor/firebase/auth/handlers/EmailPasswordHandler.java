package com.baumblatt.capacitor.firebase.auth.handlers;

import android.content.Intent;
import androidx.annotation.NonNull;

import com.baumblatt.capacitor.firebase.auth.CapacitorFirebaseAuth;
import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class EmailPasswordHandler implements ProviderHandler {
    private static final String HANDLER_TAG = "EmailPasswordHandler";

    private CapacitorFirebaseAuth plugin;
    private final FirebaseAuth mAuth;
    private boolean isNewUser;

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

        String email = call.getString("email");
        String password = call.getString("password");

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        AuthResult authResult = task.getResult();
                        isNewUser = authResult.getAdditionalUserInfo().isNewUser();

                        plugin.handleAuthCredentials(authResult.getCredential());
                    } else {
                        plugin.handleFailure(
                            "Can not sign in with email and password",
                            task.getException()
                        );
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    plugin.handleFailure("Can not sign in with email and password", e);
                }
            });
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
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            return;
        }

        jsResult.put("isNewUser", isNewUser);
    }

    @Override
    public void signOut() {
        mAuth.signOut();
    }
}
