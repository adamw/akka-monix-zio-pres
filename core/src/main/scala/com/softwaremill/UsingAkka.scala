package com.softwaremill

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object UsingAkka {

  def laserCutterCoordinator(initialConnections: List[DeviceConnection]): Behavior[CoordinatorMsg] = {
    Behaviors.setup[CoordinatorMsg] { ctx =>

      def next(connections: List[DeviceConnection], waiting: List[Commands], counter: Long): Behavior[CoordinatorMsg] = {
        (connections, waiting) match {
          case (connection :: otherConnections, firstWaiting :: otherWaiting) =>
            println(s"Spawning $counter")
            val lc = ctx.spawn(laserCutter(ctx.self, firstWaiting, connection), s"lc-$counter")
            lc ! NextCommand()
            receive(otherConnections, otherWaiting, counter + 1)
          case _ =>
            receive(connections, waiting, counter)
        }
      }

      def receive(connections: List[DeviceConnection], waiting: List[Commands], counter: Long): Behavior[CoordinatorMsg] = Behaviors
        .receiveMessage[CoordinatorMsg] {
          case ConnectionFree(connection) => next(connection :: connections, waiting, counter)
          case RunCommands(commands) => next(connections, commands :: waiting, counter)
        }

      receive(initialConnections, Nil, 0)
    }
  }

  def laserCutter(coordinator: ActorRef[ConnectionFree], commands: Commands, connection: DeviceConnection): Behavior[LaserCutterMsg] =
    Behaviors.setup[LaserCutterMsg] { ctx =>
      Behaviors.receiveMessage {
        case NextCommand() =>
          commands match {
            case Nil =>
              coordinator ! ConnectionFree(connection)
              Behaviors.stopped

            case command :: otherCommands =>
              connection.runCommand(command).onComplete {
                case Success(_) => ctx.self ! NextCommand()
                case Failure(t) => ctx.self ! Error(t)
              }
              laserCutter(coordinator, otherCommands, connection)
          }
        case Error(t) =>
          println(s"ERROR: $t")
          coordinator ! ConnectionFree(connection)
          Behaviors.stopped
      }
    }

  //

  sealed trait CoordinatorMsg

  case class ConnectionFree(connection: DeviceConnection) extends CoordinatorMsg

  case class RunCommands(commands: Commands) extends CoordinatorMsg

  sealed trait LaserCutterMsg

  case class NextCommand() extends LaserCutterMsg

  case class Error(t: Throwable) extends LaserCutterMsg

  //

  type Commands = List[String]

  case class DeviceConnection(id: String) {
    def runCommand(command: String): Future[Unit] = Future {
      if (command == "turtle up") {
        throw new LaserCutterException("There's no turtle")
      } else {
        println(s"[$id] Processing $command")
      }
    }
  }

  class LaserCutterException(msg: String) extends Exception(msg)

  //

  val coordinator = ActorSystem(laserCutterCoordinator(List(DeviceConnection("lc1"), DeviceConnection("lc2"))), "hello")

  def main(args: Array[String]): Unit = {
    coordinator ! RunCommands(List("fw 10", "right 90", "fw 10", "right 90", "fw 10", "right 90"))
    coordinator ! RunCommands(List("fw 100", "turtle up"))
    coordinator ! RunCommands(List("bk 1", "bk 1", "bk 1", "bk 1", "bk 1"))
    coordinator ! RunCommands(List("left 1", "fw 1", "left 1", "fw 1", "left 1", "fw 1", "left 1", "fw 1"))
  }

}
