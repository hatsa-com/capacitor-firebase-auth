package com.baumblatt.capacitor.firebase.auth;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;
import android.util.SparseArray;

import com.baumblatt.capacitor.firebase.auth.handlers.EmailPasswordHandler;
import com.baumblatt.capacitor.firebase.auth.handlers.FacebookProviderHandler;
import com.baumblatt.capacitor.firebase.auth.handlers.GoogleProviderHandler;
import com.baumblatt.capacitor.firebase.auth.handlers.PhoneProviderHandler;
import com.baumblatt.capacitor.firebase.auth.handlers.ProviderHandler;
import com.baumblatt.capacitor.firebase.auth.handlers.TwitterProviderHandler;
import com.getcapacitor.CapConfig;
import com.getcapacitor.Config;
import com.getcapacitor.JSObject;
import com.getcapacitor.NativePlugin;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.twitter.sdk.android.core.TwitterAuthConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@NativePlugin(requestCodes = {
    GoogleProviderHandler.RC_GOOGLE_SIGN_IN,
    TwitterAuthConfig.DEFAULT_AUTH_REQUEST_CODE,
    FacebookProviderHandler.RC_FACEBOOK_LOGIN
})
public class CapacitorFirebaseAuth extends Plugin {
    public static final String CONFIG_KEY_PREFIX = "plugins.CapacitorFirebaseAuth.";
    private static final String PLUGIN_TAG = "CapacitorFirebaseAuth";

    private FirebaseAuth mAuth;
    private final Map<String, ProviderHandler> providerHandlers = new HashMap<>();
    private final SparseArray<ProviderHandler> providerHandlerByRC = new SparseArray<>();
    private CapConfig config;

    private boolean nativeAuth = false;

    private boolean shouldLinkProvider = false;

    public void load() {
        super.load();
        this.config = getBridge().getConfig();

        getConfigValue("nada");

        String[] providers = config.getArray(CONFIG_KEY_PREFIX + "providers", new String[0]);
        this.nativeAuth = config.getBoolean(CONFIG_KEY_PREFIX + "nativeAuth", false);
        String languageCode = config.getString(CONFIG_KEY_PREFIX + "languageCode", "en");

        // FirebaseApp is not initialized in this process - Error #1
        Log.d(PLUGIN_TAG, "Verifying if the default FirebaseApp was initialized.");
        if (FirebaseApp.getApps(this.getContext()).size() == 0) {
            Log.d(PLUGIN_TAG, "Initializing the default FirebaseApp ");
            FirebaseApp.initializeApp(this.getContext());
        }

        Log.d(PLUGIN_TAG, "Retrieving FirebaseAuth instance");
        this.mAuth = FirebaseAuth.getInstance();
        this.mAuth.setLanguageCode(languageCode);

        for (String provider : providers) {
            if (provider.equalsIgnoreCase(getContext().getString(R.string.google_provider_id))) {
                Log.d(PLUGIN_TAG, "Initializing Google Provider");
                this.providerHandlers.put(provider, new GoogleProviderHandler());
                this.providerHandlers.get(provider).init(this);
                Log.d(PLUGIN_TAG, "Google Provider Initialized");
            } else if (provider.equalsIgnoreCase(getContext().getString(R.string.twitter_provider_id))) {
                Log.d(PLUGIN_TAG, "Initializing Twitter Provider");
                this.providerHandlers.put(provider, new TwitterProviderHandler());
                this.providerHandlers.get(provider).init(this);
                Log.d(PLUGIN_TAG, "Twitter Provider Initialized");
            } else if (provider.equalsIgnoreCase(getContext().getString(R.string.facebook_provider_id))) {
                Log.d(PLUGIN_TAG, "Initializing Facebook Provider");
                this.providerHandlers.put(provider, new FacebookProviderHandler());
                this.providerHandlers.get(provider).init(this);
                Log.d(PLUGIN_TAG, "Facebook Provider Initialized");
            } else if (provider.equalsIgnoreCase(getContext().getString(R.string.phone_provider_id))) {
                Log.d(PLUGIN_TAG, "Initializing Phone Provider");
                this.providerHandlers.put(provider, new PhoneProviderHandler());
                this.providerHandlers.get(provider).init(this);
                Log.d(PLUGIN_TAG, "Phone Provider Initialized");
            } else if (provider.equalsIgnoreCase(getContext().getString(R.string.email_password_provider_id))) {
                Log.d(PLUGIN_TAG, "Initializing Email Provider");
                this.providerHandlers.put(provider, new EmailPasswordHandler(mAuth));
                this.providerHandlers.get(provider).init(this);
                Log.d(PLUGIN_TAG, "Email Provider Initialized");
            }
        }

        for (ProviderHandler providerHandler : this.providerHandlers.values()) {
            this.providerHandlerByRC.put(providerHandler.getRequestCode(), providerHandler);
        }
    }

    @PluginMethod()
    public void signIn(PluginCall call) {
        shouldLinkProvider = false;

        if (!call.getData().has("providerId")) {
            call.reject("The provider id is required");
            return;
        }

        ProviderHandler handler = this.getProviderHandler(call);

        if (handler == null) {
            Log.w(PLUGIN_TAG, "Provider not supported");
            call.reject("The provider is disable or unsupported");
        } else {
            if (handler.isAuthenticated()) {
                JSObject jsResult = this.build(call, null);
                call.success(jsResult);
            } else {
                this.saveCall(call);
                handler.signIn(call);
            }
        }
    }

    @PluginMethod()
    public void signInAndLink(PluginCall call) {
        shouldLinkProvider = true;

        if (!call.getData().has("providerId")) {
            call.reject("The provider id is required");
            return;
        }

        ProviderHandler handler = this.getProviderHandler(call);

        if (handler == null) {
            Log.w(PLUGIN_TAG, "Provider not supported");
            call.reject("The provider is disable or unsupported");
        } else {
            this.saveCall(call);
            handler.signIn(call);
        }
    }

    @PluginMethod()
    public void unlink(final PluginCall call) {
        if (!call.getData().has("providerId")) {
            call.reject("The provider id is required");
            return;
        }

        String providerId = call.getString("providerId", null);
        FirebaseUser currentUser = this.mAuth.getCurrentUser();

        if (currentUser == null) {
            call.reject("The user is not signed in");
        } else {
            currentUser.unlink(providerId)
                .addOnCompleteListener(this.getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            call.success();
                        }
                    }
                });
        }
    }

    @PluginMethod()
    public void signInWithCustomToken(final PluginCall call) {
        if (!call.getData().has("customToken")) {
            call.reject("The customToken is required");
            return;
        }

        final String customToken = call.getString("customToken", null);

        this.mAuth.signInWithCustomToken(customToken)
            .addOnCompleteListener(this.getActivity(), new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(PLUGIN_TAG, "Custom Token Sign In succeed.");
                        FirebaseUser user = mAuth.getCurrentUser();
                        AuthResult authResult = task.getResult();

                        if (user == null) {
                            Log.w(PLUGIN_TAG, "Ops, no Firebase user after Sign In with Custom Token succeed.");
                            call.reject("Ops, no Firebase user after Sign In with Custom Token succeed");
                        } else {
                            JSObject jsResult = new JSObject();

                            jsResult.put("callbackId", call.getCallbackId());
                            jsResult.put("uid", authResult.getUser().getUid());
                            jsResult.put("displayName", authResult.getUser().getDisplayName());
                            jsResult.put("email", authResult.getUser().getEmail());

                            call.success(jsResult);
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(PLUGIN_TAG, "Firebase Sign In with Custom Token failure.", task.getException());
                        call.reject("Firebase Sign In with Custom Token failure.");
                    }
                }
            })
            .addOnFailureListener(this.getActivity(), new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception ex) {
                    // If sign in fails, display a message to the user.
                    Log.w(PLUGIN_TAG, "Firebase Sign In with Custom Token failure.", ex);
                    call.reject("Firebase Sign In with Custom Token failure.");
                }
            });
    }

    @PluginMethod()
    public void getCurrentUser(final PluginCall call) {
        FirebaseUser currentUser = this.mAuth.getCurrentUser();
        JSObject jsResult = new JSObject();

        jsResult.put("callbackId", call.getCallbackId());

        if (currentUser == null) {
            jsResult.put("providerId", "");
            jsResult.put("displayName", "");
            jsResult.put("uid", "");
            jsResult.put("isAuthenticated", false);
        } else {
            jsResult.put("providerId", currentUser.getProviderId());
            jsResult.put("displayName", currentUser.getDisplayName());
            jsResult.put("uid", currentUser.getUid());
            jsResult.put("isAuthenticated", true);
        }

        call.success(jsResult);
    }

    @PluginMethod()
    public void signOut(PluginCall call) {
        // sing out from providers
        for (ProviderHandler providerHandler : this.providerHandlers.values()) {
            providerHandler.signOut();
        }

        // sign out from firebase
        FirebaseUser currentUser = this.mAuth.getCurrentUser();
        if (currentUser != null) {
            this.mAuth.signOut();
        }

        call.success();
    }

    @Override
    public void startActivityForResult(PluginCall call, Intent intent, int resultCode) {
        super.startActivityForResult(call, intent, resultCode);
    }

    @Override
    public void notifyListeners(String eventName, JSObject data) {
        super.notifyListeners(eventName, data);
    }

    private ProviderHandler getProviderHandler(PluginCall call) {
        String providerId = call.getString("providerId", null);
        return this.providerHandlers.get(providerId);
    }

    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(PLUGIN_TAG, "Handle on Activity Result");

        final PluginCall savedCall = getSavedCall();
        if (savedCall == null) {
            Log.d(PLUGIN_TAG, "No saved call on activity result.");
            return;
        }

        final ProviderHandler handler = this.providerHandlerByRC.get(requestCode);
        if (handler == null) {
            Log.w(PLUGIN_TAG, "No provider handler with given request code.");
            savedCall.reject("No provider handler with given request code.");
        } else {
            handler.handleOnActivityResult(requestCode, resultCode, data);
        }
    }

    public void handleAuthCredentials(AuthCredential credential) {
        final PluginCall savedCall = getSavedCall();
        if (savedCall == null) {
            Log.d(PLUGIN_TAG, "No saved call on activity result.");
            return;
        }

        if (credential == null) {
            Log.w(PLUGIN_TAG, "Sign In failure: credentials.");
            savedCall.reject("Sign In failure: credentials.");
            return;
        }

        if (shouldLinkProvider) {
            linkProvider(savedCall, credential);
            return; // It's a "link provider" call we don't want to authenticate the user
        }

        if (this.nativeAuth) {
            nativeAuth(savedCall, credential);
        } else {
            JSObject jsResult = this.build(savedCall, null);
            savedCall.success(jsResult);
        }
    }

    private void nativeAuth(final PluginCall savedCall, final AuthCredential credential) {
        this.mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this.getActivity(), new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(PLUGIN_TAG, "Firebase Sign In with Credential succeed.");
                        FirebaseUser user = mAuth.getCurrentUser();
                        AuthResult authResult = task.getResult();

                        if (user == null) {
                            Log.w(PLUGIN_TAG, "Ops, no Firebase user after Sign In with Credential succeed.");
                            savedCall.reject("Ops, no Firebase user after Sign In with Credential succeed");
                        } else {
                            JSObject jsResult = build(savedCall, authResult);
                            savedCall.success(jsResult);
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(PLUGIN_TAG, "Firebase Sign In with Credential failure.", task.getException());
                        savedCall.reject("Firebase Sign In with Credential failure.");
                    }
                }
            })
            .addOnFailureListener(this.getActivity(), new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception ex) {
                    // If sign in fails, display a message to the user.
                    Log.w(PLUGIN_TAG, "Firebase Sign In with Credential failure.", ex);
                    savedCall.reject("Firebase Sign In with Credential failure.");
                }
            });
    }

    private boolean isProviderLinked(final FirebaseUser currentUser, final String providerId) {
        for (UserInfo userInfo : currentUser.getProviderData()) {
            if (providerId.equals(userInfo.getProviderId())) {
                return true;
            }
        }

        return false;
    }

    private void linkProvider(final PluginCall savedCall, final AuthCredential credential) {
        FirebaseUser currentUser = this.mAuth.getCurrentUser();
        final String providerId = savedCall.getString("providerId");

        if (currentUser == null) {
            savedCall.reject("Can not link provider because a user is not signed in");
            return;
        }

        if (isProviderLinked(currentUser, providerId)) {
            Log.d(PLUGIN_TAG, "Provider '" + providerId + "' is already linked to account.");

            JSObject jsResult = build(savedCall, null);
            savedCall.success(jsResult);
            return;
        }

        currentUser.linkWithCredential(credential)
            .addOnCompleteListener(this.getActivity(), new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()) {
                        Log.d(PLUGIN_TAG, "linkWithCredential:success");

                        AuthResult authResult = task.getResult();
                        JSObject jsResult = build(savedCall, authResult);

                        savedCall.success(jsResult);
                        return;
                    }

                    Log.w(PLUGIN_TAG, "linkWithCredential:failure", task.getException());
                    savedCall.reject(Objects.requireNonNull(task.getException()).getLocalizedMessage());
                }
            })
            .addOnFailureListener(this.getActivity(), new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception ex) {
                    Log.w(PLUGIN_TAG, "Firebase Link Provider failure.", ex);
                    savedCall.reject("Firebase Link Provider failure.");
                }
            });
    }

    public void handleFailure(String message, Exception e) {
        PluginCall savedCall = getSavedCall();
        if (savedCall == null) {
            Log.d(PLUGIN_TAG, "No saved call on handle failure.");
            return;
        }

        if (e != null) {
            savedCall.reject(message, e);
        } else {
            savedCall.reject(message);
        }
    }

    private JSObject build(PluginCall call, @Nullable AuthResult authResult) {
        Log.d(PLUGIN_TAG, "Building authentication result");

        JSObject jsResult = new JSObject();
        jsResult.put("callbackId", call.getCallbackId());
        jsResult.put("providerId", call.getString("providerId"));

        if (authResult != null) {
            jsResult.put("isNewUser", authResult.getAdditionalUserInfo().isNewUser());
        }

        ProviderHandler handler = this.getProviderHandler(call);
        if (handler != null) {
            handler.fillResult(jsResult);
        }

        return jsResult;
    }
}
