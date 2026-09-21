/// A `Page<T>` envelope as returned by Spring's paginated list endpoints — mirrors
/// `pantheon-web`'s `PageResponse<T>` TypeScript interface.
class PageResponse<T> {
  PageResponse({required this.content, required this.totalElements, required this.totalPages, required this.number});

  factory PageResponse.fromJson(Map<String, dynamic> json, T Function(Map<String, dynamic>) fromJson) {
    return PageResponse(
      content: (json['content'] as List).map((e) => fromJson(e as Map<String, dynamic>)).toList(),
      totalElements: json['totalElements'] as int,
      totalPages: json['totalPages'] as int,
      number: json['number'] as int,
    );
  }

  final List<T> content;
  final int totalElements;
  final int totalPages;
  final int number;
}
