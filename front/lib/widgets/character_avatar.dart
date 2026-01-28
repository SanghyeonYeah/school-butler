import 'package:flutter/material.dart';
import '../models/character_state.dart';

class CharacterAvatar extends StatefulWidget {
  final CharacterEmotion emotion;
  final double size;

  const CharacterAvatar({
    super.key,
    required this.emotion,
    this.size = 80,
  });

  @override
  State<CharacterAvatar> createState() => _CharacterAvatarState();
}

class _CharacterAvatarState extends State<CharacterAvatar>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(seconds: 3),
      vsync: this,
    )..repeat(reverse: true);

    _animation = Tween<double>(begin: 1.0, end: 1.08).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeInOut),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Hero(
      tag: 'character_avatar',
      child: ScaleTransition(
        scale: _animation,
        child: Container(
          width: widget.size,
          height: widget.size,
          decoration: BoxDecoration(
            color: _getBackgroundColor(widget.emotion),
            shape: BoxShape.circle,
            boxShadow: [
              BoxShadow(
                color: _getBackgroundColor(widget.emotion).withOpacity(0.3),
                blurRadius: 15,
                spreadRadius: 2,
              ),
            ],
          ),
          child: Center(
            child: Text(
              _getEmotionEmoji(widget.emotion),
              style: TextStyle(fontSize: widget.size * 0.5),
            ),
          ),
        ),
      ),
    );
  }

  Color _getBackgroundColor(CharacterEmotion emotion) {
    switch (emotion) {
      case CharacterEmotion.happy:
        return Colors.yellow.shade100;
      case CharacterEmotion.proud:
        return Colors.orange.shade100;
      case CharacterEmotion.tired:
        return Colors.grey.shade200;
      case CharacterEmotion.worried:
        return Colors.blue.shade100;
      case CharacterEmotion.normal:
      default:
        return Colors.green.shade100;
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
