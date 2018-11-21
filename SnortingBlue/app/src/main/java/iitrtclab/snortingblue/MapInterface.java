
/*
* This file is used to call JavaScript functions
* using java in order to manipulate the map of a
* building. This is where you can do stuff like
* set the position of the tester on the map.
*
* NOTES:
*   We have to wait until a page is loaded before the JavaScript (JS)
*   apis can be called. A page only begins to load when a call to
*   setMap is made.
*
*   We have a boolean (loaded) to determine whether or not a page
*   has been loaded. Thus, we must wait for this boolean to be
*   set to true before we can execute JS functions. However, the
*   page doesn't begin to load for a little while. And on top of that,
*   a page gets loaded on the UI thread. In this implementation,
*   we call JS functions right after a call to setMap on the UI thread.
*   Thus, we would enter the JS functions before the page begins to load.
*
*   Thus, in order to wait for the page to finish loading, we have two options:
*   spawn a new thread and wait for the boolean to be set to true and call the
*   JS function in that new thread OR call the JS function in onPageFinished.
*   I chose the first option since it's more flexible with where we can call JS.
*
*   In order for this to work, we must maintain the order of operations. Any JS
*   function called before a call to setMap must be executed before we execute
*   setMap. Thus, I created a queue. Every time we call one of the methods
*   in this file (setMap, toggleSetLocation, etc), we will add it to the queue.
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
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

public class MapInterface extends WebViewClient {

    /*CONSTANT AND VARIABLE DECLARATION*/

    ReentrantLock lock = new ReentrantLock();
    LinkedList<Integer> queue = new LinkedList<Integer>();
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
        //System.out.println("PAGE STARTED!!!");
        loaded = false;
    }

    /*
    * This function is executed when the map html has finished loading.
    * */

    public void onPageFinished(WebView view, String url) {
        //System.out.println("PAGE FINISHED!!! " + url);
        loaded = true;

    }


    /*
    * This function enqueues a call to
    * one of the functions below.
    * */

    private int enqueueFunction() {
        lock.lock();
        int id = 0;
        if(queue.size() > 0)
            id = queue.getLast() + 1;
        queue.add(id);
        lock.unlock();
        return id;
    }


    /*
    * This function dequeues a function
    * */

    private void dequeueFunction(int id) {
        lock.lock();
        queue.removeFirst();
        lock.unlock();
    }


    /*
    * This function is called when the map is going to be set
    * */

    public void setMap(String path) {

        int id = enqueueFunction();
        Thread t = new Thread(new MapRunnable(path, id, this) {
            @Override
            public void run() {

                while(queue.getFirst() != id);
                loaded = false;

                master.context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //System.out.println("SET MAP!!! " + path);
                        map.loadUrl(path);
                        dequeueFunction(id);
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

        int id = enqueueFunction();

        Thread t = new Thread(new MapRunnable(val, id,this) {
            @Override
            public void run() {

                while(queue.getFirst() != id);
                while(!master.loaded);

                master.context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //System.out.println("TOGGLE SETTING LOCATION!!!");
                        map.loadUrl("javascript:toggleSettingLocation(" + enableLocSetting + "," + "true" + ")");
                        dequeueFunction(id);
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

        int id = enqueueFunction();
        //System.out.println("SET TESTING LOCATION_1!!!");
        Thread t = new Thread(new MapRunnable(x, y, id, this) {
            @Override
            public void run() {

                while(queue.getFirst() != id);
                while(!master.loaded);

                master.context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //System.out.println("SET TESTING LOCATION!!!");
                        map.loadUrl("javascript:setTestingLocation(" + x + "," + y + ")");
                        dequeueFunction(id);
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

        int id = enqueueFunction();
        Thread t = new Thread(new MapRunnable(major, minor, rssi, id, this) {
            @Override
            public void run() {

                while(queue.getFirst() != id);
                while(!master.loaded);

                master.context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        map.loadUrl("javascript:renderBeaconByMajorMinor(" + major + "," + minor + "," + rssi + "," + "true" + ")");
                        dequeueFunction(id);
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

        int id = enqueueFunction();
        Thread t = new Thread(new MapRunnable(id, this) {
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
