package kyo

import java.util.concurrent.ConcurrentHashMap
import kyo.*
import kyo.Actor.PID
import kyo.Actor.StatelessReceiveFunction
import kyo.Actor.errors.*

abstract class Actors:
    private val registry = Actors.internal.Registry()
    def spawn[Msg](name: String)(receive: StatelessReceiveFunction[Msg])(using Frame): PID[Msg] =
        val proc = Actor.Process.initStateless(name)(receive)
        proc.pid

end Actors

object Actors:
    def init(name: String): Actors =
        val actors = internal.instances.computeIfAbsent(name, _ => new Actors {})
        return actors

    private[kyo] object internal:
        val instances = new ConcurrentHashMap[String, Actors]()

        class Registry:
            private val pids = new ConcurrentHashMap[String, PID[?]]()

        class DeadLetters(using Frame):
            import DeadLetters.DeadLetter
            private val dlq = Channel.init[DeadLetter](100, Access.MultiProducerSingleConsumer)
        end DeadLetters
        object DeadLetters:
            final case class DeadLetter(payload: Any, timestamp: Instant, target: PID[?])
        end DeadLetters
    end internal
end Actors
