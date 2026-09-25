/// Mirrors `pantheon-web`'s `DailyReportDetailView.vue` `onSaveCore` guard: weather condition
/// and both work-hours fields are mandatory together — a save can't go through with only some of
/// them filled in. `weatherBlockedTasks` and `comments` aren't part of this rule (optional either
/// way). Pulled out as a standalone function so it's testable without a widget harness.
bool dailyReportCoreFieldsComplete({required String weatherCondition, required String workHoursStart, required String workHoursEnd}) {
  return weatherCondition.isNotEmpty && workHoursStart.isNotEmpty && workHoursEnd.isNotEmpty;
}

class DailyReport {
  DailyReport({
    required this.id,
    required this.constructionSiteId,
    required this.reportDate,
    required this.sequenceNo,
    required this.status,
    this.weatherCondition,
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
        weatherCondition: json['weatherCondition'] as String?,
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
  final String? weatherCondition;
  final bool? weatherBlockedTasks;
  final String? workHoursStart;
  final String? workHoursEnd;
  final String? comments;
  final String? submittedAt;
}

class WorkforceEntry {
  WorkforceEntry({required this.id, required this.roleDescription, required this.headcount});

  factory WorkforceEntry.fromJson(Map<String, dynamic> json) => WorkforceEntry(
        id: json['id'] as String,
        roleDescription: json['roleDescription'] as String,
        headcount: json['headcount'] as int,
      );

  final String id;
  final String roleDescription;
  final int headcount;
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

class DailyReportDetail {
  DailyReportDetail({required this.report, required this.workforceEntries, required this.activities});

  factory DailyReportDetail.fromJson(Map<String, dynamic> json) => DailyReportDetail(
        report: DailyReport.fromJson(json['report'] as Map<String, dynamic>),
        workforceEntries:
            (json['workforceEntries'] as List).map((e) => WorkforceEntry.fromJson(e as Map<String, dynamic>)).toList(),
        activities: (json['activities'] as List).map((e) => ReportActivity.fromJson(e as Map<String, dynamic>)).toList(),
      );

  final DailyReport report;
  final List<WorkforceEntry> workforceEntries;
  final List<ReportActivity> activities;
}
