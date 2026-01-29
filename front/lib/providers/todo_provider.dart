import 'package:flutter/foundation.dart';
import 'package:uuid/uuid.dart';
import '../models/todo.dart';

class TodoProvider extends ChangeNotifier {
  final List<Todo> _todos = [];
  final Uuid _uuid = const Uuid();

  List<Todo> get allTodos => List.unmodifiable(_todos);

  void addSampleData() {
    _todos.addAll([
      Todo(
        id: _uuid.v4(),
        title: '프로젝트 기획서 작성',
        priority: 1,
        dueDate: DateTime.now().add(const Duration(days: 1)),
        createdAt: DateTime.now(),
      ),
      Todo(
        id: _uuid.v4(),
        title: '장보기',
        priority: 2,
        createdAt: DateTime.now(),
        isCompleted: true,
      ),
      Todo(
        id: _uuid.v4(),
        title: '운동하기',
        priority: 2,
        createdAt: DateTime.now(),
      ),
      Todo(
        id: _uuid.v4(),
        title: '책 읽기',
        priority: 3,
        createdAt: DateTime.now(),
      ),
    ]);
    notifyListeners();
  }

  // 오늘 할 일 가져오기: 미완료 항목 전체 + 오늘 완료한 항목
  List<Todo> getTodayTodos() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);

    return _todos.where((todo) {
      // 1. 아직 완료되지 않은 모든 할 일
      if (!todo.isCompleted) return true;
      
      // 2. 오늘 완료 처리된 할 일 (createdAt이나 dueDate가 오늘인 경우 포함)
      final isFromToday = todo.createdAt.year == today.year &&
          todo.createdAt.month == today.month &&
          todo.createdAt.day == today.day;
          
      return isFromToday;
    }).toList()
      ..sort((a, b) {
        if (a.isCompleted != b.isCompleted) {
          return a.isCompleted ? 1 : -1;
        }
        return a.priority.compareTo(b.priority);
      });
  }

  List<Todo> getIncompleteTodos() {
    return _todos.where((todo) => !todo.isCompleted).toList()
      ..sort((a, b) => a.priority.compareTo(b.priority));
  }

  List<Todo> getCompletedTodos() {
    return _todos.where((todo) => todo.isCompleted).toList()
      ..sort((a, b) => b.createdAt.compareTo(a.createdAt));
  }

  Future<void> addTodo(Todo todo) async {
    _todos.add(todo);
    notifyListeners();
  }

  Future<void> updateTodo(Todo todo) async {
    final index = _todos.indexWhere((t) => t.id == todo.id);
    if (index != -1) {
      _todos[index] = todo;
      notifyListeners();
    }
  }

  Future<void> deleteTodo(String id) async {
    _todos.removeWhere((todo) => todo.id == id);
    notifyListeners();
  }

  Future<void> toggleComplete(String id) async {
    final index = _todos.indexWhere((t) => t.id == id);
    if (index != -1) {
      _todos[index].isCompleted = !_todos[index].isCompleted;
      notifyListeners();
    }
  }

  Todo createTodo({
    required String title,
    DateTime? dueDate,
    int priority = 2,
    String? memo,
    List<String>? tags,
  }) {
    return Todo(
      id: _uuid.v4(),
      title: title,
      dueDate: dueDate,
      priority: priority,
      memo: memo,
      tags: tags ?? [],
      createdAt: DateTime.now(),
    );
  }
}
