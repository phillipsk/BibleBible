import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import kotlin.jvm.JvmStatic


object NapierLogger {
    @JvmStatic
    internal fun initIosNapierLogger() {
        Napier.base(DebugAntilog())
    }
}
