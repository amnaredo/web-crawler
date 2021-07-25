import $file.FetchLinks, FetchLinks._
import $file.FetchLinksAsync, FetchLinksAsync._
import scala.concurrent._, duration.Duration.Inf, java.util.concurrent.Executors
implicit val ec = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))

def fetchAllLinksAsync(startTitle: String, depth: Int): Future[Set[String]] = {
    def rec(current: Set[String], seen: Set[String], recDepth: Int): Future[Set[String]] = {
        if (recDepth >= depth) Future.successful(seen)
        else {
            val futures = for (title <- current) yield fetchLinksAsync(title)
            Future.sequence(futures).map{nextTitleLists =>
                val nextTitles = nextTitleLists.flatten
                rec(nextTitles.filter(!seen.contains(_)), seen ++ nextTitles, recDepth + 1)
            }.flatten
        }
    }
    rec(Set(startTitle), Set(startTitle), 0)
}

def fetchAllLinksRec(startTitle: String, depth: Int): Set[String] = {
    def rec(current: Set[String], seen: Set[String], recDepth: Int): Set[String] = {
        if (recDepth >= depth) seen
        else {
            val futures = for (title <- current) yield Future{ fetchLinks(title) }
            val nextTitles = futures.map(Await.result(_, Inf)).flatten
            rec(nextTitles.filter(!seen.contains(_)), seen ++ nextTitles, recDepth + 1)
        }
    }
    rec(Set(startTitle), Set(startTitle), 0)
}

def fetchAllLinksParallel(startTitle: String, depth: Int): Set[String] = {
  var seen = Set(startTitle)
  var current = Set(startTitle)
  for (i <- Range(0, depth)) {
    val futures = for (title <- current) yield Future { fetchLinks(title) }
    val nextTitleLists = futures.map(Await.result(_, Inf))
    current = nextTitleLists.flatten.filter(!seen.contains(_))
    seen = seen ++ current
  }
  seen
}

def fetchAllLinks(startTitle: String, depth: Int): Set[String] = {
  var seen = Set(startTitle)
  var current = Set(startTitle)
  for (i <- Range(0, depth)) {
     val nextTitleLists = for (title <- current) yield fetchLinks(title) 
    current = nextTitleLists.flatten.filter(!seen.contains(_))
    seen = seen ++ current
  }
  seen
}