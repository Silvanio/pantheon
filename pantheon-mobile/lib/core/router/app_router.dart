import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../features/auth/login_screen.dart';
import '../../features/auth/register_screen.dart';
import '../../features/auth/splash_screen.dart';
import '../../features/dashboard/dashboard_screen.dart';
import '../../features/daily_reports/daily_report_detail_screen.dart';
import '../../features/daily_reports/daily_report_list_screen.dart';
import '../../features/equipment/equipment_list_screen.dart';
import '../../features/orcamentos/orcamento_detail_screen.dart';
import '../../features/orcamentos/orcamento_list_screen.dart';
import '../../features/profile/profile_screen.dart';
import '../../features/projects/project_browser_screen.dart';
import '../../features/purchase_requests/purchase_request_detail_screen.dart';
import '../../features/purchase_requests/purchase_request_list_screen.dart';
import '../../features/site/permissions_screen.dart';
import '../../features/site/site_home_screen.dart';
import '../../features/site/team_screen.dart';
import '../../features/tasks/task_board_screen.dart';
import '../auth/auth_provider.dart';

final appRouterProvider = Provider<GoRouter>((ref) {
  return GoRouter(
    initialLocation: '/',
    refreshListenable: _AuthRefreshNotifier(ref),
    redirect: (context, state) {
      final auth = ref.read(authControllerProvider);
      final loggingIn = state.matchedLocation == '/login' || state.matchedLocation == '/register';
      // Never mount an authenticated screen (and its network calls) before the secure-storage
      // session restore resolves — show the splash route instead of guessing.
      if (auth.status == AuthStatus.unknown) return state.matchedLocation == '/splash' ? null : '/splash';
      if (auth.status == AuthStatus.unauthenticated) return loggingIn ? null : '/login';
      if (auth.status == AuthStatus.authenticated && (loggingIn || state.matchedLocation == '/splash')) return '/';
      return null;
    },
    routes: [
      GoRoute(path: '/splash', builder: (context, state) => const SplashScreen()),
      GoRoute(path: '/login', builder: (context, state) => const LoginScreen()),
      GoRoute(path: '/register', builder: (context, state) => const RegisterScreen()),
      GoRoute(path: '/', builder: (context, state) => const DashboardScreen()),
      GoRoute(path: '/profile', builder: (context, state) => const ProfileScreen()),
      GoRoute(
        path: '/sites/:id',
        builder: (context, state) => SiteHomeScreen(siteId: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/sites/:id/team',
        builder: (context, state) => TeamScreen(siteId: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/sites/:id/purchase-requests',
        builder: (context, state) => PurchaseRequestListScreen(siteId: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/purchase-requests/:id',
        builder: (context, state) => PurchaseRequestDetailScreen(id: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/sites/:id/orcamentos',
        builder: (context, state) => OrcamentoListScreen(siteId: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/orcamentos/:id',
        builder: (context, state) => OrcamentoDetailScreen(id: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/sites/:id/daily-reports',
        builder: (context, state) => DailyReportListScreen(siteId: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/daily-reports/:id',
        builder: (context, state) => DailyReportDetailScreen(id: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/sites/:id/tasks',
        builder: (context, state) => TaskBoardScreen(siteId: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/sites/:id/equipment',
        builder: (context, state) => EquipmentListScreen(siteId: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/sites/:id/projects',
        builder: (context, state) => ProjectBrowserScreen(siteId: state.pathParameters['id']!),
      ),
      GoRoute(
        path: '/sites/:id/permissions',
        builder: (context, state) => PermissionsScreen(siteId: state.pathParameters['id']!),
      ),
    ],
  );
});

/// Bridges Riverpod's `authControllerProvider` state changes into go_router's `redirect`,
/// which otherwise only re-evaluates on navigation, not on external state changes (e.g. the
/// 401 -> forceLogout path from `ApiClient`).
class _AuthRefreshNotifier extends ChangeNotifier {
  _AuthRefreshNotifier(Ref ref) {
    ref.listen(authControllerProvider, (_, _) => notifyListeners());
  }
}
