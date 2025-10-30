package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NotificationService : Service() {
    // in order to make the required notification, a service is required
    // to do the job for us in the foreground process

    // create the notification builder that'll be called later on
    private lateinit var notificationBuilder: NotificationCompat.Builder

    // create a system handler which controls what thread the process is being executed on
    private lateinit var serviceHandler: Handler

    // This is used to bind a two-way communication
    // in this tutorial, we will be only be using a one-way communication
    // therefore, the return can be set to null
    override fun onBind(intent: Intent): IBinder? = null

    // this is a callback and part of the life cycle
    // the onCreate callback will be called when this service
    // is created for the first time
    override fun onCreate() {
        super.onCreate()

        // create the notification with all of its contents and configurations
        // in the startForegroundService() custom function
        notificationBuilder = startForegroundService()

        // create the handler to control which thread the
        // notification will be executed on.
        // 'HandlerThread' provides the different thread for the process to be executed on,
        // while on the other hand, 'Handler' enqueues the process to HandlerThread to be executed.
        // Here, we're instantiating a new HandlerThread called "SecondThread"
        // then we pass that HandlerThread into the main Handler called serviceHandler
        val handlerThread = HandlerThread("SecondThread")
            .apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    // create the notification with all of its contents and configurations all set up
    private fun startForegroundService(): NotificationCompat.Builder {
        // create a pending Intent which is used to be executed
        // when the user clicks the notification
        // a pending Intent is the same as a regular Intent,
        // the difference is that pending Intent will be
        // executed "Later On" and not "Immediately"
        val pendingIntent = getPendingIntent()

        // to make a notification, you should know the keyword 'channel'
        // notification uses channels that'll be used to
        // set up the required configurations
        val channelId = createNotificationChannel()

        // combine both the pending Intent and the channel
        // into a notification builder
        // remember that getNotificationBuilder() is not a built-in function!
        val notificationBuilder = getNotificationBuilder(
            pendingIntent, channelId
        )

        // after all has been set and the notification builder is ready,
        // start the foreground service and the notification
        // will appear on the user's device
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    // a pending Intent is the Intent used to be executed
    // when the user clicks the notification
    private fun getPendingIntent(): PendingIntent {
        // in order to create a pending Intent, a Flag is needed
        // a flag basically controls whether the Intent can be modified or not later on
        // unfortunately Flag exists only for the API 31 and above,
        // therefore we need to check for the SDK version of the device first
        // "Build.VERSION_CODE.S" stands for 'S' which is the API 31 release name
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE else 0

        // here, we're setting MainActivity into the pending Intent
        // When the user clicks the notification, they will be
        // redirected to the Main Activity of the app
        return PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), flag
        )
    }

    // to make a notification, a channel is required to
    // set up the required configurations
    // a notification channel includes a couple of attributes:
    // channel id, channel name, and the channel priority
    private fun createNotificationChannel(): String =
    // unfortunately notification channel exists only for API 26 and above,
    // therefore we need to check for the SDK version of the device.
        // "Build.VERSION_CODES.O" stands for "Oreo" which is the API release name
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // create the channel id
            val channelId = "001"
            // create the channel name
            val channelName = "001 channel"
            // create the channel priority
            // there are 3 common types of priority:
            // IMPORTANCE_HIGH - makes a sound, vibrates, appears as heads-up notification
            // IMPORTANCE_DEFAULT - makes a sound but doesn't appear as heads-up notification
            // IMPORTANCE_LOW - silent and doesn't appear as heads-up notification
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

            // Build the channel notification based on all 3 previous attributes
            val channel = NotificationChannel(
                channelId,
                channelName,
                channelPriority
            )

            // get the NotificationManager class
            val service = requireNotNull(
                ContextCompat.getSystemService(
                    this,
                    NotificationManager::class.java
                )
            )
            // binds the channel into the NotificationManager
            // NotificationManager will trigger the notification later on
            service.createNotificationChannel(channel)


            // return the channel id
            channelId
        } else {
            ""
        }

    // build the notification with all of its contents and configurations
    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId: String) =
        NotificationCompat.Builder(this, channelId)
            // Sets the title
            .setContentTitle("Second worker process is done")
            // Sets the content
            .setContentText("Check it out!")
            // Sets the notification icon
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            // Sets the action/intent to be executed when the user clicks the notification
            .setContentIntent(pendingIntent)
            // Sets the ticker message (brief message on top of your device)
            .setTicker("Second worker process is done, check it out!")
            // setOnGoing() controls whether the notification is dismissable or not by the user
            // if true, the notification is not dismissable and can only be closed by the app
            .setOnlyAlertOnce(true)

    // this is a callback and aprt of a life cycle
    // this callback will be called when the service is started
    // in this case, after the startForeground() method is called
    // in your startForegroundService() custom function
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)

        // gets the channel id passed from the MainActivity through the Intent
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        // posts the notification task to the handler,
        // which will be executed on a different thread
        serviceHandler.post {
            // Sets up what happens after the notification is posted
            // Here, we're counting down from 10 to 0 in the notification
            countDownFromTenToZero(notificationBuilder)
            // Here we're notifying the MainActivity that the service process is done
            // by returning the channel ID through LiveData
            notifyCompletion(Id)
            // Stops the foreground service, which closes the notification
            // but the service still goes on
            stopForeground(STOP_FOREGROUND_REMOVE)
            // stop and destroy the service
            stopSelf()

        }
        return returnValue
    }

    // a function to update the notification to display a count down from 10 to 0
    private fun countDownFromTenToZero(notificationBuilder: NotificationCompat.Builder) {
        // gets the notification manager
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // count down from 10 to 0
        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            // upadtes the notification content text
            notificationBuilder.setContentText("$i seconds until last warning")
                .setSilent(true)
            // notify the notification manager about the content update
            notificationManager.notify(
                NOTIFICATION_ID,
                notificationBuilder.build()
            )
        }
    }

    // update the LiveData with the returned channel id through the Main Thread
    // the Main Thread is identified by calling the "getMainLooper()" method
    // This function is called after the count down has completed
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"

        // This is LiveData which is a data holder that automatically
        // updates the UI based on what is observed
        // It'll return the channel ID into the LiveData after
        // the countdown has reached 0, giving a sign that
        // the service process is done
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}