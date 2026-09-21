import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/api/api_client.dart';

class Equipment {
  Equipment({required this.id, required this.name, this.type, required this.status});

  factory Equipment.fromJson(Map<String, dynamic> json) => Equipment(
        id: json['id'] as String,
        name: json['name'] as String,
        type: json['type'] as String?,
        status: json['status'] as String,
      );

  final String id;
  final String name;
  final String? type;
  final String status;
}

class EquipmentRepository {
  EquipmentRepository(this._client);
  final ApiClient _client;

  Future<List<Equipment>> list(String siteId) async {
    final json = await _client.get<List<dynamic>>('/api/construction-sites/$siteId/equipment');
    return json.map((e) => Equipment.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<Equipment> create(String siteId, String name, String? type) async {
    final json = await _client.post<Map<String, dynamic>>(
      '/api/construction-sites/$siteId/equipment',
      body: {'name': name, 'type': type, 'status': 'AVAILABLE'},
    );
    return Equipment.fromJson(json);
  }
}

final equipmentRepositoryProvider = Provider((ref) => EquipmentRepository(ref.watch(apiClientProvider)));

const equipmentStatusLabels = {
  'AVAILABLE': 'Disponível',
  'IN_USE': 'Em uso',
  'MAINTENANCE': 'Manutenção',
  'UNAVAILABLE': 'Indisponível',
};
