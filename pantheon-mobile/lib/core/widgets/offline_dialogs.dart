import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../theme/app_colors.dart';
import '../offline/outbox_controller.dart';

/// Shown *before* a queueable action (approve, submit, create, upload a photo, ...) when we
/// already know we're offline — asks the user to confirm they want to save it locally and have
/// it sent automatically once connectivity returns, rather than silently queueing it. Returns
/// `true` to proceed (online, or the user confirmed), `false` if the user cancelled.
Future<bool> confirmProceedOffline(BuildContext context, WidgetRef ref) async {
  if (ref.read(outboxControllerProvider).isOnline) return true;
  final proceed = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      icon: const Icon(Icons.cloud_off_outlined, color: AppColors.safety500, size: 32),
      title: const Text('Você está offline'),
      content: const Text(
        'Esta ação será salva neste dispositivo e enviada automaticamente assim que a conexão com a internet voltar. Deseja continuar?',
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
        TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Continuar')),
      ],
    ),
  );
  return proceed ?? false;
}

/// For actions that must never be queued offline — approvals, permissions, tasks, projetos (see
/// design.md's offline-support scoping): blocks with an explanatory dialog when offline, rather
/// than silently attempting and failing. Returns `true` only when online.
Future<bool> requireOnline(BuildContext context, WidgetRef ref) async {
  if (ref.read(outboxControllerProvider).isOnline) return true;
  await showDialog<void>(
    context: context,
    builder: (context) => AlertDialog(
      icon: const Icon(Icons.cloud_off_outlined, color: AppColors.safety500, size: 32),
      title: const Text('Você está offline'),
      content: const Text('Esta ação exige conexão com a internet. Tente novamente quando estiver online.'),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: const Text('Entendi')),
      ],
    ),
  );
  return false;
}

/// Shown *after* an action turns out to have been queued instead of sent (the rarer case where
/// connectivity dropped between the proactive check above and the actual request) — an
/// acknowledgement, not a choice, since the action already happened.
Future<void> showOfflineSavedDialog(BuildContext context) {
  return showDialog<void>(
    context: context,
    builder: (context) => AlertDialog(
      icon: const Icon(Icons.cloud_off_outlined, color: AppColors.safety500, size: 32),
      title: const Text('Você está offline'),
      content: const Text(
        'Isso foi salvo neste dispositivo e será enviado automaticamente assim que a conexão com a internet voltar.',
      ),
      actions: [
        TextButton(onPressed: () => Navigator.pop(context), child: const Text('Entendi')),
      ],
    ),
  );
}
