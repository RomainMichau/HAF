package com.rmichau.haf

import com.monovore.decline._
import cats.implicits._
case class HafConfig(
  filter: String,
  rmHost: String,
  jhHost: String,
  shHost: String
)

object HafCli {
  val filterOpt: Opts[String] = Opts.argument[String]("filter")
  val rmHostOpt: Opts[String] = Opts.option[String]("rmHost", help = "ResourceManager host")
  val jhHostOpt: Opts[String] = Opts.option[String]("jhHost", help = "JobHistory host")
  val shHostOpt: Opts[String] = Opts.option[String]("shHost", help = "SparkHistory host")

  val hafConfig: Opts[HafConfig] =
    (filterOpt, rmHostOpt, jhHostOpt, shHostOpt).mapN(HafConfig.apply)
}
