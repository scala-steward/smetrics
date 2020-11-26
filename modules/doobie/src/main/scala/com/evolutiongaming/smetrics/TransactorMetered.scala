package com.evolutiongaming.smetrics

import java.sql.Connection

import cats.data.Kleisli
import cats.syntax.all._
import com.evolutiongaming.catshelper.BracketThrowable
import doobie.free.connection.ConnectionOp
import doobie.util.transactor.{Interpreter, Transactor}

object TransactorMetered {

  def apply[F[_]: BracketThrowable: MeasureDuration](
    transactor: Transactor[F],
    metrics: DoobieMetrics[F]
  ): Transactor[F] =
    transactor.copy(interpret0 = new Interpreter[F] {
      override def apply[A](fa: ConnectionOp[A]): Kleisli[F, Connection, A] =
        transactor.interpret(fa).mapF { query =>
          for {
            start    <- MeasureDuration[F].start
            result   <- query.attempt
            duration <- start
            _        <- metrics.query(duration, result.isRight)
            result   <- result.liftTo[F]
          } yield result
        }
    })
}
