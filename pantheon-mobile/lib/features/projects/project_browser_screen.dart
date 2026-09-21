import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/widgets/async_value_view.dart';
import '../../theme/app_colors.dart';
import 'project_repository.dart';

final _contentsProvider = FutureProvider.family<FolderContents, (String, String?)>(
  (ref, args) => ref.watch(projectRepositoryProvider).listContents(args.$1, args.$2),
);

/// Read-only folder browser — creating folders/uploading files stays web-only in this pass
/// (see design.md).
class ProjectBrowserScreen extends ConsumerStatefulWidget {
  const ProjectBrowserScreen({super.key, required this.siteId});
  final String siteId;

  @override
  ConsumerState<ProjectBrowserScreen> createState() => _ProjectBrowserScreenState();
}

class _ProjectBrowserScreenState extends ConsumerState<ProjectBrowserScreen> {
  final List<DocumentFolder> _stack = [];

  @override
  Widget build(BuildContext context) {
    final currentFolderId = _stack.isEmpty ? null : _stack.last.id;
    final contents = ref.watch(_contentsProvider((widget.siteId, currentFolderId)));

    return Scaffold(
      appBar: AppBar(
        title: Text(_stack.isEmpty ? 'Projetos' : _stack.last.name),
        leading: _stack.isNotEmpty
            ? IconButton(icon: const Icon(Icons.arrow_back), onPressed: () => setState(() => _stack.removeLast()))
            : null,
      ),
      body: AsyncValueView(
        value: contents,
        data: (c) {
          if (c.folders.isEmpty && c.files.isEmpty) {
            return ListView(children: const [EmptyState(message: 'Nenhum arquivo ou pasta aqui ainda.')]);
          }
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              ...c.folders.map(
                (f) => Card(
                  child: ListTile(
                    leading: const Icon(Icons.folder_outlined, color: AppColors.blueprint600),
                    title: Text(f.name, style: const TextStyle(fontWeight: FontWeight.w700)),
                    trailing: const Icon(Icons.chevron_right),
                    onTap: () => setState(() => _stack.add(f)),
                  ),
                ),
              ),
              ...c.files.map(
                (f) => Card(
                  child: ListTile(
                    leading: const Icon(Icons.insert_drive_file_outlined, color: AppColors.steel500),
                    title: Text(f.originalName),
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}
