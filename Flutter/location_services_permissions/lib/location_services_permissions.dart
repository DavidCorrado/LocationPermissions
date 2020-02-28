import 'dart:async';

import 'package:flutter/services.dart';
import 'package:location_services_permissions/utils/codec.dart';

import 'location_services_permissions_result.dart';

class LocationServicesPermissions {
  static const EventChannel _updatesChannel = const EventChannel(
      'corradodev.com/location_services_permissions/updates');

  static Stream<LocationServicesPermissionsResult>
      pollLocationServicesAndPermissions(
          {bool showDenyForeverDialog = false,
          bool showRationalDialog = true,
          String rationaleText = ""}) {
    print("pollLocationServicesAndPermissions");
    return _updatesChannel
        .receiveBroadcastStream(Codec.encodeLocationServicesPermissions(
            showRationalDialog, showDenyForeverDialog, rationaleText))
        .map<LocationServicesPermissionsResult>((dynamic element) {
      print("LocationServicesMap"+element);
            return LocationServicesPermissionsResult.fromMap(element);
        });
  }
}
