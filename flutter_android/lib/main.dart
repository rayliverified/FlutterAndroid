import 'dart:ui';
import 'package:flutter/material.dart';

import 'package:flutter_android/page/page_main.dart';
import 'package:flutter_android/themes.dart';

void main() => runApp(_widgetForRoute(window.defaultRouteName));

Widget _widgetForRoute(String route) {
  switch (route) {
    case 'page_main':
      return MyApp();
    default:
      return Center(
        child: Text('Unknown route: $route', textDirection: TextDirection.ltr),
      );
  }
}

class MyApp extends StatelessWidget {
  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      title: 'Flutter Demo',
      theme: defaultTheme,
      home: new MainPage(),
    );
  }
}