import 'package:flutter/material.dart';
import 'package:flutter_android/bloc/BlocProvider.dart';
import 'package:flutter_android/main.dart';

class BackButtonCustom extends BackButton {
  final Color color;

  const BackButtonCustom({Key key, this.color}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final AppBloc appBloc = BlocProvider.of<AppBloc>(context);

    assert(debugCheckHasMaterialLocalizations(context));
    return IconButton(
      icon: const BackButtonIcon(),
      color: color,
      tooltip: MaterialLocalizations.of(context).backButtonTooltip,
      onPressed: () {
        print("BackButtonCustom onPressed");
        Navigator.maybePop(context);
        appBloc.updateBack(Navigator.canPop(context));
      },
    );
  }
}
