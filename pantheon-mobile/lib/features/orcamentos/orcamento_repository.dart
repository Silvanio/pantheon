import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';
import '../../core/models/page_response.dart';
import 'orcamento_models.dart';

class OrcamentoRepository {
  OrcamentoRepository(this._client);
  final ApiClient _client;

  Future<PageResponse<Orcamento>> list(String siteId, {int page = 0, int size = 20}) async {
    final json = await _client.get<Map<String, dynamic>>(
      '/api/construction-sites/$siteId/orcamentos',
      query: {'page': page, 'size': size},
    );
    return PageResponse.fromJson(json, Orcamento.fromJson);
  }

  Future<OrcamentoDetail> getDetail(String id) async {
    final json = await _client.get<Map<String, dynamic>>('/api/orcamentos/$id');
    return OrcamentoDetail.fromJson(json);
  }
}

final orcamentoRepositoryProvider = Provider((ref) => OrcamentoRepository(ref.watch(apiClientProvider)));
