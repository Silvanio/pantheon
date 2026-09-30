import 'package:flutter/material.dart';

/// The Pantheon "P" monogram glyph — same geometry as `pantheon-icon.svg` (24x24
/// viewBox), drawn with [CustomPainter] instead of an SVG asset so the app doesn't
/// need the `flutter_svg` dependency for a single small mark.
class PantheonMark extends StatelessWidget {
  const PantheonMark({super.key, required this.size, this.color = Colors.white});

  final double size;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(size: Size.square(size), painter: _PantheonMarkPainter(color));
  }
}

class _PantheonMarkPainter extends CustomPainter {
  _PantheonMarkPainter(this.color);
  final Color color;

  @override
  void paint(Canvas canvas, Size size) {
    final path = Path()
      ..moveTo(6, 4)
      ..lineTo(8.3, 4)
      ..lineTo(8.3, 20)
      ..lineTo(6, 20)
      ..close()
      ..moveTo(8.3, 4)
      ..lineTo(15.2, 4)
      ..cubicTo(16.99, 4, 18.4, 5.46, 18.4, 7.3)
      ..cubicTo(18.4, 9.14, 16.99, 10.6, 15.2, 10.6)
      ..lineTo(8.3, 10.6)
      ..lineTo(8.3, 4)
      ..close();
    canvas.scale(size.width / 24, size.height / 24);
    canvas.drawPath(path, Paint()..color = color);
  }

  @override
  bool shouldRepaint(covariant _PantheonMarkPainter oldDelegate) => oldDelegate.color != color;
}
