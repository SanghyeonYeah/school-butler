import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/character_provider.dart';
import '../models/character_state.dart';
import '../widgets/character_avatar.dart';

class CharacterScreen extends StatelessWidget {
  const CharacterScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final characterProvider = Provider.of<CharacterProvider>(context);
    final state = characterProvider.state;

    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: AppBar(
        title: const Text('파트너'),
        backgroundColor: Colors.white,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            children: [
              // 캐릭터 표시
              Container(
                padding: const EdgeInsets.all(32),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  children: [
                    CharacterAvatar(
                      emotion: state.emotion,
                      size: 120,
                    ),
                    const SizedBox(height: 20),
                    Text(
                      _getEmotionText(state.emotion),
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      _getActivityText(state.activity),
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.grey[600],
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 20),

              // 메시지 영역
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(Icons.chat_bubble_outline,
                            color: Colors.blue, size: 20),
                        const SizedBox(width: 8),
                        const Text(
                          '메시지',
                          style: TextStyle(
                            fontSize: 16,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    Text(
                      state.message,
                      style: const TextStyle(
                        fontSize: 15,
                        height: 1.5,
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 20),

              // 빠른 액션 버튼들
              const Text(
                '빠른 작업',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 12,
                runSpacing: 12,
                children: [
                  _buildActionButton(
                    context,
                    '집중 시작',
                    Icons.psychology,
                    Colors.blue,
                    () {
                      characterProvider.startFocus();
                      _showSnackBar(context, '집중 모드를 시작합니다!');
                    },
                  ),
                  _buildActionButton(
                    context,
                    '휴식 하기',
                    Icons.free_breakfast,
                    Colors.green,
                    () {
                      characterProvider.startBreak();
                      _showSnackBar(context, '잠깐 쉬어가세요!');
                    },
                  ),
                  _buildActionButton(
                    context,
                    '오늘 플랜',
                    Icons.event_note,
                    Colors.orange,
                    () {
                      _showSnackBar(context, 'AI 플랜 생성 기능 준비 중입니다');
                    },
                  ),
                  _buildActionButton(
                    context,
                    '힘내기',
                    Icons.favorite,
                    Colors.red,
                    () {
                      characterProvider.setEmotion(
                        CharacterEmotion.happy,
                        message: "괜찮아! 넌 잘하고 있어. 힘내!",
                      );
                    },
                  ),
                ],
              ),

              const SizedBox(height: 20),

              // IoT 연결 상태 (준비 중)
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.grey[300]!),
                ),
                child: Row(
                  children: [
                    Icon(Icons.bluetooth_disabled, color: Colors.grey[400]),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'IoT 캐릭터',
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          Text(
                            '연결되지 않음',
                            style: TextStyle(
                              fontSize: 12,
                              color: Colors.grey[600],
                            ),
                          ),
                        ],
                      ),
                    ),
                    TextButton(
                      onPressed: () {
                        _showSnackBar(context, 'IoT 연결 기능 준비 중입니다');
                      },
                      child: const Text('연결'),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildActionButton(
    BuildContext context,
    String label,
    IconData icon,
    Color color,
    VoidCallback onPressed,
  ) {
    return ElevatedButton(
      onPressed: onPressed,
      style: ElevatedButton.styleFrom(
        backgroundColor: color,
        foregroundColor: Colors.white,
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
        ),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 20),
          const SizedBox(width: 8),
          Text(label),
        ],
      ),
    );
  }

  String _getEmotionText(CharacterEmotion emotion) {
    switch (emotion) {
      case CharacterEmotion.happy:
        return '기분 좋음 😊';
      case CharacterEmotion.proud:
        return '뿌듯함 🌟';
      case CharacterEmotion.tired:
        return '피곤함 😴';
      case CharacterEmotion.worried:
        return '걱정됨 😟';
      case CharacterEmotion.normal:
      default:
        return '평온함 😌';
    }
  }

  String _getActivityText(CharacterActivity activity) {
    switch (activity) {
      case CharacterActivity.focus:
        return '집중 중';
      case CharacterActivity.breakTime:
        return '휴식 중';
      case CharacterActivity.notify:
        return '알림';
      case CharacterActivity.idle:
      default:
        return '대기 중';
    }
  }

  void _showSnackBar(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        duration: const Duration(seconds: 2),
      ),
    );
  }
}
