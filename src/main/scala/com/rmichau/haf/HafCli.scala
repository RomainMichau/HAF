package com.rmichau.haf

import com.monovore.decline._
import cats.implicits._
case class HafConfig(
  filter: String,
  rmHost: Option[String],
  jhHost: Option[String],
  shHost: Option[String],
)

object HafCli {
  val filterOpt: Opts[String] = Opts.argument[String]("filter")
  val rmHostOpt: Opts[Option[String]] = Opts.option[String]("rmHost", help = "ResourceManager host").orNone
  val jhHostOpt: Opts[Option[String]] = Opts.option[String]("jhHost", help = "JobHistory host").orNone
  val shHostOpt: Opts[Option[String]] = Opts.option[String]("shHost", help = "SparkHistory host").orNone

  val hafConfig: Opts[HafConfig] =
    (filterOpt, rmHostOpt, jhHostOpt, shHostOpt).mapN { (filter, rmHost, jhHost, shHost) =>
      HafConfig(
        filter,
        rmHost,
        jhHost,
        shHost,
      )
    }
}
