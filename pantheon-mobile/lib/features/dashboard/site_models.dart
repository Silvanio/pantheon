class ConstructionSite {
  ConstructionSite({
    required this.id,
    required this.companyId,
    required this.name,
    required this.address,
    required this.status,
    required this.startDate,
    this.companyName,
    this.photoObjectKey,
    this.schedulePercentComplete,
  });

  factory ConstructionSite.fromJson(Map<String, dynamic> json) => ConstructionSite(
        id: json['id'] as String,
        companyId: json['companyId'] as String,
        name: json['name'] as String,
        address: json['address'] as String,
        status: json['status'] as String,
        startDate: json['startDate'] as String,
        companyName: json['companyName'] as String?,
        photoObjectKey: json['photoObjectKey'] as String?,
        schedulePercentComplete: json['schedulePercentComplete'] as int?,
      );

  final String id;
  final String companyId;
  final String name;
  final String address;
  final String status;
  final String startDate;
  final String? companyName;
  final String? photoObjectKey;
  // `null` until the obra has real Cronograma schedule tasks (see the construction-schedule
  // capability) — the UI falls back to a status-derived placeholder percentage until then,
  // mirroring pantheon-web's DashboardView.vue `PLACEHOLDER_PROGRESS`.
  final int? schedulePercentComplete;

  static const Map<String, int> _placeholderProgress = {
    'PLANNING': 10,
    'IN_PROGRESS': 55,
    'PAUSED': 40,
    'COMPLETED': 100,
  };

  int get progressPercent => schedulePercentComplete ?? _placeholderProgress[status] ?? 0;
}
