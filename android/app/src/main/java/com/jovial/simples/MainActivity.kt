package com.jovial.simples

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


open class ViewLookup {
    protected fun<T: View> AppCompatActivity.findView(id: Int) : T = findViewById<T>(id)!!
}

class MainActivity : AppCompatActivity() {

    private enum class RequestID { STARTUP, GET_DIRECTORY, SERVER_ON }

    private val ui by lazy {
        object : ViewLookup() {
            val portInput : EditText = findView(R.id.portInput)
            val prefixInput : EditText = findView(R.id.prefixInput)
            val volumeDropdown: Spinner = findView(R.id.volumeDropdown)
            val directoryButton : Button = findView(R.id.directoryButton)
            val directoryText : TextView = findView(R.id.directoryText)
            val servingButton : ToggleButton = findView(R.id.servingButton)
            val tlsButton : ToggleButton = findView(R.id.tlsButton)
            val uploadsButton = findView<ToggleButton>(R.id.uploadsButton)
            val outputText : TextView = findView(R.id.outputText)
        }
    }

    private val logListener = { line: String ->
        // Note that initial contents of ui.outputText was set at the moment
        // we added the listener
        runOnUiThread {
            ui.outputText.append(line)
            ui.outputText.scrollTo(0, Integer.MAX_VALUE )
        }
    }

    private fun hasPermission(permission: String) : Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) : Boolean {
        if (item.itemId == R.id.main_menu_quit) {
            System.exit(0)
            return true     // not reached
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkManageStoragePermission() {
        if (!Environment.isExternalStorageManager()) {
            val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    uri
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkManageStoragePermission()
        // m_sdcardDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();
        val volumes: List<String> = this@MainActivity.getSystemService<StorageManager>(
            StorageManager::class.java
        ).storageVolumes.map { v : StorageVolume  -> v.directory!!.absolutePath };
        ui.volumeDropdown.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, volumes)
        ui.volumeDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                ServerLauncher.directory = volumes[position]
                ui.directoryText.text = ServerLauncher.directory
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }
        ServerLauncher.directory = volumes[0]
        val actionBar = supportActionBar!!
        // actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME or ActionBar.DISPLAY_USE_LOGO or ActionBar.DISPLAY_SHOW_TITLE)
        actionBar.setLogo(R.drawable.meerkat_logo)
        ui.outputText.movementMethod = ScrollingMovementMethod();

        ui.portInput.setText(ServerLauncher.port.toString())
        ui.prefixInput.setText(ServerLauncher.prefix)
        ui.directoryText.text = ServerLauncher.directory
        ui.servingButton.isChecked = ServerLauncher.serverOn
        ui.tlsButton.isChecked = ServerLauncher.tlsOn
        ui.uploadsButton.isChecked = ServerLauncher.uploadsOn
        ui.outputText.text = ServerLauncher.addLogListener(logListener)
        ui.outputText.scrollTo(0, Integer.MAX_VALUE )

        ui.servingButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkManageStoragePermission()
            }
            if (isChecked && (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        || !hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        || !hasPermission(Manifest.permission.INTERNET)))
            {
                requestStoragePermissions(RequestID.SERVER_ON)
            } else {
                setServing(isChecked)
            }
        }
        val stopServing = { _: View, _: Boolean ->
            ui.servingButton.setChecked(false)
        }
        ui.tlsButton.setOnCheckedChangeListener(stopServing)
        ui.uploadsButton.setOnCheckedChangeListener(stopServing)
        ui.directoryButton.setOnClickListener { _ : View ->
            checkManageStoragePermission()
            if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || !hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                || !hasPermission(Manifest.permission.INTERNET))
            {
                requestStoragePermissions(RequestID.GET_DIRECTORY)
            } else {
                launchChooseDirectoryDialog()
            }
        }
        if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            || !hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            || !hasPermission(Manifest.permission.INTERNET))
        {
            requestStoragePermissions(RequestID.STARTUP)
        }
    }

    private fun requestStoragePermissions(id: RequestID) {
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET
        ), id.ordinal)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = permissions.size == 3 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (granted && requestCode == RequestID.GET_DIRECTORY.ordinal) {
            launchChooseDirectoryDialog()
        } else if (requestCode == RequestID.SERVER_ON.ordinal) {
            if (!granted) {
                ui.servingButton.isChecked = false
            } else {
                setServing(ui.servingButton.isChecked)
            }
        } else if (!granted){
            Toast.makeText(this, "I'll run the server anyway.  Press the directory button to re-request",
                Toast.LENGTH_LONG).show()
        }
    }

    private fun launchChooseDirectoryDialog() {
        val volumeDir : String = ui.volumeDropdown.selectedItem.toString();
        val dialog = DirectoryChooserDialog(this@MainActivity, volumeDir) { dir: String ->
            ui.directoryText.text = dir
            ServerLauncher.directory = dir
        }
        dialog.newFolderEnabled = true
        dialog.chooseDirectory()
    }

    private fun setServing(enabled: Boolean) {
        getFromUI()
        if (enabled) {
            this.currentFocus?.let { v ->
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
            }
            ServerLauncher.startServer(applicationContext.assets)
        } else {
            ServerLauncher.stopServer()
        }
    }

    private fun getFromUI() {
        ServerLauncher.port = try {
            Integer.parseInt(ui.portInput.text.toString())
        } catch (e : NumberFormatException ) {
            ui.portInput.setText("6001")
            6001
        }
        ServerLauncher.prefix = ui.prefixInput.text.toString()
        ServerLauncher.directory = ui.directoryText.text.toString()
        ServerLauncher.tlsOn = ui.tlsButton.isChecked
        ServerLauncher.uploadsOn = ui.uploadsButton.isChecked
    }

    override fun onDestroy() {
        super.onDestroy()
        ServerLauncher.removeLogListener(logListener)
        getFromUI()
    }
}
