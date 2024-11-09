package com.ooplab.exercises_fitfuel

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark

class OverlayView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    @Volatile
    private var landmarks: MutableList<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>? = null

    fun setLandmarks(landmarks: MutableList<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>) {
        this.landmarks = landmarks
        postInvalidate() // Request to redraw the view
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val landmarks = landmarks ?: return

        val width = width.toFloat()
        val height = height.toFloat()

        // Draw connections
        for (connection in poseConnections) {
            val startLandmark = landmarks.getOrNull(connection.first)
            val endLandmark = landmarks.getOrNull(connection.second)

            if (startLandmark != null && endLandmark != null) {
                val startX = startLandmark.x() * width
                val startY = startLandmark.y() * height
                val endX = endLandmark.x() * width
                val endY = endLandmark.y() * height

                canvas.drawLine(startX, startY, endX, endY, connectionPaint)
            }
        }

        // Draw landmarks
        for (landmark in landmarks) {
            val x = landmark.x() * width
            val y = landmark.y() * height

            // Draw a circle at (x, y)
            canvas.drawCircle(x, y, 8f, landmarkPaint)
        }
    }

    private val landmarkPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val connectionPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    // Pose connections as per MediaPipe's specification
    private val poseConnections = listOf(
        Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 7),
        Pair(0, 4), Pair(4, 5), Pair(5, 6), Pair(6, 8),
        Pair(9, 10),
        Pair(11, 12),
        Pair(11, 13), Pair(13, 15), Pair(15, 17), Pair(15, 19), Pair(15, 21),
        Pair(17, 19), Pair(12, 14), Pair(14, 16), Pair(16, 18), Pair(16, 20), Pair(16, 22),
        Pair(18, 20),
        Pair(11, 23), Pair(12, 24),
        Pair(23, 24),
        Pair(23, 25), Pair(24, 26),
        Pair(25, 27), Pair(26, 28),
        Pair(27, 29), Pair(28, 30),
        Pair(29, 31), Pair(30, 32),
        Pair(27, 31), Pair(28, 32)
    )
}
