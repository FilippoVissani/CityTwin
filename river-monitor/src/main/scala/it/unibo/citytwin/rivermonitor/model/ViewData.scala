package it.unibo.citytwin.rivermonitor.model

import upickle.default.ReadWriter

enum ViewState derives ReadWriter:
  case Safe, Evacuating

case class ViewData(state: ViewState) derives ReadWriter
