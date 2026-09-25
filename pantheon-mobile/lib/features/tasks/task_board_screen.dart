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
                leading: const Icon(Icons.view_column_outlined),
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
      backgroundColor: AppColors.steel50,
      appBar: AppBar(title: const Text('Tasks')),
      body: AsyncValueView(
        value: board,
        data: (b) {
          if (b.columns.isEmpty) {
            return const EmptyState(icon: Icons.view_kanban_outlined, message: 'Nenhuma coluna configurada ainda.');
          }
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
                                child: Row(
                                  children: [
                                    Text(column.name, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 15.5)),
                                    const SizedBox(width: 6),
                                    Container(
                                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                      decoration: BoxDecoration(color: AppColors.steel100, borderRadius: BorderRadius.circular(999)),
                                      child: Text('${cards.length}', style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 11.5, color: AppColors.steel600)),
                                    ),
                                  ],
                                ),
                              ),
                              IconButton(icon: const Icon(Icons.add_circle_outline), onPressed: () => _createCard(column.id)),
                            ],
                          ),
                        ),
                        Expanded(
                          child: cards.isEmpty
                              ? const EmptyState(icon: Icons.inbox_outlined, message: 'Nenhum card nesta coluna.')
                              : ListView.separated(
                                  padding: const EdgeInsets.fromLTRB(16, 4, 16, 16),
                                  itemCount: cards.length,
                                  separatorBuilder: (_, _) => const SizedBox(height: 10),
                                  itemBuilder: (context, i) => _TaskCardTile(card: cards[i], onMove: () => _moveCard(cards[i], b)),
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

class _TaskCardTile extends StatelessWidget {
  const _TaskCardTile({required this.card, required this.onMove});
  final TaskCard card;
  final VoidCallback onMove;

  bool get _isOverdue {
    if (card.dueDate == null) return false;
    final due = DateTime.tryParse(card.dueDate!);
    if (due == null) return false;
    final today = DateTime.now();
    return due.isBefore(DateTime(today.year, today.month, today.day));
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.steel200),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Text(card.title, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 14)),
              ),
              InkWell(
                borderRadius: BorderRadius.circular(8),
                onTap: onMove,
                child: const Padding(
                  padding: EdgeInsets.all(2),
                  child: Icon(Icons.swap_horiz, size: 19, color: AppColors.steel500),
                ),
              ),
            ],
          ),
          if (card.description != null && card.description!.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(
              card.description!,
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 12.5, color: AppColors.steel500),
            ),
          ],
          if (card.dueDate != null || card.commentCount > 0 || card.attachmentCount > 0) ...[
            const SizedBox(height: 10),
            Row(
              children: [
                if (card.dueDate != null)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
                    decoration: BoxDecoration(
                      color: _isOverdue ? AppColors.safety50 : AppColors.steel100,
                      borderRadius: BorderRadius.circular(999),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.event_outlined, size: 11, color: _isOverdue ? AppColors.safety600 : AppColors.steel500),
                        const SizedBox(width: 3),
                        Text(
                          card.dueDate!,
                          style: TextStyle(fontSize: 10.5, fontWeight: FontWeight.w700, color: _isOverdue ? AppColors.safety600 : AppColors.steel600),
                        ),
                      ],
                    ),
                  ),
                const Spacer(),
                if (card.commentCount > 0) ...[
                  const Icon(Icons.mode_comment_outlined, size: 13, color: AppColors.steel400),
                  const SizedBox(width: 3),
                  Text('${card.commentCount}', style: const TextStyle(fontSize: 11, color: AppColors.steel500)),
                  const SizedBox(width: 10),
                ],
                if (card.attachmentCount > 0) ...[
                  const Icon(Icons.attach_file, size: 13, color: AppColors.steel400),
                  const SizedBox(width: 3),
                  Text('${card.attachmentCount}', style: const TextStyle(fontSize: 11, color: AppColors.steel500)),
                ],
              ],
            ),
          ],
        ],
      ),
    );
  }
}
