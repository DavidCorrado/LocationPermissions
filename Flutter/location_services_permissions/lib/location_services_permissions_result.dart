import 'package:location_services_permissions/utils/codec.dart';

class LocationServicesPermissionsResult {
  LocationServicesPermissionsState locationServicesPermissionsState;

  LocationServicesPermissionsResult({
    this.locationServicesPermissionsState,
  });

  bool isSuccessful() {
    return locationServicesPermissionsState ==
        LocationServicesPermissionsState.LocationPermissionAllowed;
  }

  static LocationServicesPermissionsResult fromMap(dynamic message) {
    return LocationServicesPermissionsResult(
        locationServicesPermissionsState: Codec.encodeEnum(
            LocationServicesPermissionsState.values,
            message));
  }
}

enum LocationServicesPermissionsState {
  LocationServicesDisabled,
  LocationPermissionAllowed,
  LocationPermissionDenied
}
