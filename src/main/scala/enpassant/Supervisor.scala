package enpassant

import akka.actor.{ActorLogging, Actor, ActorRef, Props}

import core._

class Supervisor(val config: Config) extends Actor with ActorLogging {
  import context.dispatcher

  val tickActor = config.router map {
    _ => context.actorOf(Props(new TickActor(config)))
  }
  tickActor.map(_ ! Tick)

  val model = context.actorOf(Props(new Model(config.mode)))
  val proxy = context.actorOf(Props(new Proxy(config, model, tickActor)))
  //val service = context.actorOf(Props(new Service(config, model)))

  def receive = {
    case _ =>
  }
}
