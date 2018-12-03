package week2

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun main(args: Array<String>) {
    // use default registry
    provider { "coucou" }
    optionalProvider { "aze" as String? }

    test()

    use(registry {
        provider { "haha" }
        optionalProvider { "rty" as String? }
    })

    test()
}

fun test() {
    val a = A()
    println(a.s1)
    println(a.s2)

    println(get<String>())
    println(getOptional<String>())
}

class A {
    val s1: String by inject()
    val s2: String? by inject()
}

inline fun registry(block: Registry.() -> Unit) = Registry().apply(block)
inline fun <reified T> provider(noinline provider: () -> T) = registry.provider(provider)
inline fun <reified T> optionalProvider(noinline provider: () -> T?) = registry.optionalProvider(provider)

inline fun <reified T> getProvider(nullable: Boolean): () -> Any? {
    val name = T::class.qualifiedName ?: throw RuntimeException("Can't inject anonymous type")
    return registry["$name${if (nullable) "?" else ""}"]
            ?: throw  RuntimeException("No provider for $name")
}

inline fun <reified T> inject(): ReadOnlyProperty<Any, T> {
    return object : ReadOnlyProperty<Any, T> {
        var value: T? = null
        var init = false

        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            synchronized(this) {
                if (!init) {
                    value = getProvider<T>(property.returnType.isMarkedNullable)() as T?
                    init = true
                }
            }
            return value as T
        }
    }
}

inline fun <reified T : Any> get() = getProvider<T>(false)() as T
inline fun <reified T : Any> getOptional() = getProvider<T>(true)() as T?

data class Registry(
        private val registry: MutableMap<String, () -> Any?> = mutableMapOf()
) : MutableMap<String, () -> Any?> by registry {
    inline fun <reified T> provider(noinline provider: () -> T) {
        val name = T::class.qualifiedName ?: throw RuntimeException("Can't create provider for anonymous type")
        //FIXME throw if already in
        this[name] = provider
    }

    inline fun <reified T> optionalProvider(noinline provider: () -> T?) {
        val name = T::class.qualifiedName ?: throw RuntimeException("Can't create provider for anonymous type")
        //FIXME throw if already in
        this["$name?"] = provider
    }
}

// FIXME use atomic ref ?
private var currentRegistry: Registry? = null
private val defaultRegistry = Registry()
val registry: Registry
    get() = currentRegistry ?: defaultRegistry

fun use(registry: Registry) {
    currentRegistry = registry
}
