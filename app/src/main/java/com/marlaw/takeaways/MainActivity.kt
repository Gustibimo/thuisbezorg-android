package com.marlaw.takeaways

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.app.NotificationCompat
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import com.google.firebase.messaging.RemoteMessage
import com.loopj.android.http.AsyncHttpClient
import com.loopj.android.http.JsonHttpResponseHandler
import com.pusher.pushnotifications.PushNotificationReceivedListener
import com.pusher.pushnotifications.PushNotifications
import cz.msebera.android.httpclient.Header
import cz.msebera.android.httpclient.entity.StringEntity
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var recordAdapter: MenuItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tabLayout: TabLayout = findViewById(R.id.tab_layout)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        val adapter = OrderAdapter(supportFragmentManager)

        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })

    PushNotifications.start(getApplicationContext(), "58ccfa21-4368-440c-90f4-c4ff4bca95b8")
    }


    override fun onResume() {
        super.onResume()
        recordAdapter = MenuItemAdapter(this)
        val recordsView = findViewById<View>(R.id.records_view) as ListView
        recordsView.setAdapter(recordAdapter)

        refreshMenuItems()
        receiveNotifications()
    }

    private fun receiveNotifications() {

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("takeaway",
                "Pusher Takeaway",
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        Log.i("Notifications", "Ready to process notifications")
        PushNotifications.setOnMessageReceivedListenerForVisibleActivity(this, object :
            PushNotificationReceivedListener {
            override fun onMessageReceived(remoteMessage: RemoteMessage) {
                Log.i("Notification", remoteMessage.data.toString())

                val pending = remoteMessage.data["itemsPending"]?.toInt() ?: 0
                val started = remoteMessage.data["itemsStarted"]?.toInt() ?: 0
                val finished = remoteMessage.data["itemsFinished"]?.toInt() ?: 0

                val total = pending + started + finished

                val notification = when(remoteMessage.data["status"]) {
                    "STARTED" -> {
                        NotificationCompat.Builder(applicationContext, "takeaway")
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Your order")
                            .setContentText("Your order is being cooked \uD83D\uDC68\u200D\uD83C\uDF73")
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setProgress(total, finished, finished == 0)
                    }
                    "COOKED" -> {
                        NotificationCompat.Builder(applicationContext, "takeaway")
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Your order")
                            .setContentText("Your order is ready \uD83E\uDD61")
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setProgress(total, total, false)
                    }
                    "OUT_FOR_DELIVERY" -> {
                        NotificationCompat.Builder(applicationContext, "takeaway")
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Your order")
                            .setContentText("Your order is out for delivery \uD83C\uDFC3\u200D♂️")
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    }
                    "DELIVERED" -> {
                        NotificationCompat.Builder(applicationContext, "takeaway")
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Your order")
                            .setContentText("Your order is on the way \uD83D\uDEB4")
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    }
                    else -> null
                }

                notification?.let {
                    notificationManager.notify(0, it.build())
                }
            }
        })
    }

    private fun refreshMenuItems() {
        val client = AsyncHttpClient()
        client.get("http://192.168.0.105:8080/menu-items", object : JsonHttpResponseHandler() {
            override fun onSuccess(statusCode: Int, headers: Array<out Header>, response: JSONArray) {
                super.onSuccess(statusCode, headers, response)
                runOnUiThread {
                    val menuItems = IntRange(0, response.length() - 1)
                        .map { index -> response.getJSONObject(index) }
                        .map { obj ->
                            MenuItem(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                price = obj.getString("price")
                            )

                        }

                    recordAdapter.records = menuItems
                }
            }
        })
    }

    fun placeOrder(view: View) {
        val progress = ProgressDialog(this)
        progress.setMessage("Loading...")
        progress.setCancelable(false)
        progress.show()
        Handler().postDelayed({progress.dismiss()}, 3000)
        val items = recordAdapter.order
        if (items.isEmpty()) {
            val snackbar = Snackbar.make(view, "No items selected", Snackbar.LENGTH_LONG).setAction("Action",null)
            val snackbarview = snackbar.view
            snackbarview.setBackgroundColor(Color.rgb(255,100,0))
            snackbar.show()
//            Toast.makeText(this, "No items selected", Toast.LENGTH_LONG)
//                .show()
        } else {

            val request = JSONArray(items)

            val client = AsyncHttpClient()
            client.post(applicationContext, "http://192.168.0.105:8080/orders", StringEntity(request.toString()),
                "application/json", object : JsonHttpResponseHandler() {

                    override fun onSuccess(statusCode: Int, headers: Array<out Header>, response: JSONObject) {
                        val id = response.getString("id")
                        PushNotifications.subscribe(id)

                        runOnUiThread {
//                            Toast.makeText(this@MainActivity, "Order placed", Toast.LENGTH_LONG)
//                                .show()

                            val snackbar = Snackbar.make(view, "Order placed", Snackbar.LENGTH_LONG).setAction("Action",null)
                            val snackbarview = snackbar.view
                            snackbarview.setBackgroundColor(Color.rgb(255,152,0))
                            snackbar.show()
                        }
                    }
                });
        }
    }
}

