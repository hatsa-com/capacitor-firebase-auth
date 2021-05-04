import Foundation
import Capacitor
import FirebaseCore
import FirebaseAuth
import GoogleSignIn


class EmailPasswordHandler: NSObject, ProviderHandler {

    var plugin: CapacitorFirebaseAuth? = nil
    var handlerResult: [String:Any] = [:]

    func initialize(plugin: CapacitorFirebaseAuth) {
        print("Initializing Email Password Handler")

        self.plugin = plugin
    }

    func signIn(call: CAPPluginCall) {
        guard let email = call.getString("email"),
              let password = call.getString("password") else {
            self.plugin?.handleError(message: "Please set keys: email, password")
            return
        }
        
        Auth.auth().signIn(withEmail: email, password: password) { (authResult, error) in
            if error != nil {
                self.plugin?.handleError(message: error.debugDescription)
                return
            }
            
            let isNewUser = authResult?.additionalUserInfo?.isNewUser ?? false
            
            guard let credential = authResult?.credential else {
                self.plugin?.handleError(message: "There's no credentials in AuthResult")
                return
            }
            
            self.handlerResult["isNewUser"] = isNewUser;
            self.plugin?.handleAuthCredentials(credential: credential)
        }
    }

    func isAuthenticated() -> Bool {
        return Auth.auth().currentUser != nil
    }

    func fillResult(data: PluginResultData) -> PluginResultData {
        var jsResult: PluginResultData = [:]

        jsResult.merge(data){ (current, _) in current }
        jsResult.merge(self.handlerResult){ (current, _) in current }

        return jsResult
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
