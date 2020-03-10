import Flutter
import UIKit
import CoreLocation

public class SwiftLocationServicesPermissionsPlugin: NSObject, FlutterPlugin, FlutterStreamHandler, CLLocationManagerDelegate {
    private var locationServicesAndPermissionsCallback: LocationServicesAndPermissionsCallback? = nil
    private let locationManager = CLLocationManager()
    public static func register(with registrar: FlutterPluginRegistrar) {
        let eventChannel = FlutterEventChannel(name: "corradodev.com/location_services_permissions/updates", binaryMessenger: registrar.messenger())
        let instance = SwiftLocationServicesPermissionsPlugin()
        registrar.addApplicationDelegate(instance)
        eventChannel.setStreamHandler(instance)
    }

    public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        locationServicesAndPermissionsCallback = { result in
               events(result)
        }
        requestLocationServices()
        return nil
    }

    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        locationServicesAndPermissionsCallback = nil
        return nil
    }
    private func requestLocationServices() {
        locationManager.delegate = self
        if(CLLocationManager.locationServicesEnabled()) {
            let locStatus = CLLocationManager.authorizationStatus()
            switch locStatus {
                case .notDetermined:
                    locationManager.requestAlwaysAuthorization()
                    //locationManager.requestWhenInUseAuthorization()
                    //locationManager.requestLocation()
                    return
                case .denied, .restricted:
                    locationServicesAndPermissionsCallback?("LocationPermissionDenied")
                    createSettingsAlertController()
                    return
                case .authorizedAlways, .authorizedWhenInUse:
                    locationServicesAndPermissionsCallback?("LocationPermissionAllowed")
                    break
                @unknown default:
                    fatalError()
            }
        } else {
            locationServicesAndPermissionsCallback?("LocationServicesDisabled")
            createSettingsAlertController()
        }
    }

    public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if (status == CLAuthorizationStatus.denied) {
            locationServicesAndPermissionsCallback?("LocationPermissionDenied")
        } else if (status == CLAuthorizationStatus.authorizedAlways) {
            locationServicesAndPermissionsCallback?("LocationPermissionAllowed")
        }
    }
}

func getAppName() -> String{
    return Bundle.main.object(forInfoDictionaryKey: "CFBundleName") as? String ?? ""
}

func createSettingsAlertController() {
      let alertController = UIAlertController(title: "Turn On Location Services to allow \""+getAppName()+"\" to determine your location", message: nil, preferredStyle: .alert)
      let cancelAction = UIAlertAction(title: NSLocalizedString("Cancel", comment: ""), style: .cancel, handler: nil)
      let settingsAction = UIAlertAction(title: NSLocalizedString("Settings", comment: ""), style: .default) { (UIAlertAction) in
        UIApplication.shared.openURL(URL(string: UIApplication.openSettingsURLString)!)
      }
      alertController.addAction(cancelAction)
      alertController.addAction(settingsAction)
    ////[[[[UIApplication sharedInstance] delegate] window] rootViewController]?
    UIApplication.shared.delegate?.window??.rootViewController?.present(alertController, animated: true, completion: nil)
}
typealias LocationServicesAndPermissionsCallback = (String) -> Void
