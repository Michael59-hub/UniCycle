package ng.unicycle

import android.app.Application

class UniCycleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidContext.app = this
    }
}

object AndroidContext {
    lateinit var app: Application
}
