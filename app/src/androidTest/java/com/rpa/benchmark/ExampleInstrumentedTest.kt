package com.rpa.benchmark

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.widget.RelativeLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@RunWith(AndroidJUnit4::class)
class BenchmarkTest {

    private val device: UiDevice
    private val timeoutLimit = 60000L
    private lateinit var startTime: TimeMark

    init {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        device = UiDevice.getInstance(instrumentation)
    }

    @Before
    fun setUp() {
        device.pressHome()
        startTime = TimeSource.Monotonic.markNow()
    }

    @After
    fun tearDown() {
        device.pressHome()
        val elapsedTime = startTime.elapsedNow()
        println("Elapsed time: $elapsedTime")
    }

    @Test
    fun wifiConnectionTest() {
        // Open apps list by scrolling on home screen
        val workspace = device.findObject(
            By.res("com.google.android.apps.nexuslauncher:id/workspace")
        )
        workspace.scroll(Direction.DOWN, 1.0f)

        // Click on Settings icon to launch the app
        val settings = device.findObject(
            By.res("com.google.android.apps.nexuslauncher:id/icon").text("Settings")
        )
        settings.click()

        val networkAndInternet = device.wait(Until.findObject(By.text("Network & internet")), timeoutLimit)
        networkAndInternet.click()
        val wifi = device.wait(Until.findObject(By.text("Internet")), timeoutLimit)
        wifi.click()
        val addNetwork = device.wait(Until.findObject(By.text("Add network")), timeoutLimit)
        addNetwork.click()

        // Obtain an instance of UiObject2 of the text field
        val ssidField = device.wait(Until.findObject(By.res("com.android.settings:id/ssid")), timeoutLimit)
        // Call the setText method using  Kotlin's property access syntax
        val ssid = "AndroidWifi"
        ssidField.text = ssid
        //Click on Save button
        device.findObject(By.res("android:id/button1").text("Save")).click()

        // BySelector matching the just added Wi-Fi
        val ssidSelector = By.text(ssid).res("android:id/title")
        // BySelector matching the connected status
        val status = By.text("Connected").res("android:id/summary")
        // BySelector matching on entry of Wi-Fi list with the desired SSID and status
        val networkEntrySelector =
            By.clazz(RelativeLayout::class.qualifiedName.orEmpty()).hasChild(ssidSelector)
                .hasChild(status)

        // Perform the validation using hasObject
        // Wait up to 5 seconds to find the element we're looking for
        val isConnected = device.wait(Until.hasObject(networkEntrySelector), timeoutLimit)
        Assert.assertTrue("Verify if device is connected to added Wi-Fi", isConnected)

        // Perform the validation using Android APIs
        val connectedWifi = getCurrentWifiSsid()
        println("Connected Wi-Fi: $connectedWifi")
    }

    private fun getCurrentWifiSsid(): String {
        val context = InstrumentationRegistry.getInstrumentation().context
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        var ssid = ""
        val networkCallback = object : ConnectivityManager.NetworkCallback(
            FLAG_INCLUDE_LOCATION_INFO) {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val wifiInfo = networkCapabilities.transportInfo as WifiInfo
                ssid = wifiInfo.ssid
            }
        }
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerNetworkCallback(request, networkCallback)
        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        // The SSID is quoted, then we need to remove quotes
        return ssid.removeSurrounding("\"")
    }
}