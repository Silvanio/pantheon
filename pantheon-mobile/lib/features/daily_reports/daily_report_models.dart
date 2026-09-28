/// Mirrors `pantheon-web`'s `DailyReportDetailView.vue` `onSaveCore` guard: both weather
/// conditions (morning and afternoon) and both work-hours fields are mandatory together — a save
/// can't go through with only some of them filled in. `weatherBlockedTasks` and `comments` aren't
/// part of this rule (optional either way). Pulled out as a standalone function so it's testable
/// without a widget harness.
bool dailyReportCoreFieldsComplete({
  required String weatherConditionMorning,
  required String weatherConditionAfternoon,
  required String workHoursStart,
  required String workHoursEnd,
}) {
  return weatherConditionMorning.isNotEmpty &&
      weatherConditionAfternoon.isNotEmpty &&
      workHoursStart.isNotEmpty &&
      workHoursEnd.isNotEmpty;
}

/// The "global save" orchestration (`redesign-daily-report-experience` design.md Decision 1) adds
/// or removes rows locally before a single "Salvar alterações" fires the actual network calls.
/// This computes which server-known ids are no longer present in the current local working set —
/// i.e. which rows must be `DELETE`d on save — factored out as a pure function so it's unit
/// testable without standing up the whole screen. `currentIds` may contain ids not present in
/// `originalIds` (locally-added, not-yet-created rows) — those are simply ignored here, since a
/// row that never existed on the server needs no delete call.
Set<String> staleServerIds({required Set<String> originalIds, required Iterable<String?> currentIds}) {
  final stillPresent = currentIds.whereType<String>().toSet();
  return originalIds.difference(stillPresent);
}

class DailyReport {
  DailyReport({
    required this.id,
    required this.constructionSiteId,
    required this.reportDate,
    required this.sequenceNo,
    required this.status,
    this.weatherConditionMorning,
    this.weatherConditionAfternoon,
    this.weatherBlockedTasks,
    this.workHoursStart,
    this.workHoursEnd,
    this.comments,
    this.submittedAt,
  });

  factory DailyReport.fromJson(Map<String, dynamic> json) => DailyReport(
        id: json['id'] as String,
        constructionSiteId: json['constructionSiteId'] as String,
        reportDate: json['reportDate'] as String,
        sequenceNo: json['sequenceNo'] as int,
        status: json['status'] as String,
        weatherConditionMorning: json['weatherConditionMorning'] as String?,
        weatherConditionAfternoon: json['weatherConditionAfternoon'] as String?,
        weatherBlockedTasks: json['weatherBlockedTasks'] as bool?,
        workHoursStart: json['workHoursStart'] as String?,
        workHoursEnd: json['workHoursEnd'] as String?,
        comments: json['comments'] as String?,
        submittedAt: json['submittedAt'] as String?,
      );

  final String id;
  final String constructionSiteId;
  final String reportDate;
  final int sequenceNo;
  final String status;
  final String? weatherConditionMorning;
  final String? weatherConditionAfternoon;
  final bool? weatherBlockedTasks;
  final String? workHoursStart;
  final String? workHoursEnd;
  final String? comments;
  final String? submittedAt;
}

/// `membershipId` links this entry to a registered `SiteMember` (rendered with their name +
/// function, avatar-style); `null` means it's a free-text "outra mão de obra" entry (rendered
/// dashed, per `DailyReportMobile.dc.html`). Either way `roleDescription` is always populated —
/// the backend auto-fills it from the member's function when `membershipId` is given (see
/// `WorkforceEntryRequest`'s doc comment) — so it's always safe to show as the row's subtitle.
class WorkforceEntry {
  WorkforceEntry({required this.id, this.membershipId, required this.roleDescription, required this.headcount});

  factory WorkforceEntry.fromJson(Map<String, dynamic> json) => WorkforceEntry(
        id: json['id'] as String,
        membershipId: json['membershipId'] as String?,
        roleDescription: json['roleDescription'] as String,
        headcount: json['headcount'] as int,
      );

  final String id;
  final String? membershipId;
  final String roleDescription;
  final int headcount;
}

/// Mirrors the backend's `EquipmentUsageResponse` — `equipmentId` set means a reference to the
/// site's registered `Equipment` (name/status resolved client-side against the equipment
/// registry); `customName` set (and `equipmentId` null) is an "outro equipamento" free-text entry
/// (see `redesign-daily-report-experience` design.md Decision 2). Exactly one of the two is
/// non-null, enforced server-side.
class EquipmentUsage {
  EquipmentUsage({required this.id, this.equipmentId, this.customName, this.statusNote});

  factory EquipmentUsage.fromJson(Map<String, dynamic> json) => EquipmentUsage(
        id: json['id'] as String,
        equipmentId: json['equipmentId'] as String?,
        customName: json['customName'] as String?,
        statusNote: json['statusNote'] as String?,
      );

  final String id;
  final String? equipmentId;
  final String? customName;
  final String? statusNote;
}

class ReportActivity {
  ReportActivity({required this.id, required this.description, required this.progressNote, required this.status});

  factory ReportActivity.fromJson(Map<String, dynamic> json) => ReportActivity(
        id: json['id'] as String,
        description: json['description'] as String,
        progressNote: json['progressNote'] as String,
        status: json['status'] as String,
      );

  final String id;
  final String description;
  final String progressNote;
  final String status;
}

class ReportMedia {
  ReportMedia({required this.id, required this.type, this.caption});

  factory ReportMedia.fromJson(Map<String, dynamic> json) =>
      ReportMedia(id: json['id'] as String, type: json['type'] as String, caption: json['caption'] as String?);

  final String id;
  final String type;
  final String? caption;
}

/// Mirrors the backend's `DailyReportAttachmentResponse` (`redesign-daily-report-experience`
/// group 2.5 renamed its timestamp field to `uploadedAt`).
class ReportAttachment {
  ReportAttachment({required this.id, required this.originalName, this.contentType, this.uploadedAt});

  factory ReportAttachment.fromJson(Map<String, dynamic> json) => ReportAttachment(
        id: json['id'] as String,
        originalName: json['originalName'] as String,
        contentType: json['contentType'] as String?,
        uploadedAt: json['uploadedAt'] as String?,
      );

  final String id;
  final String originalName;
  final String? contentType;
  final String? uploadedAt;
}

/// A same-day Pedido de Compra "nota fiscal" surfaced read-only in the Anexos list — mirrors the
/// backend's `DailyReportImportedInvoiceResponse` (group 5.2). Not a copy: its content is fetched
/// from the existing `GET /api/purchase-request-invoices/{id}/content`.
class ImportedInvoice {
  ImportedInvoice({
    required this.id,
    required this.purchaseRequestId,
    this.purchaseRequestName,
    required this.originalName,
    this.contentType,
    this.uploadedAt,
  });

  factory ImportedInvoice.fromJson(Map<String, dynamic> json) => ImportedInvoice(
        id: json['id'] as String,
        purchaseRequestId: json['purchaseRequestId'] as String,
        purchaseRequestName: json['purchaseRequestName'] as String?,
        originalName: json['originalName'] as String,
        contentType: json['contentType'] as String?,
        uploadedAt: json['uploadedAt'] as String?,
      );

  final String id;
  final String purchaseRequestId;
  final String? purchaseRequestName;
  final String originalName;
  final String? contentType;
  final String? uploadedAt;
}

/// Mirrors `pantheon-mobile`'s `PurchaseRequestApproval` (see `purchase_request_models.dart`) —
/// same shape as the backend's `DailyReportApprovalResponse`, trimmed to the fields the approval
/// UI actually needs (see `daily-report-approval-workflow`'s design.md Decision 1: parallel,
/// independently-duplicated entities/DTOs rather than a shared type). `decidedByName`/`decidedAt`
/// are new (group 6.1/9.3): `null` while the step is still `PENDING`.
class DailyReportApproval {
  DailyReportApproval({
    required this.id,
    required this.cycleNumber,
    required this.stepOrder,
    required this.approverFunction,
    required this.status,
    required this.comment,
    this.decidedByName,
    this.decidedAt,
  });

  factory DailyReportApproval.fromJson(Map<String, dynamic> json) => DailyReportApproval(
        id: json['id'] as String,
        cycleNumber: json['cycleNumber'] as int,
        stepOrder: json['stepOrder'] as int,
        approverFunction: json['approverFunction'] as String,
        status: json['status'] as String,
        comment: json['comment'] as String?,
        decidedByName: json['decidedByName'] as String?,
        decidedAt: json['decidedAt'] as String?,
      );

  final String id;
  final int cycleNumber;
  final int stepOrder;
  final String approverFunction;
  final String status;
  final String? comment;
  final String? decidedByName;
  final String? decidedAt;
}

class DailyReportDetail {
  DailyReportDetail({
    required this.report,
    required this.workforceEntries,
    required this.equipmentUsage,
    required this.activities,
    required this.approvals,
  });

  factory DailyReportDetail.fromJson(Map<String, dynamic> json) => DailyReportDetail(
        report: DailyReport.fromJson(json['report'] as Map<String, dynamic>),
        workforceEntries:
            (json['workforceEntries'] as List).map((e) => WorkforceEntry.fromJson(e as Map<String, dynamic>)).toList(),
        equipmentUsage:
            (json['equipmentUsage'] as List? ?? const []).map((e) => EquipmentUsage.fromJson(e as Map<String, dynamic>)).toList(),
        activities: (json['activities'] as List).map((e) => ReportActivity.fromJson(e as Map<String, dynamic>)).toList(),
        approvals:
            (json['approvals'] as List).map((e) => DailyReportApproval.fromJson(e as Map<String, dynamic>)).toList(),
      );

  final DailyReport report;
  final List<WorkforceEntry> workforceEntries;
  final List<EquipmentUsage> equipmentUsage;
  final List<ReportActivity> activities;
  final List<DailyReportApproval> approvals;
}
