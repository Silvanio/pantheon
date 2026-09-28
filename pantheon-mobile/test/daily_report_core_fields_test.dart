import 'package:flutter_test/flutter_test.dart';
import 'package:pantheon_mobile/features/daily_reports/daily_report_models.dart';

void main() {
  group('dailyReportCoreFieldsComplete', () {
    test('all four fields filled in is complete', () {
      expect(
        dailyReportCoreFieldsComplete(
          weatherConditionMorning: 'SUNNY',
          weatherConditionAfternoon: 'CLOUDY',
          workHoursStart: '08:00',
          workHoursEnd: '17:00',
        ),
        isTrue,
      );
    });

    test('all four fields empty is not complete (nothing to save yet)', () {
      expect(
        dailyReportCoreFieldsComplete(
          weatherConditionMorning: '',
          weatherConditionAfternoon: '',
          workHoursStart: '',
          workHoursEnd: '',
        ),
        isFalse,
      );
    });

    test('missing morning weather only is not complete', () {
      expect(
        dailyReportCoreFieldsComplete(
          weatherConditionMorning: '',
          weatherConditionAfternoon: 'CLOUDY',
          workHoursStart: '08:00',
          workHoursEnd: '17:00',
        ),
        isFalse,
      );
    });

    test('missing afternoon weather only is not complete', () {
      expect(
        dailyReportCoreFieldsComplete(
          weatherConditionMorning: 'SUNNY',
          weatherConditionAfternoon: '',
          workHoursStart: '08:00',
          workHoursEnd: '17:00',
        ),
        isFalse,
      );
    });

    test('missing start time only is not complete', () {
      expect(
        dailyReportCoreFieldsComplete(
          weatherConditionMorning: 'RAINY',
          weatherConditionAfternoon: 'STORM',
          workHoursStart: '',
          workHoursEnd: '17:00',
        ),
        isFalse,
      );
    });

    test('missing end time only is not complete', () {
      expect(
        dailyReportCoreFieldsComplete(
          weatherConditionMorning: 'CLOUDY',
          weatherConditionAfternoon: 'SUNNY',
          workHoursStart: '08:00',
          workHoursEnd: '',
        ),
        isFalse,
      );
    });
  });

  // `redesign-daily-report-experience` task group 9: the "global save" staging model on
  // `daily_report_detail_screen.dart` diffs a local working list of ids against the ids the
  // server originally returned to know which rows to `DELETE` on save — pulled out as
  // `staleServerIds` so that diff logic is unit-testable without the whole screen.
  group('staleServerIds', () {
    test('a server id no longer present locally is stale', () {
      expect(
        staleServerIds(originalIds: {'a', 'b'}, currentIds: ['a']),
        {'b'},
      );
    });

    test('a locally-added row with no id yet is not stale (nothing to delete)', () {
      expect(
        staleServerIds(originalIds: {'a'}, currentIds: ['a', null]),
        isEmpty,
      );
    });

    test('nothing removed means nothing stale', () {
      expect(
        staleServerIds(originalIds: {'a', 'b'}, currentIds: ['a', 'b']),
        isEmpty,
      );
    });

    test('everything removed means everything is stale', () {
      expect(
        staleServerIds(originalIds: {'a', 'b'}, currentIds: const []),
        {'a', 'b'},
      );
    });
  });
}
