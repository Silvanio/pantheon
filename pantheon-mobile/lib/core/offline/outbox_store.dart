import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'offline_db.dart';
import 'outbox_entry.dart';

class OutboxStore {
  OutboxStore(this._db);
  final OfflineDb _db;

  Future<void> insert(OutboxEntry entry) async {
    final db = await _db.open();
    await db.insert('outbox', entry.toRow());
  }

  Future<List<OutboxEntry>> listAll() async {
    final db = await _db.open();
    final rows = await db.query('outbox', orderBy: 'created_at ASC');
    return rows.map(OutboxEntry.fromRow).toList();
  }

  Future<void> delete(String id) async {
    final db = await _db.open();
    await db.delete('outbox', where: 'id = ?', whereArgs: [id]);
  }

  Future<void> recordError(String id, String error) async {
    final db = await _db.open();
    await db.rawUpdate(
      'UPDATE outbox SET attempts = attempts + 1, last_error = ? WHERE id = ?',
      [error, id],
    );
  }
}

final outboxStoreProvider = Provider((ref) => OutboxStore(ref.watch(offlineDbProvider)));
