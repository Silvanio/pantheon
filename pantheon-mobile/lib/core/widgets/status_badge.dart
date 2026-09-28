import 'package:flutter/material.dart';
import '../../theme/app_colors.dart';

/// Mirrors `pantheon-web`'s `StatusBadge.vue`: same kinds, same color-per-status mapping,
/// same pt-BR labels — kept as a single source of truth here rather than per-screen ad hoc
/// colors, so a future status/color change stays a one-file edit.
enum StatusBadgeKind {
  purchaseRequest,
  purchaseRequestItem,
  orcamento,
  approval,
  constructionSite,
  material,
  dailyReport,
  equipment,
  dailyReportActivity,
}

class _Style {
  const _Style(this.background, this.foreground);
  final Color background;
  final Color foreground;
}

const _steelStyle = _Style(AppColors.steel100, AppColors.steel700);
const _amberStyle = _Style(AppColors.amber50, AppColors.amber700);
const _blueprintStyle = _Style(AppColors.blueprint100, AppColors.blueprint700);
const _emeraldStyle = _Style(AppColors.emerald50, AppColors.emerald700);
const _safetyStyle = _Style(AppColors.safety100, AppColors.safety600);

final Map<StatusBadgeKind, Map<String, _Style>> _classMap = {
  StatusBadgeKind.purchaseRequest: {
    'INICIADO': _steelStyle,
    'ORCADO': _amberStyle,
    'CONFERIDO': _blueprintStyle,
    'CONCLUIDO': _emeraldStyle,
  },
  StatusBadgeKind.orcamento: {
    'DRAFT': _steelStyle,
    'LOCKED': _blueprintStyle,
  },
  StatusBadgeKind.purchaseRequestItem: {
    'PENDING': _steelStyle,
    'CONVERTED': _blueprintStyle,
  },
  StatusBadgeKind.approval: {
    'PENDING': _amberStyle,
    'APPROVED': _emeraldStyle,
    'REJECTED': _safetyStyle,
  },
  StatusBadgeKind.constructionSite: {
    'PLANNING': _blueprintStyle,
    'IN_PROGRESS': _emeraldStyle,
    'PAUSED': _amberStyle,
    'COMPLETED': _steelStyle,
  },
  StatusBadgeKind.material: {
    'AWAITING_DELIVERY': _amberStyle,
    'DELIVERED': _blueprintStyle,
    'DELIVERED_AND_CHECKED': _emeraldStyle,
  },
  // `daily-report-approval-workflow`: DRAFT -> PENDING_APPROVAL -> APPROVED (SUBMITTED removed).
  StatusBadgeKind.dailyReport: {
    'DRAFT': _steelStyle,
    'PENDING_APPROVAL': _amberStyle,
    'APPROVED': _emeraldStyle,
  },
  // `redesign-daily-report-experience` (task group 9): equipment-usage rows resolved against the
  // registry's own `Equipment.status`, same four values as `equipment_repository.dart`'s
  // `equipmentStatusLabels` (kept separate from that map rather than imported from it, matching
  // this file's existing per-kind self-containment).
  StatusBadgeKind.equipment: {
    'AVAILABLE': _emeraldStyle,
    'IN_USE': _blueprintStyle,
    'MAINTENANCE': _amberStyle,
    'UNAVAILABLE': _steelStyle,
  },
  StatusBadgeKind.dailyReportActivity: {
    'IN_PROGRESS': _steelStyle,
    'COMPLETED': _emeraldStyle,
  },
};

final Map<StatusBadgeKind, Map<String, String>> _labelMap = {
  StatusBadgeKind.purchaseRequest: {
    'INICIADO': 'Iniciado',
    'ORCADO': 'Orçado',
    'CONFERIDO': 'Conferido',
    'CONCLUIDO': 'Concluído',
  },
  StatusBadgeKind.orcamento: {'DRAFT': 'Rascunho', 'LOCKED': 'Bloqueado'},
  StatusBadgeKind.purchaseRequestItem: {'PENDING': 'Pendente', 'CONVERTED': 'Convertido'},
  StatusBadgeKind.approval: {'PENDING': 'Pendente', 'APPROVED': 'Aprovado', 'REJECTED': 'Rejeitado'},
  StatusBadgeKind.constructionSite: {
    'PLANNING': 'Planejamento',
    'IN_PROGRESS': 'Em execução',
    'PAUSED': 'Pausada',
    'COMPLETED': 'Concluída',
  },
  StatusBadgeKind.material: {
    'AWAITING_DELIVERY': 'Aguardando entrega',
    'DELIVERED': 'Entregue',
    'DELIVERED_AND_CHECKED': 'Entregue e conferido',
  },
  StatusBadgeKind.dailyReport: {
    'DRAFT': 'Rascunho',
    'PENDING_APPROVAL': 'Em aprovação',
    'APPROVED': 'Aprovado',
  },
  StatusBadgeKind.equipment: {
    'AVAILABLE': 'Disponível',
    'IN_USE': 'Em uso',
    'MAINTENANCE': 'Manutenção',
    'UNAVAILABLE': 'Indisponível',
  },
  StatusBadgeKind.dailyReportActivity: {
    'IN_PROGRESS': 'Em andamento',
    'COMPLETED': 'Concluída',
  },
};

class StatusBadge extends StatelessWidget {
  const StatusBadge({super.key, required this.kind, required this.status});

  final StatusBadgeKind kind;
  final String status;

  @override
  Widget build(BuildContext context) {
    final style = _classMap[kind]?[status] ?? _steelStyle;
    final label = _labelMap[kind]?[status] ?? status;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(color: style.background, borderRadius: BorderRadius.circular(999)),
      child: Text(
        label,
        style: TextStyle(color: style.foreground, fontSize: 11.5, fontWeight: FontWeight.w700),
      ),
    );
  }
}
