import 'package:flutter/material.dart';
import '../models/character_state.dart';

class CharacterBubble extends StatelessWidget {
  final String message;
  final CharacterEmotion emotion;

  const CharacterBubble({
    super.key,
    required this.message,
    required this.emotion,
  });

  @override
  Widget build(BuildContext context) {
    return Hero(
      tag: 'character_bubble',
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(24),
          boxShadow: [
            BoxShadow(
              color: Colors.indigo.withOpacity(0.06),
              blurRadius: 20,
              offset: const Offset(0, 8),
            ),
          ],
        ),
        child: Row(
          children: [
            // 캐릭터 아이콘 (작은 버전)
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: _getEmotionColor(emotion).withOpacity(0.12),
                shape: BoxShape.circle,
              ),
              child: Center(
                child: Text(
                  _getEmotionEmoji(emotion),
                  style: const TextStyle(fontSize: 22),
                ),
              ),
            ),
            const SizedBox(width: 16),
            // 메시지
            Expanded(
              child: Material(
                color: Colors.transparent,
                child: Text(
                  message,
                  style: const TextStyle(
                    fontSize: 15,
                    height: 1.5,
                    color: Colors.black87,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Color _getEmotionColor(CharacterEmotion emotion) {
    switch (emotion) {
      case CharacterEmotion.happy:
        return Colors.yellow;
      case CharacterEmotion.proud:
        return Colors.orange;
      case CharacterEmotion.tired:
        return Colors.grey;
      case CharacterEmotion.worried:
        return Colors.blue;
      case CharacterEmotion.normal:
      default:
        return Colors.green;
    }
  }

  String _getEmotionEmoji(CharacterEmotion emotion) {
    switch (emotion) {
      case CharacterEmotion.happy:
        return '😊';
      case CharacterEmotion.proud:
        return '🌟';
      case CharacterEmotion.tired:
        return '😴';
      case CharacterEmotion.worried:
        return '😟';
      case CharacterEmotion.normal:
      default:
        return '😌';
    }
  }
}
