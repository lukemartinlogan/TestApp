
/*
* This file is used for safely running
* the MapInterface functions. MapInterface
* functions will not run properly until the
 * map has finished loading.
* */

package iitrtclab.snortingblue;

import android.widget.TextView;

import java.lang.Runnable;

public abstract class MapRunnable implements Runnable {

    int id;
    MapInterface master;
    boolean enableLocSetting = false;
    int major, minor, rssi;
    double x, y;
    TextView xView, yView;
    String path;

    public MapRunnable(int id, MapInterface master) {
        this.master = master;
        this.id = id;
    }

    public MapRunnable(boolean enableLocSetting, int id, MapInterface master) {
        this.id = id;
        this.master = master;
        this.enableLocSetting = enableLocSetting;
    }

    public MapRunnable(int major, int minor, int rssi, int id, MapInterface master) {
        this.major = major;
        this.minor = minor;
        this.id = id;
        this.master = master;
    }

    public MapRunnable(double x, double y, int id, MapInterface master) {
        this.x = x;
        this.y = y;
        this.id = id;
        this.master = master;
    }

    public MapRunnable(double x, double y, TextView xView, TextView yView, MapInterface master) {
        this.x = x;
        this.y = y;
        this.xView = xView;
        this.yView = yView;
        this.master = master;
    }

    public MapRunnable(String path, int id, MapInterface master) {
        this.path = path;
        this.id = id;
        this.master = master;
    }

    public abstract void run();
}
