import UIKit
import SwiftUI
import Shared

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    @State private var showSplash = true

    var body: some View {
        ZStack {
            ComposeView()
                .ignoresSafeArea()

            if showSplash {
                SplashOverlay()
                    .transition(.opacity)
            }
        }
        .onAppear {
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.4) {
                withAnimation(.easeOut(duration: 0.4)) {
                    showSplash = false
                }
            }
        }
    }
}

private struct SplashOverlay: View {
    var body: some View {
        ZStack {
            Color("SplashBackground")
                .ignoresSafeArea()

            Image("SplashLogo")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(width: 120, height: 120)

            VStack {
                Spacer()
                Image("SplashBranding")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 220, height: 88)
                    .padding(.bottom, 40)
            }
        }
    }
}