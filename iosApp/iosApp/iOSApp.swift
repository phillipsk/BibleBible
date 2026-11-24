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
        return false
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
        #if DEBUG
        if let debugPath = Bundle.main.path(forResource: "GoogleService-Info-Debug", ofType: "plist"),
           let options = FirebaseOptions(contentsOfFile: debugPath) {
            FirebaseApp.configure(options: options)
            return
        }
        #endif
        FirebaseApp.configure()
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

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken, !token.isEmpty else { return }
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
