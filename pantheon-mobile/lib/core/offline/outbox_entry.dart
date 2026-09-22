/// A queued mutation, either a plain JSON-body request or a file upload, waiting to be replayed
/// against the backend once connectivity returns. See `OutboxStore`/`OutboxController`.
class OutboxEntry {
  const OutboxEntry({
    required this.id,
    required this.method,
    required this.path,
    required this.entityLabel,
    required this.createdAt,
    required this.attempts,
    this.bodyJson,
    this.fileFieldName,
    this.localFilePath,
    this.uploadFieldsJson,
    this.lastError,
  });

  factory OutboxEntry.fromRow(Map<String, Object?> row) => OutboxEntry(
        id: row['id'] as String,
        method: row['method'] as String,
        path: row['path'] as String,
        bodyJson: row['body_json'] as String?,
        fileFieldName: row['file_field_name'] as String?,
        localFilePath: row['local_file_path'] as String?,
        uploadFieldsJson: row['upload_fields_json'] as String?,
        entityLabel: row['entity_label'] as String,
        createdAt: DateTime.fromMillisecondsSinceEpoch(row['created_at'] as int),
        attempts: row['attempts'] as int,
        lastError: row['last_error'] as String?,
      );

  final String id;
  final String method;
  final String path;
  final String? bodyJson;
  final String? fileFieldName;
  final String? localFilePath;
  final String? uploadFieldsJson;
  final String entityLabel;
  final DateTime createdAt;
  final int attempts;
  final String? lastError;

  bool get isUpload => localFilePath != null;

  Map<String, Object?> toRow() => {
        'id': id,
        'method': method,
        'path': path,
        'body_json': bodyJson,
        'file_field_name': fileFieldName,
        'local_file_path': localFilePath,
        'upload_fields_json': uploadFieldsJson,
        'entity_label': entityLabel,
        'created_at': createdAt.millisecondsSinceEpoch,
        'attempts': attempts,
        'last_error': lastError,
      };
}
