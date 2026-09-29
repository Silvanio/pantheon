import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';
import '../../core/models/page_response.dart';
import '../materials/material_models.dart';
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
    String? weatherConditionMorning,
    String? weatherConditionAfternoon,
    bool? weatherBlockedTasks,
    String? workHoursStart,
    String? workHoursEnd,
    String? comments,
  }) async {
    final json = await _client.patch<Map<String, dynamic>>(
      '/api/daily-reports/$reportId',
      body: {
        'weatherConditionMorning': weatherConditionMorning,
        'weatherConditionAfternoon': weatherConditionAfternoon,
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

  // --- Mão de obra (`redesign-daily-report-experience` group 9.2) ---
  //
  // Add/delete are structural edits to a draft report, not the "captured a photo out in the
  // field with a flaky signal" case `uploadPhoto` is queueable for — like submit/approve/reject
  // above, and like the global save's `updateCore` call, these require being online (the screen's
  // one "Salvar alterações" action calls `requireOnline` once before firing any of them).

  Future<WorkforceEntry> addWorkforceEntry(String reportId, {String? membershipId, String? roleDescription, required int headcount}) async {
    final json = await _client.post<Map<String, dynamic>>(
      '/api/daily-reports/$reportId/workforce-entries',
      body: {'membershipId': membershipId, 'roleDescription': roleDescription, 'headcount': headcount},
    );
    return WorkforceEntry.fromJson(json);
  }

  Future<void> deleteWorkforceEntry(String reportId, String entryId) =>
      _client.delete<dynamic>('/api/daily-reports/$reportId/workforce-entries/$entryId');

  // --- Equipamentos (group 9.2 — net-new on mobile) ---

  Future<EquipmentUsage> addEquipmentUsage(String reportId, {String? equipmentId, String? customName, String? statusNote}) async {
    final json = await _client.post<Map<String, dynamic>>(
      '/api/daily-reports/$reportId/equipment-usage',
      body: {'equipmentId': equipmentId, 'customName': customName, 'statusNote': statusNote},
    );
    return EquipmentUsage.fromJson(json);
  }

  Future<void> deleteEquipmentUsage(String reportId, String usageId) =>
      _client.delete<dynamic>('/api/daily-reports/$reportId/equipment-usage/$usageId');

  // --- Atividades ---

  Future<ReportActivity> addActivity(String reportId, {required String description, required String progressNote, required String status}) async {
    final json = await _client.post<Map<String, dynamic>>(
      '/api/daily-reports/$reportId/activities',
      body: {'description': description, 'progressNote': progressNote, 'status': status},
    );
    return ReportActivity.fromJson(json);
  }

  Future<void> deleteActivity(String reportId, String activityId) =>
      _client.delete<dynamic>('/api/daily-reports/$reportId/activities/$activityId');

  // --- Materiais recebidos: read-only view sourced from the real delivery-tracking `Material`
  // records (group 4), not `DailyReportMaterialReceived` — see daily_report_detail_screen.dart's
  // doc comment on why mobile only builds this half of "Materiais recebidos". Reuses the same
  // `MaterialDelivery` model/shape the Materiais tab already parses (`MaterialResponse` on the
  // backend) — no separate model needed.

  Future<List<MaterialDelivery>> listDeliveredMaterials(String reportId) async {
    final json = await _client.get<List<dynamic>>('/api/daily-reports/$reportId/delivered-materials');
    return json.map((e) => MaterialDelivery.fromJson(e as Map<String, dynamic>)).toList();
  }

  // --- Mídia & Anexos ---

  Future<List<ReportMedia>> listMedia(String reportId) async {
    final json = await _client.get<List<dynamic>>('/api/daily-reports/$reportId/media');
    return json.map((e) => ReportMedia.fromJson(e as Map<String, dynamic>)).toList();
  }

  /// Returns `true` if sent live, `false` if queued for later (offline). The local file path is
  /// what gets queued, so it must still exist on disk when connectivity returns — see
  /// `OutboxController.syncNow`'s "file not found" handling for the (rare) case it doesn't.
  /// `type` is `'PHOTO'` or `'VIDEO'` (see the backend's `MediaType`).
  Future<bool> uploadMedia(String reportId, String filePath, {required String type, String? caption}) {
    final query = StringBuffer('?type=$type');
    if (caption != null && caption.isNotEmpty) query.write('&caption=${Uri.encodeComponent(caption)}');
    return _client.uploadQueueable(
      path: '/api/daily-reports/$reportId/media$query',
      fileFieldName: 'file',
      localFilePath: filePath,
      entityLabel: type == 'VIDEO' ? 'Vídeo do diário de obra' : 'Foto do diário de obra',
    );
  }

  Future<ReportMedia> updateMediaCaption(String reportId, String mediaId, String? caption) async {
    final json = await _client.patch<Map<String, dynamic>>(
      '/api/daily-reports/$reportId/media/$mediaId',
      body: {'caption': caption},
    );
    return ReportMedia.fromJson(json);
  }

  Future<void> deleteMedia(String reportId, String mediaId) =>
      _client.delete<dynamic>('/api/daily-reports/$reportId/media/$mediaId');

  Future<List<int>> getMediaThumbnail(String reportId, String mediaId) =>
      _client.getBytes('/api/daily-reports/$reportId/media/$mediaId/thumbnail');

  /// Full-resolution original — only for the enlarged detail view, never the grid (see getMediaThumbnail).
  Future<List<int>> getMediaContent(String reportId, String mediaId) =>
      _client.getBytes('/api/daily-reports/$reportId/media/$mediaId/content');

  Future<List<ReportAttachment>> listAttachments(String reportId) async {
    final json = await _client.get<List<dynamic>>('/api/daily-reports/$reportId/attachments');
    return json.map((e) => ReportAttachment.fromJson(e as Map<String, dynamic>)).toList();
  }

  /// Attachment upload isn't queued offline (unlike photo/video media) — a document attachment
  /// (invoice, plan revision, ...) is normally added from the office with a live connection, and
  /// keeping it synchronous keeps the "does this exist yet" state unambiguous for the immediately
  /// following caption-less display. The screen calls `requireOnline` first.
  Future<ReportAttachment> uploadAttachment(String reportId, String filePath) async {
    final formData = FormData.fromMap({'file': await MultipartFile.fromFile(filePath)});
    final json = await _client.uploadMultipart<Map<String, dynamic>>('/api/daily-reports/$reportId/attachments', formData);
    return ReportAttachment.fromJson(json);
  }

  Future<void> deleteAttachment(String reportId, String attachmentId) =>
      _client.delete<dynamic>('/api/daily-reports/$reportId/attachments/$attachmentId');

  Future<List<int>> getAttachmentContent(String reportId, String attachmentId) =>
      _client.getBytes('/api/daily-reports/$reportId/attachments/$attachmentId/content');

  Future<List<ImportedInvoice>> listImportedInvoices(String reportId) async {
    final json = await _client.get<List<dynamic>>('/api/daily-reports/$reportId/imported-invoices');
    return json.map((e) => ImportedInvoice.fromJson(e as Map<String, dynamic>)).toList();
  }

  /// Reads a same-day imported invoice's file content via the existing, unrelated
  /// `pantheon-service` `PurchaseRequestInvoice` content endpoint (see
  /// `DailyReportImportedInvoiceResponse`'s doc comment) rather than anything under
  /// `/api/daily-reports/...` — this is a reference, not a copy.
  Future<List<int>> getImportedInvoiceContent(String invoiceId) => _client.getBytes('/api/purchase-request-invoices/$invoiceId/content');
}

final dailyReportRepositoryProvider = Provider((ref) => DailyReportRepository(ref.watch(apiClientProvider)));
