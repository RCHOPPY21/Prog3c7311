package com.example.flowfund

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private val TAG = "AddExpenseActivity"

    private var selectedCategory  = "Food"
    private var selectedDate      = ""
    private var selectedStartTime = ""
    private var selectedEndTime   = ""
    private var isRecurring       = false
    private var receiptBitmap: Bitmap? = null
    private var savedPhotoPath: String? = null

    // ── Camera launcher ──────────────────────────────────────────────────────
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val photo = result.data?.extras?.get("data") as? Bitmap
                if (photo != null) {
                    receiptBitmap = photo
                    showPhotoPreview(photo)
                    Log.d(TAG, "Photo taken from camera")
                }
            }
        }

    // ── Gallery launcher ─────────────────────────────────────────────────────
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri: Uri? = result.data?.data
                if (uri != null) {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    receiptBitmap = bitmap
                    showPhotoPreview(bitmap)
                    Log.d(TAG, "Photo selected from gallery")
                }
            }
        }

    // ── Camera permission launcher ───────────────────────────────────────────
    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openCamera()
            else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }

    // ── onCreate ─────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        Log.d(TAG, "AddExpenseActivity started")

        // Find all views
        val etAmount        = findViewById<EditText>(R.id.etAmount)
        val etDescription   = findViewById<EditText>(R.id.etDescription)
        val tvDate          = findViewById<TextView>(R.id.tvDate)
        val tvStartTime     = findViewById<TextView>(R.id.tvStartTime)
        val tvEndTime       = findViewById<TextView>(R.id.tvEndTime)
        val switchRecurring = findViewById<Switch>(R.id.switchRecurring)
        val btnAttachPhoto  = findViewById<AppCompatButton>(R.id.btnAttachPhoto)
        val btnSaveExpense  = findViewById<AppCompatButton>(R.id.btnSaveExpense)
        val btnFood         = findViewById<AppCompatButton>(R.id.btnCatFood)
        val btnTransport    = findViewById<AppCompatButton>(R.id.btnCatTransport)
        val btnUtilities    = findViewById<AppCompatButton>(R.id.btnCatUtilities)
        val btnEntertain    = findViewById<AppCompatButton>(R.id.btnCatEntertain)
        val btnCustom       = findViewById<AppCompatButton>(R.id.btnCatCustom)

        // Default date = today
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        tvDate.text = "📅  $today"
        selectedDate = today

        // ── Category selection ────────────────────────────────────────────────
        val categoryButtons = listOf(btnFood, btnTransport, btnUtilities, btnEntertain)

        fun highlightCategory(selected: AppCompatButton, name: String) {
            categoryButtons.forEach {
                it.setBackgroundResource(R.drawable.rounded_edittext)
                it.setTextColor(getColor(R.color.text_gray))
            }
            btnCustom.setBackgroundResource(R.drawable.rounded_edittext)
            btnCustom.setTextColor(getColor(R.color.text_gray))
            selected.setBackgroundResource(R.drawable.rounded_button_green)
            selected.setTextColor(getColor(R.color.text_dark))
            selectedCategory = name
            Log.d(TAG, "Category selected: $name")
        }

        btnFood.setOnClickListener      { highlightCategory(btnFood, "Food") }
        btnTransport.setOnClickListener { highlightCategory(btnTransport, "Transport") }
        btnUtilities.setOnClickListener { highlightCategory(btnUtilities, "Utilities") }
        btnEntertain.setOnClickListener { highlightCategory(btnEntertain, "Entertainment") }

        // Start with Food highlighted by default
        btnFood.setBackgroundResource(R.drawable.rounded_button_green)
        btnFood.setTextColor(getColor(R.color.text_dark))

        btnCustom.setOnClickListener {
            val input = EditText(this)
            input.hint = "e.g. Medical, Gym..."
            AlertDialog.Builder(this)
                .setTitle("Custom Category")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    val custom = input.text.toString().trim()
                    if (custom.isNotEmpty()) {
                        selectedCategory = custom
                        btnCustom.text = custom
                        categoryButtons.forEach {
                            it.setBackgroundResource(R.drawable.rounded_edittext)
                            it.setTextColor(getColor(R.color.text_gray))
                        }
                        btnCustom.setBackgroundResource(R.drawable.rounded_button_green)
                        btnCustom.setTextColor(getColor(R.color.text_dark))
                        Log.d(TAG, "Custom category: $custom")
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // ── Date picker ───────────────────────────────────────────────────────
        tvDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate = "%02d/%02d/%04d".format(day, month + 1, year)
                    tvDate.text = "📅  $selectedDate"
                    Log.d(TAG, "Date: $selectedDate")
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // ── Time pickers ──────────────────────────────────────────────────────
        tvStartTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                selectedStartTime = "%02d:%02d".format(hour, minute)
                tvStartTime.text = "🕐 $selectedStartTime"
                Log.d(TAG, "Start time: $selectedStartTime")
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        tvEndTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                selectedEndTime = "%02d:%02d".format(hour, minute)
                tvEndTime.text = "🕐 $selectedEndTime"
                Log.d(TAG, "End time: $selectedEndTime")
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        // ── Recurring switch ──────────────────────────────────────────────────
        switchRecurring.setOnCheckedChangeListener { _, isChecked ->
            isRecurring = isChecked
            Log.d(TAG, "Recurring: $isRecurring")
        }

        // ── Photo button ──────────────────────────────────────────────────────
        btnAttachPhoto.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Attach Receipt Photo")
                .setItems(arrayOf("📷  Take a Photo", "🖼️  Choose from Gallery")) { _, which ->
                    when (which) {
                        0 -> checkCameraPermissionAndOpen()
                        1 -> openGallery()
                    }
                }.show()
        }

        // ── Save button ───────────────────────────────────────────────────────
        btnSaveExpense.setOnClickListener {
            val amount      = etAmount.text.toString().trim()
            val description = etDescription.text.toString().trim()

            when {
                amount.isEmpty() -> {
                    etAmount.error = "Please enter an amount"
                    etAmount.requestFocus()
                    return@setOnClickListener
                }
                description.isEmpty() -> {
                    etDescription.error = "Please enter a description"
                    etDescription.requestFocus()
                    return@setOnClickListener
                }
                selectedStartTime.isEmpty() -> {
                    Toast.makeText(this, "Please select a start time", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                selectedEndTime.isEmpty() -> {
                    Toast.makeText(this, "Please select an end time", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            if (receiptBitmap != null) {
                savedPhotoPath = savePhotoToStorage(receiptBitmap!!)
            }

            val expense = Expense(
                amount      = amount.toDouble(),
                description = description,
                category    = selectedCategory,
                date        = selectedDate,
                startTime   = selectedStartTime,
                endTime     = selectedEndTime,
                photoPath   = savedPhotoPath,
                isRecurring = isRecurring
            )

            val db = FlowFundDatabase.getDatabase(this)
            lifecycleScope.launch {
                db.expenseDao().insertExpense(expense)
                Log.i(TAG, "Expense saved to DB: $expense")
                runOnUiThread {
                    Toast.makeText(
                        this@AddExpenseActivity,
                        "Expense saved! ✅",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun savePhotoToStorage(bitmap: Bitmap): String {
        val filename = "receipt_${System.currentTimeMillis()}.jpg"
        val file = File(filesDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        Log.d(TAG, "Photo saved to: ${file.absolutePath}")
        return file.absolutePath
    }

    private fun showPhotoPreview(bitmap: Bitmap) {
        val ivPreview = findViewById<ImageView>(R.id.ivReceiptPreview)
        val tvLabel   = findViewById<TextView>(R.id.tvPhotoLabel)
        ivPreview.setImageBitmap(bitmap)
        ivPreview.visibility = android.view.View.VISIBLE
        tvLabel.text = "✅ Photo attached"
    }

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    }

    private fun openGallery() {
        galleryLauncher.launch(
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        )
    }
}