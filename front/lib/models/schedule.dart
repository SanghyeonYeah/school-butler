class Schedule {
  String id;
  String title;
  DateTime dateTime;
  String? location;
  String? memo;
  List<String> tags;
  bool isCompleted;
  bool isRepeating;
  String? repeatPattern; // daily, weekdays, weekly, custom

  Schedule({
    required this.id,
    required this.title,
    required this.dateTime,
    this.location,
    this.memo,
    this.tags = const [],
    this.isCompleted = false,
    this.isRepeating = false,
    this.repeatPattern,
  });

  // JSON 직렬화
  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'dateTime': dateTime.toIso8601String(),
        'location': location,
        'memo': memo,
        'tags': tags,
        'isCompleted': isCompleted,
        'isRepeating': isRepeating,
        'repeatPattern': repeatPattern,
      };

  factory Schedule.fromJson(Map<String, dynamic> json) => Schedule(
        id: json['id'],
        title: json['title'],
        dateTime: DateTime.parse(json['dateTime']),
        location: json['location'],
        memo: json['memo'],
        tags: List<String>.from(json['tags'] ?? []),
        isCompleted: json['isCompleted'] ?? false,
        isRepeating: json['isRepeating'] ?? false,
        repeatPattern: json['repeatPattern'],
      );
}
