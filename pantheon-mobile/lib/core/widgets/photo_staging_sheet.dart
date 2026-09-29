import 'dart:io';

import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';

import '../../theme/app_colors.dart';

/// Stages one or more photos before submitting a single confirming action — originally built for
/// `material_list_screen.dart`'s "conferência" flow (add several, review, remove any, then
/// confirm as one action) and pulled out here so `daily_report_detail_screen.dart` can reuse the
/// exact same pattern for "marcar conferido" on a delivered material (see
/// `redesign-daily-report-experience` task group 9), instead of rebuilding it. Offers a
/// device-native choice per addition: a single camera shot (tap again to add more) or several
/// gallery photos at once via [ImagePicker.pickMultiImage]. Returns the staged list on
/// "Confirmar", or `null` if dismissed/cancelled.
class PhotoStagingSheet extends StatefulWidget {
  const PhotoStagingSheet({
    super.key,
    required this.title,
    required this.subtitle,
    this.confirmLabel = 'Confirmar',
    this.confirmColor = AppColors.emerald600,
  });

  final String title;
  final String subtitle;
  final String confirmLabel;
  final Color confirmColor;

  @override
  State<PhotoStagingSheet> createState() => _PhotoStagingSheetState();
}

class _PhotoStagingSheetState extends State<PhotoStagingSheet> {
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
            Text(widget.title, style: const TextStyle(fontWeight: FontWeight.w700, fontSize: 16)),
            const SizedBox(height: 4),
            Text(widget.subtitle, style: const TextStyle(fontSize: 13, color: AppColors.steel500)),
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
                    style: FilledButton.styleFrom(backgroundColor: widget.confirmColor),
                    onPressed: () => Navigator.of(context).pop(_photos),
                    child: Text(widget.confirmLabel),
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
