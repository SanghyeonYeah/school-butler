import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../models/todo.dart';
import '../providers/todo_provider.dart';

class TodoListItem extends StatefulWidget {
  final Todo todo;

  const TodoListItem({
    super.key,
    required this.todo,
  });

  @override
  State<TodoListItem> createState() => _TodoListItemState();
}

class _TodoListItemState extends State<TodoListItem> {
  double _scale = 1.0;

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<TodoProvider>(context, listen: false);

    return AnimatedScale(
      scale: _scale,
      duration: const Duration(milliseconds: 100),
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(16),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.03),
              blurRadius: 8,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(16),
          child: IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // 왼쪽 구분 띠
                Container(
                  width: 6,
                  color: widget.todo.isCompleted 
                      ? Colors.grey[300] 
                      : _getPriorityColor(widget.todo.priority),
                ),
                Expanded(
                  child: Material(
                    color: Colors.transparent,
                    child: InkWell(
                      onTapDown: (_) => setState(() => _scale = 0.97),
                      onTapUp: (_) => setState(() => _scale = 1.0),
                      onTapCancel: () => setState(() => _scale = 1.0),
                      onTap: () {
                        // TODO: 상세 화면으로 이동
                      },
                      child: Padding(
                        padding: const EdgeInsets.fromLTRB(12, 16, 8, 16),
                        child: Row(
                          children: [
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    widget.todo.title,
                                    style: TextStyle(
                                      fontSize: 17,
                                      fontWeight: FontWeight.bold,
                                      decoration: widget.todo.isCompleted
                                          ? TextDecoration.lineThrough
                                          : null,
                                      color: widget.todo.isCompleted
                                          ? Colors.grey
                                          : Colors.black87,
                                    ),
                                  ),
                                  if (widget.todo.dueDate != null) ...[
                                    const SizedBox(height: 6),
                                    Row(
                                      children: [
                                        Icon(
                                          Icons.calendar_today_rounded,
                                          size: 14,
                                          color: _isOverdue(widget.todo.dueDate!)
                                              ? Colors.redAccent
                                              : Colors.grey[500],
                                        ),
                                        const SizedBox(width: 4),
                                        Text(
                                          DateFormat('M월 d일')
                                              .format(widget.todo.dueDate!),
                                          style: TextStyle(
                                            fontSize: 13,
                                            color: _isOverdue(widget.todo.dueDate!)
                                                ? Colors.redAccent
                                                : Colors.grey[500],
                                          ),
                                        ),
                                      ],
                                    ),
                                  ],
                                ],
                              ),
                            ),
                            
                            // 무광 플랫 하트 아이콘 (크기 일관성 유지)
                            GestureDetector(
                              onTap: () {
                                provider.toggleComplete(widget.todo.id);
                              },
                              behavior: HitTestBehavior.opaque,
                              child: Container(
                                width: 48,
                                alignment: Alignment.center,
                                child: Icon(
                                  widget.todo.isCompleted 
                                      ? Icons.favorite 
                                      : Icons.favorite_outline,
                                  size: 32,
                                  color: widget.todo.isCompleted 
                                      ? Colors.red.shade400 
                                      : Colors.grey.shade300,
                                ),
                              ),
                            ),

                            PopupMenuButton(
                              icon: Icon(Icons.more_vert_rounded, color: Colors.grey[400]),
                              padding: EdgeInsets.zero,
                              constraints: const BoxConstraints(),
                              itemBuilder: (context) => [
                                const PopupMenuItem(value: 'delete', child: Text('삭제')),
                              ],
                              onSelected: (value) {
                                if (value == 'delete') _showDeleteDialog(context, provider);
                              },
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Color _getPriorityColor(int priority) {
    switch (priority) {
      case 1: return Colors.redAccent;
      case 3: return Colors.blueAccent;
      default: return Colors.orangeAccent;
    }
  }

  bool _isOverdue(DateTime dueDate) {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);
    final due = DateTime(dueDate.year, dueDate.month, dueDate.day);
    return due.isBefore(today);
  }

  void _showDeleteDialog(BuildContext context, TodoProvider provider) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('할 일 삭제'),
        content: const Text('이 할 일을 삭제하시겠습니까?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('취소')),
          TextButton(
            onPressed: () {
              provider.deleteTodo(widget.todo.id);
              Navigator.pop(context);
            },
            child: const Text('삭제', style: TextStyle(color: Colors.redAccent)),
          ),
        ],
      ),
    );
  }
}
