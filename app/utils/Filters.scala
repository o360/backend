package utils

import javax.inject.Inject

import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter

/**
  * Play filters. https://www.playframework.com/documentation/2.5.x/ScalaHttpFilters
  */
class Filters @Inject()(corsFilter: CORSFilter) extends DefaultHttpFilters(corsFilter)
