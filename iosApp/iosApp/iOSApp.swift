import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
	var body: some Scene {
		WindowGroup {
			ContentView()
                .background(Color(hex: 0x2C3E50))
//                .edgesIgnoringSafeArea(.bottom)
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
