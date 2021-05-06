import { WebPlugin } from '@capacitor/core';
import {CapacitorFirebaseAuthPlugin, GetCurrentUserResult, SignInResult} from './definitions';

export class CapacitorFirebaseAuthWeb extends WebPlugin implements CapacitorFirebaseAuthPlugin {
  constructor() {
    super({
      name: 'CapacitorFirebaseAuth',
      platforms: ['web']
    });
  }

  async signIn(options: {providerId: string;}): Promise<SignInResult> {
    return Promise.resolve({providerId: options.providerId, idToken: undefined});
  }
  
  async signInAndLink(options: {providerId: string;}): Promise<SignInResult> {
    return Promise.resolve({providerId: options.providerId, idToken: undefined});
  }
  
  async signInWithCustomToken(options: {customToken: string;}): Promise<SignInResult> {
    return Promise.resolve({providerId: "", idToken: undefined});
  }
  
  async getCurrentUser(options: {}): Promise<GetCurrentUserResult> {
    return Promise.resolve({
      callbackId: "",
      providerId: "",
      displayName: "",
      uid: "",
      isAuthenticated: false
    });
  }

  // @ts-ignore
  async unlink(options: { providerId: string; }): Promise<void> {
    return Promise.resolve();
  }

  // @ts-ignore
  async signOut(options: {}): Promise<void> {
    return Promise.resolve();
  }
}

const CapacitorFirebaseAuth = new CapacitorFirebaseAuthWeb();

export { CapacitorFirebaseAuth };
