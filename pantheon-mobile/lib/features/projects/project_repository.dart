import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';

class DocumentFolder {
  DocumentFolder({required this.id, required this.name});
  factory DocumentFolder.fromJson(Map<String, dynamic> json) =>
      DocumentFolder(id: json['id'] as String, name: json['name'] as String);
  final String id;
  final String name;
}

class DocumentFile {
  DocumentFile({required this.id, required this.originalName, required this.contentType});
  factory DocumentFile.fromJson(Map<String, dynamic> json) => DocumentFile(
        id: json['id'] as String,
        originalName: json['originalName'] as String,
        contentType: json['contentType'] as String,
      );
  final String id;
  final String originalName;
  final String contentType;
}

class FolderContents {
  FolderContents({required this.folders, required this.files});
  factory FolderContents.fromJson(Map<String, dynamic> json) => FolderContents(
        folders: (json['folders'] as List).map((e) => DocumentFolder.fromJson(e as Map<String, dynamic>)).toList(),
        files: (json['files'] as List).map((e) => DocumentFile.fromJson(e as Map<String, dynamic>)).toList(),
      );
  final List<DocumentFolder> folders;
  final List<DocumentFile> files;
}

/// Read-only projects/document browser for this pass — folder creation and file upload stay
/// web-only (see design.md "simplified screens" decision).
class ProjectRepository {
  ProjectRepository(this._client);
  final ApiClient _client;

  Future<FolderContents> listContents(String siteId, String? parentId) async {
    final query = parentId != null ? '?parentId=$parentId' : '';
    // Projetos must not work offline (see design.md's offline-support scoping) — never cached.
    final json = await _client.get<Map<String, dynamic>>('/api/sites/$siteId/projects/contents$query', offlineCapable: false);
    return FolderContents.fromJson(json);
  }
}

final projectRepositoryProvider = Provider((ref) => ProjectRepository(ref.watch(apiClientProvider)));
