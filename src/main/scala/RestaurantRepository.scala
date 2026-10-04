import org.mongodb.scala.Document
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates._
import org.mongodb.scala.model.Indexes._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try

class RestaurantRepository {

  private val collection: MongoCollection[Document] =
    MongoConnection.restaurants

  private def wait[T](result: scala.concurrent.Future[T]): T =
    Await.result(result, 15.seconds)

  // =========================
  // CREATE
  // =========================

  def addRestaurant(
      name: String,
      borough: String,
      cuisine: String,
      restaurantId: String,
      zipcode: String
  ): Boolean = {

    try {
      val document = Document(
        "name" -> name,
        "borough" -> borough,
        "cuisine" -> cuisine,
        "restaurant_id" -> restaurantId,
        "address" -> Document(
          "zipcode" -> zipcode
        ),
        "grades" -> List.empty[Document]
      )

      wait(collection.insertOne(document).toFuture())
      true

    } catch {
      case _: Exception => false
    }
  }

  // =========================
  // READ BY ID
  // =========================

  def findById(id: String): Option[Document] = {

    wait(
      collection
        .find(equal("restaurant_id", id))
        .first()
        .toFutureOption()
    )
  }

  // =========================
  // SEARCH BY NAME
  // =========================

  def searchByName(name: String): Seq[Document] = {

    wait(
      collection
        .find(regex("name", name, "i"))
        .toFuture()
    )
  }

  // =========================
  // SEARCH BY CUISINE
  // =========================

  def searchByCuisine(cuisine: String): Seq[Document] = {

    wait(
      collection
        .find(equal("cuisine", cuisine))
        .toFuture()
    )
  }

  // =========================
  // SEARCH BY BOROUGH
  // =========================

  def searchByBorough(borough: String): Seq[Document] = {

    wait(
      collection
        .find(equal("borough", borough))
        .toFuture()
    )
  }

  // =========================
  // SEARCH BY ZIP CODE
  // =========================

  def searchByZip(zipcode: String): Seq[Document] = {

    wait(
      collection
        .find(equal("address.zipcode", zipcode))
        .toFuture()
    )
  }

  // =========================
  // UPDATE
  // =========================

  def updateCuisine(
      id: String,
      newCuisine: String
  ): Boolean = {

    val result =
      wait(
        collection
          .updateOne(
            equal("restaurant_id", id),
            set("cuisine", newCuisine)
          )
          .toFuture()
      )

    result.getModifiedCount > 0
  }

  // =========================
  // DELETE
  // =========================

  def deleteRestaurant(id: String): Boolean = {

    val result =
      wait(
        collection
          .deleteOne(equal("restaurant_id", id))
          .toFuture()
      )

    result.getDeletedCount > 0
  }

  // =========================
  // INDEXES
  // =========================

  def createIndexes(): Unit = {

    wait(
      collection.createIndex(ascending("name")).toFuture()
    )

    wait(
      collection.createIndex(ascending("cuisine")).toFuture()
    )

    wait(
      collection
        .createIndex(
          compoundIndex(
            ascending("borough"),
            ascending("cuisine")
          )
        )
        .toFuture()
    )

    println("Indexes created successfully.")
  }

  // =========================
  // SHOW INDEXES
  // =========================

  def getIndexes(): Seq[Document] = {

    wait(
      collection
        .listIndexes()
        .toFuture()
    )
  }

  // =========================
  // GET ALL RESTAURANTS
  // =========================

  def getRestaurants(limit: Int = 50): Seq[Document] = {

    wait(
      collection
        .find()
        .limit(limit)
        .toFuture()
    )
  }

  // =========================
  // COUNT
  // =========================

  def countRestaurants(): Long = {

    wait(
      collection
        .countDocuments()
        .toFuture()
    )
  }

  // =========================
  // SEARCH WITH MULTIPLE FILTERS
  // =========================

  def search(
      name: Option[String],
      cuisine: Option[String],
      borough: Option[String],
      zipcode: Option[String]
  ): Seq[Document] = {

    val filters =
      Seq(
        name.map(value => regex("name", value, "i")),
        cuisine.map(value => regex("cuisine", value, "i")),
        borough.map(value => regex("borough", value, "i")),
        zipcode.map(value => equal("address.zipcode", value))
      ).flatten

    val filter =
      if (filters.isEmpty) {
        Document()
      } else {
        and(filters: _*)
      }

    wait(
      collection
        .find(filter)
        .limit(100)
        .toFuture()
    )
  }
}