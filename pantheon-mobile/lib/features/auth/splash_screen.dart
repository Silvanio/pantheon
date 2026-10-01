import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../core/widgets/pantheon_mark.dart';
import '../../theme/app_colors.dart';

/// Shown only while `AuthStatus` is `unknown` (the secure-storage session restore hasn't
/// resolved yet) — avoids ever mounting `DashboardScreen` (and firing its network calls)
/// before we know whether the user is actually authenticated. Purely presentational: the
/// router's redirect (see app_router.dart) navigates away as soon as auth resolves, so on a
/// fast local restore this entrance animation may get cut short — that's fine, by design (we
/// don't artificially slow down real launches just to show it off). The native launch screen
/// shown before this (see flutter_native_splash config in pubspec.yaml) is necessarily static —
/// this is the first point content can actually move or add copy.
class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> with TickerProviderStateMixin {
  late final AnimationController _entrance;
  late final AnimationController _glowLoop;
  late final AnimationController _dotsLoop;

  late final Animation<double> _markScale;
  late final Animation<double> _markOpacity;
  late final Animation<double> _wordOpacity;
  late final Animation<Offset> _wordOffset;
  late final Animation<double> _taglineOpacity;
  late final Animation<Offset> _taglineOffset;

  @override
  void initState() {
    super.initState();
    _entrance = AnimationController(vsync: this, duration: const Duration(milliseconds: 1350))..forward();
    _glowLoop = AnimationController(vsync: this, duration: const Duration(milliseconds: 2200))..repeat();
    _dotsLoop = AnimationController(vsync: this, duration: const Duration(milliseconds: 1400))..repeat();

    _markScale = TweenSequence<double>([
      TweenSequenceItem(tween: Tween(begin: 0.55, end: 1.08).chain(CurveTween(curve: Curves.easeOut)), weight: 60),
      TweenSequenceItem(tween: Tween(begin: 1.08, end: 1.0).chain(CurveTween(curve: Curves.easeOut)), weight: 40),
    ]).animate(CurvedAnimation(parent: _entrance, curve: const Interval(0.0, 0.5)));
    _markOpacity = CurvedAnimation(parent: _entrance, curve: const Interval(0.0, 0.3));

    _wordOpacity = CurvedAnimation(parent: _entrance, curve: const Interval(0.42, 0.68, curve: Curves.easeOut));
    _wordOffset = Tween(begin: const Offset(0, 0.25), end: Offset.zero).animate(_wordOpacity);

    _taglineOpacity = CurvedAnimation(parent: _entrance, curve: const Interval(0.58, 0.85, curve: Curves.easeOut));
    _taglineOffset = Tween(begin: const Offset(0, 0.25), end: Offset.zero).animate(_taglineOpacity);
  }

  @override
  void dispose() {
    _entrance.dispose();
    _glowLoop.dispose();
    _dotsLoop.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Stack(
        fit: StackFit.expand,
        children: [
          // A solid base + soft glows that fade to fully transparent (never to another opaque
          // color) — a full-bleed RadialGradient between two near-black opaque colors band-ed
          // visibly on a tall screen (Flutter's gradient radius is circular/single-axis, unlike
          // CSS's independently-scaled-per-axis radial-gradient, so it never reached the bottom
          // of the screen smoothly). Fading to transparent over a flat base has no seam to show.
          const ColoredBox(color: AppColors.ink950),
          Positioned(
            left: -160,
            top: -220,
            child: Container(
              width: 560,
              height: 560,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [const Color(0xFF22306B).withValues(alpha: 0.9), const Color(0xFF22306B).withValues(alpha: 0)],
                ),
              ),
            ),
          ),
          Positioned(
            right: -90,
            top: -90,
            child: Container(
              width: 280,
              height: 280,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  colors: [AppColors.blueprint500.withValues(alpha: 0.35), AppColors.blueprint500.withValues(alpha: 0)],
                ),
              ),
            ),
          ),
          Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                AnimatedBuilder(
                  animation: Listenable.merge([_entrance, _glowLoop]),
                  builder: (context, child) {
                    final glow = 0.35 + 0.2 * math.sin(_glowLoop.value * 2 * math.pi).abs();
                    return Opacity(
                      opacity: _markOpacity.value,
                      child: Transform.scale(
                        scale: _markScale.value,
                        child: Container(
                          width: 96,
                          height: 96,
                          decoration: BoxDecoration(
                            color: AppColors.blueprint600,
                            borderRadius: BorderRadius.circular(22),
                            boxShadow: [BoxShadow(color: AppColors.blueprint600.withValues(alpha: glow), blurRadius: 28, spreadRadius: 2)],
                          ),
                          alignment: Alignment.center,
                          child: child,
                        ),
                      ),
                    );
                  },
                  child: const PantheonMark(size: 54),
                ),
                const SizedBox(height: 16),
                FadeTransition(
                  opacity: _wordOpacity,
                  child: SlideTransition(
                    position: _wordOffset,
                    child: const Text(
                      'Pantheon',
                      style: TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.w800, letterSpacing: 0.2),
                    ),
                  ),
                ),
                const SizedBox(height: 10),
                FadeTransition(
                  opacity: _taglineOpacity,
                  child: SlideTransition(
                    position: _taglineOffset,
                    child: const Padding(
                      padding: EdgeInsets.symmetric(horizontal: 40),
                      child: Text(
                        'Gestão de obras, do orçamento à entrega.',
                        textAlign: TextAlign.center,
                        style: TextStyle(color: AppColors.steel300, fontSize: 13.5, fontWeight: FontWeight.w600, height: 1.4),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
          Positioned(
            bottom: 64,
            left: 0,
            right: 0,
            child: Center(
              child: AnimatedBuilder(
                animation: _dotsLoop,
                builder: (context, _) => Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [0.0, 0.13, 0.26].map(_dot).toList(),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _dot(double phaseOffset) {
    final phase = (_dotsLoop.value + phaseOffset) % 1.0;
    final opacity = 0.25 + 0.75 * math.sin(phase * math.pi).clamp(0.0, 1.0);
    return Container(
      width: 6,
      height: 6,
      margin: const EdgeInsets.symmetric(horizontal: 3),
      decoration: BoxDecoration(shape: BoxShape.circle, color: AppColors.blueprint400.withValues(alpha: opacity)),
    );
  }
}
