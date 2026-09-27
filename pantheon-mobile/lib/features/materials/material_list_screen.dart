import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:image_picker/image_picker.dart';

import '../../core/widgets/async_value_view.dart';
import '../../core/widgets/offline_dialogs.dart';
import '../../core/widgets/status_badge.dart';
import '../../theme/app_colors.dart';
import '../site/site_repository.dart';
import 'material_models.dart';
import 'material_repository.dart';

final _materialsProvider = FutureProvider.family((ref, String siteId) => ref.watch(materialRepositoryProvider).list(siteId));

/// Mirrors `pantheon-web`'s `MaterialsPanel` `canManage` prop
/// (`myPermissions?.ORCAMENTO_MANAGE === 'MANAGE'`), which gates the delivery-tracking actions.
final _canManageProvider = FutureProvider.family(
  (ref, String siteId) => ref.watch(siteRepositoryProvider).getMyPermissions(siteId).then((p) => p['ORCAMENTO_MANAGE'] == 'MANAGE'),
);

/// Delivery tracking for materials generated from concluded Pedidos de Compra — mirrors
/// `pantheon-web`'s `MaterialsPanel.vue`. Reachable from the obra home's entry grid.
class MaterialListScreen extends ConsumerStatefulWidget {
  const MaterialListScreen({super.key, required this.siteId});
  final String siteId;

  @override
  ConsumerState<MaterialListScreen> createState() => _MaterialListScreenState();
}

class _MaterialListScreenState extends ConsumerState<MaterialListScreen> {
  String? _actingId;

  Future<void> _markDelivered(MaterialDelivery material) async {
    if (!await requireOnline(context, ref)) return;
    setState(() => _actingId = material.id);
    try {
      await ref.read(materialRepositoryProvider).markDelivered(material.id);
      ref.invalidate(_materialsProvider(widget.siteId));
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('Não foi possível atualizar o status do material.')));
      }
    } finally {
      if (mounted) setState(() => _actingId = null);
    }
  }

  Future<void> _markChecked(MaterialDelivery material) async {
    if (!mounted || !await requireOnline(context, ref)) return;
    if (!mounted) return;
    final photos = await showModalBottomSheet<List<XFile>>(
      context: context,
      isScrollControlled: true,
      builder: (context) => const _MaterialCheckPhotoSheet(),
    );
    if (photos == null || !mounted) return; // cancelled
    setState(() => _actingId = material.id);
    try {
      await ref.read(materialRepositoryProvider).markChecked(material.id, photos.map((p) => p.path).toList());
      ref.invalidate(_materialsProvider(widget.siteId));
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('Não foi possível atualizar o status do material.')));
      }
    } finally {
      if (mounted) setState(() => _actingId = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final materials = ref.watch(_materialsProvider(widget.siteId));
    final canManage = ref.watch(_canManageProvider(widget.siteId));
    return Scaffold(
      backgroundColor: AppColors.steel50,
      appBar: AppBar(title: const Text('Materiais')),
      body: RefreshIndicator(
        onRefresh: () => ref.refresh(_materialsProvider(widget.siteId).future),
        child: AsyncValueView(
          value: materials,
          errorMessage: 'Não foi possível carregar os materiais.',
          data: (items) {
            if (items.isEmpty) {
              return ListView(
                children: const [
                  EmptyState(
                    icon: Icons.local_shipping_outlined,
                    message: 'Nenhum material registrado ainda.\nMateriais aparecem aqui quando um pedido de compra é concluído.',
                  ),
                ],
              );
            }
            return canManage.when(
              data: (allowed) => _list(items, allowed),
              loading: () => _list(items, false),
              error: (_, _) => _list(items, false),
            );
          },
        ),
      ),
    );
  }

  Widget _list(List<MaterialDelivery> items, bool canManage) {
    return ListView.separated(
      padding: const EdgeInsets.all(16),
      itemCount: items.length,
      separatorBuilder: (_, _) => const SizedBox(height: 10),
      itemBuilder: (context, index) => _MaterialCard(
        material: items[index],
        canManage: canManage,
        acting: _actingId == items[index].id,
        onMarkDelivered: () => _markDelivered(items[index]),
        onMarkChecked: () => _markChecked(items[index]),
      ),
    );
  }
}

/// Stages photos for a material's conferência before submitting — mirrors `pantheon-web`'s
/// `MaterialsPanel.vue` check-modal (add several, review, then confirm as one action), but with
/// a device-native choice per addition: a single camera shot (tap again to add more) or several
/// gallery photos at once via [ImagePicker.pickMultiImage]. Returns the staged list on
/// "Confirmar", or `null` if dismissed/cancelled.
class _MaterialCheckPhotoSheet extends StatefulWidget {
  const _MaterialCheckPhotoSheet();

  @override
  State<_MaterialCheckPhotoSheet> createState() => _MaterialCheckPhotoSheetState();
}

class _MaterialCheckPhotoSheetState extends State<_MaterialCheckPhotoSheet> {
  final List<XFile> _photos = [];

  Future<void> _addFromCamera() async {
    final photo = await ImagePicker().pickImage(source: ImageSource.camera, imageQuality: 85);
    if (photo != null && mounted) setState(() => _photos.add(photo));
  }

  Future<void> _addFromGallery() async {
    List<XFile> picked = const [];
    try {
      picked = await ImagePicker().pickMultiImage(imageQuality: 85);
    } catch (_) {
      // No native multi-picker available — leave the staged list unchanged.
    }
    if (picked.isNotEmpty && mounted) setState(() => _photos.addAll(picked));
  }

  void _showAddPhotoSource() {
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
                _addFromCamera();
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
        padding: EdgeInsets.only(
          left: 20,
          right: 20,
          top: 20,
          bottom: 20 + MediaQuery.of(context).viewInsets.bottom,
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Conferência de material', style: TextStyle(fontWeight: FontWeight.w700, fontSize: 16)),
            const SizedBox(height: 4),
            const Text(
              'Anexe as fotos da conferência antes de confirmar.',
              style: TextStyle(fontSize: 13, color: AppColors.steel500),
            ),
            const SizedBox(height: 14),
            if (_photos.isNotEmpty)
              SizedBox(
                height: 84,
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  itemCount: _photos.length,
                  separatorBuilder: (_, _) => const SizedBox(width: 8),
                  itemBuilder: (context, index) => Stack(
                    clipBehavior: Clip.none,
                    children: [
                      ClipRRect(
                        borderRadius: BorderRadius.circular(10),
                        child: Image.file(File(_photos[index].path), width: 84, height: 84, fit: BoxFit.cover),
                      ),
                      Positioned(
                        right: -6,
                        top: -6,
                        child: GestureDetector(
                          onTap: () => setState(() => _photos.removeAt(index)),
                          child: Container(
                            padding: const EdgeInsets.all(2),
                            decoration: const BoxDecoration(color: AppColors.safety500, shape: BoxShape.circle),
                            child: const Icon(Icons.close, size: 14, color: Colors.white),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            if (_photos.isNotEmpty) const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: _showAddPhotoSource,
              icon: const Icon(Icons.add_a_photo_outlined, size: 18),
              label: const Text('Adicionar fotos'),
            ),
            const SizedBox(height: 18),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => Navigator.of(context).pop(),
                    child: const Text('Cancelar'),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: FilledButton(
                    style: FilledButton.styleFrom(backgroundColor: AppColors.emerald600),
                    onPressed: () => Navigator.of(context).pop(_photos),
                    child: const Text('Confirmar'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _MaterialCard extends StatelessWidget {
  const _MaterialCard({
    required this.material,
    required this.canManage,
    required this.acting,
    required this.onMarkDelivered,
    required this.onMarkChecked,
  });

  final MaterialDelivery material;
  final bool canManage;
  final bool acting;
  final VoidCallback onMarkDelivered;
  final VoidCallback onMarkChecked;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.steel200),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  height: 42,
                  width: 42,
                  decoration: BoxDecoration(color: AppColors.blueprint50, borderRadius: BorderRadius.circular(12)),
                  alignment: Alignment.center,
                  child: const Icon(Icons.inventory_2_outlined, color: AppColors.blueprint600, size: 20),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(material.name, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 14.5)),
                      const SizedBox(height: 3),
                      Text(
                        '${material.quantity}${material.type != null ? ' · ${material.type}' : ''}',
                        style: const TextStyle(fontSize: 12.5, color: AppColors.steel500),
                      ),
                      if (material.sourcePurchaseRequestName != null) ...[
                        const SizedBox(height: 3),
                        Row(
                          children: [
                            const Icon(Icons.shopping_cart_outlined, size: 12, color: AppColors.steel400),
                            const SizedBox(width: 4),
                            Expanded(
                              child: Text(
                                material.sourcePurchaseRequestName!,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: const TextStyle(fontSize: 11.5, color: AppColors.steel400),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                StatusBadge(kind: StatusBadgeKind.material, status: material.status),
              ],
            ),
            if (canManage && material.status != 'DELIVERED_AND_CHECKED') ...[
              const Divider(height: 20),
              Row(
                children: [
                  Expanded(
                    child: switch (material.status) {
                      'AWAITING_DELIVERY' => OutlinedButton.icon(
                          onPressed: acting ? null : onMarkDelivered,
                          icon: acting
                              ? const SizedBox(height: 14, width: 14, child: CircularProgressIndicator(strokeWidth: 2))
                              : const Icon(Icons.local_shipping_outlined, size: 17),
                          label: const Text('Marcar como entregue'),
                        ),
                      'DELIVERED' => OutlinedButton.icon(
                          onPressed: acting ? null : onMarkChecked,
                          icon: acting
                              ? const SizedBox(height: 14, width: 14, child: CircularProgressIndicator(strokeWidth: 2))
                              : const Icon(Icons.fact_check_outlined, size: 17),
                          label: const Text('Anexar foto e conferir'),
                          style: OutlinedButton.styleFrom(foregroundColor: AppColors.emerald700),
                        ),
                      _ => const SizedBox.shrink(),
                    },
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}
