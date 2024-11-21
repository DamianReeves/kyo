package kyo

import kyo.Actor.Service
import kyo.Parse.State
import kyo.kernel.Boundary
import kyo.kernel.Reducible
import scala.util.control.NoStackTrace

/** An actor prototype not considering aspects like identity and supervision. I think we could initially go with a minimal approach similar
  * to this one providing local isolated actors.
  *
  * Instead of leaving the decision to handle the result of an actor message to the caller, this prototype separates actors into two kinds:
  * Services, where each message has a response, and fire-and-forget Actions.
  *
  * Something interesting about this design is that actors would have mailboxes with limited size and, in case they're full, new messages
  * would asynchrously block the caller, providing backpressure between actors. I'm not sure if there are solutions that provide this
  * behavior, we'd need to think through the implications of that. Another option is allowing users to define sliding and dropping
  * mailboxes.
  */
object Actor:

    def send[A](pid: PID[A], message: Any)(using Frame): Unit < Async = ???

    // Actor services receive an input and return an output to be handled by the caller.
    // E => possible error types
    // A => input
    // B => output
    abstract class Service[E, A, B]:
        def apply(input: A)(using Frame): B < (Async & Abort[E | Closed])
        def close(using Frame): Maybe[Seq[A]] < Async

    object Service:
        // Note how the state is handled via Var. Ctx/boundary is Kyo's mechanism
        // to allow context effects (Env, Local, Resource, etc) when forking fibers.
        def init[State: Tag, E, A, B: Flat, Ctx](
            mailboxSize: Int,
            initialState: State
        )(
            f: A => B < (Var[State] & Abort[E] & Async & Ctx)
        )(
            using
            boundary: Boundary[Ctx, Async & Abort[E | Closed]],
            initFrame: Frame
        ): Service[E, A, B] < (IO & Ctx) =
            // Since a service needs to reply, the message saves the sender promise
            // to fulfill it later
            case class Message(value: A, sender: Promise[E | Closed, B])
            for
                mailbox  <- Channel.init[Message](mailboxSize, Access.MultiProducerSingleConsumer)
                consumer <-
                    // Initializes the consumer that keeps listening for new messages
                    val x =
                        Loop(initialState) { state =>
                            mailbox.take.map { message =>
                                // handle the message with the current state
                                Var.runTuple(state)(f(message.value)).map { (newState, b) =>
                                    // complete the sender with the result and resume loop with the new state
                                    message.sender.complete(Result.success(b)).andThen(Loop.continue(newState))
                                }
                            }
                        }
                    Async.run(x)
            yield
                new Service[E, A, B]:
                    override def apply(input: A)(using Frame) =
                        Promise.init[E | Closed, B].map { sender =>
                            // Just enqueue the message and return the promise
                            mailbox.put(Message(input, sender)).andThen(sender.get)
                        }
                    override def close(using frame: Frame) =
                        mailbox.close.map {
                            case Absent => Absent
                            case Present(backlog) =>
                                val closed = Closed("Actor service closed", initFrame, frame)
                                // Interrupt the consumer fiber
                                consumer.interrupt(Result.Panic(closed)).andThen {
                                    // Complete all pending messages with a Closed failure and
                                    // return the message backlog.
                                    val fail = Result.fail(closed)
                                    Kyo.foreach(backlog) { message =>
                                        message.sender.complete(fail).andThen(message.value)
                                    }.map(Maybe(_))
                                }
                        }
                end new
            end for
        end init
    end Service

    // Actions are essentially fire-and-forget services. Note how it can't define a failure type
    // since the caller won't wait for the processing.
    abstract class Action[A] extends Service[Nothing, A, Unit]

    object Action:
        def init[State: Tag, A, E, Ctx](
            mailboxSize: Int,
            initialState: State
        )(
            f: A => Unit < (Var[State] & Async & Ctx)
        )(
            using
            boundary: Boundary[Ctx, Async & Abort[Closed]],
            initFrame: Frame
        ): Action[A] < (IO & Ctx) =
            for
                mailbox  <- Channel.init[A](mailboxSize, Access.MultiProducerSingleConsumer)
                consumer <-
                    // Similar to the Service impl but simpler since it doesn't need to notify the sender
                    val x =
                        Loop(initialState) { state =>
                            mailbox.take.map { value =>
                                Var.run(state)(f(value).andThen(Var.get[State])).map(Loop.continue)
                            }
                        }
                    Async.run(x)
            yield
                new Action[A]:
                    override def apply(input: A)(using Frame) =
                        // Fire-and-forget
                        mailbox.put(input)
                    override def close(using frame: Frame) =
                        mailbox.close.map {
                            case Absent => Absent
                            case Present(backlog) =>
                                val failure = Closed("Actor action closed", initFrame, frame)
                                consumer.interrupt(Result.Panic(failure)).andThen(Maybe(backlog))
                        }
                end new
        end init
    end Action

    /// A mailbox is a queue of messages that an actor can process.
    abstract class Mailbox[A] extends Sender[A]:
        self =>
        // private[kyo] def sendSystemMessage(message: Any)(using Frame): Unit < Async
        // def call[Req <: A, Res](request: Req)(using Frame): Res < (Async & Abort[Closed])
        def put(message: A)(using Frame): Unit < (Async & Abort[errors.Stopped | Closed]) = ???
    end Mailbox

    object Mailbox:

    end Mailbox

    trait Sender[-A]:
        def send(message: A)(using Frame): Unit < Async
    end Sender

    abstract class PID[-A]:
        def number: String
        // TODO: Add Stopped, Shutdown, and Unreachable as possible Error types
        // def send(message: A)(using Frame): Unit < (Async & Abort[Throwable | Closed])
        final override def toString = s"#PID($number)"
    end PID

    object PID:
        private[kyo] def init[A](number0: String)(using Frame): PID[A] =
            new PID[A]:
                val number = number0
        end init
    end PID

    enum ProcessStatus:
        case Created, Initialized, Started, Stopped, Idle

    sealed abstract class Process[-Msg, State]:
        def pid: PID[Msg]
    end Process

    object Process:
        def initStateless[Msg](
            name: String
        )(f: StatelessReceiveFunction[Msg])(using Frame): Process[Msg, Unit] = ???
    end Process

    sealed abstract class Supervisor:
    end Supervisor

    object Supervisor:
    end Supervisor

    abstract class Behavior[-Msg, State]:
        def receive(message: Msg, state: State)(using Frame): State < (Async & Abort[Throwable | Closed])
    end Behavior

    object Behavior:
        def stateless[Msg](f: Msg => Unit < Async): Behavior[Msg, Unit] =
            new Behavior[Msg, Unit]:
                def receive(message: Msg, state: Unit)(using Frame) =
                    f(message)
    end Behavior

    opaque type StatelessReceiveFunction[-Msg] = Msg => Unit < (Async & Abort[StatelessReceiveFunction.Errors])
    object StatelessReceiveFunction:
        type Errors = errors.Stopped | errors.Unreachable
    end StatelessReceiveFunction

    object errors:
        case class Stopped(message: String, createdAt: Frame, failedAt: Frame)
            extends Exception(
                s"Process created at ${createdAt.position.show} is stopped. Failure at ${failedAt.position.show}: $message"
            )
            with NoStackTrace
        case class Unreachable(message: String, createdAt: Frame, failedAt: Frame)
            extends Exception(
                s"Process created at ${createdAt.position.show} is unreachable. Failure at ${failedAt.position.show}: $message"
            )
            with NoStackTrace
    end errors

    object internal:

    end internal
end Actor
