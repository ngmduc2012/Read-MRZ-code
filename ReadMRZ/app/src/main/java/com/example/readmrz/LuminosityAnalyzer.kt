package com.example.readmrz

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.*
import com.example.readmrz.MainActivity

class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

    //    var listTextArray: ArrayList<String> = arrayListOf("")
    var line1Result: String = ""
    var line2Result: String = ""
    var line3Result: String = ""
    var textMRZResult: String = ""

    @SuppressLint("UnsafeExperimentalUsageError")
    fun processImage(imageProxy: ImageProxy) {

        val image = FirebaseVisionImage.fromBitmap(BitmapUtils.getBitmap(imageProxy)!!)
        val detector = FirebaseVision.getInstance().onDeviceTextRecognizer
        detector.processImage(image)
            .addOnSuccessListener { firebaseVisionText ->
//                Log.d("ok", firebaseVisionText.text)
                processResultText(firebaseVisionText)
            }
            .addOnFailureListener {
                Log.d("ok", "Failed")
            }.addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun findTextMRZ(text: String): String {
        var line1: String = text.substring(0, 30)
        var line2: String = text.substring(30, 60)
        var line3: String = text.substring(60, 90)

        val pattern1 = "I[A-Z0-9]+<{1,2}[0-9]{1}".toRegex()
        val pattern2 = "[A-Z0-9]{2,30}+<{2,20}[0-9]{1,2}".toRegex()
        val pattern3 = "\\w+<<(\\w+<)+<{3,15}".toRegex()

        if (pattern1.matches(line1)) {
            line1Result = line1
        }
        if (pattern2.matches(line2)) {
            line2Result = line2
        }
        if (pattern3.matches(line3)) {
            line3Result = line3
        }
        return line1Result + line2Result + line3Result

    }

    private fun processResultText(resultText: FirebaseVisionText) {
        if (resultText.textBlocks.size == 0) {
            return
        }

        for (block in resultText.textBlocks) {
            var textIndex = block.text
            if (textIndex.length >= 90) {
                textIndex = textIndex.replace(" ", "").trim()
                textIndex = textIndex.replace("\n", "")
                if (textIndex.length == 90) {
                    if (findTextMRZ(textIndex).length == 90) {
                        textMRZResult = findTextMRZ(textIndex)
                        Log.d("ok", findTextMRZ(textIndex))
                    }
//                if (listTextArray.size < 4) {
//                    if (findTextMRZ(textIndex).length==90) {
//                        listTextArray.add(findTextMRZ(textIndex))
//                    }
//                } else {
//                    Log.d("ok", "==========: " + compareText(listTextArray))
////                    MainActivity().text = compareText(listTextArray)
//                }
                }
            }
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(image: ImageProxy) {
        processImage(image)
//        i++
//        Log.d("ok", "$i")
//        saveImageToExternalStorage(BitmapUtils.getBitmap(image)!!)

    }

    // Method to save an image to external storage
    private fun saveImageToExternalStorage(bitmap: Bitmap) {
        // Get the external storage directory path
        val path = Environment.getExternalStorageDirectory().toString()

        // Create a file to save the image
        val file = File(path, "${UUID.randomUUID()}.jpg")

        try {
            // Get the file output stream
            val stream: OutputStream = FileOutputStream(file)

            // Compress the bitmap
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

            // Flush the output stream
            stream.flush()

            // Close the output stream
            stream.close()
        } catch (e: IOException) { // Catch the exception
            e.printStackTrace()
        }
        Log.d("no", Uri.parse(file.absolutePath).toString())
    }

    private fun compareText(list: ArrayList<String>): String {
        val b = IntArray(list.size)
        for (i in 0 until list.size) {
            for (j in 0 until i) {
                if (list[i].equals(list[j])) b[i]++
            }
        }
        var max = b[0]
        for (i in 0 until list.size) {
            if (b[i] > max) max = b[i]
        }
        for (i in 0 until list.size) {
            if (b[i] == max) return list[i]
        }
        return "Không tìm thấy MRZ"
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

}