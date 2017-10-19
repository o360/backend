package controllers.admin

import java.nio.file.Path

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.test.FakeEnvironment
import controllers.BaseControllerTest
import org.mockito.Mockito._
import play.api.libs.Files
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import services.{FileService, UserService}
import silhouette.DefaultEnv
import testutils.fixture.UserFixture

import scalaz.\/-

/**
  * Admin profile picture controller test.
  */
class AdminProfilePictureControllerTest extends BaseControllerTest {

  private case class TestFixture(
    silhouette: Silhouette[DefaultEnv],
    fileService: FileService,
    userService: UserService,
    controller: ProfilePictureController
  )

  private def getFixture(environment: FakeEnvironment[DefaultEnv]) = {
    val silhouette = getSilhouette(environment)
    val fileService = mock[FileService]
    val userService = mock[UserService]
    val controller = new ProfilePictureController(silhouette, fileService, userService, cc, ec)
    TestFixture(silhouette, fileService, userService, controller)
  }

  "upload" should {
    "upload and set user picture" in {
      val env = fakeEnvironment(UserFixture.admin)
      val fixture = getFixture(env)

      val filePath = mock[Path]

      val file: Files.TemporaryFile = new Files.TemporaryFile() {
        override def path = filePath
        override def file = ???
        override def temporaryFileCreator = ???
      }

      val filename = "filename.jpg"
      val data =
        MultipartFormData(Map(), files = Seq(MultipartFormData.FilePart("picture", filename, None, file)), Nil)

      val request = authenticated(
        FakeRequest()
          .withBody[MultipartFormData[Files.TemporaryFile]](data),
        env
      )

      val userId = 2

      when(fixture.userService.getById(userId)).thenReturn(toSuccessResult(UserFixture.user))
      val pictureName = "new name"
      when(fixture.fileService.upload(filename, filePath, fixture.controller.validExtensions))
        .thenReturn(\/-(pictureName))
      when(fixture.userService.update(UserFixture.user.copy(pictureName = Some(pictureName)), true)(UserFixture.admin))
        .thenReturn(toSuccessResult(UserFixture.user))

      val response = fixture.controller.upload(userId).apply(request)
      status(response) mustBe NO_CONTENT
    }

    "return bad request if file not passed" in {
      val env = fakeEnvironment(UserFixture.admin)
      val fixture = getFixture(env)

      val data = MultipartFormData(Map(), files = Seq.empty[FilePart[Files.TemporaryFile]], Nil)

      val request = authenticated(
        FakeRequest()
          .withBody[MultipartFormData[Files.TemporaryFile]](data),
        env
      )

      val response = fixture.controller.upload(123).apply(request)
      status(response) mustBe BAD_REQUEST
    }
  }
}
