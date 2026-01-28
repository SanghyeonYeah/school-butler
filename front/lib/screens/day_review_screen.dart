import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/schedule_provider.dart';
import '../providers/todo_provider.dart';
import '../providers/day_review_provider.dart';
import '../providers/character_provider.dart';

class DayReviewScreen extends StatefulWidget {
  const DayReviewScreen({super.key});

  @override
  State<DayReviewScreen> createState() => _DayReviewScreenState();
}

class _DayReviewScreenState extends State<DayReviewScreen> {
  int _rating = 3;
  String? _selectedKeyword;
  final TextEditingController _noteController = TextEditingController();

  final List<String> _keywordOptions = [
    '뿌듯한',
    '힘든',
    '평범한',
    '행복한',
    '피곤한',
    '보람찬',
    '아쉬운',
    '특별한',
  ];

  @override
  void dispose() {
    _noteController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scheduleProvider = Provider.of<ScheduleProvider>(context);
    final todoProvider = Provider.of<TodoProvider>(context);
    final reviewProvider = Provider.of<DayReviewProvider>(context);
    final characterProvider = Provider.of<CharacterProvider>(context);

    final todaySchedules = scheduleProvider.getTodaySchedules();
    final todayTodos = todoProvider.getTodayTodos();

    final completedSchedules =
        todaySchedules.where((s) => s.isCompleted).length;
    final completedTodos = todayTodos.where((t) => t.isCompleted).length;
    final totalTasks = todaySchedules.length + todayTodos.length;
    final completedTasks = completedSchedules + completedTodos;
    final completionRate =
        totalTasks > 0 ? ((completedTasks / totalTasks) * 100).round() : 0;

    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: AppBar(
        title: const Text('하루 마무리'),
        backgroundColor: Colors.white,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // 오늘 요약 카드
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '오늘 하루',
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 20),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceAround,
                      children: [
                        _buildStatItem(
                          '완료율',
                          '$completionRate%',
                          Icons.check_circle,
                          Colors.blue,
                        ),
                        _buildStatItem(
                          '일정',
                          '$completedSchedules/${todaySchedules.length}',
                          Icons.event,
                          Colors.green,
                        ),
                        _buildStatItem(
                          '할 일',
                          '$completedTodos/${todayTodos.length}',
                          Icons.task_alt,
                          Colors.orange,
                        ),
                      ],
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 24),

              // 별점 평가
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '오늘은 어땠나요?',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: List.generate(5, (index) {
                        return IconButton(
                          onPressed: () {
                            setState(() {
                              _rating = index + 1;
                            });
                          },
                          icon: Icon(
                            index < _rating ? Icons.star : Icons.star_border,
                            size: 40,
                            color: Colors.amber,
                          ),
                        );
                      }),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 20),

              // 키워드 선택
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '오늘을 표현하는 단어',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: _keywordOptions.map((keyword) {
                        final isSelected = _selectedKeyword == keyword;
                        return ChoiceChip(
                          label: Text(keyword),
                          selected: isSelected,
                          onSelected: (selected) {
                            setState(() {
                              _selectedKeyword = selected ? keyword : null;
                            });
                          },
                        );
                      }).toList(),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 20),

              // 내일에게 한 마디
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '내일에게 한 마디',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      '내일 아침에 다시 볼 수 있어요',
                      style: TextStyle(
                        fontSize: 12,
                        color: Colors.grey[600],
                      ),
                    ),
                    const SizedBox(height: 12),
                    TextField(
                      controller: _noteController,
                      maxLines: 3,
                      decoration: InputDecoration(
                        hintText: '내일의 나에게 하고 싶은 말을 적어보세요',
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                  ],
                ),
              ),

              const SizedBox(height: 24),

              // 저장 버튼
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () {
                    final review = reviewProvider.createReview(
                      date: DateTime.now(),
                      rating: _rating,
                      keyword: _selectedKeyword,
                      noteForTomorrow: _noteController.text.isNotEmpty
                          ? _noteController.text
                          : null,
                      completedSchedules: completedSchedules,
                      totalSchedules: todaySchedules.length,
                      completedTodos: completedTodos,
                      totalTodos: todayTodos.length,
                    );

                    reviewProvider.saveReview(review);
                    characterProvider.nightGreeting(completionRate);

                    Navigator.pop(context);

                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('하루 마무리가 저장되었습니다'),
                      ),
                    );
                  },
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  child: const Text(
                    '저장하기',
                    style: TextStyle(fontSize: 16),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStatItem(
    String label,
    String value,
    IconData icon,
    Color color,
  ) {
    return Column(
      children: [
        Icon(icon, color: color, size: 32),
        const SizedBox(height: 8),
        Text(
          value,
          style: const TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          label,
          style: TextStyle(
            fontSize: 12,
            color: Colors.grey[600],
          ),
        ),
      ],
    );
  }
}
