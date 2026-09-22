import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/buttons.dart';
import '../../core/widgets/offline_dialogs.dart';
import '../../theme/app_colors.dart';
import 'daily_report_list_screen.dart' show dailyReportListProvider;
import 'daily_report_models.dart';
import 'daily_report_repository.dart';

final _detailProvider = FutureProvider.family((ref, String id) => ref.watch(dailyReportRepositoryProvider).getDetail(id));
final _mediaProvider = FutureProvider.family((ref, String id) => ref.watch(dailyReportRepositoryProvider).listMedia(id));

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

  Future<void> _submitReport() async {
    if (!await confirmProceedOffline(context, ref)) return;
    setState(() => _submitting = true);
    try {
      final sentLive = await ref.read(dailyReportRepositoryProvider).submit(widget.id);
      ref.invalidate(_detailProvider(widget.id));
      if (!sentLive && mounted) {
        await showOfflineSavedDialog(context);
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Não foi possível enviar o relatório.')));
      }
    } finally {
      if (mounted) setState(() => _submitting = false);
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
  Widget build(BuildContext context) {
    final detail = ref.watch(_detailProvider(widget.id));
    return Scaffold(
      appBar: AppBar(title: const Text('Relatório')),
      body: AsyncValueView(
        value: detail,
        data: (DailyReportDetail d) {
          final isDraft = d.report.status == 'DRAFT';
          final media = ref.watch(_mediaProvider(widget.id));
          return ListView(
            padding: const EdgeInsets.all(16),
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text('Relatório #${d.report.sequenceNo} — ${d.report.reportDate}',
                        style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w800)),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('Clima e horário', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 13, color: AppColors.steel500)),
                      const SizedBox(height: 6),
                      Text('Clima: ${d.report.weatherCondition ?? '—'}'),
                      Text('Expediente: ${d.report.workHoursStart ?? '—'} – ${d.report.workHoursEnd ?? '—'}'),
                      if (d.report.comments != null) ...[const SizedBox(height: 6), Text(d.report.comments!)],
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              const Text('Mão de obra', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 14)),
              const SizedBox(height: 8),
              if (d.workforceEntries.isEmpty)
                const Padding(padding: EdgeInsets.only(bottom: 8), child: Text('Nenhuma entrada registrada.', style: TextStyle(color: AppColors.steel500)))
              else
                ...d.workforceEntries.map(
                  (w) => Card(
                    margin: const EdgeInsets.only(bottom: 8),
                    child: ListTile(dense: true, title: Text(w.roleDescription), trailing: Text('${w.headcount}')),
                  ),
                ),
              const SizedBox(height: 8),
              const Text('Atividades', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 14)),
              const SizedBox(height: 8),
              if (d.activities.isEmpty)
                const Padding(padding: EdgeInsets.only(bottom: 8), child: Text('Nenhuma atividade registrada.', style: TextStyle(color: AppColors.steel500)))
              else
                ...d.activities.map(
                  (a) => Card(
                    margin: const EdgeInsets.only(bottom: 8),
                    child: ListTile(dense: true, title: Text(a.description), subtitle: Text(a.progressNote)),
                  ),
                ),
              const SizedBox(height: 8),
              Row(
                children: [
                  const Expanded(child: Text('Fotos', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 14))),
                  if (isDraft)
                    TextButton.icon(
                      onPressed: _uploading ? null : _showPhotoSourceSheet,
                      icon: _uploading
                          ? const SizedBox(height: 14, width: 14, child: CircularProgressIndicator(strokeWidth: 2))
                          : const Icon(Icons.add_a_photo_outlined, size: 18),
                      label: const Text('Adicionar foto'),
                    ),
                ],
              ),
              const SizedBox(height: 8),
              AsyncValueView(
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
              if (isDraft) ...[
                const SizedBox(height: 24),
                SuccessButton(label: 'Enviar relatório', loading: _submitting, onPressed: _submitReport, icon: Icons.send_outlined),
                const SizedBox(height: 10),
                DangerButton(label: 'Excluir relatório', loading: _deleting, onPressed: _deleteReport, icon: Icons.delete_outline),
              ],
            ],
          );
        },
      ),
    );
  }
}
