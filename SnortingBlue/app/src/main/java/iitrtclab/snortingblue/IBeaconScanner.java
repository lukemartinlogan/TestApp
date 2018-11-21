
/*
* This class will detect beacons and gateways using
* the altbeacon library. It will initiate a scan and then
* end it after "scan_period" seconds has elapsed. The results
* of the scan will be automatically loaded into the
* BOSSA platform after the scan has completed
* successfully.
* */

package iitrtclab.snortingblue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.json.JSONObject;


public class IBeaconScanner extends TimerTask implements BeaconConsumer {

    /*CONSTANT and VARIABLE DECLARATIONS*/

    //Graphics information
    private Context context = null;
    private ProgressBar progressBar = null;
    private MapInterface mapInterface = null;

    //Beacon scanning information
    public  final static String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    private final static String targetUUID = "fda50693-a4e2-4fb1-afcf-c6eb07647825";
    private final static String gateUUID = "A0DF207C-142F-4A39-A457-6FC44D524C04";
    private BeaconManager beaconManager;

    //Scan timing information
    private Timer timer = null;         //The timer that will scan for BLE signals
    private double period = .5;         //How many seconds until timer rescans for signals
    private double current_time = 0;    //How many seconds have elapsed since timer started
    private double scan_period = 5;     //How many seconds the timer should last

    //Test case information
    private int testID;
    private double x, y;
    private int floor;
    private String building;
    private LinkedList<IBeacon> beaconList;
    String url_str = "https://api.iitrtclab.com/test";



    /*CONSTRUCTORS*/

    /*
    * This is the constructor to the bluetooth scanner.
    * It will initialize the bluetooth scanner (but not
    * start it).
    * */

    IBeaconScanner(Context context, MapInterface mapInterface, ProgressBar progressBar, double period) {
        try {
            Random rand = new Random();
            this.testID = rand.nextInt(100000);
            this.period = period;
            this.context = context;
            this.mapInterface = mapInterface;
            this.progressBar = progressBar;
            this.timer = new Timer();
            this.beaconList = new LinkedList<IBeacon>();
            this.beaconManager = BeaconManager.getInstanceForApplication(context);
            this.beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_FORMAT));
            this.beaconManager.setForegroundScanPeriod(Math.round(period*1000));
            progressBar.setMax(100);
        }
        catch(Exception e) {
            ((Activity)context).finish();
        }
    }




    /*METHODS*/

    /*
    * This function starts the bluetooth scanner. It will scan
    * for all of the beacons surrounding you for a total
    * of "scan_period" seconds. It will check for devices
    * every "period" seconds.
    * */

    public void start(String building, int floor, double x, double y, double scan_period) {
        try {
            this.scan_period = scan_period;
            this.building = building;
            this.floor = floor;
            this.x = x;
            this.y = y;
            this.current_time = 0;
            this.beaconManager.bind(this);
            this.timer.schedule(this, 0, Math.round(period * 1000));
        }
        catch(Exception e) {
            stop();
        }
    }


    /*
    * This function stops the scan of bluetooth devices.
    * */

    public void stop() {
        timer.cancel();
        timer.purge();
        beaconManager.unbind(this);
    }


    /*
    * This function ends the activity that
    * called the scanner
    * */

    public void finish() {
        ((Activity)context).finish();
    }


    /*
     * This function will be called every "period" seconds
     * due to the Timer. During the scan, it will increment
     * the progress bar. After "scan_period" seconds, this
     * function will upload a record of the test data to the
     * database.
     * */

    @Override
    public void run() {
        if(current_time >= scan_period) {
            stop();
            //String errors = uploadRecord();
            String errors = "Test";
            errorDialog(errors);
            finish();
            return;
        }

        int arr[] = new int[4];
        arr[0] = 518;
        arr[1] = 588;
        arr[2] = 549;
        arr[3] = 584;
        Random rand = new Random();

        mapInterface.renderBeaconByMajorMinor(1000, arr[rand.nextInt(4)], -90);
        current_time += period;
        updateProgress();
    }


    /*
    * This function updates the progress bar during a scan
    * so that users have a sense of how much time has elapsed.
    * */

    private void updateProgress() {
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress((int)Math.round(100*current_time/scan_period));
            }
        });
    }


    /*
    * This function will upload the test case to the
    * database. It will do this by iterating over all
    * of the beacons detected during the scan and passing
    * their attributes as parameters to an HTTP POST method.
    * */
    private String uploadRecord() {

        String error_str = "";

        try {
            for(IBeacon beacon: beaconList) {
                URL url = new URL(url_str);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(getParamString(beacon).toString());
                os.flush();

                if(conn.getResponseCode() != 200) {
                    error_str += "Error " + conn.getResponseCode() + ": " + conn.getResponseMessage() + "\n";
                }
                os.close();
                conn.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
            error_str += e.getStackTrace();
        }

        return error_str;
    }


    /*
     * This function creates the parameter string
     * to POST to the server for UploadRecord.
     * */

    private JSONObject getParamString(IBeacon beacon) throws Exception {
        JSONObject params = new JSONObject();

        params.put("major", beacon.getMajor());
        params.put("minor", beacon.getMinor());
        params.put("rssi", beacon.getRssi());
        params.put("testID", "Test" + testID);
        params.put("building_id", FirstScreen.BuildingCodes.get(building));
        params.put("floor", floor);
        params.put("x", "" + x);
        params.put("y", "" + y);
        params.put("interval", scan_period);

        return params;
    }


    /*
    * This function is called if there were problems
    * uploading records to the database. It will
    * display a log of the errors that occurred in
    * a new screen.
    * */

    private void errorDialog(String errors) {
        if(errors == "")
            return;

        Intent intent = new Intent(context, ThirdScreen.class);

        //Give the inputs to the second screen
        intent.putExtra(SecondScreen.errorDialogKey, errors);

        //Launch second screen
        context.startActivity(intent);
    }



    /*CALLBACK FUNCTIONS*/


    /*
     * This function gets called upon the call to
     * beaconManager.bind(). This is what truly
     * turns on the bluetooth scanner.
     * */

    @Override
    public void onBeaconServiceConnect() {
        try {
            beaconManager.removeAllRangeNotifiers();
            beaconManager.addRangeNotifier(beaconCB);
            beaconManager.startRangingBeaconsInRegion(new Region(targetUUID, Identifier.parse(targetUUID), null, null));
            beaconManager.startRangingBeaconsInRegion(new Region(gateUUID, Identifier.parse(gateUUID),null,null));
        }
        catch (Exception e){
            e.printStackTrace();
            stop();
        }
    }


    /*
    * Some methods that need to be here for
    * the BeaconConsumer class.
    * */

    @Override
    public Context getApplicationContext() {
        return context;
    }

    @Override
    public void unbindService(ServiceConnection serviceConnection) {
        context.unbindService(serviceConnection);
    }

    @Override
    public boolean bindService(Intent intent, ServiceConnection serviceConnection, int i) {
        return context.bindService(intent,serviceConnection,i);
    }


    /*
     * This is a callback function that will be used
     * when a scan for bluetooth devices has been
     * started. Essentially, this is where we record
     * the information of nearby bluetooth beacons. It's
     * also where we display nearby beacons on the map.
     * */

    private final RangeNotifier beaconCB = new RangeNotifier() {
        @Override
        public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
            for(Beacon x:beacons){
                beaconList.add(new IBeacon(x.getRssi(), x.getId2().toInt(),x.getId3().toInt(),x.getId1().toString()));
                mapInterface.renderBeaconByMajorMinor(x.getId2().toInt(), x.getId3().toInt(), x.getRssi());
            }
        }
    };
}
