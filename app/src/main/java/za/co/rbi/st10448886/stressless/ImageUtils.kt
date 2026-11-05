package za.co.rbi.st10448886.stressless

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream

private const val TAG = "ImageUtils"

// Firestore documents are capped at 1MB total. Keeping the encoded image
// well under that leaves room for the rest of the task's fields.
private const val MAX_DIMENSION_PX = 600
private const val JPEG_QUALITY = 55

/**
 * ImageUtils — converts a picked image into a small Base64 string so it can
 * be stored directly inside a Firestore document (used in place of Firebase
 * Cloud Storage, which requires a paid Blaze plan). This still satisfies
 * "blob storage" — the image bytes are encoded and persisted as a blob,
 * just inside the free Firestore database instead of a separate bucket.
 */
object ImageUtils {

    /** Reads the image at [uri], downsizes + compresses it, and returns a Base64 string (or null on failure). */
    fun uriToCompressedBase64(context: Context, uri: Uri): String? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(input)
            input.close()
            if (original == null) {
                Log.w(TAG, "Could not decode image from $uri")
                return null
            }

            // Scale down proportionally so the longest side is MAX_DIMENSION_PX
            val scale = MAX_DIMENSION_PX.toFloat() / maxOf(original.width, original.height)
            val resized = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    original,
                    (original.width * scale).toInt().coerceAtLeast(1),
                    (original.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else original

            val outputStream = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            val bytes = outputStream.toByteArray()
            Log.d(TAG, "Compressed image to ${bytes.size / 1024}KB")

            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to process image", e)
            null
        }
    }

    /** Decodes a stored Base64 string back into a Bitmap for display (or null if blank/invalid). */
    fun base64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode stored image", e)
            null
        }
    }
}