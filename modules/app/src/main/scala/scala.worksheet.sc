case class Yo(bla: String)

def hello =
  List(Yo("da"), Yo("-Yo ma"), Yo("-yo")).groupBy(_.bla).map((k, xs) => xs)
