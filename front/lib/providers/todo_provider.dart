import 'package:flutter/foundation.dart';
import 'package:uuid/uuid.dart';
import '../models/todo.dart';

class TodoProvider extends ChangeNotifier {
  final List<Todo> _todos = [];
  final Uuid _uuid = const Uuid();

  List<Todo> get allTodos => List.unmodifiable(_todos);

  // 샘플 데이터 넣음 (UI 테스트용)
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

  // 오늘 할 일 가져오기
  List<Todo> getTodayTodos() {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);

    return _todos
        .where((todo) =>
            !todo.isCompleted &&
            (todo.dueDate == null ||
                todo.dueDate!.isAfter(today.subtract(const Duration(days: 1)))))
        .toList()
      ..sort((a, b) => a.priority.compareTo(b.priority));
  }

  // 완료되지 않은 할 일
  List<Todo> getIncompleteTodos() {
    return _todos.where((todo) => !todo.isCompleted).toList()
      ..sort((a, b) => a.priority.compareTo(b.priority));
  }

  // 완료된 할 일
  List<Todo> getCompletedTodos() {
    return _todos.where((todo) => todo.isCompleted).toList()
      ..sort((a, b) => b.createdAt.compareTo(a.createdAt));
  }

  // 할 일 추가
  Future<void> addTodo(Todo todo) async {
    _todos.add(todo);
    notifyListeners();
  }

  // 할 일 수정
  Future<void> updateTodo(Todo todo) async {
    final index = _todos.indexWhere((t) => t.id == todo.id);
    if (index != -1) {
      _todos[index] = todo;
      notifyListeners();
    }
  }

  // 할 일 삭제
  Future<void> deleteTodo(String id) async {
    _todos.removeWhere((todo) => todo.id == id);
    notifyListeners();
  }

  // 할 일 완료 토글
  Future<void> toggleComplete(String id) async {
    final todo = _todos.firstWhere((t) => t.id == id);
    todo.isCompleted = !todo.isCompleted;
    notifyListeners();
  }

  // 새 할 일 생성 헬퍼
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
