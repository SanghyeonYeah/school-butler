import 'package:flutter/foundation.dart';
import 'package:uuid/uuid.dart';
import '../models/day_review.dart';

class DayReviewProvider extends ChangeNotifier {
  final List<DayReview> _reviews = [];
  final Uuid _uuid = const Uuid();

  List<DayReview> get allReviews => List.unmodifiable(_reviews);

  // 오늘 회고 가져오기
  DayReview? getTodayReview() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);

    try {
      return _reviews.firstWhere((review) {
        final reviewDate = DateTime(
          review.date.year,
          review.date.month,
          review.date.day,
        );
        return reviewDate.isAtSameMomentAs(today);
      });
    } catch (e) {
      return null;
    }
  }

  // 특정 날짜 회고 가져오기
  DayReview? getReviewByDate(DateTime date) {
    final targetDate = DateTime(date.year, date.month, date.day);

    try {
      return _reviews.firstWhere((review) {
        final reviewDate = DateTime(
          review.date.year,
          review.date.month,
          review.date.day,
        );
        return reviewDate.isAtSameMomentAs(targetDate);
      });
    } catch (e) {
      return null;
    }
  }

  // 회고 저장 또는 업데이트
  Future<void> saveReview(DayReview review) async {
    final index = _reviews.indexWhere((r) => r.id == review.id);
    if (index != -1) {
      _reviews[index] = review;
    } else {
      _reviews.add(review);
    }
    notifyListeners();
  }

  // 새 회고 생성 헬퍼
  DayReview createReview({
    required DateTime date,
    required int rating,
    String? keyword,
    String? noteForTomorrow,
    required int completedSchedules,
    required int totalSchedules,
    required int completedTodos,
    required int totalTodos,
    int focusMinutes = 0,
  }) {
    return DayReview(
      id: _uuid.v4(),
      date: date,
      rating: rating,
      keyword: keyword,
      noteForTomorrow: noteForTomorrow,
      completedSchedules: completedSchedules,
      totalSchedules: totalSchedules,
      completedTodos: completedTodos,
      totalTodos: totalTodos,
      focusMinutes: focusMinutes,
    );
  }

  // 완료율 계산
  int getCompletionRate(DayReview review) {
    final totalTasks = review.totalSchedules + review.totalTodos;
    if (totalTasks == 0) return 0;

    final completedTasks = review.completedSchedules + review.completedTodos;
    return ((completedTasks / totalTasks) * 100).round();
  }

  // 주간 평균 계산
  Map<String, dynamic> getWeeklyAverage() {
    final now = DateTime.now();
    final weekAgo = now.subtract(const Duration(days: 7));

    final weekReviews = _reviews.where((review) {
      return review.date.isAfter(weekAgo) && review.date.isBefore(now);
    }).toList();

    if (weekReviews.isEmpty) {
      return {
        'avgRating': 0.0,
        'avgCompletionRate': 0.0,
        'totalFocusMinutes': 0,
      };
    }

    final totalRating = weekReviews.fold(0, (sum, review) => sum + review.rating);
    final totalCompletionRate =
        weekReviews.fold(0, (sum, review) => sum + getCompletionRate(review));
    final totalFocus =
        weekReviews.fold(0, (sum, review) => sum + review.focusMinutes);

    return {
      'avgRating': totalRating / weekReviews.length,
      'avgCompletionRate': totalCompletionRate / weekReviews.length,
      'totalFocusMinutes': totalFocus,
    };
  }
}
