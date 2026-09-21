import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/auth_provider.dart';
import '../../core/widgets/app_bottom_nav.dart';
import '../../theme/app_colors.dart';

class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    final initial = (auth.email ?? 'U').trim().substring(0, 1).toUpperCase();

    return Scaffold(
      appBar: AppBar(title: const Text('Meu perfil')),
      bottomNavigationBar: const AppBottomNav(currentIndex: 1),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          CircleAvatar(
            radius: 32,
            backgroundColor: AppColors.blueprint600,
            child: Text(initial, style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.w800)),
          ),
          const SizedBox(height: 16),
          Text(auth.email ?? '—', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w700)),
          const SizedBox(height: 32),
          OutlinedButton.icon(
            onPressed: () async {
              await ref.read(authControllerProvider.notifier).logout();
              if (context.mounted) context.go('/login');
            },
            icon: const Icon(Icons.logout, color: AppColors.safety500),
            label: const Text('Sair', style: TextStyle(color: AppColors.safety500)),
          ),
        ],
      ),
    );
  }
}
