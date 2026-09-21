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
      );

  final String id;
  final String companyId;
  final String name;
  final String address;
  final String status;
  final String startDate;
  final String? companyName;
  final String? photoObjectKey;
}
