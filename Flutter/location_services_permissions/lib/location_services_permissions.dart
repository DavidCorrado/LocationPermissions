import 'dart:async';

import 'package:flutter/services.dart';

class LocationServicesPermissions {
  static const MethodChannel _channel =
      const MethodChannel('location_services_permissions');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }
}
