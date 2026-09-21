import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/widgets/async_value_view.dart';
import '../../theme/app_colors.dart';
import 'site_repository.dart';

final _overridesProvider =
    FutureProvider.family((ref, String siteId) => ref.watch(siteRepositoryProvider).listPermissionOverrides(siteId));

const _capabilityLabels = {
  'DOCUMENT_PROJECTS': 'Projetos',
  'DAILY_REPORT': 'Diário de Obra',
  'EQUIPMENT': 'Equipamentos',
  'PURCHASE_REQUEST': 'Pedido de Compra',
  'ORCAMENTO_MANAGE': 'Orçamentos',
  'TASKS': 'Tasks',
  'TEAM_MANAGE': 'Equipe',
};

const _accessLevelLabels = {
  'HIDDEN': 'Oculto',
  'VIEW': 'Ver',
  'MANAGE': 'Gerenciar',
  'VIEW_AND_APPROVE': 'Ver e aprovar',
};

const _functionLabels = {
  'ADMIN': 'Administrador',
  'CLIENT': 'Cliente',
  'ARCHITECT': 'Arquiteto',
  'ENGINEER': 'Engenheiro',
  'SITE_FOREMAN': 'Mestre de obra',
  'SERVICE_PROVIDER': 'Prestador de serviço',
};

/// Read-only — editing permission overrides stays web-only in this pass (design.md).
class PermissionsScreen extends ConsumerWidget {
  const PermissionsScreen({super.key, required this.siteId});
  final String siteId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final overrides = ref.watch(_overridesProvider(siteId));
    return Scaffold(
      appBar: AppBar(title: const Text('Permissões')),
      body: AsyncValueView(
        value: overrides,
        data: (list) {
          if (list.isEmpty) return const EmptyState(message: 'Nenhuma permissão customizada configurada.');
          return ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: list.length,
            separatorBuilder: (_, _) => const SizedBox(height: 8),
            itemBuilder: (context, index) {
              final o = list[index];
              final fn = o['function'] as String?;
              final capability = o['capability'] as String;
              final accessLevel = o['accessLevel'] as String;
              return Card(
                child: ListTile(
                  title: Text(_capabilityLabels[capability] ?? capability, style: const TextStyle(fontWeight: FontWeight.w700)),
                  subtitle: Text(fn != null ? (_functionLabels[fn] ?? fn) : 'Membro específico'),
                  trailing: Text(_accessLevelLabels[accessLevel] ?? accessLevel, style: const TextStyle(color: AppColors.steel500)),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
