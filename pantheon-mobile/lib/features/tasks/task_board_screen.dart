import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/offline_dialogs.dart';
import '../../theme/app_colors.dart';
import 'task_models.dart';
import 'task_repository.dart';

final _boardProvider = FutureProvider.family((ref, String siteId) => ref.watch(taskRepositoryProvider).getBoard(siteId));

class TaskBoardScreen extends ConsumerStatefulWidget {
  const TaskBoardScreen({super.key, required this.siteId});
  final String siteId;

  @override
  ConsumerState<TaskBoardScreen> createState() => _TaskBoardScreenState();
}

class _TaskBoardScreenState extends ConsumerState<TaskBoardScreen> {
  final _pageController = PageController();
  int _currentPage = 0;

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  Future<void> _moveCard(TaskCard card, TaskBoard board) async {
    final target = await showModalBottomSheet<TaskColumn>(
      context: context,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Padding(padding: EdgeInsets.all(16), child: Text('Mover para coluna', style: TextStyle(fontWeight: FontWeight.w700))),
            ...board.columns.map(
              (col) => ListTile(
                title: Text(col.name),
                trailing: col.id == card.columnId ? const Icon(Icons.check, color: AppColors.blueprint600) : null,
                onTap: () => Navigator.pop(context, col),
              ),
            ),
          ],
        ),
      ),
    );
    if (target == null || target.id == card.columnId) return;
    if (!mounted || !await requireOnline(context, ref)) return;
    final newSortOrder = board.cardsFor(target.id).length;
    try {
      await ref.read(taskRepositoryProvider).moveCard(card.id, target.id, newSortOrder);
      ref.invalidate(_boardProvider(widget.siteId));
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Não foi possível mover o card.')));
      }
    }
  }

  Future<void> _createCard(String columnId) async {
    final controller = TextEditingController();
    final title = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Nova tarefa'),
        content: TextField(controller: controller, decoration: const InputDecoration(labelText: 'Título'), autofocus: true),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
          TextButton(onPressed: () => Navigator.pop(context, controller.text), child: const Text('Criar')),
        ],
      ),
    );
    if (title != null && title.trim().isNotEmpty) {
      if (!mounted || !await requireOnline(context, ref)) return;
      try {
        await ref.read(taskRepositoryProvider).createCard(widget.siteId, columnId, title.trim(), null);
        ref.invalidate(_boardProvider(widget.siteId));
      } catch (_) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Não foi possível criar a tarefa.')));
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final board = ref.watch(_boardProvider(widget.siteId));
    return Scaffold(
      appBar: AppBar(title: const Text('Tasks')),
      body: AsyncValueView(
        value: board,
        data: (b) {
          if (b.columns.isEmpty) return const EmptyState(message: 'Nenhuma coluna configurada ainda.');
          return Column(
            children: [
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 12),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: List.generate(
                    b.columns.length,
                    (i) => Container(
                      margin: const EdgeInsets.symmetric(horizontal: 3),
                      width: i == _currentPage ? 20 : 6,
                      height: 6,
                      decoration: BoxDecoration(
                        color: i == _currentPage ? AppColors.blueprint600 : AppColors.steel300,
                        borderRadius: BorderRadius.circular(3),
                      ),
                    ),
                  ),
                ),
              ),
              Expanded(
                child: PageView.builder(
                  controller: _pageController,
                  onPageChanged: (i) => setState(() => _currentPage = i),
                  itemCount: b.columns.length,
                  itemBuilder: (context, index) {
                    final column = b.columns[index];
                    final cards = b.cardsFor(column.id);
                    return Column(
                      children: [
                        Padding(
                          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                          child: Row(
                            children: [
                              Expanded(
                                child: Text(
                                  '${column.name} (${cards.length})',
                                  style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 15),
                                ),
                              ),
                              IconButton(icon: const Icon(Icons.add), onPressed: () => _createCard(column.id)),
                            ],
                          ),
                        ),
                        Expanded(
                          child: cards.isEmpty
                              ? const Center(child: Text('Nenhum card nesta coluna.', style: TextStyle(color: AppColors.steel500)))
                              : ListView.separated(
                                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                                  itemCount: cards.length,
                                  separatorBuilder: (_, _) => const SizedBox(height: 8),
                                  itemBuilder: (context, i) {
                                    final card = cards[i];
                                    return Card(
                                      child: ListTile(
                                        title: Text(card.title, style: const TextStyle(fontWeight: FontWeight.w700)),
                                        subtitle: card.description != null && card.description!.isNotEmpty
                                            ? Text(card.description!, maxLines: 2, overflow: TextOverflow.ellipsis)
                                            : null,
                                        trailing: IconButton(
                                          icon: const Icon(Icons.swap_horiz, color: AppColors.steel500),
                                          onPressed: () => _moveCard(card, b),
                                        ),
                                      ),
                                    );
                                  },
                                ),
                        ),
                      ],
                    );
                  },
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
