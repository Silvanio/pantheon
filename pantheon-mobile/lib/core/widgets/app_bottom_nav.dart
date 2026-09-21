import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// Bottom navigation bar — the mobile-appropriate equivalent of the web app's persistent
/// icon-rail sidebar (`AppSidebar.vue`), which doesn't fit a phone width. See design.md.
class AppBottomNav extends StatelessWidget {
  const AppBottomNav({super.key, required this.currentIndex});

  final int currentIndex;

  @override
  Widget build(BuildContext context) {
    return BottomNavigationBar(
      currentIndex: currentIndex,
      onTap: (index) {
        if (index == currentIndex) return;
        if (index == 0) {
          context.go('/');
        } else {
          context.go('/profile');
        }
      },
      items: const [
        BottomNavigationBarItem(icon: Icon(Icons.apartment_outlined), activeIcon: Icon(Icons.apartment), label: 'Obras'),
        BottomNavigationBarItem(icon: Icon(Icons.person_outline), activeIcon: Icon(Icons.person), label: 'Perfil'),
      ],
    );
  }
}
