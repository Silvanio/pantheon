/// Mirrors `pantheon-service`'s `SiteSummaryResponse` and `pantheon-web`'s `useSiteSummary.ts`.
/// Every section is `null` when the caller's resolved access to the matching capability is
/// `HIDDEN` — the section is omitted, not shown empty/zeroed.
class SiteSummary {
  SiteSummary({
    required this.schedulePercentComplete,
    required this.dailyReports,
    required this.purchaseRequests,
    required this.orcamentos,
    required this.equipment,
    required this.projects,
    required this.tasks,
    required this.teamMembersCount,
  });

  factory SiteSummary.fromJson(Map<String, dynamic> json) => SiteSummary(
        schedulePercentComplete: json['schedulePercentComplete'] as int?,
        dailyReports: json['dailyReports'] == null
            ? null
            : DailyReportSummary.fromJson(json['dailyReports'] as Map<String, dynamic>),
        purchaseRequests: json['purchaseRequests'] == null
            ? null
            : PurchaseRequestSummary.fromJson(json['purchaseRequests'] as Map<String, dynamic>),
        orcamentos:
            json['orcamentos'] == null ? null : OrcamentoSummary.fromJson(json['orcamentos'] as Map<String, dynamic>),
        equipment:
            json['equipment'] == null ? null : EquipmentSummary.fromJson(json['equipment'] as Map<String, dynamic>),
        projects: json['projects'] == null ? null : CountStat.fromJson(json['projects'] as Map<String, dynamic>),
        tasks: json['tasks'] == null ? null : CountStat.fromJson(json['tasks'] as Map<String, dynamic>),
        teamMembersCount: json['teamMembersCount'] as int?,
      );

  final int? schedulePercentComplete;
  final DailyReportSummary? dailyReports;
  final PurchaseRequestSummary? purchaseRequests;
  final OrcamentoSummary? orcamentos;
  final EquipmentSummary? equipment;
  final CountStat? projects;
  final CountStat? tasks;
  final int? teamMembersCount;
}

class RecentItem {
  RecentItem({required this.id, required this.title, required this.createdAt});

  factory RecentItem.fromJson(Map<String, dynamic> json) => RecentItem(
        id: json['id'] as String,
        title: json['title'] as String,
        createdAt: DateTime.parse(json['createdAt'] as String),
      );

  final String id;
  final String title;
  final DateTime createdAt;

  bool get isNew => DateTime.now().difference(createdAt) <= const Duration(days: 7);
}

class CountStat {
  CountStat({required this.total, required this.recent});

  factory CountStat.fromJson(Map<String, dynamic> json) => CountStat(
        total: json['total'] as int,
        recent: (json['recent'] as List).map((e) => RecentItem.fromJson(e as Map<String, dynamic>)).toList(),
      );

  final int total;
  final List<RecentItem> recent;
}

class PurchaseRequestSummary {
  PurchaseRequestSummary({required this.total, required this.awaitingApproval, required this.recent});

  factory PurchaseRequestSummary.fromJson(Map<String, dynamic> json) => PurchaseRequestSummary(
        total: json['total'] as int,
        awaitingApproval: json['awaitingApproval'] as int,
        recent: (json['recent'] as List).map((e) => RecentItem.fromJson(e as Map<String, dynamic>)).toList(),
      );

  final int total;
  final int awaitingApproval;
  final List<RecentItem> recent;
}

class OrcamentoSummary {
  OrcamentoSummary({required this.total, required this.draft, required this.recent});

  factory OrcamentoSummary.fromJson(Map<String, dynamic> json) => OrcamentoSummary(
        total: json['total'] as int,
        draft: json['draft'] as int,
        recent: (json['recent'] as List).map((e) => RecentItem.fromJson(e as Map<String, dynamic>)).toList(),
      );

  final int total;
  final int draft;
  final List<RecentItem> recent;
}

class DailyReportSummary {
  DailyReportSummary({required this.total, required this.lastReportDate});

  factory DailyReportSummary.fromJson(Map<String, dynamic> json) => DailyReportSummary(
        total: json['total'] as int,
        lastReportDate: json['lastReportDate'] as String?,
      );

  final int total;
  final String? lastReportDate;
}

class EquipmentSummary {
  EquipmentSummary({required this.total, required this.unavailable});

  factory EquipmentSummary.fromJson(Map<String, dynamic> json) =>
      EquipmentSummary(total: json['total'] as int, unavailable: json['unavailable'] as int);

  final int total;
  final int unavailable;
}
