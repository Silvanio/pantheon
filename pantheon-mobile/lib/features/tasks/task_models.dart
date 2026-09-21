class TaskColumn {
  TaskColumn({required this.id, required this.name, required this.sortOrder});

  factory TaskColumn.fromJson(Map<String, dynamic> json) =>
      TaskColumn(id: json['id'] as String, name: json['name'] as String, sortOrder: json['sortOrder'] as int);

  final String id;
  final String name;
  final int sortOrder;
}

class TaskCard {
  TaskCard({
    required this.id,
    required this.columnId,
    required this.title,
    this.description,
    this.dueDate,
    required this.sortOrder,
    required this.commentCount,
    required this.attachmentCount,
  });

  factory TaskCard.fromJson(Map<String, dynamic> json) => TaskCard(
        id: json['id'] as String,
        columnId: json['columnId'] as String,
        title: json['title'] as String,
        description: json['description'] as String?,
        dueDate: json['dueDate'] as String?,
        sortOrder: json['sortOrder'] as int,
        commentCount: json['commentCount'] as int,
        attachmentCount: json['attachmentCount'] as int,
      );

  final String id;
  final String columnId;
  final String title;
  final String? description;
  final String? dueDate;
  final int sortOrder;
  final int commentCount;
  final int attachmentCount;
}

class TaskBoard {
  TaskBoard({required this.columns, required this.cards});

  factory TaskBoard.fromJson(Map<String, dynamic> json) => TaskBoard(
        columns: (json['columns'] as List).map((e) => TaskColumn.fromJson(e as Map<String, dynamic>)).toList()
          ..sort((a, b) => a.sortOrder.compareTo(b.sortOrder)),
        cards: (json['cards'] as List).map((e) => TaskCard.fromJson(e as Map<String, dynamic>)).toList(),
      );

  final List<TaskColumn> columns;
  final List<TaskCard> cards;

  List<TaskCard> cardsFor(String columnId) =>
      cards.where((c) => c.columnId == columnId).toList()..sort((a, b) => a.sortOrder.compareTo(b.sortOrder));
}
