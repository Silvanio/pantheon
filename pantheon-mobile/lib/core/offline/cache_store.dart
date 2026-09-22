import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:sqflite/sqflite.dart';

import 'offline_db.dart';

/// Last-known-value cache for GET responses, keyed by request path+query. Read as a fallback
/// when a live request fails for connectivity reasons — see `ApiClient.get`.
class CacheStore {
  CacheStore(this._db);
  final OfflineDb _db;

  Future<void> put(String key, Object? body) async {
    final db = await _db.open();
    await db.insert(
      'cache',
      {'key': key, 'body': jsonEncode(body), 'updated_at': DateTime.now().millisecondsSinceEpoch},
      conflictAlgorithm: ConflictAlgorithm.replace,
    );
  }

  Future<Object?> get(String key) async {
    final db = await _db.open();
    final rows = await db.query('cache', where: 'key = ?', whereArgs: [key], limit: 1);
    if (rows.isEmpty) return null;
    return jsonDecode(rows.first['body'] as String);
  }
}

final cacheStoreProvider = Provider((ref) => CacheStore(ref.watch(offlineDbProvider)));
