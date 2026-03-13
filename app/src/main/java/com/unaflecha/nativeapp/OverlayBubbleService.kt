package com.unaflecha.nativeapp

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class OverlayBubbleService : Service() {
    private var wm: WindowManager? = null
    private var bubbleView: View? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val tripId = intent?.getIntExtra("trip_id", 0) ?: 0
        if (tripId <= 0) return START_NOT_STICKY
        showBubble(
            tripId = tripId,
            origin = intent.getStringExtra("origin").orEmpty(),
            fare = intent.getDoubleExtra("fare", 0.0)
        )
        return START_NOT_STICKY
    }

    private fun showBubble(tripId: Int, origin: String, fare: Double) {
        removeBubble()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = LayoutInflater.from(this).inflate(R.layout.view_overlay_bubble, null)
        view.findViewById<TextView>(R.id.overlayTitle).text = "Nuevo viaje: $origin\n${"%.2f".format(fare)} CUP · tocar para abrir"

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 180
        }

        view.setOnClickListener {
            val openIntent = Intent(this, TripLaunchActivity::class.java).apply {
                putExtra(MainActivity.EXTRA_TARGET_URL, "${Constants.BASE_URL}/trip_map.php?trip_id=$tripId")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(openIntent)
            stopSelf()
        }

        view.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX - (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        wm?.updateViewLayout(view, params)
                        return true
                    }
                }
                return false
            }
        })

        wm?.addView(view, params)
        bubbleView = view
    }

    private fun removeBubble() {
        bubbleView?.let { runCatching { wm?.removeView(it) } }
        bubbleView = null
    }

    override fun onDestroy() {
        removeBubble()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun show(context: Context, trip: TripPayload) {
            val intent = Intent(context, OverlayBubbleService::class.java).apply {
                putExtra("trip_id", trip.id)
                putExtra("origin", trip.originText)
                putExtra("fare", trip.fareCup)
            }
            context.startService(intent)
        }
    }
}
