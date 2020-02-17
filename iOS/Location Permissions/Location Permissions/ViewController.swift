//
//  ViewController.swift
//  Location Permissions
//
//  Created by David Corrado on 2/16/20.
//  Copyright © 2020 David Corrado. All rights reserved.
//

import UIKit
import CoreLocation

class ViewController: UIViewController, CLLocationManagerDelegate {
    @IBOutlet weak var label: UILabel!
    let locationManager = CLLocationManager()
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
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
                    print("Permission Denied")
                    label.text = "Permission Denied"
                    createSettingsAlertController()
                    return
                case .authorizedAlways, .authorizedWhenInUse:
                    print("Permission Authorized")
                    label.text = "Permission Authorized"
                    break
                @unknown default:
                    fatalError()
            }
        } else {
            print("Location Services Denied")
            label.text = "Location Services Denied"
            createSettingsAlertController()
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if (status == CLAuthorizationStatus.denied) {
            print("Permission Denied")
            label.text = "Permission Denied"
        } else if (status == CLAuthorizationStatus.authorizedAlways) {
            print("Permission Authorized")
            label.text = "Permission Authorized"
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
      self.present(alertController, animated: true, completion: nil)
    }
}

