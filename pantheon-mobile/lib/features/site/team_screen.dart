import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/widgets/async_value_view.dart';
import '../../theme/app_colors.dart';
import 'site_repository.dart';

final _membersProvider = FutureProvider.family((ref, String siteId) => ref.watch(siteRepositoryProvider).listMembers(siteId));

const _functionLabels = {
  'ADMIN': 'Administrador',
  'CLIENT': 'Cliente',
  'ARCHITECT': 'Arquiteto',
  'ENGINEER': 'Engenheiro',
  'SITE_FOREMAN': 'Mestre de obra',
  'SERVICE_PROVIDER': 'Prestador de serviço',
};

class TeamScreen extends ConsumerWidget {
  const TeamScreen({super.key, required this.siteId});
  final String siteId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final members = ref.watch(_membersProvider(siteId));
    return Scaffold(
      appBar: AppBar(title: const Text('Equipe')),
      body: AsyncValueView(
        value: members,
        data: (list) {
          if (list.isEmpty) return const EmptyState(message: 'Nenhum membro cadastrado ainda.');
          return ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: list.length,
            separatorBuilder: (_, _) => const SizedBox(height: 8),
            itemBuilder: (context, index) {
              final member = list[index];
              return Card(
                child: ListTile(
                  leading: CircleAvatar(
                    backgroundColor: AppColors.blueprint600,
                    child: Text(
                      member.label.isNotEmpty ? member.label[0].toUpperCase() : '?',
                      style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w700),
                    ),
                  ),
                  title: Text(member.label, style: const TextStyle(fontWeight: FontWeight.w700)),
                  subtitle: Text(_functionLabels[member.function] ?? member.function),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
