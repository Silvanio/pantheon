import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';
import 'site_member_models.dart';

/// Capability -> access level map, mirroring `pantheon-web`'s `useSitePermissions.ts`
/// `getMyPermissions`. Gates which obra-home entry cards and in-screen actions this app shows.
class SiteRepository {
  SiteRepository(this._client);
  final ApiClient _client;

  Future<List<SiteMember>> listMembers(String siteId) async {
    final json = await _client.get<List<dynamic>>('/api/sites/$siteId/members');
    return json.map((e) => SiteMember.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<Map<String, String>> getMyPermissions(String siteId) async {
    final json = await _client.get<Map<String, dynamic>>('/api/sites/$siteId/permissions/mine');
    return json.map((key, value) => MapEntry(key, value as String));
  }

  /// The caller's own function on this site — `null` for company staff with no `SiteMembership`
  /// there (mirrors `useSiteMembers.ts`'s `getMyFunction`).
  Future<String?> getMyFunction(String siteId) async {
    final json = await _client.get<Map<String, dynamic>>('/api/sites/$siteId/members/mine');
    return json['function'] as String?;
  }

  /// Read-only in this app — editing overrides stays web-only for this pass (design.md).
  /// Permissions configuration must not work offline (design.md's offline-support scoping) —
  /// never cached.
  Future<List<Map<String, dynamic>>> listPermissionOverrides(String siteId) async {
    final json = await _client.get<List<dynamic>>('/api/sites/$siteId/permissions', offlineCapable: false);
    return json.cast<Map<String, dynamic>>();
  }
}

final siteRepositoryProvider = Provider((ref) => SiteRepository(ref.watch(apiClientProvider)));
