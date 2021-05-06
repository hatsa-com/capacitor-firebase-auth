import * as firebase from 'firebase/app';
import 'firebase/auth';

declare module "@capacitor/core" {
  interface PluginRegistry {
    CapacitorFirebaseAuth?: CapacitorFirebaseAuthPlugin;
  }
}

export interface CapacitorFirebaseAuthPlugin {
  signIn(options: {providerId: string, data?: SignInOptions}): Promise<SignInResult>;
  signInAndLink(options: {providerId: string, data?: SignInOptions}): Promise<SignInResult>;
  signInWithCustomToken(options: {customToken: string}): Promise<SignInResult>;
  getCurrentUser(options: {}): Promise<GetCurrentUserResult>;
  unlink(options: {providerId: string}): Promise<void>;
  signOut(options: {}): Promise<void>;
}

export class GoogleSignInResult{
  providerId = firebase.auth.GoogleAuthProvider.PROVIDER_ID;
  constructor(public idToken: string) {
  }
}

export class TwitterSignInResult {
  providerId = firebase.auth.TwitterAuthProvider.PROVIDER_ID;
  constructor(public idToken: string, public secret: string) {
  }
}

export class FacebookSignInResult {
  providerId = firebase.auth.FacebookAuthProvider.PROVIDER_ID;
  constructor(public idToken: string) {
  }
}

export class PhoneSignInResult {
  providerId = firebase.auth.PhoneAuthProvider.PROVIDER_ID;
  constructor(public verificationId: string, public verificationCode: string) {
  }
}
export class EmailSignInResult {
  providerId = firebase.auth.EmailAuthProvider.PROVIDER_ID;
  constructor(public verificationId: string, public verificationCode: string) {
  }
}
export class CustomTokenSignInResult {
  providerId = "";
  constructor(public verificationId: string, public verificationCode: string) {
  }
}

export type SignInResult = CustomTokenSignInResult | GoogleSignInResult | TwitterSignInResult | FacebookSignInResult | PhoneSignInResult;

export interface PhoneSignInOptions {
  phone: string,
  verificationCode?: string
}
export interface EmailSignInOptions {
  email: string,
  password: string
}

export type GetCurrentUserResult = {
  callbackId: string,
  providerId: string,
  displayName: string,
  uid: string,
  isAuthenticated: boolean
};

export type SignInOptions = PhoneSignInOptions | EmailSignInOptions;
