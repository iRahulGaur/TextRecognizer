package app.web.gourav_khunger.textrecognizer

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import android.graphics.Bitmap
import android.os.Bundle
import app.web.gourav_khunger.textrecognizer.R
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.github.javiersantos.appupdater.AppUpdaterUtils.UpdateListener
import com.github.javiersantos.appupdater.objects.Update
import android.content.DialogInterface.OnShowListener
import androidx.core.content.ContextCompat
import com.github.javiersantos.appupdater.enums.AppUpdaterError
import com.muddzdev.styleabletoast.StyleableToast
import com.theartofdev.edmodo.cropper.CropImage
import android.content.pm.PackageManager
import app.web.gourav_khunger.textrecognizer.Home
import android.provider.MediaStore
import android.text.TextUtils
import android.app.ProgressDialog
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.TextRecognition
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.OnFailureListener
import android.app.Activity
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.view.Menu
import com.bumptech.glide.Glide
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.google.mlkit.vision.text.Text
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.lang.Exception

class Home : AppCompatActivity() {
    private lateinit var toolbar: Toolbar
    private var imageHolder: CardView? = null
    private var image: ImageView? = null
    private var selectButtons: LinearLayout? = null
    private var processButtons: LinearLayout? = null
    private lateinit var captureImage: Button
    private lateinit var selectFromStorage: Button
    private lateinit var clearAll: Button
    private lateinit var processImage: Button
    private lateinit var crop: Button
    var selectImageText: TextView? = null
    var bitmap: Bitmap? = null
    var preferences: SharedPreferences? = null
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        init()
    }

    override fun onStart() {
        super.onStart()
        val isDark = preferences!!.getBoolean("theme", false)
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun init() {
        val appUpdaterUtils = AppUpdaterUtils(this)
            .setUpdateFrom(UpdateFrom.GITHUB)
            .setGitHubUserAndRepo("GouravKhunger", "TextRecognizer")
            .withListener(object : UpdateListener {
                override fun onSuccess(update: Update, isUpdateAvailable: Boolean) {
                    if (isUpdateAvailable) {
                        val builder = AlertDialog.Builder(this@Home, R.style.AlertDialogTheme)
                        builder.setTitle("Wohhhooo!!!")
                            .setMessage("A new update of the app is available!!\n\nPlease Open the link and install latest APK")
                            .setCancelable(false)
                            .setPositiveButton("Open link") { dialog1: DialogInterface, id: Int ->
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/GouravKhunger/TextRecognizer/releases")
                                )
                                startActivity(browserIntent)
                                dialog1.dismiss()
                            }
                        val alert = builder.create()
                        alert.window!!.setBackgroundDrawableResource(R.drawable.round_dialog)
                        alert.setOnShowListener { arg0: DialogInterface? ->
                            alert.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setTextColor(ContextCompat.getColor(this@Home, R.color.blue))
                            alert.getButton(AlertDialog.BUTTON_NEGATIVE)
                                .setTextColor(ContextCompat.getColor(this@Home, R.color.blue))
                        }
                        alert.show()
                    }
                }

                override fun onFailed(error: AppUpdaterError) {
                    StyleableToast.Builder(this@Home)
                        .text("Error checking update!")
                        .textColor(Color.RED)
                        .backgroundColor(ContextCompat.getColor(this@Home, R.color.green))
                        .font(R.font.font)
                        .show()
                }
            })
        appUpdaterUtils.start()
        toolbar = findViewById(R.id.toolbar_home)
        toolbar.setTitle("")
        setSupportActionBar(toolbar)
        selectImageText = findViewById(R.id.selectImageText)
        imageHolder = findViewById(R.id.imageHolder)
        image = findViewById(R.id.image)
        selectButtons = findViewById(R.id.selectionButtons)
        crop = findViewById(R.id.cropImage)
        crop.setOnClickListener(View.OnClickListener { v: View? ->
            if (bitmap != null) {
                val uri = getImageUri(this, bitmap!!)
                if (uri != null) {
                    CropImage.activity(uri)
                        .start(this)
                } else {
                    StyleableToast.Builder(this)
                        .text("I'm confused :(")
                        .textColor(Color.WHITE)
                        .backgroundColor(Color.RED)
                        .font(R.font.font)
                        .show()
                }
            }
        })
        processButtons = findViewById(R.id.processButtons)
        captureImage = findViewById(R.id.captureImage)
        captureImage.setOnClickListener(View.OnClickListener { v: View? -> captureAnImage() })
        selectFromStorage = findViewById(R.id.selectFromStorage)
        selectFromStorage.setOnClickListener(View.OnClickListener { v: View? -> selectFromStorage() })
        processImage = findViewById(R.id.processImage)
        processImage.setOnClickListener(View.OnClickListener { v: View? -> processImage() })
        clearAll = findViewById(R.id.clearAll)
        clearAll.setOnClickListener(View.OnClickListener { v: View? -> hideAll() })
        hideAll()
    }

    fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                WRITE_REQUEST_CODE
            )
        } else {
            val bytes = ByteArrayOutputStream()
            inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val path = MediaStore.Images.Media.insertImage(
                inContext.contentResolver,
                inImage,
                "Image",
                null
            )
            return Uri.parse(path)
        }
        return null
    }

    private fun copyText(text: String?) {
        if (!TextUtils.isEmpty(text) && text != null) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Recognized Text", text)
            clipboard.setPrimaryClip(clip)
            StyleableToast.Builder(this)
                .text("Text Copied!")
                .textColor(Color.WHITE)
                .backgroundColor(ContextCompat.getColor(this, R.color.green))
                .font(R.font.font)
                .show()
        }
    }

    private fun selectFromStorage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(
            Intent.createChooser(
                intent,
                "Select from:"
            ),
            PICK_IMAGE_CODE
        )
    }

    private fun captureAnImage() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        } else {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (cameraIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(cameraIntent, CAPTURE_IMAGE_CODE)
            } else {
                StyleableToast.Builder(this)
                    .text("Error: ")
                    .textColor(Color.WHITE)
                    .backgroundColor(Color.RED)
                    .font(R.font.font)
                    .show()
            }
        }
    }

    private fun hideAll() {
        image!!.setImageResource(0)
        imageHolder!!.visibility = View.GONE
        processButtons!!.visibility = View.GONE
        selectButtons!!.visibility = View.VISIBLE
        selectImageText!!.visibility = View.VISIBLE
        bitmap = null
    }

    private fun showAll() {
        imageHolder!!.visibility = View.VISIBLE
        processButtons!!.visibility = View.VISIBLE
        selectButtons!!.visibility = View.GONE
        selectImageText!!.visibility = View.GONE
    }

    private fun processImage() {
        if (bitmap != null) {
            val dialog = ProgressDialog(this)
            dialog.setTitle("Processing...")
            dialog.setMessage("Please have patience ._.")
            dialog.setCancelable(false)
            dialog.show()
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient()
            recognizer.process(inputImage)
                .addOnSuccessListener { result: Text ->
                    val text = result.text
                    dialog.dismiss()
                    if (!TextUtils.isEmpty(text) && text != null) {
                        val builder = AlertDialog.Builder(this@Home, R.style.AlertDialogTheme)
                        builder.setTitle("Text Recognized")
                            .setMessage(text)
                            .setCancelable(false)
                            .setPositiveButton("Copy") { dialog1: DialogInterface?, id: Int ->
                                copyText(
                                    text
                                )
                            }
                            .setNegativeButton("Cancel") { dialog1: DialogInterface?, which: Int -> hideAll() }
                            .setNeutralButton("Close Dialog") { dialog1: DialogInterface, which: Int -> dialog1.cancel() }
                        val alert = builder.create()
                        alert.window!!.setBackgroundDrawableResource(R.drawable.round_dialog)
                        alert.setOnShowListener { arg0: DialogInterface? ->
                            alert.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setTextColor(ContextCompat.getColor(this@Home, R.color.blue))
                            alert.getButton(AlertDialog.BUTTON_NEGATIVE)
                                .setTextColor(ContextCompat.getColor(this@Home, R.color.blue))
                        }
                        alert.show()
                    } else {
                        val builder = AlertDialog.Builder(this@Home, R.style.AlertDialogTheme)
                        builder.setTitle("Ooof")
                            .setMessage("No Text Detected!")
                            .setCancelable(false)
                            .setPositiveButton("Ok") { dialog1: DialogInterface, id: Int -> dialog1.dismiss() }
                        val alert = builder.create()
                        alert.window!!.setBackgroundDrawableResource(R.drawable.round_dialog)
                        alert.setOnShowListener { arg0: DialogInterface? ->
                            alert.getButton(AlertDialog.BUTTON_POSITIVE)
                                .setTextColor(ContextCompat.getColor(this@Home, R.color.blue))
                            alert.getButton(AlertDialog.BUTTON_NEGATIVE)
                                .setTextColor(ContextCompat.getColor(this@Home, R.color.blue))
                        }
                        alert.show()
                    }
                }
                .addOnFailureListener { e: Exception ->
                    hideAll()
                    StyleableToast.Builder(this)
                        .text("Error: " + e.message)
                        .textColor(Color.WHITE)
                        .backgroundColor(Color.RED)
                        .font(R.font.font)
                        .show()
                }
        }
    }

    public override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )
        if (requestCode == PICK_IMAGE_CODE && resultCode == RESULT_OK && data != null && data.data != null) {
            try {
                bitmap = MediaStore.Images.Media
                    .getBitmap(
                        this.contentResolver,
                        data.data
                    )
                Glide.with(this@Home)
                    .load(bitmap)
                    .into(image!!)
                showAll()
            } catch (e: IOException) {
                hideAll()
            }
        }
        if (requestCode == CAPTURE_IMAGE_CODE) {
            if (resultCode == RESULT_OK) {
                bitmap = data!!.extras!!["data"] as Bitmap?
                Glide.with(this@Home)
                    .load(bitmap)
                    .into(image!!)
                showAll()
            } else {
                hideAll()
            }
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                try {
                    bitmap = MediaStore.Images.Media
                        .getBitmap(
                            this.contentResolver,
                            result.uri
                        )
                    Glide.with(this@Home)
                        .load(bitmap)
                        .into(image!!)
                    val fdelete = File(result.uri.path)
                    if (fdelete.exists()) {
                        if (fdelete.delete()) {
                            StyleableToast.Builder(this)
                                .text("Saved storage by deleting unwanted image crop data :)")
                                .textColor(Color.WHITE)
                                .backgroundColor(ContextCompat.getColor(this, R.color.blue))
                                .font(R.font.font)
                                .length(Toast.LENGTH_LONG)
                                .show()
                        } else {
                            StyleableToast.Builder(this)
                                .text("Could not delete unwanted cropped image data :(")
                                .textColor(Color.WHITE)
                                .backgroundColor(Color.RED)
                                .font(R.font.font)
                                .length(Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                    showAll()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                hideAll()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        if (isDarkTheme) {
            menu.findItem(R.id.themeSwitcher).setIcon(R.drawable.day)
        } else {
            menu.findItem(R.id.themeSwitcher).setIcon(R.drawable.night)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.about) {
            val builder = AlertDialog.Builder(this@Home, R.style.AlertDialogTheme)
            builder.setTitle("About")
                .setMessage(resources.getString(R.string.about))
                .setCancelable(false)
                .setPositiveButton("Nice!") { dialog1: DialogInterface, id: Int ->
                    dialog1.dismiss()
                    StyleableToast.Builder(this)
                        .text("Thank you :)")
                        .textColor(Color.WHITE)
                        .backgroundColor(ContextCompat.getColor(this, R.color.green))
                        .font(R.font.font)
                        .show()
                }
            val alert = builder.create()
            alert.window!!.setBackgroundDrawableResource(R.drawable.round_dialog)
            alert.setOnShowListener { arg0: DialogInterface? ->
                alert.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this@Home, R.color.blue))
                alert.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(this@Home, R.color.blue))
            }
            alert.show()
            return true
        } else if (item.itemId == R.id.themeSwitcher) {
            editor = preferences!!.edit()
            if (isDarkTheme) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                item.setIcon(R.drawable.night)
                editor.putBoolean("theme", false)
                editor.apply()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                item.setIcon(R.drawable.day)
                editor.putBoolean("theme", true)
                editor.apply()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    val isDarkTheme: Boolean
        get() = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

    companion object {
        private const val PICK_IMAGE_CODE = 0
        private const val CAPTURE_IMAGE_CODE = 1
        private const val CAMERA_REQUEST_CODE = 2
        private const val WRITE_REQUEST_CODE = 3
    }
}