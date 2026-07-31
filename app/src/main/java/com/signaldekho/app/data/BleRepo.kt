package com.signaldekho.app.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build

@SuppressLint("MissingPermission") // callers gate on BLUETOOTH_SCAN / location
class BleRepo(context: Context) {
    private val adapter =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var callback: ScanCallback? = null

    fun startScan(onReading: (BleReading) -> Unit) {
        val scanner = adapter?.bluetoothLeScanner ?: return
        if (callback != null) return
        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val name = result.scanRecord?.deviceName
                    ?: if (Build.VERSION.SDK_INT < 31) result.device?.name else null
                onReading(BleReading(name, result.device?.address ?: "?", result.rssi))
            }
        }
        scanner.startScan(callback)
    }

    fun stopScan() {
        callback?.let { adapter?.bluetoothLeScanner?.stopScan(it) }
        callback = null
    }
}
