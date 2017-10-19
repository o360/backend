package testutils

import scala.reflect.ClassTag
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any => mockitoAny, eq => mockitoEq}

/**
  * Helper for mockito mocking framework.
  */
trait MockitoHelper {

  /**
    * Returns argumentCaptor for given class.
    */
  def argumentCaptor[A](implicit classTag: ClassTag[A]): ArgumentCaptor[A] = {
    ArgumentCaptor.forClass(classTag.runtimeClass.asInstanceOf[Class[A]])
  }

  /**
    * Scala version for mockitoAny matcher.
    */
  def *[A]: A = mockitoAny[A]

  /**
    * Scala version for mockito eq matcher.
    */
  def eqTo[A](a: A): A = mockitoEq(a)
}
