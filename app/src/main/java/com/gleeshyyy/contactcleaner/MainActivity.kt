package com.gleeshyyy.contactcleaner

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentUris
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val requestCode = 101
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private val rows = mutableListOf<Row>()
    data class Row(val rawId: Long, val name: String, val phone: String, val check: CheckBox)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        if (hasPermission()) scan() else askPermission()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 16)
            setBackgroundColor(0xFFF7F7F7.toInt())
        }
        root.addView(TextView(this).apply {
            text = "Contact Cleaner"; textSize = 27f
            setTextColor(0xFFB00020.toInt()); gravity = Gravity.CENTER_VERTICAL
        })
        root.addView(TextView(this).apply {
            text = "Find repeated phone numbers. The first occurrence is kept; later duplicates are selected for your review."
            textSize = 15f; setTextColor(0xFF222222.toInt()); setPadding(0, 10, 0, 12)
        })
        status = TextView(this).apply { textSize = 14f; setTextColor(0xFF333333.toInt()); setPadding(0, 4, 0, 10) }
        root.addView(status)
        root.addView(Button(this).apply { text = "Scan again"; setOnClickListener { if (hasPermission()) scan() else askPermission() } })
        root.addView(Button(this).apply { text = "Delete selected duplicates"; setOnClickListener { confirmDelete() } })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply {
            addView(list, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun hasPermission() =
        checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
        checkSelfPermission(Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED

    private fun askPermission() {
        requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS), requestCode)
    }

    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        if (code == requestCode && hasPermission()) scan()
        else status.text = "Grant Contacts access in Android Settings to scan and delete duplicates."
    }

    private fun normalize(phone: String) = phone.filter { it.isDigit() }.trimStart('0')

    private fun scan() {
        rows.clear()
        list.removeAllViews()
        status.text = "Scanning contacts…"
        val seen = mutableSetOf<String>()
        var phoneEntries = 0
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
        )
        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID)
            val nameCol = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneCol = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val name = it.getString(nameCol).orEmpty().ifBlank { "(No name)" }
                val phone = it.getString(phoneCol).orEmpty()
                val key = normalize(phone)
                if (key.isBlank()) continue
                phoneEntries++
                if (!seen.add(key)) {
                    val check = CheckBox(this).apply {
                        text = "$name\\n$phone"; textSize = 15f
                        setTextColor(0xFF222222.toInt()); isChecked = true; setPadding(4, 8, 4, 8)
                    }
                    list.addView(check)
                    rows.add(Row(id, name, phone, check))
                }
            }
        }
        status.text = "Scanned $phoneEntries phone entries. Found ${rows.size} possible duplicate entries."
        if (rows.isEmpty()) list.addView(TextView(this).apply {
            text = "No duplicate phone numbers found."; textSize = 16f
            setTextColor(0xFF222222.toInt()); setPadding(4, 18, 4, 18)
        })
        else list.addView(TextView(this).apply {
            text = "Review the checked entries below before deleting."; textSize = 13f
            setTextColor(0xFF555555.toInt()); setPadding(4, 4, 4, 8)
        }, 0)
    }

    private fun confirmDelete() {
        val selected = rows.filter { it.check.isChecked }
        if (selected.isEmpty()) {
            Toast.makeText(this, "No duplicates selected.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this).setTitle("Delete selected contacts?")
            .setMessage("You selected ${selected.size} duplicate entries. This cannot be automatically undone. Back up your contacts first.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> deleteSelected(selected) }
            .show()
    }

    private fun deleteSelected(selected: List<Row>) {
        var deleted = 0
        selected.forEach { row ->
            try {
                val uri = ContentUris.withAppendedId(ContactsContract.RawContacts.CONTENT_URI, row.rawId)
                if (contentResolver.delete(uri, null, null) > 0) deleted++
            } catch (_: Exception) { }
        }
        Toast.makeText(this, "Deleted $deleted selected entries.", Toast.LENGTH_LONG).show()
        scan()
    }
}
