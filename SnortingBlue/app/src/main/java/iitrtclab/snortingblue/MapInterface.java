
/*
* This file is used to call JavaScript functions
* using java in order to manipulate the map of a
* building. This is where you can do stuff like
* set the position of the tester on the map.
* */

package iitrtclab.snortingblue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class MapInterface extends WebViewClient {

    /*CONSTANT AND VARIABLE DECLARATION*/

    boolean loaded = false;
    WebView map;
    Activity context;
    double x=0, y=0;           //The location of the tester
    TextView xView, yView;


    /*CONSTRUCTORS*/

    public MapInterface(Activity context, WebView map) {
        this.map = map;
        this.context = context;
    }

    public MapInterface(Activity context, TextView xView, TextView yView,  WebView map) {
        this.map = map;
        this.context = context;
        this.xView = xView;
        this.yView = yView;
    }


    /*METHODS*/

    /*
    * This function is executed when the map has started loading.
    * */

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        loaded = false;
    }

    /*
    * This function is executed when the map has finished loading.
    * */

    @Override
    public void onPageFinished(WebView view, String url) {
        loaded = true;
    }

    /*
    * This function enables and disables the ability
    * to set locations on the map
    * */

    public void toggleSettingLocation(boolean val) {
        Thread t = new Thread(new MapRunnable(val, this) {
            @Override
            public void run() {
                while(!master.loaded);
                master.context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        map.loadUrl("javascript:toggleSettingLocation(" + enableLocSetting + "," + "true" + ")");
                    }
                });
            }
        });

        t.start();
    }


    /*
    * This function removes all of the
    * beacons being displayed. This function
    * is unnecessary for now.
    * */

    public void removeAllBeacons() {
        Thread t = new Thread(new MapRunnable(this) {
            @Override
            public void run() {
                while(!master.loaded);
                master.context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        map.loadUrl("javascript:removeAllBeacons()");
                    }
                });
            }
        });

        t.start();
    }


    /*
    * This function sets the location of the
    * tester on the map. NOTE: x and y are
    * in real-world coordinates.
    * */

    public void setTestingLocation(double x, double y) {
        this.x = x;
        this.y = y;

        Thread t = new Thread(new MapRunnable(x, y, this) {
            @Override
            public void run() {
                while(!master.loaded);
                master.context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        map.loadUrl("javascript:setTestingLocation(" + x + "," + y + ")");
                    }
                });
            }
        });

        t.start();

        System.out.println("HERE" + " " + x + " " + y);
        setTestingLocationJS(x, y);
    }

    /*
    * This function will render a beacon on
    * the map
    * */

    public void renderBeaconByMajorMinor(int major, int minor, int rssi) {
        Thread t = new Thread(new MapRunnable(major, minor, rssi, this) {
            @Override
            public void run() {
                while(!master.loaded);
                master.context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        map.loadUrl("javascript:renderBeaconByMajorMinor(" + major + "," + minor + "," + rssi + "," + "true" + ")");
                    }
                });
            }
        });

        t.start();
    }


    /*
    * This function will set the location of the
    * tester in this object. NOTE: x and y
    * are real-world coordinates. This function
    * is called whenever the map is touched.
    * */

    @JavascriptInterface
    public void setTestingLocationJS(double x, double y) {
        this.x = x;
        this.y = y;

        context.runOnUiThread(new MapRunnable(x, y, xView, yView, this) {
            @Override
            public void run() {
                DecimalFormat formatter = new DecimalFormat(
                        "#.0#####",
                        DecimalFormatSymbols.getInstance( Locale.ENGLISH )
                );
                xView.setText(formatter.format(x));
                yView.setText(formatter.format(y));
            }
        });
    }

    /*
    * This function gets the location
    * of a beacon from the database.
    * To be completed...
    * */

    @JavascriptInterface
    public void getBeaconLocation(IBeacon beacon)
    {
    }
}
