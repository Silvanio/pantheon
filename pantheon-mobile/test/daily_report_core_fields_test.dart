import 'package:flutter_test/flutter_test.dart';
import 'package:pantheon_mobile/features/daily_reports/daily_report_models.dart';

void main() {
  group('dailyReportCoreFieldsComplete', () {
    test('all three fields filled in is complete', () {
      expect(
        dailyReportCoreFieldsComplete(weatherCondition: 'SUNNY', workHoursStart: '08:00', workHoursEnd: '17:00'),
        isTrue,
      );
    });

    test('all three fields empty is not complete (nothing to save yet)', () {
      expect(dailyReportCoreFieldsComplete(weatherCondition: '', workHoursStart: '', workHoursEnd: ''), isFalse);
    });

    test('missing weather only is not complete', () {
      expect(
        dailyReportCoreFieldsComplete(weatherCondition: '', workHoursStart: '08:00', workHoursEnd: '17:00'),
        isFalse,
      );
    });

    test('missing start time only is not complete', () {
      expect(
        dailyReportCoreFieldsComplete(weatherCondition: 'RAINY', workHoursStart: '', workHoursEnd: '17:00'),
        isFalse,
      );
    });

    test('missing end time only is not complete', () {
      expect(
        dailyReportCoreFieldsComplete(weatherCondition: 'CLOUDY', workHoursStart: '08:00', workHoursEnd: ''),
        isFalse,
      );
    });
  });
}
