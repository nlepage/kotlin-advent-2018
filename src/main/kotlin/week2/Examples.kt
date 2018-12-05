package week2

import week2.di.*

fun main(args: Array<String>) {
    registry {
        provider { "coucou" } named "toto"
        optionalProvider { "aze" }
        value(1)
        optionalValue(1)
        singleton { Math.random() }
        optionalSingleton<Double> { null }
        provider({ A() })
    }

    test()
    test()
}

fun test() {
    val a: A = get()
    println(a.s1)
    println(a.s2)
    println(a.d1)
    println(a.d2)
    println(a.i1)
    println(a.i2)
}

class A(val s1: String = get(name = "toto")) {
    val s2: String? by inject()
    val d1: Double by inject()
    val d2: Double? by inject()
    val i1: Int by inject()
    val i2: Int? by inject()
}
