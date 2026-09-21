class SiteMember {
  SiteMember({
    required this.membershipId,
    this.userId,
    this.email,
    this.displayName,
    required this.function,
    required this.status,
  });

  factory SiteMember.fromJson(Map<String, dynamic> json) => SiteMember(
        membershipId: json['membershipId'] as String,
        userId: json['userId'] as String?,
        email: json['email'] as String?,
        displayName: json['displayName'] as String?,
        function: json['function'] as String,
        status: json['status'] as String,
      );

  final String membershipId;
  final String? userId;
  final String? email;
  final String? displayName;
  final String function;
  final String status;

  String get label => displayName ?? email ?? '—';
}
