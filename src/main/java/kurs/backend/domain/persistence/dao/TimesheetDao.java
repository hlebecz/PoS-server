package kurs.backend.domain.persistence.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.Timesheet;

public class TimesheetDao extends GenericDaoImpl<Timesheet, UUID> {

  private static final Logger log = LogManager.getLogger(TimesheetDao.class);

  public TimesheetDao(SessionFactory sessionFactory) {
    super(Timesheet.class, sessionFactory);
  }

  public List<Timesheet> findByEmployeeId(UUID employeeId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug("Starting transaction: findByEmployeeId with employeeId={}", employeeId);
    try {
      List<Timesheet> result =
          session
              .createQuery(
                  """
                  FROM Timesheet t
                  LEFT JOIN FETCH t.employee
                  WHERE t.employee.id = :eid
                  """,
                  Timesheet.class)
              .setParameter("eid", employeeId)
              .list();
      tx.commit();
      log.debug("Transaction committed: findByEmployeeId - count={}", result.size());
      return result;
    } catch (Exception e) {
      log.error("Transaction rollback: findByEmployeeId failed - {}", e.getMessage());
      tx.rollback();
      throw e;
    }
  }

  public List<Timesheet> findByEmployeeIdAndPeriod(UUID employeeId, LocalDate from, LocalDate to) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    log.debug(
        "Starting transaction: findByEmployeeIdAndPeriod with employeeId={}, from={}, to={}",
        employeeId,
        from,
        to);
    try {
      List<Timesheet> result =
          session
              .createQuery(
                  """
                        FROM Timesheet t
                        LEFT JOIN FETCH t.employee
                        WHERE t.employee.id = :eid
                          AND t.workDate BETWEEN :from AND :to
                        """,
                  Timesheet.class)
              .setParameter("eid", employeeId)
              .setParameter("from", from)
              .setParameter("to", to)
              .list();
      tx.commit();
      log.debug("Transaction committed: findByEmployeeIdAndPeriod - count={}", result.size());
      return result;
    } catch (Exception e) {
      log.error("Transaction rollback: findByEmployeeIdAndPeriod failed - {}", e.getMessage());
      tx.rollback();
      throw e;
    }
  }
}
