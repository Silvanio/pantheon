import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../core/auth/auth_provider.dart';
import '../../theme/app_colors.dart';
import '../notifications/push_notification_service.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _submitting = false;
  bool _obscurePassword = true;
  String? _error;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    FocusScope.of(context).unfocus();
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      await ref.read(authControllerProvider.notifier).login(_emailController.text.trim(), _passwordController.text);
      unawaited(ref.read(pushNotificationServiceProvider).refreshRegistration());
      if (mounted) context.go('/');
    } catch (_) {
      setState(() => _error = 'E-mail ou senha inválidos.');
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Scaffold(
      backgroundColor: isDark ? AppColors.steel900 : Colors.white,
      body: SafeArea(
        bottom: false,
        child: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Mirrors pantheon-web's AuthShell left-panel treatment (dark radial-gradient +
              // a soft blurred accent circle), adapted to a top banner for a single-column
              // phone layout instead of a side-by-side split.
              ClipRRect(
                borderRadius: const BorderRadius.only(bottomLeft: Radius.circular(32), bottomRight: Radius.circular(32)),
                child: Container(
                  height: 220,
                  decoration: const BoxDecoration(
                    gradient: RadialGradient(
                      center: Alignment(-0.6, -1),
                      radius: 1.6,
                      colors: [Color(0xFF22306B), AppColors.ink950],
                    ),
                  ),
                  child: Stack(
                    clipBehavior: Clip.none,
                    children: [
                      Positioned(
                        right: -50,
                        top: -50,
                        child: Container(
                          height: 190,
                          width: 190,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            gradient: RadialGradient(
                              colors: [AppColors.blueprint500.withValues(alpha: 0.35), Colors.transparent],
                            ),
                          ),
                        ),
                      ),
                      Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Container(
                              height: 56,
                              width: 56,
                              alignment: Alignment.center,
                              decoration: BoxDecoration(
                                color: AppColors.blueprint500,
                                borderRadius: BorderRadius.circular(16),
                                boxShadow: [BoxShadow(color: AppColors.blueprint900.withValues(alpha: 0.4), blurRadius: 16, offset: const Offset(0, 8))],
                              ),
                              child: const Icon(Icons.apartment, color: Colors.white, size: 30),
                            ),
                            const SizedBox(height: 14),
                            const Text(
                              'Pantheon',
                              style: TextStyle(color: Colors.white, fontSize: 20, fontWeight: FontWeight.w800, letterSpacing: 0.2),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'Todas as suas obras, sem perder o fio da meada.',
                              textAlign: TextAlign.center,
                              style: TextStyle(color: Colors.white.withValues(alpha: 0.75), fontSize: 12.5),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(28, 32, 28, 28),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const Text('Bem-vindo de volta', style: TextStyle(fontSize: 23, fontWeight: FontWeight.w800)),
                      const SizedBox(height: 6),
                      const Text(
                        'Entre para acompanhar suas obras.',
                        style: TextStyle(color: AppColors.steel500, fontSize: 14),
                      ),
                      const SizedBox(height: 28),
                      TextFormField(
                        controller: _emailController,
                        keyboardType: TextInputType.emailAddress,
                        textInputAction: TextInputAction.next,
                        decoration: const InputDecoration(
                          labelText: 'E-mail',
                          prefixIcon: Icon(Icons.mail_outline, size: 20),
                        ),
                        validator: (v) => (v == null || v.isEmpty) ? 'Informe seu e-mail' : null,
                      ),
                      const SizedBox(height: 14),
                      TextFormField(
                        controller: _passwordController,
                        obscureText: _obscurePassword,
                        textInputAction: TextInputAction.done,
                        onFieldSubmitted: (_) => _submit(),
                        decoration: InputDecoration(
                          labelText: 'Senha',
                          prefixIcon: const Icon(Icons.lock_outline, size: 20),
                          suffixIcon: IconButton(
                            icon: Icon(_obscurePassword ? Icons.visibility_outlined : Icons.visibility_off_outlined, size: 20),
                            color: AppColors.steel400,
                            onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                          ),
                        ),
                        validator: (v) => (v == null || v.isEmpty) ? 'Informe sua senha' : null,
                      ),
                      if (_error != null) ...[
                        const SizedBox(height: 14),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                          decoration: BoxDecoration(color: AppColors.safety50, borderRadius: BorderRadius.circular(10)),
                          child: Row(
                            children: [
                              const Icon(Icons.error_outline, color: AppColors.safety600, size: 16),
                              const SizedBox(width: 8),
                              Expanded(child: Text(_error!, style: const TextStyle(color: AppColors.safety600, fontSize: 13))),
                            ],
                          ),
                        ),
                      ],
                      const SizedBox(height: 24),
                      ElevatedButton(
                        onPressed: _submitting ? null : _submit,
                        style: ElevatedButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 16)),
                        child: _submitting
                            ? const SizedBox(
                                height: 18,
                                width: 18,
                                child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                              )
                            : const Text('Entrar', style: TextStyle(fontSize: 15)),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
