import 'package:flutter/foundation.dart';
import 'package:uuid/uuid.dart';
import '../models/schedule.dart';

class ScheduleProvider extends ChangeNotifier {
  final List<Schedule> _schedules = [];
  final Uuid _uuid = const Uuid();

  List<Schedule> get allSchedules => List.unmodifiable(_schedules);

  // 샘플 데이터 추가 (UI 테스트용)
  void addSampleData() {
    final now = DateTime.now();
    _schedules.addAll([
      Schedule(
        id: _uuid.v4(),
        title: '아침 운동',
        dateTime: DateTime(now.year, now.month, now.day, 7, 0),
        location: '집 근처 공원',
        isCompleted: true,
      ),
      Schedule(
        id: _uuid.v4(),
        title: '팀 미팅',
        dateTime: DateTime(now.year, now.month, now.day, 10, 0),
        location: '회의실 A',
      ),
      Schedule(
        id: _uuid.v4(),
        title: '점심 약속',
        dateTime: DateTime(now.year, now.month, now.day, 12, 30),
        location: '강남역 맛집',
      ),
    ]);
    notifyListeners();
  }

  // 오늘 일정 가져오기
  List<Schedule> getTodaySchedules() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final tomorrow = today.add(const Duration(days: 1));

    return _schedules
        .where((schedule) =>
            schedule.dateTime.isAfter(today) &&
            schedule.dateTime.isBefore(tomorrow))
        .toList()
      ..sort((a, b) => a.dateTime.compareTo(b.dateTime));
  }

  // 특정 날짜의 일정 가져오기
  List<Schedule> getSchedulesByDate(DateTime date) {
    final startOfDay = DateTime(date.year, date.month, date.day);
    final endOfDay = startOfDay.add(const Duration(days: 1));

    return _schedules
        .where((schedule) =>
            schedule.dateTime.isAfter(startOfDay) &&
            schedule.dateTime.isBefore(endOfDay))
        .toList()
      ..sort((a, b) => a.dateTime.compareTo(b.dateTime));
  }

  // 일정 추가
  Future<void> addSchedule(Schedule schedule) async {
    _schedules.add(schedule);
    notifyListeners();
  }

  // 일정 수정
  Future<void> updateSchedule(Schedule schedule) async {
    final index = _schedules.indexWhere((s) => s.id == schedule.id);
    if (index != -1) {
      _schedules[index] = schedule;
      notifyListeners();
    }
  }

  // 일정 삭제
  Future<void> deleteSchedule(String id) async {
    _schedules.removeWhere((schedule) => schedule.id == id);
    notifyListeners();
  }

  // 일정 완료 토글
  Future<void> toggleComplete(String id) async {
    final schedule = _schedules.firstWhere((s) => s.id == id);
    schedule.isCompleted = !schedule.isCompleted;
    notifyListeners();
  }

  // 새 일정 생성 헬퍼
  Schedule createSchedule({
    required String title,
    required DateTime dateTime,
    String? location,
    String? memo,
    List<String>? tags,
  }) {
    return Schedule(
      id: _uuid.v4(),
      title: title,
      dateTime: dateTime,
      location: location,
      memo: memo,
      tags: tags ?? [],
    );
  }
}
