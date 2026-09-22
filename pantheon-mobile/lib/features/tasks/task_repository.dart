import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';
import 'task_models.dart';

class TaskRepository {
  TaskRepository(this._client);
  final ApiClient _client;

  Future<TaskBoard> getBoard(String siteId) async {
    // Tasks must not work offline (see design.md's offline-support scoping) — never cached.
    final json = await _client.get<Map<String, dynamic>>('/api/construction-sites/$siteId/task-board', offlineCapable: false);
    return TaskBoard.fromJson(json);
  }

  Future<void> moveCard(String cardId, String columnId, int sortOrder) async {
    await _client.patch<dynamic>('/api/task-cards/$cardId/move', body: {'columnId': columnId, 'sortOrder': sortOrder});
  }

  Future<TaskCard> createCard(String siteId, String columnId, String title, String? description) async {
    final json = await _client.post<Map<String, dynamic>>(
      '/api/construction-sites/$siteId/task-cards',
      body: {'columnId': columnId, 'title': title, 'description': description, 'dueDate': null},
    );
    return TaskCard.fromJson(json);
  }
}

final taskRepositoryProvider = Provider((ref) => TaskRepository(ref.watch(apiClientProvider)));
