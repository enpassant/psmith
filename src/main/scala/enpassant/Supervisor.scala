package enpassant

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import core._

class Supervisor(val config: Config) extends Actor with ActorLogging {
  import context.dispatcher

  val tickActor = config.router map {
    _ => context.actorOf(TickActor.props(config), TickActor.name)
  }

  val model = context.actorOf(Model.props(config.mode), Model.name)
  val proxy = context.actorOf(Proxy.props(config), Proxy.name)
  val service = context.actorOf(Service.props(config), Service.name)

  def receive = {
    case _ =>
  }
}

object Supervisor {
  val actorSystem = ActorSystem("james")
  def props(config: Config) = Props(new Supervisor(config))
  def name = "supervisor"

  def getChild(childName: String) = actorSystem.actorSelection(s"/user/$name/$childName")
}
