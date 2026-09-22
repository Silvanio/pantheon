import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/offline/outbox_controller.dart';
import '../../core/widgets/async_value_view.dart';
import '../../theme/app_colors.dart';

class SyncScreen extends ConsumerWidget {
  const SyncScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(outboxControllerProvider);
    return Scaffold(
      appBar: AppBar(title: const Text('Sincronização')),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            margin: const EdgeInsets.all(16),
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: state.isOnline ? AppColors.emerald50 : AppColors.safety50,
              borderRadius: BorderRadius.circular(14),
            ),
            child: Row(
              children: [
                Icon(
                  state.isOnline ? Icons.cloud_done_outlined : Icons.cloud_off_outlined,
                  color: state.isOnline ? AppColors.emerald700 : AppColors.safety600,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    state.isOnline ? 'Você está online.' : 'Você está offline. As ações abaixo serão enviadas automaticamente assim que a conexão voltar.',
                    style: TextStyle(
                      color: state.isOnline ? AppColors.emerald700 : AppColors.safety600,
                      fontWeight: FontWeight.w600,
                      fontSize: 13,
                    ),
                  ),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: state.isSyncing || state.pending.isEmpty
                    ? null
                    : () => ref.read(outboxControllerProvider.notifier).syncNow(),
                icon: state.isSyncing
                    ? const SizedBox(height: 16, width: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                    : const Icon(Icons.sync, size: 18),
                label: Text(state.isSyncing ? 'Sincronizando…' : 'Sincronizar agora'),
              ),
            ),
          ),
          const SizedBox(height: 16),
          Expanded(
            child: state.pending.isEmpty
                ? const EmptyState(message: 'Nada pendente de sincronização.', icon: Icons.cloud_done_outlined)
                : ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: state.pending.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 8),
                    itemBuilder: (context, index) {
                      final entry = state.pending[index];
                      return Card(
                        child: ListTile(
                          leading: Icon(entry.isUpload ? Icons.image_outlined : Icons.pending_actions_outlined, color: AppColors.blueprint600),
                          title: Text(entry.entityLabel, style: const TextStyle(fontWeight: FontWeight.w700)),
                          subtitle: entry.lastError != null
                              ? Text('Última tentativa falhou: ${entry.lastError}', style: const TextStyle(color: AppColors.safety600, fontSize: 12))
                              : const Text('Aguardando conexão', style: TextStyle(fontSize: 12, color: AppColors.steel500)),
                          trailing: IconButton(
                            icon: const Icon(Icons.close, color: AppColors.steel400),
                            tooltip: 'Descartar',
                            onPressed: () => ref.read(outboxControllerProvider.notifier).discard(entry.id),
                          ),
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}
