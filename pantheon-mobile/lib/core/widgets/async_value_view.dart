import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../theme/app_colors.dart';

/// Standardizes the loading/error/data states for an `AsyncValue`, so every screen doesn't
/// hand-roll its own spinner/error copy.
class AsyncValueView<T> extends StatelessWidget {
  const AsyncValueView({super.key, required this.value, required this.data, this.errorMessage});

  final AsyncValue<T> value;
  final Widget Function(T data) data;
  final String? errorMessage;

  @override
  Widget build(BuildContext context) {
    return value.when(
      data: data,
      loading: () => const Center(child: Padding(padding: EdgeInsets.all(32), child: CircularProgressIndicator())),
      error: (error, _) => EmptyState(
        icon: Icons.error_outline,
        message: errorMessage ?? 'Não foi possível carregar. Tente novamente.',
      ),
    );
  }
}

class EmptyState extends StatelessWidget {
  const EmptyState({super.key, required this.message, this.icon = Icons.inbox_outlined});

  final String message;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              height: 72,
              width: 72,
              decoration: BoxDecoration(color: AppColors.blueprint50, shape: BoxShape.circle),
              alignment: Alignment.center,
              child: Icon(icon, size: 32, color: AppColors.blueprint600),
            ),
            const SizedBox(height: 16),
            Text(
              message,
              textAlign: TextAlign.center,
              style: const TextStyle(color: AppColors.steel500, fontSize: 13.5, height: 1.4),
            ),
          ],
        ),
      ),
    );
  }
}
