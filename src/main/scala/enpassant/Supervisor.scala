package enpassant

import akka.actor.{ActorLogging, Actor, ActorRef, Props}

import core._

class Supervisor(val config: Config) extends Actor with ActorLogging {
  import context.dispatcher

  val tickActor = config.router map {
    _ => context.actorOf(TickActor.props(config), TickActor.name)
  }
  tickActor.map(_ ! Tick)

  val model = context.actorOf(Model.props(config.mode), Model.name)
  val proxy = context.actorOf(Proxy.props(config, tickActor.isDefined), Proxy.name)
  val service = context.actorOf(Service.props(config), Service.name)

  def receive = {
    case _ =>
  }
}
