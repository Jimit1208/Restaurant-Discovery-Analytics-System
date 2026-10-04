case class Address(
  building: String,
  coord: List[Double],
  street: String,
  zipcode: String
)

case class Grade(
  date: String,
  grade: String,
  score: Int
)

case class Restaurant(
  name: String,
  borough: String,
  cuisine: String,
  restaurantId: String,
  address: Option[Address] = None,
  grades: List[Grade] = Nil
)