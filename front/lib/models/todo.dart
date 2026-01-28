class Todo {
  String id;
  String title;
  DateTime? dueDate;
  int priority; // 1: 높음, 2: 보통, 3: 낮음
  bool isCompleted;
  String? memo;
  List<String> tags;
  DateTime createdAt;

  Todo({
    required this.id,
    required this.title,
    this.dueDate,
    this.priority = 2,
    this.isCompleted = false,
    this.memo,
    this.tags = const [],
    required this.createdAt,
  });

  // JSON 직렬화
  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'dueDate': dueDate?.toIso8601String(),
        'priority': priority,
        'isCompleted': isCompleted,
        'memo': memo,
        'tags': tags,
        'createdAt': createdAt.toIso8601String(),
      };

  factory Todo.fromJson(Map<String, dynamic> json) => Todo(
        id: json['id'],
        title: json['title'],
        dueDate: json['dueDate'] != null ? DateTime.parse(json['dueDate']) : null,
        priority: json['priority'] ?? 2,
        isCompleted: json['isCompleted'] ?? false,
        memo: json['memo'],
        tags: List<String>.from(json['tags'] ?? []),
        createdAt: DateTime.parse(json['createdAt']),
      );
}
