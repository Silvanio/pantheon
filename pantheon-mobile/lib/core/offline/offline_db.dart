import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path/path.dart' as p;
import 'package:sqflite/sqflite.dart';

/// Local SQLite database backing offline support: `cache` (last-known GET responses, read as a
/// fallback when there's no connectivity) and `outbox` (pending mutations queued while offline,
/// replayed once connectivity returns — see design.md's offline-support decision).
class OfflineDb {
  Database? _db;

  Future<Database> open() async {
    final existing = _db;
    if (existing != null) return existing;
    final dbPath = await getDatabasesPath();
    final db = await openDatabase(
      p.join(dbPath, 'pantheon_offline.db'),
      version: 1,
      onCreate: (db, version) async {
        await db.execute('''
          CREATE TABLE cache (
            key TEXT PRIMARY KEY,
            body TEXT NOT NULL,
            updated_at INTEGER NOT NULL
          )
        ''');
        await db.execute('''
          CREATE TABLE outbox (
            id TEXT PRIMARY KEY,
            method TEXT NOT NULL,
            path TEXT NOT NULL,
            body_json TEXT,
            file_field_name TEXT,
            local_file_path TEXT,
            upload_fields_json TEXT,
            entity_label TEXT NOT NULL,
            created_at INTEGER NOT NULL,
            attempts INTEGER NOT NULL DEFAULT 0,
            last_error TEXT
          )
        ''');
      },
    );
    _db = db;
    return db;
  }
}

final offlineDbProvider = Provider((ref) => OfflineDb());
