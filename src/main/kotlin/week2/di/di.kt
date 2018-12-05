package week2.di

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

val defaultRegistry = Registry()
var currentRegistry = defaultRegistry

fun use(registry: Registry) {
    currentRegistry = registry
}

// FIXME here replace Registry() by a Registry.Builder() to allow infix calls in dsl then instantiate Registry using builder.build()
// FIXME -> remove top level definitions, force using registry DSL
// FIXME -> Registry can become immutable (nice !)
// FIXME also use a (mutable) ProviderBuilder for definitions, which will allow infix calls
// FIXME calling registry will implicitly set the currentRegistry ? (default would be an empty registry ?)
// FIXME add a module level ? to be used by use() ?
inline fun registry(block: Registry.() -> Unit) = Registry().apply(block)
inline fun <reified T : Any> provider(noinline provider: () -> T) = defaultRegistry.provider(provider)
inline fun <reified T : Any> optionalProvider(noinline provider: () -> T?) = defaultRegistry.optionalProvider(provider)
inline fun <reified T : Any> value(value: T) = defaultRegistry.value(value)
inline fun <reified T : Any> optionalValue(value: T?) = defaultRegistry.optionalValue(value)
inline fun <reified T : Any> singleton(noinline provider: () -> T) = defaultRegistry.singleton(provider)
inline fun <reified T : Any> optionalSingleton(noinline provider: () -> T?) = defaultRegistry.optionalSingleton(provider)

data class Registry(
        private val registryMap: MutableMap<String, () -> Any?> = mutableMapOf()
) : MutableMap<String, () -> Any?> by registryMap {

    inline fun <reified T : Any> provider(noinline provider: () -> T) {
        val type = T::class.qualifiedName ?: throw RuntimeException("Can't create provider for anonymous type")
        if (type in this) throw RuntimeException("A provider is already defined for $type")
        this[type] = provider
    }

    inline fun <reified T : Any> optionalProvider(noinline provider: () -> T?) {
        val type = T::class.qualifiedName?.let { "$it?" }
                ?: throw RuntimeException("Can't create provider for anonymous type")
        if (type in this) throw RuntimeException("A provider is already defined for $type")
        this[type] = provider
    }

    inline fun <reified T : Any> value(value: T) {
        provider { value }
    }

    inline fun <reified T : Any> optionalValue(value: T?) {
        optionalProvider { value }
    }

    inline fun <reified T : Any> singleton(noinline provider: () -> T) {
        var value: T? = null
        var init = false
        provider {
            synchronized(provider) {
                if (!init) {
                    value = provider()
                    init = true
                }
                value as T
            }
        }
    }

    inline fun <reified T : Any> optionalSingleton(noinline provider: () -> T?) {
        var value: T? = null
        var init = false
        optionalProvider {
            synchronized(provider) {
                if (!init) {
                    value = provider()
                    init = true
                }
                value
            }
        }
    }

    inline fun <reified T> get(nullable: Boolean): T {
        val name = T::class.qualifiedName ?: throw RuntimeException("Can't inject anonymous type")
        val provider = this["$name${if (nullable) "?" else ""}"]
                ?: throw  RuntimeException("No provider for $name")
        return provider() as T
    }
}

inline fun <reified T> inject() = object : ReadOnlyProperty<Any, T> {
    // FIXME store current registry here ?
    var value: T? = null
    var init = false

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        synchronized(this) {
            if (!init) {
                value = currentRegistry.get<T>(property.returnType.isMarkedNullable)
                init = true
            }
        }
        return value as T
    }
}

inline fun <reified T> get(nullable: Boolean = false) = currentRegistry.get<T>(nullable)
