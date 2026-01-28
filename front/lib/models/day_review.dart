class DayReview {
  String id;
  DateTime date;
  int rating; // 1-5 별점
  String? keyword; // 오늘을 표현하는 한 단어
  String? noteForTomorrow; // 내일에게 한 마디
  int completedSchedules;
  int totalSchedules;
  int completedTodos;
  int totalTodos;
  int focusMinutes; // 집중 시간 (분)

  DayReview({
    required this.id,
    required this.date,
    required this.rating,
    this.keyword,
    this.noteForTomorrow,
    this.completedSchedules = 0,
    this.totalSchedules = 0,
    this.completedTodos = 0,
    this.totalTodos = 0,
    this.focusMinutes = 0,
  });

  // JSON 직렬화
  Map<String, dynamic> toJson() => {
        'id': id,
        'date': date.toIso8601String(),
        'rating': rating,
        'keyword': keyword,
        'noteForTomorrow': noteForTomorrow,
        'completedSchedules': completedSchedules,
        'totalSchedules': totalSchedules,
        'completedTodos': completedTodos,
        'totalTodos': totalTodos,
        'focusMinutes': focusMinutes,
      };

  factory DayReview.fromJson(Map<String, dynamic> json) => DayReview(
        id: json['id'],
        date: DateTime.parse(json['date']),
        rating: json['rating'],
        keyword: json['keyword'],
        noteForTomorrow: json['noteForTomorrow'],
        completedSchedules: json['completedSchedules'] ?? 0,
        totalSchedules: json['totalSchedules'] ?? 0,
        completedTodos: json['completedTodos'] ?? 0,
        totalTodos: json['totalTodos'] ?? 0,
        focusMinutes: json['focusMinutes'] ?? 0,
      );
}
