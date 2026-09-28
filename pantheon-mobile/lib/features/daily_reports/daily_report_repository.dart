import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';
import '../../core/models/page_response.dart';
import 'daily_report_models.dart';

class DailyReportRepository {
  DailyReportRepository(this._client);
  final ApiClient _client;

  /// The backend paginates this endpoint (see `PageResponse`); mobile has no pagination UI here
  /// (same precedent as `PurchaseRequestRepository.list`), so it just requests one generously
  /// sized page and returns its content flat.
  Future<List<DailyReport>> list(String siteId) async {
    final json = await _client.get<Map<String, dynamic>>(
      '/api/construction-sites/$siteId/daily-reports',
      query: {'size': 100},
    );
    final page = PageResponse.fromJson(json, (e) => DailyReport.fromJson(e));
    return page.content;
  }

  /// Returns `true` if sent live, `false` if queued for later (offline) — see
  /// `ApiClient.mutateQueueable`. The caller doesn't need the created record back (the list just
  /// refreshes), which is what makes this safe to queue.
  Future<bool> create(String siteId, String reportDate) => _client.mutateQueueable(
        method: 'POST',
        path: '/api/construction-sites/$siteId/daily-reports',
        body: {'reportDate': reportDate},
        entityLabel: 'Novo relatório diário',
      );

  Future<DailyReportDetail> getDetail(String reportId) async {
    final json = await _client.get<Map<String, dynamic>>('/api/daily-reports/$reportId');
    return DailyReportDetail.fromJson(json);
  }

  Future<DailyReport> updateCore(
    String reportId, {
    String? weatherCondition,
    bool? weatherBlockedTasks,
    String? workHoursStart,
    String? workHoursEnd,
    String? comments,
  }) async {
    final json = await _client.patch<Map<String, dynamic>>(
      '/api/daily-reports/$reportId',
      body: {
        'weatherCondition': weatherCondition,
        'weatherBlockedTasks': weatherBlockedTasks,
        'workHoursStart': workHoursStart,
        'workHoursEnd': workHoursEnd,
        'comments': comments,
      },
    );
    return DailyReport.fromJson(json);
  }

  // Submit/approve/reject are the approval workflow itself (see
  // `daily-report-approval-workflow`), which requires being online — mirroring
  // `PurchaseRequestRepository`'s equivalent methods exactly (design.md's offline-support
  // scoping) — never queued. The screen calls `requireOnline` before invoking these, so a
  // failure here should be rare (a race where connectivity dropped in between), and just
  // surfaces as a normal error.
  Future<void> submit(String reportId) async {
    await _client.post<dynamic>('/api/daily-reports/$reportId/submit');
  }

  Future<void> approveStep(String reportId, {String? comment}) async {
    final query = (comment != null && comment.isNotEmpty) ? '?comment=${Uri.encodeComponent(comment)}' : '';
    await _client.post<dynamic>('/api/daily-reports/$reportId/approve-step$query');
  }

  Future<void> rejectStep(String reportId, String reason) async {
    await _client.post<dynamic>('/api/daily-reports/$reportId/reject-step', body: {'reason': reason});
  }

  /// Deleting is a destructive, irreversible action — like the purchase-request approval
  /// workflow, it's never queued offline. The screen calls `requireOnline` before invoking this.
  Future<void> delete(String reportId) => _client.delete<dynamic>('/api/daily-reports/$reportId');

  Future<List<ReportMedia>> listMedia(String reportId) async {
    final json = await _client.get<List<dynamic>>('/api/daily-reports/$reportId/media');
    return json.map((e) => ReportMedia.fromJson(e as Map<String, dynamic>)).toList();
  }

  /// Returns `true` if sent live, `false` if queued for later (offline). The local file path is
  /// what gets queued, so it must still exist on disk when connectivity returns — see
  /// `OutboxController.syncNow`'s "file not found" handling for the (rare) case it doesn't.
  Future<bool> uploadPhoto(String reportId, String filePath, {String? caption}) {
    final query = StringBuffer('?type=PHOTO');
    if (caption != null && caption.isNotEmpty) query.write('&caption=${Uri.encodeComponent(caption)}');
    return _client.uploadQueueable(
      path: '/api/daily-reports/$reportId/media$query',
      fileFieldName: 'file',
      localFilePath: filePath,
      entityLabel: 'Foto do diário de obra',
    );
  }
}

final dailyReportRepositoryProvider = Provider((ref) => DailyReportRepository(ref.watch(apiClientProvider)));
