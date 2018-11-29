package ru.pavkin.booking

import cats.effect._
import cats.implicits._
import monix.eval.{Task, TaskApp}

object App extends TaskApp {

  val app = new AppF[Task]

  def run(args: List[String]): Task[ExitCode] = app.run.use(_ => Task.never).as(ExitCode.Success)
}
