import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/widgets/async_value_view.dart';
import '../../theme/app_colors.dart';
import 'equipment_repository.dart';

final _listProvider = FutureProvider.family((ref, String siteId) => ref.watch(equipmentRepositoryProvider).list(siteId));

class EquipmentListScreen extends ConsumerWidget {
  const EquipmentListScreen({super.key, required this.siteId});
  final String siteId;

  Future<void> _create(BuildContext context, WidgetRef ref) async {
    final nameController = TextEditingController();
    final typeController = TextEditingController();
    final result = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Novo equipamento'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(controller: nameController, decoration: const InputDecoration(labelText: 'Nome'), autofocus: true),
            const SizedBox(height: 8),
            TextField(controller: typeController, decoration: const InputDecoration(labelText: 'Tipo')),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Criar')),
        ],
      ),
    );
    if (result == true && nameController.text.trim().isNotEmpty) {
      try {
        await ref
            .read(equipmentRepositoryProvider)
            .create(siteId, nameController.text.trim(), typeController.text.trim().isEmpty ? null : typeController.text.trim());
        ref.invalidate(_listProvider(siteId));
      } catch (_) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Não foi possível criar o equipamento.')));
        }
      }
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final list = ref.watch(_listProvider(siteId));
    return Scaffold(
      appBar: AppBar(
        title: const Text('Equipamentos'),
        actions: [IconButton(icon: const Icon(Icons.add), onPressed: () => _create(context, ref))],
      ),
      body: AsyncValueView(
        value: list,
        data: (items) {
          if (items.isEmpty) return ListView(children: const [EmptyState(message: 'Nenhum equipamento cadastrado ainda.')]);
          return ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: items.length,
            separatorBuilder: (_, _) => const SizedBox(height: 8),
            itemBuilder: (context, index) {
              final e = items[index];
              return Card(
                child: ListTile(
                  leading: const Icon(Icons.construction_outlined, color: AppColors.blueprint600),
                  title: Text(e.name, style: const TextStyle(fontWeight: FontWeight.w700)),
                  subtitle: e.type != null ? Text(e.type!) : null,
                  trailing: Text(equipmentStatusLabels[e.status] ?? e.status, style: const TextStyle(color: AppColors.steel500)),
                ),
              );
            },
          );
        },
      ),
    );
  }
}
