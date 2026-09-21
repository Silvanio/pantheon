import 'package:flutter/material.dart';

/// Shown only while [AuthStatus] is `unknown` (the secure-storage session restore hasn't
/// resolved yet) — avoids ever mounting [DashboardScreen] (and firing its network calls)
/// before we know whether the user is actually authenticated.
class SplashScreen extends StatelessWidget {
  const SplashScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const Scaffold(body: Center(child: CircularProgressIndicator()));
  }
}
