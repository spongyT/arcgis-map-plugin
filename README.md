# ArcGIS Map Plugin

Phonegap plugin for native ArcGIS Map SDK´s.

### Supported platforms:
- Android
- iOS (planned)
- Windows mobile (planned)

### Version
0.1

## Functions
- Show map
```
cordova.exec(onSuccess, onError(), "ArcGIS", "showMap", []);
```

- Hide map
```
cordova.exec(onSuccess, onError(), "ArcGIS", "hideMap", []);
```

- Zoom to scale
```
cordova.exec(onSuccess, onError(), "ArcGIS", "zoomToResolution", [[position.coords.longitude, position.coords.latitude], scale]);
```

- Zoom to resolution
```
cordova.exec(onSuccess, onError(), "ArcGIS", "zoomToScale", [[position.coords.longitude, position.coords.latitude], zoomLevel]);
```

- Add marker
```
cordova.exec(function(markerId){}, onError(), "ArcGIS", "addMarker", [[position.coords.longitude, position.coords.latitude]]);
```

- Remove marker
```
cordova.exec(onSuccess, onError(), "ArcGIS", "remove", [[markerId]]);
```

- Remove all marker
```
cordova.exec(onSuccess, onError(), "ArcGIS", "removeAll", []);
```

- Start location display manager
```
cordova.exec(onSuccess, onError(), "ArcGIS", "startLocationDisplayManager", []);
```

- Stop location display manager
```
cordova.exec(onSuccess, onError(), "ArcGIS", "stopLocationDisplayManager", []);
```
