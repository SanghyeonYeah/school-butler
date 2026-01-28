import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/date_symbol_data_local.dart';
import 'providers/schedule_provider.dart';
import 'providers/todo_provider.dart';
import 'providers/character_provider.dart';
import 'providers/day_review_provider.dart';
import 'providers/settings_provider.dart';
import 'screens/main_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // 날짜 형식 지정을 위한 로케일 데이터 초기화
  await initializeDateFormatting('ko_KR', null);

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MultiProvider(
      providers: [
        ChangeNotifierProvider(
          create: (_) {
            final provider = ScheduleProvider();
            // UI 테스트를 위한 샘플 데이터 추가
            provider.addSampleData();
            return provider;
          },
        ),
        ChangeNotifierProvider(
          create: (_) {
            final provider = TodoProvider();
            // UI 테스트를 위한 샘플 데이터 추가
            provider.addSampleData();
            return provider;
          },
        ),
        ChangeNotifierProvider(
          create: (_) => SettingsProvider(),
        ),
        ChangeNotifierProxyProvider<SettingsProvider, CharacterProvider>(
          create: (_) => CharacterProvider(),
          update: (_, settings, character) =>
              character!..updateFromSettings(settings),
        ),
        ChangeNotifierProvider(
          create: (_) => DayReviewProvider(),
        ),
      ],
      child: MaterialApp(
        title: 'Routine Partner',
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(
            seedColor: Colors.indigo,
            primary: Colors.indigo,
            surface: Colors.white,
            brightness: Brightness.light,
          ),
          useMaterial3: true,
          scaffoldBackgroundColor: const Color(0xFFF8F9FE),
          appBarTheme: const AppBarTheme(
            backgroundColor: Colors.white,
            surfaceTintColor: Colors.transparent,
            elevation: 0,
            centerTitle: true,
            titleTextStyle: TextStyle(
              color: Colors.black,
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
          cardTheme: CardThemeData(
            elevation: 0,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(20),
              side: BorderSide(color: Colors.grey.withOpacity(0.1)),
            ),
            color: Colors.white,
          ),
          navigationBarTheme: NavigationBarThemeData(
            backgroundColor: Colors.white,
            indicatorColor: Colors.indigo.withOpacity(0.1),
            labelTextStyle: WidgetStateProperty.all(
              const TextStyle(fontSize: 12, fontWeight: FontWeight.w500),
            ),
          ),
        ),
        home: const MainScreen(),
        debugShowCheckedModeBanner: false,
      ),
    );
  }
}
