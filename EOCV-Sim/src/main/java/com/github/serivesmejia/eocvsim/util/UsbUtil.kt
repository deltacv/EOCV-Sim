package com.github.serivesmejia.eocvsim.util

import javax.usb.UsbDevice
import javax.usb.UsbHostManager
import javax.usb.UsbHub

object UsbUtil {

    fun peripheriques(): List<UsbDevice> {
        val services = UsbHostManager.getUsbServices()
        val root = services.rootUsbHub
        println(root)

        return peripheriques(root as UsbHub)
    }

    fun peripheriques(root: UsbHub): List<UsbDevice> {
        val devices = mutableListOf<UsbDevice>()

        for(dev in root.attachedUsbDevices) {
            val device = dev as UsbDevice

            if(device.isUsbHub) {
                devices.addAll(peripheriques(device as UsbHub))
            } else {
                devices.add(device)
            }
        }

        return devices
    }

}