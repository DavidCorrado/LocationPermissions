import 'package:flutter/material.dart';
import 'package:location_services_permissions/location_services_permissions.dart';
import 'package:location_services_permissions/location_services_permissions_result.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  @override
  Widget build(BuildContext context) {
    return StreamBuilder<LocationServicesPermissionsResult>(
        stream:
            LocationServicesPermissions.pollLocationServicesAndPermissions(),
        builder: (BuildContext context,
            AsyncSnapshot<LocationServicesPermissionsResult> snapshot) {
          print("LocationServices" + snapshot.connectionState.toString());
          print("LocationServices" + snapshot.data.toString());
          if (!snapshot.hasData) {
            return Center(child: CircularProgressIndicator());
          }

          //if (snapshot.data == GeolocationStatus.denied) {
          return Text(snapshot.data.locationServicesPermissionsState.toString(),
              textDirection: TextDirection.ltr);
          //}

          // return _buildListView();
        });
  }
}
