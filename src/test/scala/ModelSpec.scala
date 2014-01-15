import code.akka.{SchedulerState, SchedulerUtils}
import code.model.{Job, Scheduler}
import java.sql.Time
import java.util.Date
import org.specs2.mutable.Specification

/**
 * Created by ivan on 15/01/14.
 */


class ModelSpec extends Specification {
  "Models" should {
    new bootstrap.liftweb.Boot().boot

    val job = Job.find(1).get
    val ret = SchedulerUtils.schedulerManager(job)
    val ret2=SchedulerUtils.schedulerManager(job, Some(ret.get.copy(currentHits = ret.get.targetHits)))
//    Scheduler.from(now).to(now).job(job.head).save()
    ok
  }

}
