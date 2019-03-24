package com.jovial.simples

import android.Manifest
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.ActionBar;
import android.os.Bundle
import android.content.pm.PackageManager
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.Menu
import android.view.MenuItem
import android.view.View;
import android.widget.*
import android.text.method.ScrollingMovementMethod
import android.view.inputmethod.InputMethodManager


open class ViewLookup {
    protected fun<T: View> AppCompatActivity.findView(id: Int) : T = findViewById<T>(id)!!
}

class MainActivity : AppCompatActivity() {

    private val ui by lazy {
        object : ViewLookup() {
            val portInput : EditText = findView(R.id.portInput)
            val prefixInput : EditText = findView(R.id.prefixInput)
            val directoryButton : Button = findView(R.id.directoryButton)
            val directoryText : TextView = findView(R.id.directoryText)
            val servingButton : ToggleButton = findView(R.id.servingButton)
            val tlsButton : ToggleButton = findView(R.id.tlsButton)
            val uploadsButton = findView<ToggleButton>(R.id.uploadsButton)
            val outputText : TextView = findView(R.id.outputText)
        }
    }

    private val logListener = { line: String ->
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
            setServing(isChecked)
        }
        val stopServing = { _: View, _: Boolean ->
            ui.servingButton.setChecked(false)
        }
        ui.tlsButton.setOnCheckedChangeListener(stopServing)
        ui.uploadsButton.setOnCheckedChangeListener(stopServing)
        ui.directoryButton.setOnClickListener { _ : View ->
            if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || !hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
            {
                ActivityCompat.requestPermissions(this, arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 0)
            } else {
                launchChooseDirectoryDialog()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0
            && permissions.size == 2
            && grantResults.all { it == PackageManager.PERMISSION_GRANTED })
        {
            launchChooseDirectoryDialog()
        }
    }

    private fun launchChooseDirectoryDialog() {
        val dialog = DirectoryChooserDialog(this@MainActivity) { dir: String ->
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
