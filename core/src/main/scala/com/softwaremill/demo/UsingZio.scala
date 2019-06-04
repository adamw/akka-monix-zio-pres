package com.softwaremill.demo

import scalaz.zio._

object UsingZio {

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

  sealed trait CoordinatorMsg

  case class ConnectionFree(connection: DeviceConnection) extends CoordinatorMsg

  case class RunCommands(commands: Commands) extends CoordinatorMsg

  //

  case class CoordinatorResult(inbox: Queue[CoordinatorMsg], fiber: Fiber[Nothing, Unit])

  //
}
