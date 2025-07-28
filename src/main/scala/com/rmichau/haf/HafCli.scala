package com.rmichau.haf

import com.monovore.decline._
import cats.implicits._
case class HafConfig(
  filter: String,
  rmHost: String,
  jhHost: String,
  shHost: String,
  useRM: Boolean,
  useJH: Boolean,
  useSH: Boolean
)

object HafCli {
  val filterOpt: Opts[String] = Opts.argument[String]("filter")
  val rmHostOpt: Opts[String] = Opts.option[String]("rmHost", help = "ResourceManager host")
  val jhHostOpt: Opts[String] = Opts.option[String]("jhHost", help = "JobHistory host")
  val shHostOpt: Opts[String] = Opts.option[String]("shHost", help = "SparkHistory host")

  val useRMOpt: Opts[Boolean] = Opts.flag("useRM", help = "Query ResourceManager").orFalse
  val useJHOpt: Opts[Boolean] = Opts.flag("useJH", help = "Query JobHistory").orFalse
  val useSHOpt: Opts[Boolean] = Opts.flag("useSH", help = "Query SparkHistory").orFalse

  val hafConfig: Opts[HafConfig] =
    (filterOpt, rmHostOpt, jhHostOpt, shHostOpt, useRMOpt, useJHOpt, useSHOpt).mapN { (filter, rmHost, jhHost, shHost, useRM, useJH, useSH) =>
      val anyFlag = useRM || useJH || useSH
      HafConfig(
        filter,
        rmHost,
        jhHost,
        shHost,
        if (anyFlag) useRM else true,
        if (anyFlag) useJH else true,
        if (anyFlag) useSH else true
      )
    }
}
