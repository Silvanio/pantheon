import 'dart:async';
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';

import '../../core/auth/auth_provider.dart';
import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/buttons.dart';
import '../../core/widgets/offline_dialogs.dart';
import '../../core/widgets/photo_staging_sheet.dart';
import '../../core/widgets/status_badge.dart';
import '../../theme/app_colors.dart';
import '../equipment/equipment_repository.dart';
import '../materials/material_models.dart';
import '../materials/material_repository.dart';
import '../site/site_member_models.dart';
import '../site/site_repository.dart';
import 'daily_report_list_screen.dart' show dailyReportListProvider;
import 'daily_report_models.dart';
import 'daily_report_repository.dart';

final _detailProvider = FutureProvider.autoDispose.family((ref, String id) {
  ref.watch(sessionEpochProvider);
  return ref.watch(dailyReportRepositoryProvider).getDetail(id);
});
final _mediaProvider = FutureProvider.autoDispose.family((ref, String id) {
  ref.watch(sessionEpochProvider);
  return ref.watch(dailyReportRepositoryProvider).listMedia(id);
});
final _attachmentsProvider = FutureProvider.autoDispose.family((ref, String id) {
  ref.watch(sessionEpochProvider);
  return ref.watch(dailyReportRepositoryProvider).listAttachments(id);
});
final _importedInvoicesProvider = FutureProvider.autoDispose.family((ref, String id) {
  ref.watch(sessionEpochProvider);
  return ref.watch(dailyReportRepositoryProvider).listImportedInvoices(id);
});
final _deliveredMaterialsProvider = FutureProvider.autoDispose.family((ref, String id) {
  ref.watch(sessionEpochProvider);
  return ref.watch(dailyReportRepositoryProvider).listDeliveredMaterials(id);
});

/// Full site roster for the "Adicionar mão de obra" member picker — filtered client-side to
/// active/non-invited members (an `INVITED` membership confers no access yet, so it shouldn't be
/// loggable as having worked a shift) since `SiteMemberResponse`'s dedicated `invited` boolean
/// (used for this same filter on `pantheon-web`) isn't mirrored on mobile's slimmer `SiteMember`
/// model — see `redesign-daily-report-experience` design.md's "reuse what the app already has".
final _siteMembersProvider = FutureProvider.autoDispose.family((ref, String siteId) async {
  ref.watch(sessionEpochProvider);
  final members = await ref.watch(siteRepositoryProvider).listMembers(siteId);
  return members.where((m) => m.status != 'INVITED').toList();
});

/// Seeds the equipment picker's default/browse list and resolves already-logged equipment usage
/// entries' names — mirrors `EquipmentRepository.list`'s own "no pagination UI, one generously
/// sized page" precedent (already used by the Equipamentos tab). The *search* box below wires the
/// user's query into this same repository's `name` filter instead of ever depending only on this
/// unfiltered page (see design.md's "reuse the registry's filter, not size=200 unfiltered").
final _equipmentCatalogProvider = FutureProvider.autoDispose.family((ref, String siteId) {
  ref.watch(sessionEpochProvider);
  return ref.watch(equipmentRepositoryProvider).list(siteId);
});

final _canManageMaterialsProvider = FutureProvider.autoDispose.family((ref, String siteId) {
  ref.watch(sessionEpochProvider);
  return ref.watch(siteRepositoryProvider).getMyPermissions(siteId).then((p) => p['ORCAMENTO_MANAGE'] == 'MANAGE');
});

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

final _authorityProvider = FutureProvider.autoDispose.family((ref, String siteId) async {
  ref.watch(sessionEpochProvider);
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

const _activityStatusOptions = ['IN_PROGRESS', 'COMPLETED'];
const _activityStatusLabels = {'IN_PROGRESS': 'Em andamento', 'COMPLETED': 'Concluída'};

const _monthNames = [
  'janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
  'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro',
];

/// `reportDate` is a plain `yyyy-MM-dd` `LocalDate` string — formatted by hand (no `intl` locale
/// data is initialized in this app; every other Portuguese label in this file is a hand-written
/// map for the same reason) into the mockup's exact "15 de março de 2026" long form.
String _formatLongDate(String isoDate) {
  final parts = isoDate.split('-');
  if (parts.length != 3) return isoDate;
  final month = int.tryParse(parts[1]);
  final day = int.tryParse(parts[2]);
  if (month == null || day == null || month < 1 || month > 12) return isoDate;
  return '$day de ${_monthNames[month - 1]} de ${parts[0]}';
}

final _timeFormat = DateFormat('dd/MM HH:mm');

String _initials(String name) {
  final parts = name.trim().split(RegExp(r'\s+')).where((p) => p.isNotEmpty).toList();
  if (parts.isEmpty) return '?';
  if (parts.length == 1) return parts.first.substring(0, 1).toUpperCase();
  return (parts.first.substring(0, 1) + parts.last.substring(0, 1)).toUpperCase();
}

bool _looksLikeVideoPath(String path) {
  final lower = path.toLowerCase();
  return lower.endsWith('.mp4') || lower.endsWith('.mov') || lower.endsWith('.m4v') || lower.endsWith('.avi') || lower.endsWith('.mkv');
}

// --- Local, not-yet-committed working copies of add/remove-only sub-resources ------------------
//
// `redesign-daily-report-experience` design.md Decision 1: adds/removes are staged locally and
// only actually sent to `pantheon-service` when "Salvar alterações" is pressed. `id == null` means
// "not created on the server yet" (a pending add); a server-known id that later disappears from
// the working list is what marks it for deletion — see `staleServerIds`.

class _StagedWorkforce {
  const _StagedWorkforce({this.id, this.membershipId, required this.roleDescription, required this.headcount});
  final String? id;
  final String? membershipId;
  final String roleDescription;
  final int headcount;
}

class _StagedEquipment {
  const _StagedEquipment({this.id, this.equipmentId, this.customName, this.statusNote, this.displayName, this.displayStatus});
  final String? id;
  final String? equipmentId;
  final String? customName;
  final String? statusNote;
  final String? displayName;
  final String? displayStatus;
}

class _StagedActivity {
  const _StagedActivity({this.id, required this.description, required this.progressNote, required this.status});
  final String? id;
  final String description;
  final String progressNote;
  final String status;
}

class DailyReportDetailScreen extends ConsumerStatefulWidget {
  const DailyReportDetailScreen({super.key, required this.id});
  final String id;

  @override
  ConsumerState<DailyReportDetailScreen> createState() => _DailyReportDetailScreenState();
}

class _DailyReportDetailScreenState extends ConsumerState<DailyReportDetailScreen> {
  bool _submitting = false;
  bool _deleting = false;
  bool _acting = false;
  bool _uploadingMedia = false;
  bool _uploadingAttachment = false;
  String? _actingMaterialId;
  bool _saving = false;
  String? _saveError;

  late String _siteId;

  // Core fields — no longer their own editable/save mode (design.md Decision 1 folds this into
  // the single global save too); always editable inline while the report is a draft.
  bool _coreInitialized = false;
  String _weatherConditionMorning = '';
  String _weatherConditionAfternoon = '';
  bool _weatherBlockedTasks = false;
  String _workHoursStart = '';
  String _workHoursEnd = '';
  final _commentsController = TextEditingController();
  String _origWeatherMorning = '';
  String _origWeatherAfternoon = '';
  bool _origWeatherBlockedTasks = false;
  String _origWorkHoursStart = '';
  String _origWorkHoursEnd = '';
  String _origComments = '';

  bool _workforceInitialized = false;
  final List<_StagedWorkforce> _workforce = [];
  Set<String> _workforceOriginalIds = {};

  bool _equipmentInitialized = false;
  final List<_StagedEquipment> _equipment = [];
  Set<String> _equipmentOriginalIds = {};
  final _equipmentSearchController = TextEditingController();
  Timer? _equipmentDebounce;
  List<Equipment>? _equipmentSearchResults;

  bool _activitiesInitialized = false;
  final List<_StagedActivity> _activities = [];
  Set<String> _activitiesOriginalIds = {};

  bool _mediaInitialized = false;
  final List<ReportMedia> _media = [];
  Set<String> _mediaOriginalIds = {};
  final Map<String, String> _mediaCaptionDrafts = {};
  final Map<String, String> _mediaOriginalCaptions = {};
  final Map<String, Uint8List> _mediaBytesCache = {};
  final Map<String, Uint8List> _mediaFullBytesCache = {};

  bool _attachmentsInitialized = false;
  final List<ReportAttachment> _attachments = [];
  Set<String> _attachmentOriginalIds = {};

  void _initCoreFieldsIfNeeded(DailyReport report) {
    _siteId = report.constructionSiteId;
    if (_coreInitialized) return;
    _coreInitialized = true;
    _weatherConditionMorning = report.weatherConditionMorning ?? '';
    _weatherConditionAfternoon = report.weatherConditionAfternoon ?? '';
    _weatherBlockedTasks = report.weatherBlockedTasks ?? false;
    _workHoursStart = report.workHoursStart ?? '';
    _workHoursEnd = report.workHoursEnd ?? '';
    _commentsController.text = report.comments ?? '';
    _origWeatherMorning = _weatherConditionMorning;
    _origWeatherAfternoon = _weatherConditionAfternoon;
    _origWeatherBlockedTasks = _weatherBlockedTasks;
    _origWorkHoursStart = _workHoursStart;
    _origWorkHoursEnd = _workHoursEnd;
    _origComments = _commentsController.text;
  }

  void _initWorkforceIfNeeded(List<WorkforceEntry> entries) {
    if (_workforceInitialized) return;
    _workforceInitialized = true;
    _workforce.clear();
    _workforce.addAll(entries.map((e) => _StagedWorkforce(id: e.id, membershipId: e.membershipId, roleDescription: e.roleDescription, headcount: e.headcount)));
    _workforceOriginalIds = entries.map((e) => e.id).toSet();
  }

  void _initEquipmentIfNeeded(List<EquipmentUsage> usage, List<Equipment> catalog) {
    if (_equipmentInitialized) return;
    _equipmentInitialized = true;
    final byId = {for (final e in catalog) e.id: e};
    _equipment.clear();
    _equipment.addAll(usage.map((u) {
      final match = u.equipmentId == null ? null : byId[u.equipmentId];
      return _StagedEquipment(
        id: u.id,
        equipmentId: u.equipmentId,
        customName: u.customName,
        statusNote: u.statusNote,
        displayName: match?.name,
        displayStatus: match?.status,
      );
    }));
    _equipmentOriginalIds = usage.map((u) => u.id).toSet();
  }

  void _initActivitiesIfNeeded(List<ReportActivity> activities) {
    if (_activitiesInitialized) return;
    _activitiesInitialized = true;
    _activities.clear();
    _activities.addAll(activities.map((a) => _StagedActivity(id: a.id, description: a.description, progressNote: a.progressNote, status: a.status)));
    _activitiesOriginalIds = activities.map((a) => a.id).toSet();
  }

  void _initMediaIfNeeded(List<ReportMedia> media) {
    if (_mediaInitialized) return;
    _mediaInitialized = true;
    _media.clear();
    _media.addAll(media);
    _mediaOriginalIds = media.map((m) => m.id).toSet();
    _mediaCaptionDrafts.clear();
    _mediaOriginalCaptions.clear();
    for (final m in media) {
      _mediaCaptionDrafts[m.id] = m.caption ?? '';
      _mediaOriginalCaptions[m.id] = m.caption ?? '';
    }
  }

  void _initAttachmentsIfNeeded(List<ReportAttachment> attachments) {
    if (_attachmentsInitialized) return;
    _attachmentsInitialized = true;
    _attachments.clear();
    _attachments.addAll(attachments);
    _attachmentOriginalIds = attachments.map((a) => a.id).toSet();
  }

  bool get _coreDirty =>
      _weatherConditionMorning != _origWeatherMorning ||
      _weatherConditionAfternoon != _origWeatherAfternoon ||
      _weatherBlockedTasks != _origWeatherBlockedTasks ||
      _workHoursStart != _origWorkHoursStart ||
      _workHoursEnd != _origWorkHoursEnd ||
      _commentsController.text.trim() != _origComments;

  bool get _workforceDirty =>
      _workforce.any((e) => e.id == null) || staleServerIds(originalIds: _workforceOriginalIds, currentIds: _workforce.map((e) => e.id)).isNotEmpty;

  bool get _equipmentDirty =>
      _equipment.any((e) => e.id == null) || staleServerIds(originalIds: _equipmentOriginalIds, currentIds: _equipment.map((e) => e.id)).isNotEmpty;

  bool get _activitiesDirty =>
      _activities.any((e) => e.id == null) || staleServerIds(originalIds: _activitiesOriginalIds, currentIds: _activities.map((e) => e.id)).isNotEmpty;

  bool get _mediaDirty =>
      staleServerIds(originalIds: _mediaOriginalIds, currentIds: _media.map((m) => m.id)).isNotEmpty ||
      _mediaCaptionDrafts.entries.any((entry) => entry.value.trim() != (_mediaOriginalCaptions[entry.key] ?? ''));

  bool get _attachmentsDirty => staleServerIds(originalIds: _attachmentOriginalIds, currentIds: _attachments.map((a) => a.id)).isNotEmpty;

  bool get _isDirty => _coreDirty || _workforceDirty || _equipmentDirty || _activitiesDirty || _mediaDirty || _attachmentsDirty;

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

  /// Fires every staged add/remove/edit together, mirroring `pantheon-web`'s single "Salvar
  /// alterações" orchestration (design.md Decision 1) — the existing per-section endpoints stay,
  /// this just sequences the calls and reports one combined success/error instead of a save
  /// button per section. Structural edits (unlike photo capture) require being online, same as
  /// submit/approve/reject/delete elsewhere on this screen.
  Future<void> _saveAll() async {
    setState(() => _saveError = null);
    if (!dailyReportCoreFieldsComplete(
      weatherConditionMorning: _weatherConditionMorning,
      weatherConditionAfternoon: _weatherConditionAfternoon,
      workHoursStart: _workHoursStart,
      workHoursEnd: _workHoursEnd,
    )) {
      setState(() => _saveError = 'Preencha o clima (manhã e tarde) e o horário de início e fim do expediente.');
      return;
    }
    if (!_isDirty) return;
    if (!await requireOnline(context, ref)) return;

    setState(() => _saving = true);
    final failed = <String>{};
    final repo = ref.read(dailyReportRepositoryProvider);

    if (_coreDirty) {
      try {
        await repo.updateCore(
          widget.id,
          weatherConditionMorning: _weatherConditionMorning,
          weatherConditionAfternoon: _weatherConditionAfternoon,
          weatherBlockedTasks: _weatherBlockedTasks,
          workHoursStart: _workHoursStart,
          workHoursEnd: _workHoursEnd,
          comments: _commentsController.text.trim().isEmpty ? null : _commentsController.text.trim(),
        );
        _origWeatherMorning = _weatherConditionMorning;
        _origWeatherAfternoon = _weatherConditionAfternoon;
        _origWeatherBlockedTasks = _weatherBlockedTasks;
        _origWorkHoursStart = _workHoursStart;
        _origWorkHoursEnd = _workHoursEnd;
        _origComments = _commentsController.text.trim();
      } catch (_) {
        failed.add('clima e horário');
      }
    }

    for (final id in staleServerIds(originalIds: _workforceOriginalIds, currentIds: _workforce.map((e) => e.id))) {
      try {
        await repo.deleteWorkforceEntry(widget.id, id);
        _workforceOriginalIds.remove(id);
      } catch (_) {
        failed.add('mão de obra');
      }
    }
    for (var i = 0; i < _workforce.length; i++) {
      final entry = _workforce[i];
      if (entry.id != null) continue;
      try {
        final created = await repo.addWorkforceEntry(
          widget.id,
          membershipId: entry.membershipId,
          roleDescription: entry.membershipId == null ? entry.roleDescription : null,
          headcount: entry.headcount,
        );
        _workforce[i] = _StagedWorkforce(id: created.id, membershipId: created.membershipId, roleDescription: created.roleDescription, headcount: created.headcount);
        _workforceOriginalIds.add(created.id);
      } catch (_) {
        failed.add('mão de obra');
      }
    }

    for (final id in staleServerIds(originalIds: _equipmentOriginalIds, currentIds: _equipment.map((e) => e.id))) {
      try {
        await repo.deleteEquipmentUsage(widget.id, id);
        _equipmentOriginalIds.remove(id);
      } catch (_) {
        failed.add('equipamentos');
      }
    }
    for (var i = 0; i < _equipment.length; i++) {
      final entry = _equipment[i];
      if (entry.id != null) continue;
      try {
        final created = await repo.addEquipmentUsage(widget.id, equipmentId: entry.equipmentId, customName: entry.customName, statusNote: entry.statusNote);
        _equipment[i] = _StagedEquipment(
          id: created.id,
          equipmentId: created.equipmentId,
          customName: created.customName,
          statusNote: created.statusNote,
          displayName: entry.displayName,
          displayStatus: entry.displayStatus,
        );
        _equipmentOriginalIds.add(created.id);
      } catch (_) {
        failed.add('equipamentos');
      }
    }

    for (final id in staleServerIds(originalIds: _activitiesOriginalIds, currentIds: _activities.map((e) => e.id))) {
      try {
        await repo.deleteActivity(widget.id, id);
        _activitiesOriginalIds.remove(id);
      } catch (_) {
        failed.add('atividades');
      }
    }
    for (var i = 0; i < _activities.length; i++) {
      final entry = _activities[i];
      if (entry.id != null) continue;
      try {
        final created = await repo.addActivity(widget.id, description: entry.description, progressNote: entry.progressNote, status: entry.status);
        _activities[i] = _StagedActivity(id: created.id, description: created.description, progressNote: created.progressNote, status: created.status);
        _activitiesOriginalIds.add(created.id);
      } catch (_) {
        failed.add('atividades');
      }
    }

    for (final id in staleServerIds(originalIds: _mediaOriginalIds, currentIds: _media.map((m) => m.id))) {
      try {
        await repo.deleteMedia(widget.id, id);
        _mediaOriginalIds.remove(id);
        _mediaOriginalCaptions.remove(id);
        _mediaCaptionDrafts.remove(id);
      } catch (_) {
        failed.add('mídia');
      }
    }
    for (final media in List<ReportMedia>.from(_media)) {
      final draft = _mediaCaptionDrafts[media.id]?.trim() ?? '';
      final original = _mediaOriginalCaptions[media.id] ?? '';
      if (draft == original) continue;
      try {
        final updated = await repo.updateMediaCaption(widget.id, media.id, draft.isEmpty ? null : draft);
        _mediaOriginalCaptions[media.id] = updated.caption ?? '';
        _mediaCaptionDrafts[media.id] = updated.caption ?? '';
        final idx = _media.indexWhere((m) => m.id == media.id);
        if (idx != -1) _media[idx] = updated;
      } catch (_) {
        failed.add('mídia');
      }
    }

    for (final id in staleServerIds(originalIds: _attachmentOriginalIds, currentIds: _attachments.map((a) => a.id))) {
      try {
        await repo.deleteAttachment(widget.id, id);
        _attachmentOriginalIds.remove(id);
      } catch (_) {
        failed.add('anexos');
      }
    }

    if (mounted) setState(() => _saving = false);
    if (failed.isEmpty) {
      ref.invalidate(_detailProvider(widget.id));
      ref.invalidate(_mediaProvider(widget.id));
      ref.invalidate(_attachmentsProvider(widget.id));
      _coreInitialized = false;
      _workforceInitialized = false;
      _equipmentInitialized = false;
      _activitiesInitialized = false;
      _mediaInitialized = false;
      _attachmentsInitialized = false;
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Alterações salvas.')));
    } else {
      setState(() => _saveError = 'Não foi possível salvar: ${failed.join(', ')}.');
    }
  }

  // --- Mão de obra -----------------------------------------------------------------------------

  Future<void> _showAddWorkforceSheet() async {
    List<SiteMember> members = const [];
    try {
      members = await ref.read(_siteMembersProvider(_siteId).future);
    } catch (_) {
      // Falls back to an empty roster — the sheet still allows "outra mão de obra".
    }
    if (!mounted) return;
    final linkedIds = _workforce.map((e) => e.membershipId).whereType<String>().toSet();
    final available = members.where((m) => !linkedIds.contains(m.membershipId)).toList();
    final result = await showModalBottomSheet<_StagedWorkforce>(
      context: context,
      isScrollControlled: true,
      builder: (context) => _AddWorkforceSheet(members: available),
    );
    if (result != null) setState(() => _workforce.add(result));
  }

  // --- Equipamentos ------------------------------------------------------------------------------

  void _onEquipmentSearchChanged(String value) {
    _equipmentDebounce?.cancel();
    _equipmentDebounce = Timer(const Duration(milliseconds: 350), () async {
      final query = value.trim();
      if (query.isEmpty) {
        if (mounted) setState(() => _equipmentSearchResults = null);
        return;
      }
      try {
        final results = await ref.read(equipmentRepositoryProvider).list(_siteId, name: query);
        if (mounted) setState(() => _equipmentSearchResults = results);
      } catch (_) {
        // Keep the previous results on a transient search failure.
      }
    });
  }

  Future<void> _showAddCustomEquipmentSheet() async {
    final result = await showModalBottomSheet<_StagedEquipment>(
      context: context,
      isScrollControlled: true,
      builder: (context) => const _AddCustomEquipmentSheet(),
    );
    if (result != null) setState(() => _equipment.add(result));
  }

  // --- Atividades --------------------------------------------------------------------------------

  Future<void> _showAddActivitySheet() async {
    final result = await showModalBottomSheet<_StagedActivity>(
      context: context,
      isScrollControlled: true,
      builder: (context) => const _AddActivitySheet(),
    );
    if (result != null) setState(() => _activities.add(result));
  }

  // --- Materiais recebidos -------------------------------------------------------------------

  Future<void> _markMaterialChecked(MaterialDelivery material) async {
    if (!mounted || !await requireOnline(context, ref)) return;
    if (!mounted) return;
    final photos = await showModalBottomSheet<List<XFile>>(
      context: context,
      isScrollControlled: true,
      builder: (context) => const PhotoStagingSheet(
        title: 'Conferência de material',
        subtitle: 'Anexe as fotos da conferência antes de confirmar.',
      ),
    );
    if (photos == null || !mounted) return;
    setState(() => _actingMaterialId = material.id);
    try {
      await ref.read(materialRepositoryProvider).markChecked(material.id, photos.map((p) => p.path).toList());
      ref.invalidate(_deliveredMaterialsProvider(widget.id));
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Não foi possível atualizar o status do material.')));
      }
    } finally {
      if (mounted) setState(() => _actingMaterialId = null);
    }
  }

  // --- Mídia & Anexos ------------------------------------------------------------------------

  Future<Uint8List> _loadMediaBytes(String mediaId) async {
    final cached = _mediaBytesCache[mediaId];
    if (cached != null) return cached;
    final bytes = Uint8List.fromList(await ref.read(dailyReportRepositoryProvider).getMediaThumbnail(widget.id, mediaId));
    _mediaBytesCache[mediaId] = bytes;
    return bytes;
  }

  /// Full-resolution original for the enlarged detail sheet — the grid's thumbnail (_loadMediaBytes)
  /// looks blurry stretched up to that size.
  Future<Uint8List> _loadMediaFullBytes(String mediaId) async {
    final cached = _mediaFullBytesCache[mediaId];
    if (cached != null) return cached;
    final bytes = Uint8List.fromList(await ref.read(dailyReportRepositoryProvider).getMediaContent(widget.id, mediaId));
    _mediaFullBytesCache[mediaId] = bytes;
    return bytes;
  }

  Future<void> _showAddMediaSheet() async {
    final items = await showModalBottomSheet<List<_MediaStagingItem>>(
      context: context,
      isScrollControlled: true,
      builder: (context) => const _MediaStagingSheet(),
    );
    if (items == null || items.isEmpty || !mounted) return;
    if (!await confirmProceedOffline(context, ref)) return;
    setState(() => _uploadingMedia = true);
    var anyOffline = false;
    var anyFailed = false;
    for (final item in items) {
      try {
        final sentLive = await ref.read(dailyReportRepositoryProvider).uploadMedia(
              widget.id,
              item.file.path,
              type: item.isVideo ? 'VIDEO' : 'PHOTO',
              caption: item.caption.trim().isEmpty ? null : item.caption.trim(),
            );
        if (!sentLive) anyOffline = true;
      } catch (_) {
        anyFailed = true;
      }
    }
    ref.invalidate(_mediaProvider(widget.id));
    _mediaInitialized = false;
    if (mounted) setState(() => _uploadingMedia = false);
    if (anyFailed && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Não foi possível enviar um ou mais itens de mídia.')));
    } else if (anyOffline && mounted) {
      await showOfflineSavedDialog(context);
    }
  }

  /// Photos open in a swipeable gallery positioned at the tapped item — see `_MediaDetailSheet`.
  /// Videos have no full-res viewer yet, so they still open alone, non-navigable.
  Future<void> _showMediaDetailSheet(ReportMedia media) async {
    final photos = _media.where((m) => m.type == 'PHOTO').toList();
    final items = media.type == 'PHOTO' ? photos : [media];
    final initialIndex = media.type == 'PHOTO' ? photos.indexWhere((m) => m.id == media.id) : 0;
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      builder: (context) => _MediaDetailSheet(
        items: items,
        initialIndex: initialIndex < 0 ? 0 : initialIndex,
        captionDrafts: _mediaCaptionDrafts,
        loadFullBytes: _loadMediaFullBytes,
        onRemove: (item) => setState(() => _media.removeWhere((m) => m.id == item.id)),
      ),
    );
    setState(() {});
  }

  Future<void> _downloadAndNotify(String filename, Future<List<int>> Function() fetch) async {
    if (!await requireOnline(context, ref)) return;
    try {
      final bytes = await fetch();
      final file = File('${Directory.systemTemp.path}/$filename');
      await file.writeAsBytes(bytes, flush: true);
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Anexo baixado: $filename')));
    } catch (_) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Não foi possível baixar o anexo.')));
    }
  }

  Future<void> _addAttachment(ImageSource source) async {
    // No document/file picker exists yet in this app (see doc comment on the Anexos section
    // below) — a photo of a paper document (delivery slip, handwritten note) via camera/gallery
    // is the one attachment-creation path mobile can offer without adding a new dependency.
    final picker = ImagePicker();
    final photo = await picker.pickImage(source: source, imageQuality: 85);
    if (photo == null) return;
    if (!mounted || !await requireOnline(context, ref)) return;
    setState(() => _uploadingAttachment = true);
    try {
      final created = await ref.read(dailyReportRepositoryProvider).uploadAttachment(widget.id, photo.path);
      setState(() {
        _attachments.add(created);
        _attachmentOriginalIds.add(created.id);
      });
    } catch (_) {
      if (mounted) ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Não foi possível enviar o anexo.')));
    } finally {
      if (mounted) setState(() => _uploadingAttachment = false);
    }
  }

  // --- Approval / submit / delete (unchanged behavior from before this redesign) -------------

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
    _equipmentSearchController.dispose();
    _equipmentDebounce?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final detail = ref.watch(_detailProvider(widget.id));
    return Scaffold(
      backgroundColor: AppColors.steel50,
      appBar: AppBar(
        titleSpacing: 0,
        title: detail.maybeWhen(
          data: (d) => Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('Diário de Obra #${d.report.sequenceNo}', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w800)),
              Text(_formatLongDate(d.report.reportDate), style: const TextStyle(fontSize: 11.5, color: AppColors.steel500)),
            ],
          ),
          orElse: () => const Text('Diário de Obra'),
        ),
        actions: [
          detail.maybeWhen(
            data: (d) => Padding(
              padding: const EdgeInsets.only(right: 16),
              child: StatusBadge(kind: StatusBadgeKind.dailyReport, status: d.report.status),
            ),
            orElse: () => const SizedBox.shrink(),
          ),
        ],
      ),
      body: AsyncValueView(
        value: detail,
        data: (DailyReportDetail d) {
          _initCoreFieldsIfNeeded(d.report);
          final isDraft = d.report.status == 'DRAFT';
          final media = ref.watch(_mediaProvider(widget.id));
          final attachments = ref.watch(_attachmentsProvider(widget.id));
          final importedInvoices = ref.watch(_importedInvoicesProvider(widget.id));
          final deliveredMaterials = ref.watch(_deliveredMaterialsProvider(widget.id));
          final canManageMaterials = ref.watch(_canManageMaterialsProvider(_siteId));
          final equipmentCatalog = ref.watch(_equipmentCatalogProvider(_siteId));
          _initWorkforceIfNeeded(d.workforceEntries);
          _initActivitiesIfNeeded(d.activities);
          equipmentCatalog.whenData((catalog) => _initEquipmentIfNeeded(d.equipmentUsage, catalog));
          media.whenData(_initMediaIfNeeded);
          attachments.whenData(_initAttachmentsIfNeeded);

          return ListView(
            padding: EdgeInsets.fromLTRB(16, 16, 16, 32 + MediaQuery.of(context).padding.bottom),
            children: [
              if (d.approvals.isNotEmpty) ...[
                Consumer(
                  builder: (context, ref, _) {
                    final authority = ref.watch(_authorityProvider(d.report.constructionSiteId));
                    final currentCycle = d.approvals.map((a) => a.cycleNumber).reduce((a, b) => a > b ? a : b);
                    final currentCyclePending = d.approvals.where((a) => a.cycleNumber == currentCycle && a.status == 'PENDING').toList()
                      ..sort((a, b) => a.stepOrder.compareTo(b.stepOrder));
                    final currentPending = currentCyclePending.isEmpty ? null : currentCyclePending.first;

                    return _SectionCard(
                      icon: Icons.rule_folder_outlined,
                      title: 'Aprovação',
                      child: Column(
                        children: [
                          ...d.approvals.where((a) => a.cycleNumber == currentCycle).map((a) => _ApprovalStepRow(approval: a)),
                          if (currentPending != null)
                            authority.when(
                              data: (auth) {
                                if (!auth.canActOn(currentPending.approverFunction)) return const SizedBox.shrink();
                                return Padding(
                                  padding: const EdgeInsets.only(top: 4),
                                  child: Row(
                                    children: [
                                      Expanded(child: SuccessButton(label: 'Aprovar', loading: _acting, onPressed: _approve)),
                                      const SizedBox(width: 10),
                                      Expanded(child: DangerButton(label: 'Rejeitar', loading: _acting, onPressed: _showRejectDialog)),
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
                const SizedBox(height: 14),
              ],

              // 1. Clima & Expediente
              _SectionCard(
                icon: Icons.wb_sunny_outlined,
                title: 'Clima & Expediente',
                child: isDraft ? _buildCoreForm() : _buildCoreReadOnly(d.report),
              ),
              const SizedBox(height: 14),

              // 2. Mão de obra
              _SectionCard(
                icon: Icons.groups_outlined,
                title: 'Mão de obra',
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (_workforce.isEmpty)
                      const Padding(
                        padding: EdgeInsets.only(bottom: 8),
                        child: Text('Nenhuma entrada registrada.', style: TextStyle(color: AppColors.steel500)),
                      )
                    else
                      Column(
                        children: _workforce
                            .map((w) => Padding(
                                  padding: const EdgeInsets.only(bottom: 8),
                                  child: _WorkforceRow(entry: w, editable: isDraft, onRemove: () => setState(() => _workforce.remove(w))),
                                ))
                            .toList(),
                      ),
                    if (isDraft)
                      OutlinedButton.icon(
                        onPressed: _showAddWorkforceSheet,
                        icon: const Icon(Icons.add, size: 18),
                        label: const Text('Adicionar mão de obra'),
                        style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(44)),
                      ),
                  ],
                ),
              ),
              const SizedBox(height: 14),

              // 3. Equipamentos
              _SectionCard(
                icon: Icons.construction_outlined,
                title: 'Equipamentos',
                child: AsyncValueView(
                  value: equipmentCatalog,
                  data: (catalog) => _buildEquipmentSection(catalog, isDraft),
                ),
              ),
              const SizedBox(height: 14),

              // 4. Atividades
              _SectionCard(
                icon: Icons.checklist_outlined,
                title: 'Atividades',
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (_activities.isEmpty)
                      const Padding(
                        padding: EdgeInsets.only(bottom: 8),
                        child: Text('Nenhuma atividade registrada.', style: TextStyle(color: AppColors.steel500)),
                      )
                    else
                      Column(
                        children: _activities
                            .map((a) => Padding(
                                  padding: const EdgeInsets.only(bottom: 10),
                                  child: _ActivityRow(entry: a, editable: isDraft, onRemove: () => setState(() => _activities.remove(a))),
                                ))
                            .toList(),
                      ),
                    if (isDraft)
                      OutlinedButton.icon(
                        onPressed: _showAddActivitySheet,
                        icon: const Icon(Icons.add, size: 18),
                        label: const Text('Adicionar atividade'),
                        style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(44)),
                      ),
                  ],
                ),
              ),
              const SizedBox(height: 14),

              // 5. Materiais recebidos — read-only, sourced from the real delivery-tracking
              // `Material` records for this date (see design.md Decision 3), not
              // `DailyReportMaterialReceived`. The approved mockup's "Materiais recebidos" section
              // shows only this list (no manual-add affordance) — unlike `pantheon-web`, mobile
              // never had a manual materials-received UI, and since the mockup itself doesn't call
              // for adding one, this change intentionally leaves that gap unfilled to match the
              // mockup exactly rather than building a section it doesn't depict.
              _SectionCard(
                icon: Icons.local_shipping_outlined,
                title: 'Materiais recebidos',
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Padding(
                      padding: EdgeInsets.only(bottom: 10),
                      child: Text(
                        'Importado dos Pedidos de Compra entregues nesta data.',
                        style: TextStyle(fontSize: 11.5, color: AppColors.steel400),
                      ),
                    ),
                    AsyncValueView(
                      value: deliveredMaterials,
                      errorMessage: 'Não foi possível carregar os materiais recebidos.',
                      data: (items) {
                        if (items.isEmpty) {
                          return const Text('Nenhum material entregue nesta data.', style: TextStyle(color: AppColors.steel500));
                        }
                        final canManage = canManageMaterials.valueOrNull ?? false;
                        return Column(
                          children: items
                              .map((m) => Padding(
                                    padding: const EdgeInsets.only(bottom: 8),
                                    child: _DeliveredMaterialRow(
                                      material: m,
                                      canManage: canManage,
                                      acting: _actingMaterialId == m.id,
                                      onMarkChecked: () => _markMaterialChecked(m),
                                    ),
                                  ))
                              .toList(),
                        );
                      },
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),

              // 6. Mídia & Anexos
              _SectionCard(
                icon: Icons.perm_media_outlined,
                title: 'Mídia & Anexos',
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    SizedBox(
                      height: 84,
                      child: AsyncValueView(
                        value: media,
                        errorMessage: 'Não foi possível carregar as fotos.',
                        data: (_) => ListView(
                          scrollDirection: Axis.horizontal,
                          children: [
                            ..._media.map((m) => Padding(
                                  padding: const EdgeInsets.only(right: 10),
                                  child: _MediaThumbnail(media: m, loadBytes: _loadMediaBytes, onTap: () => _showMediaDetailSheet(m)),
                                )),
                            if (isDraft)
                              GestureDetector(
                                onTap: _uploadingMedia ? null : _showAddMediaSheet,
                                child: Container(
                                  width: 84,
                                  height: 84,
                                  decoration: BoxDecoration(
                                    borderRadius: BorderRadius.circular(12),
                                    border: Border.all(color: AppColors.steel300, width: 1.5),
                                  ),
                                  alignment: Alignment.center,
                                  child: _uploadingMedia
                                      ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2))
                                      : const Icon(Icons.add, color: AppColors.steel400, size: 22),
                                ),
                              ),
                          ],
                        ),
                      ),
                    ),
                    if (isDraft) ...[
                      const SizedBox(height: 14),
                      Row(
                        children: [
                          Expanded(
                            child: OutlinedButton.icon(
                              onPressed: _uploadingMedia ? null : _showAddMediaSheet,
                              icon: const Icon(Icons.photo_camera_outlined, size: 17),
                              label: const Text('Câmera'),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: OutlinedButton.icon(
                              onPressed: _uploadingMedia ? null : _showAddMediaSheet,
                              icon: const Icon(Icons.photo_library_outlined, size: 17),
                              label: const Text('Galeria'),
                            ),
                          ),
                        ],
                      ),
                    ],
                    const SizedBox(height: 14),
                    // Anexos: real DailyReportAttachment rows (removable) merged with same-day
                    // imported Pedido de Compra invoices (read-only, visually tagged) — see
                    // `daily-report-media-and-signoff` spec's "Same-day Pedido de Compra invoice
                    // surfaced". No generic "add any file" entry point exists yet on mobile (no
                    // document/file-picker dependency in this app and the approved mockup shows
                    // no such affordance either); `_addAttachment` reuses camera/gallery for a
                    // photographed document instead, offered below the list when drafting.
                    AsyncValueView(
                      value: attachments,
                      errorMessage: 'Não foi possível carregar os anexos.',
                      data: (_) {
                        final invoices = importedInvoices.valueOrNull ?? const [];
                        if (_attachments.isEmpty && invoices.isEmpty) {
                          return const Text('Nenhum anexo adicionado ainda.', style: TextStyle(color: AppColors.steel500));
                        }
                        return Column(
                          children: [
                            ..._attachments.map((a) => Padding(
                                  padding: const EdgeInsets.only(bottom: 8),
                                  child: _AttachmentRow(
                                    name: a.originalName,
                                    editable: isDraft,
                                    onTap: () => _downloadAndNotify(a.originalName, () => ref.read(dailyReportRepositoryProvider).getAttachmentContent(widget.id, a.id)),
                                    onRemove: () => setState(() => _attachments.remove(a)),
                                  ),
                                )),
                            ...invoices.map((inv) => Padding(
                                  padding: const EdgeInsets.only(bottom: 8),
                                  child: _AttachmentRow(
                                    name: inv.originalName,
                                    tag: 'Do Pedido ${inv.purchaseRequestName ?? '#${inv.purchaseRequestId.substring(0, 8)}'}',
                                    onTap: () => _downloadAndNotify(inv.originalName, () => ref.read(dailyReportRepositoryProvider).getImportedInvoiceContent(inv.id)),
                                  ),
                                )),
                          ],
                        );
                      },
                    ),
                    if (isDraft) ...[
                      const SizedBox(height: 10),
                      TextButton.icon(
                        onPressed: _uploadingAttachment ? null : () => _addAttachment(ImageSource.camera),
                        icon: _uploadingAttachment
                            ? const SizedBox(height: 14, width: 14, child: CircularProgressIndicator(strokeWidth: 2))
                            : const Icon(Icons.attach_file, size: 17),
                        label: const Text('Adicionar anexo'),
                      ),
                    ],
                  ],
                ),
              ),

              if (isDraft) ...[
                const SizedBox(height: 24),
                SuccessButton(label: 'Enviar para aprovação', loading: _submitting, onPressed: _submitReport, icon: Icons.send_outlined),
                const SizedBox(height: 10),
                DangerButton(label: 'Excluir relatório', loading: _deleting, onPressed: _deleteReport, icon: Icons.delete_outline),
              ],
            ],
          );
        },
      ),
      bottomNavigationBar: detail.maybeWhen(
        data: (d) => d.report.status == 'DRAFT' ? _buildSaveBar() : null,
        orElse: () => null,
      ),
    );
  }

  Widget _buildSaveBar() {
    return SafeArea(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        decoration: const BoxDecoration(color: Colors.white, border: Border(top: BorderSide(color: AppColors.steel200))),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (_saveError != null) ...[
              Text(_saveError!, style: const TextStyle(color: AppColors.safety600, fontSize: 12)),
              const SizedBox(height: 8),
            ],
            Row(
              children: [
                Row(
                  children: [
                    Container(
                      width: 6,
                      height: 6,
                      decoration: BoxDecoration(
                        color: _isDirty ? AppColors.amber600 : AppColors.emerald600,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 5),
                    Text(
                      _isDirty ? 'Não salvo' : 'Tudo salvo',
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                        color: _isDirty ? AppColors.amber600 : AppColors.emerald600,
                      ),
                    ),
                  ],
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: SizedBox(
                    height: 46,
                    child: ElevatedButton(
                      onPressed: _saving ? null : _saveAll,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.blueprint600,
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      ),
                      child: _saving
                          ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                          : const Text('Salvar', style: TextStyle(fontWeight: FontWeight.w800)),
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEquipmentSection(List<Equipment> catalog, bool isDraft) {
    final candidatesById = {for (final e in catalog) e.id: e};
    if (_equipmentSearchResults != null) {
      for (final e in _equipmentSearchResults!) {
        candidatesById[e.id] = e;
      }
    }
    for (final staged in _equipment.where((e) => e.equipmentId != null && !candidatesById.containsKey(e.equipmentId))) {
      candidatesById[staged.equipmentId!] = Equipment(id: staged.equipmentId!, name: staged.displayName ?? 'Equipamento', status: staged.displayStatus ?? 'AVAILABLE');
    }
    final candidates = candidatesById.values.toList();
    final addedIds = _equipment.where((e) => e.equipmentId != null).map((e) => e.equipmentId).toSet();
    final customEntries = _equipment.where((e) => e.equipmentId == null).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (isDraft) ...[
          TextField(
            controller: _equipmentSearchController,
            onChanged: _onEquipmentSearchChanged,
            decoration: InputDecoration(
              hintText: 'Buscar equipamento…',
              hintStyle: const TextStyle(fontSize: 12.5, color: AppColors.steel400),
              prefixIcon: const Icon(Icons.search, size: 18, color: AppColors.steel400),
              isDense: true,
              contentPadding: const EdgeInsets.symmetric(vertical: 12, horizontal: 12),
              border: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.steel300)),
              enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(10), borderSide: const BorderSide(color: AppColors.steel300)),
            ),
          ),
          const SizedBox(height: 12),
        ],
        if (candidates.isEmpty)
          const Padding(
            padding: EdgeInsets.only(bottom: 8),
            child: Text('Nenhum equipamento cadastrado encontrado.', style: TextStyle(color: AppColors.steel500)),
          )
        else
          Column(
            children: candidates.map((eq) {
              final checked = addedIds.contains(eq.id);
              return Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: InkWell(
                  onTap: !isDraft
                      ? null
                      : () => setState(() {
                            if (checked) {
                              _equipment.removeWhere((e) => e.equipmentId == eq.id);
                            } else {
                              _equipment.add(_StagedEquipment(equipmentId: eq.id, displayName: eq.name, displayStatus: eq.status));
                            }
                          }),
                  borderRadius: BorderRadius.circular(11),
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
                    decoration: BoxDecoration(border: Border.all(color: AppColors.steel200), borderRadius: BorderRadius.circular(11)),
                    child: Row(
                      children: [
                        Checkbox(
                          value: checked,
                          onChanged: !isDraft
                              ? null
                              : (v) => setState(() {
                                    if (v == true) {
                                      _equipment.add(_StagedEquipment(equipmentId: eq.id, displayName: eq.name, displayStatus: eq.status));
                                    } else {
                                      _equipment.removeWhere((e) => e.equipmentId == eq.id);
                                    }
                                  }),
                          activeColor: AppColors.blueprint600,
                          materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
                        ),
                        const SizedBox(width: 4),
                        Expanded(child: Text(eq.name, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 12.5))),
                        StatusBadge(kind: StatusBadgeKind.equipment, status: eq.status),
                      ],
                    ),
                  ),
                ),
              );
            }).toList(),
          ),
        if (customEntries.isNotEmpty) ...[
          const SizedBox(height: 4),
          ...customEntries.map((e) => Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
                  decoration: BoxDecoration(border: Border.all(color: AppColors.steel300, style: BorderStyle.solid), borderRadius: BorderRadius.circular(11)),
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(e.customName ?? '—', style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 12.5)),
                            if (e.statusNote != null && e.statusNote!.isNotEmpty)
                              Text(e.statusNote!, style: const TextStyle(fontSize: 11, color: AppColors.steel500)),
                          ],
                        ),
                      ),
                      if (isDraft)
                        InkWell(onTap: () => setState(() => _equipment.remove(e)), child: const Icon(Icons.close, size: 18, color: AppColors.steel400)),
                    ],
                  ),
                ),
              )),
        ],
        const SizedBox(height: 8),
        if (isDraft)
          OutlinedButton.icon(
            onPressed: _showAddCustomEquipmentSheet,
            icon: const Icon(Icons.add, size: 18),
            label: const Text('Adicionar equipamento'),
            style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(44)),
          ),
      ],
    );
  }

  Widget _buildCoreReadOnly(DailyReport report) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Icon(_weatherIcons[report.weatherConditionMorning] ?? Icons.help_outline, size: 16, color: AppColors.steel500),
            const SizedBox(width: 6),
            Text(
              'Manhã: ${report.weatherConditionMorning != null ? (_weatherLabels[report.weatherConditionMorning] ?? report.weatherConditionMorning!) : '—'}',
              style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13.5),
            ),
          ],
        ),
        const SizedBox(height: 6),
        Row(
          children: [
            Icon(_weatherIcons[report.weatherConditionAfternoon] ?? Icons.help_outline, size: 16, color: AppColors.steel500),
            const SizedBox(width: 6),
            Text(
              'Tarde: ${report.weatherConditionAfternoon != null ? (_weatherLabels[report.weatherConditionAfternoon] ?? report.weatherConditionAfternoon!) : '—'}',
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
        if (report.comments != null && report.comments!.isNotEmpty) ...[
          const SizedBox(height: 8),
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            decoration: BoxDecoration(color: AppColors.steel50, border: Border.all(color: AppColors.steel100), borderRadius: BorderRadius.circular(10)),
            child: Text(report.comments!, style: const TextStyle(fontSize: 12.5, color: AppColors.steel600)),
          ),
        ],
      ],
    );
  }

  Widget _buildCoreForm() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text('MANHÃ *', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w800, color: AppColors.steel600)),
        const SizedBox(height: 7),
        Wrap(
          spacing: 6,
          runSpacing: 8,
          children: _weatherOptions.map((option) {
            final selected = _weatherConditionMorning == option;
            return ChoiceChip(
              label: Text(_weatherLabels[option]!),
              avatar: Icon(_weatherIcons[option], size: 16, color: selected ? Colors.white : AppColors.steel500),
              selected: selected,
              onSelected: (_) => setState(() => _weatherConditionMorning = selected ? '' : option),
              selectedColor: AppColors.blueprint600,
              labelStyle: TextStyle(color: selected ? Colors.white : AppColors.steel700, fontWeight: FontWeight.w600, fontSize: 12.5),
              backgroundColor: AppColors.steel50,
              side: BorderSide(color: selected ? AppColors.blueprint600 : AppColors.steel300),
            );
          }).toList(),
        ),
        const SizedBox(height: 14),
        const Text('TARDE *', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w800, color: AppColors.steel600)),
        const SizedBox(height: 7),
        Wrap(
          spacing: 6,
          runSpacing: 8,
          children: _weatherOptions.map((option) {
            final selected = _weatherConditionAfternoon == option;
            return ChoiceChip(
              label: Text(_weatherLabels[option]!),
              avatar: Icon(_weatherIcons[option], size: 16, color: selected ? Colors.white : AppColors.steel500),
              selected: selected,
              onSelected: (_) => setState(() => _weatherConditionAfternoon = selected ? '' : option),
              selectedColor: AppColors.blueprint600,
              labelStyle: TextStyle(color: selected ? Colors.white : AppColors.steel700, fontWeight: FontWeight.w600, fontSize: 12.5),
              backgroundColor: AppColors.steel50,
              side: BorderSide(color: selected ? AppColors.blueprint600 : AppColors.steel300),
            );
          }).toList(),
        ),
        const SizedBox(height: 14),
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
          onChanged: (_) => setState(() {}),
          decoration: const InputDecoration(labelText: 'Comentários', border: OutlineInputBorder()),
        ),
      ],
    );
  }
}

class _SectionCard extends StatelessWidget {
  const _SectionCard({required this.icon, required this.title, required this.child});
  final IconData icon;
  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        border: Border.all(color: AppColors.steel200),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 16, color: AppColors.blueprint600),
              const SizedBox(width: 8),
              Expanded(
                child: Text(title, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 14.5, color: AppColors.steel800)),
              ),
            ],
          ),
          const SizedBox(height: 14),
          child,
        ],
      ),
    );
  }
}

class _WorkforceRow extends StatelessWidget {
  const _WorkforceRow({required this.entry, required this.editable, required this.onRemove});
  final _StagedWorkforce entry;
  final bool editable;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    final isLinked = entry.membershipId != null;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
      decoration: BoxDecoration(
        color: isLinked ? AppColors.blueprint50 : null,
        border: Border.all(color: isLinked ? AppColors.blueprint200 : AppColors.steel300, style: isLinked ? BorderStyle.solid : BorderStyle.solid),
        borderRadius: BorderRadius.circular(11),
      ),
      child: Row(
        children: [
          if (isLinked) ...[
            CircleAvatar(
              radius: 14,
              backgroundColor: AppColors.blueprint600,
              child: Text(_initials(entry.roleDescription), style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.w700)),
            ),
            const SizedBox(width: 10),
          ],
          Expanded(child: Text(entry.roleDescription, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 12.5))),
          Text(
            '×${entry.headcount}',
            style: TextStyle(fontWeight: FontWeight.w800, fontSize: 12.5, color: isLinked ? AppColors.blueprint600 : AppColors.steel600),
          ),
          if (editable) ...[
            const SizedBox(width: 8),
            InkWell(onTap: onRemove, child: const Icon(Icons.close, size: 17, color: AppColors.steel400)),
          ],
        ],
      ),
    );
  }
}

class _ActivityRow extends StatelessWidget {
  const _ActivityRow({required this.entry, required this.editable, required this.onRemove});
  final _StagedActivity entry;
  final bool editable;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(13),
      decoration: BoxDecoration(border: Border.all(color: AppColors.steel200), borderRadius: BorderRadius.circular(12)),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(child: Text(entry.description, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 12.5))),
              if (editable) InkWell(onTap: onRemove, child: const Icon(Icons.close, size: 17, color: AppColors.steel400)),
            ],
          ),
          const SizedBox(height: 6),
          if (entry.progressNote.isNotEmpty) Text(entry.progressNote, style: const TextStyle(fontSize: 11.5, color: AppColors.steel500)),
          const SizedBox(height: 8),
          StatusBadge(kind: StatusBadgeKind.dailyReportActivity, status: entry.status),
        ],
      ),
    );
  }
}

class _ApprovalStepRow extends StatelessWidget {
  const _ApprovalStepRow({required this.approval});
  final DailyReportApproval approval;

  @override
  Widget build(BuildContext context) {
    Widget marker;
    switch (approval.status) {
      case 'APPROVED':
        marker = const CircleAvatar(radius: 12, backgroundColor: AppColors.emerald600, child: Icon(Icons.check, size: 13, color: Colors.white));
        break;
      case 'REJECTED':
        marker = const CircleAvatar(radius: 12, backgroundColor: AppColors.safety500, child: Icon(Icons.close, size: 13, color: Colors.white));
        break;
      default:
        marker = CircleAvatar(
          radius: 12,
          backgroundColor: AppColors.amber50,
          child: Text('${approval.stepOrder}', style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w800, color: AppColors.amber700)),
        );
    }
    final functionLabel = _approverFunctionLabels[approval.approverFunction] ?? approval.approverFunction;
    String subtitle;
    if (approval.status == 'PENDING') {
      subtitle = 'pendente';
    } else {
      final who = approval.decidedByName ?? '—';
      final when = approval.decidedAt != null ? _timeFormat.format(DateTime.parse(approval.decidedAt!).toLocal()) : '';
      subtitle = when.isEmpty ? who : '$who · $when';
    }
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          marker,
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  approval.status == 'PENDING' ? '$functionLabel — pendente' : functionLabel,
                  style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 12.5),
                ),
                if (approval.status != 'PENDING') Text(subtitle, style: const TextStyle(fontSize: 11, color: AppColors.steel500)),
                if (approval.comment != null && approval.comment!.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Text(approval.comment!, style: const TextStyle(fontSize: 11, color: AppColors.safety600)),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _DeliveredMaterialRow extends StatelessWidget {
  const _DeliveredMaterialRow({required this.material, required this.canManage, required this.acting, required this.onMarkChecked});
  final MaterialDelivery material;
  final bool canManage;
  final bool acting;
  final VoidCallback onMarkChecked;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(border: Border.all(color: AppColors.steel200), borderRadius: BorderRadius.circular(12)),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(material.name, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 12.5)),
                const SizedBox(height: 2),
                Text(
                  '${material.quantity}${material.type != null ? ' ${material.type}' : ''}${material.sourcePurchaseRequestName != null ? ' · ${material.sourcePurchaseRequestName}' : ''}',
                  style: const TextStyle(fontSize: 10.5, color: AppColors.steel400),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          if (canManage && material.status == 'DELIVERED')
            acting
                ? const SizedBox(height: 18, width: 18, child: CircularProgressIndicator(strokeWidth: 2))
                : TextButton(onPressed: onMarkChecked, child: const Text('Marcar conferido', style: TextStyle(fontSize: 11.5)))
          else
            StatusBadge(kind: StatusBadgeKind.material, status: material.status),
        ],
      ),
    );
  }
}

class _MediaDetailSheet extends StatefulWidget {
  const _MediaDetailSheet({
    required this.items,
    required this.initialIndex,
    required this.captionDrafts,
    required this.loadFullBytes,
    required this.onRemove,
  });

  final List<ReportMedia> items;
  final int initialIndex;
  final Map<String, String> captionDrafts;
  final Future<Uint8List> Function(String) loadFullBytes;
  final void Function(ReportMedia) onRemove;

  @override
  State<_MediaDetailSheet> createState() => _MediaDetailSheetState();
}

class _MediaDetailSheetState extends State<_MediaDetailSheet> {
  late final PageController _pageController;
  late int _index;
  late final TextEditingController _captionController;

  ReportMedia get _current => widget.items[_index];

  @override
  void initState() {
    super.initState();
    _index = widget.initialIndex;
    _pageController = PageController(initialPage: _index);
    _captionController = TextEditingController(text: widget.captionDrafts[_current.id] ?? '');
  }

  @override
  void dispose() {
    _pageController.dispose();
    _captionController.dispose();
    super.dispose();
  }

  void _onPageChanged(int i) {
    setState(() {
      _index = i;
      _captionController.text = widget.captionDrafts[_current.id] ?? '';
    });
  }

  void _goTo(int i) {
    _pageController.animateToPage(i, duration: const Duration(milliseconds: 220), curve: Curves.easeOut);
  }

  @override
  Widget build(BuildContext context) {
    final media = _current;
    return Padding(
      padding: EdgeInsets.only(left: 20, right: 20, top: 20, bottom: 20 + MediaQuery.of(context).viewInsets.bottom),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (media.type == 'PHOTO')
            ClipRRect(
              borderRadius: BorderRadius.circular(12),
              child: SizedBox(
                height: MediaQuery.of(context).size.height * 0.55,
                width: double.infinity,
                child: Container(
                  color: AppColors.ink950,
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      PageView.builder(
                        controller: _pageController,
                        itemCount: widget.items.length,
                        onPageChanged: _onPageChanged,
                        itemBuilder: (context, i) => FutureBuilder<Uint8List>(
                          future: widget.loadFullBytes(widget.items[i].id),
                          builder: (context, snap) => snap.hasData
                              ? Image.memory(snap.data!, fit: BoxFit.contain)
                              : const Center(child: CircularProgressIndicator(color: Colors.white)),
                        ),
                      ),
                      if (_index > 0)
                        Positioned(left: 4, child: _MediaNavArrow(icon: Icons.chevron_left, onTap: () => _goTo(_index - 1))),
                      if (_index < widget.items.length - 1)
                        Positioned(right: 4, child: _MediaNavArrow(icon: Icons.chevron_right, onTap: () => _goTo(_index + 1))),
                      if (widget.items.length > 1)
                        Positioned(
                          top: 8,
                          child: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                            decoration: BoxDecoration(color: Colors.black54, borderRadius: BorderRadius.circular(999)),
                            child: Text(
                              '${_index + 1} / ${widget.items.length}',
                              style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w700),
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
              ),
            )
          else
            Container(
              height: 120,
              width: double.infinity,
              decoration: BoxDecoration(color: AppColors.steel100, borderRadius: BorderRadius.circular(12)),
              alignment: Alignment.center,
              child: const Icon(Icons.videocam_outlined, size: 36, color: AppColors.steel400),
            ),
          const SizedBox(height: 14),
          TextField(
            controller: _captionController,
            decoration: const InputDecoration(labelText: 'Legenda'),
            onChanged: (v) => widget.captionDrafts[media.id] = v,
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(child: OutlinedButton(onPressed: () => Navigator.pop(context), child: const Text('Fechar'))),
              const SizedBox(width: 10),
              Expanded(
                child: DangerButton(
                  label: 'Remover',
                  icon: Icons.delete_outline,
                  onPressed: () {
                    widget.onRemove(media);
                    Navigator.pop(context);
                  },
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _MediaNavArrow extends StatelessWidget {
  const _MediaNavArrow({required this.icon, required this.onTap});
  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.black45,
      shape: const CircleBorder(),
      child: InkWell(
        customBorder: const CircleBorder(),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(6),
          child: Icon(icon, color: Colors.white, size: 26),
        ),
      ),
    );
  }
}

class _MediaThumbnail extends StatelessWidget {
  const _MediaThumbnail({required this.media, required this.loadBytes, required this.onTap});
  final ReportMedia media;
  final Future<Uint8List> Function(String) loadBytes;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: ClipRRect(
        borderRadius: BorderRadius.circular(12),
        child: Container(
          width: 84,
          height: 84,
          color: AppColors.steel100,
          child: media.type == 'PHOTO'
              ? FutureBuilder<Uint8List>(
                  future: loadBytes(media.id),
                  builder: (context, snap) {
                    if (!snap.hasData) return const Center(child: SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2)));
                    return Image.memory(snap.data!, fit: BoxFit.cover, width: 84, height: 84);
                  },
                )
              : const Center(child: Icon(Icons.play_circle_fill, color: AppColors.steel400, size: 30)),
        ),
      ),
    );
  }
}

class _AttachmentRow extends StatelessWidget {
  const _AttachmentRow({required this.name, this.tag, this.editable = false, required this.onTap, this.onRemove});
  final String name;
  final String? tag;
  final bool editable;
  final VoidCallback onTap;
  final VoidCallback? onRemove;

  @override
  Widget build(BuildContext context) {
    final isImported = tag != null;
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        decoration: BoxDecoration(
          color: isImported ? AppColors.blueprint50 : null,
          border: Border.all(color: isImported ? AppColors.blueprint200 : AppColors.steel200),
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          children: [
            const Icon(Icons.description_outlined, size: 18, color: AppColors.blueprint600),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 11.5)),
                  if (tag != null) Text(tag!, style: const TextStyle(fontSize: 10, color: AppColors.blueprint600, fontWeight: FontWeight.w700)),
                ],
              ),
            ),
            if (editable && onRemove != null) InkWell(onTap: onRemove, child: const Icon(Icons.close, size: 17, color: AppColors.steel400)),
          ],
        ),
      ),
    );
  }
}

// --- Add sheets ----------------------------------------------------------------------------

class _AddWorkforceSheet extends StatefulWidget {
  const _AddWorkforceSheet({required this.members});
  final List<SiteMember> members;

  @override
  State<_AddWorkforceSheet> createState() => _AddWorkforceSheetState();
}

class _AddWorkforceSheetState extends State<_AddWorkforceSheet> {
  bool _customMode = false;
  SiteMember? _selectedMember;
  final _customLabelController = TextEditingController();
  final _headcountController = TextEditingController(text: '1');

  @override
  void dispose() {
    _customLabelController.dispose();
    _headcountController.dispose();
    super.dispose();
  }

  void _confirm() {
    if (_customMode) {
      final label = _customLabelController.text.trim();
      final headcount = int.tryParse(_headcountController.text.trim()) ?? 0;
      if (label.isEmpty || headcount < 1) return;
      Navigator.pop(context, _StagedWorkforce(membershipId: null, roleDescription: label, headcount: headcount));
    } else {
      final member = _selectedMember;
      if (member == null) return;
      final role = _approverFunctionLabels[member.function] ?? member.function;
      Navigator.pop(context, _StagedWorkforce(membershipId: member.membershipId, roleDescription: '${member.label} · $role', headcount: 1));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(left: 20, right: 20, top: 20, bottom: 20 + MediaQuery.of(context).viewInsets.bottom),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Adicionar mão de obra', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: ChoiceChip(
                  label: const Text('Equipe'),
                  selected: !_customMode,
                  onSelected: (_) => setState(() => _customMode = false),
                  selectedColor: AppColors.blueprint600,
                  labelStyle: TextStyle(color: !_customMode ? Colors.white : AppColors.steel700, fontWeight: FontWeight.w700),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: ChoiceChip(
                  label: const Text('Outra mão de obra'),
                  selected: _customMode,
                  onSelected: (_) => setState(() => _customMode = true),
                  selectedColor: AppColors.blueprint600,
                  labelStyle: TextStyle(color: _customMode ? Colors.white : AppColors.steel700, fontWeight: FontWeight.w700),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          if (_customMode) ...[
            TextField(controller: _customLabelController, decoration: const InputDecoration(labelText: 'Descrição'), autofocus: true),
            const SizedBox(height: 12),
            TextField(
              controller: _headcountController,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: 'Quantidade de pessoas'),
            ),
          ] else if (widget.members.isEmpty)
            const Text('Nenhum membro disponível para adicionar.', style: TextStyle(color: AppColors.steel500))
          else
            DropdownButtonFormField<SiteMember>(
              initialValue: _selectedMember,
              decoration: const InputDecoration(labelText: 'Membro da equipe'),
              items: widget.members
                  .map((m) => DropdownMenuItem(value: m, child: Text('${m.label} · ${_approverFunctionLabels[m.function] ?? m.function}')))
                  .toList(),
              onChanged: (v) => setState(() => _selectedMember = v),
            ),
          const SizedBox(height: 20),
          SizedBox(width: double.infinity, child: ElevatedButton(onPressed: _confirm, child: const Text('Adicionar'))),
        ],
      ),
    );
  }
}

class _AddCustomEquipmentSheet extends StatefulWidget {
  const _AddCustomEquipmentSheet();

  @override
  State<_AddCustomEquipmentSheet> createState() => _AddCustomEquipmentSheetState();
}

class _AddCustomEquipmentSheetState extends State<_AddCustomEquipmentSheet> {
  final _nameController = TextEditingController();
  final _statusNoteController = TextEditingController();

  @override
  void dispose() {
    _nameController.dispose();
    _statusNoteController.dispose();
    super.dispose();
  }

  void _confirm() {
    final name = _nameController.text.trim();
    if (name.isEmpty) return;
    Navigator.pop(
      context,
      _StagedEquipment(customName: name, statusNote: _statusNoteController.text.trim().isEmpty ? null : _statusNoteController.text.trim()),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(left: 20, right: 20, top: 20, bottom: 20 + MediaQuery.of(context).viewInsets.bottom),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Outro equipamento', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
          const SizedBox(height: 4),
          const Text('Para equipamentos ainda não cadastrados no registro do canteiro.', style: TextStyle(fontSize: 12.5, color: AppColors.steel500)),
          const SizedBox(height: 14),
          TextField(controller: _nameController, decoration: const InputDecoration(labelText: 'Nome do equipamento'), autofocus: true),
          const SizedBox(height: 12),
          TextField(controller: _statusNoteController, decoration: const InputDecoration(labelText: 'Observação (opcional)')),
          const SizedBox(height: 20),
          SizedBox(width: double.infinity, child: ElevatedButton(onPressed: _confirm, child: const Text('Adicionar'))),
        ],
      ),
    );
  }
}

class _AddActivitySheet extends StatefulWidget {
  const _AddActivitySheet();

  @override
  State<_AddActivitySheet> createState() => _AddActivitySheetState();
}

class _AddActivitySheetState extends State<_AddActivitySheet> {
  final _descriptionController = TextEditingController();
  final _progressNoteController = TextEditingController();
  String _status = 'IN_PROGRESS';

  @override
  void dispose() {
    _descriptionController.dispose();
    _progressNoteController.dispose();
    super.dispose();
  }

  void _confirm() {
    final description = _descriptionController.text.trim();
    final progressNote = _progressNoteController.text.trim();
    if (description.isEmpty || progressNote.isEmpty) return;
    Navigator.pop(context, _StagedActivity(description: description, progressNote: progressNote, status: _status));
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(left: 20, right: 20, top: 20, bottom: 20 + MediaQuery.of(context).viewInsets.bottom),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Adicionar atividade', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
          const SizedBox(height: 14),
          TextField(controller: _descriptionController, decoration: const InputDecoration(labelText: 'Descrição'), autofocus: true, maxLines: 2),
          const SizedBox(height: 12),
          TextField(controller: _progressNoteController, decoration: const InputDecoration(labelText: 'Nota de progresso'), maxLines: 2),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            children: _activityStatusOptions.map((option) {
              final selected = _status == option;
              return ChoiceChip(
                label: Text(_activityStatusLabels[option]!),
                selected: selected,
                onSelected: (_) => setState(() => _status = option),
                selectedColor: AppColors.blueprint600,
                labelStyle: TextStyle(color: selected ? Colors.white : AppColors.steel700, fontWeight: FontWeight.w600),
              );
            }).toList(),
          ),
          const SizedBox(height: 20),
          SizedBox(width: double.infinity, child: ElevatedButton(onPressed: _confirm, child: const Text('Adicionar'))),
        ],
      ),
    );
  }
}

class _MediaStagingItem {
  _MediaStagingItem({required this.file, required this.isVideo});
  final XFile file;
  final bool isVideo;
  String caption = '';
}

/// The captioned multi-add staging sheet for Fotos & Vídeos — mirrors `PhotoStagingSheet`'s
/// "add several, review, confirm as one action" shape (see `redesign-daily-report-experience`
/// task group 9.4) but extended with a caption field per item and video capture/selection, so it
/// isn't just a reuse of that simpler widget.
class _MediaStagingSheet extends StatefulWidget {
  const _MediaStagingSheet();

  @override
  State<_MediaStagingSheet> createState() => _MediaStagingSheetState();
}

class _MediaStagingSheetState extends State<_MediaStagingSheet> {
  final List<_MediaStagingItem> _items = [];

  Future<void> _addPhotoFromCamera() async {
    final photo = await ImagePicker().pickImage(source: ImageSource.camera, imageQuality: 85);
    if (photo != null && mounted) setState(() => _items.add(_MediaStagingItem(file: photo, isVideo: false)));
  }

  Future<void> _addVideoFromCamera() async {
    final video = await ImagePicker().pickVideo(source: ImageSource.camera);
    if (video != null && mounted) setState(() => _items.add(_MediaStagingItem(file: video, isVideo: true)));
  }

  Future<void> _addFromGallery() async {
    List<XFile> picked = const [];
    try {
      picked = await ImagePicker().pickMultipleMedia(imageQuality: 85);
    } catch (_) {
      // No native multi-picker available — leave the staged list unchanged.
    }
    if (picked.isNotEmpty && mounted) {
      setState(() => _items.addAll(picked.map((f) => _MediaStagingItem(file: f, isVideo: _looksLikeVideoPath(f.path)))));
    }
  }

  void _showAddSource() {
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
                _addPhotoFromCamera();
              },
            ),
            ListTile(
              leading: const Icon(Icons.videocam_outlined),
              title: const Text('Gravar vídeo'),
              onTap: () {
                Navigator.pop(context);
                _addVideoFromCamera();
              },
            ),
            ListTile(
              leading: const Icon(Icons.photo_library_outlined),
              title: const Text('Escolher da galeria'),
              onTap: () {
                Navigator.pop(context);
                _addFromGallery();
              },
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.only(left: 20, right: 20, top: 20, bottom: 20 + MediaQuery.of(context).viewInsets.bottom),
        child: ConstrainedBox(
          constraints: BoxConstraints(maxHeight: MediaQuery.of(context).size.height * 0.8),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Adicionar mídia', style: TextStyle(fontWeight: FontWeight.w800, fontSize: 16)),
              const SizedBox(height: 4),
              const Text('Fotos e vídeos ficam disponíveis após o envio.', style: TextStyle(fontSize: 12.5, color: AppColors.steel500)),
              const SizedBox(height: 14),
              if (_items.isNotEmpty)
                Flexible(
                  child: ListView.separated(
                    shrinkWrap: true,
                    itemCount: _items.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 10),
                    itemBuilder: (context, index) {
                      final item = _items[index];
                      return Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Stack(
                            children: [
                              ClipRRect(
                                borderRadius: BorderRadius.circular(10),
                                child: item.isVideo
                                    ? Container(
                                        width: 64,
                                        height: 64,
                                        color: AppColors.steel100,
                                        alignment: Alignment.center,
                                        child: const Icon(Icons.videocam_outlined, color: AppColors.steel400),
                                      )
                                    : Image.file(File(item.file.path), width: 64, height: 64, fit: BoxFit.cover),
                              ),
                            ],
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: TextField(
                              decoration: const InputDecoration(labelText: 'Legenda (opcional)', isDense: true),
                              onChanged: (v) => item.caption = v,
                            ),
                          ),
                          IconButton(
                            icon: const Icon(Icons.close, size: 18, color: AppColors.steel400),
                            onPressed: () => setState(() => _items.removeAt(index)),
                          ),
                        ],
                      );
                    },
                  ),
                ),
              if (_items.isNotEmpty) const SizedBox(height: 12),
              OutlinedButton.icon(onPressed: _showAddSource, icon: const Icon(Icons.add_a_photo_outlined, size: 18), label: const Text('Adicionar')),
              const SizedBox(height: 18),
              Row(
                children: [
                  Expanded(child: OutlinedButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Cancelar'))),
                  const SizedBox(width: 10),
                  Expanded(
                    child: FilledButton(
                      style: FilledButton.styleFrom(backgroundColor: AppColors.blueprint600),
                      onPressed: _items.isEmpty ? null : () => Navigator.of(context).pop(_items),
                      child: const Text('Confirmar'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
