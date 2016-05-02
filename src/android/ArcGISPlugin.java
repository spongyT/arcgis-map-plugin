package com.spongyt.cordova.plugins.arcgis;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapOptions;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnLongPressListener;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.tsystems.hybridapptest.R;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import static com.esri.core.geometry.SpatialReference.WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE;

public class ArcGISPlugin extends CordovaPlugin {

    private final static String TAG = "ArcGISPlugin";

    private final static String URL_STREET_MAP = "http://server.arcgisonline.com/arcgis/rest/services/ESRI_StreetMap_World_2D/MapServer";

    private Activity activity;
    private Context context;
    private GraphicsLayer graphicsLayer;

    private ViewGroup root;
    private MapView mapView;

    private HashMap<String, Method> methods;

    final SpatialReference mapViewSpatialReference = SpatialReference.create(WKID_WGS84_WEB_MERCATOR_AUXILIARY_SPHERE);
    final SpatialReference wgs84SpatialReference = SpatialReference.create(SpatialReference.WKID_WGS84);

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        activity = cordova.getActivity();
        context = activity;
    }

    @Override
    public boolean execute(final String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, String.format("Action: %s, args: %s", action, args));

        if (methods == null) {
            methods = new HashMap<String, Method>();

            Method[] classMethods = ArcGISPlugin.this.getClass().getDeclaredMethods();
            Class<?>[] parameterTypes = null;
            for (Method method : classMethods) {
                parameterTypes = method.getParameterTypes();

                boolean parameterTypesMatch = parameterTypes.length >= 2 && parameterTypes[0].equals(JSONArray.class) && parameterTypes[1].equals(CallbackContext.class);
                if (!method.isAccessible())
                    method.setAccessible(true);

                if (parameterTypesMatch) {
                    methods.put(method.getName(), method);
                }
            }
        }

        if (methods.containsKey(action)) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        methods.get(action).invoke(ArcGISPlugin.this, args, callbackContext);
                    } catch (InvocationTargetException e) {
                        Log.w(TAG, "Error while executing plugin method for action " + action, e);
                        callbackContext.error(String.format("Error performing action: %s. Reason : %s", action, e.getLocalizedMessage()));
                    } catch (Exception e) {
                        Log.w(TAG, "Error while executing plugin method for action " + action, e);
                        callbackContext.error("'" + action + "' is not defined in ArcGIS plugin.");
                    }
                }
            });
            return true;
        } else {
            return false;
        }
//
//
//        if ("getMap".equals(action)) {
//            getMap(args, callbackContext);
//            return true;
//        }
//
//        if ("zoomToScale".equals(action)) {
//            zoomToResolution(args, callbackContext);
//            return true;
//        }
//
//        if ("zoomToResolution".equals(action)) {
//            zoomToScale(args, callbackContext);
//            return true;
//        }
//
//        if ("addMarker".equals(action)) {
//            addMarker(args, callbackContext);
//            return true;
//        }
//
//        return false;
    }

    private void showMap(JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (mapView == null) {
            mapView = new MapView(context);
            MapOptions mapOptions = new MapOptions(MapOptions.MapType.STREETS);
            mapOptions.setZoom(16);
            mapView.setMapOptions(mapOptions);

            mapView.addLayer(new ArcGISTiledMapServiceLayer(URL_STREET_MAP));
            graphicsLayer = new GraphicsLayer();
            mapView.addLayer(graphicsLayer);

            mapView.setOnLongPressListener(longPressListener);
            mapView.setOnSingleTapListener(singleTabListener);
            mapView.getLocationDisplayManager().setLocationListener(locationListener);

            root = (ViewGroup) activity.findViewById(R.id.fl_map);

            mapView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, (int) (250 * context.getResources().getDisplayMetrics().density)));
            root.addView(mapView);
            mapView.unpause();

            callbackContext.success();
        } else {
            callbackContext.success();
        }
    }

    private void hideMap(JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (mapView != null) {
            mapView.pause();
            root.removeView(mapView);

            mapView = null;
            graphicsLayer = null;
            root = null;

            callbackContext.success();
        } else {
            callbackContext.success();
        }
    }

    private void zoomToResolution(JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONArray latLong = args.getJSONArray(0);
        Point centerPoint = getPointFromParam(latLong);
        if (centerPoint == null)
            throw new JSONException("Center point have to be given as first parameter in an double array (e.g. [123.14222 ,-21.09134])");

        double resolution = args.getDouble(1);

        if (mapView != null) {
            mapView.zoomToResolution(centerPoint, resolution);
        } else {
            throw new JSONException("Map view not yet initialized");
        }
    }

    private void zoomToScale(JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONArray latLong = args.getJSONArray(0);
        Point centerPoint = getPointFromParam(latLong);
        if (centerPoint == null)
            throw new JSONException("Center point have to be given as first parameter in an double array (e.g. [123.14222 ,-21.09134])");

        double scale = args.getDouble(1);

        if (mapView != null) {
            mapView.zoomToScale(centerPoint, scale);
        } else {
            throw new JSONException("Map view not yet initialized");
        }
    }

    private void addMarker(JSONArray args, CallbackContext callbackContext) throws JSONException {
        JSONArray latLong = args.getJSONArray(0);
        Point centerPoint = getPointFromParam(latLong);
        if (centerPoint == null)
            throw new JSONException("Marker point have to be given as first parameter in an double array (e.g. [123.14222 ,-21.09134])");

        if (graphicsLayer != null) {
            SimpleMarkerSymbol markerSymbol = new SimpleMarkerSymbol(Color.RED, 15, SimpleMarkerSymbol.STYLE.CIRCLE);
            Graphic markerGraphic = new Graphic(centerPoint, markerSymbol);
            int markerId = graphicsLayer.addGraphic(markerGraphic);
            callbackContext.success(markerId);
        } else {
            throw new JSONException("Map view not yet initialized");
        }
    }

    private void removeAll(JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (graphicsLayer != null) {
            graphicsLayer.removeAll();
            callbackContext.success();
        } else {
            throw new JSONException("Map view not yet initialized");
        }
    }


    private void remove(JSONArray args, CallbackContext callbackContext) throws JSONException {
        int[] ids = new int[args.length()];
        for (int i = 0; i < args.length(); i++) {
            ids[i] = args.getInt(i);
        }

        if (graphicsLayer != null) {
            graphicsLayer.removeGraphics(ids);
            callbackContext.success();
        } else {
            throw new JSONException("Map view not yet initialized");
        }
    }

    private void startLocationDisplayManager(JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (mapView != null) {
            mapView.getLocationDisplayManager().start();
            callbackContext.success();
        } else {
            throw new JSONException("Map view not yet initialized");
        }
    }

    private void stopLocationDisplayManager(JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (mapView != null) {
            mapView.getLocationDisplayManager().stop();
            callbackContext.success();
        } else {
            throw new JSONException("Map view not yet initialized");
        }
    }

    //--------------------------------------------------------------------------
    // LOCAL METHODS
    //--------------------------------------------------------------------------


    private static Point getPointFromParam(JSONArray param) throws JSONException {
        if (param != null) {
            if (param.length() == 2) {
                try {
                    Double longitude = param.getDouble(0);
                    Double latitude = param.getDouble(1);
                    return new Point(longitude, latitude);
                } catch (JSONException e) {
                    Log.w(TAG, e);
                }
            }
        }
        return null;
    }

    //--------------------------------------------------------------------------
    // LISTENER
    //--------------------------------------------------------------------------


    private OnLongPressListener longPressListener = new OnLongPressListener() {
        @Override
        public boolean onLongPress(float v, float v1) {
            Point pressedPoint = mapView.toMapPoint(v, v1);
            webView.loadUrl("javascript:onLongPress(" + pressedPoint.getX() + "," + pressedPoint.getY() + ")");
            return true;
        }
    };

    private OnSingleTapListener singleTabListener = new OnSingleTapListener() {
        @Override
        public void onSingleTap(float v, float v1) {
            int[] graphicIds = graphicsLayer.getGraphicIDs(v, v1, 20, 1);
            if (graphicIds.length >= 1) {
                webView.loadUrl(String.format("javascript:onGraphicSelected(%s)", graphicIds[0]));
            }
        }
    };

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            try{
                JSONObject locationJson = new JSONObject();
                locationJson.put("latitude", location.getLatitude());
                locationJson.put("longitude", location.getLongitude());
                webView.loadUrl(String.format("javascript:onLocationChanged(%s)", locationJson));
            }catch (JSONException e){
                Log.e(TAG, "Error forwarding location", e);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

}