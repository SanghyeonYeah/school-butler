import 'package:flutter/foundation.dart';
import '../models/character_state.dart';
import 'settings_provider.dart';

class CharacterProvider extends ChangeNotifier {
  CharacterState _state = CharacterState.defaultState;
  SettingsProvider? _settings;

  CharacterState get state => _state;

  // ProxyProvider로부터 설정 업데이트
  void updateFromSettings(SettingsProvider settings) {
    if (_settings?.speakingStyle != settings.speakingStyle) {
      _settings = settings;
      // 말투가 바뀌면 즉시 새로운 말투로 메시지 업데이트
      _state = _state.copyWith(
        message: _getDefaultMessage(_state.activity, _state.emotion),
      );
    } else {
      _settings = settings;
    }
  }

  // 상태 변경
  void updateState(CharacterState newState) {
    _state = newState;
    notifyListeners();
  }

  // 활동 변경
  void setActivity(CharacterActivity activity, {String? message}) {
    _state = _state.copyWith(
      activity: activity,
      message: message ?? _getDefaultMessage(activity, _state.emotion),
    );
    notifyListeners();
  }

  // 감정 변경
  void setEmotion(CharacterEmotion emotion, {String? message}) {
    _state = _state.copyWith(
      emotion: emotion,
      message: message ?? _getDefaultMessage(_state.activity, emotion),
    );
    notifyListeners();
  }

  // 메시지 설정
  void setMessage(String message) {
    _state = _state.copyWith(message: message);
    notifyListeners();
  }

  // 집중 모드 시작
  void startFocus() {
    final activity = CharacterActivity.focus;
    final emotion = CharacterEmotion.normal;
    _state = CharacterState(
      activity: activity,
      emotion: emotion,
      message: _getDefaultMessage(activity, emotion),
    );
    notifyListeners();
  }

  // 휴식 모드 시작
  void startBreak() {
    final activity = CharacterActivity.breakTime;
    final emotion = CharacterEmotion.happy;
    _state = CharacterState(
      activity: activity,
      emotion: emotion,
      message: _getDefaultMessage(activity, emotion),
    );
    notifyListeners();
  }

  // 기본 메시지 생성 (말투 반영)
  String _getDefaultMessage(
      CharacterActivity activity, CharacterEmotion emotion) {
    final style = _settings?.speakingStyle ?? '친절함';

    if (style == 'ㅈ같음') {
      return _getGrumpyMessage(activity, emotion);
    } else if (style == '츤데레') {
      return _getTsundereMessage(activity, emotion);
    } else {
      return _getKindMessage(activity, emotion);
    }
  }

  String _getKindMessage(CharacterActivity activity, CharacterEmotion emotion) {
    switch (activity) {
      case CharacterActivity.focus:
        return emotion == CharacterEmotion.tired
            ? "조금만 더 힘내보자! 거의 다 왔어."
            : "지금은 집중할 시간이야. 함께 파이팅!";
      case CharacterActivity.breakTime:
        return "정말 고생 많았어! 꿀 같은 휴식 시간이야.";
      case CharacterActivity.notify:
        return "띵동! 계획했던 일이 시작될 시간이야.";
      case CharacterActivity.idle:
        switch (emotion) {
          case CharacterEmotion.happy:
            return "오늘 컨디션 최고인걸? 같이 달려보자!";
          case CharacterEmotion.proud:
            return "방금 그 일, 정말 잘 해냈어! 멋지다.";
          case CharacterEmotion.tired:
            return "많이 피곤하지? 내가 곁에 있어줄게.";
          case CharacterEmotion.worried:
            return "무슨 걱정 있어? 하나씩 천천히 해보자.";
          case CharacterEmotion.normal:
            return "안녕! 오늘도 기분 좋게 시작해볼까?";
        }
    }
  }

  String _getGrumpyMessage(
      CharacterActivity activity, CharacterEmotion emotion) {
    switch (activity) {
      case CharacterActivity.focus:
        return emotion == CharacterEmotion.tired
            ? "닥치고 집중이나 해. 죽겠냐?"
            : "핸드폰 꺼라. 쳐다보지 말고 공부나 해.";
      case CharacterActivity.breakTime:
        return "와, 이걸 쉬네? 양심 어디?";
      case CharacterActivity.notify:
        return "야, 일어나. 할 일 안 해?";
      case CharacterActivity.idle:
        switch (emotion) {
          case CharacterEmotion.happy:
            return "뭐 좋은 일이라도 있냐? 재수없게..";
          case CharacterEmotion.proud:
            return "고작 그거 하고 뿌듯해하는 수준 보소.";
          case CharacterEmotion.tired:
            return "벌써 지침? 니가 그렇지 뭐.";
          case CharacterEmotion.worried:
            return "걱정할 시간에 손가락이나 움직여.";
          case CharacterEmotion.normal:
            return "뭐. 할 일이라도 생겼냐?";
        }
    }
  }

  String _getTsundereMessage(
      CharacterActivity activity, CharacterEmotion emotion) {
    switch (activity) {
      case CharacterActivity.focus:
        return "딱히 널 위해서 응원하는 건 아니니까, 열공이나 해!";
      case CharacterActivity.breakTime:
        return "흥, 너무 무리하다 쓰러지면 곤란하다고? 쉬는 김에 제대로 쉬어.";
      case CharacterActivity.notify:
        return "시간 넘었잖아! 내가 챙겨주지 않으면 암것도 못 한다니까?";
      case CharacterActivity.idle:
        switch (emotion) {
          case CharacterEmotion.happy:
            return "기분 좋아 보이네? 나 때문은 아니겠지?";
          case CharacterEmotion.tired:
            return "여, 여기 비타민.. 아, 아무것도 아냐! 쉬기나 해!";
          case CharacterEmotion.proud:
            return "흥, 제법이네. 뭐 칭찬해달라는 건 아니지?";
          case CharacterEmotion.worried:
            return "바보같이 뭘 그렇게 썩은 표정이야? 기운 내라고.";
          case CharacterEmotion.normal:
            return "할 일 있으면 빨리 말해. 바쁘니까.";
        }
    }
  }

  // 하루 시작 인사
  void morningGreeting() {
    final activity = CharacterActivity.idle;
    final emotion = CharacterEmotion.happy;
    _state = CharacterState(
      activity: activity,
      emotion: emotion,
      message: _getDefaultMessage(activity, emotion),
    );
    notifyListeners();
  }

  // 하루 마무리 인사
  void nightGreeting(int completionRate) {
    CharacterEmotion emotion;
    if (completionRate >= 80) {
      emotion = CharacterEmotion.proud;
    } else if (completionRate >= 50) {
      emotion = CharacterEmotion.happy;
    } else {
      emotion = CharacterEmotion.normal;
    }

    _state = CharacterState(
      activity: CharacterActivity.idle,
      emotion: emotion,
      message: _getDefaultMessage(CharacterActivity.idle, emotion),
    );
    notifyListeners();
  }
}
