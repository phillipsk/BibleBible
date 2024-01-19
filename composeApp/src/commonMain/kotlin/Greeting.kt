class Greeting {
    private val platform = getPlatform()

    internal fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}