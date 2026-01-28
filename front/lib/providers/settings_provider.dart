import 'package:flutter/material.dart';

class SettingsProvider extends ChangeNotifier {
  TimeOfDay _morningNotificationTime = const TimeOfDay(hour: 8, minute: 0);
  TimeOfDay _nightNotificationTime = const TimeOfDay(hour: 23, minute: 0);
  String _speakingStyle = 'ㅈ같음'; // 유저의 원래 선호도 복구
  String _characterTheme = '기본';
  int _reminderMinutes = 10;

  final List<String> availableStyles = ['친절함', 'ㅈ같음', '츤데레', '조용함'];

  TimeOfDay get morningNotificationTime => _morningNotificationTime;
  TimeOfDay get nightNotificationTime => _nightNotificationTime;
  String get speakingStyle => _speakingStyle;
  String get characterTheme => _characterTheme;
  int get reminderMinutes => _reminderMinutes;

  void updateMorningTime(TimeOfDay newTime) {
    _morningNotificationTime = newTime;
    notifyListeners();
  }

  void updateNightTime(TimeOfDay newTime) {
    _nightNotificationTime = newTime;
    notifyListeners();
  }

  void updateSpeakingStyle(String style) {
    _speakingStyle = style;
    notifyListeners();
  }

  void updateCharacterTheme(String theme) {
    _characterTheme = theme;
    notifyListeners();
  }

  void updateReminderMinutes(int minutes) {
    _reminderMinutes = minutes;
    notifyListeners();
  }

  String formatTime(BuildContext context, TimeOfDay time) {
    final hour = time.hourOfPeriod == 0 ? 12 : time.hourOfPeriod;
    final period = time.period == DayPeriod.am ? '오전' : '오후';
    final minute = time.minute.toString().padLeft(2, '0');
    return '$period $hour:$minute';
  }
}
