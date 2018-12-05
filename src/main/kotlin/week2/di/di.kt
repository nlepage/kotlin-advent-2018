package week2.di

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// FIXME use a (mutable) ProviderBuilder for definitions, which will allow infix calls
// FIXME add a module level ? to be used by use() ?

inline fun registry(block: Registry.Builder.() -> Unit) = Registry.Builder().apply(block)._build().also { Registry.current = it }

data class Registry(
        private val registryMap: Map<Key, () -> Any?>
) : Map<Registry.Key, () -> Any?> by registryMap {

    companion object {
        private var _current: Registry? = null
        var current: Registry
            @Synchronized get() = _current ?: throw RuntimeException("No registry defined")
            @Synchronized set(value) {
                _current = value
            }
    }

    data class Key(
            val type: String,
            val nullable: Boolean
    )

    data class Builder(
            val _providers: MutableList<Pair<Key, () -> Any?>> = mutableListOf()
    ) {
        inline fun <reified T> provider(nullable: Boolean, noinline provider: () -> T) {
            val type = T::class.qualifiedName ?: throw RuntimeException("Can't create provider for anonymous type")
            _providers += Key(type, nullable) to provider
        }

        inline fun <reified T : Any> provider(noinline provider: () -> T) = provider(false, provider)

        inline fun <reified T : Any> optionalProvider(noinline provider: () -> T?) = provider(true, provider)

        inline fun <reified T : Any> value(value: T) = provider { value }

        inline fun <reified T : Any> optionalValue(value: T?) = optionalProvider { value }

        inline fun <reified T> singleton(nullable: Boolean, noinline provider: () -> T) {
            var value: T? = null
            var init = false
            provider(nullable) {
                synchronized(provider) {
                    if (!init) {
                        value = provider()
                        init = true
                    }
                    value as T
                }
            }
        }

        inline fun <reified T : Any> singleton(noinline provider: () -> T) = singleton(false, provider)

        inline fun <reified T : Any> optionalSingleton(noinline provider: () -> T?) = singleton(true, provider)

        fun _build(): Registry {
            // FIXME make some verifications (duplicate key)
            return Registry(mapOf(*_providers.toTypedArray()))
        }
    }

    inline fun <reified T> resolve(nullable: Boolean): T {
        val type = T::class.qualifiedName ?: throw RuntimeException("Can't inject anonymous type")
        val provider = this[Key(type, nullable)] ?: throw  RuntimeException("No provider for $type nullable=$nullable")
        return provider() as T
    }
}

inline fun <reified T> get(nullable: Boolean = false) = Registry.current.resolve<T>(nullable)

inline fun <reified T> inject() = object : ReadOnlyProperty<Any, T> {
    val registry = Registry.current
    var value: T? = null
    var init = false

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        synchronized(this) {
            if (!init) {
                value = registry.resolve<T>(property.returnType.isMarkedNullable)
                init = true
            }
        }
        return value as T
    }
}
