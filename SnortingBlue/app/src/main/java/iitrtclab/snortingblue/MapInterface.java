
/*
* This file is used to call JavaScript functions
* using java in order to manipulate the map of a
* building. This is where you can do stuff like
* set the position of the tester on the map.
* */


/*
* We have to make sure that the operations execute
* sequentially. This is kinda hard to do with WebViews
* since onPageFinished can be called after loadUrl is
* called.
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
import java.util.concurrent.Semaphore;

public class MapInterface extends WebViewClient {

    /*CONSTANT AND VARIABLE DECLARATION*/

    Semaphore sem = new Semaphore(1);
    boolean loaded = false;
    WebView map;
    Activity context;
    double x=0, y=0;           //The location of the tester
    TextView xView, yView;


    /*CONSTRUCTORS*/

    public MapInterface(Activity context, TextView xView, TextView yView,  WebView map) {
        this.map = map;
        this.context = context;
        this.xView = xView;
        this.yView = yView;
    }


    /*METHODS*/

    /*
    * This function is executed when the map html has started loading
    * */

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        loaded = false;
    }

    /*
    * This function is executed when the map html has finished loading.
    * */

    @Override
    public void onPageFinished(WebView view, String url) {
        loaded = true;
    }


    /*
    * This function is called when the map is going to be set
    * */

    public void setMap(String path) {
        loaded = false;
        Thread t = new Thread(new MapRunnable(path, this) {
            @Override
            public void run() {
                try {
                    sem.acquire();
                } catch(Exception e) {
                    e.printStackTrace();
                    return;
                }

                master.context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        map.loadUrl(path);
                        sem.release();
                    }
                });
            }
        });

        t.start();
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
                try {
                    sem.acquire();
                } catch(Exception e) {
                    e.printStackTrace();
                    return;
                }

                master.context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        map.loadUrl("javascript:toggleSettingLocation(" + enableLocSetting + "," + "true" + ")");
                        sem.release();
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
                try {
                    sem.acquire();
                } catch(Exception e) {
                    e.printStackTrace();
                    return;
                }

                master.context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        map.loadUrl("javascript:setTestingLocation(" + x + "," + y + ")");
                        sem.release();
                    }
                });
            }
        });

        t.start();
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
                try {
                    sem.acquire();
                } catch(Exception e) {
                    e.printStackTrace();
                    return;
                }

                master.context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        map.loadUrl("javascript:renderBeaconByMajorMinor(" + major + "," + minor + "," + rssi + "," + "true" + ")");
                        sem.release();
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
                try {
                    sem.acquire();
                } catch(Exception e) {
                    e.printStackTrace();
                    return;
                }

                master.context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        map.loadUrl("javascript:removeAllBeacons()");
                        sem.release();
                    }
                });
            }
        });

        t.start();
    }





    /*JAVASCRIPT INTERFACES*/


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
