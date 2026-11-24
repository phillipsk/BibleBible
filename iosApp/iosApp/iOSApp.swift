import SwiftUI
import UIKit
import UserNotifications
import FirebaseCore
import FirebaseMessaging
import ComposeApp

final class NotificationAppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    private var topicObserver: NSObjectProtocol?

    private var isDebugBuild: Bool {
        #if DEBUG
        return true
        #else
        return Bundle.main.bundleIdentifier?.hasSuffix(".debug") == true
        #endif
    }

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        configureFirebase()
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        application.registerForRemoteNotifications()
        fetchAndSyncFcmToken()
        FoundationModelsBridge.register()

        topicObserver = NotificationCenter.default.addObserver(
            forName: NSNotification.Name("BibleBiblePushTopicsChanged"),
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.syncTopics()
        }
        return true
    }

    private func configureFirebase() {
        if FirebaseApp.app() != nil { return }
        let bundleId = Bundle.main.bundleIdentifier ?? "unknown"
        if isDebugBuild,
           let debugPath = Bundle.main.path(forResource: "GoogleService-Info-Debug", ofType: "plist"),
           let options = FirebaseOptions(contentsOfFile: debugPath) {
            FirebaseApp.configure(options: options)
            #if DEBUG
            print("[Push] Configured Firebase with GoogleService-Info-Debug.plist (bundleId=\(bundleId))")
            #endif
            return
        }
        FirebaseApp.configure()
        #if DEBUG
        print("[Push] Configured Firebase with default GoogleService-Info.plist (bundleId=\(bundleId))")
        #endif
    }

    private func syncTopics() {
        let messaging = Messaging.messaging()
        let enabled = MainViewControllerKt.areIosNotificationsEnabled()
        if enabled {
            for topic in MainViewControllerKt.iosPushTopics(isDebug: isDebugBuild) {
                messaging.subscribe(toTopic: topic)
            }
        } else {
            for topic in MainViewControllerKt.iosAllPushTopics() {
                messaging.unsubscribe(fromTopic: topic)
            }
        }
    }

    private func fetchAndSyncFcmToken() {
        Messaging.messaging().token { [weak self] token, error in
            guard let self else { return }
            if let error {
                #if DEBUG
                print("[Push] FCM token fetch error: \(error.localizedDescription)")
                #endif
                return
            }
            guard let token, !token.isEmpty else { return }
            #if DEBUG
            print("[Push] FCM token ready (len=\(token.count))")
            #endif
            MainViewControllerKt.handleIosPushTokenRefresh(token: token, isDebug: self.isDebugBuild)
            self.syncTopics()
        }
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().setAPNSToken(deviceToken, type: isDebugBuild ? .sandbox : .prod)
        #if DEBUG
        print("[Push] APNs token received (bytes=\(deviceToken.count), sandbox=\(isDebugBuild))")
        #endif
        fetchAndSyncFcmToken()
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        #if DEBUG
        print("[Push] APNs registration failed: \(error.localizedDescription)")
        #endif
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken, !token.isEmpty else { return }
        #if DEBUG
        print("[Push] FCM delegate token refreshed (len=\(token.count))")
        #endif
        MainViewControllerKt.handleIosPushTokenRefresh(token: token, isDebug: isDebugBuild)
        syncTopics()
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound, .badge])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        MainViewControllerKt.handleIosNotificationTap(userInfo: userInfo)
        completionHandler()
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(NotificationAppDelegate.self) private var appDelegate

	var body: some Scene {
		WindowGroup {
			ZStack {
				Color(hex: 0x09111E)
					.ignoresSafeArea(.all)
				ContentView()
			}
		}
	}
}

extension Color {
    init(hex: UInt) {
        let red = Double((hex >> 16) & 0xFF) / 255.0
        let green = Double((hex >> 8) & 0xFF) / 255.0
        let blue = Double(hex & 0xFF) / 255.0
        self.init(red: red, green: green, blue: blue)
    }
}
