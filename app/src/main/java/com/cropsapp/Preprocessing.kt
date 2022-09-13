package com.cropsapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class Preprocessing(context: Context) {
    private val modelFile = File(context.filesDir, "scripted_best.pt")
    private val module: Module

    init {
        instance = this
        if (!modelFile.exists() || modelFile.length() == 0L) {
            val inputStream = context.assets.open("scripted_best.pt") //throws IO
            val outputStream = modelFile.outputStream()
            inputStream.copyTo(outputStream)
        }
        module = Module.load(modelFile.absolutePath)
    }

    /**
     * Runs object detection on the given image, crops the image to the detected box that is
     * the closest to the center and returns the cropped image. Returns the original image if
     * no boxes are found.
     */
    fun crop(bitmap: Bitmap): Bitmap {
        val paddedBitmap: Bitmap
        val size: Int
        if (bitmap.width > bitmap.height) {
            size = bitmap.width
            val difference = bitmap.width - bitmap.height
            paddedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.width, bitmap.config)
            Canvas(paddedBitmap).apply {
                drawARGB(0xFF, 0, 0, 0)
                drawBitmap(bitmap, 0f, difference / 2f, null)
            }
        } else {
            size = bitmap.height
            val difference = bitmap.height - bitmap.width
            paddedBitmap = Bitmap.createBitmap(bitmap.height, bitmap.height, bitmap.config)
            Canvas(paddedBitmap).apply {
                drawARGB(0xFF, 0, 0, 0)
                drawBitmap(bitmap, difference / 2f, 0f, null)
            }
        }
        val boxes = detect(paddedBitmap, size, size)
        var bestBox = floatArrayOf()
        boxes.forEach { box ->
            box.convertToCenterCoordinate()
            val center = size / 2
            if (bestBox.isEmpty() || distance(box, center) < distance(bestBox, center)) {
                bestBox = box
            }
        }
        if (bestBox.isEmpty()) {
            return bitmap
        }
        val x = (bestBox[0] - bestBox[2] / 2).toInt().coerceIn(0..size)
        val y = (bestBox[1] - bestBox[3] / 2).toInt().coerceIn(0..size)
        val width = bestBox[2].toInt()
        val height = bestBox[3].toInt()
        return Bitmap.createBitmap(paddedBitmap, x, y, width, height)
    }

    /**
     * Given box and the center point of the square image, returns the distance of the box
     * to the center
     */
    private fun distance(box: FloatArray, center: Int): Float {
        return (box[0] - center).pow(2) + (box[1] - center).pow(2)
    }

    /**
     * Scales the given bitmap to 640x640, runs the preprocessing model, returns the found boxes
     * rescaled to the given width and height and rotated if necessary. Boxes are made squares
     * for the real-time detection.
     */
    fun detect(
        bitmap: Bitmap,
        width: Int,
        height: Int,
        rotationDegrees: Float = 0f,
        makeSquare: Boolean = false
    ): Set<FloatArray> {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true)

        val input = TensorImageUtils.bitmapToFloat32Tensor(
            scaledBitmap,
            floatArrayOf(0f, 0f, 0f),
            floatArrayOf(1f, 1f, 1f)
        )
        val scores = module.forward(IValue.from(input)).toTuple()[0].toTensor()
        val boxes = nonMaxSuppression(scores.dataAsFloatArray)
        boxes.forEach {
            it.rotate(rotationDegrees)
            it.scaleBox(width.toFloat(), height.toFloat(), makeSquare)
        }
        return boxes
    }

    /**
     * Scales a box on a 640x640 image to a box on an image with the given width and height.
     * Optionally makes the box a square.
     */
    private fun FloatArray.scaleBox(width: Float, height: Float, makeSquare: Boolean) {
        this[0] *= (width / 640)
        this[1] *= (height / 640)
        this[2] *= (width / 640)
        this[3] *= (height / 640)

        if (makeSquare) {
            convertToCenterCoordinate()
            if (width > height)
                this[2] = this[3]
            else
                this[3] = this[2]
            convertToCoordinates()
        }
    }

    /**
     * Adapts the coordinates of the box if the source image was rotated by rotationDegrees
     */
    private fun FloatArray.rotate(rotationDegrees: Float) {
        val box = copyOf()
        when (rotationDegrees) {
            90f -> {
                this[0] = box[1]
                this[1] = 640 - box[2]
                this[2] = box[3]
                this[3] = 640 - box[0]
            }
            180f -> {
                this[0] = 640 - box[2]
                this[1] = 640 - box[3]
                this[2] = 640 - box[0]
                this[3] = 640 - box[1]
            }
            -90f -> {
                this[0] = 640 - box[3]
                this[1] = box[0]
                this[2] = 640 - box[1]
                this[3] = box[2]
            }
        }
    }

    private fun nonMaxSuppression(prediction: FloatArray): Set<FloatArray> {
        val list = mutableListOf<FloatArray>()

        /*
        Every row is 7 elements:
        x, y, width, height, confidence, class1(sugar beet), class2(other plant)
        We filter out rows with low confidence
        */
        for (i in 4..prediction.size step 7) {
            if (prediction[i] > CONFIDENCE_THRESHOLD) {
                list.add(prediction.copyOfRange(i - 4, i + 3))
            }
        }
        val boxes = mutableListOf<FloatArray>()
        val scores = mutableListOf<Float>()

        //only take boxes where most likely class is sugar beet
        list.filter { array -> array[5] >= array[6] }.forEach {
            it.convertToCoordinates()
            boxes.add(it)
            scores.add(it[5])
        }

        //Perform non max suppression
        val newBoxes = mutableSetOf<FloatArray>()
        boxes.forEachIndexed { i, box1 ->
            var discard = false
            boxes.forEachIndexed { j, box2 ->
                if (iou(box1, box2) > IOU_THRESHOLD) {
                    if (scores[j] > scores[i])
                        discard = true
                }
            }
            if (!discard)
                newBoxes.add(box1)
        }

        return newBoxes
    }

    /**
     * Computes the ratio "intersection over union" for the given two boxes
     */
    private fun iou(box1: FloatArray, box2: FloatArray): Float {
        val intersection = box1.copyOf()

        intersection[0] = max(box1[0], box2[0])
        intersection[1] = max(box1[1], box2[1])
        intersection[2] = min(box1[2], box2[2])
        intersection[3] = min(box1[3], box2[3])

        val intersectionArea = area(intersection)
        return intersectionArea / (area(box1) + area(box2) - intersectionArea)
    }

    private fun area(box: FloatArray): Float {
        return (box[2] - box[0]) * (box[3] - box[1])
    }

    /**
     * Converts the given box from x(center), y(center), width, height
     * to x(top left), y(top left), x(bottom right), y(bottom right)
     */
    private fun FloatArray.convertToCoordinates() {
        val box = copyOf()

        this[0] -= box[2] / 2
        this[1] -= box[3] / 2
        this[2] = box[0] + box[2] / 2
        this[3] = box[1] + box[3] / 2
    }

    /**
     * Converts the given box from x(top left), y(top left), x(bottom right), y(bottom right)
     * to x(center), y(center), width, height
     */
    private fun FloatArray.convertToCenterCoordinate() {
        val box = copyOf()

        this[0] = (box[0] + box[2]) / 2
        this[1] = (box[1] + box[3]) / 2
        this[2] -= box[0]
        this[3] -= box[1]
    }

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.25
        private const val IOU_THRESHOLD = 0.45
        lateinit var instance: Preprocessing
    }
}