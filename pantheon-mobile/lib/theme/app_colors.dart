import 'package:flutter/material.dart';

/// Color tokens ported 1:1 from `pantheon-web/src/style.css`'s `@theme` block, so the
/// mobile app stays visually in lockstep with the web app without sharing a build
/// toolchain. Keep these hex values synced with that file if the web palette changes.
class AppColors {
  AppColors._();

  // Blueprint blue — primary/brand.
  static const blueprint50 = Color(0xFFF1F4FF);
  static const blueprint100 = Color(0xFFE4E9FF);
  static const blueprint200 = Color(0xFFC3D0FF);
  static const blueprint300 = Color(0xFF9AAEFF);
  static const blueprint400 = Color(0xFF6F86FF);
  static const blueprint500 = Color(0xFF3D63FF);
  static const blueprint600 = Color(0xFF2F53F0);
  static const blueprint700 = Color(0xFF1E3FD6);
  static const blueprint800 = Color(0xFF1B35AC);
  static const blueprint900 = Color(0xFF182C6E);

  // Steel / graphite — neutrals.
  static const steel50 = Color(0xFFF6F7FB);
  static const steel100 = Color(0xFFEEF0F6);
  static const steel200 = Color(0xFFE1E4EE);
  static const steel300 = Color(0xFFC7CBDB);
  static const steel400 = Color(0xFF9298B3);
  static const steel500 = Color(0xFF6B7290);
  static const steel600 = Color(0xFF4E546E);
  static const steel700 = Color(0xFF333A55);
  static const steel800 = Color(0xFF1F2440);
  static const steel900 = Color(0xFF12172E);

  // Ink — the app's persistent nav surfaces (always dark, doesn't flip with theme).
  static const ink950 = Color(0xFF0B0F1F);
  static const ink900 = Color(0xFF12172E);
  static const ink800 = Color(0xFF1B2242);
  static const ink700 = Color(0xFF242C52);

  // Safety / coral — danger, rejection, destructive actions.
  static const safety50 = Color(0xFFFDF0F0);
  static const safety100 = Color(0xFFFBDEDE);
  static const safety500 = Color(0xFFE14F4F);
  static const safety600 = Color(0xFFC93D3D);

  // Status semantics reuse Material's amber/emerald-equivalent greens, matching
  // pantheon-web's StatusBadge.vue convention (emerald=success, amber=pending).
  static const emerald50 = Color(0xFFECFDF5);
  static const emerald600 = Color(0xFF059669);
  static const emerald700 = Color(0xFF047857);
  static const amber50 = Color(0xFFFFFBEB);
  static const amber600 = Color(0xFFD97706);
  static const amber700 = Color(0xFFB45309);
}
