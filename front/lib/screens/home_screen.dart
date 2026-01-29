import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../providers/schedule_provider.dart';
import '../providers/todo_provider.dart';
import '../providers/character_provider.dart';
import '../widgets/character_bubble.dart';
import '../widgets/schedule_list_item.dart';
import '../widgets/todo_list_item.dart';

class HomeScreen extends StatelessWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final scheduleProvider = Provider.of<ScheduleProvider>(context);
    final todoProvider = Provider.of<TodoProvider>(context);
    final characterProvider = Provider.of<CharacterProvider>(context);

    final todaySchedules = scheduleProvider.getTodaySchedules();
    final todayTodos = todoProvider.getTodayTodos();

    final completedSchedules =
        todaySchedules.where((s) => s.isCompleted).length;
    final completedTodos = todayTodos.where((t) => t.isCompleted).length;

    return Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: Column(
          children: [
            // 상단 헤더 (날짜 강조)
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 24, 20, 10),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          DateFormat('yyyy년 M월 d일').format(DateTime.now()),
                          style: const TextStyle(
                            fontSize: 28,
                            fontWeight: FontWeight.bold,
                            letterSpacing: -0.5,
                          ),
                        ),
                        Text(
                          DateFormat('EEEE', 'ko_KR').format(DateTime.now()),
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w500,
                            color: Colors.indigo[400],
                          ),
                        ),
                      ],
                    ),
                  ),
                  // 요약 정보
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    decoration: BoxDecoration(
                      color: Colors.indigo.withValues(alpha: 0.05),
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: Row(
                      children: [
                        _buildHeaderSummary('일정', completedSchedules, todaySchedules.length),
                        Container(
                          margin: const EdgeInsets.symmetric(horizontal: 12),
                          width: 1,
                          height: 24,
                          color: Colors.indigo.withValues(alpha: 0.2),
                        ),
                        _buildHeaderSummary('할 일', completedTodos, todayTodos.length),
                      ],
                    ),
                  ),
                ],
              ),
            ),

            Expanded(
              child: CustomScrollView(
                slivers: [
                  const SliverToBoxAdapter(child: SizedBox(height: 10)),

                  // 오늘의 일정 섹션
                  _buildSectionHeader(context, '오늘 일정', () {
                    // TODO: 일정 추가 화면 이동
                  }),

                  todaySchedules.isEmpty
                      ? _buildEmptyState('오늘 일정이 없습니다')
                      : SliverPadding(
                          padding: const EdgeInsets.symmetric(horizontal: 20),
                          sliver: SliverList(
                            delegate: SliverChildBuilderDelegate(
                              (context, index) => ScheduleListItem(
                                schedule: todaySchedules[index],
                              ),
                              childCount: todaySchedules.length,
                            ),
                          ),
                        ),

                  const SliverToBoxAdapter(child: SizedBox(height: 20)),

                  // 오늘의 할 일 섹션
                  _buildSectionHeader(context, '할 일', () {
                    // TODO: 할 일 추가 화면 이동
                  }),

                  todayTodos.isEmpty
                      ? _buildEmptyState('오늘 할 일이 없습니다')
                      : SliverPadding(
                          padding: const EdgeInsets.symmetric(horizontal: 20),
                          sliver: SliverList(
                            delegate: SliverChildBuilderDelegate(
                              (context, index) => TodoListItem(todo: todayTodos[index]),
                              childCount: todayTodos.length,
                            ),
                          ),
                        ),

                  const SliverToBoxAdapter(child: SizedBox(height: 120)),
                ],
              ),
            ),
            
            // 하단 캐릭터 말풍선 영역
            Container(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 20),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    Colors.white.withValues(alpha: 0),
                    Colors.white,
                  ],
                ),
              ),
              child: CharacterBubble(
                message: characterProvider.state.message,
                emotion: characterProvider.state.emotion,
              ),
            ),
          ],
        ),
      ),
      // 오른쪽 아래 플로팅 버튼(하루 마무리) 삭제됨
    );
  }

  Widget _buildHeaderSummary(String label, int completed, int total) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.center,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          label,
          style: TextStyle(fontSize: 11, color: Colors.grey[600]),
        ),
        Text(
          '$completed/$total',
          style: const TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: Colors.indigo,
          ),
        ),
      ],
    );
  }

  Widget _buildSectionHeader(BuildContext context, String title, VoidCallback onAdd) {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 10, 10, 10),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              title,
              style: const TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
              ),
            ),
            IconButton(
              onPressed: onAdd,
              icon: const Icon(Icons.add_circle_outline, color: Colors.indigo),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyState(String message) {
    return SliverToBoxAdapter(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: Colors.grey[100],
            borderRadius: BorderRadius.circular(16),
          ),
          child: Center(
            child: Text(
              message,
              style: TextStyle(color: Colors.grey[500]),
            ),
          ),
        ),
      ),
    );
  }
}
