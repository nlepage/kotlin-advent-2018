package week2.di

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

// FIXME add a module level ? to be used by use() ?

inline fun registry(block: Registry.Builder.() -> Unit) = Registry.Builder().apply(block).build().also { Registry.current = it }

class Registry(registryMap: Map<Key, () -> Any?>) : Map<Registry.Key, () -> Any?> by registryMap {

    companion object {
        private var _current: Registry? = null
        var current: Registry
            @Synchronized get() = _current ?: throw RuntimeException("No registry defined")
            @Synchronized set(value) {
                _current = value
            }
    }

    // FIXME toString
    data class Key(
            val type: String,
            val nullable: Boolean,
            val name: String? = null
    )

    class ProviderBuilder(
            val provider: () -> Any?,
            var key: Key
    ) {
        infix fun named(name: String) = apply { key = key.copy(name = name) }

        fun build() = key to provider
    }

    class Builder(
            val providers: MutableList<ProviderBuilder> = mutableListOf()
    ) {
        inline fun <reified T> provider(nullable: Boolean, noinline provider: () -> T): ProviderBuilder {
            val type = T::class.qualifiedName ?: throw RuntimeException("Can't create provider for anonymous type")
            return ProviderBuilder(provider, Key(type, nullable)).also { providers += it }
        }

        inline fun <reified T : Any> provider(noinline provider: () -> T) = provider(false, provider)

        inline fun <reified T : Any> optionalProvider(noinline provider: () -> T?) = provider(true, provider)

        inline fun <reified T : Any> value(value: T) = provider { value }

        inline fun <reified T : Any> optionalValue(value: T?) = optionalProvider { value }

        inline fun <reified T> singleton(nullable: Boolean, noinline provider: () -> T): ProviderBuilder {
            var value: T? = null
            var init = false
            return provider(nullable) {
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

        fun build(): Registry {
            return providers
                    .map { it.build() }
                    .also {
                        val errors = it.fold(Pair(setOf<Key>(), listOf<String>())) { (keys, errors), (key) ->
                            if (key in keys)
                                Pair(keys, errors + "Provider already defined for $key")
                            else
                                Pair(keys + key, errors)
                        }.second
                        if (errors.isNotEmpty())
                            throw RuntimeException("""One or more errors in registry definition:
                                |${errors.joinToString("\n", transform = { "  $it" })}""".trimMargin())
                    }
                    .toTypedArray()
                    .let { Registry(mapOf(*it)) }
        }
    }

    inline fun <reified T> resolve(nullable: Boolean, name: String?): T {
        val type = T::class.qualifiedName ?: throw RuntimeException("Can't inject anonymous type")
        val key = Key(type, nullable, name)
        val provider = this[key] ?: throw  RuntimeException("No provider for $key")
        return provider() as T
    }
}

inline fun <reified T> get(nullable: Boolean = false, name: String? = null) = Registry.current.resolve<T>(nullable, name)

inline fun <reified T> inject(name: String? = null) = object : ReadOnlyProperty<Any, T> {
    val registry = Registry.current
    var value: T? = null
    var init = false

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        synchronized(this) {
            if (!init) {
                value = registry.resolve<T>(property.returnType.isMarkedNullable, name)
                init = true
            }
        }
        return value as T
    }
}
