package pfdstest

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll

object LazySpecification extends Properties("Lazy")
{
  property("constructor") = forAll { (a: Int) =>
    Lazy(a).force == a
  }

  property("evaluated only once") = forAll { (s: String) =>
    var x = 0
    val l = Lazy { x += 1; s }
    l.force + l.force + l.force == s + s + s && x == 1
  }
}

object StreamSpecification extends Properties("Stream")
{
  property("cons") = forAll { (l: List[Int]) =>
    def check(s: Stream[Int], l: List[Int]): Boolean = {
      (s, l) match {
        case (Stream.Empty         , Nil       ) => true
        case (Stream.Empty         , _         ) => false
        case (_                    , Nil       ) => false
        case (Stream.Cons(h1, news), h2 :: newl) => if(h1 == h2) check(news.force, newl) else false
      }
    }
    check(Stream.fromList(l), l)
  }
}
