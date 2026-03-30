package kurs.backend.domain.persistence.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;

import kurs.backend.domain.persistence.entity.Timesheet;

public class TimesheetDao extends GenericDaoImpl<Timesheet, UUID> {

  public TimesheetDao() {
    super(Timesheet.class);
  }

  public List<Timesheet> findByEmployeeId(UUID employeeId) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      List<Timesheet> result =
          session
              .createQuery("FROM Timesheet t WHERE t.employee.id = :eid", Timesheet.class)
              .setParameter("eid", employeeId)
              .list();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }

  public List<Timesheet> findByEmployeeIdAndPeriod(UUID employeeId, LocalDate from, LocalDate to) {
    Session session = getSession();
    Transaction tx = session.beginTransaction();
    try {
      List<Timesheet> result =
          session
              .createQuery(
                  """
                        FROM Timesheet t
                        WHERE t.employee.id = :eid
                          AND t.workDate BETWEEN :from AND :to
                        """,
                  Timesheet.class)
              .setParameter("eid", employeeId)
              .setParameter("from", from)
              .setParameter("to", to)
              .list();
      tx.commit();
      return result;
    } catch (Exception e) {
      tx.rollback();
      throw e;
    }
  }
}
