package com.example.sophie.ble2.logging

/**
 * Created by Sophie on 3/8/2018.
 */
import mu.KLogging

class SingletonLog {


    constructor() {
//check its a singleton
        logger.info("Hello World.")
    }

    //use logging
    companion object : KLogging() {
        //make it singleton
        val instance: SingletonLog by lazy {
            SingletonLog()
        }
    }
    //function to be able to make random messages
    fun logDown(string: String) {
        logger.info(string)
    }

}