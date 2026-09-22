import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../theme/app_colors.dart';
import '../offline/outbox_controller.dart';

/// A small floating indicator, visible on every authenticated screen (see `main.dart`), showing
/// offline status and/or a pending-sync count — tap opens the sync screen. Hidden entirely when
/// online with nothing pending, so it never distracts from the normal, connected experience.
class SyncStatusBadge extends ConsumerWidget {
  const SyncStatusBadge({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(outboxControllerProvider);
    if (state.isOnline && state.pending.isEmpty) return const SizedBox.shrink();

    final label = !state.isOnline
        ? (state.pending.isEmpty ? 'Offline' : 'Offline · ${state.pending.length} por sincronizar')
        : '${state.pending.length} por sincronizar';

    return SafeArea(
      child: Align(
        alignment: Alignment.bottomRight,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Material(
            color: state.isOnline ? AppColors.blueprint600 : AppColors.safety500,
            borderRadius: BorderRadius.circular(999),
            elevation: 3,
            child: InkWell(
              borderRadius: BorderRadius.circular(999),
              onTap: () => context.push('/sync'),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(state.isOnline ? Icons.sync : Icons.cloud_off_outlined, color: Colors.white, size: 16),
                    const SizedBox(width: 6),
                    Text(label, style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w700)),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
