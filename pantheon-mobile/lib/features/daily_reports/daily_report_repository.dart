import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';
import 'daily_report_models.dart';

class DailyReportRepository {
  DailyReportRepository(this._client);
  final ApiClient _client;

  Future<List<DailyReport>> list(String siteId) async {
    final json = await _client.get<List<dynamic>>('/api/construction-sites/$siteId/daily-reports');
    return json.map((e) => DailyReport.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<DailyReport> create(String siteId, String reportDate) async {
    final json = await _client.post<Map<String, dynamic>>(
      '/api/construction-sites/$siteId/daily-reports',
      body: {'reportDate': reportDate},
    );
    return DailyReport.fromJson(json);
  }

  Future<DailyReportDetail> getDetail(String reportId) async {
    final json = await _client.get<Map<String, dynamic>>('/api/daily-reports/$reportId');
    return DailyReportDetail.fromJson(json);
  }

  Future<DailyReport> updateCore(
    String reportId, {
    String? weatherCondition,
    bool? weatherBlockedTasks,
    String? workHoursStart,
    String? workHoursEnd,
    String? comments,
  }) async {
    final json = await _client.patch<Map<String, dynamic>>(
      '/api/daily-reports/$reportId',
      body: {
        'weatherCondition': weatherCondition,
        'weatherBlockedTasks': weatherBlockedTasks,
        'workHoursStart': workHoursStart,
        'workHoursEnd': workHoursEnd,
        'comments': comments,
      },
    );
    return DailyReport.fromJson(json);
  }

  Future<DailyReport> submit(String reportId) async {
    final json = await _client.post<Map<String, dynamic>>('/api/daily-reports/$reportId/submit');
    return DailyReport.fromJson(json);
  }

  Future<List<ReportMedia>> listMedia(String reportId) async {
    final json = await _client.get<List<dynamic>>('/api/daily-reports/$reportId/media');
    return json.map((e) => ReportMedia.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<ReportMedia> uploadPhoto(String reportId, String filePath, {String? caption}) async {
    final formData = FormData.fromMap({'file': await MultipartFile.fromFile(filePath)});
    final query = StringBuffer('?type=PHOTO');
    if (caption != null && caption.isNotEmpty) query.write('&caption=${Uri.encodeComponent(caption)}');
    final json = await _client.uploadMultipart<Map<String, dynamic>>(
      '/api/daily-reports/$reportId/media$query',
      formData,
    );
    return ReportMedia.fromJson(json);
  }
}

final dailyReportRepositoryProvider = Provider((ref) => DailyReportRepository(ref.watch(apiClientProvider)));
