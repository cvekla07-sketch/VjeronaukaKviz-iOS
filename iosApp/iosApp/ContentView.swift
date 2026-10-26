import SwiftUI
import UIKit
import ComposeApp

/// Prikazuje Compose UI (isti kod kao Android ekrani) unutar iOS aplikacije.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        // Kao enableEdgeToEdge() na Androidu: Compose sam crta ispod status bara.
        ComposeView().ignoresSafeArea()
    }
}
