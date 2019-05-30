package com.softwaremill

import scalaz.zio.{DefaultRuntime, Fiber, IO, Queue, UIO}

object UsingZio {

  def coordinator(initialConnections: List[DeviceConnection]): IO[Nothing, CoordinatorResult] = {

    def next(inbox: Queue[CoordinatorMsg], connections: List[DeviceConnection], waiting: List[Commands]): IO[Nothing, Unit] = {
      (connections, waiting) match {
        case (connection :: otherConnections, firstWaiting :: otherWaiting) =>
          UIO(println(s"Spawning using ${connection.id}")).andThen(
            laserCutter(firstWaiting, connection)
              .catchAll {
                e: LaserCutterException => UIO(println(s"ERROR: $e"))
              }
              .ensuring(inbox.offer(ConnectionFree(connection)))
              .fork
              .andThen(receive(inbox, otherConnections, otherWaiting)))

        case _ =>
          receive(inbox, connections, waiting)
      }
    }

    def receive(inbox: Queue[CoordinatorMsg], connections: List[DeviceConnection], waiting: List[Commands]): IO[Nothing, Unit] = {
      inbox
        .take
        .flatMap {
          case ConnectionFree(connection) => next(inbox, connection :: connections, waiting)
          case RunCommands(commands) => next(inbox, connections, commands :: waiting)
        }
    }

    for {
      inbox <- Queue.unbounded[CoordinatorMsg]
      fiber <- receive(inbox, initialConnections, Nil).fork
    } yield CoordinatorResult(inbox, fiber)
  }

  def laserCutter0(commands: Commands, connection: DeviceConnection): IO[LaserCutterException, Unit] = {
    commands match {
      case Nil =>
        IO.unit
      case command :: otherCommands =>
        connection.runCommand(command).andThen(laserCutter0(otherCommands, connection))
    }
  }

  def laserCutter(commands: Commands, connection: DeviceConnection): IO[LaserCutterException, Unit] = {
    IO.collectAll(commands.map(connection.runCommand)).unit
  }

  //

  sealed trait CoordinatorMsg

  case class ConnectionFree(connection: DeviceConnection) extends CoordinatorMsg

  case class RunCommands(commands: Commands) extends CoordinatorMsg

  case class CoordinatorResult(inbox: Queue[CoordinatorMsg], fiber: Fiber[Nothing, Unit])

  //

  type Commands = List[String]

  case class DeviceConnection(id: String) {
    def runCommand(command: String): IO[LaserCutterException, Unit] = IO {
      if (command == "turtle up") {
        throw new LaserCutterException("There's no turtle")
      } else {
        println(s"[$id] Processing $command")
      }
    }.refineOrDie { case e: LaserCutterException => e }
  }

  class LaserCutterException(msg: String) extends Exception(msg)

  //

  def main(args: Array[String]): Unit = {
    new DefaultRuntime {}.unsafeRun(for {
      r <- coordinator(List(DeviceConnection("lc1"), DeviceConnection("lc2")))
      _ <- r.inbox.offer(RunCommands(List("fw 10", "right 90", "fw 10", "right 90", "fw 10", "right 90")))
      _ <- r.inbox.offer(RunCommands(List("fw 100", "turtle up")))
      _ <- r.inbox.offer(RunCommands(List("bk 1", "bk 1", "bk 1", "bk 1", "bk 1")))
      _ <- r.inbox.offer(RunCommands(List("left 1", "fw 1", "left 1", "fw 1", "left 1", "fw 1", "left 1", "fw 1")))
      _ <- r.fiber.join
    } yield ())
  }

}
