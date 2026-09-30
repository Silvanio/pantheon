import 'package:flutter/material.dart';

/// Registered on [GoRouter]'s `observers` in `app_router.dart` — required for [RefreshOnReturn] to
/// receive [RouteAware] callbacks.
final routeObserver = RouteObserver<ModalRoute<dynamic>>();

/// Calls [onReturnVisible] whenever a route pushed on top of this screen (e.g. `context.push` to a
/// detail/edit screen) is popped, making this screen visible again.
///
/// `.autoDispose` on a screen's data provider does NOT cover this case by itself: `context.push`
/// keeps the pushed-FROM screen mounted (just obscured) for as long as the pushed-TO screen is on
/// top, so the underlying screen's `ref.watch` never stops, the provider's watcher count never
/// reaches zero, and it's never torn down/refetched — only a full pop (this screen itself removed,
/// then a fresh instance pushed later) triggers autoDispose. That leaves exactly the flow this app
/// is built around stale: open an item from a list, edit/submit/approve it, come back — the list
/// still shows what it showed before, since it was never actually gone. This mixin closes that gap
/// by refreshing on the `didPopNext` signal (this screen becoming visible again) instead of relying
/// on remount.
mixin RefreshOnReturn<T extends StatefulWidget> on State<T> implements RouteAware {
  void onReturnVisible();

  ModalRoute<dynamic>? _subscribedRoute;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final route = ModalRoute.of(context);
    if (route != _subscribedRoute) {
      if (_subscribedRoute != null) routeObserver.unsubscribe(this);
      _subscribedRoute = route;
      if (route != null) routeObserver.subscribe(this, route);
    }
  }

  @override
  void dispose() {
    routeObserver.unsubscribe(this);
    super.dispose();
  }

  @override
  void didPopNext() => onReturnVisible();

  @override
  void didPop() {}

  @override
  void didPush() {}

  @override
  void didPushNext() {}
}
