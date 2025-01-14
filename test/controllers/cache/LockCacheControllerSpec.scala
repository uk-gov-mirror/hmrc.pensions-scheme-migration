/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.cache

import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfter, MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.LockCacheRepository
import repositories.models.MigrationLock
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class LockCacheControllerSpec extends WordSpec with MustMatchers with MockitoSugar with BeforeAndAfter {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val repo = mock[LockCacheRepository]
  private val authConnector: AuthConnector = mock[AuthConnector]
  private val id = "id"
  private val pstr = "pstr"
  private val psaId = "A2222222"
  private val lock: MigrationLock = MigrationLock(pstr, id, psaId)
  private val fakeRequest = FakeRequest().withHeaders("pstr" -> pstr, "psaId" -> psaId)
  private val fakePostRequest = FakeRequest("POST", "/").withHeaders("pstr" -> pstr, "psaId" -> psaId)

  private val modules: Seq[GuiceableModule] = Seq(
    bind[AuthConnector].toInstance(authConnector),
    bind[LockCacheRepository].toInstance(repo)
  )

  before {
    reset(repo)
    reset(authConnector)
  }

  "LockCacheController" when {
    "calling getLockOnScheme" must {
      "return OK with the data" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(repo.getLockByPstr(eqTo(pstr))) thenReturn Future.successful(Some(lock))
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller.getLockOnScheme(fakeRequest)
        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(lock)
      }

      "return NOT FOUND when the data doesn't exist" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(repo.getLockByPstr(eqTo(pstr))) thenReturn Future.successful(None)
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller.getLockOnScheme(fakeRequest)
        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(repo.getLockByPstr(eqTo(pstr))) thenReturn Future.failed(new Exception())
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller.getLockOnScheme(fakeRequest)
        an[Exception] must be thrownBy status(result)
      }

      "throw an exception when the call is not authorised" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed(new Exception())

        val result = controller.getLockOnScheme(fakeRequest)
        an[Exception] must be thrownBy status(result)
      }

    }

    "calling getLock" must {
      "return OK with the data" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(repo.getLock(eqTo(lock))(any())) thenReturn Future.successful(Some(lock))
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(Some(id))

        val result = controller.getLock(fakeRequest)
        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(lock)
      }

      "return NOT FOUND when the data doesn't exist" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(repo.getLock(eqTo(lock))(any())) thenReturn Future.successful(None)
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(Some(id))

        val result = controller.getLock(fakeRequest)
        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(repo.getLock(eqTo(lock))(any())) thenReturn Future.failed(new Exception())
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(Some(id))

        val result = controller.getLock(fakeRequest)
        an[Exception] must be thrownBy status(result)
      }

      "throw an exception when the call is not authorised" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(None)

        val result = controller.getLock(fakeRequest)
        an[CredIdNotFoundFromAuth] must be thrownBy status(result)
      }

    }

    "calling getLockByUser" must {
      "return OK with the data" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(repo.getLockByCredId(eqTo(id))) thenReturn Future.successful(Some(lock))
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(Some(id))

        val result = controller.getLockByUser(fakeRequest)
        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(lock)
      }

      "return NOT FOUND when the data doesn't exist" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(repo.getLockByCredId(eqTo(id))) thenReturn Future.successful(None)
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(Some(id))

        val result = controller.getLockByUser(fakeRequest)
        status(result) mustEqual NOT_FOUND
      }

      "throw an exception when the repository call fails" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(repo.getLockByCredId(eqTo(id))) thenReturn Future.failed(new Exception())
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(Some(id))

        val result = controller.getLockByUser(fakeRequest)
        an[Exception] must be thrownBy status(result)
      }

      "throw an exception when the call is not authorised" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(None)

        val result = controller.getLockByUser(fakeRequest)
        an[CredIdNotFoundFromAuth] must be thrownBy status(result)
      }

    }

    "calling save" must {

      "return OK when the data is saved successfully" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(repo.setLock(any())) thenReturn Future.successful(true)
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(Some(id))

        val result = controller.lock(fakePostRequest)
        status(result) mustEqual OK
      }

      "throw an exception when the call is not authorised" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(None)

        val result = controller.lock(fakePostRequest)
        an[CredIdNotFoundFromAuth] must be thrownBy status(result)
      }
    }

    "calling removeLockOnScheme" must {
      "return OK when the data is removed successfully" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(repo.releaseLockByPstr(eqTo(pstr))) thenReturn Future.successful(true)
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.successful(())

        val result = controller.removeLockOnScheme()(fakeRequest)
        status(result) mustEqual OK
      }

      "throw an exception when the call is not authorised" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(authConnector.authorise[Unit](any(), any())(any(), any())) thenReturn Future.failed(new Exception())

        val result = controller.removeLockOnScheme()(fakeRequest)
        an[Exception] must be thrownBy status(result)
      }
    }

    "calling removeLockByUser" must {
      "return OK when the data is removed successfully" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(repo.releaseLockByCredId(eqTo(id))) thenReturn Future.successful(true)
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(Some(id))

        val result = controller.removeLockByUser()(fakeRequest)
        status(result) mustEqual OK
      }

      "throw an exception when the call is not authorised" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(None)

        val result = controller.removeLockByUser()(fakeRequest)
        an[CredIdNotFoundFromAuth] must be thrownBy status(result)
      }
    }

    "calling removeLock" must {
      "return OK when the data is removed successfully" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(repo.releaseLock(eqTo(lock))) thenReturn Future.successful(true)
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(Some(id))

        val result = controller.removeLock()(fakeRequest)
        status(result) mustEqual OK
      }

      "throw an exception when the call is not authorised" in {
        val app = new GuiceApplicationBuilder()
          .configure(conf = "auditing.enabled" -> false, "metrics.enabled" -> false, "metrics.jvm" -> false, "run.mode" -> "Test")
          .overrides(modules: _*).build()
        val controller = app.injector.instanceOf[LockCacheController]
        when(authConnector.authorise[Option[String]](any(), any())(any(), any())) thenReturn Future.successful(None)

        val result = controller.removeLock()(fakeRequest)
        an[CredIdNotFoundFromAuth] must be thrownBy status(result)
      }
    }
  }
}
