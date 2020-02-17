import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:location_services_permissions/location_services_permissions.dart';

void main() {
  const MethodChannel channel = MethodChannel('location_services_permissions');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await LocationServicesPermissions.platformVersion, '42');
  });
}
