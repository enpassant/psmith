package core

import nl.grons.metrics.scala.InstrumentedBuilder
import com.codahale.metrics.MetricRegistry

trait Instrumented extends InstrumentedBuilder {
    val metricRegistry = Instrumented.metricRegistry
}

object Instrumented {
    val metricRegistry = new MetricRegistry()
}
// vim: set ts=4 sw=4 et:
