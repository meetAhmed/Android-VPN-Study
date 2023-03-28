package com.moduscreate.vpn.study

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContract
import com.moduscreate.vpn.study.vpn.MyVpnService
import com.moduscreate.vpn.study.vpn.isMyVpnServiceRunning

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupUI()

        val btnStartVPN = findViewById<Button>(R.id.btnStartVPN)

        btnStartVPN.setOnClickListener {
            if (isMyVpnServiceRunning) {
                stopVpn()
                setupUI(isStart = false)
            } else {
                prepareVpn()
                setupUI(isStart = true)
            }
        }
    }

    private val vpnContent = registerForActivityResult(VpnContent()) {
        if (it) {
            startVpn()
        }
    }

    private fun prepareVpn() {
        VpnService.prepare(this@MainActivity)?.let {
            vpnContent.launch(it)
        } ?: kotlin.run {
            startVpn()
        }
    }

    private fun startVpn() {
        startService(Intent(this@MainActivity, MyVpnService::class.java).also { it.action = MyVpnService.ACTION_CONNECT })
    }

    private fun stopVpn() {
        startService(Intent(this@MainActivity, MyVpnService::class.java).also { it.action = MyVpnService.ACTION_DISCONNECT })
    }

    class VpnContent : ActivityResultContract<Intent, Boolean>() {
        override fun createIntent(context: Context, input: Intent): Intent {
            return input
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
            return resultCode == RESULT_OK
        }
    }

    private fun setupUI(isStart: Boolean? = null) {
        if (isStart == true) {
            findViewById<Button>(R.id.btnStartVPN).text = getString(R.string.stop_vpn)
        } else if (isStart == false) {
            findViewById<Button>(R.id.btnStartVPN).text = getString(R.string.start_vpn)
        } else {
            if (isMyVpnServiceRunning) {
                findViewById<Button>(R.id.btnStartVPN).text = getString(R.string.stop_vpn)
            } else {
                findViewById<Button>(R.id.btnStartVPN).text = getString(R.string.start_vpn)
            }
        }
    }
}