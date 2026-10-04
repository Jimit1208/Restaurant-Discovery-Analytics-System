import scala.io.StdIn

object Main {

  private val repository = new RestaurantRepository()
  private val analytics = new RestaurantAnalytics()

  def main(args: Array[String]): Unit = {

    println()
    println("==============================================")
    println("   RESTAURANT DISCOVERY & ANALYTICS SYSTEM")
    println("==============================================")

    try {

      repository.createIndexes()

      var running = true

      while (running) {

        showMenu()

        val choice =
          StdIn.readLine("Enter your choice: ").trim

        choice match {

          case "1" =>
            addRestaurant()

          case "2" =>
            searchRestaurants()

          case "3" =>
            updateRestaurant()

          case "4" =>
            deleteRestaurant()

          case "5" =>
            analyticsMenu()

          case "6" =>
            showIndexes()

          case "7" =>
            running = false
            println("\nThank you for using the system.")

          case _ =>
            println("\nInvalid choice. Please select 1-7.")
        }
      }

    } catch {

      case exception: Exception =>
        println("\nApplication error:")
        println(exception.getMessage)

    } finally {

      MongoConnection.close()
    }
  }

  // =====================================
  // MAIN MENU
  // =====================================

  private def showMenu(): Unit = {

    println()
    println("----------------------------------------------")
    println("                    MENU")
    println("----------------------------------------------")
    println("1. Add Restaurant")
    println("2. Search/View Restaurants")
    println("3. Update Restaurant")
    println("4. Delete Restaurant")
    println("5. Restaurant Analytics")
    println("6. Index Information")
    println("7. Exit")
    println("----------------------------------------------")
  }

  // =====================================
  // CREATE
  // =====================================

  private def addRestaurant(): Unit = {

    println("\n--- ADD RESTAURANT ---")

    val name =
      StdIn.readLine("Restaurant name: ")

    val borough =
      StdIn.readLine("Borough: ")

    val cuisine =
      StdIn.readLine("Cuisine: ")

    val id =
      StdIn.readLine("Restaurant ID: ")

    val zipcode =
      StdIn.readLine("ZIP code: ")

    val success =
      repository.addRestaurant(
        name,
        borough,
        cuisine,
        id,
        zipcode
      )

    if (success)
      println("Restaurant added successfully.")
    else
      println("Could not add restaurant.")
  }

  // =====================================
  // SEARCH
  // =====================================

  private def searchRestaurants(): Unit = {

    println("\n--- SEARCH RESTAURANTS ---")
    println("1. Search by ID")
    println("2. Search by Name")
    println("3. Search by Cuisine")
    println("4. Search by Borough")
    println("5. Search by ZIP code")

    val choice =
      StdIn.readLine("Enter choice: ")

    val results =
      choice match {

        case "1" =>
          repository
            .findById(
              StdIn.readLine("Restaurant ID: ")
            )
            .toSeq

        case "2" =>
          repository.searchByName(
            StdIn.readLine("Restaurant name: ")
          )

        case "3" =>
          repository.searchByCuisine(
            StdIn.readLine("Cuisine: ")
          )

        case "4" =>
          repository.searchByBorough(
            StdIn.readLine("Borough: ")
          )

        case "5" =>
          repository.searchByZip(
            StdIn.readLine("ZIP code: ")
          )

        case _ =>
          println("Invalid choice.")
          Seq.empty
      }

    println(s"\nFound ${results.size} restaurant(s).")

    results.take(20).foreach { restaurant =>
      println(restaurant.toJson)
    }
  }

  // =====================================
  // UPDATE
  // =====================================

  private def updateRestaurant(): Unit = {

    println("\n--- UPDATE RESTAURANT ---")

    val id =
      StdIn.readLine("Restaurant ID: ")

    val cuisine =
      StdIn.readLine("New cuisine: ")

    if (repository.updateCuisine(id, cuisine))
      println("Restaurant updated successfully.")
    else
      println("Restaurant not found or unchanged.")
  }

  // =====================================
  // DELETE
  // =====================================

  private def deleteRestaurant(): Unit = {

    println("\n--- DELETE RESTAURANT ---")

    val id =
      StdIn.readLine("Restaurant ID: ")

    val confirmation =
      StdIn.readLine("Confirm deletion? (y/n): ")

    if (confirmation.equalsIgnoreCase("y")) {

      if (repository.deleteRestaurant(id))
        println("Restaurant deleted successfully.")
      else
        println("Restaurant not found.")

    } else {

      println("Deletion cancelled.")
    }
  }

  // =====================================
  // ANALYTICS
  // =====================================

  private def analyticsMenu(): Unit = {

    var back = false

    while (!back) {

      println()
      println("--- RESTAURANT ANALYTICS ---")
      println("1. Restaurants by Cuisine")
      println("2. Restaurants by Borough")
      println("3. Grade Distribution")
      println("4. Top Rated Restaurants")
      println("5. Back")

      val choice =
        StdIn.readLine("Enter choice: ")

      choice match {

        case "1" =>
          printResults(
            analytics.restaurantsByCuisine()
          )

        case "2" =>
          printResults(
            analytics.restaurantsByBorough()
          )

        case "3" =>
          printResults(
            analytics.gradeDistribution()
          )

        case "4" =>
          printResults(
            analytics.topRatedRestaurants()
          )

        case "5" =>
          back = true

        case _ =>
          println("Invalid choice.")
      }
    }
  }

  // =====================================
  // INDEXES
  // =====================================

  private def showIndexes(): Unit = {

    println("\n--- MONGODB INDEXES ---")

    repository
      .getIndexes()
      .foreach(index => println(index.toJson))
  }

  // =====================================
  // PRINT RESULTS
  // =====================================

  private def printResults(
      results: Seq[org.mongodb.scala.Document]
  ): Unit = {

    println()

    results.foreach { result =>
      println(result.toJson)
    }
  }
}