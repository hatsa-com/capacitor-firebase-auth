import Foundation
import Capacitor
import FirebaseCore
import FirebaseAuth
import GoogleSignIn


class EmailPasswordHandler: NSObject, ProviderHandler {

    var plugin: CapacitorFirebaseAuth? = nil

    func initialize(plugin: CapacitorFirebaseAuth) {
        print("Initializing Email Password Handler")

        self.plugin = plugin
    }

    func signIn(call: CAPPluginCall) {
        guard let data = call.getObject("data") else {
            call.reject("The auth data is required")
            return
        }

        guard let email = data["email"] as? String,
              let password = data["password"] as? String else {
            call.reject("Email and password are required")
            return
        }
        
        let credential = EmailAuthProvider.credential(withEmail: email, password: password)
        self.plugin?.handleAuthCredentials(credential: credential)
    }

    func isAuthenticated() -> Bool {
        return Auth.auth().currentUser != nil
    }

    func fillResult(data: PluginResultData) -> PluginResultData {
        return data
    }

    func signOut(){
        do {
            try Auth.auth().signOut()
        } catch let signOutError as NSError {
            print ("Error signing out: %@", signOutError)
            self.plugin?.handleError(message: "Error signing out: \(signOutError)")
        }
    }
}
