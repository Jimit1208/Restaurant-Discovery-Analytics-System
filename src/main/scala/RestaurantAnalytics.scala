import org.mongodb.scala.Document
import org.mongodb.scala.model.Aggregates._
import org.mongodb.scala.model.Accumulators._
import org.mongodb.scala.model.Sorts._

import scala.concurrent.Await
import scala.concurrent.duration._

class RestaurantAnalytics {

  private val collection =
    MongoConnection.restaurants

  private def wait[T](
      result: scala.concurrent.Future[Seq[Document]]
  ): Seq[Document] =
    Await.result(result, 30.seconds)

  // =====================================
  // RESTAURANTS BY CUISINE
  // =====================================

  def restaurantsByCuisine(): Seq[Document] = {

    wait(
      collection
        .aggregate(
          Seq(
            group(
              "$cuisine",
              sum("count", 1)
            ),
            sort(descending("count"))
          )
        )
        .toFuture()
    )
  }

  // =====================================
  // RESTAURANTS BY BOROUGH
  // =====================================

  def restaurantsByBorough(): Seq[Document] = {

    wait(
      collection
        .aggregate(
          Seq(
            group(
              "$borough",
              sum("count", 1)
            ),
            sort(descending("count"))
          )
        )
        .toFuture()
    )
  }

  // =====================================
  // GRADE DISTRIBUTION
  // =====================================

  def gradeDistribution(): Seq[Document] = {

    wait(
      collection
        .aggregate(
          Seq(
            unwind("$grades"),
            group(
              "$grades.grade",
              sum("count", 1)
            ),
            sort(descending("count"))
          )
        )
        .toFuture()
    )
  }

  // =====================================
  // TOP RATED RESTAURANTS
  // =====================================

  def topRatedRestaurants(): Seq[Document] = {

    wait(
      collection
        .aggregate(
          Seq(
            unwind("$grades"),
            sort(ascending("grades.score")),
            limit(10),
            project(
              Document(
                "name" -> 1,
                "borough" -> 1,
                "cuisine" -> 1,
                "score" -> "$grades.score"
              )
            )
          )
        )
        .toFuture()
    )
  }
    // =====================================
  // AVERAGE SCORE BY CUISINE
  // =====================================

  def averageScoreByCuisine(): Seq[Document] = {

    wait(
      collection
        .aggregate(
          Seq(
            unwind("$grades"),
            group(
              "$cuisine",
              avg("averageScore", "$grades.score"),
              sum("ratings", 1)
            ),
            sort(descending("averageScore"))
          )
        )
        .toFuture()
    )
  }
}