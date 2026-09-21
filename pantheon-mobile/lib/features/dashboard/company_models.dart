class CompanyMembership {
  CompanyMembership({required this.companyId, required this.companyName, required this.role, required this.onboardingStatus});

  factory CompanyMembership.fromJson(Map<String, dynamic> json) => CompanyMembership(
        companyId: json['companyId'] as String,
        companyName: json['companyName'] as String?,
        role: json['role'] as String,
        onboardingStatus: json['onboardingStatus'] as String,
      );

  final String companyId;
  final String? companyName;
  final String role;
  final String onboardingStatus;
}

class OnboardingStatus {
  OnboardingStatus({required this.companies});

  factory OnboardingStatus.fromJson(Map<String, dynamic> json) => OnboardingStatus(
        companies: (json['companies'] as List).map((e) => CompanyMembership.fromJson(e as Map<String, dynamic>)).toList(),
      );

  final List<CompanyMembership> companies;

  /// The first fully onboarded company — mirrors `useCompanyOnboarding.ts`'s `activeCompany`.
  CompanyMembership? get active {
    for (final c in companies) {
      if (c.onboardingStatus == 'COMPLETE') return c;
    }
    return null;
  }
}
