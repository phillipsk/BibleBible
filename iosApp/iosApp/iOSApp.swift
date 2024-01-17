import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        NapierLogger().doInitIosNapierLogger()
    }
	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}