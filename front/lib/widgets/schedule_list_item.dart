import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../models/schedule.dart';
import '../providers/schedule_provider.dart';

class ScheduleListItem extends StatefulWidget {
  final Schedule schedule;

  const ScheduleListItem({
    super.key,
    required this.schedule,
  });

  @override
  State<ScheduleListItem> createState() => _ScheduleListItemState();
}

class _ScheduleListItemState extends State<ScheduleListItem> {
  double _scale = 1.0;

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<ScheduleProvider>(context, listen: false);

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
              color: Colors.black.withValues(alpha: 0.03), // withOpacity 대신 withValues 사용
              blurRadius: 8,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(16),
          child: Container(
            decoration: BoxDecoration(
              border: Border(
                left: BorderSide(
                  width: 6,
                  color: widget.schedule.isCompleted 
                      ? Colors.grey.shade300 
                      : Colors.indigo,
                ),
              ),
            ),
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
                  padding: const EdgeInsets.fromLTRB(12, 16, 4, 16),
                  child: Row(
                    children: [
                      Expanded(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              widget.schedule.title,
                              style: TextStyle(
                                fontSize: 17,
                                fontWeight: FontWeight.bold,
                                decoration: widget.schedule.isCompleted
                                    ? TextDecoration.lineThrough
                                    : null,
                                color: widget.schedule.isCompleted
                                    ? Colors.grey
                                    : Colors.black87,
                              ),
                            ),
                            const SizedBox(height: 6),
                            Row(
                              children: [
                                Icon(
                                  Icons.access_time_rounded,
                                  size: 14,
                                  color: Colors.grey[500],
                                ),
                                const SizedBox(width: 4),
                                Text(
                                  DateFormat('HH:mm').format(widget.schedule.dateTime),
                                  style: TextStyle(
                                    fontSize: 13,
                                    color: Colors.grey[600],
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                      GestureDetector(
                        onTap: () {
                          provider.toggleComplete(widget.schedule.id);
                        },
                        behavior: HitTestBehavior.opaque,
                        child: Container(
                          width: 44, 
                          height: 44,
                          alignment: Alignment.center,
                          child: Icon(
                            widget.schedule.isCompleted 
                                ? Icons.favorite 
                                : Icons.favorite_border,
                            size: 32,
                            color: widget.schedule.isCompleted 
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
        ),
      ),
    );
  }

  void _showDeleteDialog(BuildContext context, ScheduleProvider provider) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('일정 삭제'),
        content: const Text('이 일정을 삭제하시겠습니까?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('취소')),
          TextButton(
            onPressed: () {
              provider.deleteSchedule(widget.schedule.id);
              Navigator.pop(context);
            },
            child: const Text('삭제', style: TextStyle(color: Colors.redAccent)),
          ),
        ],
      ),
    );
  }
}
