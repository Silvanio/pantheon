import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/buttons.dart';
import '../../core/widgets/offline_dialogs.dart';
import '../../core/widgets/status_badge.dart';
import '../../theme/app_colors.dart';
import '../site/site_repository.dart';
import 'daily_report_list_screen.dart' show dailyReportListProvider;
import 'daily_report_models.dart';
import 'daily_report_repository.dart';

final _detailProvider = FutureProvider.family((ref, String id) => ref.watch(dailyReportRepositoryProvider).getDetail(id));
final _mediaProvider = FutureProvider.family((ref, String id) => ref.watch(dailyReportRepositoryProvider).listMedia(id));

/// Mirrors `purchase_request_detail_screen.dart`'s `_Authority`/`canActOn`: same function-match /
/// company-staff-with-no-site-role mechanism, reused verbatim for `DAILY_REPORT` per
/// `daily-report-approval-workflow`'s design.md Decision 4 (the *acting* side is identical to
/// Pedido de Compra — only the passive view-only visibility rule differs, and that's enforced
/// entirely server-side, see design.md Decision 3, so nothing extra is needed here for it).
class _Authority {
  const _Authority(this.myFunction, this.accessLevel);
  final String? myFunction;
  final String? accessLevel;

  bool get canApprove => accessLevel == 'MANAGE' || accessLevel == 'VIEW_AND_APPROVE';

  bool get canManage => accessLevel == 'MANAGE';

  bool canActOn(String approverFunction) {
    if (myFunction != null) return myFunction == approverFunction && canApprove;
    return canManage;
  }
}

final _authorityProvider = FutureProvider.family((ref, String siteId) async {
  final repo = ref.watch(siteRepositoryProvider);
  final results = await Future.wait([repo.getMyFunction(siteId), repo.getMyPermissions(siteId)]);
  final myFunction = results[0] as String?;
  final perms = results[1] as Map<String, String>;
  return _Authority(myFunction, perms['DAILY_REPORT']);
});

const _approverFunctionLabels = {
  'ADMIN': 'Administrador',
  'CLIENT': 'Cliente',
  'ARCHITECT': 'Arquiteto',
  'ENGINEER': 'Engenheiro',
  'SITE_FOREMAN': 'Mestre de obra',
  'SERVICE_PROVIDER': 'Prestador de serviço',
};

const _weatherOptions = ['SUNNY', 'PARTLY_CLOUDY', 'CLOUDY', 'RAINY', 'STORM'];
const _weatherLabels = {
  'SUNNY': 'Ensolarado',
  'PARTLY_CLOUDY': 'Parcialmente nublado',
  'CLOUDY': 'Nublado',
  'RAINY': 'Chuvoso',
  'STORM': 'Tempestade',
};
const _weatherIcons = {
  'SUNNY': Icons.wb_sunny_outlined,
  'PARTLY_CLOUDY': Icons.wb_cloudy_outlined,
  'CLOUDY': Icons.cloud_outlined,
  'RAINY': Icons.grain_outlined,
  'STORM': Icons.thunderstorm_outlined,
};

class DailyReportDetailScreen extends ConsumerStatefulWidget {
  const DailyReportDetailScreen({super.key, required this.id});
  final String id;

  @override
  ConsumerState<DailyReportDetailScreen> createState() => _DailyReportDetailScreenState();
}

class _DailyReportDetailScreenState extends ConsumerState<DailyReportDetailScreen> {
  bool _uploading = false;
  bool _submitting = false;
  bool _deleting = false;
  bool _acting = false;

  bool _editingCore = false;
  bool _coreInitialized = false;
  String _weatherCondition = '';
  bool _weatherBlockedTasks = false;
  String _workHoursStart = '';
  String _workHoursEnd = '';
  final _commentsController = TextEditingController();
  bool _coreSaving = false;
  String? _coreError;

  void _initCoreFieldsIfNeeded(DailyReport report) {
    if (_coreInitialized) return;
    _coreInitialized = true;
    _weatherCondition = report.weatherCondition ?? '';
    _weatherBlockedTasks = report.weatherBlockedTasks ?? false;
    _workHoursStart = report.workHoursStart ?? '';
    _workHoursEnd = report.workHoursEnd ?? '';
    _commentsController.text = report.comments ?? '';
    _editingCore = !dailyReportCoreFieldsComplete(
      weatherCondition: _weatherCondition,
      workHoursStart: _workHoursStart,
      workHoursEnd: _workHoursEnd,
    );
  }

  Future<void> _pickTime(bool isStart) async {
    final initial = TimeOfDay.now();
    final picked = await showTimePicker(context: context, initialTime: initial);
    if (picked == null) return;
    final formatted = '${picked.hour.toString().padLeft(2, '0')}:${picked.minute.toString().padLeft(2, '0')}';
    setState(() {
      if (isStart) {
        _workHoursStart = formatted;
      } else {
        _workHoursEnd = formatted;
      }
    });
  }

  Future<void> _saveCore() async {
    setState(() => _coreError = null);
    // Mirrors pantheon-web's onSaveCore guard: weather + both work-hours fields are mandatory
    // together — a partial set can't be submitted (see dailyReportCoreFieldsComplete).
    if (!dailyReportCoreFieldsComplete(weatherCondition: _weatherCondition, workHoursStart: _workHoursStart, workHoursEnd: _workHoursEnd)) {
      setState(() => _coreError = 'Preencha o clima e o horário de início e fim do expediente.');
      return;
    }
    setState(() => _coreSaving = true);
    try {
      await ref.read(dailyReportRepositoryProvider).updateCore(
            widget.id,
            weatherCondition: _weatherCondition,
            weatherBlockedTasks: _weatherBlockedTasks,
            workHoursStart: _workHoursStart,
            workHoursEnd: _workHoursEnd,
            comments: _commentsController.text.trim().isEmpty ? null : _commentsController.text.trim(),
          );
      ref.invalidate(_detailProvider(widget.id));
      setState(() => _editingCore = false);
    } catch (_) {
      setState(() => _coreError = 'Não foi possível salvar. Tente novamente.');
    } finally {
      if (mounted) setState(() => _coreSaving = false);
    }
  }

  Future<void> _addPhoto(ImageSource source) async {
    final picker = ImagePicker();
    final photo = await picker.pickImage(source: source, imageQuality: 85);
    if (photo == null) return;
    if (!mounted || !await confirmProceedOffline(context, ref)) return;
    setState(() => _uploading = true);
    try {
      final sentLive = await ref.read(dailyReportRepositoryProvider).uploadPhoto(widget.id, photo.path);
      ref.invalidate(_mediaProvider(widget.id));
      if (!sentLive && mounted) {
        await showOfflineSavedDialog(context);
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Não foi possível enviar a foto.')));
      }
    } finally {
      if (mounted) setState(() => _uploading = false);
    }
  }

  void _showPhotoSourceSheet() {
    showModalBottomSheet(
      context: context,
      builder: (context) => SafeArea(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.photo_camera_outlined),
              title: const Text('Tirar foto'),
              onTap: () {
                Navigator.pop(context);
                _addPhoto(ImageSource.camera);
              },
            ),
            ListTile(
              leading: const Icon(Icons.photo_library_outlined),
              title: const Text('Escolher da galeria'),
              onTap: () {
                Navigator.pop(context);
                _addPhoto(ImageSource.gallery);
              },
            ),
          ],
        ),
      ),
    );
  }

  /// The approval workflow (submit-for-approval/approve/reject) requires being online — mirrors
  /// `purchase_request_detail_screen.dart`'s `_act`/`requireOnline` pattern exactly, since
  /// submitting now creates approval steps rather than just flipping a status flag, unlike the
  /// old plain "submit" this replaces (which was safe to queue offline).
  Future<void> _submitReport() async {
    if (!await requireOnline(context, ref)) return;
    setState(() => _submitting = true);
    try {
      await ref.read(dailyReportRepositoryProvider).submit(widget.id);
      ref.invalidate(_detailProvider(widget.id));
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('Não foi possível enviar o relatório para aprovação.')));
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  /// Mirrors `purchase_request_detail_screen.dart`'s `_act`: approve/reject also require being
  /// online (see `_submitReport`'s doc comment above).
  Future<void> _act(Future<void> Function() action) async {
    if (!await requireOnline(context, ref)) return;
    setState(() => _acting = true);
    try {
      await action();
      ref.invalidate(_detailProvider(widget.id));
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Não foi possível concluir a ação.')));
      }
    } finally {
      if (mounted) setState(() => _acting = false);
    }
  }

  Future<void> _approve() async {
    await _act(() => ref.read(dailyReportRepositoryProvider).approveStep(widget.id));
  }

  Future<void> _showRejectDialog() async {
    final controller = TextEditingController();
    final reason = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Rejeitar etapa'),
        content: TextField(
          controller: controller,
          decoration: const InputDecoration(labelText: 'Motivo da rejeição'),
          autofocus: true,
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancelar')),
          TextButton(onPressed: () => Navigator.pop(context, controller.text), child: const Text('Rejeitar')),
        ],
      ),
    );
    if (reason != null && reason.trim().isNotEmpty) {
      final repo = ref.read(dailyReportRepositoryProvider);
      await _act(() => repo.rejectStep(widget.id, reason.trim()));
    }
  }

  Future<void> _deleteReport() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Excluir relatório'),
        content: const Text('Tem certeza que deseja excluir este relatório? Esta ação não pode ser desfeita.'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text('Cancelar')),
          TextButton(onPressed: () => Navigator.pop(context, true), child: const Text('Excluir')),
        ],
      ),
    );
    if (confirmed != true) return;
    if (!mounted || !await requireOnline(context, ref)) return;
    final siteId = ref.read(_detailProvider(widget.id)).valueOrNull?.report.constructionSiteId;
    setState(() => _deleting = true);
    try {
      await ref.read(dailyReportRepositoryProvider).delete(widget.id);
      if (siteId != null) ref.invalidate(dailyReportListProvider(siteId));
      if (mounted) Navigator.pop(context);
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Não foi possível excluir o relatório.')));
      }
    } finally {
      if (mounted) setState(() => _deleting = false);
    }
  }

  @override
  void dispose() {
    _commentsController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final detail = ref.watch(_detailProvider(widget.id));
    return Scaffold(
      backgroundColor: AppColors.steel50,
      appBar: AppBar(title: const Text('Relatório')),
      body: AsyncValueView(
        value: detail,
        data: (DailyReportDetail d) {
          _initCoreFieldsIfNeeded(d.report);
          final isDraft = d.report.status == 'DRAFT';
          final media = ref.watch(_mediaProvider(widget.id));
          return ListView(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 32),
            children: [
              // Header card
              Container(
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(18),
                  boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.04), blurRadius: 10, offset: const Offset(0, 3))],
                ),
                child: Row(
                  children: [
                    Container(
                      height: 46,
                      width: 46,
                      decoration: BoxDecoration(color: AppColors.blueprint50, borderRadius: BorderRadius.circular(13)),
                      alignment: Alignment.center,
                      child: const Icon(Icons.article_outlined, color: AppColors.blueprint600, size: 22),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Relatório #${d.report.sequenceNo}', style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w800)),
                          const SizedBox(height: 2),
                          Text(d.report.reportDate, style: const TextStyle(fontSize: 12.5, color: AppColors.steel500)),
                        ],
                      ),
                    ),
                    StatusBadge(kind: StatusBadgeKind.dailyReport, status: d.report.status),
                  ],
                ),
              ),

              if (d.approvals.isNotEmpty) ...[
                const SizedBox(height: 16),
                Consumer(
                  builder: (context, ref, _) {
                    final authority = ref.watch(_authorityProvider(d.report.constructionSiteId));

                    // Mirrors `purchase_request_detail_screen.dart`'s `currentCycle`/
                    // `currentCyclePending`: all of a cycle's steps are created PENDING up
                    // front on submit, so more than one can be PENDING at once — the actionable
                    // one is the lowest step order within the current (highest) cycle.
                    final currentCycle =
                        d.approvals.map((a) => a.cycleNumber).reduce((a, b) => a > b ? a : b);
                    final currentCyclePending = d.approvals
                        .where((a) => a.cycleNumber == currentCycle && a.status == 'PENDING')
                        .toList()
                      ..sort((a, b) => a.stepOrder.compareTo(b.stepOrder));
                    final currentPending = currentCyclePending.isEmpty ? null : currentCyclePending.first;

                    return _SectionCard(
                      icon: Icons.rule_folder_outlined,
                      title: 'Etapas de aprovação',
                      child: Column(
                        children: [
                          ...d.approvals.where((a) => a.cycleNumber == currentCycle).map(
                                (a) => Padding(
                                  padding: const EdgeInsets.only(bottom: 8),
                                  child: Row(
                                    children: [
                                      Expanded(
                                        child: Text(
                                          _approverFunctionLabels[a.approverFunction] ?? a.approverFunction,
                                          style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13.5),
                                        ),
                                      ),
                                      StatusBadge(kind: StatusBadgeKind.approval, status: a.status),
                                    ],
                                  ),
                                ),
                              ),
                          if (currentPending != null)
                            authority.when(
                              data: (auth) {
                                if (!auth.canActOn(currentPending.approverFunction)) return const SizedBox.shrink();
                                return Padding(
                                  padding: const EdgeInsets.only(top: 4),
                                  child: Row(
                                    children: [
                                      Expanded(
                                        child: SuccessButton(label: 'Aprovar', loading: _acting, onPressed: _approve),
                                      ),
                                      const SizedBox(width: 10),
                                      Expanded(
                                        child: DangerButton(label: 'Rejeitar', loading: _acting, onPressed: _showRejectDialog),
                                      ),
                                    ],
                                  ),
                                );
                              },
                              loading: () => const SizedBox.shrink(),
                              error: (_, _) => const SizedBox.shrink(),
                            ),
                        ],
                      ),
                    );
                  },
                ),
              ],

              const SizedBox(height: 16),
              // Core: weather, hours, comments
              _SectionCard(
                icon: Icons.wb_sunny_outlined,
                title: 'Clima e horário',
                trailing: (!_editingCore && isDraft)
                    ? TextButton(onPressed: () => setState(() => _editingCore = true), child: const Text('Editar'))
                    : null,
                child: _editingCore && isDraft
                    ? _buildCoreForm()
                    : _buildCoreReadOnly(d.report),
              ),

              const SizedBox(height: 16),
              _SectionCard(
                icon: Icons.groups_outlined,
                title: 'Mão de obra',
                child: d.workforceEntries.isEmpty
                    ? const Text('Nenhuma entrada registrada.', style: TextStyle(color: AppColors.steel500))
                    : Column(
                        children: d.workforceEntries
                            .map(
                              (w) => Padding(
                                padding: const EdgeInsets.only(bottom: 8),
                                child: Row(
                                  children: [
                                    Expanded(child: Text(w.roleDescription, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13.5))),
                                    Container(
                                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                      decoration: BoxDecoration(color: AppColors.steel100, borderRadius: BorderRadius.circular(999)),
                                      child: Text('${w.headcount}', style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 12)),
                                    ),
                                  ],
                                ),
                              ),
                            )
                            .toList(),
                      ),
              ),

              const SizedBox(height: 16),
              _SectionCard(
                icon: Icons.checklist_outlined,
                title: 'Atividades',
                child: d.activities.isEmpty
                    ? const Text('Nenhuma atividade registrada.', style: TextStyle(color: AppColors.steel500))
                    : Column(
                        children: d.activities
                            .map(
                              (a) => Padding(
                                padding: const EdgeInsets.only(bottom: 10),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(a.description, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13.5)),
                                    const SizedBox(height: 2),
                                    Text(a.progressNote, style: const TextStyle(fontSize: 12.5, color: AppColors.steel500)),
                                  ],
                                ),
                              ),
                            )
                            .toList(),
                      ),
              ),

              const SizedBox(height: 16),
              _SectionCard(
                icon: Icons.photo_library_outlined,
                title: 'Fotos',
                trailing: isDraft
                    ? TextButton.icon(
                        onPressed: _uploading ? null : _showPhotoSourceSheet,
                        icon: _uploading
                            ? const SizedBox(height: 14, width: 14, child: CircularProgressIndicator(strokeWidth: 2))
                            : const Icon(Icons.add_a_photo_outlined, size: 18),
                        label: const Text('Adicionar'),
                      )
                    : null,
                child: AsyncValueView(
                  value: media,
                  data: (items) {
                    if (items.isEmpty) return const Text('Nenhuma foto adicionada ainda.', style: TextStyle(color: AppColors.steel500));
                    return Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: items
                          .map(
                            (m) => Container(
                              height: 90,
                              width: 90,
                              decoration: BoxDecoration(color: AppColors.steel100, borderRadius: BorderRadius.circular(10)),
                              alignment: Alignment.center,
                              child: Icon(
                                m.type == 'PHOTO' ? Icons.image_outlined : Icons.videocam_outlined,
                                color: AppColors.steel400,
                              ),
                            ),
                          )
                          .toList(),
                    );
                  },
                ),
              ),

              if (isDraft) ...[
                const SizedBox(height: 24),
                SuccessButton(
                  label: 'Enviar para aprovação',
                  loading: _submitting,
                  onPressed: _submitReport,
                  icon: Icons.send_outlined,
                ),
                const SizedBox(height: 10),
                DangerButton(label: 'Excluir relatório', loading: _deleting, onPressed: _deleteReport, icon: Icons.delete_outline),
              ],
            ],
          );
        },
      ),
    );
  }

  Widget _buildCoreReadOnly(DailyReport report) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(_weatherIcons[report.weatherCondition] ?? Icons.help_outline, size: 16, color: AppColors.steel500),
            const SizedBox(width: 6),
            Text(
              report.weatherCondition != null ? (_weatherLabels[report.weatherCondition] ?? report.weatherCondition!) : '—',
              style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13.5),
            ),
          ],
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            const Icon(Icons.schedule, size: 16, color: AppColors.steel500),
            const SizedBox(width: 6),
            Text('${report.workHoursStart ?? '—'} – ${report.workHoursEnd ?? '—'}', style: const TextStyle(fontSize: 13.5)),
          ],
        ),
        if (report.comments != null) ...[
          const SizedBox(height: 8),
          Text(report.comments!, style: const TextStyle(fontSize: 13, color: AppColors.steel600)),
        ],
      ],
    );
  }

  Widget _buildCoreForm() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('Clima *', style: TextStyle(fontSize: 12.5, fontWeight: FontWeight.w600, color: AppColors.steel600)),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: _weatherOptions.map((option) {
            final selected = _weatherCondition == option;
            return ChoiceChip(
              label: Text(_weatherLabels[option]!),
              avatar: Icon(_weatherIcons[option], size: 16, color: selected ? Colors.white : AppColors.steel500),
              selected: selected,
              onSelected: (_) => setState(() => _weatherCondition = selected ? '' : option),
              selectedColor: AppColors.blueprint600,
              labelStyle: TextStyle(color: selected ? Colors.white : AppColors.steel700, fontWeight: FontWeight.w600, fontSize: 12.5),
              backgroundColor: AppColors.steel50,
              side: BorderSide(color: selected ? AppColors.blueprint600 : AppColors.steel200),
            );
          }).toList(),
        ),
        const SizedBox(height: 12),
        CheckboxListTile(
          value: _weatherBlockedTasks,
          onChanged: (v) => setState(() => _weatherBlockedTasks = v ?? false),
          contentPadding: EdgeInsets.zero,
          controlAffinity: ListTileControlAffinity.leading,
          dense: true,
          title: const Text('O clima impediu a execução de tarefas planejadas', style: TextStyle(fontSize: 12.5)),
        ),
        const SizedBox(height: 4),
        Row(
          children: [
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () => _pickTime(true),
                icon: const Icon(Icons.schedule, size: 17),
                label: Text(_workHoursStart.isEmpty ? 'Início *' : _workHoursStart),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () => _pickTime(false),
                icon: const Icon(Icons.schedule, size: 17),
                label: Text(_workHoursEnd.isEmpty ? 'Fim *' : _workHoursEnd),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        TextField(
          controller: _commentsController,
          maxLines: 3,
          decoration: const InputDecoration(labelText: 'Comentários', border: OutlineInputBorder()),
        ),
        if (_coreError != null) ...[
          const SizedBox(height: 8),
          Text(_coreError!, style: const TextStyle(color: AppColors.safety600, fontSize: 12.5)),
        ],
        const SizedBox(height: 12),
        SizedBox(
          width: double.infinity,
          child: ElevatedButton(
            onPressed: _coreSaving ? null : _saveCore,
            child: _coreSaving
                ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                : const Text('Salvar'),
          ),
        ),
      ],
    );
  }
}

class _SectionCard extends StatelessWidget {
  const _SectionCard({required this.icon, required this.title, required this.child, this.trailing});
  final IconData icon;
  final String title;
  final Widget child;
  final Widget? trailing;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [BoxShadow(color: Colors.black.withValues(alpha: 0.03), blurRadius: 8, offset: const Offset(0, 2))],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 17, color: AppColors.blueprint600),
              const SizedBox(width: 8),
              Expanded(
                child: Text(title, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 13, color: AppColors.steel700)),
              ),
              ?trailing,
            ],
          ),
          const SizedBox(height: 12),
          child,
        ],
      ),
    );
  }
}
