enum CharacterActivity {
  idle,
  focus,
  breakTime,
  notify,
}

enum CharacterEmotion {
  normal,
  happy,
  proud,
  tired,
  worried,
}

class CharacterState {
  final CharacterActivity activity;
  final CharacterEmotion emotion;
  final String message;

  CharacterState({
    required this.activity,
    required this.emotion,
    required this.message,
  });

  CharacterState copyWith({
    CharacterActivity? activity,
    CharacterEmotion? emotion,
    String? message,
  }) {
    return CharacterState(
      activity: activity ?? this.activity,
      emotion: emotion ?? this.emotion,
      message: message ?? this.message,
    );
  }

  // 기본 상태
  static CharacterState get defaultState => CharacterState(
        activity: CharacterActivity.idle,
        emotion: CharacterEmotion.normal,
        message: "안녕! 오늘도 함께 해볼까?",
      );
}
