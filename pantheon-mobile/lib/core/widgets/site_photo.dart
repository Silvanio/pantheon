import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../theme/app_colors.dart';
import '../api/api_config.dart';
import '../auth/auth_provider.dart';

/// Bearer header for authenticated image requests — mirrors `pantheon-web`'s `SitePhoto.vue`,
/// which fetches the photo as an authenticated blob rather than an unauthenticated `<img src>`
/// since the endpoint requires a token. `CachedNetworkImage` supports this directly via
/// `httpHeaders`, so there's no need to manually fetch+decode bytes like the web version does.
final _authHeaderProvider = FutureProvider<Map<String, String>>((ref) async {
  final token = await ref.watch(tokenStorageProvider).read();
  return token != null ? {'Authorization': 'Bearer $token'} : const {};
});

/// A construction site's cover photo (`GET /api/construction-sites/{siteId}/photo/content`),
/// with a branded gradient + building icon placeholder when the site has none, matches loading,
/// or the request fails — used by the dashboard's site cards and the obra home's hero header.
class SitePhoto extends ConsumerWidget {
  const SitePhoto({super.key, required this.siteId, required this.hasPhoto});

  final String siteId;
  final bool hasPhoto;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    if (!hasPhoto) return const _PhotoPlaceholder();
    final headers = ref.watch(_authHeaderProvider);
    return headers.when(
      data: (h) => CachedNetworkImage(
        imageUrl: '${ApiConfig.baseUrl}/api/construction-sites/$siteId/photo/content',
        httpHeaders: h,
        fit: BoxFit.cover,
        width: double.infinity,
        height: double.infinity,
        fadeInDuration: const Duration(milliseconds: 200),
        placeholder: (_, _) => const _PhotoPlaceholder(),
        errorWidget: (_, _, _) => const _PhotoPlaceholder(),
      ),
      loading: () => const _PhotoPlaceholder(),
      error: (_, _) => const _PhotoPlaceholder(),
    );
  }
}

class _PhotoPlaceholder extends StatelessWidget {
  const _PhotoPlaceholder();

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [AppColors.blueprint600, AppColors.ink900],
        ),
      ),
      alignment: Alignment.center,
      child: const Icon(Icons.apartment, color: Colors.white24, size: 40),
    );
  }
}
