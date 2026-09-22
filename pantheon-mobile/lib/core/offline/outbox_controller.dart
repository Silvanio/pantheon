import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:uuid/uuid.dart';

import '../api/api_client.dart';
import 'outbox_entry.dart';
import 'outbox_store.dart';

class OutboxState {
  const OutboxState({required this.pending, required this.isOnline, required this.isSyncing});

  static const initial = OutboxState(pending: [], isOnline: true, isSyncing: false);

  final List<OutboxEntry> pending;
  final bool isOnline;
  final bool isSyncing;

  OutboxState copyWith({List<OutboxEntry>? pending, bool? isOnline, bool? isSyncing}) => OutboxState(
        pending: pending ?? this.pending,
        isOnline: isOnline ?? this.isOnline,
        isSyncing: isSyncing ?? this.isSyncing,
      );
}

/// Tracks connectivity and the pending-actions outbox, and drives replaying it — the mobile
/// half of offline support (see design.md). `ApiClient` enqueues into this via [enqueue]/
/// [enqueueUpload] when a mutation fails for connectivity reasons instead of surfacing an
/// error; this controller drains the queue automatically on reconnect, or on demand via
/// [syncNow] (the "Sincronizar agora" action in the sync screen).
class OutboxController extends StateNotifier<OutboxState> {
  OutboxController(this._ref) : super(OutboxState.initial) {
    _init();
  }

  final Ref _ref;
  late final OutboxStore _store;
  StreamSubscription<List<ConnectivityResult>>? _connectivitySub;

  Future<void> _init() async {
    _store = _ref.read(outboxStoreProvider);
    await _refreshPending();

    final connectivity = Connectivity();
    try {
      final initial = await connectivity.checkConnectivity();
      state = state.copyWith(isOnline: _isOnline(initial));
    } catch (_) {
      // Plugin not available on this platform/target — assume online and let real requests
      // surface their own connectivity errors instead of blocking the app.
    }
    if (state.isOnline && state.pending.isNotEmpty) unawaited(syncNow());

    _connectivitySub = connectivity.onConnectivityChanged.listen((results) {
      final online = _isOnline(results);
      final wasOffline = !state.isOnline;
      state = state.copyWith(isOnline: online);
      if (online && wasOffline) unawaited(syncNow());
    });
  }

  bool _isOnline(List<ConnectivityResult> results) => results.any((r) => r != ConnectivityResult.none);

  @override
  void dispose() {
    _connectivitySub?.cancel();
    super.dispose();
  }

  Future<void> _refreshPending() async {
    state = state.copyWith(pending: await _store.listAll());
  }

  Future<void> enqueue({
    required String method,
    required String path,
    Object? body,
    required String entityLabel,
  }) async {
    await _store.insert(OutboxEntry(
      id: _newId(),
      method: method,
      path: path,
      bodyJson: body != null ? jsonEncode(body) : null,
      entityLabel: entityLabel,
      createdAt: DateTime.now(),
      attempts: 0,
    ));
    await _refreshPending();
  }

  Future<void> enqueueUpload({
    required String path,
    required String fileFieldName,
    required String localFilePath,
    Map<String, String> fields = const {},
    required String entityLabel,
  }) async {
    await _store.insert(OutboxEntry(
      id: _newId(),
      method: 'POST',
      path: path,
      fileFieldName: fileFieldName,
      localFilePath: localFilePath,
      uploadFieldsJson: jsonEncode(fields),
      entityLabel: entityLabel,
      createdAt: DateTime.now(),
      attempts: 0,
    ));
    await _refreshPending();
  }

  Future<void> discard(String id) async {
    await _store.delete(id);
    await _refreshPending();
  }

  /// Replays every queued entry, oldest first. Stops early if a connectivity error recurs
  /// (nothing else will succeed either); a per-entry server/business error is recorded and that
  /// entry is skipped so later ones still get a chance.
  Future<void> syncNow() async {
    if (state.isSyncing) return;
    state = state.copyWith(isSyncing: true);
    try {
      final apiClient = _ref.read(apiClientProvider);
      final entries = await _store.listAll();
      for (final entry in entries) {
        try {
          if (entry.isUpload) {
            if (!File(entry.localFilePath!).existsSync()) {
              await _store.recordError(entry.id, 'Arquivo local não encontrado — anexe novamente.');
              continue;
            }
            final fields = entry.uploadFieldsJson != null
                ? (jsonDecode(entry.uploadFieldsJson!) as Map).cast<String, String>()
                : <String, String>{};
            await apiClient.replayUpload(entry.path, entry.fileFieldName!, entry.localFilePath!, fields);
          } else {
            await apiClient.replayRaw(
              entry.method,
              entry.path,
              body: entry.bodyJson != null ? jsonDecode(entry.bodyJson!) : null,
            );
          }
          await _store.delete(entry.id);
        } on DioException catch (e) {
          if (ApiClient.isConnectivityError(e)) {
            state = state.copyWith(isOnline: false);
            break;
          }
          await _store.recordError(entry.id, e.response?.statusCode != null ? 'Erro ${e.response!.statusCode}' : (e.message ?? 'Falhou'));
        } catch (e) {
          debugPrint('[outbox] Unexpected error replaying ${entry.entityLabel}: $e');
          await _store.recordError(entry.id, 'Erro inesperado');
        }
      }
    } finally {
      await _refreshPending();
      state = state.copyWith(isSyncing: false);
    }
  }

  static const _uuid = Uuid();
  String _newId() => _uuid.v4();
}

final outboxControllerProvider = StateNotifierProvider<OutboxController, OutboxState>((ref) => OutboxController(ref));
