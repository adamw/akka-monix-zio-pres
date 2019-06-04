package com.softwaremill.demo

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object UsingAkka {

  //

  type Commands = List[String]

  class LaserCutterException(msg: String) extends Exception(msg)

  case class DeviceConnection(id: String) {
    def runCommand(command: String): Future[Unit] = Future {
      if (command == "turtle up") {
        throw new LaserCutterException("There's no turtle")
      } else {
        println(s"[$id] Processing $command")
      }
    }
  }

  //

}
