import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/widgets/async_value_view.dart';
import '../../theme/app_colors.dart';
import 'equipment_repository.dart';

class _EquipmentFilter {
  const _EquipmentFilter({this.name, this.status, this.type});
  final String? name;
  final String? status;
  final String? type;

  bool get isActive => name != null || status != null || type != null;

  _EquipmentFilter copyWith({String? name, bool clearName = false, String? status, bool clearStatus = false, String? type, bool clearType = false}) {
    return _EquipmentFilter(
      name: clearName ? null : (name ?? this.name),
      status: clearStatus ? null : (status ?? this.status),
      type: clearType ? null : (type ?? this.type),
    );
  }
}

final _filterProvider = StateProvider.family<_EquipmentFilter, String>((ref, siteId) => const _EquipmentFilter());

final _listProvider = FutureProvider.family((ref, String siteId) {
  final filter = ref.watch(_filterProvider(siteId));
  return ref.watch(equipmentRepositoryProvider).list(siteId, name: filter.name, status: filter.status, type: filter.type);
});

const _statusIcons = {
  'AVAILABLE': Icons.check_circle_outline,
  'IN_USE': Icons.play_circle_outline,
  'MAINTENANCE': Icons.build_outlined,
  'UNAVAILABLE': Icons.block,
};

class EquipmentListScreen extends ConsumerWidget {
  const EquipmentListScreen({super.key, required this.siteId});
  final String siteId;

  Future<void> _create(BuildContext context, WidgetRef ref) async {
    final nameController = TextEditingController();
    final typeController = TextEditingController();
    final result = await showModalBottomSheet<bool>(
      context: context,
      isScrollControlled: true,
      builder: (context) => Padding(
        padding: EdgeInsets.only(left: 20, right: 20, top: 20, bottom: MediaQuery.of(context).viewInsets.bottom + 20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text('Novo equipamento', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800)),
            const SizedBox(height: 16),
            TextField(controller: nameController, decoration: const InputDecoration(labelText: 'Nome'), autofocus: true),
            const SizedBox(height: 12),
            TextField(controller: typeController, decoration: const InputDecoration(labelText: 'Tipo')),
            const SizedBox(height: 20),
            ElevatedButton(onPressed: () => Navigator.pop(context, true), child: const Text('Criar')),
          ],
        ),
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

  Future<void> _openFilterSheet(BuildContext context, WidgetRef ref) async {
    final current = ref.read(_filterProvider(siteId));
    final nameController = TextEditingController(text: current.name);
    final typeController = TextEditingController(text: current.type);
    String? status = current.status;

    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (context) => StatefulBuilder(
        builder: (context, setSheetState) => Padding(
          padding: EdgeInsets.only(left: 20, right: 20, top: 20, bottom: MediaQuery.of(context).viewInsets.bottom + 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  const Expanded(
                    child: Text('Filtrar equipamentos', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800)),
                  ),
                  TextButton(
                    onPressed: () {
                      nameController.clear();
                      typeController.clear();
                      setSheetState(() => status = null);
                    },
                    child: const Text('Limpar'),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              TextField(controller: nameController, decoration: const InputDecoration(labelText: 'Nome')),
              const SizedBox(height: 12),
              TextField(controller: typeController, decoration: const InputDecoration(labelText: 'Tipo')),
              const SizedBox(height: 12),
              DropdownButtonFormField<String?>(
                initialValue: status,
                decoration: const InputDecoration(labelText: 'Status'),
                items: [
                  const DropdownMenuItem(value: null, child: Text('Todos')),
                  ...equipmentStatusLabels.entries.map((e) => DropdownMenuItem(value: e.key, child: Text(e.value))),
                ],
                onChanged: (value) => setSheetState(() => status = value),
              ),
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: () {
                  ref.read(_filterProvider(siteId).notifier).state = _EquipmentFilter(
                    name: nameController.text.trim().isEmpty ? null : nameController.text.trim(),
                    status: status,
                    type: typeController.text.trim().isEmpty ? null : typeController.text.trim(),
                  );
                  Navigator.pop(context);
                },
                child: const Text('Aplicar filtros'),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final list = ref.watch(_listProvider(siteId));
    final filter = ref.watch(_filterProvider(siteId));
    return Scaffold(
      backgroundColor: AppColors.steel50,
      appBar: AppBar(
        title: const Text('Equipamentos'),
        actions: [
          IconButton(
            icon: Badge(isLabelVisible: filter.isActive, smallSize: 8, child: const Icon(Icons.filter_list)),
            tooltip: 'Filtrar',
            onPressed: () => _openFilterSheet(context, ref),
          ),
          IconButton(icon: const Icon(Icons.add), onPressed: () => _create(context, ref)),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () => ref.refresh(_listProvider(siteId).future),
        child: AsyncValueView(
          value: list,
          errorMessage: 'Não foi possível carregar os equipamentos.',
          data: (items) {
            if (items.isEmpty) {
              return ListView(
                children: [
                  EmptyState(
                    icon: Icons.construction_outlined,
                    message: filter.isActive
                        ? 'Nenhum equipamento encontrado com esses filtros.'
                        : 'Nenhum equipamento cadastrado ainda.',
                  ),
                ],
              );
            }
            return ListView.separated(
              padding: const EdgeInsets.all(16),
              itemCount: items.length,
              separatorBuilder: (_, _) => const SizedBox(height: 10),
              itemBuilder: (context, index) {
                final e = items[index];
                return Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: AppColors.steel200),
                  ),
                  child: Row(
                    children: [
                      Container(
                        height: 42,
                        width: 42,
                        decoration: BoxDecoration(color: AppColors.blueprint50, borderRadius: BorderRadius.circular(12)),
                        alignment: Alignment.center,
                        child: const Icon(Icons.construction_outlined, color: AppColors.blueprint600, size: 20),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(e.name, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 14.5)),
                            if (e.type != null) ...[
                              const SizedBox(height: 3),
                              Text(e.type!, style: const TextStyle(fontSize: 12.5, color: AppColors.steel500)),
                            ],
                          ],
                        ),
                      ),
                      Row(
                        children: [
                          Icon(_statusIcons[e.status] ?? Icons.help_outline, size: 15, color: AppColors.steel500),
                          const SizedBox(width: 5),
                          Text(
                            equipmentStatusLabels[e.status] ?? e.status,
                            style: const TextStyle(color: AppColors.steel600, fontWeight: FontWeight.w600, fontSize: 12.5),
                          ),
                        ],
                      ),
                    ],
                  ),
                );
              },
            );
          },
        ),
      ),
    );
  }
}
