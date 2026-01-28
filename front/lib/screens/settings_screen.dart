import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/settings_provider.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  Future<void> _selectTime(
    BuildContext context,
    SettingsProvider provider,
    bool isMorning,
  ) async {
    final TimeOfDay? picked = await showTimePicker(
      context: context,
      initialTime: isMorning
          ? provider.morningNotificationTime
          : provider.nightNotificationTime,
    );
    if (picked != null) {
      if (isMorning) {
        provider.updateMorningTime(picked);
      } else {
        provider.updateNightTime(picked);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final settingsProvider = Provider.of<SettingsProvider>(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('설정'),
      ),
      body: ListView(
        padding: const EdgeInsets.symmetric(vertical: 8),
        children: [
          _buildSection(
            '알림',
            [
              _buildSettingTile(
                context,
                title: '하루 시작 알림',
                subtitle: settingsProvider.formatTime(
                    context, settingsProvider.morningNotificationTime),
                icon: Icons.notifications_active_outlined,
                onTap: () => _selectTime(context, settingsProvider, true),
              ),
              _buildSettingTile(
                context,
                title: '하루 마무리 알림',
                subtitle: settingsProvider.formatTime(
                    context, settingsProvider.nightNotificationTime),
                icon: Icons.nightlight_round,
                onTap: () => _selectTime(context, settingsProvider, false),
              ),
              _buildSettingTile(
                context,
                title: '일정 리마인더',
                subtitle: '${settingsProvider.reminderMinutes}분 전',
                icon: Icons.access_time,
                onTap: () {
                  // 간단한 바텀 시트나 다이얼로그로 선택 가능하게 구현 가능
                },
              ),
            ],
          ),
          _buildSection(
            '캐릭터',
            [
              _buildSettingTile(
                context,
                title: '캐릭터 테마',
                subtitle: settingsProvider.characterTheme,
                icon: Icons.palette_outlined,
                onTap: () {},
              ),
              _buildSettingTile(
                context,
                title: '말투 설정',
                subtitle: settingsProvider.speakingStyle,
                icon: Icons.chat_rounded,
                onTap: () {
                  final currentIndex = settingsProvider.availableStyles
                      .indexOf(settingsProvider.speakingStyle);
                  final nextIndex = (currentIndex + 1) %
                      settingsProvider.availableStyles.length;
                  settingsProvider.updateSpeakingStyle(
                      settingsProvider.availableStyles[nextIndex]);
                },
              ),
            ],
          ),
          _buildSection(
            'IoT 연동',
            [
              _buildSettingTile(
                context,
                title: 'IoT 캐릭터 연결',
                subtitle: '연결되지 않음',
                icon: Icons.bluetooth_outlined,
                onTap: () {},
              ),
            ],
          ),
          _buildSection(
            '데이터 관리',
            [
              _buildSettingTile(
                context,
                title: '데이터 백업',
                subtitle: '방금 전 백업됨',
                icon: Icons.cloud_upload_outlined,
                onTap: () {},
              ),
              _buildSettingTile(
                context,
                title: '데이터 초기화',
                subtitle: '모든 기록을 삭제합니다',
                icon: Icons.delete_outline,
                onTap: () {},
                color: Colors.redAccent,
              ),
            ],
          ),
          const SizedBox(height: 40),
          Center(
            child: Text(
              'Version 1.0.0',
              style: TextStyle(color: Colors.grey[400], fontSize: 12),
            ),
          ),
          const SizedBox(height: 20),
        ],
      ),
    );
  }

  Widget _buildSection(String title, List<Widget> children) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(24, 24, 24, 12),
          child: Text(
            title,
            style: const TextStyle(
              fontSize: 13,
              fontWeight: FontWeight.bold,
              color: Colors.indigo,
              letterSpacing: 0.5,
            ),
          ),
        ),
        Container(
          margin: const EdgeInsets.symmetric(horizontal: 16),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20),
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(20),
            child: Column(
              children: children.asMap().entries.map((entry) {
                final index = entry.key;
                final widget = entry.value;
                return Column(
                  children: [
                    widget,
                    if (index < children.length - 1)
                      Divider(
                        height: 1,
                        indent: 56,
                        endIndent: 16,
                        color: Colors.grey.withOpacity(0.1),
                      ),
                  ],
                );
              }).toList(),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildSettingTile(
    BuildContext context, {
    required String title,
    required String subtitle,
    required IconData icon,
    required VoidCallback onTap,
    Color? color,
  }) {
    return Theme(
      data: Theme.of(context).copyWith(
        splashColor: Colors.transparent,
        highlightColor: Colors.transparent,
        hoverColor: Colors.transparent,
      ),
      child: ListTile(
        onTap: onTap,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
        leading: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: (color ?? Colors.indigo).withOpacity(0.1),
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(icon, color: color ?? Colors.indigo, size: 20),
        ),
        title: Text(
          title,
          style: TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w500,
            color: color ?? Colors.black87,
          ),
        ),
        subtitle: Text(
          subtitle,
          style: TextStyle(
            fontSize: 13,
            color: Colors.grey[600],
          ),
        ),
        trailing: const Icon(Icons.chevron_right, size: 18, color: Colors.grey),
      ),
    );
  }
}
