import 'package:flutter/material.dart';
import 'app_colors.dart';

/// Builds the app's light/dark `ThemeData` from [AppColors], mirroring the web app's
/// theme toggle (`steel-*` flips with the theme; `ink-*` never does — see AppSidebar.vue).
class AppTheme {
  AppTheme._();

  static const _fontFamily = 'Manrope';

  static ThemeData light() {
    final colorScheme = ColorScheme.light(
      primary: AppColors.blueprint600,
      onPrimary: Colors.white,
      secondary: AppColors.blueprint500,
      surface: Colors.white,
      onSurface: AppColors.steel800,
      error: AppColors.safety500,
      onError: Colors.white,
    );
    return _base(colorScheme, scaffoldBackground: AppColors.steel50, cardColor: Colors.white);
  }

  static ThemeData dark() {
    final colorScheme = ColorScheme.dark(
      primary: AppColors.blueprint500,
      onPrimary: Colors.white,
      secondary: AppColors.blueprint400,
      surface: AppColors.steel800,
      onSurface: AppColors.steel100,
      error: AppColors.safety500,
      onError: Colors.white,
    );
    return _base(colorScheme, scaffoldBackground: AppColors.steel900, cardColor: AppColors.steel800);
  }

  static ThemeData _base(ColorScheme colorScheme, {required Color scaffoldBackground, required Color cardColor}) {
    final base = ThemeData(colorScheme: colorScheme, useMaterial3: true, fontFamily: _fontFamily);
    return base.copyWith(
      scaffoldBackgroundColor: scaffoldBackground,
      textTheme: base.textTheme.apply(fontFamily: _fontFamily),
      appBarTheme: AppBarTheme(
        backgroundColor: scaffoldBackground,
        foregroundColor: colorScheme.onSurface,
        elevation: 0,
        surfaceTintColor: Colors.transparent,
        titleTextStyle: TextStyle(
          fontFamily: _fontFamily,
          fontWeight: FontWeight.w800,
          fontSize: 18,
          color: colorScheme.onSurface,
        ),
      ),
      cardTheme: CardThemeData(
        color: cardColor,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
          side: BorderSide(color: colorScheme.brightness == Brightness.dark ? AppColors.steel700 : AppColors.steel200),
        ),
        margin: EdgeInsets.zero,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.blueprint600,
          foregroundColor: Colors.white,
          disabledBackgroundColor: AppColors.steel300,
          textStyle: const TextStyle(fontFamily: _fontFamily, fontWeight: FontWeight.w700, fontSize: 14),
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: colorScheme.onSurface,
          side: BorderSide(color: colorScheme.brightness == Brightness.dark ? AppColors.steel600 : AppColors.steel300),
          textStyle: const TextStyle(fontFamily: _fontFamily, fontWeight: FontWeight.w700, fontSize: 14),
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        ),
      ),
      textButtonTheme: TextButtonThemeData(
        style: TextButton.styleFrom(
          foregroundColor: AppColors.blueprint600,
          textStyle: const TextStyle(fontFamily: _fontFamily, fontWeight: FontWeight.w700, fontSize: 14),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: colorScheme.brightness == Brightness.dark ? AppColors.steel900 : Colors.white,
        contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: colorScheme.brightness == Brightness.dark ? AppColors.steel700 : AppColors.steel200),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: BorderSide(color: colorScheme.brightness == Brightness.dark ? AppColors.steel700 : AppColors.steel200),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: AppColors.blueprint500, width: 1.5),
        ),
        labelStyle: TextStyle(color: colorScheme.brightness == Brightness.dark ? AppColors.steel400 : AppColors.steel500),
      ),
      bottomNavigationBarTheme: BottomNavigationBarThemeData(
        backgroundColor: colorScheme.brightness == Brightness.dark ? AppColors.ink950 : Colors.white,
        selectedItemColor: AppColors.blueprint500,
        unselectedItemColor: AppColors.steel400,
        type: BottomNavigationBarType.fixed,
        showUnselectedLabels: true,
      ),
      dividerTheme: DividerThemeData(
        color: colorScheme.brightness == Brightness.dark ? AppColors.steel700 : AppColors.steel200,
        space: 1,
      ),
    );
  }
}
