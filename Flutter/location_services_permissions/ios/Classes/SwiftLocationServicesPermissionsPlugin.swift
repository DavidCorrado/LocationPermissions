import Flutter
import UIKit
import CoreLocation

public class SwiftLocationServicesPermissionsPlugin: NSObject, FlutterPlugin, FlutterStreamHandler, UIApplicationDelegate, CLLocationManagerDelegate {
    private var locationServicesAndPermissionsCallback: ((String) -> Void)? = nil
    private let locationManager = CLLocationManager()
    private var lastLocationServicesAndPermissionStatus: String? = nil
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let eventChannel = FlutterEventChannel(name: "corradodev.com/location_services_permissions/updates", binaryMessenger: registrar.messenger())
        let instance = SwiftLocationServicesPermissionsPlugin()
        registrar.addApplicationDelegate(instance)
        eventChannel.setStreamHandler(instance)
        instance.locationManager.delegate = instance
    }
    
    public func applicationDidBecomeActive(_ application: UIApplication) {
        if(locationServicesAndPermissionsCallback != nil){
            requestLocationServices()
        }
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
        if(CLLocationManager.locationServicesEnabled()) {
            let locStatus = CLLocationManager.authorizationStatus()
            switch locStatus {
                case .notDetermined:
                    locationManager.requestWhenInUseAuthorization()
                    return
                case .denied, .restricted:
                    //TODO make this an enum and constants
                    locationServicesAndPermissionsCallback?("LocationPermissionDenied")
                    createSettingsAlertController(isLocationServices: false)
                    return
                case .authorizedAlways, .authorizedWhenInUse:
                    locationServicesAndPermissionsCallback?("LocationPermissionAllowed")
                    break
                @unknown default:
                    fatalError()
            }
        } else {
            locationServicesAndPermissionsCallback?("LocationServicesDisabled")
            createSettingsAlertController(isLocationServices: true)
        }
    }

    public func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if (status == CLAuthorizationStatus.denied) {
            locationServicesAndPermissionsCallback?("LocationPermissionDenied")
        } else if (status == CLAuthorizationStatus.authorizedAlways) {
            locationServicesAndPermissionsCallback?("LocationPermissionAllowed")
        }
    }
    
    func getAppName() -> String{
        return Bundle.main.object(forInfoDictionaryKey: "CFBundleName") as? String ?? ""
    }

    func createSettingsAlertController(isLocationServices:Bool) {
        //TODO finalize text and verify settings page and steps
        let alertController = UIAlertController(title: "Location Services Required", message: Bundle.main.object(forInfoDictionaryKey: "NSLocationWhenInUseUsageDescription") as! String + "\n\nTurn on Location Services in Settings > Privacy to allow " + getAppName() + " to determine your current location", preferredStyle: .alert)
        let cancelAction = UIAlertAction(title: NSLocalizedString("Cancel", comment: ""), style: .cancel, handler: nil)
        let settingsAction = UIAlertAction(title: NSLocalizedString("Go to Settings", comment: ""), style: .default) { (UIAlertAction) in
            UIApplication.shared.openURL(URL(string: UIApplication.openSettingsURLString)!)
        }
        alertController.addAction(settingsAction)
        alertController.addAction(cancelAction)
        UIApplication.shared.delegate?.window??.rootViewController?.present(alertController, animated: true, completion: nil)
    }
}


