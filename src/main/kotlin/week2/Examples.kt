package week2

import week2.di.*

fun main(args: Array<String>) {
    val testPrinter = TestPrinter()

    registry {
        // Default provider for A
        provider<A> { AImpl() }

        // Default provider for Printer is a singleton
        singleton<Printer> { StdOutPrinter() }

        // Provider for A named testA with testPrinter injected in constructor
        provider<A> { AImpl(get(name = "testPrinter")) } named "testA"

        // Provider for Printer named "testPrinter" with value testPrinter
        value<Printer>(testPrinter) named "testPrinter"

        // Random double provider named rnd
        provider { Math.random() } named "rnd"

        // Random double singleton named globalRnd
        singleton { Math.random() } named "globalRnd"

        // String provider
        provider<String> { "Hello !" }

        // Optional string provider
        optionalProvider<String> { null }

        // Redefining a provider for same type (type includes nullability) and same name will throw an error:
        // provider<String> { "Hi !" }

        // All the registry is computed and if it contains several errors, all errors will be displayed
    }

    // Get default A
    val a: A = get()
    // Write properties using default printer
    a.printProperties()

    // Get test A
    val testA: A = get(name = "testA")
    // Write properties using test printer
    testA.printProperties()

    // Display
    println("testPrinter: ${testPrinter.lines}")
}

interface A {
    fun printProperties()
}

// Default B injected in constructor
class AImpl(val printer: Printer = get()) : A {
    val string: String by inject()
    val nullString: String? by inject()
    val rnd: Double by inject(name = "rnd")
    val globalRnd: Double by inject(name = "globalRnd")

    override fun printProperties() {
        printer.println("string: $string")
        printer.println("nullString: $nullString")
        printer.println("rnd: $rnd")
        printer.println("globalRnd: $globalRnd")
    }
}

interface Printer {
    fun println(s: String)
}

class StdOutPrinter : Printer {
    override fun println(s: String) = kotlin.io.println(s)
}

class TestPrinter : Printer {
    val lines: MutableList<String> = mutableListOf()

    override fun println(s: String) {
        lines += s
    }
}